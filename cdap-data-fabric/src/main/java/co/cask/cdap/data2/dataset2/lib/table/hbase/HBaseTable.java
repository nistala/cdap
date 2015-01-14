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

package co.cask.cdap.data2.dataset2.lib.table.hbase;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.table.ConflictDetection;
import co.cask.cdap.api.dataset.table.Scanner;
import co.cask.cdap.data2.dataset2.lib.table.ordered.BufferingTable;
import co.cask.cdap.data2.dataset2.lib.table.ordered.IncrementValue;
import co.cask.cdap.data2.dataset2.lib.table.ordered.PutValue;
import co.cask.cdap.data2.dataset2.lib.table.ordered.Update;
import co.cask.cdap.data2.util.hbase.HBaseTableUtil;
import co.cask.tephra.Transaction;
import co.cask.tephra.TransactionCodec;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import javax.annotation.Nullable;

/**
 * Dataset client for HBase tables.
 */
// todo: do periodic flush when certain threshold is reached
// todo: extract separate "no delete inside tx" table?
// todo: consider writing & reading using HTable to do in multi-threaded way
public class HBaseTable extends BufferingTable {
  private static final Logger LOG = LoggerFactory.getLogger(HBaseTable.class);

  public static final String DELTA_WRITE = "d";
  private final HTable hTable;
  private final String hTableName;

  private Transaction tx;

  private final TransactionCodec txCodec;

  public HBaseTable(String name, ConflictDetection level, Configuration hConf, boolean enableReadlessIncrements)
    throws IOException {
    super(name, level, enableReadlessIncrements);

    hTableName = HBaseTableUtil.getHBaseTableName(name);
    HTable hTable = new HTable(hConf, hTableName);
    // todo: make configurable
    hTable.setWriteBufferSize(HBaseTableUtil.DEFAULT_WRITE_BUFFER_SIZE);
    hTable.setAutoFlush(false);
    this.hTable = hTable;
    this.txCodec = new TransactionCodec();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
                  .add("hTable", hTable)
                  .add("hTableName", hTableName)
                  .toString();
  }

  @Override
  public void startTx(Transaction tx) {
    super.startTx(tx);
    this.tx = tx;
  }

  @Override
  protected void persist(NavigableMap<byte[], NavigableMap<byte[], Update>> buff) throws Exception {
    List<Put> puts = Lists.newArrayList();
    for (Map.Entry<byte[], NavigableMap<byte[], Update>> row : buff.entrySet()) {
      Put put = new Put(row.getKey());
      Put incrementPut = null;
      for (Map.Entry<byte[], Update> column : row.getValue().entrySet()) {
        // we want support tx and non-tx modes
        if (tx != null) {
          // TODO: hijacking timestamp... bad
          Update val = column.getValue();
          if (val instanceof IncrementValue) {
            incrementPut = getIncrementalPut(incrementPut, row.getKey());
            incrementPut.add(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey(), tx.getWritePointer(),
                             Bytes.toBytes(((IncrementValue) val).getValue()));
          } else if (val instanceof PutValue) {
            put.add(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey(), tx.getWritePointer(),
                    wrapDeleteIfNeeded(((PutValue) val).getValue()));
          }
        } else {
          Update val = column.getValue();
          if (val instanceof IncrementValue) {
            incrementPut = getIncrementalPut(incrementPut, row.getKey());
            incrementPut.add(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey(),
                             Bytes.toBytes(((IncrementValue) val).getValue()));
          } else if (val instanceof PutValue) {
            put.add(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey(), ((PutValue) val).getValue());
          }
        }
      }
      if (incrementPut != null) {
        puts.add(incrementPut);
      }
      if (!put.isEmpty()) {
        puts.add(put);
      }
    }
    if (!puts.isEmpty()) {
      hTable.put(puts);
      hTable.flushCommits();
    } else {
      LOG.info("No writes to persist!");
    }
  }

  private Put getIncrementalPut(Put existing, byte[] row) {
    if (existing != null) {
      return existing;
    }
    Put put = new Put(row);
    put.setAttribute(DELTA_WRITE, Bytes.toBytes(true));
    return put;
  }

  @Override
  protected void undo(NavigableMap<byte[], NavigableMap<byte[], Update>> persisted) throws Exception {
    // NOTE: we use Delete with the write pointer as the specific version to delete.
    List<Delete> deletes = Lists.newArrayList();
    for (Map.Entry<byte[], NavigableMap<byte[], Update>> row : persisted.entrySet()) {
      Delete delete = new Delete(row.getKey());
      for (Map.Entry<byte[], Update> column : row.getValue().entrySet()) {
        // we want support tx and non-tx modes
        if (tx != null) {
          // TODO: hijacking timestamp... bad
          delete.deleteColumn(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey(), tx.getWritePointer());
        } else {
          delete.deleteColumn(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column.getKey());
        }
      }
      deletes.add(delete);
    }
    hTable.delete(deletes);
    hTable.flushCommits();
  }

  @Override
  protected NavigableMap<byte[], byte[]> getPersisted(byte[] row, byte[] startColumn, byte[] stopColumn, int limit)
    throws Exception {

    // todo: this is very inefficient: column range + limit should be pushed down via server-side filters
    return getRange(getInternal(row, null), startColumn, stopColumn, limit);
  }

  @Override
  protected NavigableMap<byte[], byte[]> getPersisted(byte[] row, @Nullable byte[][] columns) throws Exception {
    return getInternal(row, columns);
  }

  @Override
  protected Scanner scanPersisted(byte[] startRow, byte[] stopRow) throws Exception {
    Scan scan = new Scan();
    scan.addFamily(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY);
    // todo: should be configurable
    // NOTE: by default we assume scanner is used in mapreduce job, hence no cache blocks
    scan.setCacheBlocks(false);
    scan.setCaching(1000);

    if (startRow != null) {
      scan.setStartRow(startRow);
    }
    if (stopRow != null) {
      scan.setStopRow(stopRow);
    }

    txCodec.addToOperation(scan, tx);

    ResultScanner resultScanner = hTable.getScanner(scan);
    return new HBaseScanner(resultScanner);
  }

  private NavigableMap<byte[], byte[]> getInternal(byte[] row, @Nullable byte[][] columns) throws IOException {
    Get get = new Get(row);
    // todo: uncomment when doing caching fetching data in-memory
    // get.setCacheBlocks(false);
    get.addFamily(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY);
    if (columns != null) {
      for (byte[] column : columns) {
        get.addColumn(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY, column);
      }
    }

    // no tx logic needed
    if (tx == null) {
      get.setMaxVersions(1);
      Result result = hTable.get(get);
      return result.isEmpty() ? EMPTY_ROW_MAP : result.getFamilyMap(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY);
    }

    txCodec.addToOperation(get, tx);

    Result result = hTable.get(get);
    return getRowMap(result);
  }

  static NavigableMap<byte[], byte[]> getRowMap(Result result) {
    if (result.isEmpty()) {
      return EMPTY_ROW_MAP;
    }

    // note: server-side filters all everything apart latest visible for us, so we can flatten it here
    NavigableMap<byte[], NavigableMap<Long, byte[]>> versioned =
      result.getMap().get(HBaseOrderedTableAdmin.DATA_COLUMN_FAMILY);

    NavigableMap<byte[], byte[]> rowMap = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
    for (Map.Entry<byte[], NavigableMap<Long, byte[]>> column : versioned.entrySet()) {
      rowMap.put(column.getKey(), column.getValue().firstEntry().getValue());
    }

    return unwrapDeletes(rowMap);
  }
}
