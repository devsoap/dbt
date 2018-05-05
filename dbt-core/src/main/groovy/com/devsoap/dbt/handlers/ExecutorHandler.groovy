package com.devsoap.dbt.handlers

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.data.BlockTransaction
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.func.Pair
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.Status
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
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
        ctx.request.body.then { body ->
            def mapper = ctx.get(ObjectMapper)
            def ds = ctx.get(DataSource)
            def transaction = mapper.readValue(body.text, BlockTransaction)

            if(!validateChain(transaction)) {
                ctx.response.status(Status.of(400, 'Transaction chain invalid'))
                return
            }

            executeCommands(ds, transaction).onError { e ->
                log.info("Sending rolled back transaction to ledger")
                println mapper.writeValueAsString(transaction)
                client.post(config.ledger.remoteUrl.toURI(), { spec ->
                    spec.body.text(mapper.writeValueAsString(transaction))
                }).then {
                    ctx.error(e)
                }
            }.then {
                log.info("Updating ledger with execution result")
                client.post(config.ledger.remoteUrl.toURI(), { spec ->
                    spec.body.text(mapper.writeValueAsString(transaction))
                }).then {
                    ctx.response.send(mapper.writeValueAsString(transaction))
                }
            }
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

    private static Map toMap(ResultSet resultSet) {
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
                   columnValues << resultSet.getObject(columnIndex)
                }
            }
        }
        map
    }
}
