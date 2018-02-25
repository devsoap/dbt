package com.devsoap.dbt

import com.devsoap.dbt.config.DBTConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Log
import ratpack.exec.Promise
import ratpack.http.client.HttpClient
import ratpack.service.Service

import javax.inject.Inject

@Log
class DBTManager implements Service {

    private final String ledgerUrl
    private final HttpClient httpClient
    private final ObjectMapper mapper

    @Inject
    DBTManager(DBTConfig config, HttpClient httpClient, ObjectMapper mapper){
        ledgerUrl = config.ledger.url
        this.httpClient = httpClient
        this.mapper = mapper
    }

    Promise<JsonNode> execute(ExecuteQuery queryBuilder) {
        log.info("Executing new transaction")
        def builder = new TransactionBuilder(this)
        queryBuilder.build(builder)
        def transaction = builder.build()

        log.info("Sending transaction $transaction.id to ledger")
        httpClient.post(ledgerUrl.toURI(), { spec ->
            spec.body.text(mapper.writeValueAsString(transaction))
        }).flatMap { response ->
            Promise.value(mapper.readTree(response.body.text))
        }
    }

    Promise<JsonNode> execute(String transactionId, ExecuteQuery queryBuilder) {
        log.info("Amending existing transaction $transactionId")

        log.info("Getting transaction $transactionId from ledger")
        httpClient.get(ledgerUrl.toURI(), { spec ->
            spec.headers.add('X-Transaction-Id', transactionId)
        }).flatMap { response ->
            def oldTransaction = mapper.readValue(response.body.text, BlockTransaction)
            if(oldTransaction == null) {
                throw new RuntimeException("Transaction with id $transactionId could not be found")
            }
            if(oldTransaction.completed) {
                throw new RuntimeException("Cannot modify a completed transaction")
            }

            def builder = new TransactionBuilder(this, oldTransaction)
            queryBuilder.build(builder)
            def transaction = builder.build()

            if(transaction.id != transactionId) {
                throw new RuntimeException("Transaction id changed")
            }

            log.info("Sending transaction $transactionId to ledger")
            httpClient.post(ledgerUrl.toURI(), { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            })
        }.flatMap { response ->
            Promise.value(mapper.readTree(response.body.text))
        }
    }

    class TransactionBuilder {

        private final List<String> queries = []

        private final DBTManager manager

        private final BlockTransaction transaction

        private TransactionBuilder(DBTManager manager){
            this(manager, new BlockTransaction())
        }

        private TransactionBuilder(DBTManager manager, BlockTransaction transaction) {
            this.manager = manager
            this.transaction = transaction
        }

        void query(String sql){
            queries << sql
        }

        String id() {
            transaction.id
        }

        void complete() {
            transaction.end()
        }

        private BlockTransaction build() {
            queries.each { transaction.execute(it) }
            transaction
        }
    }

    @FunctionalInterface
    interface ExecuteQuery {
        void build(TransactionBuilder builder)
    }
}
