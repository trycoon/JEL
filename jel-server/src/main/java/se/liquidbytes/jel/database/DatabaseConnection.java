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
package se.liquidbytes.jel.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author Henrik Östman
 */
@VertxGen
@ProxyGen
public interface DatabaseConnection {

    /**
     * Closes the connection. Important to always close the connection when you
     * are done so it's returned to the pool.
     *
     * @param handler the handler called when this operation completes.
     */
    /*@ProxyClose
     void close(Handler<AsyncResult<Void>> handler);*/

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
    @Fluent
    DatabaseConnection getSite(String id, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection getSites(Handler<AsyncResult<JsonArray>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection addSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection updateSite(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param id
     * @param resultHandler
     * @return
     */
    @Fluent
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
    @Fluent
    DatabaseConnection getUser(String id, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param username
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection getUserByUsername(String username, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection getUsers(Handler<AsyncResult<JsonArray>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection addUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection updateUser(JsonObject document, Handler<AsyncResult<JsonObject>> resultHandler);

    /**
     *
     * @param id
     * @param resultHandler
     * @return
     */
    @Fluent
    DatabaseConnection removeUser(String id, Handler<AsyncResult<Void>> resultHandler);
}
