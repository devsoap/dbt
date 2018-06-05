# Distributed Blockchain Transactions (DBT)

This is a framework for working with SQL transactions in a distributed network. The framework allows you to compose SQL 
transactions of multiple queries on different nodes and execute them later. 

To ensure the reliability of the transactions a ledger using blockchain is used to keep track on ongoing transactions and
ensure queries within the transactions are consistent.

[![Build Status](https://travis-ci.org/devsoap/dbt.svg?branch=master)](https://travis-ci.org/devsoap/dbt)


## DBT Core

The core library containing the DBTModule to include in your Ratpack application. 

The DBT module can be added like so:

```groovy
bindings {
    module (DBTModule) { config ->
      config.ledger.remoteUrl = '<ledger host>:<ledger port>/ledger'
      config.executor.remoteUrl = '<executor host>:<executor port>/executor'
    }
}
```

## DBT Test

The test module contains all the integration tests for the module. 


## Docker

There are two projects available for building the executor and the ledger as docker images. 

* docker-ledger - Run the ledger as a docker image
* docker-executor - Run the executor as a docker image

For more information how to run and configure them see their corresponding READMEs.


## Docker Compose

The project also comes with a *docker-compose.yml* file for running the docker images. 

To run the images first you need to buid them and publish them to your local docker registry. 

You can do that by running:

1) ``./gradlew dbt-executor:distDocker``
2) ``./gradlew dbt-ledger:distDocker``

After you have generated the Docker images you can run them both by running ``docker-compose up`` in the root folder. 

The ledger will be available at http://localhost:5050/ledger and the executor 
will be available at http://localhost:5051/executor


## How to make a distributed transaction (in a ratpack application)

Once you have the executor and the ledger configured and running, you can start making transactions to the database 
the executor is connected to.

By default the executor will set up an in-memory database without any tables, so the first transaction you want to make
is to create the table you want.

