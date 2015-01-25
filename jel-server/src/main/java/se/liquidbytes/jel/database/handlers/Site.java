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
package se.liquidbytes.jel.database.handlers;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 *
 * @author Henrik Östman
 */
@ProxyGen
public interface Site {

    /**
     *
     * @param id
     * @param resultHandler
     */
    void getSite(String id, Handler<AsyncResult<String>> resultHandler);

    /**
     *
     * @param resultHandler
     */
    void getSites(Handler<AsyncResult<String>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     */
    void addSite(JsonObject document, Handler<AsyncResult<String>> resultHandler);

    /**
     *
     * @param document
     * @param resultHandler
     */
    void updateSite(JsonObject document, Handler<AsyncResult<String>> resultHandler);

    /**
     *
     * @param id
     * @param resultHandler
     */
    void removeSite(String id, Handler<AsyncResult<String>> resultHandler);

}
