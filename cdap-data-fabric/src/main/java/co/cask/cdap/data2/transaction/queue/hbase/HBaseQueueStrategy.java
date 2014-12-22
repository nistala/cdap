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

package co.cask.cdap.data2.transaction.queue.hbase;

import co.cask.cdap.data2.transaction.queue.QueueScanner;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;

import java.io.IOException;

/**
 *
 */
public interface HBaseQueueStrategy {

  /**
   * Creates a {@link QueueScanner} from the given {@link Scan} on the HBase table.
   *
   * @param hTable HTable for talking to HBase
   * @param scan The scan request
   * @param numRows Maximum number of rows to scan for
   * @return A {@link QueueScanner} that scans over the give table
   */
  QueueScanner create(HTable hTable, Scan scan, int numRows) throws IOException;

  /**
   * Creates the actual row key used for accessing the HBase table from the given queue entry row key.
   */
  byte[] getActualRowKey(byte[] originalRowKey);
}
