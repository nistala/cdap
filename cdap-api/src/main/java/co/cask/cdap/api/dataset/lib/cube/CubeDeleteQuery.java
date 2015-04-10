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

package co.cask.cdap.api.dataset.lib.cube;

import co.cask.cdap.api.annotation.Beta;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Defines a query for deleting data in {@link Cube}.
 */
@Beta
public class CubeDeleteQuery {
  private final long startTs;
  private final long endTs;
  private final int resolution;
  private final String measureName;
  private final Map<String, String> sliceByTagValues;

  /**
   * Creates instance of {@link CubeDeleteQuery} that defines selection of data to delete from {@link Cube}.
   * @param startTs start time of the data selection, in seconds since epoch
   * @param endTs end time of the data selection, in seconds since epoch
   * @param resolution resolution of the aggregations to delete from
   * @param sliceByTagValues tag name, tag value pairs that define the data selection
   * @param measureName name of the measure to delete, {@code null} means delete all
   */
  public CubeDeleteQuery(long startTs, long endTs, int resolution,
                         Map<String, String> sliceByTagValues, @Nullable String measureName) {
    this.startTs = startTs;
    this.endTs = endTs;
    this.resolution = resolution;
    this.measureName = measureName;
    this.sliceByTagValues = Collections.unmodifiableMap(new HashMap<String, String>(sliceByTagValues));
  }

  public long getStartTs() {
    return startTs;
  }

  public long getEndTs() {
    return endTs;
  }

  public int getResolution() {
    return resolution;
  }

  public String getMeasureName() {
    return measureName;
  }

  public Map<String, String> getSliceByTags() {
    return sliceByTagValues;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("CubeDeleteQuery");
    sb.append("{startTs=").append(startTs);
    sb.append(", endTs=").append(endTs);
    sb.append(", resolution=").append(resolution);
    sb.append(", measureName='").append(measureName == null ? "null" : measureName).append('\'');
    sb.append(", sliceByTagValues=").append(sliceByTagValues);
    sb.append('}');
    return sb.toString();
  }
}