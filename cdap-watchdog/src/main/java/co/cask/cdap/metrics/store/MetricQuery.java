/*
 * Copyright 2014 Cask Data, Inc.
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

package co.cask.cdap.metrics.store;

import java.util.List;

/**
 *
 */
public final class MetricQuery {
  private final long startTs;
  private final long endTs;
  private final String metric;
  private final List<TagValue> tagsSlice;
  private final List<String> tagsGroupBy;

  public MetricQuery(long startTs, long endTs, String metric, List<TagValue> tagsSlice, List<String> tagsGroupBy) {
    this.startTs = startTs;
    this.endTs = endTs;
    this.metric = metric;
    this.tagsSlice = tagsSlice;
    this.tagsGroupBy = tagsGroupBy;
  }

  public long getStartTs() {
    return startTs;
  }

  public long getEndTs() {
    return endTs;
  }

  public String getMetric() {
    return metric;
  }

  public List<TagValue> getTagsSlice() {
    return tagsSlice;
  }

  public List<String> getTagsGroupBy() {
    return tagsGroupBy;
  }
}
