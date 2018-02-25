package com.devsoap.dbt.framework

import com.devsoap.dbt.BlockTransaction
import com.devsoap.dbt.handlers.LedgerHandler
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification

class LedgerSpec extends Specification {

    def mapper = new ObjectMapper()
    def jsonSlurper = new JsonSlurper()

    @AutoCleanup
    def aut = new GroovyRatpackMainApplicationUnderTest()

    void 'transaction sent to ledger'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
        when:
            def response = mapper.readValue(aut.httpClient.requestSpec { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.post(LedgerHandler.PATH).body.text, BlockTransaction)
        then:
            response.id == transaction.id
            response.completed == false
    }

    void 'completed transaction marked as completed'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
            transaction.end()
        when:
            def response = mapper.readValue(aut.httpClient.requestSpec { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.post(LedgerHandler.PATH).body.text, BlockTransaction)
        then:
            response.id == transaction.id
            response.completed == true
    }

    void 'completed transaction sent to executor from ledger'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
            transaction.end()
        when:
            def response = mapper.readValue(aut.httpClient.requestSpec { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.post(LedgerHandler.PATH).body.text, BlockTransaction)
        then:
            response.id == transaction.id
            response.executed == true
            response.rolledback == false
            response.completed == true
    }

    void 'result is attached to block and executed in order'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("INSERT INTO LOGS(LOG_ID,LOG_VALUE) VALUES (1, 'HELLO')")
            transaction.execute("SELECT * FROM LOGS")
            transaction.end()
        when:
            def response = mapper.readValue(aut.httpClient.requestSpec { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.post(LedgerHandler.PATH).body.text, BlockTransaction)
            def json = jsonSlurper.parseText(response.queries[1].result)
        then:
            response.id == transaction.id
            json.LOG_ID.first() == 1
            json.LOG_VALUE.first() == 'HELLO'
    }
}
