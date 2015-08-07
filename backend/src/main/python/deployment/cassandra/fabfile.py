#!/usr/bin/python
"""
Overview: http://fabric.readthedocs.org/en/1.0.0/tutorial.html
Execution: http://fabric.readthedocs.org/en/1.0.0/usage/execution.html

Author(s): fei@highform.com
"""

import csv
import os
import os.path
import tempfile
import collections
import yaml
import sys
import getpass
import time

from fabric.api import *
from fabric.tasks import *
from fabric.decorators import * 
from fabric.colors import *

sys.path.append("..")
from utils import deployments

"""
env variables
"""

# Global dictionary containing map of roles to hosts.
# A role provided as an argument to a task must be part of this list;
# otherwise fabric will throw an error saying that role(s) do not exist.
env.roledefs = {
}

#environment variable definitions
env.user='ubuntu'
env.use_ssh_config = True
env.s3_bucket = 's3://brp-admin/config/'
env.root = "/mnt"
#env.project = "cassandra"
#env.project_root = os.path.join(env.root, env.project)
env.tmp_dir = "/mnt/tmp"
env.cassandra_dir = os.path.join(env.root, "cassandra")
env.logs_dir = os.path.join(env.cassandra_dir, "logs")
env.conf_dir = os.path.join(env.cassandra_dir, "conf")
env.cassandra_tar = "s3://br-resources/cassandra/apache-cassandra-1.2.4-bin.tar.gz"

@task
def realm(deploy_realm):
  """
  Set the realm for deployment
  """
  assert deploy_realm
  env.deploy_realm = deploy_realm

@task
def role(deploy_role, regions=None, zones=None, instances=None):
  """
  Set the role for deployment. Must be non empty
  """
  assert deploy_role
  env.deploy_role = deploy_role

  # This allows fabric to generate hostlist from role
  env.roledefs[deploy_role] = deployments._get_hosts(deploy_role)
  env.roles = [deploy_role]

  env.deploy_regions = regions
  env.deploy_zones = zones
  env.deploy_instances = instances

@task 
def project(deploy_project):
  """
  Set the project used for deployment
  """
  assert deploy_project
  env.deploy_project = deploy_project

@task 
def region(deploy_region):
  """
  Set the region used for deployment
  """
  assert deploy_region
  env.deploy_region = deploy_region

@task
def push_s3cfg():
  """
  Push local s3cfg file to remote host
  """ 
  env.user = "ubuntu"
  deployments._push_s3cfg()

@task
def deploy_s3cmd():
  """
  Install s3cmd and push s3cfg config to local host
  """
  deployments._bootstrap_s3cmd()

@task
def deploy_dist():
  """
  Push backend dist
  """
  deployments._deploy_dist()

@task
def deploy_scripts():
  """
  Push production scripts
  """
  deployments._deploy_scripts()

@task
def deploy_monitor():
  """
  Deploy monitoring
  """
  deployments._deploy_monitor()

@task
def enable_root_login():
  """
  Copy ssh authorized_keys to enable root login
  """
  deployments._enable_root_login()

@task
def launch_cassandra_node(deploy_name, region = "us-east-1"):
  """
  Launch new instance and deploy queryserver
  """
  assert deploy_name
  if region == None:
    if hasattr(env, 'deploy_region') and env.deploy_region != None:
      region = env.deploy_region
    else:
      region = "us-east-1"

  role = ""
  if hasattr(env, 'deploy_role') and env.deploy_role != None:
    role == env.deploy_role 

  tags = {
       "Name" : deploy_name,
       "Project" : "bloomstore",
       "Role" : role 
  }
  instance = deployments._launch_ec2_server(region=region, instance_type="m1.xlarge", tags=tags, security_groups = ["BloomStore"])
  return instance

@task 
def launch_and_add_cassandra_node(deploy_name, seed=None, region = "us-east-1", token=None):
  """
  Launch new instance and add node to cassandra cluster
  """
  instance = launch_cassandra_node(deploy_name = deploy_name, region = region)

  print yellow("Waiting to connect to instance "), instance.public_dns_name

  if seed == None:
    seed = instance.private_ip_address

  print yellow("Launching cassandra node with seed "), seed 
  time.sleep(10)
  try:
    with settings(host_string=instance.public_dns_name, user="ubuntu"):
      add_cassandra_node(seed, token)
  except SystemExit:
    # Retry in case of error
    print red("There was an error in deploying cassandra node. Retrying...")
    time.sleep(120)
    with settings(host_string=instance.public_dns_name, user="ubuntu"):
      add_cassandra_node(seed, token)
    pass

@task
def launch_cassandra_cluster(pagedb_hosts, seed):
  """
  Launch a new cassandra cluster with given nodes
  """
  hosts = pagedb_hosts.split(";")
  i = 0
  num = len(hosts)
  print hosts, num
  for host in hosts:
    # token range for Murmur3Partitioner is [-2**63, 2**63-1]
    token = (i * (2**64) / num) - (2**63)
    i = i + 1
    print token
    with settings(host_string=host, user="ubuntu"):
      add_cassandra_node(seed, token)
      print green("Waiting 2 min after starting cassandra node %s" % str(host))
      time.sleep(120)
    pass


@task
def push_cassandra_config(pagedb_hosts, seed):
  """
  Generates yaml configuration and pushes to each node. Assumes cassandra is already installed on each node 
  """
  hosts = pagedb_hosts.split(";")
  i = 0
  num = len(hosts)
  print hosts, num
  for host in hosts:
    # token range for Murmur3Partitioner is [-2**63, 2**63-1]
    token = (i * (2**64) / num) - (2**63)
    i = i + 1
    print token
    with settings(host_string=host, user="ubuntu"):
      _push_cassandra_config(token, seed)
      _start_cassandra()
      print green("Waiting 2 min after starting cassandra node %s" % str(host))
      time.sleep(120)
    pass


@task
def add_cassandra_node(seed, token=None):
  """
  Add new node to existing cassandra cluster
  """
  _bootstrap_basic()
  _setup_raid0("/dev/xvdb", "/dev/xvdc", "/dev/xvdd", "/dev/xvde")
  _install_cassandra()
  _push_cassandra_config(token, seed)
  _start_cassandra()

def _bootstrap_basic():
  """
  Perform basic operations on the AMI (e.g. import security keys, configure timezone)
  """
  # https://forums.aws.amazon.com/thread.jspa?messageID=341020
  sudo("gpg --keyserver keyserver.ubuntu.com --recv-key 40976EAF437D05B5")
  sudo("gpg -a --export 40976EAF437D05B5 | apt-key add -")

  sudo("apt-get update")

  # fix timezone
  sudo("echo UTC | tee /etc/timezone")
  sudo("dpkg-reconfigure --frontend noninteractive tzdata")
  sudo("apt-get install -y --force-yes ntp cronolog dstat htop unzip nmap apache2-utils siege logtail s3cmd")
  sudo("apt-get install -y --force-yes python-pip libxml2-dev libxslt-dev python-dev python-protobuf")
  sudo("pip install simplejson pycassa lxml cssselect beautifulsoup4 fabric boto pytz")

def _setup_raid0(*drives):
  """
  Setup raid0 on node
  """
  sudo("apt-get install mdadm --no-install-recommends")
  for drive in drives:
    with settings(warn_only=True):
      sudo("umount %s " % drive)
    sudo("""echo -e "n\np\n1\n \n \nt\nfd\nw" | fdisk %s""" % drive) 
    pass
  sudo("/usr/bin/yes | mdadm --create --verbose --auto=yes /dev/md0 --level=0 --raid-devices=%d %s" % (len(drives), " ".join(drives) ))
  sudo("apt-get install -y --force-yes xfsprogs")
  sudo("mkfs -t xfs /dev/md0")
  sudo("mount /dev/md0 /mnt")
  sudo("chown -R ubuntu:ubuntu /mnt")

def _install_cassandra():
  with cd("%(root)s" % env):
    run("s3cmd get %(cassandra_tar)s" % env)
    run("tar xvzf apache-cassandra-1.2.4-bin.tar.gz; rm apache-cassandra-1.2.4-bin.tar.gz")
    run("ln -s apache-cassandra-1.2.4 cassandra_latest")
    run("mkdir -p %(cassandra_dir)s" % env)

def _generate_cassandra_yaml(token, seed ):
  local_ip_addr = run("curl 169.254.169.254/latest/meta-data/local-ipv4")
  local_ip = str(local_ip_addr)
  public_ip_addr = run("curl 169.254.169.254/latest/meta-data/public-ipv4")
  public_ip = str(public_ip_addr)
  template = yaml.safe_load(open("conf/cassandra.yaml", "r"))
  template["listen_address"] = local_ip
  template["broadcast_address"] = local_ip
  if seed != None:
    template["seed_provider"][0]["parameters"][0]["seeds"] = seed
  if token != None:
    template["initial_token"] = int(token)
  template["endpoint_snitch"] = "Ec2Snitch"

  _, f = tempfile.mkstemp(prefix="cassandra-"+ local_ip +"-", suffix=".yaml")
  with open(f, "w") as fd:
    yaml.safe_dump(template, fd, default_flow_style=False)

  print "yaml file:", f
  return f

def _push_cassandra_config(token, seed):
  """ Initialize and push cassandra config for node """

  with settings(warn_only=True):
    run("pkill -9 -f CassandraDaemon")
  deployments._rsync("conf/cassandra-env.sh", "/mnt/cassandra_latest/conf/cassandra-env.sh")
  f = _generate_cassandra_yaml(token, seed)
  print "yaml file:", f
  put(f, "/mnt/cassandra_latest/conf/cassandra.yaml")
  deployments._rsync("conf/log4j-server.properties", "/mnt/cassandra_latest/conf/log4j-server.properties")

def _start_cassandra():
  with settings(warn_only=True):
    run("pkill -9 -f CassandraDaemon")
  run("nohup /mnt/cassandra_latest/bin/cassandra > /dev/null &", pty=False)

