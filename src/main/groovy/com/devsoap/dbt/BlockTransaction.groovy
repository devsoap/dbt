package com.devsoap.dbt

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class BlockTransaction implements Serializable {

    String id = UUID.randomUUID().toString()

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
        queries << new Query(queries.empty? null : queries.last(), query)
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
    static final class Query implements Serializable {
        String data
        String hash
        long timeStamp
        Object result

        Query() {
            // For serialization
        }

        Query(Query previous, String data) {
            this.data = data
            timeStamp = new Date().getTime()
            hash = generateHash(previous?.hash)
        }

        private final String generateHash(String previousHash) {
            def digest = MessageDigest.getInstance('SHA-256')
            def hash = digest.digest("${previousHash?:''}$timeStamp$data".getBytes(StandardCharsets.UTF_8))
            hash.encodeHex().toString()
        }
    }
}
