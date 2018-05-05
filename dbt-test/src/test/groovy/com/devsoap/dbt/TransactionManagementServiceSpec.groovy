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
