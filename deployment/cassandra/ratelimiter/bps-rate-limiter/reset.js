//var redis = require("redis"),
var redis = require("/usr/lib/node_modules/redis"),
    client = redis.createClient();

client.on("error", function (err) {
  console.log("Redis error: " + err);
});

// app list (ex: Cassandra-DATA_ETL,Cassandra-P13N,Cassandra-RSRP,Cassandra-THEMATIC)
var apps = [];
// op list throttled by qps (ex: CONNECT,SCAN,PREPARE,READ, WRITE)
var qpsOps = [];
// op list throttled by bps (ex: WRITE)
var bpsOps = [];
// op list throttled by total number (ex: CONNECT)
var totalOps = [];

var resetQPSFinish = false;
var resetTokenFinish = false;

// Initial refresh interval is 10 seconds.
var refreshInterval = 10;
var qpsResetCount = 0;
var bpsResetCount = 0;
var connResetCount = 0;

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

function resetConnectionQuota() {
  connResetCount++;
  var gcUnclosedConn = false;
  if(connResetCount == 4*3600/refreshInterval) { // clean up unclosed connection unclosed connections every 4 hrs
    connResetCount = 0;
    gcUnclosedConn = true; 
  }

  apps.forEach(function(app) {
    totalOps.forEach(function(op) {
    var totalQuotaKey ="TotalQuota-"+app+"-"+op;
    var currQuotaKey ="CurrQuota-"+app+"-"+op;
    var newQuotaKey ="NewQuota-"+app+"-"+op;

    client.get(currQuotaKey, function(err1, currQuota) {
      client.get(newQuotaKey, function(err1, newQuota) {
        client.get(totalQuotaKey, function(err1, totalQuota) {
          if(gcUnclosedConn) newQuota = totalQuota;
          console.log("clean up unclosed connections for "+app+" : "+gcUnclosedConn);
          //console.log("before reset: current connection quota: " + currQuotaKey + ":"+currQuota);
          //console.log("before reset: new connection new quota: " + newQuotaKey + ":"+newQuota);

          currQuota = Math.min(Math.max(Number(currQuota), 0)+Number(newQuota), Number(totalQuota));
          newQuota = 0;
          client.set(currQuotaKey, currQuota);
          client.set(newQuotaKey, newQuota);
          console.log("after reset: current connection quota: " + currQuotaKey + ":"+currQuota);
          //console.log("afer reset: connection new quota: " + newQuotaKey + ":"+newQuota);
        });
      });
    });
  });
  });
}

// this reset will first calculate qps,
// then qps willl be used to determine which apps are under allocated and over allocated,
// and how much capacity over allocated apps can spare in adjust_capacity,
// after adjust_capacity is done, each app's tokens will be refilled based on new capacity.
function resetQPSRates() {
  resetRates('QPS');
}

function resetBPSRates() {
  resetRates('BPS');
}

function resetRates(option) {
  if(option == 'QPS') {
    var capacityKeyPrefix = 'Capacity-';
    var ops = qpsOps;
    var ratePrefix = 'QPS-';
    var tokenPrefix = '';
  }
  else { // BPS
    var capacityKeyPrefix = 'ByteCapacity-';
    var ops = bpsOps;
    var ratePrefix = 'BPS-';
    var tokenPrefix = 'Bytes-';
  }

  apps.forEach(function(app) {
    ops.forEach(function(op) {
      var appKey = app + '-' + op;
      var tokenKey = tokenPrefix + appKey;
      var capacityKey = capacityKeyPrefix + appKey;
      client.get(capacityKey, function(err1, capacity) {
        client.get(tokenKey, function(err2, currentToken) {
          // calculate the qps.
          currentToken = Math.max(0, currentToken);
          var capacityTokens = capacity * refreshInterval;
          var qps = (capacityTokens - currentToken) / refreshInterval;
          // log("debug", appKey+' capacity tokens'+capacityTokens);
          //log("debug", appKey+' capacity '+capacity);
          //log("debug", appKey+' refresh interval '+refreshInterval);
          //log("debug", appKey+' current tokens '+currentToken);
          //log("debug", appKey+' current capacity '+capacityTokens);
          //log("debug", appKey+' qps '+qps);
          //log("debug", 'set '+ratePrefix+appKey+' to ' + qps);
          client.set(ratePrefix + appKey, qps);
          var opNum = apps.length*ops.length;
          if(option == 'QPS') {
            qpsResetCount++;
            // adjust capacity every 20 secs, remember to revert it to 100 secs (opNum*10)
            if(qpsResetCount == (opNum*10)) {
              adjust_capacity(option);
              qpsResetCount = 0;
            }
            else if(qpsResetCount%opNum == 0) reset_tokens(option);
          }
          else {
            bpsResetCount++;
            // adjust capacity every 20 secs, remember to revert it to 100 secs (opNum*10)
            if(bpsResetCount == (opNum*10)) {
              adjust_capacity(option);
              bpsResetCount = 0;
            }
            else if(bpsResetCount%opNum == 0) reset_tokens(option);
         } 
        });
        });
    });
    });
}

function reset_tokens(option) {
  log("debug", 'reset tokens');
  if(option == 'QPS') {
    var capacityPrefix = 'Capacity-';
    var ops = qpsOps;
    var tokenPrefix = '';
  }
  else {
    var capacityPrefix = 'ByteCapacity-';
    var ops = bpsOps;
    var tokenPrefix = 'Bytes-';
  }

  apps.forEach(function(app) {
    ops.forEach(function(op) {
      var tokenKey = tokenPrefix+app + '-' + op;
      var capacityKey = capacityPrefix + app + '-' + op;
      client.get(capacityKey, function(err1, capacity) {
        var capacityTokens = capacity * refreshInterval;
        log("debug", 'set token:'+tokenKey+' to ' + capacityTokens);
        client.set(tokenKey, capacityTokens);
      });
    });
  });
}

// On every 2 second, refresh global variables.
function sched_refresh_global() {
    setTimeout(function() {
      client.get('refresh_interval', function(err, reply) {
        refreshInterval = reply;
      });

      client.lrange('op-qps', 0, -1, function(err, ops) {
        qpsOps = ops;
      });

      client.lrange('op-bps', 0, -1, function(err, ops) {
        bpsOps = ops;
      });

      client.lrange('op-total', 0, -1, function(err, ops) {
        totalOps = ops;
      });

      client.lrange('apps', 0, -1, function(err, appList) {
        apps = appList;
      });

      sched_refresh_global();
    }, 2000);
}

// Schedule reset and refresh global
function sched_all() {
  setInterval(
    function() {
      resetQPSRates();
      resetBPSRates();
      resetConnectionQuota();
    },
    refreshInterval*1000
  );
  sched_refresh_global();
}

// Init global variables, and schedule jobs

client.lrange('op-qps', 0, -1, function(err1, ops) {
  client.lrange('apps', 0, -1, function(err2, appList) {
    qpsOps = ops;
    apps = appList;
  });
});

client.lrange('op-bps', 0, -1, function(err1, ops) {
  bpsOps = ops;
});

client.lrange('op-total', 0, -1, function(err1, ops) {
  totalOps = ops;
});

sched_all();

// some helper funtions
function add(array, op, value) {
  if(array[op] == undefined) array[op] = 0;
  array[op] += value;
}

function set(array, op, app, value) {
  if (array[app] === undefined) array[app] = {}
  array[app][op] = value;
}

function append(array, op, app) {
  if(array[op] == undefined) array[op] = [];
  array[op].push(app);
}

function setCapacity(prefix, app, op, newCapacity, capacities) {
  //var key = 'Capacity-' + app + '-' + op;
  var key = prefix + app + '-' + op;
  log("debug", 'set '+app+'-'+op+' capacity to ' + newCapacity);
  client.set(key, newCapacity);
  capacities[app][op]=newCapacity;
}

function setCapacities(prefix, apps, op, newCapacities, oldCapacities) {
  for (var i = 0; i < apps.length; i++) {
    var app = apps[i];
    setCapacity(prefix, app, op, newCapacities[app][op], oldCapacities);
  }
}

function isNonZero(value) {
  return value != undefined && value != 0;
}

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

/**
 * distribute capacity evenly among apps
 * capacity: capacity to distribute
 * apps: apps to receive distributed capacity 
 * op: operation
 * capacities: origin capacities of apps
 **/
 
function distributeCapacityEvenly(capacityPrefix, capacity, apps, op, capacities) {
  var extra = Math.floor(Math.abs(capacity/apps.length));
  if(capacity < 0) extra = -1*extra;
  // luckApp is randomly picked up app to receive the remaining capacity, 
  // example: capacity = 3, #app is 2, each of which gets 1, luckyApp get remaining 1
 log('debug', 'redistributing capacity '+capacity+' among '+apps.toString()+'-'+op);
  if(extra != 0) {
    for(var appi = 0; appi < apps.length; appi++) {
        var app = apps[appi];
        var newCapacity = capacities[app][op]+extra;
        setCapacity(capacityPrefix, app, op, newCapacity, capacities);
    }
  }

  if(capacity%apps.length == 0) return;
  else {
    var luckyApp = apps[0]; 
    var newCapacity = capacities[luckyApp][op]+capacity%apps.length;
    setCapacity(capacityPrefix, luckyApp, op, newCapacity, capacities);
  }
}

/**
 * deduct capacity from apps in a greedy manner
 * 
 * total: total capacity to deduct
 * apps: apps to deduct capacity from
 * op: operation
 * capacities: origin capacities of apps
 **/
function deductCapacity(prefix, total, apps, belowCapacities, op, capacities) {
  log('debug', 'deduct capacity '+total+' from '+apps.toString()+'-'+op);
  if(total == 0) return ;
  for(var appi = 0; appi < apps.length; appi++) {
      var app = apps[appi];
      var spare = belowCapacities == null ? capacities[app][op] :capacities[app][op]-belowCapacities[app][op];

      if(total > spare && spare != 0) {
        log('debug', 'deduct capacity '+spare+' from '+app+'-'+op);
        var newCapacity = capacities[app][op]-spare;
        setCapacity(prefix, app, op, newCapacity, capacities);
        total -= spare;
      }
      else {
        log('debug', 'deduct capacity '+total+' from '+app+'-'+op);
        var newCapacity = capacities[app][op]-total;
        setCapacity(prefix, app, op, newCapacity, capacities);
        break ;
      }
  }
}

function adjust_capacity(option) {
   if(option == 'QPS') {
     var opList = 'op-qps';
     var capacityPrefix = 'Capacity-';
     var allocatedCapacityPrefix = 'AllocatedCapacity-';
     var ratePrefix = 'QPS-';
   }
   else {
     var opList = 'op-bps';
     var capacityPrefix = 'ByteCapacity-';
     var allocatedCapacityPrefix = 'AllocatedByteCapacity-';
     var ratePrefix = 'BPS-';
   }

   log('debug', 'adjusting capacity '+option);
   client.lrange('apps', 0, -1, function(err1, appList) {
    client.lrange(opList, 0, -1, function(err2, ops) {
      getCapacity(appList, ops, capacityPrefix, function(capacities) {
        getCapacity(appList, ops, allocatedCapacityPrefix, function(allocatedCapacities) {
          getCapacity(appList, ops, ratePrefix, function(rate) {
            var overCapacityApps = {}; // apps that are being throttled
            var overCapacityCreditors = {}; // apps which give capacity to others but need more capacity now
            var totalCapacityCreditorNeeds = {}; // capacity creditors need
            var belowCapacityApps = {}; // apps that contribute capacity 
            var belowCapacities = {}; // deducted capacities for each app 
            var totalCapacityUnused = {}; // capacity unused 
            
            // detect unfair capacity distribution
            for(var appi = 0; appi < appList.length; appi++) {
              var app = appList[appi];
              for(var opi = 0; opi < ops.length; opi++) {
                var op = ops[opi];
                var allocated = +allocatedCapacities[app][op];
                var capacity = +capacities[app][op];
                var qps = +rate[app][op];
                log('debug', app+'-'+op+': '+ option+':'+qps+' capacity:'+capacity+' allocated:'+allocated);
                if(capacity > allocated) {
                  // apps whose capacity was adjusted
                  if(qps >= capacity) {
                    // I already have extended capacity, but I need more
                    log('debug', app+'-'+op+' already has extended capacity, but needs more capacity '+option+':'+qps+' capacity:'+capacity);
                    append(overCapacityApps, op, app);
                  }
                  else {
                    // I already have extended capacity, but I don't need that much, give it back
                    log('debug', app+'-'+op+' gives capacity back '+(capacity-allocated));
                    // add capacity-allocated to unused capacity
                    add(totalCapacityUnused, op, capacity-allocated);
                    append(belowCapacityApps, op, app);
                    if(qps <= allocated*0.7 && Math.floor((allocated-qps)/2) != 0) {
                      // if used less than 70%, add 50% of unused to unused capacity
                      var spare = Math.floor((allocated-qps)/2);
                      log('debug', app+'-'+op+' gives some other unused capacity back'+spare);
                      add(totalCapacityUnused, op, spare);
                      set(belowCapacities, op, app, allocated-spare);
                    }
                    else 
                      // set allocated as the belowCapacities
                      set(belowCapacities, op, app, allocated);
                  }
                }
                else {
                  // apps whose capacity was taken away or not adjusted
                  if(qps >= capacity) {
                    if(capacity == allocated) {
                      // my capacity was not adjusted, just request more capacity
                      log('debug', app+'-'+op+' needs more capacity ');
                      append(overCapacityApps, op, app);
                    }
                    else {
                      // my capacity was taken, and I need more now, please return all of my given capacity to me.
                      log('debug', app+'-'+op+' needs given capacity back, my current capacity:  '+capacity+', but I should have '+allocated);
                      add(totalCapacityCreditorNeeds, op, allocated-capacity);
                      append(overCapacityCreditors, op, app);
                      // get my giving capacity back immediately
                      setCapacity(capacityPrefix, app, op, allocated, capacities);
                    }
                  }
                  else if(qps <= capacity*0.7) {
                    // used less than 70%, I can spare up to 50% of unused 
                    var spare = Math.floor((capacity-qps)/2);
                    log('debug', app+'-'+op+': spare:'+spare);
                    if(spare != 0) {
                      add(totalCapacityUnused, op, spare);
                      append(belowCapacityApps, op, app);
                      set(belowCapacities, op, app, capacity-spare);
                    }
                  }
                }
              }
            }

            // redistribute capacity
            for(var opi = 0; opi < ops.length; opi++) {
              var op = ops[opi];
              // Creditors are requesting capacity back
              if(isNonZero(totalCapacityCreditorNeeds[op])) {
                  if(isNonZero(totalCapacityUnused[op]) && totalCapacityCreditorNeeds[op] <= totalCapacityUnused[op]) {
                    // get creditors' capacity back from unused capacity if it's available
                    log('debug', 'operation:'+op+'get creditors capacity back from unused capacity, total capacity creditor needs is '+totalCapacityCreditorNeeds[op]);
                    totalCapacityUnused[op] -= totalCapacityCreditorNeeds[op];
                    // take capacity out from below capacity apps 
                    deductCapacity(capacityPrefix, totalCapacityCreditorNeeds[op], belowCapacityApps[op], belowCapacities, op, capacities);
                  }
                  else if(isNonZero(totalCapacityUnused[op]) && totalCapacityCreditorNeeds[op] > totalCapacityUnused[op]) {
                    log('debug', 'operation:'+op+': get creditors capacity back from under capacity and bad apps');
                    // if unused capacity cannot satisfy then get it back from over capacity apps 
                    totalCapacityCreditorNeeds[op] -= totalCapacityUnused[op];
                    totalCapacityUnused[op] = 0; // all the extra capacity has been used up to satisfy creditors' needs
                    // take capacity of below capacity apps away
                    setCapacities(capacityPrefix, belowCapacityApps[op], op, belowCapacities, capacities);
                    // take capacity back from over capacity apps
                    log('debug', 'over capacity apps op:'+op+":"+overCapacityApps[op]);
                    deductCapacity(capacityPrefix, totalCapacityCreditorNeeds[op], overCapacityApps[op], null, op, capacities);
                  }
                  else {
                    // if unused capacity is not available, take capacity back evenly from over capacity apps to me
                    log('debug', 'operation:'+op+': get creditors capacity back from bad apps');
                    deductCapacity(capacityPrefix, totalCapacityCreditorNeeds[op], overCapacityApps[op], null, op, capacities);
                  }
              }

              // some apps are over capacity while some apps are willing to give extra capacity
              if(overCapacityApps[op] != undefined && overCapacityApps[op].length != 0 && isNonZero(totalCapacityUnused[op])) {
                // take capacity of over allocated apps away
                for (var i = 0; i < belowCapacityApps[op].length; i++) {
                    var app = belowCapacityApps[op][i];
                    log("debug", 'debug below capacity apps '+app+'-'+op+' capacity to ');
                }
                for (var i = 0; i < overCapacityApps[op].length; i++) {
                    var app = overCapacityApps[op][i];
                    log("debug", 'debug over capacity apps '+app+'-'+op+' capacity to ');
                }

		            // set below capacity apps to below capacities
                setCapacities(capacityPrefix, belowCapacityApps[op], op, belowCapacities, capacities);
  		          log('debug', 'each app in '+overCapacityApps[op]+' gets capacity of '+totalCapacityUnused[op]);
                distributeCapacityEvenly(capacityPrefix, totalCapacityUnused[op], overCapacityApps[op], op, capacities);
              }
            }
            reset_tokens(option); 
          });
        });
      });
    });
  });
}
