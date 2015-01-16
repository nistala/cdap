/*
 * Copyright © 2014-2015 Cask Data, Inc.
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

package co.cask.cdap.api.schedule;

import com.google.common.base.Objects;

/**
 * Defines a cron-based schedule for running a program. 
 */
public class Schedule {

  private final String name;

  private final String description;

  private final String cronEntry;

  public Schedule(String name, String description, String cronEntry) {
    this.name = name;
    this.description = description;
    this.cronEntry = cronEntry;
  }

  /**
   * @return Name of the schedule.
   */
  public String getName() {
    return name;
  }

  /**
   * @return Schedule description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return Cron expression for the schedule.
   */
  public String getCronEntry() {
    return cronEntry;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Schedule that = (Schedule) o;

    return Objects.equal(this.name, that.name) &&
      Objects.equal(this.description, that.description) &&
      Objects.equal(this.cronEntry, that.cronEntry);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, cronEntry);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("name", name)
      .add("description", description)
      .add("cronEntry", cronEntry)
      .toString();
  }
}
