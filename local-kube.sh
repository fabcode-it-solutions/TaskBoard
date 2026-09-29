kubectl delete -f taskinn-backend/kubernetes-local.yaml
kubectl delete -f taskinn-frontend/kubernetes-local.yaml

eval $(minikube docker-env)

docker build -t taskinn-frontend:local --build-arg BUILD_CONFIGURATION=staging --build-arg BASE_HREF=/dev/ taskinn-frontend/.

cd taskinn-backend
mvn clean package jib:dockerBuild -Djib.to.image=taskinn-backend:local

cd ..

kubectl apply -f taskinn-backend/kubernetes-local.yaml
kubectl apply -f taskinn-frontend/kubernetes-local.yaml
