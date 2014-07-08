package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.data2.transaction.queue.QueueScanner;
import com.continuuity.hbase.wd.DistributedScanner;
import com.google.common.base.Function;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Threads;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A {@link HBaseQueueStrategy} that scans HBase by using the {@link DistributedScanner}.
 */
final class SaltedHBaseQueueStrategy implements HBaseQueueStrategy, Closeable {

  private static final Function<byte[], byte[]> ROW_KEY_CONVERTER = new Function<byte[], byte[]>() {
    @Override
    public byte[] apply(byte[] input) {
      return HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR.getOriginalKey(input);
    }
  };

  private final ExecutorService scansExecutor;

  SaltedHBaseQueueStrategy() {
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
    ResultScanner scanner = DistributedScanner.create(hTable, scan, HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR, scansExecutor);
    return new HBaseQueueScanner(scanner, numRows, ROW_KEY_CONVERTER);
  }

  @Override
  public byte[] getActualRowKey(byte[] originalRowKey) {
    return HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR.getDistributedKey(originalRowKey);
  }

  @Override
  public void close() throws IOException {
    scansExecutor.shutdownNow();
  }
}
