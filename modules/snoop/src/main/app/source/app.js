angular.module('FFWDStateIndex', ['ngMaterial']);

angular.module('FFWDStateIndex')
    .controller('app', function($scope){
        $scope.data = {
            events: {
                commonTags: null,
                commonAttributes: null,
                datapoints: {}
            },
            metrics: {
                commonTags: null,
                commonAttributes: null,
                datapoints: {}
            }
        };

        $scope.ws = new WebSocket("ws://127.0.0.1:8080/stream");
        $scope.ws.onopen = function(){
            console.debug("WS Opened!");
        };
        $scope.ws.onclose = function(){
            console.debug("WS Closed!");
        };
        $scope.ws.onerror = function (err) {
            console.debug("WS Error: %O", err);
        };
        var calculateCommonTags = function(datapoints){
            var commonTags = [];
            if (datapoints.length > 0){
                commonTags = angular.copy(datapoints[0].meta.tags);
            }
            datapoints.forEach(function(series){
                commonTags = _.intersection(commonTags, series.meta.tags);
            });
            datapoints.forEach(function(series){
                series.meta.identifyingTags = _.difference(series.tags, commonTags);
            });
            return commonTags;
        };

        var calculateCommonAttributes = function(data){
            var commonTags = [];
            var commonAttributes = {};
            var datapoints = _.values(data.datapoints);

            if (datapoints.length > 0){
                commonTags = angular.copy(datapoints[0].meta.tags);
                commonAttributes = angular.copy(datapoints[0].meta.attributes);
            }
            datapoints.forEach(function(series){
                Object.keys(commonAttributes).forEach(function(key){
                    var value = commonAttributes[key];
                    if (series.meta.attributes[key] != value){
                        delete commonAttributes[key];
                    }
                });
                commonTags = _.intersection(commonTags, series.meta.tags);
            });
            datapoints.forEach(function(series){
                series.meta.identifyingKeys = Object.keys(series.meta.attributes).filter(function(key){ return commonAttributes[key] === undefined}).sort();
                series.meta.identifyingTags = _.difference(series.tags, commonTags);
            });
            data.commonTags = commonTags;
            data.commonAttributes = commonAttributes;
        };
        $scope.ws.onmessage = function(evt){
            console.debug("WS Message: %O", evt);
            $scope.$apply(function(){
                var data = JSON.parse(evt.data);
                if (data.events !== undefined) {
                    // TODO: Handle events
                    /*
                    var events = $scope.data.events;
                    Object.keys(data.events).forEach(function (eventId) {
                        var event = data.events[eventId];
                        var timestamp = new Date(event.time);
                        if (events.datapoints[eventId] === undefined) {
                            events.datapoints[eventId] = {
                                meta: {
                                    tags: event.tags,
                                    attributes: event.attributes
                                },
                                values: [{time: timestamp, value: event.value}]
                            }
                        } else {
                            events.datapoints[eventId].values.unshift({time: timestamp, value: event.value});
                        }
                    });
                    update($scope.data.events);
                    */
                }
                if (data.metrics !== undefined){
                    var metrics = $scope.data.metrics;
                    Object.keys(data.metrics).forEach(function (metricId) {
                        var metric = data.metrics[metricId];
                        var timestamp = new Date(metric.time);
                        if (metrics.datapoints[metricId] === undefined) {
                            metrics.datapoints[metricId] = {
                                meta: {
                                    tags: metric.tags,
                                    attributes: metric.attributes
                                },
                                values: [{time: timestamp, value: metric.value}]
                            }
                        } else {
                            metrics.datapoints[metricId].values.unshift({time: timestamp, value: metric.value});
                        }
                    });
                    calculateCommonAttributes($scope.data.metrics);
                }
            });
            console.debug($scope.data);
        };
    });
