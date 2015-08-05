var redis = require("redis"),
    client = redis.createClient();

client.on("error", function (err) {
  console.log("Redis error: " + err);
});

var apps = [];
var operations = [];

// Initial refresh interval is 10 seconds.
var refreshInterval = 10;

// Reset the tokens.
// Also calculate the qps.
function reset_tokens(appKey, capacityKey) {
  client.get(capacityKey, function(err1, capacity) {
    client.get(appKey, function(err2, currentToken) {
      currentToken = Math.max(0, currentToken);
      var capacityTokens = capacity * refreshInterval;
      var qps = (capacityTokens - currentToken) / refreshInterval;
      client.set('QPS-' + appKey, qps);
      client.set(appKey, capacityTokens);
    });
  });
}

// Based on the refresh interval, 
// it is resetting the allowed tokens within refresh interval
function sched_reset(appName, operation) {
    setTimeout(function() {
      var key = appName + '-' + operation;
      var capacityKey = 'Capacity-' + key;
      reset_tokens(key, capacityKey);
      sched_reset(appName, operation);
    }, 1000 * refreshInterval);
}

// On every 2 second, refresh global variables.
function sched_refresh_global() {
    setTimeout(function() {
      client.get('refresh_interval', function(err, reply) {
        refreshInterval = reply;
      });
      client.lrange('operations', 0, -1, function(err, ops) {
        operations = ops;
      });
      client.lrange('apps', 0, -1, function(err, appList) {
        apps = appList;
      });
      sched_refresh_global();
    }, 2000);
}

// Schedule reset and refresh global
function sched_all() {
  apps.forEach(function(app) {
    operations.forEach(function(op) {
      sched_reset(app, op);
    });
  });
  sched_refresh_global();
}

// Init global variables, and schedule jobs
client.lrange('operations', 0, -1, function(err1, ops) {
  client.lrange('apps', 0, -1, function(err2, appList) {
    operations = ops;
    apps = appList;
    sched_all();
  });
});

