package com.devsoap.dbt.handlers

import com.devsoap.dbt.services.LedgerService
import groovy.util.logging.Slf4j
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson

@Slf4j
class LedgerGetTransactionHandler implements Handler {

    @Override
    void handle(Context ctx) throws Exception {
        ctx.with {
            if(request.method == HttpMethod.GET && header('X-Transaction-Id').present) {
                def id = request.headers['X-Transaction-Id'].toString()
                def ledgerService = ctx.get(LedgerService)
                ledgerService.fetchTransaction(id).then {
                    if(it.present) {
                        render(Jackson.json(it.get()))
                    } else {
                        notFound()
                    }
                }
            } else {
                next()
            }
        }
    }
}
