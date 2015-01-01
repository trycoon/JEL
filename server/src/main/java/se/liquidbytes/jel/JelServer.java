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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.Settings;

/**
 *
 * @author Henrik Östman
 */
public class JelServer extends AbstractVerticle {
    
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Starting point
     *
     * @param args
     */
    public static void main(String[] args) {
        
        logger.info("Starting JEL-server");
        
        try {
            Settings.init(args);
            
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new JelServer());
            
        } catch (Throwable ex) {
            logger.error("General error caught during server deployment, application is shutting down. Message: {}.", ex.getMessage(), ex.getCause());
            System.exit(1);
        }
        
    }

    /**
     *
     */
    @Override
    public void start() {
        
        logger.info(Settings.getInformationString());
        
        try {
            
            startServer();
            
        } catch (Throwable ex) {
            logger.error("General error caught during server-startup, application is shutting down. Message: {}.", ex.getMessage(), ex.getCause());
            System.exit(1);
        }
    }

    /**
     *
     */
    /*@Override
     public void stop() {

     }*/

    /*  Runtime.getRuntime().addShutdownHook(new Thread() {
     public void run() {
     try { LogService.this.stop(); }
     catch (InterruptedException ignored) {}
     }
     });*/
    private void startServer() {
        vertx.createHttpServer(new HttpServerOptions().setPort(8080)).requestHandler(req -> req.response().end("Hello World!")).listen();
        
        logger.info("JEL-server is up and running on port {}", Settings.get("port"));        
    }
}
