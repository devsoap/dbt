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
