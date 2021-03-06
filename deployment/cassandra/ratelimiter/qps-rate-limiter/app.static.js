var express = require('express'),
    redis = require('redis'),
    http = require('http'),
    io = require('socket.io'),
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
function getCapacity(apps, ops, callback) {
  var capacities = {}
  apps.forEach(function(app) {
    ops.forEach(function(op) {
      var key = 'Capacity-' + app + '-' + op;
      client.get(key, function(err, capacity) {
        if (capacities[app] === undefined) {
          capacities[app] = {}
        }
        capacities[app][op] = capacity;
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

  client.lrange('apps', 0, -1, function(err1, appList) {
    client.lrange('operations', 0, -1, function(err2, ops) {
      getCapacity(appList, ops, function(capacities) {
        console.log(capacities);

        res.render('layout', {
          title : 'Rate Limiter Control',
          apps  : appList,
          ops   : ops,
          capacity : capacities 
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
    var key = 'Capacity-' + app + '-' + op;
    log('debug', key)
    client.set(key, value);
  });
});



var args = process.argv.slice(2);
server.listen(args[0]);
log('debug', 'listening on ' + args[0]);

