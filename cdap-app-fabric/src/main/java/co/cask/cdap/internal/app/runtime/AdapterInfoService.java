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

package co.cask.cdap.internal.app.runtime;

import co.cask.cdap.adapter.Sink;
import co.cask.cdap.adapter.Source;
import co.cask.cdap.api.schedule.SchedulableProgramType;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility service that provides access to adapterInfos currently registered
 */
public class AdapterInfoService extends AbstractIdleService {
  private static final Logger LOG = LoggerFactory.getLogger(AdapterInfoService.class);
  private final CConfiguration configuration;
  private Map<String, AdapterInfo> adapterInfos;

  @Inject
  public AdapterInfoService(CConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void startUp() throws Exception {
    LOG.info("Starting AdapterInfoService");
    adapterInfos = registerAdapters();
  }

  @Override
  protected void shutDown() throws Exception {
    LOG.info("Shutting down AdapterInfoService");
  }

  /**
   * Retrieves information about an Adapter
   *
   * @param adapterType the type of the requested AdapterInfo
   * @return requested AdapterInfo or null if no such AdapterInfo exists
   */
  public AdapterInfo getAdapter(String adapterType) {
    return adapterInfos.get(adapterType);
  }

  private Map<String, AdapterInfo> registerAdapters() {
    ImmutableMap.Builder<String, AdapterInfo> builder = ImmutableMap.builder();
    Collection<File> files = Collections.EMPTY_LIST;
    try {
      File baseDir = new File(configuration.get(Constants.AppFabric.ADAPTER_DIR));
      files = FileUtils.listFiles(baseDir, new String[]{"jar"}, true);

    } catch (Exception e) {
      LOG.warn("Unable to read the plugins directory ");
    }

    for (File file : files) {
      try {
        Manifest manifest = new JarFile(file.getAbsolutePath()).getManifest();
        if (manifest != null) {
          Attributes mainAttributes = manifest.getMainAttributes();
          String adapterType = mainAttributes.getValue("CDAP-Adapter-Type");
          String sourceTypeString = mainAttributes.getValue("CDAP-Source-Type");
          Source.Type sourceType = Source.Type.valueOf(sourceTypeString);
          String sinkTypeString = mainAttributes.getValue("CDAP-Sink-Type");
          Sink.Type sinkType = Sink.Type.valueOf(sinkTypeString);
          String scheduleProgramId = mainAttributes.getValue("CDAP-Scheduled-Program-Id");
          SchedulableProgramType scheduleProgramType =
            SchedulableProgramType.valueOf(mainAttributes.getValue("CDAP-Scheduled-Program-Type"));

          AdapterInfo adapterInfo = new AdapterInfo(file, adapterType, sourceType, sinkType, scheduleProgramId,
                                                    scheduleProgramType);
          if (adapterType != null && scheduleProgramId != null && scheduleProgramType != null) {
            builder.put(adapterType, adapterInfo);
          } else {
            LOG.error("Missing information for adapter at {}", file.getAbsolutePath());
          }
        }
      } catch (IOException e) {
        LOG.warn(String.format("Unable to read adapter jar %s", file.getAbsolutePath()));
      }

    }
    return builder.build();
  }

  /**
   * Holds information about an Adapter
   */
  public static final class AdapterInfo {

    private final File file;
    private final String type;
    private final Source.Type sourceType;
    private final Sink.Type sinkType;
    private final String scheduleProgramId;
    private final SchedulableProgramType scheduleProgramType;

    public AdapterInfo(File file, String adapterType, Source.Type sourceType, Sink.Type sinkType,
                       String scheduleProgramId, SchedulableProgramType scheduleProgramType) {
      this.file = file;
      this.type = adapterType;
      this.sourceType = sourceType;
      this.sinkType = sinkType;
      this.scheduleProgramId = scheduleProgramId;
      this.scheduleProgramType = scheduleProgramType;
    }

    public File getFile() {
      return file;
    }

    public String getType() {
      return type;
    }

    public Source.Type getSourceType() {
      return sourceType;
    }

    public Sink.Type getSinkType() {
      return sinkType;
    }

    public String getScheduleProgramId() {
      return scheduleProgramId;
    }

    public SchedulableProgramType getScheduleProgramType() {
      return scheduleProgramType;
    }
  }
}
