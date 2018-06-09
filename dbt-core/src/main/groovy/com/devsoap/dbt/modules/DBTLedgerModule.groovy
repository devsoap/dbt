package com.devsoap.dbt.modules

import com.devsoap.dbt.actions.LedgerChainAction
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.LedgerGetTransactionHandler
import com.devsoap.dbt.handlers.LedgerListTransactionsHandler
import com.devsoap.dbt.handlers.LedgerUpdateTransactionHandler
import com.devsoap.dbt.services.LedgerService
import com.google.inject.multibindings.Multibinder
import ratpack.handling.HandlerDecorator
import ratpack.server.ServerConfig

class DBTLedgerModule extends DBTModule {

    @Override
    protected void configure() {
        super.configure()

        bind(LedgerChainAction)
        bind(LedgerGetTransactionHandler)
        bind(LedgerListTransactionsHandler)
        bind(LedgerUpdateTransactionHandler)
        bind(LedgerService)

        Multibinder.newSetBinder(binder(), HandlerDecorator).addBinding()
                .toInstance(HandlerDecorator.prependHandlers(LedgerChainAction))
    }

    @Override
    protected void defaultConfig(ServerConfig serverConfig, DBTConfig config) {
        config.ledger.enabled = !config.ledger.remoteUrl
        if(!config.ledger.remoteUrl) {
            config.ledger.remoteUrl = "http://localhost:${serverConfig.port}/${config.ledger.path}"
        }
    }
}
