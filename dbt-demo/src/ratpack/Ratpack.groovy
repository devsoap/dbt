import com.devsoap.dbt.DBTModule
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.demo.DatabaseService
import com.devsoap.dbt.services.TransactionManagerService
import org.h2.jdbcx.JdbcDataSource

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {
    sysProps()
    require('/dbt', DBTConfig)
  }

  bindings {
    module (DBTModule) {
      it.ledger.remoteUrl = 'http://localhost:5050/ledger'
      it.executor.remoteUrl = 'http://localhost:5050/executor'
    }

    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))
    bind DatabaseService
  }

  handlers {

    /**
     * Consumer services
     */
    get('') {
      get(TransactionManagerService).execute { transaction ->
        transaction.query("INSERT INTO LOGS(LOG_ID,LOG_VALUE) VALUES (${new Random().nextInt()}, 'HELLO')")
      }.then {
        redirect("/gateway/${it['id'].textValue()}")
      }
    }

    get('gateway/:transactionId?') {
      get(TransactionManagerService).execute(pathTokens.transactionId, { transaction ->
        transaction.query("INSERT INTO LOGS(LOG_ID,LOG_VALUE) VALUES (${new Random().nextInt()}, 'WORLD')")
      }).then {
        redirect("/gateway2/${it['id'].textValue()}")
      }
    }

    get('gateway2/:transactionId?') {
      get(TransactionManagerService).execute(pathTokens.transactionId, { transaction ->
        transaction.query("SELECT * FROM LOGS")
        transaction.complete()
      }).then {
        redirect('/ledger')
      }
    }
  }
}
