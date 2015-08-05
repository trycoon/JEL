/*
 * Copyright 2015 Henrik Östman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    //TODO: Use config.storagepath!
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
        DatabaseConnection conn = new DatabaseConnectionImpl(vertx, db);
        future.complete(conn);
      } catch (Throwable e) {
        future.fail(e);
      }
    }, handler::handle);
  }

}
