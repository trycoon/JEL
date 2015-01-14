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
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import io.vertx.core.AbstractVerticle;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henrik Östman
 */
public class OrientDB extends AbstractVerticle {

    //
    // USE VERTX-SERVICE-PROXY! https://github.com/vert-x3/vertx-service-proxy
    //
    // https://github.com/orientechnologies/orientdb/wiki/Embedded-Server
    // http://www.orientechnologies.com/docs/2.0/orientdb.wiki/Tutorial-Document-and-graph-model.html
    // http://www.orientechnologies.com/docs/last/orientdb.wiki/SQL-Insert.html
    // http://www.orientechnologies.com/docs/last/orientdb.wiki/Time-series-use-case.html
    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private OServer server;

    @Override
    public void start() {

        logger.info("Starting up database server");

        // Start embedded OrientDB-server
        try {
            server = OServerMain.create();
            server.startup(generateConfig());
            server.activate();

        } catch (ClassNotFoundException ex) {
            logger.error("fel", ex);
        } catch (InstantiationException ex) {
            logger.error("fel", ex);
        } catch (IllegalAccessException ex) {
            logger.error("fel", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("fel", ex);
        } catch (SecurityException ex) {
            logger.error("fel", ex);
        } catch (InvocationTargetException ex) {
            logger.error("fel", ex);
        } catch (NoSuchMethodException ex) {
            logger.error("fel", ex);
        } catch (IOException ex) {
            logger.error("fel", ex);
        } catch (Exception ex) {
            logger.error("fel", ex);
        }

        // Create database in not existing and create an connection.
        ODatabaseDocumentTx db = new ODatabaseDocumentTx("plocal:./storage/databases/jel");
        if (!db.exists()) {
            db.create();
        } else {
            // TODO: Checkup connection-pooling. com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
            db.open("admin", "admin");
        }

        try {
            ODocument site = new ODocument("Site");
            site.field("name", "Gaudi");
            site.field("location", "Madrid");
            site.field("city", new ODocument("City").field("name", "Rome").field("country", "Italy"));
            site.save();
        } finally {
            db.close();
        }

        // Setup eventbus-listener.
        vertx.eventBus().consumer("storage.handler", message -> {
            logger.info("Msg: " + message.body());
        });

        // Send a test-message.
        vertx.eventBus().send("storage.handler", "hello");

        logger.info("Database up and running");
    }

    @Override
    public void stop() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }

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
                //.append("    <entry name=\"server.database.path\" value=\"./storage/database\"/>")
                //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
                .append("    <entry name=\"plugin.dynamic\" value=\"false\"/>")
                .append("  </properties>")
                .append("</orient-server>");

        return builder.toString();
    }
}
