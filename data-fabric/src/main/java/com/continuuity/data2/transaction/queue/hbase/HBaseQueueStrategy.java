package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.transaction.queue.QueueScanner;
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
