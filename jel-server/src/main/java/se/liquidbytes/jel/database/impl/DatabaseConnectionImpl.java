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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import se.liquidbytes.jel.database.DatabaseConnection;
import se.liquidbytes.jel.database.impl.actions.GetSite;
import se.liquidbytes.jel.database.impl.actions.GetSites;

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
        new GetSite(vertx, conn, id).execute(resultHandler);
        return this;
    }

    @Override
    public DatabaseConnection getSites(Handler<AsyncResult<JsonArray>> resultHandler) {
        new GetSites(vertx, conn).execute(resultHandler);
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
