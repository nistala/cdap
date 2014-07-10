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
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
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
 * shard = <salt> <consumer_group_id> <consumer_instance_id>
 * salt = First byte of MD5 of <consumer_group_id>, <consumer_instance_id> and <queue_name>
 * consumer_group_id = 8 bytes long value of the target consumer group or 0 if it is FIFO
 * consumer_instance_id = 4 bytes int value of target consumer instance id or -1 if FIFO
 * queue_prefix = <name_hash> <queue_name>
 * name_hash = First byte of MD5 of <queue_name>
 * queue_name = flowlet_name + "/" + output_name
 * write_pointer = 8 bytes long value of the write pointer of the transaction
 * counter = 4 bytes int value of a monotonic increasing number assigned for each entry written in the same transaction
 * }
 * </pre>
 */
public class ShardedHBaseQueueProducer extends HBaseQueueProducer {

  private static final HashFunction HASH_FUNCTION = Hashing.murmur3_32();

  private final QueueName queueName;
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

    this.queueName = queueName;

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

      // Default for FIFO
      int instanceId = -1;

      if (dequeueStrategy != DequeueStrategy.FIFO) {
        // TODO: Add metrics
        if (dequeueStrategy == DequeueStrategy.ROUND_ROBIN) {
          instanceId = QueueEntryRow.getRoundRobinConsumerInstance(writePointer, counter, config.getGroupSize());
        } else if (dequeueStrategy == DequeueStrategy.HASH) {
          instanceId = QueueEntryRow.getHashConsumerInstance(queueEntry.getHashKeys(),
                                                             config.getHashKey(), config.getGroupSize());
        } else {
          throw new IllegalArgumentException("Unsupported consumer strategy: " + dequeueStrategy);
        }
      }

      byte[] rowKey = new byte[ShardedHBaseQueueAdmin.PREFIX_BYTES - HBaseQueueAdmin.SALT_BYTES + rowKeyBase.length];

      Bytes.putLong(rowKey, 0, config.getGroupId());
      Bytes.putInt(rowKey, Bytes.SIZEOF_LONG, instanceId);
      Bytes.putBytes(rowKey, rowKey.length - rowKeyBase.length, rowKeyBase, 0, rowKeyBase.length);
      rowKeys.add(HBaseQueueAdmin.ROW_KEY_DISTRIBUTOR.getDistributedKey(rowKey));
    }
  }
}
