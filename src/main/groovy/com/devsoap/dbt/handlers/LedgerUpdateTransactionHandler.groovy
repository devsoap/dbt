package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.devsoap.dbt.data.LedgerData
import com.devsoap.dbt.services.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Log
import groovy.util.logging.Slf4j
import ratpack.config.ConfigData
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import ratpack.server.ServerConfig
import ratpack.session.Session

import javax.inject.Inject

@Slf4j
class LedgerUpdateTransactionHandler implements Handler {

    private final String executorUrl

    @Inject
    LedgerUpdateTransactionHandler(DBTConfig config) {
        executorUrl = config.executor.remoteUrl
    }

    @Override
    void handle(Context ctx) throws Exception {
        ctx.with {
            if(ctx.request.method == HttpMethod.POST) {
                if(!executorUrl) {
                    throw new RuntimeException("Executor URL is not set, cannot update transaction")
                }

                def ledgerService = get(LedgerService)
                def session = get(Session)
                request.body.then { body ->
                    def mapper = get(ObjectMapper)
                    def transaction = mapper.readValue(body.text, BlockTransaction)
                    log.info("Recieved transaction $transaction.id")
                    ledgerService.fetchTransaction(session,transaction.id).then {
                        if(it.present) {
                            log.info "Transaction $transaction.id exists, updating transaction"
                            ledgerService.updateTransaction(session, transaction).then {
                                log.info("Transaction $it updated in ledger")
                                if(transaction.completed){
                                    log.info("Sending transaction $transaction.id to executor at $executorUrl")
                                    redirect(executorUrl)
                                } else {
                                    render(Jackson.json(transaction))
                                }
                            }
                        } else {
                            log.info("Creating new transaction")
                            ledgerService.newTransaction(session, transaction).then {
                                log.info("Transaction $it added to ledger")
                                if(transaction.completed){
                                    log.info("Sending transaction $transaction.id to executor at $executorUrl")
                                    redirect(executorUrl)
                                } else {
                                    render(Jackson.json(transaction))
                                }
                            }
                        }
                    }
                }
            } else {
                next()
            }
        }
    }
}
