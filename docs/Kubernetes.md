# Kubernetes Deployment

[Docker Desktop](https://www.docker.com/products/docker-desktop) is one of the easiest way to get started with
Kubernetes on Mac or Windows.  
Other possible solutions are [Kind](https://kind.sigs.k8s.io/), [Minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/),
[Rancher Desktop](https://rancherdesktop.io/).
Here are quick instructions for a [Kubernetes](https://www.docker.com/products/kubernetes)
cluster with [Docker Desktop](https://www.docker.com/products/docker-desktop).

Install Docker, [enable](https://www.techrepublic.com/article/how-to-add-kubernetes-support-to-docker-desktop/)
its Kubernetes plugin, then run:
```
./scripts/cassandra.sh
```
There is a manual step here (TODO: automate): you have to first apply `CQL` changes to Cassandra.
There is a `DDL` file for that: ([cassandra.cql](../kubernetes/cassandra.cql)).
See [Cassandra.md](Cassandra.md#seed-cassandra) for details: wait for cassandra to be up first, then run its DDL.  
And then run [bikes.sh](../scripts/bikes.sh):
```
./scripts/bikes.sh
```

The yaml deployment files are in directory [kubernetes](../kubernetes).  
They are separated in different scripts so that they can be incrementally deployed.


## Script Description
This section describes what the script [bikes.sh](../scripts/bikes.sh) does and other useful commands.  
First, it builds the docker image:
```shell script
sbt docker:publishLocal
```
Then sets the context:

```shell script
sbt package docker:build   # in order to generate the docker image
export KUBECONFIG=~/.kube/config  # this file is created by docker for desktop when enabling kubernetes.
kubectl config set-context docker-desktop
```
Then deploys 3 nodes and creates a load balancer so that you can access `monitor.html` from the browser:
```shell script
kubectl apply -f kubernetes/add-bikes-cluster-1.json
kubectl config set-context --current --namespace=bikes-cluster-1
kubectl apply -f kubernetes/bikes-cluster-deployment.yml
kubectl expose deployment bikes-cluster-demo --type=LoadBalancer --name=bikes-service
```

## Clean up

```
kubectl delete service -l app=bikes-cluster-demo
kubectl delete deployments -l app=bikes-cluster-demo
```

## Troubleshoot
Other useful commands to check if things are good along the way, or useful to troubleshoot:

```shell script
kubectl config view
kubectl get deployments
kubectl get pods 
kubectl get pods --all-namespaces
kubectl get replicasets
kubectl get services bikes-service
kubectl describe services bikes-service
kubectl get pods --output=wide
kubectl get pods -o wide

kubectl config current-context
kubectl cluster-info
kubectl get ing
kubectl get all --all-namespaces
kubectl get all -n bikes-cluster-1
kubectl get namespaces
kubectl cluster-info dump
kubectl logs bikes-cluster-demo-86f4ff69f9-6k9dz
kubectl logs --selector app=bikes-cluster-demo --tail=200
lsof -i :8084

kubectl wait --for=condition=ready pod -l app=bikes
```

Get a [shell to the running](https://kubernetes.io/docs/tasks/debug/debug-application/get-shell-running-container/)
container:
```
kubectl exec --stdin --tty shell-demo -- /bin/bash
```
In order to destroy the entire kubernetes deployment, you can do:

```shell script
# will delete all under that namespace:
kubectl delete namespaces bikes-cluster-1
```
but that will not remove the volumes Cassandra used. In order to properly clean up Cassandra, see [here](Cassandra.md#cleanup).

# Minikube
## Random Notes
With Minikube, the `provisioner` for cassandra's `StatefulSet` is not `docker.io/hostpath` but `k8s.io/minikube-hostpath`

In order to load the generated bikes image into minikube:
```shell
sbt docker:publishLocal
minikube image load bikes:0.3
minikube image ls
# which should produce something like:
#    registry.k8s.io/pause:3.9
#    ...
#    docker.io/library/bikes:0.3
```