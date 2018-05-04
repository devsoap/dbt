package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.devsoap.dbt.services.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
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
        ctx.with {
            if(ctx.request.method == HttpMethod.POST) {
                if(!config.executor.remoteUrl) {
                    throw new RuntimeException("Executor URL is not set, cannot update transaction")
                }

                def ledgerService = get(LedgerService)
                request.body.then { body ->
                    def mapper = get(ObjectMapper)
                    def transaction = mapper.readValue(body.text, BlockTransaction)
                    log.info("Recieved transaction $transaction.id")
                    ledgerService.fetchTransaction(transaction.id).then {
                        if(it.present) {
                            log.info "Transaction $transaction.id exists, updating transaction"
                            ledgerService.updateTransaction(transaction).then {
                                log.info("Transaction $it updated in ledger")
                                if(transaction.completed && !transaction.executed){
                                    log.info("Sending transaction $transaction.id to executor at $config.executor.remoteUrl")
                                    redirect(config.executor.remoteUrl)
                                } else {
                                    render(Jackson.json(transaction))
                                }
                            }
                        } else {
                            log.info("Creating new transaction")
                            ledgerService.newTransaction(transaction).then {
                                log.info("Transaction $it added to ledger")
                                if(transaction.completed && !transaction.executed){
                                    log.info("Sending transaction $transaction.id to executor at $config.executor.remoteUrl")
                                    redirect(config.executor.remoteUrl)
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
