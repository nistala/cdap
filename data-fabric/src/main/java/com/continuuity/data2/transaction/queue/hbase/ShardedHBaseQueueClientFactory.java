package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.QueueClientFactory;
import com.continuuity.data2.queue.QueueConsumer;
import com.continuuity.data2.queue.QueueProducer;
import com.continuuity.data2.transaction.queue.QueueMetrics;
import com.google.common.base.Function;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;

import java.io.IOException;

/**
 * A {@link QueueClientFactory} that creates {@link QueueProducer} and {@link QueueConsumer} with sharded queue logic.
 */
public final class ShardedHBaseQueueClientFactory implements QueueClientFactory {

  private static final int DEFAULT_WRITE_BUFFER_SIZE = 4 * 1024 * 1024;

  private final Configuration hConf;
  private final ShardedHBaseQueueAdmin queueAdmin;
  private final HBaseQueueUtil queueUtil;
  private final Function<QueueName, Iterable<ConsumerConfig>> consumerConfigs;

  @Inject
  public ShardedHBaseQueueClientFactory(Configuration hConf,
                                        ShardedHBaseQueueAdmin queueAdmin,
                                        HBaseQueueUtil queueUtil,
                                        Function<QueueName, Iterable<ConsumerConfig>> consumerConfigs) {
    this.hConf = hConf;
    this.queueAdmin = queueAdmin;
    this.queueUtil = queueUtil;
    this.consumerConfigs = consumerConfigs;
  }

  @Override
  public QueueProducer createProducer(QueueName queueName) throws IOException {
    return createProducer(queueName, QueueMetrics.NOOP_QUEUE_METRICS);
  }

  @Override
  public QueueProducer createProducer(QueueName queueName, QueueMetrics queueMetrics) throws IOException {
    ensureTableExists(queueName);
    return new ShardedHBaseQueueProducer(createHTable(queueAdmin.getActualTableName(queueName)),
                                         queueName, queueMetrics, consumerConfigs.apply(queueName));
  }

  @Override
  public QueueConsumer createConsumer(QueueName queueName,
                                      ConsumerConfig consumerConfig, int numGroups) throws IOException {
    ensureTableExists(queueName);
    HBaseConsumerStateStore stateStore = new HBaseConsumerStateStore(queueName, consumerConfig,
                                                                     createHTable(queueAdmin.getConfigTableName()));
    return queueUtil.getQueueConsumer(consumerConfig, createHTable(queueAdmin.getActualTableName(queueName)),
                                      queueName, stateStore.getState(), stateStore,
                                      new ShardedHBaseQueueStrategy(consumerConfig));
  }

  private void ensureTableExists(QueueName queueName) throws IOException {
    if (!queueAdmin.exists(queueName)) {
      queueAdmin.create(queueName);
    }
  }

  private HTable createHTable(String name) throws IOException {
    HTable queueTable = new HTable(hConf, name);
    // TODO: make configurable
    queueTable.setWriteBufferSize(DEFAULT_WRITE_BUFFER_SIZE);
    queueTable.setAutoFlush(false);
    return queueTable;
  }
}
