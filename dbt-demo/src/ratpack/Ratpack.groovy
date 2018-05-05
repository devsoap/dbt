import com.devsoap.dbt.DBTModule
import com.devsoap.dbt.config.DBTConfig
import com.devsoap.dbt.demo.DatabaseService
import com.devsoap.dbt.services.TransactionManagerService
import org.h2.jdbcx.JdbcDataSource
import ratpack.form.Form
import ratpack.handlebars.HandlebarsModule
import ratpack.http.Status

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack
import static ratpack.handlebars.Template.handlebarsTemplate

ratpack {

  serverConfig {
    sysProps()
    require('/dbt', DBTConfig)
  }

  bindings {
    module HandlebarsModule

    module (DBTModule) {
      it.ledger.remoteUrl = 'http://localhost:5050/ledger'
      it.executor.remoteUrl = 'http://localhost:5050/executor'
    }

    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))
    bind DatabaseService
  }

  handlers {

    get {
      render handlebarsTemplate('index.html')
    }

    post('addQueryToTransaction') {
      def transactionManager = get(TransactionManagerService)
      parse(Form).then { Form form ->
        def query = form.get('query')
        def transactionId = form.get('transactionId')
        if(transactionId) {
          transactionManager.execute(transactionId) {
            it.query(query)
          } .then {
            response.send(it.toString())
          }
        } else {
          transactionManager.execute {
            it.query(query)
          } .then {
            response.send(it.toString())
          }
        }
      }
    }

    post('executeTransaction') {
      def transactionManager = get(TransactionManagerService)
      parse(Form).then { Form form ->
        def transactionId = form.get('transactionId')
        transactionManager.execute(transactionId) {
          it.complete()
        }.onError {
          response.status(Status.of(501))
          response.send(it.message)
        }.then {
          response.send(it.toString())
        }
      }
    }
  }
}
