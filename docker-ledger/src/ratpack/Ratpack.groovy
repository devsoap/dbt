import com.devsoap.dbt.modules.DBTLedgerModule
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
        module (DBTLedgerModule)  { config ->
            log.info("Using Mongo database at $config.ledger.databaseUrl")
            bindInstance(MongoClient, new MongoClient(new MongoClientURI(config.ledger.databaseUrl)))
        }
    }
}
