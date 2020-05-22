# Kubernetes Deployment

The easiest way to get started with Kubernetes on Mac or Windows is probably [Docker Desktop](https://www.docker.com/products/docker-desktop).  
Other possible solutions are [Kind](https://kind.sigs.k8s.io/), [Minikube](https://kubernetes.io/docs/tasks/tools/install-minikube/).  
Here are quick instructions for a [Kubernetes](https://www.docker.com/products/kubernetes) cluster within [Docker Desktop](https://www.docker.com/products/docker-desktop).

Install Docker, [enable](https://www.techrepublic.com/article/how-to-add-kubernetes-support-to-docker-desktop/) its Kubernetes
plugin, then simply do:
```
./scripts/bikes.sh
```

The yaml deployment files are in directory [kubernetes](../kubernetes). They are separated in different scripts so that  
can be incrementally deployed.


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
lsof -i :8084
kubectl delete pod bikes-cluster-demo-86bd5f6c59-hxd9p  #removes one pod
```

In order to destroy the entire kubernetes deployment, you can do:

```shell script
# will delete all under that namespace:
kubectl delete namespaces bikes-cluster-1
```

# TroubleShooting
The docker versioned used for this code was `2.3.0.2 (45183)` with:

```
Engine 19.03.8
Notary 0.6.1
Compose 1.25.5
Credential Helper 0.6.3
Kubernetes v1.16.5
```

Getting:
```shell script
The connection to the server kubernetes.docker.internal:6443 was refused - did you specify the right host or port?
```
likely means you have not enabled a local Kubernetes cluster in Docker Desktop: click the main Docker icon 
-> Kubernetes -> enable local cluster.
