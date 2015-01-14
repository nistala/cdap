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

package co.cask.cdap.data2.dataset2.lib.table;

import co.cask.cdap.api.annotation.Beta;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.batch.Split;
import co.cask.cdap.api.data.batch.SplitReader;
import co.cask.cdap.api.dataset.DataSetException;
import co.cask.cdap.api.dataset.lib.AbstractDataset;
import co.cask.cdap.api.dataset.table.Delete;
import co.cask.cdap.api.dataset.table.Get;
import co.cask.cdap.api.dataset.table.Increment;
import co.cask.cdap.api.dataset.table.OrderedTable;
import co.cask.cdap.api.dataset.table.Put;
import co.cask.cdap.api.dataset.table.Result;
import co.cask.cdap.api.dataset.table.Row;
import co.cask.cdap.api.dataset.table.Scanner;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.api.dataset.table.TableSplit;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import javax.annotation.Nullable;

/**
 *
 */
class TableDataset extends AbstractDataset implements Table {
  // empty immutable row's column->value map constant
  // Using ImmutableSortedMap instead of Maps.unmodifiableNavigableMap to avoid conflicts with
  // Hadoop, which uses an older version of guava without that method.
  protected static final NavigableMap<byte[], byte[]> EMPTY_ROW_MAP =
      ImmutableSortedMap.<byte[], byte[]>orderedBy(Bytes.BYTES_COMPARATOR).build();

  private static final Logger LOG = LoggerFactory.getLogger(TableDataset.class);

  private final OrderedTable table;

  TableDataset(String instanceName, OrderedTable table) {
    super(instanceName, table);
    this.table = table;
  }

  @Override
  public Row get(byte[] row, byte[][] columns) {
    try {
      return new Result(row, table.get(row, columns));
    } catch (Exception e) {
      LOG.debug("get failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("get failed", e);
    }
  }

  @Override
  public Row get(byte[] row) {
    try {
      return new Result(row, table.get(row));
    } catch (Exception e) {
      LOG.debug("get failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("get failed", e);
    }
  }

  @Override
  public byte[] get(byte[] row, byte[] column) {
    try {
      return table.get(row, column);
    } catch (Exception e) {
      LOG.debug("get failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("get failed", e);
    }
  }

  @Override
  public Row get(byte[] row, byte[] startColumn, byte[] stopColumn, int limit) {
    try {
      return new Result(row, table.get(row, startColumn, stopColumn, limit));
    } catch (Exception e) {
      LOG.debug("get failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("get failed", e);
    }
  }


  @Override
  public Row get(Get get) {
    return get.getColumns().isEmpty() ?
      get(get.getRow()) :
      get(get.getRow(), get.getColumns().toArray(new byte[get.getColumns().size()][]));
  }

  @Override
  public void put(byte[] row, byte[][] columns, byte[][] values) {
    try {
      table.put(row, columns, values);
    } catch (Exception e) {
      LOG.debug("put failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("put failed", e);
    }
  }

  @Override
  public void put(byte[] row, byte[] column, byte[] value) {
    try {
      table.put(row, column, value);
    } catch (Exception e) {
      LOG.debug("put failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("put failed", e);
    }
  }

  @Override
  public void put(Put put) {
    Preconditions.checkArgument(!put.getValues().isEmpty(), "Put must have at least one value");
    byte[][] columns = new byte[put.getValues().size()][];
    byte[][] values = new byte[put.getValues().size()][];
    int i = 0;
    for (Map.Entry<byte[], byte[]> columnValue : put.getValues().entrySet()) {
      columns[i] = columnValue.getKey();
      values[i] = columnValue.getValue();
      i++;
    }
    put(put.getRow(), columns, values);
  }

  @Override
  public void delete(byte[] row) {
    try {
      table.delete(row);
    } catch (Exception e) {
      LOG.debug("delete failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("delete failed", e);
    }
  }

  @Override
  public void delete(byte[] row, byte[] column) {
    try {
      table.delete(row, column);
    } catch (Exception e) {
      LOG.debug("delete failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("delete failed", e);
    }
  }

  @Override
  public void delete(byte[] row, byte[][] columns) {
    try {
      table.delete(row, columns);
    } catch (Exception e) {
      LOG.debug("delete failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("delete failed", e);
    }
  }

  @Override
  public void delete(Delete delete) {
    if (delete.getColumns().isEmpty()) {
      delete(delete.getRow());
    } else {
      delete(delete.getRow(), delete.getColumns().toArray(new byte[delete.getColumns().size()][]));
    }
  }

  @Override
  public long incrementAndGet(byte[] row, byte[] column, long amount) {
    try {
      return table.incrementAndGet(row, column, amount);
    } catch (NumberFormatException e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw e;
    } catch (Exception e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("increment failed", e);
    }
  }

  @Override
  public Row incrementAndGet(byte[] row, byte[][] columns, long[] amounts) {
    Map<byte[], Long> incResult;
    try {
      incResult = table.incrementAndGet(row, columns, amounts);
    } catch (NumberFormatException e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw e;
    } catch (Exception e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() + ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("increment failed", e);
    }
    // todo: define IncrementResult to make it more efficient
    return new Result(row, Maps.transformValues(incResult, new Function<Long, byte[]>() {
      @Nullable
      @Override
      public byte[] apply(@Nullable Long input) {
        return input == null ? null : Bytes.toBytes(input);
      }
    }));
  }

  @Override
  public Row incrementAndGet(Increment increment) {
    Preconditions.checkArgument(!increment.getValues().isEmpty(), "Increment must have at least one value");
    byte[][] columns = new byte[increment.getValues().size()][];
    long[] values = new long[increment.getValues().size()];
    int i = 0;
    for (Map.Entry<byte[], Long> columnValue : increment.getValues().entrySet()) {
      columns[i] = columnValue.getKey();
      values[i] = columnValue.getValue();
      i++;
    }
    return incrementAndGet(increment.getRow(), columns, values);
  }

  @Override
  public void increment(byte[] row, byte[] column, long amount) {
    try {
      table.increment(row, column, amount);
    } catch (Exception e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() +
                  ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("increment failed", e);
    }
  }

  @Override
  public void increment(byte[] row, byte[][] columns, long[] amounts) {
    try {
      table.increment(row, columns, amounts);
    } catch (Exception e) {
      LOG.debug("increment failed for table: " + getTransactionAwareName() +
                  ", row: " + Bytes.toStringBinary(row), e);
      throw new DataSetException("increment failed", e);
    }
  }

  @Override
  public void increment(Increment increment) {
    Preconditions.checkArgument(!increment.getValues().isEmpty(), "Increment must have at least one value");
    byte[][] columns = new byte[increment.getValues().size()][];
    long[] values = new long[increment.getValues().size()];
    int i = 0;
    for (Map.Entry<byte[], Long> columnValue : increment.getValues().entrySet()) {
      columns[i] = columnValue.getKey();
      values[i] = columnValue.getValue();
      i++;
    }
    increment(increment.getRow(), columns, values);
  }

  @Override
  public boolean compareAndSwap(byte[] row, byte[] column, byte[] expectedValue, byte[] newValue) {
    try {
      return table.compareAndSwap(row, column, expectedValue, newValue);
    } catch (Exception e) {
      String msg = "compareAndSwap failed for table: " + getTransactionAwareName();
      LOG.debug(msg, e);
      throw new DataSetException(msg, e);
    }
  }

  @Override
  public Scanner scan(byte[] startRow, byte[] stopRow) {
    try {
      return table.scan(startRow, stopRow);
    } catch (Exception e) {
      LOG.debug("scan failed for table: " + getTransactionAwareName(), e);
      throw new DataSetException("scan failed", e);
    }
  }

  @Override
  public void write(byte[] key, Put put) {
    put(put);
  }

  /**
   * Returns splits for a range of keys in the table.
   *
   * @param numSplits Desired number of splits. If greater than zero, at most this many splits will be returned.
   *                  If less or equal to zero, any number of splits can be returned.
   * @param start If non-null, the returned splits will only cover keys that are greater or equal.
   * @param stop If non-null, the returned splits will only cover keys that are less.
   * @return list of {@link Split}
   */
  @Beta
  @Override
  public List<Split> getSplits(int numSplits, byte[] start, byte[] stop) {
    try {
      return table.getSplits(numSplits, start, stop);
    } catch (Exception e) {
      LOG.error("getSplits failed for table: " + getTransactionAwareName(), e);
      throw new DataSetException("getSplits failed", e);
    }
  }

  @Override
  public List<Split> getSplits() {
    return getSplits(-1, null, null);
  }

  @Override
  public SplitReader<byte[], Row> createSplitReader(Split split) {
    return new TableScanner();
  }

  /**
   * Implements a split reader for a key range of a table, based on the Scanner implementation of the underlying
   * table implementation.
   */
  public class TableScanner extends SplitReader<byte[], Row> {

    // the underlying scanner
    private Scanner scanner;
    // the current key
    private byte[] key = null;
    // the current row, that is, a map from column key to value
    private Map<byte[], byte[]> row = null;

    @Override
    public void initialize(Split split) throws InterruptedException {
      TableSplit tableSplit = (TableSplit) split;
      try {
        this.scanner = table.scan(tableSplit.getStart(), tableSplit.getStop());
      } catch (Exception e) {
        LOG.debug("scan failed for table: " + getTransactionAwareName(), e);
        throw new DataSetException("scan failed", e);
      }
    }

    @Override
    public boolean nextKeyValue() throws InterruptedException {
      // call the underlying scanner, and depending on whether there it returns something, set current key and row.
      Row next = this.scanner.next();
      if (next == null) {
        this.key = null;
        this.row = null;
        return false;
      } else {
        this.key = next.getRow();
        this.row = next.getColumns();
        return true;
      }
    }

    @Override
    public byte[] getCurrentKey() throws InterruptedException {
      return this.key;
    }

    @Override
    public Row getCurrentValue() throws InterruptedException {
      return new Result(this.key, this.row);
    }

    @Override
    public void close() {
      this.scanner.close();
    }
  }
}
