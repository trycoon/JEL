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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import se.liquidbytes.jel.database.DatabaseConnection;

/**
 *
 * @author Henrik Östman
 */
public class DatabaseConnectionImpl implements DatabaseConnection {

  private final Vertx vertx;
  private final ODatabaseDocumentTx conn;

  /**
   * Constructor
   *
   * @param vertx
   * @param conn
   */
  public DatabaseConnectionImpl(Vertx vertx, ODatabaseDocumentTx conn) {
    this.vertx = vertx;
    this.conn = conn;
  }

  /*@Override
   public void close(Handler<AsyncResult<Void>> handler) {
   throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }*/
  @Override
  public DatabaseConnection getSite(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new GetSite(vertx, conn, id).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection getSites(Handler<AsyncResult<JsonArray>> resultHandler) {
    //new GetSites(vertx, conn).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection addSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new AddSite(vertx, conn, document).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection updateSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new UpdateSite(vertx, conn, document).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection removeSite(String id, Handler<AsyncResult<Void>> resultHandler) {
    //new RemoveSite(vertx, conn, id).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection getUser(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new GetUser(vertx, conn, id).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection getUserByUsername(String username, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new GetUserByUsername(vertx, conn, username).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection getUsers(Handler<AsyncResult<JsonArray>> resultHandler) {
    //new GetUsers(vertx, conn).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection addUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new AddUser(vertx, conn, document).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection updateUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler) {
    //new UpdateUser(vertx, conn, document).execute(resultHandler);
    return this;
  }

  @Override
  public DatabaseConnection removeUser(String id, Handler<AsyncResult<Void>> resultHandler) {
    //new RemoveUser(vertx, conn, id).execute(resultHandler);
    return this;
  }

}
