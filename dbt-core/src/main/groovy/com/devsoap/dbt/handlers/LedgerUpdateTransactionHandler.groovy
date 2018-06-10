/*
 * Copyright 2018 Devsoap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.devsoap.dbt.data.Query
import com.devsoap.dbt.services.LedgerService
import com.devsoap.dbt.services.MongoLedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.http.Status
import ratpack.http.client.HttpClient
import ratpack.jackson.Jackson

import javax.inject.Inject

@Slf4j
class LedgerUpdateTransactionHandler implements Handler {

    private final DBTConfig config
    private final HttpClient client
    private final ObjectMapper mapper

    @Inject
    LedgerUpdateTransactionHandler(DBTConfig config, HttpClient client, ObjectMapper mapper) {
        this.config = config
        this.mapper = mapper
        this.client = client
    }

    @Override
    void handle(Context ctx) throws Exception {
        if (ctx.request.method != HttpMethod.POST) {
            ctx.next()
            return
        }

        if (!config.executor.remoteUrl) {
            throw new RuntimeException("Executor URL is not set, cannot update transaction")
        }

        def ledgerService = ctx.get(LedgerService)
        ctx.request.body.then { body ->
            def mapper = ctx.get(ObjectMapper)
            BlockTransaction transaction = mapper.readValue(body.text, BlockTransaction)
            if(!transaction.id) {
                log.info("Recieved null transaction id, creating new transaction")
                newTransaction(ctx, transaction)
            } else {
                log.info("Recieved transaction $transaction.id, updating it")
                ledgerService.fetchTransaction(transaction.id).then { Optional<BlockTransaction> t ->
                    t.present ? updateTransaction(ctx, t.get(), transaction) : newTransaction(ctx,  transaction)
                }
            }
        }
    }

    private void updateTransaction(Context ctx, BlockTransaction oldTransaction, BlockTransaction newTransaction) {

        log.info "Transaction $newTransaction.id exists, updating transaction"
        def transaction = cloneTransaction(oldTransaction, newTransaction)

        def ledgerService = ctx.get(LedgerService)
        ledgerService.updateTransaction(transaction).then { id ->
            log.info("Transaction $id updated in ledger")
            if (transaction.completed & !(transaction.executed || transaction.rolledback)) {
                log.info("Sending transaction $id to executor at $config.executor.remoteUrl")
                client.post(config.executor.remoteUrl.toURI(), { spec ->
                    spec.body.text(mapper.writeValueAsString(transaction))
                }).onError { err ->
                    log.error('Failed to reach executor', err)
                    ctx.response.status(Status.of(404, 'Executor not found')).send()
                }.then {
                    ctx.response.send(it.body.text)
                }
            } else {
                ctx.render(Jackson.json(transaction))
            }
        }
    }

    private void newTransaction(Context ctx, BlockTransaction newTransaction) {

        log.info("Creating new transaction")
        def transaction = cloneTransaction(new BlockTransaction(), newTransaction)

        def ledgerService = ctx.get(LedgerService)
        ledgerService.newTransaction(transaction).then {
            log.info("Transaction $it added to ledger")
            if(transaction.completed){
                log.info("Sending transaction $transaction.id to executor at $config.executor.remoteUrl")
                client.post(config.executor.remoteUrl.toURI(), { spec ->
                    spec.body.text(mapper.writeValueAsString(transaction))
                }).onError { err ->
                    log.error('Failed to reach executor', err)
                    ctx.response.status(Status.of(404, 'Executor not found')).send()
                }.then {
                    ctx.response.send(it.body.text)
                }
            } else {
                ctx.render(Jackson.json(transaction))
            }
        }
    }

    private static BlockTransaction cloneTransaction(BlockTransaction oldTransaction, BlockTransaction newTransaction) {
        def transaction = new BlockTransaction()
        transaction.id = newTransaction.id
        transaction.executed = newTransaction.executed
        transaction.completed = newTransaction.completed
        transaction.rolledback = newTransaction.rolledback

        newTransaction.queries.each { q ->
            def query = transaction.queries.isEmpty() ?
                    new Query(transaction, q.query) :
                    new Query(transaction.queries.last(), q.query)
            query.resultError = q.resultError
            query.result = q.result
            transaction.queries << query
        }

        transaction
    }
}
