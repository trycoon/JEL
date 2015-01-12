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
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        /* Router router = Router.router(vertx);

        if (Settings.get("requestlog").equals("true")) {
            io.vertx.ext.apex.addons.Logger loggerHandler = io.vertx.ext.apex.addons.Logger.logger(true, io.vertx.ext.apex.addons.Logger.Format.SHORT);
            router.route().handler(loggerHandler);
        }

        HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(Integer.valueOf(Settings.get("port"))).setHost("localhost"));
        server.requestHandler(router::accept);

        // Paths starting with '/dynamic' return pages generated from handlebars templates
        TemplateEngine engine = HandlebarsTemplateEngine.create("template/", "hbs");
        router.route("/dynamic").handler(TemplateHandler.templateHandler(engine, "mainpage", "text/x-handlebars-template"));

        // Paths starting with `/static` serve as static resources (from filesystem or classpath)
        router.route("/static").handler(StaticServer.staticServer("static/"));

        // Create a sub router for our REST API
        Router apiRouter = Router.router(vertx);

        // We need body parsing
        apiRouter.route().handler(BodyHandler.bodyHandler());
        apiRouter.route("/sites")
                .method(POST)
                .consumes("application/json")
                .handler(context -> {
                    JsonObject site = context.getBodyAsJson();
                    // .... store the site
                    context.response().end(); // Send back 200-OK
                });
        apiRouter.route("/sites")
                .method(GET)
                .produces("application/json")
                .handler(context -> {
                    context.response().end("{[{site: {id: 123, name: \"hepp\"}]}"); // Send back 200-OK
                });
        // ... more API 
        // attach the sub router to the main router at the mount point "/api"
        router.mountSubRouter("/api", apiRouter);

        server.listen();
*/
    }

    /**
     *
     */
    /*@Override
     public void stop() {

     }*/
}


/*
package atarno.vertx;

import io.vertx.core.http.*;
import io.vertx.ext.apex.addons.Favicon;
import io.vertx.ext.apex.addons.StaticServer;
import io.vertx.ext.apex.core.BodyHandler;
import io.vertx.ext.apex.core.FailureRoutingContext;
import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;

public class MultiHandlerTest extends VertxTestBase {

    String works = "http://localhost:8888/x/0";
    String stuck = "http://localhost:8888/x/1";

    public MultiHandlerTest() throws Exception {

        super();
        disableThreadChecks();
    }

    private void deploy1() {

        try {
            Router router = Router.router( vertx );

            for ( int i = 0; i < 2; i++ ) {
                Router exRouter = Router.router( vertx );
                exRouter.route()
                        .handler( BodyHandler.bodyHandler() );
                for ( HttpMethod method : HttpMethod.values() ) {
                    exRouter.route( "/x/" + i )
                            .method( method )
                            .handler( ctx -> {
                                String body = ctx.getBodyAsString();
                                ctx.response()
                                   .end( body );
                            } );
                }
                router.mountSubRouter( "/", exRouter );
            }

            setDefaultRoutes( router );

            CountDownLatch latch = new CountDownLatch( 1 );
            vertx.createHttpServer( new HttpServerOptions().setHost( "localhost" )
                                                           .setPort( 8888 )
                                                           .setAcceptBacklog( 10000 ) )
                 .requestHandler( router::accept )
                 .listen( result -> {
                     if ( result.succeeded() ) {
                         latch.countDown();
                     }
                     else {
                         fail( result.cause()
                                     .getMessage() );
                     }
                 } );
            awaitLatch( latch );
        }
        catch ( Exception e ) {
            fail( e.getMessage() );
        }
    }

    @Test
    public void test() throws InterruptedException {


        deploy1();

        HttpClientRequest req = vertx.createHttpClient( new HttpClientOptions() )
                                     .request( HttpMethod.POST, stuck );
        CountDownLatch latch = new CountDownLatch( 1 );
        req.handler( resp -> {
            System.out.println( resp.statusCode() );
            if ( resp.statusCode() == 200 ) {
                resp.bodyHandler( buffer -> {
                    System.out.println( buffer.getString( 0, buffer.length() ) );
                    latch.countDown();
                } );
            }
            try {
                assertEquals( 200, resp.statusCode() );
            }
            catch ( Exception e ) {
                fail( e.getMessage() );
            }

        } ).end( "{\"number\":1,\"string\":\"hello world!\"}" );

        awaitLatch( latch );

    }

    private void setDefaultRoutes( Router router ) {

        router.route()
              .handler( Favicon.favicon() );

        //static files
        router.routeWithRegex( "/web/*" )
              .handler( StaticServer.staticServer()
                                    .setDirectoryListing( false ) )
              .failureHandler( ( FailureRoutingContext context ) -> {

                  context.response()
                         .setStatusCode( 404 )
                         .end();
              } );

        //no matcher - MUST be last in routers chain
        router.route()
              .handler( ( RoutingContext context ) -> {
                  context.fail( 404 );
              } );
    }

}
*/
