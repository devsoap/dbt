import com.devsoap.dbt.modules.DBTLedgerModule
import com.devsoap.dbt.services.LedgerService
import com.devsoap.dbt.services.MongoLedgerService
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import org.slf4j.LoggerFactory

import static ratpack.groovy.Groovy.ratpack

def log = LoggerFactory.getLogger('dbt-ledger')

ratpack {
    serverConfig {
        env()
        sysProps()
    }

    bindings {
        bind(LedgerService, MongoLedgerService)
        module (DBTLedgerModule)
    }
}
