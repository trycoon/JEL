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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.database.DatabaseServiceVerticle;
import se.liquidbytes.jel.system.Settings;
import se.liquidbytes.jel.system.SystemInfo;

/**
 *
 * @author Henrik Östman
 */
public final class JelServer {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static String databaseVerticleReference;
    private static String requestVerticleReference;
    private static Vertx vertx;

    /**
     * Starting point
     *
     * @param args
     */
    public static void main(String[] args) {

        logger.info("Starting JEL-server");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownServer();
            }
        });

        try {

            // Load settings and parse arguments
            Settings.init(args);
            logger.info(SystemInfo.getStartupInformation());
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

        vertx = Vertx.vertx();
        // Startup database-system as own worker-verticle.
        DeploymentOptions deployOptions = new DeploymentOptions();
        deployOptions.setInstances(1);
        deployOptions.setWorker(true);
        vertx.deployVerticle(new DatabaseServiceVerticle(), deployOptions, db_res -> {
            databaseVerticleReference = db_res.result();

            if (db_res.succeeded()) {
                // Startup verticle handling HTTP-requests for static and dynamic HTML.
                vertx.deployVerticle(new RequestRouterVerticle(), req_res -> {
                    requestVerticleReference = db_res.result();

                    if (req_res.succeeded()) {
                        logger.info("JEL-server is up and running on port {}", Settings.get("port"));
                    } else {
                        logger.error("Failed to deploy request-verticle.", req_res.cause());
                        System.exit(2);
                    }
                });
            } else {
                logger.error("Failed to deploy database-verticle.", db_res.cause());
                System.exit(2);
            }
        });
    }

    /**
     * Shutdown server, should only be called upon by shutdown-hook. Closes
     * internal threads and release resources.
     */
    private static void shutdownServer() {
        logger.info("Shuting down JEL-server");

        if (vertx != null) {

            // Undeploy started verticles in order, then shutdown Vertx itself.
            // We are adding some delays to give the threads time to shut down properly.
            // The application got a smoother and less error-prone shutdown by doing so.
            if (requestVerticleReference != null) {
                logger.debug("Undeploying request-verticle.");
                vertx.undeployVerticle(requestVerticleReference);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }
            if (databaseVerticleReference != null) {
                logger.debug("Undeploying database-verticle.");
                vertx.undeployVerticle(databaseVerticleReference);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }

            //TODO: Shutdown adapters!
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // Ignore
            }

            logger.debug("Closing Vertx.io.");
            vertx.close();
        }
    }
}
