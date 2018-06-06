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
import com.devsoap.dbt.handlers.ExecutorHandler
import com.devsoap.dbt.handlers.ConfigInfoHandler
import com.google.inject.Inject
import groovy.util.logging.Slf4j
import ratpack.groovy.handling.GroovyChainAction
import ratpack.handling.Handlers

@Slf4j
class ExecutorChainAction extends GroovyChainAction {

    private final String executorPath
    private final boolean enabled

    @Inject
    ExecutorChainAction(DBTConfig config) {
        executorPath = config.executor.path
        enabled = config.executor.enabled
    }

    @Override
    void execute() throws Exception {
        if(!enabled) return

        log.info("Registering executor at /$executorPath")
        path(executorPath, ExecutorHandler)
        path("$executorPath/config", ConfigInfoHandler)
    }
}
