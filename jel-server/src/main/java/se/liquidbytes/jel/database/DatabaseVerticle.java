/*
 * Copyright (c) 2014, Henrik Östman, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package se.liquidbytes.jel.database;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;
import java.lang.invoke.MethodHandles;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henrik Östman
 */
public final class DatabaseVerticle extends AbstractVerticle {

    // https://github.com/orientechnologies/orientdb/wiki/Embedded-Server
    // http://www.orientechnologies.com/docs/2.0/orientdb.wiki/Tutorial-Document-and-graph-model.html
    // http://www.orientechnologies.com/docs/last/orientdb.wiki/SQL-Insert.html
    // https://github.com/orientechnologies/orientdb/wiki/Document-Database#asynchronous-query
    // http://www.orientechnologies.com/docs/last/orientdb.wiki/Time-series-use-case.html
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private OServer server;
    private ODatabaseDocumentTx db;

    /**
     * Default constructor.
     */
    public DatabaseVerticle() {
        // Nothing
    }

    /**
     *
     */
    @Override
    public void start() {

        logger.info("Starting up database server");

        // Start embedded OrientDB-server
        try {
            server = OServerMain.create();
            server.removeShutdownHook();    // Preventing us from stopping. We invoke server.close ourself.
            server.startup(generateConfig());
            server.activate();
        } catch (IllegalArgumentException ex) {
            logger.error("Probably syntax error in configuration to database-server.", ex);
            System.exit(2);
        } catch (Exception ex) {
            logger.error("general error starting database-server.", ex);
            System.exit(2);
        }

        // Create database if not existing and establish a connection.
        db = new ODatabaseDocumentTx("plocal:./storage/databases/jel");
        if (!db.exists()) {
            db.create();
        } else {
            // TODO: Checkup connection-pooling. com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
            db.open("admin", "admin");
        }

        // Setup Vertx-service-proxy that acts as the API-router for the database against the rest of the application.
        Database database = Database.create(vertx, db);
        ProxyHelper.registerService(Database.class, vertx, database, "jel.database");

        logger.info("Database up and running");

        // TEST, remove later
        Database proxy = Database.createProxy(vertx, "jel.database");
        proxy.site(res -> {
            if (res.succeeded()) {
                res.result().getSite("123", res2 -> {
                    if (res2.succeeded()) {
                        logger.info("got something " + res2.result());
                    }
                });
            }
        });
    }

    /**
     *
     */
    @Override
    public void stop() {
        if (server != null) {
            logger.info("Shuting down databaseserver.");

            if (db != null) {
                db.close();
                db = null;
            }

            server.shutdown();
            server = null;
        }
    }

    /**
     *
     * @return
     */
    private String generateConfig() {

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<orient-server>")
                .append("  <network>")
                .append("    <protocols>")
                .append("       <protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>")
                .append("       <protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>")
                .append("    </protocols>")
                .append("    <listeners>")
                .append("      <listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>")
                .append("      <listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>")
                .append("    </listeners>")
                .append("  </network>")
                .append("  <users>")
                .append("    <user name=\"root\" password=\"OEVkk5t7!\" resources=\"*\"/>")
                .append("  </users>")
                .append("  <properties>")
                .append("    <entry name=\"log.console.level\" value=\"info\"/>")
                .append("    <entry name=\"log.file.level\" value=\"fine\"/>")
                //The following is required to eliminate an error or warning, "Error on resolving property: ORIENTDB_HOME"
                .append("    <entry name=\"plugin.dynamic\" value=\"false\"/>")
                .append("  </properties>")
                .append("</orient-server>");

        return builder.toString();
    }

}
