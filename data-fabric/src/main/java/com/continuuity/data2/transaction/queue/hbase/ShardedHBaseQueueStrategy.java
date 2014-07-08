package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.DequeueStrategy;
import com.continuuity.data2.transaction.queue.QueueScanner;
import com.google.common.base.Function;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Scan;

import java.io.IOException;
import java.util.Arrays;

/**
 * A {@link HBaseQueueStrategy} that scans with sharded keys.
 *
 * See {@link ShardedHBaseQueueProducer} for row key schema.
 */
final class ShardedHBaseQueueStrategy implements HBaseQueueStrategy {

  private static final Function<byte[], byte[]> ROW_KEY_CONVERTER = new Function<byte[], byte[]>() {
    @Override
    public byte[] apply(byte[] input) {
      return Arrays.copyOfRange(input, Bytes.SIZEOF_LONG + Bytes.SIZEOF_INT, input.length);
    }
  };

  private final ConsumerConfig consumerConfig;

  ShardedHBaseQueueStrategy(ConsumerConfig consumerConfig) {
    this.consumerConfig = consumerConfig;
  }

  @Override
  public QueueScanner create(HTable hTable, Scan scan, int numRows) throws IOException {
    // Modify the scan with sharded key prefix
    Scan shardedScan = new Scan(scan);
    shardedScan.setStartRow(getActualRowKey(scan.getStartRow()));
    shardedScan.setStopRow(getActualRowKey(scan.getStopRow()));

    return new HBaseQueueScanner(hTable.getScanner(shardedScan), numRows, ROW_KEY_CONVERTER);
  }

  @Override
  public byte[] getActualRowKey(byte[] originalRowKey) {
    byte[] result = new byte[Bytes.SIZEOF_LONG + Bytes.SIZEOF_INT + originalRowKey.length];
    Bytes.putBytes(result, Bytes.SIZEOF_LONG + Bytes.SIZEOF_INT, originalRowKey, 0, originalRowKey.length);

    // Default for FIFO case.
    int instanceId = -1;
    long groupId = 0;

    if (consumerConfig.getDequeueStrategy() != DequeueStrategy.FIFO) {
      instanceId = consumerConfig.getInstanceId();
      groupId = consumerConfig.getGroupId();
    }

    Bytes.putLong(result, 0, groupId);
    Bytes.putInt(result, Bytes.SIZEOF_LONG, instanceId);

    return result;
  }
}
