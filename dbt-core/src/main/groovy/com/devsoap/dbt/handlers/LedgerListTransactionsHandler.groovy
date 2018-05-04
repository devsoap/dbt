package com.devsoap.dbt.handlers

import com.devsoap.dbt.services.LedgerService
import groovy.util.logging.Slf4j
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson
import ratpack.session.Session

@Slf4j
class LedgerListTransactionsHandler implements Handler {

    @Override
    void handle(Context ctx) throws Exception {
        ctx.with {
            if(request.method == HttpMethod.GET && !header('X-Transaction-Id').present) {
                def session = get(Session)
                log.info("Listing transactions in session $session.id")
                def ledgerService = get(LedgerService)
                ledgerService.allTransactions(session).then {
                    render(Jackson.json(it))
                }
            } else {
                next()
            }
        }
    }
}
