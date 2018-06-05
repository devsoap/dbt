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


### Docker

There are two projects available for building the executor and the ledger as docker images. 

* docker-ledger - Run the ledger as a docker image
* docker-executor - Run the executor as a docker image

For more information how to run and configure them see their corresponding READMEs.

