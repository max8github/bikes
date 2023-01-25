#!/bin/bash

kubectl delete -f kubernetes/bikes-cluster-deployment.yml
docker rmi bikes:0.3
sbt clean
sbt docker:publishLocal
minikube image rm bikes:0.3
minikube image load bikes:0.3
kubectl apply -f kubernetes/bikes-cluster-deployment.yml