#!/usr/bin/python
"""
Overview: http://fabric.readthedocs.org/en/1.0.0/tutorial.html
Execution: http://fabric.readthedocs.org/en/1.0.0/usage/execution.html

Author(s): Prateek Gupta (prateek@bloomreach.com)
"""

import os
import os.path
import tempfile
import yaml
import sys
import time
import traceback

from fabric.api import *
from fabric.tasks import *
from fabric.decorators import *
from fabric.colors import *
from fabric.contrib.files import exists
import fabric.exceptions

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
env.cassandra_installed_dir = os.path.join(env.root, "cassandra_latest")
env.cassandra_bin = os.path.join(env.cassandra_installed_dir, "bin")
env.restart = os.path.join(env.cassandra_bin, "restart.sh")
env.nodetool = os.path.join(env.cassandra_bin, "nodetool")
env.logs_dir = os.path.join(env.cassandra_dir, "logs")
env.conf_dir = os.path.join(env.cassandra_installed_dir, "conf")
env.cassandra_tar = "s3://br-resources/cassandra/apache-cassandra-2.0.4-SNAPSHOT-20140620-bin.tar.gz"
env.cassandra_local_tar = "apache-cassandra-2.0.4-SNAPSHOT-20140117-bin.tar.gz"
env.cassandra_ver = "apache-cassandra-2.0.4-SNAPSHOT"
BR_TOP = "$dist/../../../.."
env.bstore_tools = "$BR_TOP/work/src/bstore/tools"
env.bstore_scripts = os.path.join(env.bstore_tools, "scripts")
env.src_topology = "conf/prod-cassandra-topology.properties"

azs = { 'us-east-1' : ['us-east-1c', 'us-east-1d', 'us-east-1e'], 'us-west-1' : ['us-west-1a', 'us-west-1b', 'us-west-1c'] }
datacenter_ganglia_ports = { 'bstore_staging' : '8662',
                             'bstore_stagingfrontend' : '8663',
                             'pagedb-backend' : '8664',
                             'pagedb-frontend' : '8665',
			     'userdb-stagingfrontend' : '8666',
			     'userdb-stagingbackend' : '8666',
			     'userdb-frontend' : '8667',
			     'userdb-backend' : '8667'
			   }

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
  _deploy_monitor()

@task
def enable_root_login():
  """
  Copy ssh authorized_keys to enable root login
  """
  deployments._enable_root_login()

def _deploy_monitor():
  """
  Deploy monitoring
  """
  deployments._deploy_munin()

  deployments._rsync("../../scripts/ops-tools/monitoring/nrpe-ub.cfg", "/tmp")
  deployments._rsync("../../scripts/ops-tools/monitoring/server-configs/nagios-plugins/lib/", "/tmp/nagios_plugins")
  deployments._rsync("../../scripts/ops-tools/monitoring/server-configs/nagios-plugins/config/", "/tmp/nagios_config")

  deployments._install_nagios()


@task
def push_topology_file(src_topology=env.src_topology):
  """
      Push topology file to nodes.

      $ fab -H ip1,ip2,ip3,ip4,ip5 push_topology_file

      It can be combined with print_hosts command, for example

      $ fab -P print_hosts:datacenter=bstore_staging push_topology_file
  """
  sudo("chown -R ubuntu:ubuntu %(root)s" % env)
  run("mkdir -p %(tmp_dir)s" % env)
  run("cp %(conf_dir)s/cassandra-topology.properties %(tmp_dir)s" % env)
  deployments._rsync(src_topology, "%(conf_dir)s/cassandra-topology.properties" % env)


@task
def push_tools():
  """ Push customed tools to cassandra_bin. """
  sudo("chown -R ubuntu:ubuntu %(root)s" % env)
  run("mkdir -p %(tmp_dir)s" % env)
  assert exists(env.cassandra_bin), "Cannot find cassandra bin folder!"
  deployments._rsync(env.bstore_scripts, env.tmp_dir)
  sudo("chmod 755 %(tmp_dir)s/scripts/*" % env)
  run("mv %(tmp_dir)s/scripts/* %(cassandra_bin)s" % env)


@task
def create_placement_group(name, region="us-east-1"):
  """
    Create placement group, for example
    $ fab create_placement_group:name='prod.bstore.backend.us-east'
  """
  deployments._create_placement_group(name=name, region=region)
  print yellow("Placement group '%s' created in region %s." % (name, region))


@task
def launch_cassandra_node(deploy_name, region = "us-east-1", az = "us-east-1c", project = "bloomstore", roleval = "backend", placement_group = None, security_group="BloomStore"):
  """
  Launch new instance and deploy queryserver
  """
  assert deploy_name
  if region == None:
    if hasattr(env, 'deploy_region') and env.deploy_region != None:
      region = env.deploy_region
    else:
      region = "us-east-1"

  roleval = ""
  if hasattr(env, 'deploy_role') and env.deploy_role != None:
    roleval == env.deploy_role

  tags = {
       "Name" : deploy_name,
       "Project" : project,
       "Role" : roleval
  }

  try:
    instance = deployments._launch_ec2_server(region=region,
                                              az=az,
                                              instance_type="i2.xlarge",
                                              tags=tags,
                                              security_groups=[security_group],
                                              ami="ami-dc0625b4",
                                              placement_group=placement_group)
  except:
    time.sleep(10)
    instance = deployments._launch_ec2_server(region=region,
                                              az=az,
                                              instance_type="i2.xlarge",
                                              tags=tags,
                                              security_groups=[security_group],
                                              ami="ami-dc0625b4",
                                              placement_group=placement_group)

  return instance


@task
def launch_and_add_cassandra_node(datacenter,
                                  deploy_name,
                                  seed=None,
                                  region="us-east-1",
                                  az="us-east-1c",
                                  project="bloomstore",
                                  roleval="backend",
                                  placement_group=None,
                                  maintenance_time=(0,0)):
  """
  Launch new instance and add node to cassandra cluster
  """
  instance = launch_cassandra_node(deploy_name = deploy_name, region = region, az = az, project = project, roleval = roleval, placement_group = placement_group)

  print yellow("Waiting to connect to instance "), instance.public_dns_name

  if seed == None:
    seed = instance.private_ip_address

  print yellow("Launching cassandra node with seed "), seed
  wait_for_node(instance.public_dns_name)
  try:
    with settings(host_string=instance.public_dns_name, user="ubuntu"):
      add_cassandra_node(datacenter, seed, project, roleval, maintenance_time)
  except SystemExit:
    # Retry in case of error
    print red("There was an error in deploying cassandra node. Retrying...")
    time.sleep(120)
    with settings(host_string=instance.public_dns_name, user="ubuntu"):
      add_cassandra_node(datacenter, seed, project, roleval, maintenance_time)
    pass


@task
def launch_cassandra_cluster(datacenter, num_hosts,
                             region="us-east-1", num_azs = 1,
                             initial_seeds="", realm="test",
                             project = "bloomstore", roleval = "backend", security_group="BloomStore"):
  """
  Launch a new cassandra cluster with given nodes
  """
  # Adjust topology file according to different realm.
  if realm != 'prod':
    env.src_topology = 'conf/%s-cassandra-topology.properties' % (realm,)

  instances = []
  seeds = initial_seeds.strip().split()
  j = int(num_hosts)/int(num_azs)
  print j
  print ",".join(seeds)
  print len(seeds)
  with open(env.src_topology, "a") as f:
    for i in range(0, int(num_hosts)):
      az_id = i / j
      index = i % j
      az = azs[region][int(az_id)]
      placement_group = '{0}.{1}.{2}.{3}.{4}'.format(realm, az, project, roleval, datacenter)
      create_placement_group(placement_group, region)
      name = '{0}.{1}.{2}.cassandra.{3}.bloomreach.com'.format(index,az,realm, datacenter)
      print yellow("Launching instance %s in zone %s" %(name, az))
      instance = launch_cassandra_node(name, region, az, project, roleval, placement_group, security_group)
      instances.append(instance)
      if len(seeds) == 0 and i == 0:
        seeds.append(instance.private_ip_address)
      print green("Successfully launched instance %s" %name )
      line = '{0}={1}:{2}\n'.format(instance.private_ip_address,datacenter,az)
      f.write(line)

  env.hosts = [instance.public_dns_name for instance in instances]
  execute("initialize_cassandra_cluster", datacenter, ' '.join(seeds), project, roleval)


def _get_evenly_distributed_maintence_hours(hosts, this_host):
  """
     Get evenly distributed maintenance hours.
     For example, if we have 3 nodes, then their maintenance hour should be
     (sun, 0:00), (tue, 8:00), and (thu, 16:00)

     :rtype tuple
     :return (week, hour)
  """
  DAY_HOURS = 24
  WEEK_HOURS = 7 * DAY_HOURS

  index = hosts.index(this_host)
  cron_job_time = (index * WEEK_HOURS / len(hosts))
  week = cron_job_time / DAY_HOURS
  hour = cron_job_time % DAY_HOURS
  return week, hour


@task
@parallel
def initialize_cassandra_cluster(datacenter, seeds, project, roleval):
  wait_for_node(env.host)
  seed=seeds.split(" ")
  seed_list=",".join(seed)
  print yellow("seed list:" + seed_list)
  # equally distributing the maintenance hours
  maintenance_time = _get_evenly_distributed_maintence_hours(env.hosts, env.host_string)
  print yellow("maintenance time: %d, %d" % maintenance_time)
  try:
    with settings(user="ubuntu"):
      add_cassandra_node(datacenter, seed_list, project, roleval, maintenance_time)
  except Exception, e:
    # Retry in case of error
    print red("There was an error in deploying cassandra node. Retrying..." + repr(e))
    traceback.print_exc()
    time.sleep(120)
    with settings(user="ubuntu"):
      add_cassandra_node(datacenter, seed_list, project, roleval, maintenance_time)



@task
def push_cassandra_config(pagedb_hosts, seed, project, roleval):
  """
  Generates yaml configuration and pushes to each node. Assumes cassandra is already installed on each node
  """
  hosts = pagedb_hosts.split(";")
  i = 0
  num = len(hosts)
  print hosts, num
  for host in hosts:
    # token range for Murmur3Partitioner is [-2**63, 2**63-1]
#    token = (i * (2**64) / num) - (2**63)
#    i = i + 1
    with settings(host_string=host, user="ubuntu"):
      with settings(warn_only=True):
        r = deployments.pkill('CassandraDaemon',30)
        if r != 0:
          raise Exception("could not kill existing CassandraDaemon")

      _push_cassandra_config(seed, project, roleval)
      _start_cassandra()
      print green("Waiting 2 min after starting cassandra node %s" % str(host))
      time.sleep(120)
    pass


@task
def add_cassandra_node(datacenter, seed, project, roleval='backend', maintenance_time=(0,0)):
  """
  Add new node to existing cassandra cluster
  """
  _bootstrap_basic()
  _setup_filesystem("/dev/xvdb")
  push_s3cfg()
  _install_java7()
  _install_cassandra()
  _modify_ulimit()
  _set_swap()
  _push_restart_script()
  push_tools()
  _setup_cassandra(datacenter, seed, project, roleval)
  _start_cassandra()
  _setup_maintenace_cronjobs(*maintenance_time)
  with settings(warn_only=True):
    _deploy_monitor()

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


def _install_java7():
  """
  Install Orcale JDK 7 and set it as default
  http://www.webupd8.org/2012/01/install-oracle-java-jdk-7-in-ubuntu-via.html
  """
  print yellow("Installing java 7 ...")
  sudo("add-apt-repository ppa:webupd8team/java -y")
  sudo("apt-get update")
  sudo("echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections")
  sudo("apt-get install oracle-java7-installer -y --force-yes")
  sudo("apt-get install oracle-java7-set-default -y --force-yes")

def _setup_filesystem(*drives):
  fsDrive = "/dev/md0"
  if len(drives) > 1:
    """
    Multiple drives available, we should setup a raid0 config for this case
    """
    sudo("apt-get install mdadm --no-install-recommends")
    for drive in drives:
      with settings(warn_only=True):
        sudo("umount %s " % drive)
      sudo("""echo -e "n\np\n1\n \n \nt\nfd\nw" | fdisk %s""" % drive)
      # remove the drive from /etc/fstab
      # we have to escape / in drive,
      driveEscaped = drive.replace("/","\/");
      sudo("sed -i \'/%s/d\' /etc/fstab" % driveEscaped)
      pass
    sudo("/usr/bin/yes | mdadm --create --verbose --auto=yes %s --level=0 --raid-devices=%d %s" % (fsDrive, len(drives), " ".join(drives) ))
    sudo("echo DEVICE %s | tee /etc/mdadm/mdadm.conf" % " ".join(drives))
    sudo("mdadm --detail --scan | tee -a /etc/mdadm/mdadm.conf")
  else:
     fsDrive=drives[0]

  sudo("mke2fs -t ext4 %s" %fsDrive)
  sudo("""echo "%s /mnt ext4 defaults,nobootwait,noatime 0 2" | tee -a /etc/fstab""" %fsDrive)
  sudo("update-initramfs -u")
  sudo("mount %s /mnt" %fsDrive)
  sudo("chown -R ubuntu:ubuntu /mnt")

def _install_cassandra():
  with cd("%(root)s" % env):
    run("s3cmd get --force %(cassandra_tar)s" % env)
    run("tar xvzf %(cassandra_local_tar)s; rm %(cassandra_local_tar)s" % env)
    run("ln -s %(cassandra_ver)s cassandra_latest" % env)
    run("mkdir -p %(cassandra_dir)s" % env)

def _setup_maintenace_cronjobs(week=0, hour=0):
  assert hour < 24
  assert week < 7   # 0: sunday, 1: monday, ..., 6: saturday
  bindings = dict(env)
  bindings['hour'] = hour
  bindings['week'] = week
  run('crontab -l | { cat; echo "0 %(hour)d * * %(week)d %(nodetool)s repair"; } | crontab -' % bindings)


def _set_swap():
  sudo("dd if=/dev/zero of=/mnt/swap bs=1M count=12288")
  sudo("chmod 600 /mnt/swap")
  sudo("mkswap /mnt/swap")
  sudo("swapon /mnt/swap")

def _modify_ulimit():
  sudo("""echo "* soft nofile 200000" | sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "* hard nofile 200000" | sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root soft nofile 200000" | sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root hard nofile 200000" | sudo tee -a /etc/security/limits.conf""")

  sudo("""echo "* soft memlock 4194304"  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "* hard memlock 4194304"  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root soft memlock 4194304"  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root hard memlock 4194304"  |sudo tee -a /etc/security/limits.conf""")


  sudo("""echo "* soft as unlimited"  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "* hard as unlimited "  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root soft as unlimited"  |sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "root hard as unlimited "  |sudo tee -a /etc/security/limits.conf""")
  sudo("sysctl -w vm.max_map_count=131072")


def _generate_cassandra_yaml(seed, project, roleval):
  local_ip_addr = run("curl 169.254.169.254/latest/meta-data/local-ipv4")
  local_ip = str(local_ip_addr)
  public_ip_addr = run("curl 169.254.169.254/latest/meta-data/public-ipv4")
  public_ip = str(public_ip_addr)
  template = yaml.safe_load(open("conf/"+project+"_"+roleval+".yaml", "r"))
  template["listen_address"] = local_ip
  template["broadcast_address"] = local_ip
  if seed != None:
    template["seed_provider"][0]["parameters"][0]["seeds"] = seed
  template["num_tokens"] = 32
  template["endpoint_snitch"] = "PropertyFileSnitch"
  _, f = tempfile.mkstemp(prefix="cassandra-"+ local_ip +"-", suffix=".yaml")
  with open(f, "w") as fd:
    yaml.safe_dump(template, fd, default_flow_style=False)

  print "yaml file:", f
  return f


def _push_cassandra_config(seed, project, roleval, installed_dir=env.cassandra_installed_dir):
  """ Initialize and push cassandra config for node """

  deployments._rsync("conf/cassandra-env.sh", "%s/conf/cassandra-env.sh" % installed_dir)
  f = _generate_cassandra_yaml(seed, project, roleval)
  print "yaml file:", f
  put(f, "%s/conf/cassandra.yaml" % installed_dir)
  deployments._rsync("conf/log4j-server.properties", "%s/conf/log4j-server.properties" % installed_dir)
  deployments._rsync(env.src_topology, "%s/conf/cassandra-topology.properties" % installed_dir)

def _push_restart_script():
  """ Push the restart script to the cassandra node at /mnt location """
  deployments._rsync("./restart.sh", "/mnt/cassandra_latest/bin/")
  deployments._rsync("./stop.sh", "/mnt/cassandra_latest/bin/")

@task
def start_cassandra(host, user='ubuntu'):
  with settings(host_string=host, user=user):
    _start_cassandra()


def _start_cassandra():
  with settings(warn_only=True):
    r = deployments.pkill('CassandraDaemon', 30)
    if r != 0:
      raise Exception("could not kill existing CassandraDaemon")
  run("nohup /mnt/cassandra_latest/bin/cassandra > /dev/null &", pty=False)


@task
def print_hosts(datacenter, realm='*', region="us-east-1", instance_type='*', private_ip=False, verbose=False):
  """ print all ips given realm and datacenter. """
  hosts = deployments._get_relevant_hosts(datacenter, realm, region, instance_type, private_ip, verbose)
  print green(",".join(hosts))
  env.hosts = hosts


@task
@runs_once
def cssh():
  """ Should be concatenated with print_hosts
      $ fab print_hosts:datacenter=pagedb-frontend,instance_type='i2.xlarge' cssh
  """
  hosts = ' '.join(env.hosts)
  with settings(warn_only=True):
    if local('cssh --username ubuntu %s' % hosts).failed:
      local('csshx --login ubuntu %s' % hosts)


def _setup_cassandra(datacenter, seed, project, roleval, installed_dir=env.cassandra_installed_dir):
  # push cassandra related configs
  _push_cassandra_config(seed, project, roleval, installed_dir)
  # setup ganglia
  setup_ganglia(datacenter, installed_dir)


def _deploy_cassandra_build(bin, cassandra_local_tar, cassandra_version, datacenter, realm='*', region="us-east-1", seeds='', verbose=False, project = "bloomstore", roleval = "backend"):
  """ Deploy the customed built cassandra binary to a temp folder. """
  sudo("chown -R ubuntu:ubuntu %(root)s" % env)
  tmp_folder = env.tmp_dir
  run("mkdir -p %(tmp_folder)s" % locals())
  print green("Downloading the build %(bin)s..." % locals())
  run("s3cmd get --force %(bin)s %(tmp_folder)s/%(cassandra_local_tar)s" % locals(), quiet=True)
  run("tar xvzf %(tmp_folder)s/%(cassandra_local_tar)s -C %(tmp_folder)s; rm %(tmp_folder)s/%(cassandra_local_tar)s" % locals(), quiet=True)
  deployments._rsync("conf/cassandra-env.sh", "%(tmp_folder)s/%(cassandra_version)s/conf/cassandra-env.sh" % locals())
  # if the client does not specify the seeds, it will try to figure it out from datacenter and realm
  if not seeds:
    seeds = ','.join(deployments._get_relevant_hosts(datacenter=datacenter, realm=realm, region=region, private_ip=True))
  print green("Seeds: " + seeds)
  installed_dir = "%(tmp_folder)s/%(cassandra_version)s" % locals()
  _setup_cassandra(datacenter, seeds, project, roleval, installed_dir)
  return installed_dir


def _start_cassandra_process():
  """ Restarting the node """
  print green("Restarting CassandraDaemon")
  run(env.restart, pty=False)


def _switch_cassandra_running_build(casssandra_target_running_build):
  """ 'Hot switch' the cassandra running build with a replaced build.
      Steps:
        1. Terminate CassandraDaemon
        2. Relink cassandra_latest to deployed build
        3. Restart CassandraDaemon
  """
  print yellow("Switching to build: " + casssandra_target_running_build)
  print green("Terminating CassandraDaemon")
  r = deployments.pkill('CassandraDaemon', 30, wait_secs=30)
  if r != 0:
    raise Exception("could not kill existing CassandraDaemon")

  with cd("%(root)s" % env):
    run("unlink cassandra_latest")
    run("ln -s %(casssandra_target_running_build)s cassandra_latest" % locals())
    run("mkdir -p %(cassandra_dir)s" % env)

  _start_cassandra_process()


def _wait_until_cassandra_is_up(host, timeout=180):
  """ Use telnet localhost 9160 to see if CassandraDaemon is up and running. """
  print green("Waiting for CassandraDaemon at %(host)s to be up" % locals())
  timeout = int(timeout)
  while run("exec 6<>/dev/tcp/localhost/9160", warn_only=True, quiet=True).failed:
    # sleep for a while and try again.
    run("sleep 1", quiet=True)
    timeout -= 1
    sys.stdout.write('.')
    sys.stdout.flush()
    if timeout <= 0:
      print red("\nWARNING: CassandraDaemon at %(host)s is not restarted, please fix it ASAP" % locals())
      raise fabric.exceptions.CommandTimeout("CassandraDaemon at %(host)s is not restarted, please fix it ASAP" % locals())

  print green("\nCassandraDaemon at %(host)s is UP." % locals())


def _move_deployed_build_to_root(deployed_build_folder):
  """ Move deployed build folder to root aka /mnt """
  basename = os.path.basename(deployed_build_folder)
  root = env.root
  today = time.strftime('%Y%m%dZ%H%M')
  casssandra_build_destination = "%(root)s/%(basename)s-d%(today)s" % locals()
  run("mv %(deployed_build_folder)s %(casssandra_build_destination)s" % locals())
  return casssandra_build_destination

@task
def stop_cassandra_process():
  """
  Stop the protocol listeners for gossip, thrift and binary
  Stop any compaction and index building
  Stop backups
  Now drain
  Should be safe to kill now
  Kill the process of CassandraDaemon assuming there is the process
  """
  run("%(nodetool)s disablebinary" % env)
  run("%(nodetool)s disablethrift" % env)
  run("%(nodetool)s disablegossip" % env)
  run("%(nodetool)s disablebackup" % env)
  run("%(nodetool)s stop compaction" % env)
  run("%(nodetool)s stop index_build" % env)
  run("%(nodetool)s drain" % env)
  r = deployments.pkill('CassandraDaemon', 30, wait_secs=30)
  if r != 0:
    raise Exception("could not kill existing CassandraDaemon")


@task
def start_cassandra_process():
  """ Start cassandra process and wait until the server is up and running assuming there is no cassandra process running """
  _start_cassandra_process()
  _wait_until_cassandra_is_up(host=env.host)


@task
def restart_cassandra_process():
  """ Restart cassandra process, esp useful when combined with print_hosts.
      Example:
        # Do a rolling restart for frontend cluster

        $ fab print_hosts:datacenter=pagedb-frontend restart_cassandra_process
  """
  stop_cassandra_process()
  start_cassandra_process()


@task
def wait_until_cassandra_is_up(timeout=180):
  """ Wait for CassandraDaemon to be up and running. """
  _wait_until_cassandra_is_up(host=env.host, timeout=timeout)


@task
def switch_cassandra_running_build(casssandra_target_running_build='/mnt/apache-cassandra-2.0.1'):
  """ This command will terminate CassandraDaemon, relink cassandra_latest, and restart cassandra """
  _switch_cassandra_running_build(casssandra_target_running_build)
  _wait_until_cassandra_is_up(host=env.host)


@task
def recover_bad_node(force=False):
  """ Recover the bad node assuming it is in a bad state. """
  if not force and not run("exec 6<>/dev/tcp/localhost/9160", warn_only=True, quiet=True).failed:
    print green("Your cassandra process is fine, we don't need to recover it.")
    return
  # If it is in a bad state, restart it.
  stop_cassandra_process()
  start_cassandra_process()


@task
def deploy_cassandra_build(bin, datacenter, realm='*', region="us-east-1", seeds='', timeout=300, verbose=False, project = "bloomstore", roleval = "backend"):
  """ Deploy the cassandra customed build binary to one node. """
  cassandra_local_tar = os.path.basename(bin)
  cassandra_version = '-'.join(cassandra_local_tar.split('-')[:4])
  if realm != 'prod':
    env.src_topology = 'conf/%s-cassandra-topology.properties' % (realm,)
  cassandra_tmp_folder = _deploy_cassandra_build(bin, cassandra_local_tar, cassandra_version,
                                                 datacenter, realm=realm, region=region, seeds=seeds, verbose=verbose, project = project, roleval = roleval)
  serving_folder = _move_deployed_build_to_root(cassandra_tmp_folder)
  _switch_cassandra_running_build(serving_folder)
  _wait_until_cassandra_is_up(env.host, timeout=timeout)


@task
def wait_for_node(host):
  while True:
    try:
      with settings(host_string=host, warn_only=True):
        print yellow("checking "+host+" ...")
        echo_command_output = run("echo check")
        if echo_command_output.find("check") >= 0:
          print green(host + " UP")
          return True
    except:
      print yellow(host + "...offline")
      time.sleep(1)


@task
def setup_ganglia(datacenter, installed_dir=env.cassandra_installed_dir):
  """
  Install ganglia monitoring for Cassandra
  staging backend:  port 8662
  staging frontend: port 8663
  prod backend:     port 8664
  prod frontend:    port 8665
  """
  try:
    ganglia_port = datacenter_ganglia_ports[datacenter]
  except KeyError as err:
    print red("Cannot find matching ganglia port: {}".format(err))
    return

  print yellow("Installing ganglia monitoring using port " + ganglia_port + "...")

  lib_dir = os.path.join(installed_dir, "lib")
  conf_dir = os.path.join(installed_dir, "conf")

  sudo("apt-get update")
  with settings(warn_only=True):
    sudo("apt-get install -y --force-yes ganglia-monitor")
  deployments._rsync("$BR_TOP/tools/3rd_party_libs/cassandra/jmxetric-1.0.4.jar", "%s/" % lib_dir)
  deployments._rsync("$BR_TOP/tools/3rd_party_libs/cassandra/gmetric4j-1.0.3.jar", "%s/" % lib_dir)
  deployments._rsync("$BR_TOP/tools/3rd_party_libs/cassandra/oncrpc-1.0.7.jar", "%s/" % lib_dir)
  deployments._rsync("conf/cassandra-env.sh", "%s/cassandra-env.sh" % env.tmp_dir)
  deployments._rsync("conf/jmxetric.xml", "%s/jmxetric.xml" % env.tmp_dir)
  deployments._rsync("conf/ganglia/gmond.conf", "%s/gmond.conf" % env.tmp_dir)
  deployments._rsync("conf/ganglia/conf.d/modpython.conf", "%s/modpython.conf" % env.tmp_dir)
  deployments._rsync("conf/ganglia/conf.d/simple_diskstats.conf", "%s/simple_diskstats.conf" % env.tmp_dir)
  deployments._rsync("conf/ganglia/python_modules/simple_diskstats.py", "%s/simple_diskstats.py" % env.tmp_dir)
  sudo("mv %s/cassandra-env.sh %s/cassandra-env.sh" % (env.tmp_dir, conf_dir))
  sudo("mv %s/jmxetric.xml %s/jmxetric.xml" % (env.tmp_dir, conf_dir))
  sudo("mv %s/gmond.conf /etc/ganglia/gmond.conf" % env.tmp_dir)
  sudo("mkdir -p /etc/ganglia/conf.d 1>/dev/null")
  sudo("mv %s/modpython.conf /etc/ganglia/conf.d/modpython.conf" % env.tmp_dir)
  sudo("mv %s/simple_diskstats.conf /etc/ganglia/conf.d/simple_diskstats.conf" % env.tmp_dir)
  sudo("mkdir -p /usr/lib/ganglia/python_modules 1>/dev/null")
  sudo("mv %s/simple_diskstats.py /usr/lib/ganglia/python_modules/simple_diskstats.py" % env.tmp_dir)
  sudo("sed -i -e s/'<SEND_PORT>'/" + ganglia_port + "/g %s/cassandra-env.sh" % conf_dir)
  sudo("sed -i -e s/'<SEND_PORT>'/" + ganglia_port + "/g %s/jmxetric.xml" % conf_dir)
  sudo("sed -i -e s/'<SEND_PORT>'/" + ganglia_port + "/g /etc/ganglia/gmond.conf")
  sudo("sed -i -e s/'<HOST_LOCATION>'/" + env.host + "/g /etc/ganglia/gmond.conf")

  sudo("sudo /etc/init.d/ganglia-monitor restart")


@task
def launch_ratelimiter_node(deploy_name, region = "us-east-1", az = "us-east-1c", project = "bloomstore", roleval = "backend"):
  """
  Launch new rate limiter node.
  """
  assert deploy_name

  tags = {
       "Name" : deploy_name,
       "Project" : project,
       "Role" : roleval
  }
  try:
    instance = deployments._launch_ec2_server(region=region,
                                              az=az,
                                              instance_type="c3.large",
                                              tags=tags,
                                              security_groups=["BloomStore"],
                                              key_name = "gsg-keypair",
                                              ami="ami-dc0625b4")
  except:
    time.sleep(10)
    instance = deployments._launch_ec2_server(region=region,
                                              az=az,
                                              instance_type="c3.large",
                                              tags=tags,
                                              security_groups=["BloomStore"],
                                              key_name = "gsg-keypair",
                                              ami="ami-dc0625b4")

  env.hosts = [instance.public_dns_name]
  return instance


@task
def setup_ratelimiter():
  ''' Setup the machine of rate limiter. '''
  sudo('apt-get update')
  run('s3cmd get -f s3://br-software/redis-2.6.17.tar.gz')
  run('tar xvfz redis-2.6.17.tar.gz')
  sudo('apt-get -y --force-yes install python-software-properties')
  sudo('add-apt-repository ppa:chris-lea/node.js -y')
  sudo('apt-get update')
  sudo('apt-get -y --force-yes install make')
  sudo('apt-get -y --force-yes install nginx')
  sudo('apt-get -y --force-yes install nodejs')
  sudo('cd redis-2.6.17 && make')
  sudo('npm install redis --global')
  sudo('npm install forever --global')
  sudo('npm install socket.io --global')

  sudo("""echo "* soft nofile 200000" | sudo tee -a /etc/security/limits.conf""")
  sudo("""echo "* hard nofile 200000" | sudo tee -a /etc/security/limits.conf""")

  # setup PATH
  run("echo PATH=$PATH:/home/ubuntu/redis-2.6.17/src >> ~/.bashrc")

  # setup folders
  sudo('chown ubuntu:ubuntu /mnt')
  #run('mkdir -p /mnt/node')
  run('mkdir -p /mnt/logs')
  run('mkdir -p /mnt/logs/redis')
  run('mkdir -p /mnt/redis')

  # setup redis
  put('ratelimiter/*', '/mnt/')
  run('ln -s /mnt/bps-rate-limiter /mnt/node')
  run('mkdir -p /mnt/node/logs')
  run('mv /mnt/redis.conf /home/ubuntu/redis-2.6.17/redis.conf')
  run('/home/ubuntu/redis-2.6.17/src/redis-server /home/ubuntu/redis-2.6.17/redis.conf')

  with cd("/mnt/node"):
    sudo('npm install connect')
    sudo('npm install connect-route')
    sudo('npm install ejs')
    sudo('npm install express')

  sudo('chmod +x /mnt/node/*')
  run('/mnt/node/init_redis')
  run('/mnt/node/restore_redis')
  run('/mnt/node/reset_node')

  # setup app.js
  run("""echo '''#!/bin/sh -e
set -e
DAEMON=/mnt/node/app.js
FOREVER_LOG=/mnt/node/logs/forever.log
STDOUT_LOG=/mnt/node/logs/stdout.log
STDERR_LOG=/mnt/node/logs/stderr.log
DEFAULT_PORT=8080
case "$1" in
start) forever -l $FOREVER_LOG -o $STDOUT_LOG -e $STDERR_LOG -a start $DAEMON $DEFAULT_PORT;;
stop) forever stop $DAEMON ;;
force-reload|restart)
forever restart $DAEMON ;;
*) echo "Usage: /etc/init.d/node {start|stop|restart|force-reload}"
exit 1
;;
esac
exit 0''' > ~/node""")
  sudo('chown root:root ~/node')
  sudo('mv ~/node /etc/init.d/node')
  sudo('chmod 755 /etc/init.d/node')
  sudo('/etc/init.d/node start')

  # setup nginx
  run("""echo '''upstream nodes {
    server localhost:8080;
    server localhost:8081;
}

server {
    listen 80;
    server_name ratelimiter.bloomreach.com;

    root /mnt/node/public;

    location / {
        proxy_pass http://nodes;
        proxy_redirect off;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}''' >  ~/node.conf""")
  sudo('chown root:root ~/node.conf')
  sudo('mv ~/node.conf /etc/nginx/sites-available/')
  sudo('rm /etc/nginx/sites-enabled/default')
  sudo('ln -s /etc/nginx/sites-available/node.conf /etc/nginx/sites-enabled/default')
  sudo('service nginx restart')
