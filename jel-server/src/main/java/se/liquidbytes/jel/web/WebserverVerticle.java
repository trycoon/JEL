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
package se.liquidbytes.jel.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.ErrorHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.SystemInfo;
import se.liquidbytes.jel.web.api.AdapterApi;
import se.liquidbytes.jel.web.api.DeviceApi;
import se.liquidbytes.jel.web.api.PluginApi;
import se.liquidbytes.jel.web.api.SiteApi;
import se.liquidbytes.jel.web.api.SystemApi;
import se.liquidbytes.jel.web.api.UserApi;

/**
 * Class responsible of handling responses of HTTP-requests
 */
public class WebserverVerticle extends AbstractVerticle {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * HTTP-server instance
   */
  private HttpServer server;
  /**
   * Config settings received during deployment
   */
  private JsonObject config;
  /**
   * System API handler instance
   */
  private SystemApi systemApi;
  /**
   * Plugin API handler instance
   */
  private PluginApi pluginApi;
  /**
   * Adapter API handler instance
   */
  private AdapterApi adapterApi;
  /**
   * Site API handler instance
   */
  private SiteApi siteApi;
  /**
   * User API handler instance
   */
  private UserApi userApi;
  /**
   * Device API handler instance
   */
  private DeviceApi deviceApi;

  /**
   * Method should be called during deployment of verticle
   *
   * @param vertx Vertx-instance
   * @param context Current context
   */
  @Override
  public void init(Vertx vertx, Context context) {
    super.init(vertx, context);
    config = context.config();
  }

  /**
   * Method should be called when verticle should start up
   *
   * @param future Future for reporting back success or failure of execution
   */
  @Override
  public void start(Future<Void> future) {

    // If no API should be exposed, and thus the client won't work either, then don't start the webserver.
    if (Settings.get("skipapi").equals("true")) {
      logger.info("No API will be exposed according to the settings provided during application startup.");
      future.complete();
      return;
    }

    systemApi = new SystemApi(vertx);
    pluginApi = new PluginApi(vertx);
    adapterApi = new AdapterApi(vertx);
    siteApi = new SiteApi(vertx);
    userApi = new UserApi(vertx);
    deviceApi = new DeviceApi(vertx);

    HttpServerOptions options = new HttpServerOptions();
    options.setHost(SystemInfo.getIP());
    options.setPort(Integer.parseInt(config.getString("port")));

    server = vertx.createHttpServer(options);
    server.requestHandler(createRouter()::accept);
    server.listen(result -> {
      if (result.succeeded()) {
        logger.info(String.format("Jel REST-API now listening on %s port %d.", options.getHost(), options.getPort()));
        future.complete();
      } else {
        future.fail(result.cause());
      }
    });
  }

  /**
   * Method should be called when verticle should stop/undeploy
   *
   * @param future Future for reporting back success or failure of execution
   */
  @Override
  public void stop(Future<Void> future) {
    if (server == null) {
      future.complete();
      return;
    }
    server.close(result -> {
      if (result.failed()) {
        future.fail(result.cause());
      } else {
        future.complete();
      }
    });
  }

  /**
   * Method creates a Router-instance listening on the routes we setup here.
   *
   * @return A router-instance
   */
  private Router createRouter() {
    Router router = Router.router(vertx);
    router.route().failureHandler(ErrorHandler.create(Settings.isDebug())); // Show error details only when running in debug/development-mode

    // Log all request.
    router.route().handler(LoggerHandler.create(LoggerFormat.SHORT));

    if (Settings.get("skipweb").equals("false")) {
      // Serve favicons, https://en.wikipedia.org/wiki/Favicon
      router.route().handler(FaviconHandler.create());
      // Static resources
      staticHandler(router);
    }

    // Session / cookies for users
    router.route().handler(CookieHandler.create());
    SessionStore sessionStore = LocalSessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(sessionStore).setSessionCookieName("jel.session"); //TODO: Add HttpOnly to SessionHandler
    router.route().handler(sessionHandler);

    // Dynamic pages
    /*if (Settings.get("skipweb").equals("false")) {      dynamicPages(router);
    }*/

    // API
    router.mountSubRouter("/api", apiRouter());

    // SockJS / EventBus
    //router.route("/eventbus/*").handler(eventBusHandler());

    //No matcher - MUST be last in routers chain
    router.route().handler(con -> {
      con.fail(404);
    });

    return router;
  }

  /**
   * Set up router for serving static resources (Javascript, CSS, images...)
   *
   * @param router Router to add handler to.
   * @return Router with added handler.
   */
  private Router staticHandler(Router router) {
    StaticHandler staticHandler = StaticHandler.create();
    staticHandler.setCachingEnabled(!Settings.isDebug()); // Disable caching when running in debug/development-mode
    router.route("/assets/*").handler(staticHandler);
    return router;
  }

  /**
   * Set up router for handling rendering of webpages.
   *
   * @param router Router to add handler to.
   * @return Router with added handler.
   */
  /*private Router dynamicPages(Router router) {
   HandlebarsTemplateEngine hbsEngine = HandlebarsTemplateEngine.create();
   hbsEngine.setMaxCacheSize(0); /* no cache since we wan't hot-reload for templates */ //TODO: checkup this setting!

  /* TemplateHandler templateHandler = TemplateHandler.create(hbsEngine);    //router.get("/private/*").handler(userContextHandler::fromSession);
    router.getWithRegex(".+\\.hbs").handler(con -> {
      final Session session = con.session();
      con.data().put("userLogin", session.get("login"));
      con.data().put("accessToken", session.get("accessToken"));

      con.next();
    });
    router.getWithRegex(".+\\.hbs").handler(templateHandler);
    return router;
   }*/

  /**
   * Set up router for handling REST API-requests.
   *
   * @return Router with added handler.
   */
  private Router apiRouter() {
    /*
     * TODO : provide authentication through the AuthService / AuthProvider instead of a custom api handler
     * TODO : every page except login must be private TODO : use FormLoginHandler for the actual login form TODO : use RedirectAuthHandler for "/private"
     */

    //TODO: Add CORS support using a --cors argument to JEL. https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/cors/Server.java
    Router router = Router.router(vertx);
    //TODO: Add support for XML/PLIST and more?
    router.route().consumes("application/json");
    router.route().produces("application/json");

    router.route().consumes("application/xml");
    router.route().produces("application/xml");

    router.route().handler(BodyHandler.create().setBodyLimit(1024 * 5000)); // Max 5 MiB upload limit
    router.route().handler((RoutingContext con) -> {
      String reqContentType = con.request().getHeader(CONTENT_TYPE.toString());

      // If contenttype has not been specified as a header, look for it in the query-string
      if (reqContentType == null) {
        if (con.request().query() != null) {
          // Arrays.asList(con.request().query().split("&")).; //TODO: Find querystring library in vert.x.
        }
      }

      // If no contenttype has been specified, fallback to JSON.
      if (reqContentType == null) {
        reqContentType = "application/json";
      }

      switch (reqContentType) {
        case "application/xml": {
          con.response().headers().add(CONTENT_TYPE, "application/xml");
          break;
        }
        default: {
          con.response().headers().add(CONTENT_TYPE, "application/json");
        }
      }

      con.next();
    });

    // System-api
    router.get("/system/info").handler(systemApi::systemInformation);
    router.get("/system/resources").handler(systemApi::systemResources);
    // Plugin-api
    router.post("/plugins").handler(pluginApi::install);
    router.get("/plugins").handler(pluginApi::listInstalled);
    router.put("/plugins/:name").handler(pluginApi::update);
    router.delete("/plugins/:name").handler(pluginApi::uninstall);
    router.get("/repoplugins/:filter").handler(pluginApi::listRepoPlugins);
    // Adapter-api
    router.get("/adaptertypes").handler(adapterApi::listAdaptertypes);
    router.post("/adapters").handler(adapterApi::add);
    router.get("/adapters").handler(adapterApi::list);
    router.get("/adapters/:adapterId").handler(adapterApi::retrieve);
    router.delete("/adapters/:adapterId").handler(adapterApi::remove);
    router.get("/adapters/:adapterId/supportedDevices").handler(deviceApi::listSupportedDevices);
    // User-api
    //router.route("/users*").handler(userContextHandler::fromApiToken);
    router.post("/users").handler(userApi::create);
    router.get("/users").handler(userApi::list);
    router.get("/users/:userId").handler(userApi::retrieve);
    router.put("/users/:userId").handler(userApi::update);
    router.delete("/users/:userId").handler(userApi::delete);
    // Session-api (login/logout)
    router.post("/users/sessions").handler(userApi::createSession);
    router.get("/users/sessions").handler(userApi::listSessions);
    router.get("/users/:userId/sessions").handler(userApi::retrieveSession);
    router.delete("/users/:userId/sessions").handler(userApi::deleteSession);
    // Site-api
    //router.route("/sites*").handler(userContextHandler::fromApiToken);
    router.post("/sites").handler(siteApi::create);
    router.get("/sites").handler(siteApi::list);
    router.get("/sites/:siteId").handler(siteApi::retrieve);
    router.put("/sites/:siteId").handler(siteApi::update);
    router.delete("/sites/:siteId").handler(siteApi::delete);
    // Device-api
    router.post("/sites/:siteId/devices").handler(deviceApi::create);
    router.get("/sites/:siteId/devices").handler(deviceApi::list);
    router.get("/sites/:siteId/devices/:deviceId").handler(deviceApi::retrieve);
    router.put("/sites/:siteId/devices/:deviceId").handler(deviceApi::update);
    router.delete("/sites/:siteId/devices/:deviceId").handler(deviceApi::delete);
    // Unbound devices-api (devices that not yet been bound to a site)
    //router.route("/unbounddevices*").handler(userContextHandler::fromApiToken);
    router.post("/unbounddevices").handler(deviceApi::createUnboundDevice);
    router.get("/unbounddevices").handler(deviceApi::listUnboundDevices);
    router.get("/unbounddevices/:deviceId").handler(deviceApi::retrieveUnboundDevice);
    router.put("/unbounddevices/:deviceId").handler(deviceApi::updateUnboundDevice);
    router.delete("/unbounddevices/:deviceId").handler(deviceApi::deleteUnboundDevice);

    return router;
  }

  /**
   * Set up Eventbus and specify which namespaces are allowed for inbound and outbound communication.
   *
   * @return A SocketJSHandler-instance
   */
  private SockJSHandler eventBusHandler() {
    SockJSHandler handler = SockJSHandler.create(vertx);

    // TODO: Add new eventbus code for vertx-3.1

    return handler;
  }
}
