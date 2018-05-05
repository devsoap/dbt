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

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.impose.Impositions
import ratpack.impose.ServerConfigImposition
import ratpack.server.RatpackServer

class CustomPortMainApplicationUnderTest extends GroovyRatpackMainApplicationUnderTest {

    private final int port

    CustomPortMainApplicationUnderTest(int port) {
        this.port = port
    }

    @Override
    protected Impositions createImpositions() throws Exception {
        Impositions.of {
            it.add(ServerConfigImposition.of {
                it.port(port)
            })
        }
    }

    @Override
    protected RatpackServer createServer() throws Exception {
        System.setProperty('ratpack.dbt.ledger.remoteUrl', "http://localhost:$port/ledger")
        System.setProperty('ratpack.dbt.executor.remoteUrl', "http://localhost:$port/executor")
        return super.createServer()
    }
}
