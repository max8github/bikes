# Overview
This demo uses a number of Akka features: [Typed Actors](https://doc.akka.io/docs/akka/current/typed/index.html),
[Akka Cluster Sharding](http://doc.akka.io/docs/akka/current/scala/typed/cluster-sharding.html),
[Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html),
[Akka Persistence](https://doc.akka.io/docs/akka/current/typed/index-persistence.html),
[FSM](https://doc.akka.io/docs/akka/current/typed/fsm.html),
[Cluster Singleton](https://doc.akka.io/docs/akka/current/typed/cluster-singleton.html#cluster-singleton).

It implements an imaginary bicycle service described [below](#bikes-service).  
This service can be deployed on Kubernetes along with Cassandra.  
Other components this service interacts with are:
* a REST [client](#client)
* a fake external service stubbed with `Future`s containing `Thread.sleep()` [see here](src/main/scala/akka/sample/bikes/Procurement.scala).

A good client to generate automatically lots of requests is [Gatling](https://gatling.io/): see [below](#gatling-client)
for instructions.  
A [Postman](https://www.postman.com/) [collection](postman/BikeService.postman_collection.json) can also be used to
send requests manually ([see below](#postman-client).  
Opening a browser on [localhost](http://127.0.0.1:8084/monitor/monitor.html) will show cluster's behavior like shard
rebalancing, nodes joining, requests evolving etc.  
This is the same UI as in Hugh McKee's [crop circles](https://www.youtube.com/watch?v=CjkiznureoU&t=1563s)
demonstrating [clustering in Java](https://github.com/mckeeh3/akka-java-cluster-openshift).

# Quick Start
To run the service, do:
```
sbt run
```
Then click [here](http://127.0.0.1:8084/monitor/monitor.html) to visualize the 3-node cluster.  
Start creating requests with [Postman](#postman-client), or [Gatling](#gatling-client), or just:
```
curl --request POST 'http://127.0.0.1:8084/bike' \
--header 'Content-Type: application/json' \
--data-raw '{"instructions": {"version":"b1c1c1e", "location":"git@github.com:blueprints/myblueprint.git"}, "bom":{"version":"", "location":""}, "mechanic": {"version":"", "location":""}, "access":""}'
```
A new entity should appear on the browser changing colors as it goes through different phases (FSM states).  
Eventually entities will disappear after not being used for a while (see
[application_local.conf](src/main/resources/application_local.conf)'s `receive-timeout`)

To see what state the entity is at any point, do:
```
curl --request GET 'http://127.0.0.1:8084/bike/b1c1c1e' --header 'Content-Type: application/json'
```
To see a list of current active entities, do:
```
curl --request GET 'http://127.0.0.1:8084/bike' --header 'Content-Type: application/json'
```

See [Run](#run) for more detailed instructions.

> Note: the default store when running locally is in-memory, as by default [application_local.conf](src/main/resources/application_local.conf)  
> has `akka.persistence.journal.plugin = "akka.persistence.journal.inmem"`.  
> To use [Cassandra](docs/Cassandra.md), see [below](#1-a-3-node-cluster-running-locally).

# Bikes Service
This code represents an imaginary service for renting custom-made bicycles.  
It is similar to common city bike services, but for semi-professional bikers looking for tailored bicycles.

For that, bikers provide a detailed description of the specific bicycle they want.  
If the same bike is not already available, the service will build one.  
Users will not pay for the construction of the bike. They will only pay for using it.  
The service will assemble the bike for a user and once provisioned, will give the coordinates to locate it.

The blueprint, which is the set of instructions, sizes, bill of materials (BOM), mechanic's service, etc, of a
specific bike is saved by the user in a document repository. The user provides the location (URL) of the blueprint
in the request to the service.

The service, on a request, will: check if the same bike is already available. If not, download the blueprint from the  
repository, follow its instructions by invoking a number of services in order to assemble the bike.  
When the bicycle is ready, it is automatically reserved for the user that originally requested it.

The user will poll the service to find out when the bike is ready (no notifications) and where to go find it.  
At the end of the reservation period, the bicycle is returned to the service, which owns it.

The user can possibly un-reserve (*yield*) the bike returning it earlier to the service.  
The service will hold the bike for future use.

During assembling and creation of the bicycle, provisioning could get blocked, perhaps because of lack of parts,
or another service being not available, or because of some input errors in the original blueprint.  
In those cases, the bike processing will go into an error state.

The user can willingly unblock the bike from the error state, by issuing what's called a *kick* action.

## Implementation
Every request contains the location of the blueprint on the web, and any info needed to be
able to access it and practically execute its work.  
This is captured by a json payload sent in a POST request for the reservation.

The service gets the request and makes a persistent actor out of it.  
This is what's called an *entity actor*, which is an actor with some state, that gets persisted in case of service
crashes or simply because is no longer in active use (passivated).

A unique id extracted from the json payload identifies the specific custom bike.

The entity actor is modeled as an FSM and represents the entire bike provisioning process.  
From request to construction to reservation, a bike request goes through different phases, including downloading the blueprint,
building the bike, reserving it, un-reserving it, setting it in error state.  
You can only un-reserve a bike that is reserved, or vice-versa, and can only get out of an error state with a
kicking action (see the FSM state diagram [here](docs/fsm.puml).

If the entity is passivated (perhaps because the bike has not been used in a long time), it can be restored at a later
time for further processing (see also [LMDB](docs/Code.md#remembering-entities)).

For more notes about the code, see [Code](docs/Code.md).

# REST API
See the included [Postman collection](postman/BikeService.postman_collection.json) for all available endpoints.  
A bike request is a POST:
```
POST /bike
```
with payload:
```json
{
  "instructions": {
    "location": "git@github.com:username/instructions.git",
    "version": "b1eb1e-1d83-40d1-912a-bde20f1189c6"
  },
  "bom": {
    "location": "git@github.com:username/bom.git",
    "version": "b03b03b0-1d83-40d1-912a-bde20f1189c6"
  },
  "mechanic": {
    "location": "http://soldermybike.com",
    "version": "1.3.9"
  },
  "access": {
    "token": "08c5a19695414c0b92dd5d8b4b8683c8"
  }
}
```

The service will create a unique id out of the blueprint, and then, using Akka Cluster Sharding, will:
- check if the bike with that id is already there
- if not there, start constructing it, saving it as a new persistent entity

# Run

## The Main Cluster

There are different ways to run the cluster, the first two are for running locally and the third is for running on Kubernetes.

### 1. A 3-Node Cluster Running Locally
First: install Cassandra and start it locally (see [more detail here](docs/Cassandra.md))

>Note: without Cassandra, you can still run persistence by using an in-memory persistence storage, as shown [above](#quick-start), but you will not
be able to maintain state of FSMs after crashes or cluster rebalancing: the entity actors will be restarted from their
initial state.  

In order to enable cassandra persistence, edit [application_local.conf](src/main/resources/application_local.conf)
changing line `plugin = "akka.persistence.journal.inmem"` to:
```
    plugin = "cassandra-journal"
```

The simplest way to run the service is from a terminal:

```
sbt run
```

This command starts a three actor systems (a three node cluster) in the same JVM process.

#### Dynamic BikeServer Ports

In the log snippet below, note the dynamic ports opened by each Main node's `BikeServer` to connect to. 
The number of ports are by default 3, for the minimum 3 node cluster. 

```
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-3] [] - BikeServer online at http://127.0.0.1:8084/
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-14] [] - BikeServer online at http://127.0.0.1:8056/
[2019-11-04 14:43:45,861] [INFO] [akka.actor.typed.ActorSystem] [BikeService-akka.actor.default-dispatcher-16] [] - BikeServer online at http://127.0.0.1:8082/
```

Ports can be all dynamically found to avoid bind errors, but for convenience, one port is not dynamic: `8084`.
This way, you can bookmark your browser or use Postman to rely on that for visualizing 'crop circles' at 
[http://127.0.0.1:8084/monitor/monitor.html](http://127.0.0.1:8084/monitor/monitor.html).  
All other nodes in the cluster can be used - whichever port they will have. This is because every node is the 
same: each has a REST http server to talk to.


### 2. A 3-Node Cluster In Separate JVMs

It is more interesting to run cluster nodes in separate processes. Stop the application and then open three terminal windows.

>Note that if the backing store is in-memory and you kill a node to see rebalancing,
the entity actors will not necessarily restart from where they left off with their states.
>To make sure you have entities restart from where they left off with their states,
you must use a persistent store like Cassandra.

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

#### Dynamic BikeServer port

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


### 3. A Multi-Pod Node Cluster Deployed With Kubernetes

After installing Docker Desktop and enabling Kubernetes, run:
```
./scripts/cassandra.sh
# wait for cassandra to be up first...
./scripts/bikes.sh
```
For more detailed instructions, see [Kubernetes.md](docs/Kubernetes.md).

Once the Kubernetes cluster is up, Cassandra and the akka cluster deployed in it, run the [Gatling client](#gatling-client)
to create load and visualize it all at
[http://127.0.0.1:8084/monitor/monitor.html](http://127.0.0.1:8084/monitor/monitor.html).

## Client

### Curl
You can create requests manually by using curl:

```
curl --location --request POST 'http://127.0.0.1:8084/bike' \
--header 'Content-Type: application/json' \
--data-raw '{"instructions": {"version":"60e29908-b443-4697-8761-a3ab1dac0927", "location":"git@github.com:bikes/mybike.git", "bom":"", "mechanic": "", "repo": "git"}, "credentials":""}'
```

### Gatling Client

In a new terminal use `Gatling` load tester, (see [Gatling](https://gatling.io/)) by running:

```
sbt gatling:test
```

The load tester will run for some time (configure it in this class) creating lots of requests. Visualize what is happening by
going to [http://127.0.0.1:8084/monitor/monitor.html](http://127.0.0.1:8084/monitor/monitor.html)

### Postman Client
Install postman and then import [collection](postman/BikeService.postman_collection.json) and  
[environment](postman/BikeService.postman_environment.json) present in directory [postman](postman).

## Shutting down and Cleanup

See [Kubernetes](docs/Kubernetes.md) and [Cassandra](docs/Cassandra.md) for CLI details and cleanup.
