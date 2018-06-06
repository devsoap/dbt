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

Once you have the executor and the ledger configured and running, you can start making transactions to the database 
the executor is connected to.

By default the executor will set up an in-memory database without any tables, so the first transaction you want to make
is to create the table you want.

The preferred way would be to use one the libraries provided to communicate with the ledger, but you can also try out
the functionality by posting JSON directly to the ledger.


#### Creating a transaction

Here is an example transaction you can POST to the ledger. In this example we post a completed transaction which will be 
executed immediatly after registering with the ledger. If you want to postpone execution set to *completed* property to false.

POST http://localhost:5050/ledger
```json
{
    "completed" : true,
	"queries": [
		{ "query" : "CREATE TABLE foo(bar INT)" },
		{ "query" : "INSERT INTO foo (bar) VALUES (5)" },
		{ "query" : "SELECT * FROM foo" }		
	]
}
```

The response you will get back looks something like this
```json
{
	"id": "5db5f0d506994a54b6482f587a953c43",
	"queries": [
		{
			"query": "CREATE TABLE foo(bar INT)",
			"id": "d1e2a4d4d5b3233ef07bc52ad935f459da26e5917c542c499541538156007d94",
			"parent": "5db5f0d506994a54b6482f587a953c43",
			"timeStamp": 1528306534048,
			"result": null,
			"resultError": null
		},
		{
			"query": "INSERT INTO foo (bar) VALUES (5)",
			"id": "c3df3ea4402f4b4aa0ffec1fd728fe00a013106a46719846362b9629b5a3e4f6",
			"parent": "d1e2a4d4d5b3233ef07bc52ad935f459da26e5917c542c499541538156007d94",
			"timeStamp": 1528306534050,
			"result": null,
			"resultError": null
		},
		{
			"query": "SELECT * FROM foo",
			"id": "ef59de620262b5a41c2944f9539c27dd375e1123d6c43b1251f407ae6c34ec59",
			"parent": "c3df3ea4402f4b4aa0ffec1fd728fe00a013106a46719846362b9629b5a3e4f6",
			"timeStamp": 1528306534050,
			"result": {
				"BAR": [
					5
				]
			},
			"resultError": null
		}
	],
	"completed": true,
	"executed": true,
	"rolledback": false
}
```

As you can see, the queries were made and the results returned successfully. 

The ledger will automatically create the necessery hashes for the blockchain. 

#### Viewing the ledger

If you do a GET request to the ledger URL (http://localhost:5050/ledger) you can see all transactions and their state.