package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.DequeueStrategy;
import com.continuuity.data2.transaction.queue.QueueScanner;
import com.continuuity.hbase.wd.DistributedScanner;
import com.google.common.base.Function;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Threads;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link HBaseQueueStrategy} that scans with sharded keys.
 *
 * See {@link ShardedHBaseQueueProducer} for row key schema.
 */
final class ShardedHBaseQueueStrategy implements HBaseQueueStrategy, Closeable {

  private static final HashFunction HASH_FUNCTION = Hashing.murmur3_32();

  private static final Function<byte[], byte[]> ROW_KEY_CONVERTER = new Function<byte[], byte[]>() {
    @Override
    public byte[] apply(byte[] input) {
      return Arrays.copyOfRange(input, ShardedHBaseQueueAdmin.PREFIX_BYTES, input.length);
    }
  };

  private final QueueName queueName;
  private final ConsumerConfig consumerConfig;
  private final ExecutorService scansExecutor;

  ShardedHBaseQueueStrategy(QueueName queueName, ConsumerConfig consumerConfig) {
    this.queueName = queueName;
    this.consumerConfig = consumerConfig;

    // Using the "direct handoff" approach, new threads will only be created
    // if it is necessary and will grow unbounded. This could be bad but in DistributedScanner
    // we only create as many Runnables as there are buckets data is distributed to. It means
    // it also scales when buckets amount changes.
    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 20,
                                                         60, TimeUnit.SECONDS,
                                                         new SynchronousQueue<Runnable>(),
                                                         Threads.newDaemonThreadFactory("queue-consumer-scan"));
    executor.allowCoreThreadTimeOut(true);
    this.scansExecutor = executor;
  }

  @Override
  public QueueScanner create(HTable hTable, Scan scan, int numRows) throws IOException {
    // Modify the scan with sharded key prefix
    Scan shardedScan = new Scan(scan);
    shardedScan.setStartRow(getShardedKey(scan.getStartRow()));
    shardedScan.setStopRow(getShardedKey(scan.getStopRow()));

    ResultScanner scanner = DistributedScanner.create(hTable, shardedScan,
                                                      HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR, scansExecutor);
    return new HBaseQueueScanner(scanner, numRows, ROW_KEY_CONVERTER);

//    return new HBaseQueueScanner(hTable.getScanner(shardedScan), numRows, ROW_KEY_CONVERTER);
  }

  private byte[] getShardedKey(byte[] originalRowKey) {
    byte[] result = new byte[ShardedHBaseQueueAdmin.PREFIX_BYTES - HBaseQueueAdmin.SALT_BYTES + originalRowKey.length];
    Bytes.putBytes(result, ShardedHBaseQueueAdmin.PREFIX_BYTES - HBaseQueueAdmin.SALT_BYTES,
                   originalRowKey, 0, originalRowKey.length);

    // Default for FIFO case.
    int instanceId = -1;
    long groupId = consumerConfig.getGroupId();

    if (consumerConfig.getDequeueStrategy() != DequeueStrategy.FIFO) {
      instanceId = consumerConfig.getInstanceId();
      groupId = consumerConfig.getGroupId();
    }

    Bytes.putLong(result, 0, groupId);
    Bytes.putInt(result, Bytes.SIZEOF_LONG, instanceId);

    return result;
  }

  @Override
  public byte[] getActualRowKey(byte[] originalRowKey) {
    return HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR.getDistributedKey(getShardedKey(originalRowKey));
  }

  @Override
  public void close() throws IOException {
    scansExecutor.shutdownNow();
  }
}
