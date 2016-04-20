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

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.SystemInfo;
import static se.liquidbytes.jel.system.JelService.API_ENDPOINT;
import se.liquidbytes.jel.system.PublicEvents;
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
   * REST-API mount point
   */
  public final static String REST_MOUNTPOINT = "/api";
  /**
   * Eventbus mount point, used for pushing information/events to connected clients.
   */
  public final static String EVENTBUS_MOUNTPOINT = "/eventbus/*";
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

    // Log all request.
    router.route().handler(LoggerHandler.create(LoggerFormat.SHORT));

    // Serve favicons, https://en.wikipedia.org/wiki/Favicon
    if (Settings.get("skipweb").equals("false")) {
      router.route().handler(FaviconHandler.create());
    }

    // Session / cookies for users
    router.route().handler(CookieHandler.create());
    SessionStore sessionStore = LocalSessionStore.create(vertx);
    SessionHandler sessionHandler = SessionHandler.create(sessionStore).setSessionCookieName("jel.session").setCookieHttpOnlyFlag(true);
    router.route().handler(sessionHandler);

    // API
    router.mountSubRouter(REST_MOUNTPOINT, apiRouter());

    //  EventBus ( SockJS)
    router.route(EVENTBUS_MOUNTPOINT).handler(eventBusHandler());

    router.route().failureHandler(ErrorHandler.create(Settings.isDebug())); // Show error details only when running in debug/development-mode

    if (Settings.get("skipweb").equals("false")) {
      // Static resources, must be placed after API-router to not catch API calls also.
      staticHandler(router);
    }

    // No matcher - MUST be last in router chain
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
    StaticHandler staticHandler = StaticHandler.create().setWebRoot("webroot");
    staticHandler.setCachingEnabled(!Settings.isDebug()).setMaxAgeSeconds(3600 * 24 * 80); // 80 days caching, disabled when running in debug/development-mode.

    router.route("/*").handler(staticHandler);

    return router;
  }

  /**
   * Set up router for handling REST API-requests.
   *
   * @return Router with added handler.
   */
  private Router apiRouter() {

    //TODO: Add CORS support using a --cors argument to JEL. https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/cors/Server.java
    Router router = Router.router(vertx);

    // Handle bodies, limit body sizes, and handle file uploads. Max 5 MiB upload limit
    router.route().handler(BodyHandler.create().setBodyLimit(1024 * 5000));

    router.route().handler(con -> {

      con.response().setChunked(true);
      setNoCacheHeaders(con);

      String contenType = con.request().headers().get("Content-Type");
      if (contenType == null || contenType.isEmpty()) {
        // Set default if no content-type specified.
        contenType = RepresentationFactory.HAL_JSON;
      }

      switch (contenType.trim().toLowerCase()) {
        case "application/json":
        case RepresentationFactory.HAL_JSON:
          contenType = RepresentationFactory.HAL_JSON;
          break;
        case "application/xml":
        case RepresentationFactory.HAL_XML:
          contenType = RepresentationFactory.HAL_XML;
          break;
        default:
          // End request if we could not handle requested type.
          con.response().setStatusCode(406).end("Unknown Content-Type.");
          return;
      }

      con.response().putHeader("content-type", contenType + "; charset=utf-8");
      con.put("__content-type", contenType);
      con.next();
    });

    // Root API help
    router.get("/").handler(con -> {
      Representation rep = PresentationFactory.getRepresentation(API_ENDPOINT)
          .withLink("system", API_ENDPOINT + "/system")
          .withLink("plugins", API_ENDPOINT + "/plugins")
          .withLink("repoplugins", API_ENDPOINT + "/repoplugins")
          .withLink("adaptertypes", API_ENDPOINT + "/adaptertypes")
          .withLink("adapters", API_ENDPOINT + "/adapters")
          .withLink("users", API_ENDPOINT + "/users")
          .withLink("sites", API_ENDPOINT + "/sites");

      con.response().end(rep.toString(con.get("__content-type")));
    });
    // System-api
    router.get("/system/info").handler(systemApi::systemInformation);
    router.get("/system/resources").handler(systemApi::systemResources);

    router.get("/system").handler(con -> {
      Representation rep = PresentationFactory.getRepresentation(API_ENDPOINT + "/system")
          .withLink("info", API_ENDPOINT + "/system/info")
          .withLink("resources", API_ENDPOINT + "/system/resources");

      con.response().end(rep.toString(con.get("__content-type")));
    });

    // Plugin-api
    router.post("/plugins").handler(pluginApi::install);
    router.get("/plugins").handler(pluginApi::listInstalled);
    router.put("/plugins/:name").handler(pluginApi::update);
    router.delete("/plugins/:name").handler(pluginApi::uninstall);
    router.get("/repoplugins/:filter").handler(pluginApi::listRepoPlugins);
    // Devices & Adapters
    router.get("/adapters/devices").handler(deviceApi::listAllDevices);
    router.get("/adapters/:adapterId/supportedDevices").handler(deviceApi::retrieveSupportedAdapterDevices);
    router.get("/adapters/:adapterId/devices").handler(deviceApi::listAdapterDevices);
    router.post("/adapters/:adapterId/devices").handler(deviceApi::createAdapterDevice);
    router.get("/adapters/:adapterId/devices/:deviceId").handler(deviceApi::retrieveAdapterDevice);
    router.put("/adapters/:adapterId/devices/:deviceId").handler(deviceApi::updateAdapterDevice);
    router.delete("/adapters/:adapterId/devices/:deviceId").handler(deviceApi::deleteAdapterDevice);
    router.get("/adapters/:adapterId/devices/:deviceId/value").handler(deviceApi::getDeviceValue);
    router.put("/adapters/:adapterId/devices/:deviceId/value").handler(deviceApi::setDeviceValue);
    router.get("/adaptertypes").handler(adapterApi::listAdaptertypes);
    router.post("/adapters").handler(adapterApi::add);
    router.get("/adapters").handler(adapterApi::list);
    router.get("/adapters/:adapterId").handler(adapterApi::retrieve);
    router.delete("/adapters/:adapterId").handler(adapterApi::remove);
    // User-api
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
    router.post("/sites").handler(siteApi::create);
    router.get("/sites").handler(siteApi::list);
    router.get("/sites/:siteId").handler(siteApi::retrieve);
    router.put("/sites/:siteId").handler(siteApi::update);
    router.delete("/sites/:siteId").handler(siteApi::delete);
    // Device-api
    router.post("/sites/:siteId/devices").handler(deviceApi::addToSite);
    router.get("/sites/:siteId/devices").handler(deviceApi::listOnSite);
    router.get("/sites/:siteId/devices/:deviceId").handler(deviceApi::retrieveOnSite);
    router.put("/sites/:siteId/devices/:deviceId").handler(deviceApi::updateOnSite);
    router.delete("/sites/:siteId/devices/:deviceId").handler(deviceApi::deleteFromSite);

    return router;
  }

  /**
   * Set up Eventbus and specify which namespace are allowed for inbound and outbound communication.
   *
   * @return SockJSHandler instance.
   */
  private SockJSHandler eventBusHandler() {
    BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress(PublicEvents.EVENTBUS_PUBLIC));
    //BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddress(PublicEvents.EVENTBUS_PUBLIC)); //TODO: figur out appropriate permission.
    return SockJSHandler.create(vertx).bridge(options, event -> {
      switch (event.type()) {
        case SOCKET_CREATED:
          logger.debug("Socket created for remote client: " + event.socket().remoteAddress().toString());
          break;
        case SOCKET_CLOSED:
          logger.debug("Socket closed for remote client: " + event.socket().remoteAddress().toString());
          break;
        case PUBLISH:
          logger.debug("Eventbus message received from client from remote client: " + event.socket().remoteAddress().toString());
          break;
      }

      event.complete(true);
    });
  }

  /**
   * Set no cache headers on response
   *
   * @param con context.
   */
  private void setNoCacheHeaders(RoutingContext con) {
    if (!con.response().headWritten()) {
      // Make sure we don't cache stuff we don't want to, this can be overridden later for responses that should be cached.
      con.response()
          .putHeader("Cache-Control", "private, no-cache, no-store, must-revalidate")
          .putHeader("Pragma", "no-cache")
          .putHeader("Expires", LocalDateTime.parse("2010-01-10T13:37:00").toString());
    }
  }
}
