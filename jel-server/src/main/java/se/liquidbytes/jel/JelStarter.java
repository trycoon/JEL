/*
 * Copyright 2015 Henrik Östman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.liquidbytes.jel;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.database.DatabaseServiceVerticle;
import se.liquidbytes.jel.system.MainVerticle;
import se.liquidbytes.jel.web.WebserverVerticle;

/**
 * Class responsible for starting up and shutting down the application
 *
 * @author Henrik Östman
 */
public final class JelStarter {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Stack of id's of deployed verticles, could be used if we want to undeploy them in reverse order.
   */
  private static Deque<String> deploymentIds;
  /**
   * Vert.x instance
   */
  private static Vertx vertx;

  /**
   * Starting point
   *
   * @param args Arguments passed to the application during startup
   */
  public static void main(String[] args) {
    deploymentIds = new ArrayDeque();

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

    } catch (Exception ex) {
      logger.error("General error caught during server-startup, application is shutting down.", ex);
      System.exit(1);
    }
  }

  /**
   * Startup application verticles
   */
  private static void startServer() {

    logger.info("Starting JEL-server..." + (Settings.isDebug() ? " AND RUNNING IN DEBUG-MODE!" : ""));

    VertxOptions options = new VertxOptions();
    if (Settings.isDebug()) {
      options.setBlockedThreadCheckInterval(1000 * 60 * 60); // Disable errors about event-loop being blocked when stuck on breakpoints. This will clutter the console otherwise.
    }

    vertx = Vertx.vertx(options);

    Future<Void> future = Future.future();
    future.setHandler(res -> {
      if (res.failed()) {
        shutdownServer(); // If any of the vertices failed to deploy, shut down the application.
      } else {
        logger.debug("Done deploying main verticles.");
      }
    });

    // Start up verticles in turn. If one fails ignore the rest.
    deployDatabaseVerticle(future, (Future<Void> event1) -> {
      deployMainJelVerticle(future, (Future<Void> event2) -> {
        deployWebserverVerticle(future, null);
      });
    });
  }

  /**
   * Method that deploys the database verticle.
   *
   * @param future Future for reporting back success or failure of deployment
   * @param next Optional handler to call after a successfull deployment
   */
  private static void deployDatabaseVerticle(Future<Void> future, Handler<Future<Void>> next) {
    DeploymentOptions deployOptions = new DeploymentOptions();
    deployOptions.setInstances(1);
    deployOptions.setWorker(true);
    JsonObject config = new JsonObject();
    config.put("storagepath", Settings.get("storagepath"));
    deployOptions.setConfig(config);

    vertx.deployVerticle(new DatabaseServiceVerticle(), deployOptions, res -> {
      if (res.failed()) {
        logger.error("Failed to deploy DatabaseService-verticle.", res.cause());
        future.fail(res.cause());
      } else {
        deploymentIds.push(res.result()); // Keep record on which verticles we successfully have deployed.
        logger.info("Successfully deployed DatabaseService-verticle.");
        if (next != null) {
          next.handle(future);
        } else {
          future.complete();
        }
      }
    });
  }

  /**
   * Method that deploys the main JEL verticle.
   *
   * @param future Future for reporting back success or failure of deployment
   * @param next Optional handler to call after a successfull deployment
   */
  private static void deployMainJelVerticle(Future<Void> future, Handler<Future<Void>> next) {
    DeploymentOptions deployOptions = new DeploymentOptions();
    deployOptions.setInstances(1);
    deployOptions.setWorker(true);

    vertx.deployVerticle(new MainVerticle(), deployOptions, res -> {
      if (res.failed()) {
        logger.error("Failed to deploy MainJel-verticle.", res.cause());
        future.fail(res.cause());
      } else {
        deploymentIds.push(res.result()); // Keep record on which verticles we successfully have deployed.
        logger.info("Successfully deployed MainJel-verticle.");
        if (next != null) {
          next.handle(future);
        } else {
          future.complete();
        }
      }
    });
  }

  /**
   * Startup verticle handling HTTP-requests for static/dynamic HTML and REST-requests.
   *
   * @param future Future for reporting back success or failure of deployment
   * @param next Optional handler to call after a successfull deployment
   */
  private static void deployWebserverVerticle(Future<Void> future, Handler<Future<Void>> next) {
    DeploymentOptions deployOptions = new DeploymentOptions();
    JsonObject config = new JsonObject();
    config.put("port", Settings.get("port"));
    deployOptions.setConfig(config);

    vertx.deployVerticle(new WebserverVerticle(), deployOptions, res -> {
      if (res.failed()) {
        logger.error("Failed to deploy Webserver-verticle on port {}", config.getString("port"), res.cause());
        future.fail(res.cause());
      } else {
        deploymentIds.push(res.result()); // Keep record on which verticles we successfully have deployed.
        logger.info("Successfully deployed Webserver-verticle.");
        if (next != null) {
          next.handle(future);
        } else {
          future.complete();
        }
      }
    });
  }

  /**
   * Shutdown server, should only be called upon by shutdown-hook. Closes internal threads and release resources.
   */
  private static void shutdownServer() {
    logger.info("Shutting down JEL-server...");

    if (vertx != null) {
      vertx.close(res -> {
        vertx = null; // Make sure we don't get stuck in a endless loop.

        if (res.succeeded()) {
          logger.info("JEL has been successfully shutdown.");
          System.exit(0);
        } else {
          logger.error("An error reported during application shutdown.", res.cause());
          System.exit(1);
        }
      });
    }
  }
}
