package com.devsoap.dbt.actions

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.ExecutorHandler
import com.google.inject.Inject
import groovy.util.logging.Slf4j
import ratpack.groovy.handling.GroovyChainAction

@Slf4j
class ExecutorChainAction extends GroovyChainAction {

    private final String executorPath

    @Inject
    ExecutorChainAction(DBTConfig config) {
        executorPath = config.executor.path
    }

    @Override
    void execute() throws Exception {
        log.info("Registering executor at $executorPath")
        path(executorPath, ExecutorHandler)
    }
}
