package com.devsoap.dbt.framework

import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification

class TransactionManagementServiceSpec extends Specification {


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

}
