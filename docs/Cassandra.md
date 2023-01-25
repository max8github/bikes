# Overview
You can use [Cassandra](http://cassandra.apache.org) as the backing store for persistent actors when running locally.

# Local Install of Cassandra on Mac
Install and run:
```
brew install cassandra
cassandra -f
```

In [MainLocal.scala](../src/main/scala/akka/sample/bikes/MainLocal.scala) hard-code the configuration like this:
```
    val config = ConfigFactory.load("application_local_cassandra.conf")
```
so to use [application_local_cassandra.conf](../src/main/resources/application_local_cassandra.conf)
instead of [application_local.conf](../src/main/resources/application_local.conf).  
TODO (improvement): it should instead be passed at runtime by using something like
`-Dresource.config=application_local_cassandra.conf`.  
TODO: the ddl should be included for when running on k9.

Run the Cassandra [nodetool](https://cwiki.apache.org/confluence/display/CASSANDRA2/NodeTool)
inside the first Pod, to display the status of the ring:
```
kubectl exec -it cassandra-0 -- nodetool status
```

# On Kubernetes
Reference docs: [Example: Deploying Cassandra with a StatefulSet](https://kubernetes.io/docs/tutorials/stateful-application/cassandra/)
For deploying Cassandra on Kubernetes, just run script [cassandra.sh](../scripts/cassandra.sh):
```
cd bikes
./script/cassandra.sh
```
It contains the same commands that you can find by reading
[Deploying Cassandra with Stateful Sets](https://kubernetes.io/docs/tutorials/stateful-application/cassandra/),
which are about creating a `Namespace` (`bikes-cluster-1`), creating a `Service` for Cassandra, and creating a
`StatefulSet`. If the deployment is successful, then showing objects like `Pod`, `PersistentVolumeClaim`, `PersistentVolume`,
`StatefulSet` and `Service` would look like:
```
kubectl get pvc
NAME                         STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
cassandra-data-cassandra-0   Bound    pvc-6f618094-2e30-4d48-8fbc-5f479a7937bd   1Gi        RWO            fast           6m38s
cassandra-data-cassandra-1   Bound    pvc-b8154401-82a3-4546-b526-d411e99e83ea   1Gi        RWO            fast           5m52s
cassandra-data-cassandra-2   Bound    pvc-285a8c87-82aa-4dbd-86c7-3866af953886   1Gi        RWO            fast           4m51s
➜   kubectl get po
NAME          READY   STATUS    RESTARTS   AGE
cassandra-0   1/1     Running   0          13m
cassandra-1   1/1     Running   0          12m
cassandra-2   1/1     Running   0          11m
➜   kubectl get svc
NAME        TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)    AGE
cassandra   ClusterIP   None         <none➜         9042/TCP   13m
➜   kubectl get sts
NAME        READY   AGE
cassandra   3/3     13m
➜   kubectl get pvc
NAME                         STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
cassandra-data-cassandra-0   Bound    pvc-6f618094-2e30-4d48-8fbc-5f479a7937bd   1Gi        RWO            fast           13m
cassandra-data-cassandra-1   Bound    pvc-b8154401-82a3-4546-b526-d411e99e83ea   1Gi        RWO            fast           12m
cassandra-data-cassandra-2   Bound    pvc-285a8c87-82aa-4dbd-86c7-3866af953886   1Gi        RWO            fast           11m
➜   kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                        STORAGECLASS   REASON   AGE
pvc-285a8c87-82aa-4dbd-86c7-3866af953886   1Gi        RWO            Delete           Bound    bikes-cluster-1/cassandra-data-cassandra-2   fast                    11m
pvc-6f618094-2e30-4d48-8fbc-5f479a7937bd   1Gi        RWO            Delete           Bound    bikes-cluster-1/cassandra-data-cassandra-0   fast                    13m
pvc-b8154401-82a3-4546-b526-d411e99e83ea   1Gi        RWO            Delete           Bound    bikes-cluster-1/cassandra-data-cassandra-1   fast                    12m
➜   kubectl get storageclass
NAME                 PROVISIONER          RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION   AGE
fast                 docker.io/hostpath   Delete          Immediate           false                  15m
hostpath (default)   docker.io/hostpath   Delete          Immediate           false                  8h
```
and the log of one pod (one of the cassandra nodes) looks like [this one](resources/cassandra-0.log.md).  
Note that the `Provisioner` being used here is `docker.io/hostpath`. For minikube it would instead be
`k8s.io/minikube-hostpath`. For Rancher Desktop, you would not specify the `StorageClass` in file
`cassandra-statefulset.yml`: you would just set `storageClassName` to `local-path` in `spec.volumeClaimTemplates`.

## Seed Cassandra
In order to prepare Cassandra with all necessary tables, its DDL specified in file [cassandra.cql](cassandra.cql)
needs to be applied. For that, you need to first do
[port-forwarding](https://kubernetes.io/docs/tasks/access-application-cluster/port-forward-access-application-cluster/)
with kubernetes:
```
kubectl port-forward cassandra-0 9042:9042
```
at which point you can then issue `CQL` commands with `cqlsh`:

> TODO: ultimately, [cass-operator](https://docs.datastax.com/en/cass-operator/doc/cass-operator/cassOperatorConnectToK8sFromOutsideCluster.html)
> should be used.

## DDL
Run the DDL file with:
```
cqlsh -f kubernetes/cassandra.cql
```
To check if it all worked, get keyspaces info, get tables info, get table info with:
```
SELECT * FROM system_schema.keyspaces;
SELECT * FROM system_schema.tables WHERE keyspace_name = 'akka';
SELECT * FROM system_schema.columns WHERE keyspace_name = 'akka' AND table_name = 'messages';
SELECT * FROM akka.messages;
```
See [here](cassandra_tables.md) for some sample output of the select commands above.

# Script
Following are the main commands used by the [cassandra.sh](../scripts/cassandra.sh)
script used to deploy Cassandra in Kubernetes (TODO: DDL missing):
```
export KUBECONFIG=~/.kube/config
kubectl config set-context --current --namespace=bikes-cluster-1
kubectl config get-contexts
kubectl apply -f kubernetes/add-bikes-cluster-1.json
kubectl apply -f kubernetes/cassandra-service.yml
kubectl get svc cassandra
kubectl apply -f kubernetes/cassandra-statefulset.yml
kubectl get statefulset cassandra
kubectl apply -f kubernetes/bikes-cluster-deployment.yml
kubectl expose deployment bikes-cluster-demo --type=LoadBalancer --name=bikes-service
kubectl expose deployment cassandra --type=LoadBalancer --name=mycassandra-service
```

# Cleanup
Delete the Cassandra service:
```
kubectl delete service -l app=cassandra
```

Run the following command to delete everything in the Cassandra `StatefulSet`:
```
grace=$(kubectl get po cassandra-0 -o=jsonpath='{.spec.terminationGracePeriodSeconds}') \
  && kubectl delete statefulset -l app=cassandra \
  && echo "Sleeping $grace" \
  && sleep $grace \
  && kubectl delete pvc -l app=cassandra
```

The last command will delete the
[PersistentVolumeClaim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistentvolumeclaims).


## Troubleshooting
The Java version should be 1.8. To change it on Mac, use [sdkman](https://sdkman.io/).  
See also [change version of java on mac](https://stackoverflow.com/questions/21964709/how-to-set-or-change-the-default-java-jdk-version-on-os-x)

Various useful commands:
```
which java
echo $JAVA_HOME
/usr/libexec/java_home
/usr/libexec/java_home -V
export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
/usr/libexec/java_home -V
/usr/libexec/java_home
java -version


chsh -s /bin/bash
echo $CASSANDRA_HOME
which cassandra
ls -al /usr/local/bin/cassandra
ls /usr/local/Cellar/cassandra/3.11.5
brew info cassandra
ls /usr/local/var/lib/cassandra/data
ls /usr/local/var/lib/cassandra/data/akka
sudo rm -rf /usr/local/var/lib/cassandra/*
```

