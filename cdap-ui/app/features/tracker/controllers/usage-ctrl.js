/*
 * Copyright © 2016 Cask Data, Inc.
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

class TrackerUsageController {
  constructor($state, $scope, myTrackerApi) {

    this.$state = $state;
    this.$scope = $scope;
    this.myTrackerApi = myTrackerApi;

    let entitySplit = this.$state.params.entityType.split(':');

    this.entityType = entitySplit;


    this.timeRangeOptions = [
      {
        label: 'Last 7 days',
        start: 'now-7d',
        end: 'now'
      },
      {
        label: 'Last 14 days',
        start: 'now-14d',
        end: 'now'
      },
      {
        label: 'Last month',
        start: 'now-30d',
        end: 'now'
      },
      {
        label: 'Last 6 months',
        start: 'now-180d',
        end: 'now'
      },
      {
        label: 'Last 12 months',
        start: 'now-365d',
        end: 'now'
      }
    ];

    this.timeRange = {
      start: $state.params.start || 'now-7d',
      end: $state.params.end || 'now'
    };

    this.customTimeRange = {
      startTime: null,
      endTime: null
    };

    this.selectedTimeRange = this.findTimeRange();

    this.fetchAuditHistogram();
    this.fetchTopDatasets();
    this.fetchTimeSince();
  }

  findTimeRange() {
    let match = this.timeRangeOptions.filter( (option) => {
      return option.start === this.timeRange.start && option.end === this.timeRange.end;
    });

    if (match.length === 0) {
      this.isCustom = true;
      this.customTimeRange.startTime = new Date(parseInt(this.$state.params.start, 10) * 1000);
      this.customTimeRange.endTime = new Date(parseInt(this.$state.params.end, 10) * 1000);
    }

    return match.length > 0 ? match[0] : { label: 'Custom' };
  }

  goCustomDate() {
    let startTime = parseInt(this.customTimeRange.startTime.valueOf() / 1000, 10);
    let endTime = parseInt(this.customTimeRange.endTime.valueOf() / 1000, 10);

    this.$state.go('tracker.detail.entity.usage', { start: startTime, end: endTime });
  }

  fetchAuditHistogram() {
    let params = {
      namespace: this.$state.params.namespace,
      // startTime: this.timeRange.start,
      // endTime: this.timeRange.end,
      scope: this.$scope
    };

    this.myTrackerApi.getAuditHistogram(params)
      .$promise
      .then((response) => {
        this.auditHistogram = response;
      }, (err) => {
        console.log('Error', err);
      });
  }

  fetchTopDatasets() {
    let params = {
      namespace: this.$state.params.namespace,
      limit: 5,
      // startTime: 'now-7d',
      // endTime: 'now',
      scope: this.$scope,
      entity: 'datasets'
    };

    this.myTrackerApi.getTopEntities(params)
      .$promise
      .then((response) => {
        this.topDatasets = response;
        this.emptyRows = false;
        if (this.topDatasets.length >= 3) {
          this.emptyRows = true;
          this.totalEmptyRows = Array.apply(null, {length: 5 - this.topDatasets.length}).map(Number.call, Number);
        }
      }, (err) => {
        console.log('Error', err);
      });
  }

  fetchTimeSince() {
    let params = {
      namespace: this.$state.params.namespace,
      scope: this.$scope,
      entityName: this.$state.params.entityId,
      entityType: this.$state.params.entityType
    };

    this.myTrackerApi.getTimeSince(params)
      .$promise
      .then((response) => {
        this.timeSince = response;
        console.log('this.timeSince: ', this.timeSince);
      }, (err) => {
        console.log('Error', err);
      });
  }
}

TrackerUsageController.$inject = ['$state', '$scope', 'myTrackerApi'];

angular.module(PKG.name + '.feature.tracker')
.controller('TrackerUsageController', TrackerUsageController);

