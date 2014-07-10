package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.QueueProducer;
import com.continuuity.data2.transaction.queue.QueueMetrics;

import java.io.IOException;

/**
 * Interface for creating sharded queue producer.
 *
 * TODO: It is temporary for sharded queue testing.
 */
public interface ShardedQueueProducerFactory {

  QueueProducer createProducer(QueueName queueName, QueueMetrics queueMetrics,
                               Iterable<ConsumerConfig> consumerConfigs) throws IOException;
}
