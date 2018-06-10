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
import groovy.util.logging.Slf4j
import ratpack.exec.Promise

@Slf4j
class InMemoryLedgerService implements LedgerService {

    private static final LedgerData data = new LedgerData()

    InMemoryLedgerService() {
        log.warn('Using in-memory ledger service which will not store the ledger between restarts. Please consider ' +
                'using the Mongo ledger service instead.')
    }

    @Override
    Promise<Optional<BlockTransaction>> fetchTransaction(String transactionId) {
        Promise.value(Optional.ofNullable(data.transactions.find {it.id == transactionId}))
    }

    @Override
    Promise<List<BlockTransaction>> allTransactions() {
        log.info("Found ${data.transactions.size()} transactions")
        Promise.value(data.transactions)
    }

    @Override
    Promise<String> newTransaction(BlockTransaction transaction) {
        log.info("Adding new transaction $transaction.id")
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }

    @Override
    Promise<String> updateTransaction(BlockTransaction transaction) {
        log.info("Updating transaction $transaction.id")
        data.transactions.removeAll {it.id == transaction.id}
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }

    private static class LedgerData {
        final List<BlockTransaction> transactions = []
    }
}
