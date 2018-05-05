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

import com.devsoap.dbt.data.BlockTransaction

import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
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
            transaction.commit()
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
            transaction.commit()
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
            transaction.commit()
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
