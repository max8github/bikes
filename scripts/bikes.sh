#!/bin/bash

set -exu

sbt docker:publishLocal

export KUBECONFIG=~/.kube/config
kubectl config set-context docker-desktop

kubectl apply -f kubernetes/add-bikes-cluster-1.json
kubectl config set-context --current --namespace=bikes-cluster-1
kubectl apply -f kubernetes/bikes-cluster-deployment.yml
kubectl expose deployment bikes-cluster-demo --type=LoadBalancer --name=bikes-cluster-demo

for i in {1..10}
do
  echo "Waiting for pods to get ready..."
  kubectl get pods
  [ `kubectl get pods | grep Running | wc -l` -eq 3 ] && break
  sleep 4
done

if [ $i -eq 10 ]
then
  # shellcheck disable=SC2242
  exit "Pods did not get ready" >&2
  exit 1
fi

POD=$(kubectl get pods | grep bikes | grep Running | head -n1 | awk '{ print $1 }')

for i in {1..10}
do
  echo "Checking for MemberUp logging..."
  kubectl logs $POD | grep MemberUp || true
  [ `kubectl logs $POD | grep MemberUp | wc -l` -eq 3 ] && break
  sleep 3
done

kubectl get pods

echo "Logs"
echo "=============================="
for POD in $(kubectl get pods | grep bikes | awk '{ print $1 }')
do
  echo "Logging for $POD"
  kubectl logs $POD
done

if [ $i -eq 10 ]
then
  echo "No 3 MemberUp log events found"
  echo "=============================="

  exit 1
fi
