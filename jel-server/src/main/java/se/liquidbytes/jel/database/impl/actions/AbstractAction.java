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
package se.liquidbytes.jel.database.impl.actions;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import se.liquidbytes.jel.JelException;

/**
 *
 * @author Henrik Östman
 * @param <T>
 */
public abstract class AbstractAction<T> implements Handler<Future<T>> {

    protected final Vertx vertx;
    protected final ODatabaseDocumentTx conn;

    /**
     * Constructor
     *
     * @param vertx
     * @param conn
     */
    protected AbstractAction(Vertx vertx, ODatabaseDocumentTx conn) {
        this.vertx = vertx;
        this.conn = conn;
    }

    @Override
    public void handle(Future<T> future) {
        try {
            T result = execute(conn);
            future.complete(result);
        } catch (Throwable e) {
            future.fail(e);
        }
    }

    public void execute(Handler<AsyncResult<T>> resultHandler) {
        vertx.executeBlocking(this, resultHandler);
    }

    protected abstract T execute(ODatabaseDocumentTx conn) throws JelException;

}
