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

import co.cask.cdap.metrics.data.AggregatesTable;
import co.cask.cdap.metrics.data.MetricsTableFactory;
import co.cask.cdap.metrics.data.TimeSeriesTables;
import co.cask.cdap.metrics.transport.MetricType;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Executes metrics requests, returning a json object representing the result of the request.
 */
public class DefaultMetricStore implements MetricStore {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultMetricStore.class);

  private final Supplier<AggregatesTable> aggregatesTables;
  private final TimeSeriesTables timeSeriesTables;

  public DefaultMetricStore(final MetricsTableFactory metricsTableFactory) {
    this.timeSeriesTables = new TimeSeriesTables(metricsTableFactory);
    this.aggregatesTables = Suppliers.memoize(new Supplier<AggregatesTable>() {
      @Override
      public AggregatesTable get() {
        return metricsTableFactory.createAggregates();
      }
    });
  }

  @Override
  public void add(MetricType metricType, MetricSlice metricSlice, TimeValue timeValue) {
    // todo
  }

  @Override
  public List<TimeSeries> query(MetricQuery query) {
    if (query.getStartTs() >= 0 && query.getEndTs() >= 0) {
      return timeRangeQuery(query);
    } else {
      return totalsQuery(query);
    }
  }

  private List<TimeSeries> timeRangeQuery(MetricQuery query) {
    return null;
  }

  private List<TimeSeries> totalsQuery(MetricQuery query) {
    return null;
  }

}
