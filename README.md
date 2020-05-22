# Overview
This first part shows the minimal code to set up a cluster.  
Most of this code is taken and re-adapted from 
[this akka sample](https://developer.lightbend.com/start/?group=akka&project=akka-samples-cluster-sharding-scala).

Opening a browser on [localhost](http://127.0.0.1:8084/monitor/monitor.html) will show the cluster.    

# Quick Start
To run the service, do:
```
sbt run
```
Then click [here](http://127.0.0.1:8084/monitor/monitor.html) to visualize the 3-node cluster.  

See below for running nodes in separate JVMs.


# Run

## The Main Cluster

### 1. A 3-Node Cluster Running Locally
The simplest way to run the service is from a terminal:

```
sbt run
```

This command starts a three actor systems (a three node cluster) in the same JVM process.

#### Dynamic BikeService Ports

In the log snippet below, note the dynamic ports opened by each Main node's `BikeService` to connect to. 
The number of ports are by default 3, for the minimum 3 node cluster. 

```
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-3] [] - BikeService online at http://127.0.0.1:8084/
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-14] [] - BikeService online at http://127.0.0.1:8056/
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-16] [] - BikeService online at http://127.0.0.1:8082/
```

Ports can be all dynamically found to avoid bind errors, but for convenience, one port is not dynamic: `8084`.

### 2. A 3-Node Cluster In Separate JVMs

It is more interesting to run cluster nodes in separate processes. Stop the application and then open three terminal windows.

In the first terminal window, start the first seed node with:

```
sbt "runMain akka.sample.bikes.Main 2553"
```    

`2553` corresponds to the port of the first seed-nodes element in the configuration. 
In the log output you see that the cluster node has been started and changed status to '`MemberUp`'.

You'll see a log message when a `client` sends a message with a request, and for each of those you'll see
a log message from the routes showing the action taken.

In the second terminal window, start the second seed node with:

```
    sbt "runMain akka.sample.bikes.Main 2554"
```

`2554` corresponds to the port of the second seed-nodes element in the configuration.  
In the log output you see that the cluster node has been started and joins the other seed node and 
becomes a member of the cluster. Its status changed to 'Up'. Switch over to the first terminal window and see in 
the log output that the member joined.

Some of the bikes that were originally on the `ActorSystem` on port `2553` will be migrated to the newly
joined `ActorSystem` on port `2554`. The migration is straightforward: the old entity actor FSM is stopped and then restarted
on the newly created `ActorSystem` with the state where it was left off.

Start another node in the third terminal window with the following command:

```
    sbt "runMain akka.sample.bikes.Main 0"
```

Now you don't need to specify the port number, as it is dynamically generated: `0` means that it will use a random available port. 
It will join one of the configured seed nodes. Look at the log output in the different terminal windows.

Start even more nodes in the same way, if you like.

#### Dynamic BikeService port

Each node's log will show its dynamic port opened for clients to connect to, saying something like:

```
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-16] [] - BikeService online at http://127.0.0.1:8082/
```

> Note: Before running a client (browser, Postman, Gatling, curl...), you have to take note of a port.  
> Port `8084` however will always be opened first: so, for convenience, you can just use that to display crop circles:  
> [http://127.0.0.1:8084/monitor/monitor.html](http://127.0.0.1:8084/monitor/monitor.html)


#### Shutting down

Shut down one of the nodes by pressing 'ctrl-c' in one of the terminal windows. 
The other nodes will detect the failure after a while, which you can see in the log output in the other terminals.

