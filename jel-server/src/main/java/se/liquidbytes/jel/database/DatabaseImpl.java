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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import se.liquidbytes.jel.database.handlers.Site;
import se.liquidbytes.jel.database.handlers.User;
import se.liquidbytes.jel.database.handlers.impl.SiteImpl;
import se.liquidbytes.jel.database.handlers.impl.UserImpl;

/**
 *
 * @author Henrik Östman
 */
public class DatabaseImpl implements Database {

    private final Vertx vertx;
    private final ODatabaseDocumentTx dbDoc;

    /**
     * Constructor
     *
     * @param vertx
     * @param dbDoc
     */
    public DatabaseImpl(Vertx vertx, ODatabaseDocumentTx dbDoc) {
        this.vertx = vertx;
        this.dbDoc = dbDoc;
    }

    /**
     *
     * @param resultHandler
     */
    @Override
    public void site(Handler<AsyncResult<Site>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(new SiteImpl(vertx, dbDoc)));
    }

    /**
     *
     * @param resultHandler
     */
    @Override
    public void user(Handler<AsyncResult<User>> resultHandler) {
        resultHandler.handle(Future.succeededFuture(new UserImpl(vertx, dbDoc)));
    }

}
