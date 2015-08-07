#!/bin/bash

# Author: Fei Chen
# Script to install ElasticSearch

set -x
set -o pipefail -o errexit -o nounset

# download url of elasticsearch
es_url="http://download.elasticsearch.org/elasticsearch/elasticsearch"

# use latest elasticsearch distribution
es_file="elasticsearch-0.90.3.tar.gz"

dir=$(cd $(dirname $0); pwd)
now=$(date -u '+%Y%m%dT%H%M%SZ')

remote=
buildtag=$now

usage()
{
cat << EOF
usage: $0 options

Deploy code to TPM servers

OPTIONS:
   -h      Show this message
   -r      Remote host  (Required)
   -b      Build tag (Default - $buildtag)
EOF
}

# parse options to get args
while getopts "h:r:b:" OPTION
do
     case $OPTION in
         h)
             usage
             exit 1
             ;;
         r)
             remote="$OPTARG";;
         b)
             buildtag="$OPTARG";;
         ?)
             ;;
     esac
done

if [[ -z $remote ]]
then
    echo 'Host not specified'
    usage
    exit 1
fi

echo ""
echo "Remote machine: $remote"
echo "Tag: $buildtag"

echo "Ready to deploy, stop now for cancelling. (sleep for 5 secs)"
sleep 5

remotedir="/mnt/elasticsearch-$buildtag"

# Create tpm folder
ssh -t ubuntu@${remote} "sudo chown ubuntu:ubuntu /mnt"
ssh ubuntu@${remote} "mkdir -p ${remotedir}"

# Install elasticsearch
ssh ubuntu@${remote} "cd ${remotedir}; wget ${es_url}/${es_file} >> log.txt 2>&1; tar -xvf ${es_file}; rm ${es_file}"
es=$(ssh ubuntu@${remote} "cd ${remotedir}; ls | grep elasticsearch")

# Create soft link
remoteesdir="/mnt/elasticsearch"
ssh ubuntu@${remote} "ln -s ${remotedir}/${es} ${remoteesdir}"

# Create folders
ssh ubuntu@${remote} "mkdir -p ${remoteesdir}/data ${remoteesdir}/work ${remoteesdir}/logs ${remoteesdir}/plugins"

# Change system limits
ssh -t ubuntu@${remote} "sudo chmod 666 /etc/security/limits.conf; sudo chmod 666 /etc/pam.d/common-session"
ssh -t ubuntu@${remote} "sudo echo ubuntu soft nofile 32000 >> /etc/security/limits.conf"
ssh -t ubuntu@${remote} "sudo echo ubuntu hard nofile 32000 >> /etc/security/limits.conf"
ssh -t ubuntu@${remote} "sudo echo session required pam_limits.so >> /etc/pam.d/common-session"
ssh -t ubuntu@${remote} "sudo chmod 644 /etc/security/limits.conf; sudo chmod 644 /etc/pam.d/common-session"

# Copy configuration file
scp config/elasticsearch.yml ubuntu@${remote}:/${remoteesdir}/config/

# Install elasticsearch plugin
ssh ubuntu@${remote} "${remoteesdir}/bin/plugin -install karmi/elasticsearch-paramedic"
ssh ubuntu@${remote} "${remoteesdir}/bin/plugin -install lukas-vlcek/bigdesk"
# ssh ubuntu@${remote} "${remoteesdir}/bin/plugin -install elasticsearch/elasticsearch-cloud-aws/1.11.0"
