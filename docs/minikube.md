# Instructions for running Cassandra
For reference documentation, see: [Deploying Cassandra with a StatefulSet](https://kubernetes.io/docs/tutorials/stateful-application/cassandra/)  
For instructions on Docker Desktop (similar), see page [Cassandra](Cassandra.md).

In short, commands for minikube are:
```
git clone https://github.com/max8github/bikes.git
minikube start --memory 5120 --cpus=4
cd bikes
export KUBECONFIG=~/.kube/config
kubectl apply -f kubernetes/add-bikes-cluster-1.json
kubectl config set-context --current --namespace=bikes-cluster-1
kubectl apply -f kubernetes/cassandra-service.yml
kubectl apply -f kubernetes/cassandra-statefulset.yml
```
Now check in Minikube's dashboard what the status of the cluster is:
```
minikube dashboard
```
Alternatively, use `kubectl` commands like:
```
kubectl get pods
kubectl get pvc
kubectl get pv
kubectl get sts
kubectl get svc
```
Use `minukube stop` to stop the VM and `minikube delete` to completely delete it if you need to.

After applying the stateful set above, the cassandra cluster should show three healthy
pods like this:

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

And now, after doing port-forwarding from another terminal:
```
kubectl port-forward cassandra-0 9042:9042
```
you should be able to use cqlsh to issue CQL commands to Cassandra.
For example, you can first do:
```shell
cqlsh -f kubernetes/cassandra.cql
```
and then, get into the cqlsh shell:
```shell
cqlsh
```
and once there, issue something like:
```sql
SELECT * FROM system_schema.keyspaces;
```
and be able to spot akka in the output.