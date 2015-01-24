/*
 * Copyright © 2014 Cask Data, Inc.
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

import java.util.Iterator;
import java.util.List;

/**
 *
 */
public final class MetricScanResult implements Iterable<TimeValue> {
  private final String metricName;
  private final List<TagValue> tagValues;
  private final Iterable<TimeValue> timeValues;

  public MetricScanResult(String metricName, List<TagValue> tagValues, Iterable<TimeValue> timeValues) {
    this.metricName = metricName;
    this.tagValues = tagValues;
    this.timeValues = timeValues;
  }

  public String getMetricName() {
    return metricName;
  }

  public List<TagValue> getTagValues() {
    return tagValues;
  }

  @Override
  public Iterator<TimeValue> iterator() {
    return timeValues.iterator();
  }
}
