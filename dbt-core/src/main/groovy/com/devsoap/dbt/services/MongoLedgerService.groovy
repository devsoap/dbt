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

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import com.gmongo.GMongo
import com.mongodb.*
import com.mongodb.util.JSON
import groovy.util.logging.Slf4j
import ratpack.exec.Promise

import javax.inject.Inject

@Slf4j
class MongoLedgerService implements LedgerService {

    private final String dbUrl
    private DB db
    private final ObjectMapper mapper

    @Inject
    MongoLedgerService(DBTConfig config, ObjectMapper mapper) {
        dbUrl = config.ledger.databaseUrl
        this.mapper = mapper
    }

    @Override
    Promise<Optional<BlockTransaction>> fetchTransaction(String transactionId) {
        BlockTransaction transaction = transactions.findOne(['id':transactionId])?.findAll { it.key != '_id' } as BlockTransaction
        Promise.value(Optional.ofNullable(transaction))
    }

    @Override
    Promise<List<BlockTransaction>> allTransactions() {
        def cursor = transactions.find()
        log.info("Found ${cursor.size()} transactions")
        Promise.value(cursor.collect {it.findAll { it.key != '_id' } as BlockTransaction})
    }

    @Override
    Promise<String> newTransaction(BlockTransaction transaction) {
        log.info("Adding new transaction $transaction.id")
        transactions << JSON.parse(mapper.writeValueAsString(transaction))
        Promise.value(transaction.id)
    }

    @Override
    Promise<String> updateTransaction(BlockTransaction transaction) {
        log.info("Updating transaction $transaction.id")
        transactions.findAndModify(
                ['id':transaction.id] as BasicDBObject,
                JSON.parse(mapper.writeValueAsString(transaction)) as DBObject
        )
        Promise.value(transaction.id)
    }

    private DBCollection getTransactions() {
        database.getCollection('transactions')
    }

    private DB getDatabase() {
        if(!db) {
            db = new GMongo(new MongoURI(dbUrl)).getDB('dbt')
        }
        db
    }
}
