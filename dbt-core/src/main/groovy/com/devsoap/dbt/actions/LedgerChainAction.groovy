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
package com.devsoap.dbt.actions

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.ConfigInfoHandler
import com.devsoap.dbt.handlers.JsonSchemaHandler
import com.devsoap.dbt.handlers.LedgerGetTransactionHandler
import com.devsoap.dbt.handlers.LedgerListTransactionsHandler
import com.devsoap.dbt.handlers.LedgerUpdateTransactionHandler
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.google.inject.Inject
import groovy.util.logging.Slf4j
import ratpack.groovy.handling.GroovyChainAction
import ratpack.handling.Handlers

@Slf4j
class LedgerChainAction extends GroovyChainAction {

    private final String ledgerPath
    private final boolean enabled

    @Inject
    LedgerChainAction(DBTConfig config) {
        ledgerPath = config.ledger.path
        enabled = config.ledger.enabled
    }

    @Override
    void execute() throws Exception {
        if(!enabled) return

        log.info("Registering ledger at /$ledgerPath")
        path(ledgerPath, Handlers.chain(
                registry.get(LedgerGetTransactionHandler),
                registry.get(LedgerUpdateTransactionHandler),
                registry.get(LedgerListTransactionsHandler)
        ))
        path("$ledgerPath/config", ConfigInfoHandler)
        path("$ledgerPath/schema", JsonSchemaHandler)
    }
}
