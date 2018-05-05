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
package com.devsoap.dbt

import com.devsoap.dbt.actions.ExecutorChainAction
import com.devsoap.dbt.actions.LedgerChainAction
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.LedgerData
import com.devsoap.dbt.handlers.ExecutorHandler
import com.devsoap.dbt.handlers.LedgerGetTransactionHandler
import com.devsoap.dbt.handlers.LedgerListTransactionsHandler
import com.devsoap.dbt.handlers.LedgerUpdateTransactionHandler
import com.devsoap.dbt.services.LedgerService
import com.devsoap.dbt.services.TransactionManagerService
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.multibindings.Multibinder
import groovy.util.logging.Slf4j
import ratpack.guice.ConfigurableModule
import ratpack.handling.HandlerDecorator
import ratpack.server.ServerConfig

@Slf4j
class DBTModule extends ConfigurableModule<DBTConfig> {

    @Override
    protected void configure() {
        bind(ObjectMapper)

        bind(LedgerChainAction)
        bind(LedgerGetTransactionHandler)
        bind(LedgerListTransactionsHandler)
        bind(LedgerUpdateTransactionHandler)

        bind(ExecutorChainAction)
        bind(ExecutorHandler)

        bind(LedgerData)

        bind(LedgerService)
        bind(TransactionManagerService)

        Multibinder.newSetBinder(binder(), HandlerDecorator).addBinding()
                .toInstance(HandlerDecorator.prependHandlers(LedgerChainAction))

        Multibinder.newSetBinder(binder(), HandlerDecorator).addBinding()
                .toInstance(HandlerDecorator.prependHandlers(ExecutorChainAction))
    }

    @Override
    protected DBTConfig createConfig(ServerConfig serverConfig) {
        (DBTConfig) serverConfig.getAsConfigObject('/dbt', DBTConfig)?.getObject() ?: super.createConfig(serverConfig)
    }

    @Override
    protected void defaultConfig(ServerConfig serverConfig, DBTConfig config) {
        if(!config.executor.remoteUrl) {
            config.executor.remoteUrl = "http://localhost:${serverConfig.port}/${config.executor.path}"
        }
        if(!config.ledger.remoteUrl) {
            config.executor.remoteUrl = "http://localhost:${serverConfig.port}/${config.ledger.path}"
        }
    }
}