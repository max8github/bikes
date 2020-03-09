# Overview
You can use [Cassandra](http://cassandra.apache.org) as the backing store for persistent actors when running locally.

# Local Install of Cassandra on Mac
Install and run:
```
brew install cassandra
cassandra -f
```

Change the value of `akka.persistence.journal.plugin`' in [application_local.conf](src/main/resources/application_local.conf) to:
```
plugin = "cassandra-journal"
```

# On Kubernetes
For deploying Cassandra on Kubernetes, just run the [cassandra.sh](../scripts/cassandra.sh) script:
```
cd bikes
./script/cassandra.sh
```
It contains the same commands that you can find by reading [Deploying Cassandra with Stateful Sets](https://kubernetes.io/docs/tutorials/stateful-application/cassandra/).

# Script
Following are the main commands used by the [cassandra.sh](../scripts/cassandra.sh) script used to deploy Cassandra in Kubernetes:
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

The last command will delete the [PersistentVolumeClaim](https://kubernetes.io/docs/concepts/storage/persistent-volumes/#persistentvolumeclaims).


## Troubleshooting
The Java version should be 1.8. To change it on Mac,
see [change version of java on mac](https://stackoverflow.com/questions/21964709/how-to-set-or-change-the-default-java-jdk-version-on-os-x)

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

