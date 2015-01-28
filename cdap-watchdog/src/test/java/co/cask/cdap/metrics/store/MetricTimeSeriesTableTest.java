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
package co.cask.cdap.metrics.store;

import co.cask.cdap.data2.dataset2.lib.table.inmemory.InMemoryMetricsTable;
import co.cask.cdap.data2.dataset2.lib.table.inmemory.InMemoryOrderedTableService;
import co.cask.cdap.metrics.data.EntityTable;
import co.cask.cdap.metrics.transport.MetricType;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test base for {@link co.cask.cdap.metrics.data.TimeSeriesTable}.
 */
public class MetricTimeSeriesTableTest {

  @Test
  public void test() throws Exception {
    InMemoryOrderedTableService.create("EntityTable");
    InMemoryOrderedTableService.create("DataTable");
    int resolution = 10;
    int rollTimebaseInterval = 2;

    MetricTimeSeriesTable table = new MetricTimeSeriesTable(new InMemoryMetricsTable("DataTable"),
                                                            new EntityTable(new InMemoryMetricsTable("EntityTable")),
                                                            resolution, rollTimebaseInterval);

    // aligned to start of resolution bucket
    // "/1000" because time is expected to be in seconds
    long ts = ((System.currentTimeMillis() / 1000) / resolution) * resolution;

    // testing encoding with multiple tags
    List<TagValue> tagValues = ImmutableList.of(new TagValue("tag1", "value1"),
                                                new TagValue("tag2", "value2"),
                                                new TagValue("tag3", "value3"));


    // trying adding one by one, in same (first) time resolution bucket
    for (int i = 0; i < 5; i++) {
      for (int k = 1; k < 4; k++) {
        table.add(ImmutableList.of(new Aggregation(tagValues, MetricType.COUNTER, "metric" + k,
                                                   new TimeValue(ts, k))));
      }
    }

    // trying adding one by one, in different time resolution buckets
    for (int i = 0; i < 3; i++) {
      for (int k = 1; k < 4; k++) {
        table.add(ImmutableList.of(new Aggregation(tagValues, MetricType.COUNTER, "metric" + k,
                                                   new TimeValue(ts + resolution * i, 2 * k))));
      }
    }

    // trying adding as list
    // first incs in same (second) time resolution bucket
    List<Aggregation> aggs = Lists.newArrayList();
    for (int i = 0; i < 7; i++) {
      for (int k = 1; k < 4; k++) {
        aggs.add(new Aggregation(tagValues, MetricType.COUNTER, "metric" + k,
                                 new TimeValue(ts + resolution, 3 * k)));
      }
    }
    // then incs in different time resolution buckets
    for (int i = 0; i < 3; i++) {
      for (int k = 1; k < 4; k++) {
        aggs.add(new Aggregation(tagValues, MetricType.COUNTER, "metric" + k,
                                 new TimeValue(ts + resolution * i, 4 * k)));
      }
    }

    table.add(aggs);

    // verify each metric
    for (int k = 1; k < 4; k++) {
      MetricScan scan = new MetricScan(ts - 2 * resolution, ts + 3 * resolution,
                                       "metric" + k, MetricType.COUNTER, tagValues);
      Table<String, List<TagValue>, List<TimeValue>> expected = HashBasedTable.create();
      expected.put("metric" + k, tagValues, ImmutableList.of(new TimeValue(ts, 11 * k),
                                                             new TimeValue(ts + resolution, 27 * k),
                                                             new TimeValue(ts + 2 * resolution, 6 * k)));
      assertScan(table, expected, scan);
    }

    // verify all metrics with fuzzy metric in scan
    Table<String, List<TagValue>, List<TimeValue>> expected = HashBasedTable.create();
    for (int k = 1; k < 4; k++) {
      expected.put("metric" + k, tagValues, ImmutableList.of(new TimeValue(ts, 11 * k),
                                                             new TimeValue(ts + resolution, 27 * k),
                                                             new TimeValue(ts + 2 * resolution, 6 * k)));
    }
    MetricScan scan = new MetricScan(ts - 2 * resolution, ts + 3 * resolution,
                                     // metric = null means "all"
                                     null, MetricType.COUNTER, tagValues);
    assertScan(table, expected, scan);

    // todo: scan fuzzy tag values
  }

  private void assertScan(MetricTimeSeriesTable table,
                          Table<String, List<TagValue>, List<TimeValue>> expected, MetricScan scan) throws Exception {
    Table<String, List<TagValue>, List<TimeValue>> resultTable = HashBasedTable.create();
    MetricScanner scanner = table.scan(scan);
    try {
      while (scanner.hasNext()) {
        MetricScanResult result = scanner.next();
        List<TimeValue> timeValues = resultTable.get(result.getMetricName(), result.getTagValues());
        if (timeValues == null) {
          timeValues = Lists.newArrayList();
          resultTable.put(result.getMetricName(), result.getTagValues(), timeValues);
        }
        timeValues.addAll(Lists.newArrayList(result.iterator()));
      }
    } finally {
      scanner.close();
    }

    Assert.assertEquals(expected, resultTable);
  }
}
