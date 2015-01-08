/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.conversion;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.stream.StreamBatchReadable;
import co.cask.cdap.api.dataset.lib.FileSet;
import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.api.mapreduce.AbstractMapReduce;
import co.cask.cdap.api.mapreduce.MapReduceContext;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.twill.filesystem.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MapReduce job that reads events from a stream over a given time interval and writes the events out to a FileSet
 * in avro format.
 */
public class StreamConversionMapReduce extends AbstractMapReduce {
  private static final Logger LOG = LoggerFactory.getLogger(StreamConversionMapReduce.class);
  public static final String SCHEMA_KEY = "cdap.stream.conversion.output.schema";
  public static final String STREAM_NAME = "cdap.stream.conversion.stream.name";
  public static final String FILESET_NAME = "cdap.stream.conversion.fileset.name";
  public static final String RUN_FREQUENCY_MS = "cdap.stream.conversion.run.frequency.ms";
  // impala only supports scalar types, so we can't include headers as a map
  // instead, allow a comma separated list of headers to be set in runtime args as columns to include.
  // assumes the schema of the output dataset is appropriately named.
  public static final String HEADERS = "cdap.stream.conversion.headers";
  public static final String MAPPER_MEMORY = "cdap.stream.conversion.mapper.memorymb";
  private String jobTime;
  private Location filesetLocation;
  private Location jobOutputLocation;

  @Override
  public void configure() {
    setDescription("Job to read a chunk of stream events and write them to a FileSet");
    setMapperResources(new Resources(512));
  }

  @Override
  public void beforeSubmit(MapReduceContext context) throws Exception {
    Job job = context.getHadoopJob();
    job.setMapperClass(StreamConversionMapper.class);
    job.setNumReduceTasks(0);
    job.setMapOutputKeyClass(AvroKey.class);
    job.setMapOutputValueClass(NullWritable.class);

    Map<String, String> runtimeArgs = context.getRuntimeArguments();
    // set memory if present in runtime args
    // TODO: move this capability into the framework.
    String mapperMemoryMBStr = runtimeArgs.get(MAPPER_MEMORY);
    if (mapperMemoryMBStr != null) {
      int mapperMemoryMB = Integer.parseInt(mapperMemoryMBStr);
      job.getConfiguration().setInt(Job.MAP_MEMORY_MB, mapperMemoryMB);
      // Also set the Xmx to be smaller than the container memory.
      job.getConfiguration().set(Job.MAP_JAVA_OPTS, "-Xmx" + (int) (mapperMemoryMB * 0.8) + "m");
    }

    // the time when this MapReduce job is supposed to start if this job is started by the scheduler
    long logicalStartTime = context.getLogicalStartTime();
    long runFrequency = Long.parseLong(getRequiredArg(runtimeArgs, RUN_FREQUENCY_MS));
    jobTime = String.valueOf(TimeUnit.SECONDS.convert(logicalStartTime, TimeUnit.MILLISECONDS));
    String streamName = getRequiredArg(runtimeArgs, STREAM_NAME);
    StreamBatchReadable.useStreamInput(context, streamName, logicalStartTime - runFrequency, logicalStartTime);

    String filesetName = getRequiredArg(runtimeArgs, FILESET_NAME);
    FileSet fileSet = context.getDataset(filesetName);

    // this schema is our schema, which is slightly different than avro's schema in terms of the types it supports.
    // Incompatibilities will surface here and cause the job to fail.
    // TODO: support output formats other than avro
    String schemaStr = fileSet.getOutputFormatConfiguration().get("schema");
    job.getConfiguration().set(SCHEMA_KEY, schemaStr);
    Schema schema = new Schema.Parser().parse(schemaStr);
    AvroJob.setOutputKeySchema(job, schema);
    // set the headers that should be included for each event
    String headersStr = runtimeArgs.get(HEADERS);
    if (headersStr != null) {
      job.getConfiguration().set(HEADERS, runtimeArgs.get(HEADERS));
    }

    // each job will output to it's logical start time so that concurrent jobs don't step on each other.
    // the assumption here is that multiple runs will not have the same logical start time.
    // TODO: set the output path here instead of assuming it is set in the runtime args by whatever
    //       is running this program. Requires some way of setting runtime args in this method (or some other method)
    filesetLocation = fileSet.getBaseLocation();
    LOG.error("ashau - fileset location = {}.", filesetLocation.toURI().toString());
    jobOutputLocation = fileSet.getOutputLocation();
    LOG.error("ashau - output location = {}.", jobOutputLocation.toURI().toString());
    context.setOutput(filesetName);
  }

  private String getRequiredArg(Map<String, String> runtimeArgs, String key) {
    String val = runtimeArgs.get(key);
    if (val == null) {
      throw new IllegalArgumentException(key + " must be set in the runtime arguments.");
    }
    return val;
  }

  @Override
  public void onFinish(boolean succeeded, MapReduceContext context) throws Exception {
    if (succeeded) {
      LOG.error("ashau - cleaning up output.");
      // this will change once there is support for partitions. For now, copy output files to the base
      // path so they can all be queried by Impala and Hive.
      for (Location loc : jobOutputLocation.list()) {
        String locName = loc.getName();
        // mapreduce also puts other stuff like a success file in the directory. we just want the output files.
        if (isOutputFile(locName)) {
          loc.renameTo(filesetLocation.append(jobTime + "." + locName));
        }
      }
      // recursively delete the output directory now that we've moved all the files.
      jobOutputLocation.delete(true);
    }
  }

  // mapreduce will put a _SUCCESS file in the directory, as well as hidden files.
  private boolean isOutputFile(String fileName) {
    char firstChar = fileName.charAt(0);
    return !(firstChar == '.' || firstChar == '_');
  }

  /**
   * Mapper that reads events from a stream and writes them out as Avro.
   */
  public static class StreamConversionMapper extends
    Mapper<LongWritable, StreamEvent, AvroKey<GenericRecord>, NullWritable> {
    private Schema schema;
    private String[] headers;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      schema = new Schema.Parser().parse(context.getConfiguration().get(SCHEMA_KEY));
      String headersStr = context.getConfiguration().get(HEADERS);
      headers = headersStr == null ? new String[0] : headersStr.split(",");
    }

    @Override
    public void map(LongWritable timestamp, StreamEvent streamEvent, Context context)
      throws IOException, InterruptedException {
      // TODO: replace with stream event -> avro record conversion
      GenericRecordBuilder recordBuilder = new GenericRecordBuilder(schema)
        .set("ts", streamEvent.getTimestamp())
        .set("body", Bytes.toString(streamEvent.getBody()));
      Map<String, String> eventHeaders = streamEvent.getHeaders();
      if (eventHeaders != null) {
        for (String header : headers) {
          recordBuilder.set(header, eventHeaders.get(header));
        }
      }
      GenericRecord record = recordBuilder.build();
      context.write(new AvroKey<GenericRecord>(record), NullWritable.get());
    }
  }
}
