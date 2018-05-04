package com.devsoap.dbt.handlers

import com.devsoap.dbt.services.LedgerService
import groovy.util.logging.Slf4j
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson

@Slf4j
class LedgerListTransactionsHandler implements Handler {

    @Override
    void handle(Context ctx) throws Exception {
        ctx.with {
            if(request.method == HttpMethod.GET && !header('X-Transaction-Id').present) {
                log.info("Listing transactions...")
                def ledgerService = get(LedgerService)
                ledgerService.allTransactions().then {
                    render(Jackson.json(it))
                }
            } else {
                next()
            }
        }
    }
}
