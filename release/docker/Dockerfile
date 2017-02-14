###
# Dockerfile to create JRE bundles from Install4J
###

FROM ubuntu

###
# Usual maintenance
###

RUN apt-get update && apt-get install -y curl x11-apps libxi6 libxtst6


###
# Create and work out of the download directory
###

RUN mkdir /download

WORKDIR /download

###
# Download and install Java
###

RUN curl -v -j -k -L -H "Cookie: oraclelicense=accept-securebackup-cookie"  http://download.oracle.com/otn-pub/java/jdk/8u51-b16/jre-8u51-linux-x64.tar.gz > /download/jre-8u51-linux-x64.tar.gz

RUN tar xzfv /download/jre-8u51-linux-x64.tar.gz -C /download

RUN mkdir /usr/java

RUN mv /download/jre1.8.0_51 /usr/java

RUN ln -s /usr/java/jre1.8.0_51/bin/java /usr/bin/java

###
# Download and install Install4J
###

RUN curl -SL http://download-keycdn.ej-technologies.com/install4j/install4j_unix_6_1_4.sh -o /download/install4j_unix_6_1_4.sh

RUN sh /download/install4j_unix_6_1_4.sh -q

