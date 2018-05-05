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
package com.devsoap.dbt.services

import com.devsoap.dbt.data.BlockTransaction
import com.devsoap.dbt.data.LedgerData
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.service.Service

@Slf4j
class LedgerService implements Service {

    private static final LedgerData data = new LedgerData()

    Promise<Optional<BlockTransaction>> fetchTransaction(String transactionId) {
        Promise.value(Optional.ofNullable(data.transactions.find {it.id == transactionId}))
    }

    Promise<List<BlockTransaction>> allTransactions() {
        Promise.value(data.transactions)
    }

    Promise<String> newTransaction(BlockTransaction transaction) {
        log.info("Adding new transaction $transaction.id")
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }

    Promise<String> updateTransaction(BlockTransaction transaction) {
        log.info("Updating transaction $transaction.id")
        data.transactions.removeAll {it.id == transaction.id}
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }
}
