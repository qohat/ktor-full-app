# ktor-fidu-app

_Ktor Full Project_

## Quick Start 🚀
**IMPORTANT**

_Currently we have two different options for starting the app. 
For FrontEnd devs we suggest using the second one._

## First of all we should build the docker image
```
$ ./gradlew installDist 

$ docker build -t fidu-app .
```


## 1. If you want to run only the backend locally ⚙️

```
$ docker run --p 8080:8080 --d --name fidu-app fidu-app:latest
```

## For stoping the Docker Container

```
$ docker stop ${YOUR_CONTAINER_ID}
```

## 2. Using Docker Compose with PostgresSQL ⚙️

```
$ docker compose up -d
```

## Using the Backed Service

* Open your browser in the next url [http://localhost:8080/list/CCF](http://localhost:8080/list/CCF)
It should show the CCF list

## For stoping the Docker Compose

```
docker-compose down --remove-orphans
```