#!/usr/bin/python

import sys, os, time, sched, re, getopt, fnmatch
import types, resource, getpass, glob, linecache
import copy

name_prefix = 'simple_diskstats_'
bytes_read = name_prefix + 'bytes_read'
bytes_written = name_prefix + 'bytes_written'
params = {
    'filepath' : '/proc/diskstats'
}
curr_metrics = {
  bytes_read : {
    'value' : 0,
    'time' : 0
  },
  bytes_written : {
    'value' : 0,
    'time' : 0
  }
}
last_metrics = copy.deepcopy(curr_metrics)

diskstat_indices = {
  bytes_read : 5,
  bytes_written : 9
} 

def get_metrics(name):
  global curr_metrics
  global last_metrics
  global params
  global fd

  fd.seek(0)
  last_metrics[name] = copy.deepcopy(curr_metrics[name])
  curr_metrics[name] = {
    'value' : 0,
    'time' : 0
  }
  for line in fd.readlines():
    l = line.split()
    if len(l) < 13: continue
    if l[5] == '0' and l[9] == '0': continue
    if l[3:] == ['0',] * 11: continue
    curr_metrics[name]['value'] = curr_metrics[name]['value'] + long(l[diskstat_indices[name]])
  curr_metrics[name]['time'] = time.time()

def get_delta(name):
  get_metrics(name)
  '''Translate number of sectors to number of bytes.'''
  delta = (curr_metrics[name]['value'] - last_metrics[name]['value']) * 512.0 / (curr_metrics[name]['time'] - last_metrics[name]['time'])
  return delta

def metric_init(lparams):
  '''Initialize metric descriptors.'''
  global params
  global fd

  '''Set parameters.'''
  for key in lparams:
    params[key] = lparams[key]
  
  if not os.path.exists(params['filepath']):
    raise Exception, 'File %s does not exist' % filename
  fd = open(params['filepath'], 'r', 0)

  time_max = 60
  groups = 'diskstats_total'
  descriptors = [
    {
      'name': name_prefix + 'bytes_read',
      'call_back': get_delta,
      'time_max': time_max,
      'value_type': 'float',
      'units': 'Bytes',
      'slope': 'both',
      'format': '%f',
      'description': 'Bytes Read',
      'groups': groups
    },
    {
      'name': name_prefix + 'bytes_written',
      'call_back': get_delta,
      'time_max': time_max,
      'value_type': 'float',
      'units': 'Bytes',
      'slope': 'both',
      'format': '%f',
      'description': 'Bytes Written',
      'groups': groups
    }
  ]
  return descriptors

def metric_cleanup():
  '''Cleanup.'''
  fd.close()

if __name__ == '__main__':
  global params
  descriptors = metric_init(params)

  while True:
    for d in descriptors:
      print (('%s = %s') % (d['name'], d['format'])) % (d['call_back'](d['name']))
    time.sleep(1)
    print ''
