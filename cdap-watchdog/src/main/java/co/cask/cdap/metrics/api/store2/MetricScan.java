/*
 * Copyright 2015 Cask Data, Inc.
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

package co.cask.cdap.metrics.api.store2;

import co.cask.cdap.metrics.transport.MetricType;

import java.util.List;

/**
 *
 */
public class MetricScan {
  private final List<TagValue> tagValues;
  private final MetricType metricType;
  private final String metricName;
  private final long startTs;
  private final long endTs;

  public MetricScan(long endTs, long startTs, String metricName, MetricType metricType, List<TagValue> tagValues) {
    this.endTs = endTs;
    this.startTs = startTs;
    this.metricName = metricName;
    this.metricType = metricType;
    this.tagValues = tagValues;
  }

  public List<TagValue> getTagValues() {
    return tagValues;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public String getMetricName() {
    return metricName;
  }

  public long getStartTs() {
    return startTs;
  }

  public long getEndTs() {
    return endTs;
  }
}
