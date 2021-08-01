#!/bin/bash

# Define the Project home
PROJECT_HOME="/home/janaka/Documents/MSC/ACA-S2/DC/Project/MyProject/P2PFileSharer";


JAR_FILE="$PROJECT_HOME/org.dc.p2p.fs.rest.service/target/org.dc.p2p.fs.rest.service-1.0.0.jar";
SPRINGBOOT_PROPERTIES="$PROJECT_HOME/org.dc.p2p.fs.rest.service/src/main/resources/application.properties";
CONFIG_PROPERTIES="$PROJECT_HOME/org.dc.p2p.fs.ui/src/main/resources/config.properties";
FILE_NAME_TXT="$PROJECT_HOME/org.dc.p2p.fs.ui/src/main/resources/FileNames.txt";

# Node Count ( Includign 0th node)
NODE_COUNT=9

echo "Starting the Cluster ..."

for id in $(seq 0 $NODE_COUNT);
do
  echo "Creating Node$id ..."
  mkdir Node$id;
	mkdir Node$id/download;
	mkdir Node$id/storage;
	cp $JAR_FILE Node$id/;
	cp $SPRINGBOOT_PROPERTIES Node$id/;
        cp $CONFIG_PROPERTIES Node$id/;
        cp $FILE_NAME_TXT Node$id/;
        RST_PORT="505$id";
        sed -i "s/\(server\.port=\).*\$/\1${RST_PORT}/" Node$id/application.properties;
        SERVER_PORT="500$id";
        sed -i "s/\(SERVER_PORT=\).*\$/\1${SERVER_PORT}/" Node$id/config.properties;
        REST_SERVICE_PORT="505$id";
        sed -i "s/\(REST_SERVICE_PORT=\).*\$/\1${RST_PORT}/" Node$id/config.properties;
        SERVER_NAME=Node$id;
        sed -i "s/\(SERVER_NAME=\).*\$/\1${SERVER_NAME}/" Node$id/config.properties;
        FILE_NAME_LIST="\.\/Node$id\/FileNames.txt";
        sed -i "s/\(FILE_NAME_LIST=\).*\$/\1${FILE_NAME_LIST}/" Node$id/config.properties;
        FILE_STORAGE_DIR="\.\/Node$id\/storage"
        sed -i "s/\(FILE_STORAGE_DIR=\).*\$/\1${FILE_STORAGE_DIR}/" Node$id/config.properties;
        FILE_DOWNLOAD_DIR="\.\/Node$id\/download"
        sed -i "s/\(FILE_DOWNLOAD_DIR=\).*\$/\1${FILE_DOWNLOAD_DIR}/" Node$id/config.properties;
        HOP_COUNT="5"
        sed -i "s/\(HOP_COUNT=\).*\$/\1${HOP_COUNT}/" Node$id/config.properties;

done

echo "Starting the Cluster ..."

#Start the Node in background mode
for id in $(seq 0 $NODE_COUNT);
do
  echo "Starting Node$id ..."
  java -DpropFileLocation=./Node$id/config.properties -jar  Node$id/org.dc.p2p.fs.rest.service-1.0.0.jar --spring.config.location=./Node$id/application.properties > Node$id/Node$id.log &
  sleep 5;
done;

# Useful commands

# kill -9 `jps | grep "org.dc.p2p.fs.rest.service" | cut -d " " -f 1`
# grep -ra "PERF" | cut -b 28-40 | head -2 |

# grep -ra "MESSAGE" | grep "#RECEIVED#" | cut -b 1-5 | sort | uniq -c;
# grep -ra "MESSAGE" | grep "#FORWARDED#" | cut -b 1-5 | sort | uniq -c;
# grep -ra "MESSAGE" | grep "#ANSWERED#" | cut -b 1-5 | sort | uniq -c;

# List total neighbour count per node
#for id in {0..9};
#do
#	echo "Node$id neighbour count = " ;
#	grep -ra "#RT SIZE#"  | grep Node$id |  grep "2021-07-31 19:"  | cut -b 130- | uniq ;
#done;








