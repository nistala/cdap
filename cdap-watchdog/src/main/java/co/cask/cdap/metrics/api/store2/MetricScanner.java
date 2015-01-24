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

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.table.Row;
import co.cask.cdap.api.dataset.table.Scanner;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public final class MetricScanner implements Iterator<MetricScanResult> {

  private final MetricRecordCodec codec;

  private final Scanner scanner;
  private final long startTs;
  private final long endTs;


  // Track the number of row scanned through the iterator. It's for reporting and debugging purpose.
  private int rowScanned;

  // Use an internal iterator to avoid leaking AbstractIterator methods to outside.
  private final Iterator<MetricScanResult> internalIterator;

  /**
   * Construct a MetricScanner. Should only be called by MetricTable.
   */
  MetricScanner(Scanner scanner, MetricRecordCodec codec, long startTs, long endTs) {
    this.scanner = scanner;
    this.codec = codec;
    this.internalIterator = createIterator();
    this.startTs = startTs;
    this.endTs = endTs;
  }

  public void close() {
    scanner.close();
  }

  public int getRowScanned() {
    return rowScanned;
  }

  @Override
  public boolean hasNext() {
    return internalIterator.hasNext();
  }

  @Override
  public MetricScanResult next() {
    return internalIterator.next();
  }

  @Override
  public void remove() {
    internalIterator.remove();
  }

  private Iterator<MetricScanResult> createIterator() {
    return new AbstractIterator<MetricScanResult>() {
      @Override
      protected MetricScanResult computeNext() {
        Row rowResult;
        while ((rowResult = scanner.next()) != null) {
          rowScanned++;
          byte[] rowKey = rowResult.getRow();

          // Decode context and metric from key
          String metricName = codec.getMetricName(rowKey);
          List<TagValue> tagValues = codec.getTagValues(rowKey);

          boolean exhausted = false;
          List<TimeValue> timeValues = Lists.newLinkedList();
          // todo: entry set is ordered by ts?
          for (Map.Entry<byte[], byte[]> columnValue : rowResult.getColumns().entrySet()) {
            long ts = codec.getTimestamp(rowKey, columnValue.getKey());
            if (ts < startTs) {
              continue;
            }

            if (ts > endTs) {
              exhausted = true;
              break;
            }

            // todo: move Bytes.toLong into codec?
            TimeValue timeValue = new TimeValue(ts, Bytes.toLong(columnValue.getValue()));
            timeValues.add(timeValue);
          }

          if (timeValues.isEmpty() && exhausted) {
            break;
          }

          return new MetricScanResult(metricName, tagValues, timeValues);
        }

        scanner.close();
        return endOfData();
      }
    };
  }
}
