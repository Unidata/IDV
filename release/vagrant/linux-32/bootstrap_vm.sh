#!/bin/bash

sudo apt-get update

sudo apt-get install -y curl x11-apps libxi6 libxtst6

sudo mkdir /home/vagrant/download

sudo curl -v -j -k -L -H "Cookie: oraclelicense=accept-securebackup-cookie"  http://download.oracle.com/otn-pub/java/jdk/8u112-b15/jre-8u112-linux-i586.tar.gz > /home/vagrant/download/jre-8u112-linux-i586.tar.gz

sudo tar xzfv /home/vagrant/download/jre-8u112-linux-i586.tar.gz -C /home/vagrant/download

sudo mkdir /usr/java

sudo mv /home/vagrant/download/jre1.8.0_112 /usr/java

sudo ln -s /usr/java/jre1.8.0_112/bin/java /usr/bin/java

sudo curl -SL http://download-aws.ej-technologies.com/install4j/install4j_unix_5_1_15.sh -o /home/vagrant/download/install4j_unix_5_1_15.sh

sudo chown -R vagrant:vagrant /home/vagrant/
