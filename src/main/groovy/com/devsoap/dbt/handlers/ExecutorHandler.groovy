package com.devsoap.dbt.handlers

import com.devsoap.dbt.BlockTransaction
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import groovy.util.logging.Log
import ratpack.exec.Promise
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.Status
import ratpack.jdbctx.Transaction

import javax.sql.DataSource
import java.sql.ResultSet

@Log
class ExecutorHandler implements Handler {

    static final String PATH = 'executor'

    @Override
    void handle(Context ctx) throws Exception {
        ctx.request.body.then { body ->
            def mapper = ctx.get(ObjectMapper)
            def ds = ctx.get(DataSource)
            def transaction = mapper.readValue(body.text, BlockTransaction)

            if(!validateChain(transaction)) {
                ctx.response.status = Status.of(400, 'Transaction chain invalid')
                return
            }

            executeCommands(ds, mapper, transaction).then {
                transaction.executed = true
                ctx.response.send(mapper.writeValueAsString(transaction))
            }
        }
    }

    boolean validateChain(BlockTransaction transaction) {
        //FIXME
        true
    }

    Promise<BlockTransaction> executeCommands(DataSource ds, ObjectMapper mapper, BlockTransaction transaction) {
        def txDs = Transaction.dataSource(ds)
        def tx = Transaction.create { ds.connection }
        tx.wrap {
            Promise.sync {
                transaction.queries.each { block ->
                    log.info "Executing $block.data ..."
                    if(block.data.toLowerCase().startsWith("select")){
                        def result = txDs.connection
                                .createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
                                .executeQuery(block.data)
                        block.result = toJson(mapper, result)
                    } else {
                        txDs.connection.createStatement().execute(block.data)
                    }
                }
                transaction
            }
        }
    }

    private static JsonNode toJson(ObjectMapper mapper, ResultSet resultSet) {
        def json = mapper.createObjectNode()

        if(resultSet.last()) {
            int rows = resultSet.row
            log.info("Converting $rows rows to json")
            resultSet.beforeFirst()

            resultSet.metaData.columnCount.times { column ->
                def columnIndex = column + 1
                def columnName = resultSet.metaData.getColumnName(columnIndex)
                ArrayNode columnValue = json.get(columnName)
                if(!columnValue) {
                    columnValue = mapper.createArrayNode()
                    json.set(columnName, columnValue)
                }

                resultSet.beforeFirst()
                while(resultSet.next()) {
                    columnValue.addPOJO(resultSet.getObject(columnIndex))
                }
            }
        }
        //mapper.writeValueAsString(json)
        mapper.valueToTree(json)
    }
}
