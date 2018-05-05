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
package com.devsoap.dbt.data

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
     * Commit the transaction to database
     *
     * @return
     */
    void commit() {
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
        String resultError

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
