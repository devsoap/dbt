package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.jackson.Jackson

import javax.inject.Inject

class ConfigInfoHandler implements Handler {

    private final DBTConfig config

    @Inject
    ConfigInfoHandler(DBTConfig config) {
        this.config = config
    }

    @Override
    void handle(Context ctx) throws Exception {
        if(ctx.request.method == HttpMethod.GET) {
            ctx.render Jackson.json(config)
        } else {
            ctx.next()
        }
    }
}
