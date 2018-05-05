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
import com.devsoap.dbt.services.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson

import javax.inject.Inject

@Slf4j
class LedgerUpdateTransactionHandler implements Handler {

    private final DBTConfig config

    @Inject
    LedgerUpdateTransactionHandler(DBTConfig config) {
        this.config = config
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
            log.info("Recieved transaction $transaction.id")
            ledgerService.fetchTransaction(transaction.id).then { Optional<BlockTransaction> t ->
                t.present ? updateTransaction(ctx, transaction) : newTransaction(ctx, transaction)
            }
        }
    }

    private void updateTransaction(Context ctx, BlockTransaction transaction) {
        def ledgerService = ctx.get(LedgerService)
        log.info "Transaction $transaction.id exists, updating transaction"
        ledgerService.updateTransaction(transaction).then {
            log.info("Transaction $it updated in ledger")
            if (transaction.completed & !(transaction.executed || transaction.rolledback)) {
                log.info("Sending transaction $transaction.id to executor at $config.executor.remoteUrl")
                ctx.redirect(config.executor.remoteUrl)
            } else {
                ctx.render(Jackson.json(transaction))
            }
        }
    }

    private void newTransaction(Context ctx, BlockTransaction transaction) {
        if(transaction.executed || transaction.rolledback) {
            log.error("Tried to create a already executed transaction $transaction.id")
            throw new IllegalArgumentException("Cannot create a transaction with executed or rolledback status")
        }

        def ledgerService = ctx.get(LedgerService)
        log.info("Creating new transaction")
        ledgerService.newTransaction(transaction).then {
            log.info("Transaction $it added to ledger")
            if(transaction.completed){
                log.info("Sending transaction $transaction.id to executor at $config.executor.remoteUrl")
                ctx.redirect(config.executor.remoteUrl)
            } else {
                ctx.render(Jackson.json(transaction))
            }
        }
    }
}
