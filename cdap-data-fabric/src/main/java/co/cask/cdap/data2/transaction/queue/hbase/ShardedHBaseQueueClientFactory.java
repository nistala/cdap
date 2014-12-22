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

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.queue.QueueName;
import co.cask.cdap.data2.queue.ConsumerConfig;
import co.cask.cdap.data2.queue.QueueClientFactory;
import co.cask.cdap.data2.queue.QueueConsumer;
import co.cask.cdap.data2.queue.QueueProducer;
import co.cask.cdap.data2.transaction.queue.QueueAdmin;
import co.cask.cdap.data2.transaction.queue.QueueMetrics;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;

import java.io.IOException;

/**
 * A {@link QueueClientFactory} that creates {@link QueueProducer} and {@link QueueConsumer} with sharded queue logic.
 */
public final class ShardedHBaseQueueClientFactory implements QueueClientFactory, ShardedQueueProducerFactory {

  private static final int DEFAULT_WRITE_BUFFER_SIZE = 4 * 1024 * 1024;

  private final CConfiguration cConf;
  private final Configuration hConf;
  private final ShardedHBaseQueueAdmin queueAdmin;
  private final HBaseQueueUtil queueUtil;

  @Inject
  public ShardedHBaseQueueClientFactory(CConfiguration cConf, Configuration hConf, QueueAdmin queueAdmin) {
    this.cConf = cConf;
    this.hConf = hConf;
    this.queueAdmin = (ShardedHBaseQueueAdmin) queueAdmin;
    this.queueUtil = new HBaseQueueUtilFactory().get();
  }

  @Override
  public QueueProducer createProducer(QueueName queueName) throws IOException {
    return createProducer(queueName, QueueMetrics.NOOP_QUEUE_METRICS);
  }

  @Override
  public QueueProducer createProducer(QueueName queueName, QueueMetrics queueMetrics) throws IOException {
    throw new UnsupportedOperationException("Non-sharded producer not support. Use " +
                                            "createProducer(QueueName, QueueMetrics, Iterable<ConsumerConfig>) " +
                                            "instead.");
  }

  @Override
  public QueueProducer createProducer(QueueName queueName, QueueMetrics queueMetrics,
                                      Iterable<ConsumerConfig> consumerConfigs) throws IOException {
    ensureTableExists(queueName);
    return new ShardedHBaseQueueProducer(createHTable(queueAdmin.getActualTableName(queueName)),
                                         queueName, queueMetrics, consumerConfigs);
  }

  @Override
  public QueueConsumer createConsumer(QueueName queueName,
                                      ConsumerConfig consumerConfig, int numGroups) throws IOException {
    ensureTableExists(queueName);
    HBaseConsumerStateStore stateStore = new HBaseConsumerStateStore(queueName, consumerConfig,
                                                                     createHTable(queueAdmin.getConfigTableName()));
    return queueUtil.getQueueConsumer(cConf, consumerConfig, createHTable(queueAdmin.getActualTableName(queueName)),
                                      queueName, stateStore.getState(), stateStore,
                                      new ShardedHBaseQueueStrategy(queueName, consumerConfig));
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
