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

import com.devsoap.dbt.data.BlockTransaction
import ratpack.exec.Promise
import ratpack.service.Service

interface LedgerService extends Service {

    Promise<Optional<BlockTransaction>> fetchTransaction(String transactionId)

    Promise<List<BlockTransaction>> allTransactions()

    Promise<String> newTransaction(BlockTransaction transaction)

    Promise<String> updateTransaction(BlockTransaction transaction)
}