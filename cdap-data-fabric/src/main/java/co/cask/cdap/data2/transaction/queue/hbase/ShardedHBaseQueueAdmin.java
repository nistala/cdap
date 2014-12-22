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

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.data2.transaction.queue.QueueConstants;
import co.cask.cdap.data2.util.hbase.HBaseTableUtil;
import com.google.inject.Inject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.twill.filesystem.LocationFactory;

import java.io.IOException;

/**
 * Admin for sharded HBase queue.
 */
public final class ShardedHBaseQueueAdmin extends HBaseQueueAdmin {

  // Number of bytes as the row key prefix. See ShardedHbaseQueueProducer for the schema.
  public static final int PREFIX_BYTES = HBaseQueueAdmin.SALT_BYTES + Bytes.SIZEOF_LONG + Bytes.SIZEOF_INT;

  @Inject
  public ShardedHBaseQueueAdmin(Configuration hConf, CConfiguration cConf,
                                LocationFactory locationFactory, HBaseTableUtil tableUtil) throws IOException {
    super(hConf, cConf, locationFactory, tableUtil, QueueConstants.QueueType.SHARDED_QUEUE);
  }

  @Override
  protected void createQueueTable(HTableDescriptor htd, byte[][] splitKeys) throws IOException {
    htd.setValue(HBaseQueueAdmin.PROPERTY_PREFIX_BYTES, Integer.toString(PREFIX_BYTES));
    super.createQueueTable(htd, splitKeys);
  }
}
