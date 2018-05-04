package com.devsoap.dbt.demo

import org.flywaydb.core.Flyway
import ratpack.service.Service
import ratpack.service.StartEvent

import javax.sql.DataSource

class DatabaseService implements Service {

    @Override
    void onStart(StartEvent event){
        new Flyway(dataSource: event.registry.get(DataSource)).migrate()
    }
}
