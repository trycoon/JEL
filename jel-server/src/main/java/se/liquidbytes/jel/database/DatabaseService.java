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

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.database.impl.DatabaseServiceImpl;

/**
 *
 * @author Henrik Östman
 */
@ProxyGen
@VertxGen
public interface DatabaseService {

    /**
     *
     * @param vertx
     * @param config
     * @return
     */
    static DatabaseService create(Vertx vertx, String config) {
        return new DatabaseServiceImpl(vertx, config);
    }

    /**
     *
     * @param vertx
     * @param address
     * @return
     */
    static DatabaseService createEventBusProxy(Vertx vertx, String address) {
        return ProxyHelper.createProxy(DatabaseService.class, vertx, address);
    }

    /**
     * Returns a connection that can be used to perform database operations on.
     * It's important to remember to close the connection when you are done, so
     * it is returned to the pool.
     *
     * @param handler the handler which is called when the
     * <code>DatabaseConnection</code> object is ready for use.
     */
    void getConnection(Handler<AsyncResult<DatabaseConnection>> handler);

    /**
     * Normally invoked by the <code>DatabaseServiceVerticle</code> to start the
     * service when deployed. This is usually not called by the user.
     */
    @ProxyIgnore
    public void start();

    /**
     * Normally invoked by the <code>DatabaseServiceVerticle</code> to stop the
     * service when the verticle is stopped/undeployed. This is usually not
     * called by the user.
     */
    @ProxyIgnore
    public void stop();
    
}
