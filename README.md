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
      config.ledger.remoteUrl = 'http://localhost:5050/ledger'
      config.executor.remoteUrl = 'http://localhost:5050/executor'
    }
}
```

## DBT Demo

Demo application for testing the transactions. 

The demo application has a database with one table LOGS which you can use to insert and select data from.

The demo can be run by running ``./gradlew :dbt-demo:run`` after which the demo is running on http://localhost:5050


## DBT Test

The test module contains all the integration tests for the module. 