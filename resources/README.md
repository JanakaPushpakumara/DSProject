
## BootstrapServer

### How to Run
* Navigate to the <PROECT_HOME>/resources/BootstrapServer/Java directory ad execute the below commands.
 ```sh
    $ javac BootstrapServer.java 
    $ java BootstrapServer 
    Bootstrap Server created at 55555. Waiting for incoming data...

   ```
## ClusterFomation.sh Script

### How to Run
* Create a New directory for cluster nodes and copy the script.
* Update the PROJECT_HOME in the script with the absolute path of the cloned project home .
* Run the script with the below command, and It will create default 10 node cluster. ( make sure to build the project 
  with "mvn clean install" command before running the script)

 ```sh
   sh ClusterFomation.sh
   ```
Ex: 
 ```sh
$ sh ClusterFomation.sh
Creating Node0 ...
Creating Node1 ...
Creating Node2 ...
Creating Node3 ...
Creating Node4 ...
Creating Node5 ...
Creating Node6 ...
Creating Node7 ...
Creating Node8 ...
Creating Node9 ...
Nodes created, Starting the cluster ...
Starting Node0 ...
Starting Node1 ...
Starting Node2 ...
Starting Node3 ...
Starting Node4 ...
Starting Node5 ...
Starting Node6 ...
Starting Node7 ...
Starting Node8 ...
Starting Node9 ...

   ```
Ex: BS server Logs
 ```sh
$ java BootstrapServer
Bootstrap Server created at 55555. Waiting for incoming data...
127.0.0.1 : 60546 - 0029 REG 127.0.0.1 5000 Node0
127.0.0.1 : 33196 - 0029 REG 127.0.0.1 5001 Node1
127.0.0.1 : 52121 - 0029 REG 127.0.0.1 5002 Node2
127.0.0.1 : 42300 - 0029 REG 127.0.0.1 5003 Node3
0 2
127.0.0.1 : 58012 - 0029 REG 127.0.0.1 5004 Node4
3 1
127.0.0.1 : 34538 - 0029 REG 127.0.0.1 5005 Node5
3 1
127.0.0.1 : 41933 - 0029 REG 127.0.0.1 5006 Node6
4 3
127.0.0.1 : 49392 - 0029 REG 127.0.0.1 5007 Node7
0 3
127.0.0.1 : 55408 - 0029 REG 127.0.0.1 5008 Node8
3 1
127.0.0.1 : 52058 - 0029 REG 127.0.0.1 5009 Node9
5 1

   ```

Ex: All the UI windows of the ten node cluster.
![ClusterNodes](../resources/images/ClusterNodes.png?raw=true)

Ex: Search results when executing search query.
![ClusterNodes](../resources/images/SearchResults.png?raw=true)


