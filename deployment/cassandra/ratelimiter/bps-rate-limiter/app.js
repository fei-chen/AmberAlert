var express = require('express'),
    //redis = require('redis'),
    redis = require('/usr/lib/node_modules/redis'),
    http = require('http'),
    //io = require('socket.io'), 
    io = require('/usr/lib/node_modules/socket.io'),
    app = express(),
    client = redis.createClient();

var server = http.createServer(app);

function log(type, msg) {
    var color   = '\u001b[0m',
        reset = '\u001b[0m';
    switch(type) {
        case "info":
            color = '\u001b[36m';
            break;
        case "warn":
            color = '\u001b[33m';
            break;
        case "error":
            color = '\u001b[31m';
            break;
        case "msg":
            color = '\u001b[34m';
            break;
        default:
            color = '\u001b[0m'
    }
    console.log(color + '   ' + type + '  - ' + reset + msg);
}

app.set('views', __dirname + '/views');
app.set('view engine', 'ejs');
app.use(express.static(__dirname + '/public'));

// Get all capacity values, something like
// { 'Cassandra-CONNECT':    { ALTER: '1', DROP: '1', CREATE: '1', WRITE: '2000', READ: '1000', PREPARE: '2000', SCAN: '50', CONNECT: '1000' }
function getCapacity(apps, ops, keyPrefix, callback) {
  var capacities = {}
  apps.forEach(function(app) {
    ops.forEach(function(op) {
      var key = keyPrefix + app + '-' + op;
      client.get(key, function(err, capacity) {
        if (capacities[app] === undefined) {
          capacities[app] = {}
        }
        capacities[app][op] = parseInt(capacity);
        var finished = true;
        for (var key in capacities) {
          finished = finished && (Object.keys(capacities[key]).length === ops.length);
        }
        if (callback !== undefined &&
            Object.keys(capacities).length === apps.length &&
            finished) {
          callback(capacities);
        }
      });
    });
  });
}

// Setup request handlers
app.get('/RateLimiterControl', function (req, res) {
  var apps = [], ops = [], values = [];
  client.lrange('apps', 0, -1, function(err1, appList) {
    apps = apps.concat(appList);
    
    // get qps capacity
    client.lrange('op-qps', 0, -1, function(err2, opList) {
      getCapacity(appList, opList, 'AllocatedCapacity-', function(capacities) {
        ops = ops.concat(opList);
        for(var key in capacities) values[key] = capacities[key];
        
        // get connect quota capacity
        client.lrange('op-total', 0, -1, function(err2, opList) {
          getCapacity(appList, opList, 'TotalQuota-', function(capacities) {
           ops = ops.concat(opList);
           for(var app in values) 
             for(var op in capacities[app]) 
                values[app][op] = capacities[app][op];
          
            // get write-bps capacity
            client.lrange('op-bps', 0, -1, function(err2, opList) {
              getCapacity(appList, opList, 'AllocatedByteCapacity-', function(capacities) {
                 for(var app in values)
                   for(var op in capacities[app])
                      values[app][op] = capacities[app][op];

                  ops = ops.concat(opList);
                  res.render('layout', {
                    title : 'Rate Limiter Control',
                    apps  : apps,
                    ops   : ops,
                    capacity : values 
                  });
              });
            });
          });
        });
      });
    });
  });
});

app.get("/:appName/:operation/:permits", function(req, res) {
  var key = req.params.appName + '-' + req.params.operation;
  var permits = req.params.permits;
  client.decrby(key, permits, function(err, reply) {
    if (reply === null || reply === undefined) {
      reply = '';
    }
    res.end(reply.toString());
  });
});

// PPQ: permits per query
app.get("/PPQ/:operation", function(req, res) {
  var ppqKey = 'PPQ-' + req.params.operation;
  client.get(ppqKey, function(err, reply) {
    if (reply === null) {
      reply = '';
    }
    res.end(reply.toString());
  });
});

// refresh interval handler.
app.get("/refreshInterval", function(req, res) {
  client.get("refresh_interval", function(err, reply) {
    if (reply === null) {
      reply = '';
    }
    res.end(reply.toString());
  });
});

var server = http.createServer(app);
var socket  = io.listen(server);

// Set up socket connection handlers.
socket.on('connection', function(browser) {
  // browser is the browser socket object.
  var subscribe = redis.createClient();
  subscribe.subscribe('realtime');
  log('debug', 'socket connected');      
  subscribe.on("message", function(channel, message) {
      browser.send(message);
      log('debug', "received from channel #" + channel + " : " + message);
  });

  browser.on('disconnect', function() {
      log('debug', 'disconnecting from redis');
  });

  // The client is requesting to update the capacity
  browser.on('set_capacity', function (msg) {
    var app = msg.app;
    var op = msg.op;
    var value = msg.value;
    if(op == 'CONNECT-TOTAL') {
      var key = 'TotalQuota-'+app+'-'+op;
      client.set(key, value);
      key = 'NewQuota-'+app+'-'+op;
      client.set(key, 0);
      key = 'CurrQuota-'+app+'-'+op;
      client.set(key, value);
      return ;
    }
    else if(op == 'WRITE-BPS') {
      var allocatedPrefix = 'AllocatedByteCapacity-';
      var capacityPrefix = 'ByteCapacity-';
      var tokenPrefix = 'Bytes-';
    }
    else {
      var allocatedPrefix = 'AllocatedCapacity-';
      var capacityPrefix = 'Capacity-';
      var tokenPrefix = '';
    }
    

    var key = allocatedPrefix + app + '-' + op;
    client.set(key, value);

    // reset capacity to allocated for all apps 
    client.lrange('apps', 0, -1, function(err1, appList) {
        var ops = [op];
        getCapacity(appList, ops, allocatedPrefix, function(allocatedCapacities) {
            for(var appi = 0; appi < appList.length; appi++) {
              var app = appList[appi];
              for(var opi = 0; opi < ops.length; opi++) {
                var op = ops[opi];
                var key = capacityPrefix + app + '-' + op;
                client.set(key, allocatedCapacities[app][op]);
              }
            }
        });
    });

    // reset current token
    // if current token is not updated, then qps/bps will be incorrectly calculated in reset.js,
    // qps/bps will be calculated based on current capacity and out of dated remaining token  
    key = tokenPrefix+app + '-' + op;
    client.get('refresh_interval', function(err, reply) {
        client.set(key, value*reply);
    });
  });
});

var args = process.argv.slice(2);
log('debug', 'listening on ' + args[0]);
server.listen(args[0]);
