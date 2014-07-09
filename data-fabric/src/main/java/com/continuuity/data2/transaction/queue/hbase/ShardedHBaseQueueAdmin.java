package com.continuuity.data2.transaction.queue.hbase;

import com.continuuity.api.common.Bytes;
import com.continuuity.common.conf.CConfiguration;
import com.continuuity.data.DataSetAccessor;
import com.continuuity.data2.transaction.queue.QueueConstants;
import com.continuuity.data2.util.hbase.HBaseTableUtil;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.twill.filesystem.LocationFactory;

import java.io.IOException;

/**
 * Admin for sharded HBase queue.
 */
public final class ShardedHBaseQueueAdmin extends HBaseQueueAdmin {

  @Inject
  public ShardedHBaseQueueAdmin(Configuration hConf, CConfiguration cConf, DataSetAccessor dataSetAccessor,
                                LocationFactory locationFactory, HBaseTableUtil tableUtil) throws IOException {
    super(hConf, cConf, dataSetAccessor, locationFactory, tableUtil, QueueConstants.QueueType.SHARDED_QUEUE);
  }

  @Override
  protected void createQueueTable(HTableDescriptor htd, byte[][] splitKeys) throws IOException {
    htd.setValue(HBaseQueueAdmin.PROPERTY_PREFIX_BYTES, Integer.toString(Bytes.SIZEOF_LONG + Bytes.SIZEOF_INT));
    super.createQueueTable(htd, splitKeys);
  }
}
