/*
 * Copyright (c) 2014, Henrik Östman, All rights reserved.
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
package se.liquidbytes.jel.adapter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;

/**
 *
 * @author Henrik Östman
 */
public abstract class BaseAdapter {

    class AdapterVerticle extends AbstractVerticle {

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }
    }

    protected final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String adapterVerticleId;
    private final String name;

    /**
     *
     * @param name
     */
    public BaseAdapter(String name) {
        this.name = name;
    }

    /**
     *
     */
    public void start() {

        if (adapterVerticleId == null) {
            Vertx vertx = Vertx.vertx();
            DeploymentOptions deployOptions = new DeploymentOptions();
            deployOptions.setWorker(true);
            vertx.deployVerticle(new AdapterVerticle(), deployOptions, (AsyncResultHandler<String>) (AsyncResult<String> event) -> {
                if (event.failed()) {
                    throw new JelException(String.format("Failed to start adapter '%s'.", name), event.cause());
                }

                adapterVerticleId = event.result();
            });
        }
    }

    /**
     *
     */
    public void stop() {
        if (adapterVerticleId != null) {
            Vertx vertx = Vertx.vertx();
            vertx.undeployVerticle(adapterVerticleId, (AsyncResultHandler<Void>) (AsyncResult<Void> event) -> {
                if (event.failed()) {
                    logger.error("Failed to stop adapter '{}'.", name, event.cause());
                }

                if (event.succeeded()) {
                    adapterVerticleId = null;
                }
            });
        }
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }
}
