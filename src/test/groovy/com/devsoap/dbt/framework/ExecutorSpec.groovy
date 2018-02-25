package com.devsoap.dbt.framework

import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import spock.lang.AutoCleanup
import spock.lang.Specification

class ExecutorSpec extends Specification {

    def mapper = new ObjectMapper()

    @AutoCleanup
    def aut = new GroovyRatpackMainApplicationUnderTest()



}
