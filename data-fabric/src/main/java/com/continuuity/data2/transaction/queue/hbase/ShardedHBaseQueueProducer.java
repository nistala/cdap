package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.queue.QueueName;
import com.continuuity.data2.queue.ConsumerConfig;
import com.continuuity.data2.queue.DequeueStrategy;
import com.continuuity.data2.queue.QueueEntry;
import com.continuuity.data2.transaction.queue.QueueEntryRow;
import com.continuuity.data2.transaction.queue.QueueMetrics;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.hadoop.hbase.client.HTable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Implements sharded queue on HBase. The row key has structure of:
 *
 * <pre>
 * {@code
 *
 * row_key = <shard> <queue_prefix> <write_pointer> <counter>
 * shard = <consumer_instance_id> <consumer_group_id>
 * consumer_instance_id = 4 bytes int value of target consumer instance id or -1 if FIFO.
 * consumer_group_id = 8 bytes long value of the target consumer group or 0 if it is FIFO
 * queue_prefix = <name_hash> <queue_name>
 * name_hash = First byte of MD5 of <queue_name>
 * queue_name = flowlet_name + "/" + output_name
 * write_pointer = 8 bytes long value of the write pointer of the transaction
 * counter = 4 bytes int value of a monotonic increasing number assigned for each entry written in the same transaction
 * }
 * </pre>
 */
public class ShardedHBaseQueueProducer extends HBaseQueueProducer {

  private static final byte[] FIFO_PREFIX = Bytes.add(Bytes.toBytes(-1), Bytes.toBytes(0L));

  private final List<ConsumerConfig> consumerConfigs;

  /**
   * Constructs a new instance.
   *
   * @param hTable The HTable for accessing HBase
   * @param queueName Name of the queue
   * @param queueMetrics For logging queue related metrics
   * @param consumerConfigs Sets of configurations for consumers that consume entries produced by this producer.
   */
  public ShardedHBaseQueueProducer(HTable hTable, QueueName queueName,
                                   QueueMetrics queueMetrics, Iterable<ConsumerConfig> consumerConfigs) {
    super(hTable, queueName, queueMetrics);

    // Only interested in groups, size, strategy and hash key.
    this.consumerConfigs = ImmutableList.copyOf(Iterables.filter(consumerConfigs, new Predicate<ConsumerConfig>() {

      private final Set<Long> seenGroups = Sets.newHashSet();

      @Override
      public boolean apply(ConsumerConfig input) {
        return seenGroups.add(input.getGroupId());
      }
    }));
  }

  @Override
  protected void getRowKeys(QueueEntry queueEntry, byte[] rowKeyBase,
                            long writePointer, int counter, Collection<byte[]> rowKeys) {

    // Generates all row keys, one per consumer group.
    for (ConsumerConfig config : consumerConfigs) {
      DequeueStrategy dequeueStrategy = config.getDequeueStrategy();

      // For FIFO, the prefix is fixed.
      if (dequeueStrategy == DequeueStrategy.FIFO) {
        rowKeys.add(Bytes.add(FIFO_PREFIX, rowKeyBase));
      } else {
        // TODO: Add metrics
        int instanceId;
        if (dequeueStrategy == DequeueStrategy.ROUND_ROBIN) {
          instanceId = QueueEntryRow.getRoundRobinConsumerInstance(writePointer, counter, config.getGroupSize());
        } else if (dequeueStrategy == DequeueStrategy.HASH) {
          instanceId = QueueEntryRow.getHashConsumerInstance(queueEntry.getHashKeys(),
                                                             config.getHashKey(), config.getGroupSize());
        } else {
          throw new IllegalArgumentException("Unsupported consumer strategy: " + dequeueStrategy);
        }

        byte[] rowKey = new byte[Bytes.SIZEOF_INT + Bytes.SIZEOF_LONG + rowKeyBase.length];

        Bytes.putInt(rowKey, 0, instanceId);
        Bytes.putLong(rowKey, Bytes.SIZEOF_INT, config.getGroupId());
        Bytes.putBytes(rowKey, rowKey.length - rowKeyBase.length, rowKeyBase, 0, rowKeyBase.length);
        rowKeys.add(rowKey);
      }
    }
  }
}
