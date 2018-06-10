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

import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.modules.DBTExecutorModule
import com.devsoap.dbt.modules.DBTLedgerModule
import com.devsoap.dbt.services.InMemoryLedgerService
import com.devsoap.dbt.services.LedgerService
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import ratpack.service.Service
import ratpack.service.StartEvent

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {
    sysProps()
    require('/dbt', DBTConfig)
  }

  bindings {

    // Configure Ledger
    bind(LedgerService, InMemoryLedgerService)
    module (DBTLedgerModule)

    // Configure executor
    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))
    bind FlywayMigrationService
    module (DBTExecutorModule)
  }
}

class FlywayMigrationService implements Service {

  @Override
  void onStart(StartEvent event){
    new Flyway(dataSource: event.registry.get(DataSource)).migrate()
  }
}
