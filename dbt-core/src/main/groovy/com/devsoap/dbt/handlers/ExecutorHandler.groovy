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
package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.HttpMethod
import ratpack.http.Status
import ratpack.http.client.HttpClient
import ratpack.jdbctx.Transaction

import javax.inject.Inject
import javax.sql.DataSource
import java.sql.ResultSet

@Slf4j
class ExecutorHandler implements Handler {

    private final DBTConfig config
    private final HttpClient client
    private final ObjectMapper mapper

    @Inject
    ExecutorHandler(DBTConfig config, HttpClient client, ObjectMapper mapper) {
        this.mapper = mapper
        this.client = client
        this.config = config
    }

    @Override
    void handle(Context ctx) throws Exception {
        if(ctx.request.method == HttpMethod.POST) {
            ctx.request.body.then { body ->
                def mapper = ctx.get(ObjectMapper)
                def ds = ctx.get(DataSource)
                def transaction = mapper.readValue(body.text, BlockTransaction)

                log.info('Recieved transaction {} for execution', transaction.id)

                if(!validateChain(transaction)) {
                    log.error("Transaction chain validation failed for transaction {}", transaction.id)
                    ctx.response.status(Status.of(400, 'Transaction chain invalid')).send()
                    return
                }

                log.info('Executing transaction {} on {}', transaction.id, ds)
                executeCommands(ds, transaction).onError { e ->
                    log.info("Sending rolled back transaction to ledger")
                    client.post(config.ledger.remoteUrl.toURI(), { spec ->
                        spec.body.text(mapper.writeValueAsString(transaction))
                    }).onError { ee ->
                        log.error('Failed to reach ledger', ee)
                        ctx.response.status(Status.of(404, 'Ledger not found')).send()
                    }.then {
                        log.error('Transaction {} rolled back', transaction.id)
                        ctx.response.status(Status.of(505, 'Transaction rollback')).send()
                    }
                }.then {
                    log.info("Updating ledger with execution result")
                    client.post(config.ledger.remoteUrl.toURI(), { spec ->
                        spec.body.text(mapper.writeValueAsString(transaction))
                    }).onError { ee ->
                        log.error('Failed to reach ledger', ee)
                        ctx.response.status(Status.of(404, 'Ledger not found')).send()
                    }.then {
                        ctx.response.send(it.body.text)
                    }
                }
            }
        } else {
            ctx.next()
        }
    }

    private static boolean validateChain(BlockTransaction transaction) {
        if(transaction.queries[0].parent != transaction.id) {
            return false
        }
        for(int i=1; i<transaction.queries.size(); i++) {
            def query = transaction.queries[i]
            def prev = transaction.queries[i-1]
            if(query.id != query.generateHash()) {
                return false
            }
            if(query.parent != prev.generateHash()) {
                return false
            }
        }
        true
    }

    private static Promise<BlockTransaction> executeCommands(DataSource ds, BlockTransaction transaction) {
        def txDs = Transaction.dataSource(ds)
        def tx = Transaction.create { ds.connection }
        tx.wrap {
            try {
                transaction.queries.each { block ->
                    try{
                        log.info "Executing $block.query ..."
                        if(block.query.toLowerCase().startsWith("select")){
                            log.info('Saving result from Select query')
                            def result = txDs.connection
                                    .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
                                    .executeQuery(block.query)
                            block.result = toMap(result)
                        } else {
                            txDs.connection.createStatement().execute(block.query)
                        }
                    } catch (Exception e) {
                        block.resultError = e.message
                        throw e
                    }
                }
                transaction.executed = true
                Promise.sync { transaction }
            } catch (Exception e) {
                log.error("Failed to execute transaction $transaction.id, transaction rolled back", e)
                tx.rollback()
                transaction.rolledback = true
                Promise.error(e)
            }
        }
    }

    private static Map<String,List<String>> toMap(ResultSet resultSet) {
        def map = [:]

        if(resultSet.last()) {
            resultSet.beforeFirst()
            resultSet.metaData.columnCount.times { column ->
                def columnIndex = column + 1
                def columnName = resultSet.metaData.getColumnName(columnIndex)

                def columnValues = map[columnName] as List
                if(columnValues == null) {
                    map[columnName] = columnValues = []
                }

                resultSet.beforeFirst()
                while(resultSet.next()) {
                   columnValues << resultSet.getObject(columnIndex).toString()
                }
            }
        }
        map
    }
}
