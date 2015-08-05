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
package se.liquidbytes.jel.system;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.ProxyIgnore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.system.impl.JelServiceImpl;

/**
 *
 * @author Henrik Östman
 */
@ProxyGen // Generate the proxy and handler
public interface JelService {

  /**
   * Factory method to create an instance of the service.
   *
   * @param vertx Vertx instance
   * @return Service instance
   */
  static JelService create(Vertx vertx) {
    return new JelServiceImpl(vertx);
  }

  /**
   * Factory method to create an proxy to the service.
   *
   * @param vertx Vertx instance
   * @param address Eventbus address to listen on
   * @return Proxy instance
   */
  static JelService createProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(JelService.class, vertx, address);
  }

  /**
   * Method for starting up service.
   */
  @ProxyIgnore
  void start();

  /**
   * Method for stopping service, must be called upon during application shutdown.
   */
  @ProxyIgnore
  void stop();

  // Plugins
  void getInstalledPlugins(Handler<AsyncResult<JsonArray>> resultHandler);

  void getAvailablePluginsToInstall(Handler<AsyncResult<JsonArray>> resultHandler);

  void getAvailablePluginsToUpdate(Handler<AsyncResult<JsonArray>> resultHandler);

  void installPlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler);

  void updatePlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler);

  void uninstallPlugin(String name, Handler<AsyncResult<Void>> resultHandler);

  // Sites
  void createSite(JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler);

  void listSites(Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveSite(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateSite(String id, JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler);

  void deleteSite(String id, Handler<AsyncResult<Void>> resultHandler);

  // Devices
  void createUnboundDevice(JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler);

  void listUnboundDevices(Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveUnboundDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateUnboundDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler);

  void deleteUnboundDevice(String id, Handler<AsyncResult<Void>> resultHandler);
}
