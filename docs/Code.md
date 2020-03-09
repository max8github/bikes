# Code
## Main
This is the `main` class. [Main.scala](../src/main/scala/akka/sample/bikes/Main.scala)
starts an ActorSystem with Cluster Sharding enabled. It joins the cluster and starts a `FleetsMaster` actor for the system.

There are two different run modes: local and Kubernetes.  
The default is local. To enable Kubernetes runs (see [Run](../README.md#run) below), a Docker environment variable is set
(`RUN_LOCALLY=false`: see [build.sbt](../build.sbt)).

## FleetsMaster

[FleetsMaster.scala](../src/main/scala/akka/sample/bikes/FleetsMaster.scala) starts the infrastructure
for sharding any number of entity types on each clustered node (there is only one type, the Bike). It is the entry point to the Cluster.

Takes requests from the HTTP actor and manages conversation with the Cluster.

## Bike - sharded data by type

A sharded [Bike](../src/main/scala/akka/sample/bikes/Bike.scala) has a declared type and receives requests via the `FleetsMaster`.
Common operations on bicycles are:

* create a bike
* return bike id
* check bike status (a bike is an FSM, can be in different states)
* reserve/un-reserve a bike

## Receiving Bike Requests

A [BikeService](../src/main/scala/akka/sample/bikes/BikeService.scala) is started with
HTTP [BikeRoutes](../src/main/scala/akka/sample/bikes/BikeRoutes.scala)
to receive and unmarshal bike requests by id.
The requests are sharded using [Akka Cluster Sharding](http://doc.akka.io/docs/akka/current/scala/typed/cluster-sharding.html).

## Configuration

This application is configured in [application.conf](../src/main/resources/application.conf) for Kubernetes
deployments and [application_local.conf](../src/main/resources/application_local.conf) for a local deployment of the service
running in one or more JVMs with in-memory persistence storage or Cassandra.

### Remembering Entities
Entities are passivated after a set amount of time of inactivity. That time is set with key `bikes.receive-timeout`.
You can also forcefully passivate an entity by using `DELETE /bike/[bikeId]`.
```
curl --request DELETE 'http://127.0.0.1:8084/bike/b1c1c1e'
```
Note that entities will be remembered after a restart: the information is saved in a
[LMDB](https://symas.com/products/lightning-memory-mapped-database) store. See
[Remembering Entities](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html#remembering-entities) for more
details.  
Please note that, when running locally, LMDB files are saved under the `target` directory, and as such they
will be wiped out at every `sbt clean`. In order to make them less ephemeral, you may want to set a different directory
in `application_local.conf` for key `akka.cluster.distributed-data.durable.lmdb.dir`.

To try that out, issue a `POST` (see [Quick Start](../README.md#quick-start)), and then a `DELETE`:
the entity will disappear from the d3.js UI at [monitor.html](http://127.0.0.1:8084/monitor/monitor.html).  
But then, if you issue a `GET`:
```
curl --location --request GET 'http://127.0.0.1:8084/bike/b1c1c1e' --header 'Content-Type: application/json'
```
then the `b1c1c1e` entity will reappear in the UI with the same state (color) as it was left when passivated.

## Server

* [BikeService](../src/main/scala/akka/sample/bikes/BikeService.scala) - Akka HTTP server
* [BikeRoutes](../src/main/scala/akka/sample/bikes/BikeRoutes.scala)  - HTTP routes receiver which will unmarshall and pass on request data
