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
package se.liquidbytes.jel.database.handlers.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import se.liquidbytes.jel.database.handlers.User;

/**
 *
 * @author Henrik Östman
 */
public class UserImpl implements User {

    private final Vertx vertx;
    private final ODatabaseDocumentTx dbDoc;

    /**
     * Construktor
     *
     * @param vertx
     * @param dbDoc
     */
    public UserImpl(Vertx vertx, ODatabaseDocumentTx dbDoc) {
        this.vertx = vertx;
        this.dbDoc = dbDoc;
    }

    @Override
    public void getUser(String id, Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getUserByUsername(String username, Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getUsers(Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addUser(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateUser(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeUser(String id, Handler<AsyncResult<String>> resultHandler) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
