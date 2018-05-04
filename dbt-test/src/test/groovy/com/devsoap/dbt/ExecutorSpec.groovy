package com.devsoap.dbt

import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.AutoCleanup
import spock.lang.Specification

class ExecutorSpec extends Specification {

    def mapper = new ObjectMapper()

    def PATH = 'executor'

    @AutoCleanup
    def aut = new CustomPortMainApplicationUnderTest(8888)

    void 'transaction sent to executor'() {
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
            recievedTransaction.executed
    }



}
