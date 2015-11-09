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
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.system.adapter.AdapterManager;
import se.liquidbytes.jel.system.device.DeviceManager;
import se.liquidbytes.jel.system.impl.JelServiceImpl;
import se.liquidbytes.jel.system.plugin.PluginManager;

/**
 *
 * @author Henrik Östman
 */
@ProxyGen // Generate the proxy and handler
public interface JelServiceProxy {

  /**
   * Factory method to create an instance of the service.
   *
   * @param vertx Vertx instance
   * @return Service instance
   */
  static JelServiceProxy create(Vertx vertx) {
    JelService.vertx(vertx);
    JelService.adapterManager(new AdapterManager());
    JelService.pluginManager(new PluginManager(Settings.getStoragePath().toString()));
    JelService.deviceManager(new DeviceManager());
    return new JelServiceImpl();
  }

  /**
   * Factory method to create an proxy to the service.
   *
   * @param vertx Vertx instance
   * @param address Eventbus address to listen on
   * @return Proxy instance
   */
  static JelServiceProxy createProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(JelServiceProxy.class, vertx, address);
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

  // System
  void systemInformation(Handler<AsyncResult<JsonObject>> resultHandler);

  void systemResources(Handler<AsyncResult<JsonObject>> resultHandler);

  // Plugins
  void listInstalledPlugins(Handler<AsyncResult<JsonArray>> resultHandler);

  void listAvailablePluginsToInstall(Handler<AsyncResult<JsonArray>> resultHandler);

  void listAvailablePluginsToUpdate(Handler<AsyncResult<JsonArray>> resultHandler);

  void installPlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler);

  void updatePlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler);

  void uninstallPlugin(String name, Handler<AsyncResult<Void>> resultHandler);

  // Adapters
  void listAvailableAdapterTypes(Handler<AsyncResult<JsonArray>> resultHandler);

  void listAdapters(Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveAdapter(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  void addAdapter(JsonObject adapter, Handler<AsyncResult<Void>> resultHandler);

  void removeAdapter(String id, Handler<AsyncResult<Void>> resultHandler);

  // Sites
  void createSite(JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler);

  void listSites(Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveSite(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateSite(String id, JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler);

  void deleteSite(String id, Handler<AsyncResult<Void>> resultHandler);

  // Devices
  void createAdapterDevice(String adapterId, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler);

  void listAdapterDevices(String adapterId, Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveAdapterDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateAdapterDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler);

  void deleteAdapterDevice(String id, Handler<AsyncResult<Void>> resultHandler);

  void listSupportedAdapterDevices(String adapterId, Handler<AsyncResult<JsonArray>> resultHandler);

  void listAllDevices(Handler<AsyncResult<JsonArray>> resultHandler);

  void listSiteDevices(String siteId, Handler<AsyncResult<JsonArray>> resultHandler);

  void retrieveDeviceValue(String adapterId, String deviceId, Handler<AsyncResult<JsonObject>> resultHandler);

  void updateDeviceValue(String adapterId, String deviceId, JsonObject value, Handler<AsyncResult<Void>> resultHandler);
  /*
   router.post ("/sites/:siteId/devices").handler(deviceApi::addToSite);
   router.get ("/sites/:siteId/devices").handler(deviceApi::listOnSite);
   router.get ("/sites/:siteId/devices/:deviceId").handler(deviceApi::retrieveOnSite);
   router.put ("/sites/:siteId/devices/:deviceId").handler(deviceApi::updateOnSite);
   router.delete ("/sites/:siteId/devices/:deviceId").handler(deviceApi::deleteFromSite);

   // User-api
   router.post ("/users").handler(userApi::create);
   router.get ("/users").handler(userApi::list);
   router.get ("/users/:userId").handler(userApi::retrieve);
   router.put ("/users/:userId").handler(userApi::update);
   router.delete ("/users/:userId").handler(userApi::delete);
   // Session-api (login/logout)
   router.post ("/users/sessions").handler(userApi::createSession);
   router.get ("/users/sessions").handler(userApi::listSessions);
   router.get ("/users/:userId/sessions").handler(userApi::retrieveSession);
   router.delete ("/users/:userId/sessions").handler(userApi::deleteSession);*/
}
