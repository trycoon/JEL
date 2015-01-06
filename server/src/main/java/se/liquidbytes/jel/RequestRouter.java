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
package se.liquidbytes.jel;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.core.Router;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.Settings;

/**
 *
 * @author Henrik Östman
 */
public class RequestRouter extends AbstractVerticle {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     *
     */
    @Override
    public void start() {

        logger.info("Starting up request-handler");

        Router router = Router.router(vertx);

        if (Settings.get("requestlog").equals("true")) {
            io.vertx.ext.apex.addons.Logger loggerHandler = io.vertx.ext.apex.addons.Logger.logger(true, io.vertx.ext.apex.addons.Logger.Format.SHORT);
            router.route().handler(loggerHandler);
        }
        
        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(Integer.valueOf(Settings.get("port"))).setHost("localhost"));
        server.requestHandler(router::accept);

        // Paths starting with `/static` serve as static resources (from filesystem or classpath)
        //router.route("/static").handler(StaticServer.staticServer("static"));

        // Paths starting with `/dynamic` return pages generated from handlebars templates 
        //router.route("/dynamic").handler(HandlebarsTemplateEngine.create());
        // Create a sub router for our REST API
        Router apiRouter = Router.router(vertx);
        
        // We need body parsing
        //apiRouter.route(BodyHandler.bodyHandler());
        /*apiRouter.route("/sites")
                .method(POST)
                .consumes("application/json")
                .handler(context -> {
                    JsonObject site = context.getBodyAsJson();
                    // .... store the site
                    context.response().end(); // Send back 200-OK
                });*/
        /*apiRouter.route("/sites")
                .method(GET)
                .produces("application/json")
                .handler(context -> {
                    context.response().end("{[{site: {id: 123, name: \"hepp\"}]}"); // Send back 200-OK
                });*/
        // ... more API 
        // attach the sub router to the main router at the mount point "/api"
        router.mountSubRouter("/api", apiRouter);
        
        server.listen();

    }

    /**
     *
     */
    /*@Override
     public void stop() {

     }*/
}
