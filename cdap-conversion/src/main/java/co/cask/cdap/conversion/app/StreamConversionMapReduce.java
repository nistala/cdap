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

package co.cask.cdap.conversion.app;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.data.format.FormatSpecification;
import co.cask.cdap.api.data.format.StructuredRecord;
import co.cask.cdap.api.data.stream.StreamBatchReadable;
import co.cask.cdap.api.dataset.lib.FileSet;
import co.cask.cdap.api.dataset.lib.FileSetArguments;
import co.cask.cdap.api.mapreduce.AbstractMapReduce;
import co.cask.cdap.api.mapreduce.MapReduceContext;
import co.cask.cdap.api.stream.GenericStreamEventData;
import co.cask.cdap.conversion.avro.Converter;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MapReduce job that reads events from a stream over a given time interval and writes the events out to a FileSet
 * in avro format.
 */
public class StreamConversionMapReduce extends AbstractMapReduce {
  private static final Gson GSON = new Gson();
  private static final Logger LOG = LoggerFactory.getLogger(StreamConversionMapReduce.class);
  private static final Type mapType = new TypeToken<Map<String, String>>() { }.getType();
  public static final String ADAPTER_PROPERTIES = "adapter.properties";
  public static final String SOURCE_NAME = "source.name";
  public static final String SOURCE_PROPERTIES = "source.properties";
  public static final String SINK_NAME = "sink.name";
  public static final String SINK_PROPERTIES = "sink.properties";
  public static final String SCHEMA = "schema";
  public static final String FORMAT_NAME = "format.name";
  public static final String FORMAT_SETTINGS = "format.settings";
  public static final String FREQUENCY = "frequency";
  public static final String HEADERS = "headers";
  // for mapred
  public static final String SCHEMA_KEY = "cdap.stream.conversion.output.schema";
  private String sinkName;
  private String outputPath;
  private Long partitionTime;

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

    // setup input arguments
    String streamName = getRequiredArg(runtimeArgs, SOURCE_NAME);
    // the time when this MapReduce job is supposed to start if this job is started by the scheduler
    long logicalStartTime = context.getLogicalStartTime();

    // get adapter properties -- format name, format settings, schema, and run frequency
    Map<String, String> adapterProperties = GSON.fromJson(getRequiredArg(runtimeArgs, ADAPTER_PROPERTIES), mapType);
    String formatName = getRequiredArg(adapterProperties, FORMAT_NAME);
    Map<String, String> formatSettings = GSON.fromJson(getRequiredArg(adapterProperties, FORMAT_SETTINGS), mapType);
    // set the headers that should be included for each event
    String headersStr = adapterProperties.get(HEADERS);
    Set<String> headers = Sets.newHashSet();
    if (headersStr != null) {
      job.getConfiguration().set(HEADERS, headersStr);
      for (String header : headersStr.split(",")) {
        headers.add(header);
      }
    }

    // read a time interval from the stream as input, using the given format
    partitionTime = logicalStartTime;
    long runFrequency = Long.parseLong(getRequiredArg(adapterProperties, FREQUENCY));
    sinkName = getRequiredArg(runtimeArgs, SINK_NAME);
    Map<String, String> sinkArgs = Maps.newHashMap();
    FileSetArguments.setOutputPath(sinkArgs, String.valueOf(partitionTime));
    FileSet sink = context.getDataset(sinkName, sinkArgs);

    /*TimePartitionedFileSetArguments.setOutputPartitionTime(sinkArgs, partitionTime);
    TimePartitionedFileSet sink = context.getDataset(sinkName, sinkArgs);*/

    String schemaStr = getRequiredArg(adapterProperties, SCHEMA);
    FormatSpecification formatSpec = new FormatSpecification(
      formatName,
      bodySchema(co.cask.cdap.api.data.schema.Schema.parse(schemaStr), headers),
      formatSettings);
    StreamBatchReadable.useStreamInput(context, streamName,
                                       logicalStartTime - runFrequency, logicalStartTime,
                                       formatSpec);

    // this schema is our schema, which is slightly different than avro's schema in terms of the types it supports.
    // Incompatibilities will surface here and cause the job to fail.
    // TODO: support output formats other than avro
    Schema schema = new Schema.Parser().parse(schemaStr);
    job.getConfiguration().set(SCHEMA_KEY, schema.toString());
    AvroJob.setOutputKeySchema(job, schema);
    context.setOutput(sinkName, sink);
    //outputPath = FileSetArguments.getOutputPath(sink.getUnderlyingFileSet().getRuntimeArguments());
  }

  // strip timestamp and headers from the full schema to get the schema of the stream body
  private co.cask.cdap.api.data.schema.Schema bodySchema(
    co.cask.cdap.api.data.schema.Schema schema, Set<String> headers) {

    List<co.cask.cdap.api.data.schema.Schema.Field> fields = Lists.newArrayList();
    for (co.cask.cdap.api.data.schema.Schema.Field field : schema.getFields()) {
      String fieldName = field.getName();
      if (!"ts".equals(fieldName) && !headers.contains(fieldName)) {
        fields.add(field);
      }
    }
    return co.cask.cdap.api.data.schema.Schema.recordOf("eventBody", fields);
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
    /*
    if (succeeded) {
      TimePartitionedFileSet converted = context.getDataset(sinkName);

      LOG.info("Adding partition for time {} with path {} to dataset '{}'", partitionTime, outputPath, sinkName);
      converted.addPartition(partitionTime, outputPath);
    }*/
  }

  /**
   * Mapper that reads events from a stream and writes them out as Avro.
   */
  public static class StreamConversionMapper extends
    Mapper<LongWritable, GenericStreamEventData<StructuredRecord>, AvroKey<GenericRecord>, NullWritable> {
    private Schema schema;
    private String[] headers;
    private Converter converter;

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
      schema = new Schema.Parser().parse(context.getConfiguration().get(SCHEMA_KEY));
      String headersStr = context.getConfiguration().get(HEADERS);
      headers = headersStr == null ? new String[0] : headersStr.split(",");
      converter = new Converter(schema, headers);
    }

    @Override
    public void map(LongWritable timestamp, GenericStreamEventData<StructuredRecord> streamEvent, Context context)
      throws IOException, InterruptedException {
      Map<String, String> headers = Objects.firstNonNull(streamEvent.getHeaders(), ImmutableMap.<String, String>of());
      GenericRecord record = converter.convert(streamEvent.getBody(), timestamp.get(), headers);
      context.write(new AvroKey<GenericRecord>(record), NullWritable.get());
    }
  }
}
