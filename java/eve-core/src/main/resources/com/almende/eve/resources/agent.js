/**
 * Javascript for Agent web interface
 */


var myApp = angular.module('controller', ['ngResource']);


/**
 * Adjust the height of given textarea to match its contents
 * @param {Element} elem HTML DOM Textarea element
 */
function resize (elem) {
    var scrollTop = document.body.scrollTop;

    elem.style.height = 'auto';
    elem.style.height = (elem.scrollHeight + 20) + 'px';

    document.body.scrollTop = scrollTop;  // restore the scroll top
}

/**
 * @constructor Controller
 * Angular JS controller to control the page
 */
function Controller($scope, $resource) {
    var loadingText = '...';
    var url = document.location.href;
    var lastSlash = url.lastIndexOf('/');
    $scope.url         = url.substring(0, lastSlash + 1);
    $scope.urls        = loadingText;
    $scope.title       = loadingText;
    $scope.version     = loadingText;
    $scope.description = loadingText;
    $scope.type        = loadingText;
    $scope.id          = loadingText;
    $scope.mode = 'form';

    // form
    $scope.methods = [{}];
    $scope.method = $scope.methods[0];
    $scope.result = '';
    $scope.formStatus = '';

    // json rpc
    $scope.request = undefined;
    $scope.response = undefined;
    $scope.rpcStatus = '';

    // event logs
    $scope.lastTimestamp = 0;
    $scope.pollingInterval = 10000;  // polling interval in milliseconds
    $scope.logs = [];
    $scope.enableEvents = true;

    /**
     * Change the currently selected method
     */
    $scope.setMethod = function () {
        for (var i = 0; i < $scope.methods.length; i++) {
            var method = $scope.methods[i];
            if (method.method == $scope.methodName) {
                $scope.method = method;
                break;
            }
        }
    };

    /**
     * Send a JSON-RPC request
     * @param {String} url        Url where to send the request
     * @param {JSON} request      A JSON-RPC 2.0 request, like
     *                            {"id":1,"method":"add","params":{"a":2,"b":3}}
     * @param {function} callback A callback method. The callback will be
     *                            called with a JSON-RPC response as
     *                            first argument (of type JSON), for example
     *                            {"jsonrpc":"2.0","id":1,"result":5}
     * @param {function} errback  Optional callback function in case of
     *                            an error
     */
    $scope.send = function (url, request, callback, errback) {
        $.ajax({
            'type': 'POST',
            'url': url,
            'contentType': 'application/json',
            'data': JSON.stringify(request),
            'success': callback,
            'error': function (err) {
                if (errback) {
                    errback(err);
                }
                else {
                    console.log(err);
                }
            }
        });
    };

    /**
     * Check whether a given type is a primitive type like 'string', 'long',
     * 'double', but not some complex type like 'Map<String, String>' or
     * 'Contact'.
     * @param {String} type   The name of a type
     * @return {boolean}      True if primitive, else false
     */
    $scope.isPrimitiveType = function (type) {
        var primitives = ['string', 'char', 'long', 'double', 'int',
            'number', 'float', 'byte', 'short', 'boolean'];
        return (primitives.indexOf(type.toLowerCase()) != -1);
    };

    /**
     * Format the given date as string
     * @param {Date | Number} date
     * @return {String} formattedDate
     */
    $scope.formatDate = function(date) {
        var d = new Date(date);
        return d.toISOString ? d.toISOString() : d.toString();
    };

    /**
     * Send an JSON-RPC request.
     * The request is built up from the current values in the form,
     * and the field result in the response is filled in in the field #result
     */
    $scope.sendForm = function () {
        try {
            var request = {};
            request.id = 1;
            request.method = $scope.method.method;
            request.params = {};
            for (var i = 0; i < $scope.method.params.length; i++) {
                var param = $scope.method.params[i];
                if (param.required || (param.value && param.value.length > 0) ) {
                    if (param.type.toLowerCase() == 'string') {
                        request.params[param.name] = param.value;
                    }
                    else {
                        request.params[param.name] = JSON.parse(param.value);
                    }
                }
            }

            var start = +new Date();
            $scope.formStatus = 'sending...';
            $scope.send($scope.url, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                $scope.formStatus = 'ready in ' + diff + ' ms';

                if (response.error) {
                    $scope.result = 'Error: ' + JSON.stringify(response.error, null, 2);
                }
                else {
                    if (response.result instanceof Object) {
                        $scope.result = JSON.stringify(response.result, null, 2) || '';
                    }
                    else {
                        $scope.result = (response.result != undefined) ? String(response.result) : '';
                    }
                }
                $scope.$apply();
                resize($('#result').get(0));
            }, function (err) {
                $scope.formStatus = 'failed. Error: ' + JSON.stringify(err);
                $scope.$apply();
            });
        }
        catch (err) {
            $scope.formStatus = 'Error: ' + err;
        }
    };

    /**
     * Send a JSON-RPC request.
     * The request is read from the field #request, and the response is
     * filled in in the field #response
     */
    $scope.sendJsonRpc = function() {
        var $scope = $scope;
        try {
            var request = JSON.parse($scope.request);
            $scope.request = JSON.stringify(request, null, 2);
            $scope.$apply();
            resize($('#request').get(0));

            $scope.rpcStatus = 'sending...';
            var start = +new Date();
            $scope.send($scope.url, request, function (response) {
                var end = +new Date();
                var diff = (end - start);
                $scope.response = JSON.stringify(response, null, 2);
                $scope.rpcStatus = 'ready in ' + diff + ' ms';
                $scope.$apply();
                resize($('#response').get(0));
            }, function (err) {
                $scope.rpcStatus = 'failed. Error: ' + JSON.stringify(err);
                $scope.$apply();
            });
        }
        catch (err) {
            $scope.rpcStatus = 'Error: ' + err;
        }
    };

    /**
     * Store the setting enableEvents
     */
    $scope.updateEnableEvents = function () {
        if ($scope.enableEvents == true) {
            // enableEvents==true is the default setting, do not store it
            delete localStorage['enableEvents'];
            $scope.startMonitoringEvents();
        }
        else {
            localStorage['enableEvents'] = false;
            $scope.stopMonitoringEvents();
            $scope.clearEvents();
        }
    };

    /**
     * Start monitoring the events of the agent
     */
    $scope.startMonitoringEvents = function () {
        $scope.updateEvents();
    };

    /**
     * Stop monitoring the events of the agent
     */
    $scope.stopMonitoringEvents = function () {
        if ($scope.updateEventsTimer) {
            clearTimeout($scope.updateEventsTimer);
            delete $scope.updateEventsTimer;
        }
    };

    /**
     * Retrieve the latest event logs, and set a timeout for the next update
     */
    $scope.updateEvents = function () {
        $scope.stopMonitoringEvents();

        $.ajax({
            'type': 'GET',
            'url': "events?since=" + $scope.lastTimestamp,
            'contentType': 'application/json',
            'success': function (newLogs) {
                while (newLogs && newLogs.length) {
                    var newLog = newLogs.shift();
                    $scope.lastTimestamp = newLog.timestamp;
                    $scope.logs.push(newLog);
                }
                $scope.lastUpdate = (new Date()).toISOString();
                $scope.$apply();

                // set a new timeout
                $scope.updateEventsTimer = setTimeout($scope.updateEvents, $scope.pollingInterval);
            },
            'error': function (err) {
                // set a new timeout
                $scope.updateEventsTimer = setTimeout($scope.updateEvents, $scope.pollingInterval);
            }
        });
    };

    /**
     * Clear the list with events
     */
    $scope.clearEvents = function () {
        $scope.logs = [];
    };

    /**
     * Load information and data from the agent via JSON-RPC calls.
     * Retrieve the methods, type, id, description, etc.
     */
    $scope.load = function () {
        // read settings from local storage
        if (localStorage['enableEvents'] != undefined) {
            $scope.enableEvents = localStorage['enableEvents'];
        }

        var reqs = [
            {
                'method': 'getUrls',
                'field': 'urls',
                'callback': function () {
                    $scope.updateEnableEvents();
                }
            },
            {
                'method': 'getType',
                'field': 'type',
                'callback': function () {
                    document.title = ($scope.type || 'Agent') + ' ' + ($scope.id || '');
                }
            },
            {
                'method': 'getId',
                'field': 'id',
                'callback': function () {
                    document.title = ($scope.type || 'Agent') + ' ' + ($scope.id || '');
                }
            },
            {'method': 'getDescription', 'field': 'description'},
            {'method': 'getVersion', 'field': 'version'},
            {
                'method': 'getMethods',
                'field': 'methods',
                'params': {'asJSON': true},
                'callback': function () {
                    if ($scope && $scope.methods && $scope.methods[0]) {
                        $scope.methodName = $scope.methods[0].method;
                        $scope.setMethod();
                        $scope.$apply();

                        // update method select box
                        setTimeout(function () {
                            $(".chzn-select").chosen();
                        }, 15);
                    }
                }
            }
        ];

        var total = reqs.length;
        var left = total;
        var decrement = function () {
            left--;
            if (left > 0) {
                $scope.progress = Math.round((total - left) / total * 100) + '%';
            }
            else {
                $scope.loading = false;
            }
            $scope.$apply();
        };
        for (var i = 0; i < reqs.length; i++) {
            (function (req) {
                var request = {
                    "id":1,
                    "method": req.method,
                    "params": req.params || {}
                };
                $scope.send($scope.url, request, function(response) {
                    $scope[req.field] = response.result;
                    if (response.error) {
                        //$scope.error = JSON.stringify(response.error, null, 2);
                        var err = response.error;
                        $scope.error = 'Error ' + err.code + ': ' + err.message +
                            ((err.data && err.data.description) ? ', ' + err.data.description : '');
                    }
                    $scope.$apply();
                    if (req.callback) {
                        req.callback(response.result);
                    }
                    decrement();
                }, function (err) {
                    decrement();
                    console.log(err);
                });
            })(reqs[i]);
        }
    };

    // fill in an initial JSON-RPC request
    var defaultRequest = {
        "id": 1,
        "method": "getMethods",
        "params": {
            "asJSON": false
        }
    };
    $scope.request = JSON.stringify(defaultRequest, null, 2);

    $scope.loading = true;
    $scope.load();
}
