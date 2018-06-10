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
package com.devsoap.dbt.modules

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.ConfigInfoHandler
import com.devsoap.dbt.handlers.JsonSchemaHandler
import com.devsoap.dbt.services.TransactionManagerService
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.guice.ConfigurableModule
import ratpack.server.ServerConfig

@Slf4j
class DBTModule extends ConfigurableModule<DBTConfig> {

    @Override
    protected void configure() {
        bind(ObjectMapper)
        bind(TransactionManagerService)
        bind(ConfigInfoHandler)
        bind(JsonSchemaHandler)
    }

    @Override
    protected DBTConfig createConfig(ServerConfig serverConfig) {
        (DBTConfig) serverConfig.getAsConfigObject('/dbt', DBTConfig)?.getObject() ?: super.createConfig(serverConfig)
    }
}