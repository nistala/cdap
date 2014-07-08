package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.common.utils.ImmutablePair;
import com.continuuity.data2.transaction.queue.QueueEntryRow;
import com.continuuity.data2.transaction.queue.QueueScanner;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

/**
 * An adapter class to convert HBase {@link ResultScanner} into {@link QueueScanner}.
 */
final class HBaseQueueScanner implements QueueScanner {
  private final ResultScanner scanner;
  private final LinkedList<Result> cached = Lists.newLinkedList();
  private final int numRows;
  private final Function<byte[], byte[]> rowKeyConverter;

  public HBaseQueueScanner(ResultScanner scanner, int numRows, Function<byte[], byte[]> rowKeyConverter) {
    this.scanner = scanner;
    this.numRows = numRows;
    this.rowKeyConverter = rowKeyConverter;
  }

  @Override
  public ImmutablePair<byte[], Map<byte[], byte[]>> next() throws IOException {
    while (true) {
      if (cached.size() > 0) {
        Result result = cached.removeFirst();
        Map<byte[], byte[]> row = result.getFamilyMap(QueueEntryRow.COLUMN_FAMILY);
        return ImmutablePair.of(rowKeyConverter.apply(result.getRow()), row);
      }
      Result[] results = scanner.next(numRows);
      if (results.length == 0) {
        return null;
      }
      Collections.addAll(cached, results);
    }
  }

  @Override
  public void close() throws IOException {
    scanner.close();
  }
}
