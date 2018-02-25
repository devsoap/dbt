package com.devsoap.dbt.handlers

import com.devsoap.dbt.BlockTransaction
import com.devsoap.dbt.services.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Log
import ratpack.handling.Context
import ratpack.handling.Handler

@Log
class LedgerHandler implements Handler {

    static final String PATH = 'ledger'

    @Override
    void handle(Context ctx) {
        def ledgerService = ctx.get(LedgerService)

        ctx.byMethod {
            delegate = it

            get({
                def transaction = ledgerService.fetchTransaction(ctx.request.headers['X-Transaction-Id'].toString())
                def mapper = ctx.get(ObjectMapper)
                ctx.response.send(mapper.writeValueAsString(transaction))
            } as Handler)

            post({
                ctx.request.body.then { body ->
                    def mapper = ctx.get(ObjectMapper)
                    def transaction = mapper.readValue(body.text, BlockTransaction)

                    def existingTransaction = ledgerService.fetchTransaction(transaction.id)
                    if(existingTransaction) {
                        ledgerService.updateTransaction(transaction)
                    } else {
                        log.info("Creating new transaction")
                        ledgerService.newTransaction(transaction)
                    }
                    if(transaction.completed){
                        log.info("Sending transaction $transaction.id to executor")
                        ctx.redirect(ExecutorHandler.PATH)
                    } else {
                        ctx.response.send(mapper.writeValueAsString(transaction))
                    }
                }
            } as Handler)
        }
    }

}
