package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.DequeueStrategy;
import com.continuuity.data2.transaction.queue.QueueScanner;
import com.google.common.base.Function;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
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

  private static final HashFunction HASH_FUNCTION = Hashing.murmur3_32();

  private static final Function<byte[], byte[]> ROW_KEY_CONVERTER = new Function<byte[], byte[]>() {
    @Override
    public byte[] apply(byte[] input) {
      return Arrays.copyOfRange(input, ShardedHBaseQueueAdmin.PREFIX_BYTES, input.length);
    }
  };

  private final QueueName queueName;
  private final ConsumerConfig consumerConfig;

  ShardedHBaseQueueStrategy(QueueName queueName, ConsumerConfig consumerConfig) {
    this.queueName = queueName;
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
    byte[] result = new byte[ShardedHBaseQueueAdmin.PREFIX_BYTES + originalRowKey.length];
    Bytes.putBytes(result, ShardedHBaseQueueAdmin.PREFIX_BYTES, originalRowKey, 0, originalRowKey.length);

    // Default for FIFO case.
    int instanceId = -1;
    long groupId = consumerConfig.getGroupId();

    if (consumerConfig.getDequeueStrategy() != DequeueStrategy.FIFO) {
      instanceId = consumerConfig.getInstanceId();
      groupId = consumerConfig.getGroupId();
    }

    HashCode hash = HASH_FUNCTION.newHasher()
      .putLong(consumerConfig.getGroupId())
      .putInt(instanceId)
      .putString(queueName.toString())
      .hash();

    hash.writeBytesTo(result, 0, 1);
    Bytes.putLong(result, 1, groupId);
    Bytes.putInt(result, 1 + Bytes.SIZEOF_LONG, instanceId);

    return result;
  }
}
