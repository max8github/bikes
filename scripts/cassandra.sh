#!/bin/bash

set -exu

export KUBECONFIG=~/.kube/config
kubectl config set-context --current --namespace=bikes-cluster-1
kubectl apply -f kubernetes/add-bikes-cluster-1.json
kubectl apply -f kubernetes/cassandra-service.yml
kubectl apply -f kubernetes/cassandra-statefulset.yml

#kubectl config get-contexts
#kubectl get svc cassandra
#kubectl get statefulset cassandra

for i in {1..25}
do
  echo "Waiting for pods to get ready..."
  kubectl get pods -l app=cassandra
  [ `kubectl get pods -l app=cassandra | grep Running | wc -l` -eq 3 ] && break
  sleep 8
done

if [ $i -eq 25 ]
then
  # shellcheck disable=SC2242
  exit "Pods did not get ready" >&2
  exit 1
else
  echo "Cassandra is up"
fi
