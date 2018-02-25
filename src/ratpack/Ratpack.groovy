import com.devsoap.dbt.app.DatabaseService
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.DBTManager
import com.devsoap.dbt.handlers.ExecutorHandler
import com.devsoap.dbt.handlers.LedgerHandler
import com.devsoap.dbt.services.LedgerService
import com.fasterxml.jackson.databind.ObjectMapper
import org.h2.jdbcx.JdbcDataSource

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack

ratpack {

  serverConfig {

    /**
     * DBT Framework config
     */
    require("", DBTConfig)
  }

  bindings {
    bindInstance(new ObjectMapper())
    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))
    bindInstance(new DatabaseService())


    /**
     * DBT Framework manager
     */
    bind(DBTManager)
    bind(LedgerService)
    bind(ExecutorHandler)
    bind(LedgerHandler)
  }

  handlers {

    /**
     *  DBT Framework handlers
     */
    path('executor', ExecutorHandler)
    path('ledger', LedgerHandler)

    /**
     * Consumer services
     */
    get('frontend') {
      get(DBTManager).execute { transaction ->
        transaction.query("INSERT INTO LOGS(LOG_ID,LOG_VALUE) VALUES (${new Random().nextInt()}, 'HELLO')")
      }.then {
        redirect("/gateway/${it['id'].textValue()}")
      }
    }

    get('gateway/:transactionId?') {
      get(DBTManager).execute(pathTokens.transactionId, { transaction ->
        transaction.query("INSERT INTO LOGS(LOG_ID,LOG_VALUE) VALUES (${new Random().nextInt()}, 'WORLD')")
      }).then {
        redirect("/gateway2/${it['id'].textValue()}")
      }
    }

    get('gateway2/:transactionId?') {
      get(DBTManager).execute(pathTokens.transactionId, { transaction ->
        transaction.query("SELECT * FROM LOGS")
        transaction.complete()
      }).then {
        render it.toString()
      }
    }
  }
}
