package com.devsoap.dbt.data

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import groovy.transform.ToString

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@ToString
class BlockTransaction implements Serializable {

    String id = UUID.randomUUID().toString().replace('-','')

    List<Query> queries = []

    boolean completed = false

    boolean executed = false

    boolean rolledback = false

    /**
     * Executes a query in the transaction
     *
     * @param query
     *      the query to execute
     * @return
     *      the result of the query
     */
    void execute(String query) {
        if(queries.isEmpty()){
            queries << new Query(this, query)
        } else {
            queries << new Query(queries.last(), query)
        }
    }

    /**
     * End the current transaction
     *
     * @return
     */
    void end() {
        completed = true
    }

    // A block in the chain
    @ToString
    static final class Query implements Serializable {
        String query
        String id
        String parent
        long timeStamp

        Map<String, List> result

        Query() {
            // For serialization
        }

        Query(Query previous, String query) {
            this.query = query
            timeStamp = new Date().getTime()
            parent = previous.id
            id = generateHash()
        }

        Query(BlockTransaction transaction, String query) {
            this.query = query
            timeStamp = new Date().getTime()
            parent = transaction.id
            id = generateHash()
        }

        final String generateHash() {
            def digest = MessageDigest.getInstance('SHA-256')
            def hash = digest.digest("${parent?:''}$timeStamp$query".getBytes(StandardCharsets.UTF_8))
            hash.encodeHex().toString()
        }
    }
}
