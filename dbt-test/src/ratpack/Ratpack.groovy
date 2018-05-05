import com.devsoap.dbt.DBTModule
import com.devsoap.dbt.config.DBTConfig
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

    module (DBTModule) {
      it.ledger.remoteUrl = 'http://localhost:8888/ledger'
      it.executor.remoteUrl = 'http://localhost:8888/executor'
    }

    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))
    bind DatabaseService
  }
}

class DatabaseService implements Service {

  @Override
  void onStart(StartEvent event){
    new Flyway(dataSource: event.registry.get(DataSource)).migrate()
  }
}
