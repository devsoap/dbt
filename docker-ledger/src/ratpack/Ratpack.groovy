import com.devsoap.dbt.DBTModule
import org.h2.jdbcx.JdbcDataSource
import org.slf4j.LoggerFactory

import javax.sql.DataSource

import static ratpack.groovy.Groovy.ratpack

def log = LoggerFactory.getLogger('dbt-ledger')

ratpack {
    serverConfig {
        env()
        sysProps()
    }

    bindings {
        module (DBTModule)  { config ->
            log.info "Executor available at $config.executor.remoteUrl"
            log.info "Ledger available at $config.ledger.remoteUrl"
        }
    }
}
