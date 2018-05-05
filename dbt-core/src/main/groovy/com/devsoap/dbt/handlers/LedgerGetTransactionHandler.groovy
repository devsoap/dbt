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
