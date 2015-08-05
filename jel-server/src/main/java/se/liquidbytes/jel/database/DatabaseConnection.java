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
package se.liquidbytes.jel.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.database.impl.DatabaseConnectionImpl;

/**
 *
 * @author Henrik Östman
 */
//@ProxyGen // Generate the proxy and handler
public interface DatabaseConnection {

  static DatabaseConnection create(Vertx vertx) {
    return new DatabaseConnectionImpl(vertx, null); //TODO: Replace null with right value. Check out MongoService-project.
  }

  static DatabaseConnection createProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(DatabaseConnection.class, vertx, address);
  }
  /**
   * Closes the connection. Important to always close the connection when you are done so it's returned to the pool.
   *
   * @param handler the handler called when this operation completes.
   */
  // void close(Handler<AsyncResult<Void>> handler);*/

  /*
   ===========================================================================
   | SITE - interface
   ===========================================================================
   */
  /**
   *
   * @param id
   * @param resultHandler
   * @return
   */
  DatabaseConnection getSite(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param resultHandler
   * @return
   */
  DatabaseConnection getSites(Handler<AsyncResult<JsonArray>> resultHandler);

  /**
   *
   * @param document
   * @param resultHandler
   * @return
   */
  DatabaseConnection addSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param document
   * @param resultHandler
   * @return
   */
  DatabaseConnection updateSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param id
   * @param resultHandler
   * @return
   */
  DatabaseConnection removeSite(String id, Handler<AsyncResult<Void>> resultHandler);

  /*
   ===========================================================================
   | USER - interface
   ===========================================================================
   */
  /**
   *
   * @param id
   * @param resultHandler
   * @return
   */
  DatabaseConnection getUser(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param username
   * @param resultHandler
   * @return
   */
  DatabaseConnection getUserByUsername(String username, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param resultHandler
   * @return
   */
  DatabaseConnection getUsers(Handler<AsyncResult<JsonArray>> resultHandler);

  /**
   *
   * @param document
   * @param resultHandler
   * @return
   */
  DatabaseConnection addUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param document
   * @param resultHandler
   * @return
   */
  DatabaseConnection updateUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

  /**
   *
   * @param id
   * @param resultHandler
   * @return
   */
  DatabaseConnection removeUser(String id, Handler<AsyncResult<Void>> resultHandler);
}
