package com.devsoap.dbt.services

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.http.Status
import ratpack.http.client.HttpClient
import ratpack.service.Service

import javax.inject.Inject

@Slf4j
class TransactionManagerService implements Service {
    private final HttpClient httpClient
    private final ObjectMapper mapper
    private final DBTConfig config

    @Inject
    TransactionManagerService(DBTConfig config, HttpClient httpClient, ObjectMapper mapper){
        this.config = config
        this.httpClient = httpClient
        this.mapper = mapper
    }

    Promise<JsonNode> execute(ExecuteQuery queryBuilder) {
        if(!config.ledger.remoteUrl) {
            throw new RuntimeException("Ledger remote url is not set, cannot execute query")
        }

        def builder = new TransactionBuilder()
        queryBuilder.build(builder)
        def transaction = builder.build()

        log.info("Sending transaction $transaction.id to ledger at $config.ledger.remoteUrl")
        httpClient.post(config.ledger.remoteUrl.toURI(), { spec ->
            spec.body.text(mapper.writeValueAsString(transaction))
        }).onError {
            log.error("Failed to send transaction $transaction.id to ledger $config.ledger.remoteUrl")
        }.map { response ->
            if(response.status == Status.OK) {
                mapper.readTree(response.body.text)
            }
        }
    }

    Promise<JsonNode> execute(String transactionId, ExecuteQuery queryBuilder) {
        if(!config.ledger.remoteUrl) {
            throw new RuntimeException("Ledger remote url is not set, cannot execute query")
        }

        log.info("Sending transaction $transactionId to ledger at $config.ledger.remoteUrl")
        httpClient.get(config.ledger.remoteUrl.toURI(), { spec ->
            spec.headers.add('X-Transaction-Id', transactionId)
        }).flatMap { response ->
            if(response.status == Status.of(404)) {
                throw new RuntimeException("Transaction with id '$transactionId' could not be found")
            }
            if(response.status != Status.OK) {
                throw new RuntimeException("Ledger returned ${response.statusCode} ${response.status.message} for $transactionId")
            }
            def oldTransaction = mapper.readValue(response.body.text, BlockTransaction)
            if(oldTransaction.completed) {
                throw new RuntimeException("Cannot modify a completed transaction")
            }

            log.info("Updating transaction $transactionId content with new query")
            def builder = new TransactionBuilder(oldTransaction)
            queryBuilder.build(builder)
            def transaction = builder.build()

            if(transaction.id != transactionId) {
                throw new RuntimeException("Transaction id changed")
            }

            log.info("Sending updated transaction $transaction.id to ledger at $config.ledger.remoteUrl")
            httpClient.post(config.ledger.remoteUrl.toURI(), { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }).onError {
                log.error("Failed to send transaction $transaction.id to ledger $config.ledger.remoteUrl")
            }
        }.map { response ->
            mapper.readTree(response.body.text)
        }
    }

    class TransactionBuilder {

        private final List<String> queries = []

        private final BlockTransaction transaction

        private TransactionBuilder() {
            this.transaction = new BlockTransaction()
        }

        private TransactionBuilder(BlockTransaction transaction) {
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
