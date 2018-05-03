package com.devsoap.dbt.framework

import com.devsoap.dbt.data.BlockTransaction

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.impose.Imposition
import ratpack.impose.Impositions
import ratpack.impose.ImpositionsSpec
import ratpack.impose.ServerConfigImposition
import ratpack.server.RatpackServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class LedgerSpec extends Specification {

    def mapper = new ObjectMapper()

    def PATH = 'ledger'

    @AutoCleanup
    GroovyRatpackMainApplicationUnderTest aut = new CustomPortMainApplicationUnderTest(8888)

    void 'transaction sent to ledger'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
        when:
            String json = aut.httpClient.requestSpec{ spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.postText(PATH)
            def recievedTransaction = mapper.readValue(json, BlockTransaction)
        then:
            recievedTransaction.id == transaction.id
            !recievedTransaction.completed
    }

    void 'completed transaction marked as completed'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
            transaction.end()
        when:
            String json = aut.httpClient.requestSpec{ spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.postText(PATH)
            def recievedTransaction = mapper.readValue(json, BlockTransaction)
        then:
            recievedTransaction.id == transaction.id
            recievedTransaction.completed
    }

    void 'completed transaction sent to executor from ledger'() {
        setup:
            def transaction = new BlockTransaction()
            transaction.execute("SELECT * FROM LOGS")
            transaction.end()
        when:
            def response = mapper.readValue(aut.httpClient.requestSpec { spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.post(PATH).body.text, BlockTransaction)
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
            String json = aut.httpClient.requestSpec{ spec ->
                spec.body.text(mapper.writeValueAsString(transaction))
            }.postText(PATH)
            def recievedTransaction = mapper.readValue(json, BlockTransaction)
            def result = recievedTransaction.queries[1].result
        then:
            recievedTransaction.id == transaction.id
            recievedTransaction.queries.first().result == null // insert query has no result

            result['LOG_ID'].first() == 1
            result['LOG_VALUE'].first() == 'HELLO'
    }
}
