package com.devsoap.dbt.actions

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.LedgerGetTransactionHandler
import com.devsoap.dbt.handlers.LedgerListTransactionsHandler
import com.devsoap.dbt.handlers.LedgerUpdateTransactionHandler
import com.google.inject.Inject
import groovy.util.logging.Slf4j
import ratpack.groovy.handling.GroovyChainAction
import ratpack.handling.Handlers

@Slf4j
class LedgerChainAction extends GroovyChainAction {

    private final String ledgerPath

    @Inject
    LedgerChainAction(DBTConfig config) {
        ledgerPath = config.ledger.path
    }

    @Override
    void execute() throws Exception {
        log.info("Registering ledger at $ledgerPath")
        path(ledgerPath, Handlers.chain(
                registry.get(LedgerGetTransactionHandler),
                registry.get(LedgerUpdateTransactionHandler),
                registry.get(LedgerListTransactionsHandler)
        ))
    }
}
