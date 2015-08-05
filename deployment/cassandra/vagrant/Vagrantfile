# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "base"
  config.vm.box_url = "http://files.vagrantup.com/precise64.box"
  config.vm.provision :puppet, :module_path => "modules" do |puppet|
    puppet.manifests_path = "manifests"
    puppet.manifest_file  = "default.pp"
    puppet.module_path = "modules"
  end
  config.vm.define :node1 do |node1|
    node1.vm.network "private_network", ip: "192.168.7.12"
    node1.vm.hostname = "node1.local"
    node1.vm.network "forwarded_port", guest: 1414, host: 7444, auto_correct: true
    node1.vm.network "forwarded_port", guest: 9042, host: 9042, auto_correct: true
    node1.vm.network "forwarded_port", guest: 9160, host: 9160, auto_correct: true
    node1.vm.network "forwarded_port", guest: 7199, host: 7199, auto_correct: true
  end
  config.vm.define :node2 do |node2|
    node2.vm.network "private_network", ip: "192.168.7.13"
    node2.vm.hostname = "node2.local"
    node2.vm.network "forwarded_port", guest: 1414, host: 7445, auto_correct: true
  end
  config.vm.define :node3 do |node3|
    node3.vm.network "private_network", ip: "192.168.7.14"
    node3.vm.hostname = "node3.local"
    node3.vm.network "forwarded_port", guest: 1414, host: 7446, auto_correct: true
  end
end
