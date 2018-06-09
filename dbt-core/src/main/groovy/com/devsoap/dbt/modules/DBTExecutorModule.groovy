package com.devsoap.dbt.modules

import com.devsoap.dbt.actions.ExecutorChainAction
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.handlers.ExecutorHandler
import com.google.inject.multibindings.Multibinder
import ratpack.handling.HandlerDecorator
import ratpack.server.ServerConfig

class DBTExecutorModule extends DBTModule {

    @Override
    protected void configure() {
        super.configure()

        bind(ExecutorChainAction)
        bind(ExecutorHandler)

        Multibinder.newSetBinder(binder(), HandlerDecorator).addBinding()
                .toInstance(HandlerDecorator.prependHandlers(ExecutorChainAction))
    }

    @Override
    protected void defaultConfig(ServerConfig serverConfig, DBTConfig config) {
        config.executor.enabled = !config.executor.remoteUrl
        if(!config.executor.remoteUrl) {
            config.executor.remoteUrl = "http://localhost:${serverConfig.port}/${config.executor.path}"
        }
    }
}
