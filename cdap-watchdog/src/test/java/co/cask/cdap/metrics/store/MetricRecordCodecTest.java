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

package co.cask.cdap.metrics.store;

import co.cask.cdap.data2.dataset2.lib.table.MetricsTable;
import co.cask.cdap.data2.dataset2.lib.table.inmemory.InMemoryMetricsTable;
import co.cask.cdap.data2.dataset2.lib.table.inmemory.InMemoryOrderedTableService;
import co.cask.cdap.metrics.data.EntityTable;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MetricRecordCodecTest {
  @Test
  public void test() {
    InMemoryOrderedTableService.create("MetricRecordCodecTest");
    MetricsTable table = new InMemoryMetricsTable("MetricRecordCodecTest");
    int resolution = 10;
    int rollTimebaseInterval = 2;
    MetricRecordCodec codec = new MetricRecordCodec(new EntityTable(table), resolution, rollTimebaseInterval);

    // testing encoding with multiple tags
    List<TagValue> tagValues = ImmutableList.of(new TagValue("tag1", "value1"),
                                                new TagValue("tag2", "value2"),
                                                new TagValue("tag3", "value3"));
    // note: we use seconds everywhere and rely on this
    long ts = 1422312915;
    byte[] rowKey = codec.createRowKey(tagValues, "myMetric", ts);
    byte[] column = codec.createColumn(ts);

    Assert.assertEquals((ts / resolution) * resolution, codec.getTimestamp(rowKey, column));
    Assert.assertEquals(tagValues, codec.getTagValues(rowKey));
    Assert.assertEquals("myMetric", codec.getMetricName(rowKey));

    // testing encoding without one tag
    tagValues = ImmutableList.of(new TagValue("myTag", "myValue"));
    rowKey = codec.createRowKey(tagValues, "mySingleTagMetric", ts);
    Assert.assertEquals((ts / resolution) * resolution, codec.getTimestamp(rowKey, column));
    Assert.assertEquals(tagValues, codec.getTagValues(rowKey));
    Assert.assertEquals("mySingleTagMetric", codec.getMetricName(rowKey));

    // testing encoding without empty tags
    rowKey = codec.createRowKey(new ArrayList<TagValue>(), "myNoTagsMetric", ts);
    Assert.assertEquals((ts / resolution) * resolution, codec.getTimestamp(rowKey, column));
    Assert.assertEquals(new ArrayList<TagValue>(), codec.getTagValues(rowKey));
    Assert.assertEquals("myNoTagsMetric", codec.getMetricName(rowKey));

    // todo: test that rollTimebaseInterval applies well

  }
}
