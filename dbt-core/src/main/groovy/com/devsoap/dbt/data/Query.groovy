package com.devsoap.dbt.data

import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.ToString

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@ToString
class Query implements Serializable {

    @JsonProperty(required = true)
    String query
    String id
    String parent
    long timeStamp

    // Column -> Values
    Map<String, List<String>> result

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