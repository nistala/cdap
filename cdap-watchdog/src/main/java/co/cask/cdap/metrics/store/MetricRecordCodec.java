/*
 * Copyright 2015 Cask Data, Inc.
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

package co.cask.cdap.metrics.store;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.metrics.data.EntityTable;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class MetricRecordCodec {
  // encoding types
  private static final String TYPE_METRIC_NAME = "metricName";
  private static final String TYPE_AGG_GROUP = "aggGroup";

  private final EntityTable entityTable;

  private final int resolution;
  private final int rollTimebaseInterval;
  // Cache for delta values.
  private final byte[][] deltaCache;

  public MetricRecordCodec(EntityTable entityTable, int resolution, int rollTimebaseInterval) {
    this.entityTable = entityTable;
    this.resolution = resolution;
    this.rollTimebaseInterval = rollTimebaseInterval;
    this.deltaCache = createDeltaCache(rollTimebaseInterval);
  }

  public byte[] createRowKey(List<TagValue> tagValues, String metricName, long ts) {
    // Row key format: <encoded agg group><time base><encoded tag1 value>...<encoded tagN value><encoded metric name>.
    // todo: reserve first byte for versioning and other things for future
    // "+2" is for <encoded agg group> and <encoded metric name>
    byte[] rowKey = new byte[(tagValues.size() + 2) * entityTable.getIdSize() + Bytes.SIZEOF_INT];

    int offset = writeEncodedAggGroup(tagValues, rowKey, 0);

    long startTs = ts / resolution * resolution;
    int timeBase = getTimeBase(startTs);
    offset = Bytes.putInt(rowKey, offset, timeBase);

    for (TagValue tagValue : tagValues) {
      if (tagValue.getValue() != null) {
        // encoded value is unique within values of the tag name
        offset = writeEncoded(tagValue.getTagName(), tagValue.getValue(), rowKey, offset);
      } else {
        // todo: this is only applicable for constructing scan, throw smth if constructing key for writing data
        // writing "ANY" as a value
        offset = writeAnyEncoded(rowKey, offset);
      }
    }

    if (metricName != null) {
      writeEncoded(TYPE_METRIC_NAME, metricName, rowKey, offset);
    } else {
      // todo: this is only applicable for constructing scan, throw smth if constructing key for writing data
      // writing "ANY" value
      writeAnyEncoded(rowKey, offset);
    }
    return rowKey;
  }

  public byte[] createColumn(long ts) {
    long timestamp = ts / resolution * resolution;
    int timeBase = getTimeBase(timestamp);

    return deltaCache[(int) ((ts - timeBase) / resolution)];
  }

  public String getMetricName(byte[] rowKey) {
    // last encoded is metric name
    long encoded = readEncoded(rowKey, rowKey.length - entityTable.getIdSize());
    return entityTable.getName(encoded, TYPE_METRIC_NAME);
  }

  public List<TagValue> getTagValues(byte[] rowKey) {
    // todo: in some cases, the client knows the agg group - so to optimize we can accept is as a parameter
    // first encoded is aggregation group
    long encodedAggGroup = readEncoded(rowKey, 0);
    String aggGroup = entityTable.getName(encodedAggGroup, TYPE_AGG_GROUP);
    if (aggGroup.isEmpty()) {
      return Collections.emptyList();
    }

    // aggregation group is defined by list of tag names concatenated with "." (see writeEncodedAggGroup for details)
    String[] tagNames = aggGroup.split("\\.");

    // todo: assert count of tag values is same as tag names?
    List<TagValue> tags = Lists.newArrayListWithCapacity(tagNames.length);
    for (int i = 0; i < tagNames.length; i++) {
      // tag values go right after encoded agg group and timebase (encoded as int)
      long encodedTagValue = readEncoded(rowKey, entityTable.getIdSize() *  (i + 1) + Bytes.SIZEOF_INT);
      String tagValue = entityTable.getName(encodedTagValue, tagNames[i]);
      tags.add(new TagValue(tagNames[i], tagValue));
    }

    return tags;
  }

  public long getTimestamp(byte[] rowKey, byte[] column) {
    // timebase is encoded as int after the encoded agg group
    int timebase = Bytes.toInt(rowKey, entityTable.getIdSize());
    // time leftover is encoded as 2 byte column name
    int leftover = Bytes.toShort(column) * resolution;

    return timebase + leftover;
  }

  private int writeEncodedAggGroup(List<TagValue> tagValues, byte[] rowKey, int offset) {
    // aggregation group is defined by list of tag names
    StringBuilder sb = new StringBuilder();
    for (TagValue tagValue : tagValues) {
      sb.append(tagValue.getTagName()).append(".");
    }

    return writeEncoded(TYPE_AGG_GROUP, sb.toString(), rowKey, offset);
  }

  /**
   * Save a long id into the given byte array, assuming the given array is always big enough.
   * @return incremented offset
   */
  private int writeEncoded(String type, String entity, byte[] destination, int offset) {
    long id = entityTable.getId(type, entity);
    int idSize = entityTable.getIdSize();
    while (idSize != 0) {
      idSize--;
      destination[offset + idSize] = (byte) (id & 0xff);
      id >>= 8;
    }

    return offset + entityTable.getIdSize();
  }

  /**
   * Save a long id into the given byte array, assuming the given array is always big enough.
   * @return incremented offset
   */
  private int writeAnyEncoded(byte[] destination, int offset) {
    // all encoded ids start with 1, so all zeroes is special case to say "any" matches
    // todo: all zeroes - should we move to entity table somehow?
    int idSize = entityTable.getIdSize();
    while (idSize != 0) {
      idSize--;
      destination[offset + idSize] = 0;
    }

    return offset + entityTable.getIdSize();
  }

  private long readEncoded(byte[] bytes, int offset) {
    long id = 0;
    int idSize = entityTable.getIdSize();
    for (int i = 0; i < idSize; i++) {
      id |= (bytes[offset + i] & 0xff) << ((idSize - i - 1) * 8);
    }
    return id;
  }

  /**
   * Returns timebase computed with the table setting for the given timestamp.
   */
  private int getTimeBase(long time) {
    // We are using 4 bytes timebase for row
    long timeBase = time / rollTimebaseInterval * rollTimebaseInterval;
    Preconditions.checkArgument(timeBase < 0x100000000L, "Timestamp is too large.");
    return (int) timeBase;
  }

  private byte[][] createDeltaCache(int rollTime) {
    byte[][] deltas = new byte[rollTime + 1][];

    for (int i = 0; i <= rollTime; i++) {
      deltas[i] = Bytes.toBytes((short) i);
    }
    return deltas;
  }
}
