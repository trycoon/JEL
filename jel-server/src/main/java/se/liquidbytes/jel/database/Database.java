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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.database.handlers.Site;
import se.liquidbytes.jel.database.handlers.User;

/**
 *
 * @author Henrik Östman
 */
@ProxyGen
@VertxGen
public interface Database {

    /**
     *
     * @param vertx
     * @param dbDoc
     * @return
     */
    static Database create(Vertx vertx, ODatabaseDocumentTx dbDoc) {
        return new DatabaseImpl(vertx, dbDoc);
    }

    /**
     *
     * @param vertx
     * @param address
     * @return
     */
    static Database createProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(Database.class, vertx, address);
    }

    /**
     *
     * @param resultHandler
     */
    void site(Handler<AsyncResult<Site>> resultHandler);

    /**
     *
     * @param resultHandler
     */
    void user(Handler<AsyncResult<User>> resultHandler);

}
