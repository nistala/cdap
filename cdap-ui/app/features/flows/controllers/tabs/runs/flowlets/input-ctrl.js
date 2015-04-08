angular.module(PKG.name + '.feature.flows')
  .controller('FlowletDetailInputController', function($state, $scope, MyDataSource, myHelpers) {

    var dataSrc = new MyDataSource($scope);
    var flowletid = $state.params.flowletid;
    $scope.inputs = [];

    // Initialize
    dataSrc
      .request({
        _cdapNsPath: '/apps/' + $state.params.appId+  '/flows/' + $state.params.programId
      })
      .then(function (res) {

        // INPUTS
        angular.forEach(res.connections, function(v) {
          if (v.targetName === flowletid) {
            $scope.inputs.push({
              name: v.sourceName
            });
          }
        });

        if ($scope.inputs.length > 0) {
          // INPUT METRICS
          // dataSrc
          //   .poll({
          //     _cdapPath: '/metrics/query?context=namespace.' + $state.params.namespace
          //                   + '.app.' + $state.params.appId
          //                   + '.flow.' + $state.params.programId
          //                   + '.flowlet.' + $scope.inputs[0].name
          //                   + '&metric=system.process.events.out&start=now-60s&count=60',
          //     method: 'POST'
          //   }, function (res) {
          //     updateInput(res.series[0].data);

          //   });

          angular.forEach($scope.inputs, function (input) {
            // POLLING GRAPH
            dataSrc
              .poll({
                _cdapPath: '/metrics/query?context=namespace.' + $state.params.namespace
                              + '.app.' + $state.params.appId
                              + '.flow.' + $state.params.programId
                              + '.flowlet.' + input.name
                              + '&metric=system.process.events.out&start=now-60s&count=60',
                method: 'POST'
              }, function (res) {
                // updateInput(res.series[0].data);
                var response = res.series[0].data;
                if(angular.isObject(response)) {
                  var v = [];

                  angular.forEach(response, function(val) {
                    v.push({
                      time: val.time,
                      y: val.value
                    });
                  });

                  if (input.history) {
                    input.stream = v.slice(-1);
                  }

                  input.history = [
                    {
                      label: 'output',
                      values: v
                    }
                  ];

                }

              });

            // POLLING ARRIVAL RATE
            dataSrc
              .poll({
                _cdapPath: '/metrics/query?context=namespace.' + $state.params.namespace
                              + '.app.' + $state.params.appId
                              + '.flow.' + $state.params.programId
                              + '.flowlet.' + input.name
                              + '&metric=system.process.events.out&start=now-60s&count=1',
                method: 'POST'
              }, function (res) {
                if (res.series[0]) {
                  input.total = res.series[0].data[0].value / 60;
                }
              });

          });

        }

      });
  });
