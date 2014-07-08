/*
 * Copyright 2012-2013 Continuuity,Inc. All Rights Reserved.
 */
package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.transaction.queue.AbstractQueueConsumer;
import com.continuuity.data2.transaction.queue.QueueEntryRow;
import com.continuuity.data2.transaction.queue.QueueScanner;
import com.google.common.collect.Lists;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Queue consumer for HBase.
 */
abstract class HBaseQueueConsumer extends AbstractQueueConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(HBaseQueueConsumer.class);

  // Persist latest start row every n entries consumed.
  // The smaller this number, the more frequent latest startRow is persisted, which makes more
  // entries could be evicted since startRow is used by the eviction logic to determine what can be evicted.
  // The down side of decreasing this value is more overhead on each postCommit call for writing to HBase.
  private static final int PERSIST_START_ROW_LIMIT = 1000;

  private final HTable hTable;
  private final HBaseConsumerStateStore stateStore;
  private final byte[] queueRowPrefix;
  private final HBaseQueueStrategy queueStrategy;
  private boolean closed;

  /**
   * Creates a HBaseQueue2Consumer.
   * @param consumerConfig Configuration of the consumer.
   * @param hTable The HTable instance to use for communicating with HBase. This consumer is responsible for closing it.
   * @param queueName Name of the queue.
   * @param consumerState The persisted state of this consumer.
   * @param stateStore The store for persisting state for this consumer.
   */
  HBaseQueueConsumer(ConsumerConfig consumerConfig, HTable hTable, QueueName queueName,
                     HBaseConsumerState consumerState, HBaseConsumerStateStore stateStore,
                     HBaseQueueStrategy queueStrategy) {
    // For HBase, eviction is done at table flush time, hence no QueueEvictor is needed.
    super(consumerConfig, queueName);
    this.hTable = hTable;
    this.stateStore = stateStore;
    this.queueRowPrefix = QueueEntryRow.getQueueRowPrefix(queueName);
    this.queueStrategy = queueStrategy;

    byte[] startRow = consumerState.getStartRow();

    if (startRow != null && startRow.length > 0) {
      this.startRow = startRow;
    }
  }

  @Override
  protected boolean claimEntry(byte[] rowKey, byte[] claimedStateValue) throws IOException {
    Put put = new Put(queueStrategy.getActualRowKey(rowKey));
    put.add(QueueEntryRow.COLUMN_FAMILY, stateColumnName, claimedStateValue);
    return hTable.checkAndPut(put.getRow(), QueueEntryRow.COLUMN_FAMILY,
                              stateColumnName, null, put);
  }

  @Override
  protected void updateState(Set<byte[]> rowKeys, byte[] stateColumnName, byte[] stateContent) throws IOException {
    if (rowKeys.isEmpty()) {
      return;
    }
    List<Put> puts = Lists.newArrayListWithCapacity(rowKeys.size());
    for (byte[] rowKey : rowKeys) {
      Put put = new Put(queueStrategy.getActualRowKey(rowKey));
      put.add(QueueEntryRow.COLUMN_FAMILY, stateColumnName, stateContent);
      puts.add(put);
    }
    hTable.put(puts);
    hTable.flushCommits();
  }

  @Override
  protected void undoState(Set<byte[]> rowKeys, byte[] stateColumnName) throws IOException, InterruptedException {
    if (rowKeys.isEmpty()) {
      return;
    }
    List<Row> ops = Lists.newArrayListWithCapacity(rowKeys.size());
    for (byte[] rowKey : rowKeys) {
      Delete delete = new Delete(queueStrategy.getActualRowKey(rowKey));
      delete.deleteColumn(QueueEntryRow.COLUMN_FAMILY, stateColumnName);
      ops.add(delete);
    }
    hTable.batch(ops);
    hTable.flushCommits();
  }

  @Override
  protected QueueScanner getScanner(byte[] startRow, byte[] stopRow, int numRows) throws IOException {
    // Scan the table for queue entries.
    Scan scan = createScan(startRow, stopRow, numRows);

    /** TODO: Remove when {@link DequeueScanAttributes#ATTR_QUEUE_ROW_PREFIX} is removed. It is for transition. **/
    DequeueScanAttributes.setQueueRowPrefix(scan, queueRowPrefix);
    DequeueScanAttributes.set(scan, getConfig());
    DequeueScanAttributes.set(scan, transaction);

    return queueStrategy.create(hTable, scan, numRows);
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    try {
      stateStore.saveState(new HBaseConsumerState(startRow, getConfig().getGroupId(), getConfig().getInstanceId()));
    } finally {
      if (queueStrategy instanceof Closeable) {
        ((Closeable) queueStrategy).close();
      }
      hTable.close();
      closed = true;
    }
  }

  @Override
  public void postTxCommit() {
    super.postTxCommit();
    if (commitCount >= PERSIST_START_ROW_LIMIT) {
      try {
        stateStore.saveState(new HBaseConsumerState(startRow, getConfig().getGroupId(), getConfig().getInstanceId()));
        commitCount = 0;
      } catch (IOException e) {
        LOG.error("Failed to persist start row to HBase.", e);
      }
    }
  }

  protected abstract Scan createScan(byte[] startRow, byte[] stopRow, int numRows);

}
