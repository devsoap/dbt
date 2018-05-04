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
