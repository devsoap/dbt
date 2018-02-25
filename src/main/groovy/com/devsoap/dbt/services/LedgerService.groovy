package com.devsoap.dbt.services

import com.devsoap.dbt.BlockTransaction
import groovy.util.logging.Log
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent

@Log
class LedgerService implements Service {

    static final transient List<BlockTransaction> transactions = []

    BlockTransaction fetchTransaction(String transactionId) {
        log.info("Fetching transaction $transactionId")
        log.info("Transactions:$transactions")
        transactions.find {it.id == transactionId}
    }

    String newTransaction(BlockTransaction transaction) {
        log.info("Adding new transaction $transaction.id")
        transactions << transaction
        transaction.id
    }

    String updateTransaction(BlockTransaction transaction) {
        log.info("Updating transaction $transaction.id")
        def existingTransaction = fetchTransaction(transaction.id)
        def index = transactions.indexOf(existingTransaction)
        transactions.remove(index)
        transactions.add(index, transaction)
    }
}
