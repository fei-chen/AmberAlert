class cassandra {
  exec {
    "extract_cassandra":
    command => "tar xfz /vagrant/apache-cassandra-2.0.4-SNAPSHOT-bin.tar.gz",
    cwd => "/vagrant",
    user => "vagrant",
    path    => "/usr/bin/:/bin/",
    require => Exec["accept_license"],
    creates => "/vagrant/apache-cassandra-2.0.4-SNAPSHOT",
  }

  file {
    "cassandra-env.sh":
    path => "/vagrant/apache-cassandra-2.0.4-SNAPSHOT/conf/cassandra-env.sh",
    ensure => file,
    source => "puppet:///modules/cassandra/cassandra-env.sh",
    require => Exec["extract_cassandra"],
    owner => vagrant,
  }

  file {
    "cassandra.yaml":
    path => "/home/vagrant/cassandra.yaml",
    ensure => file,
    content => template('cassandra/cassandra.yaml.erb'),
    require => Exec["extract_cassandra"],
    owner => vagrant,
  }

  file {
    "cassandra-topology.properties":
    path => "/vagrant/apache-cassandra-2.0.4-SNAPSHOT/conf/cassandra-topology.properties",
    ensure => file,
    source => "puppet:///modules/cassandra/cassandra-topology.properties",
    require => Exec["extract_cassandra"],
    owner => vagrant,
  }

  file {
    "log4j-server.properties":
    path => "/vagrant/apache-cassandra-2.0.4-SNAPSHOT/conf/log4j-server.properties",
    ensure => file,
    source => "puppet:///modules/cassandra/log4j-server.properties",
    require => Exec["extract_cassandra"],
    owner => vagrant,
  }

  exec {
    "start-cassandra":
    command => 'nohup /vagrant/apache-cassandra-2.0.4-SNAPSHOT/bin/cassandra &',
    cwd => "/home/vagrant",
    user => "vagrant",
    path    => "/usr/bin/:/bin/",
    require => [ Package[oracle-java7-installer], File["cassandra-env.sh"], File["cassandra.yaml"], File["cassandra-topology.properties"], File["log4j-server.properties"] ],
  }
}