import com.devsoap.dbt.DBTModule
import com.devsoap.dbt.services.TransactionManagerService
import org.flywaydb.core.Flyway
import org.h2.jdbcx.JdbcDataSource
import ratpack.form.Form
import ratpack.handlebars.HandlebarsModule
import ratpack.http.Status
import ratpack.service.Service

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack
import static ratpack.handlebars.Template.handlebarsTemplate

/*
 * Ratpack main configuration
 */
ratpack {

  /*
   * Configure application bindings
   */
  bindings {

    /*
     * Render views with Handlebars
     */
    module HandlebarsModule

    /*
     *  Configure dbt module to point to correct ledger and executor instances
     */
    module (DBTModule) { config ->
      config.ledger.remoteUrl = 'http://localhost:5050/ledger'
      config.executor.remoteUrl = 'http://localhost:5050/executor'
    }

    /*
     * Configure data source to use H2 in-memory database
     */
    bindInstance(DataSource, new JdbcDataSource(url: 'jdbc:h2:mem:dbtdb;DB_CLOSE_DELAY=-1', user: ''))

    /*
     * Migrate Flyway migrations at application start
     */
    bindInstance { event -> new Flyway(dataSource: event.registry.get(DataSource)).migrate() } as Service
  }


  /*
   * Configure API endpoints
   */
  handlers {

    /*
     * Render index.html at root url
     */
    get {
      render handlebarsTemplate('index.html')
    }

    /*
     * Adds a new query to the current transaction
     */
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

    /*
     * Executes the current transaction by marking the transaction as complete and executing the transaction
     */
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

    /*
     * Adds the query to a new transaction and executes the transaction immediately
     */
    post('executeQuery') {
      def transactionManager = get(TransactionManagerService)
      parse(Form).then { Form form ->
        def query = form.get('query')
        transactionManager.execute {
          it.query(query).complete()
        } .then {
          response.send(it.toString())
        }
      }
    }
  }
}
