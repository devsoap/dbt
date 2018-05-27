@Grapes([
        @Grab('io.ratpack:ratpack-groovy:1.5.4'),
        @Grab('org.slf4j:slf4j-simple:1.7.25')
])

import com.devsoap.dbt.DBTModule

import static ratpack.groovy.Groovy.ratpack

ratpack {
    bindings {
        module (DBTModule) { config ->
            config.executor.remoteUrl = System.properties.getProperty('dbt.executor.url')
        }
    }
}
