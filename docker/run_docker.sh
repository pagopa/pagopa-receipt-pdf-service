#!/bin/bash

# sh ./run_docker.sh <local|dev|uat|prod>

ENV=$1

if [ -z "$ENV" ]
then
  ENV="local"
  echo "No environment specified: local is used."
fi

pip3 install yq

if [ "$ENV" = "local" ]; then
  image="service-local:latest"
  ENV="dev"
else
  repository=$(yq -r '."microservice-chart".image.repository' ../helm/values-$ENV.yaml)
  image="${repository}:latest"
fi
export image=${image}

FILE=.env
if test -f "$FILE"; then
    rm .env
fi
config=$(yq  -r '."microservice-chart".envConfig' ../helm/values-$ENV.yaml)
# set word splitting
IFS=$'\n'
for line in $(echo "$config" | yq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
    echo "$line" >> .env
done

keyvault=$(yq  -r '."microservice-chart".keyvault.name' ../helm/values-$ENV.yaml)
secret=$(yq  -r '."microservice-chart".envSecret' ../helm/values-$ENV.yaml)
for line in $(echo "$secret" | yq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
  IFS='=' read -r -a array <<< "$line"
  response=$(az keyvault secret show --vault-name $keyvault --name "${array[1]}")
  response=$(echo "$response" | tr -d '\n')
  value=$(echo "$response" | yq -r '.value')
  value=$(echo "$value" | sed 's/\$/\$\$/g')
  value=$(echo "$value" | tr -d '\n')
  echo "${array[0]}=$value" >> .env
done


stack_name=$(cd .. && basename "$PWD")
docker compose -p "${stack_name}" up -d --remove-orphans --force-recreate --build


# waiting the containers
printf 'Waiting for the service'
attempt_counter=0
max_attempts=50
url="http://localhost:8080/q/health/live"

until [ "$(curl -s -w '%{http_code}' -o /dev/null "$url")" -eq 200 ]; do
    if [ ${attempt_counter} -eq ${max_attempts} ]; then
        echo -e "\nMax attempts reached. Service not available."
        echo "Check if the service is running on the correct port and health check endpoint."
        exit 1
    fi

    printf '.'
    attempt_counter=$((attempt_counter + 1))
    sleep 10
done
echo -e "\nService Started"