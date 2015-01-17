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

package co.cask.cdap.data2.datafabric.dataset.service.mds;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.DatasetSpecification;
import co.cask.cdap.api.dataset.module.EmbeddedDataset;
import co.cask.cdap.api.dataset.table.Table;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Dataset instances metadata store
 */
public final class DatasetInstanceMDS extends AbstractObjectsStore {
  /**
   * Prefix for rows containing instance info.
   * NOTE: even though we don't have to have it now we may want to store different type of data in one table, so
   *       the prefix may help us in future
   */
  private static final byte[] INSTANCE_PREFIX = Bytes.toBytes("i_");

  public DatasetInstanceMDS(DatasetSpecification spec, @EmbeddedDataset("") Table table) {
    super(spec, table);
  }

  @Nullable
  public DatasetSpecification get(String name) {
    return get(getInstanceKey(name), DatasetSpecification.class);
  }

  public void write(DatasetSpecification instanceSpec) {
    put(getInstanceKey(instanceSpec.getName()), instanceSpec);
  }

  public boolean delete(String name) {
    if (get(name) == null) {
      return false;
    }
    delete(getInstanceKey(name));
    return true;
  }

  public Collection<DatasetSpecification> getAll() {
    Map<String, DatasetSpecification> instances = scan(INSTANCE_PREFIX, DatasetSpecification.class);
    return instances.values();
  }

  public Collection<DatasetSpecification> getByTypes(Set<String> typeNames) {
    List<DatasetSpecification> filtered = Lists.newArrayList();

    for (DatasetSpecification spec : getAll()) {
      if (typeNames.contains(spec.getType())) {
        filtered.add(spec);
      }
    }

    return filtered;
  }

  public void deleteAll() {
    deleteAll(INSTANCE_PREFIX);
  }

  private byte[] getInstanceKey(String name) {
    return Bytes.add(INSTANCE_PREFIX, Bytes.toBytes(name));
  }
}
