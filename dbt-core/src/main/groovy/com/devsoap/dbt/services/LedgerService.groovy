package com.devsoap.dbt.services

import com.devsoap.dbt.data.BlockTransaction
import com.devsoap.dbt.data.LedgerData
import groovy.util.logging.Slf4j
import ratpack.exec.Promise
import ratpack.service.Service
import ratpack.session.Session

@Slf4j
class LedgerService implements Service {

    private static final LedgerData data = new LedgerData()

    Promise<Optional<BlockTransaction>> fetchTransaction(Session session, String transactionId) {
        Promise.value(Optional.ofNullable(data.transactions.find {it.id == transactionId}))
    }

    Promise<List<BlockTransaction>> allTransactions(Session session) {
        Promise.value(data.transactions)
    }

    Promise<String> newTransaction(Session session, BlockTransaction transaction) {
        log.info("Adding new transaction $transaction.id to session ${session.id}")
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }

    Promise<String> updateTransaction(Session session, BlockTransaction transaction) {
        log.info("Updating transaction $transaction.id in session ${session.id}")
        data.transactions.removeAll {it.id == transaction.id}
        data.transactions.add(transaction)
        Promise.value(transaction.id)
    }
}
