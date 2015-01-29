/*
 * Copyright (c) 2015, Henrik Östman, All rights reserved.
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
package se.liquidbytes.jel.database.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.lang.invoke.MethodHandles;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.database.DatabaseConnection;
import se.liquidbytes.jel.database.DatabaseService;

/**
 *
 * @author Henrik Östman
 */
public class DatabaseServiceImpl implements DatabaseService {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Vertx vertx;
    private final String config;
    private OServer server;
    private ODatabaseDocumentTx db;

    /**
     * Constructor
     *
     * @param vertx
     * @param config
     */
    public DatabaseServiceImpl(Vertx vertx, String config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    public void start() {
        logger.info("Starting up database server");

        // Start embedded OrientDB-server
        try {
            server = OServerMain.create();
            server.removeShutdownHook();    // Preventing us from stopping. We invoke server.close ourself.
            server.startup(this.config);
            server.activate();
        } catch (IllegalArgumentException ex) {
            logger.error("Probably syntax error in configuration to database-server.", ex);
            System.exit(2);
        } catch (Exception ex) {
            logger.error("general error starting database-server.", ex);
            System.exit(2);
        }

        //TODO: Maybe this should be moved to getConnection()?
        // Create database if not existing and establish a connection.
        db = new ODatabaseDocumentTx("plocal:./storage/databases/jel");
        if (!db.exists()) {
            db.create();
        } else {
            // TODO: Checkup connection-pooling. com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
            db.open("admin", "admin");
        }

        logger.info("Database up and running");
    }

    @Override
    public void stop() {
        if (server != null) {
            logger.info("Shuting down databaseserver.");

            vertx.executeBlocking(future -> {
                try {
                    if (db != null) {
                        db.close();
                        db = null;
                    }

                    server.shutdown();
                    server = null;
                    future.complete();
                } catch (Throwable e) {
                    future.fail(e);
                }
            }, null);
        }
    }

    @Override
    public void getConnection(Handler<AsyncResult<DatabaseConnection>> handler) {
        vertx.executeBlocking(future -> {
            try {
                DatabaseConnection conn = null; // = new DatabaseConnectionImpl(vertx, dataSource.getConnection());
                future.complete(conn);
            } catch (Throwable e) {
                future.fail(e);
            }
        }, handler::handle);
    }
    

}
