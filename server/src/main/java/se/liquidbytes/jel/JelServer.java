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
package se.liquidbytes.jel;

import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.database.OrientDB;
import se.liquidbytes.jel.system.Settings;

/**
 *
 * @author Henrik Östman
 */
public class JelServer {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Starting point
     *
     * @param args
     */
    public static void main(String[] args) {

        logger.info("Starting JEL-server");

        /*Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
         // Do stuff.
         }
         });*/
        try {

            // Load settings and parse arguments
            Settings.init(args);

            logger.info(Settings.getInformationString());

            startServer();

        } catch (Throwable ex) {
            logger.error("General error caught during server-startup, application is shutting down. Message: {}.", ex.getMessage(), ex.getCause());
            System.exit(1);
        }

    }

    /**
     * Startup application verticles
     */
    private static void startServer() {

        Vertx vertx = Vertx.vertx();

        // Startup database-system as own worker-verticle.
        DeploymentOptions deployOptions = new DeploymentOptions();
        deployOptions.setInstances(1);
        deployOptions.setWorker(true);
        vertx.deployVerticle(new OrientDB(), deployOptions, (AsyncResultHandler<String>) (AsyncResult<String> event) -> {
            if (event.failed()) {
                throw new JelException("Failed to deploy database-verticle.", event.cause());
            }

            if (event.succeeded()) {
                // Startup verticle handling HTTP-requests for static and dynamic HTML.
                vertx.deployVerticle(new RequestRouter(), (AsyncResultHandler<String>) (AsyncResult<String> req_event) -> {
                    if (req_event.failed()) {
                        throw new JelException("Failed to deploy request-verticle.", event.cause());
                    }

                    if (req_event.succeeded()) {
                        logger.info("JEL-server is up and running on port {}", Settings.get("port"));
                    }
                });

            }
        });

    }
}


// APEX-feedback:
// 
/*
vertx.deployVerticle(new RequestVerticle(), (AsyncResultHandler<String>) (AsyncResult<String> req_event) -> {
                    if (req_event.failed()) {
                        throw new JelException("Failed to deploy request-verticle.", event.cause());
                    }


*/