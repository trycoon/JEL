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
package se.liquidbytes.jel.system.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.JelService;
import se.liquidbytes.jel.system.JelServiceProxy;
import static se.liquidbytes.jel.system.adapter.AdapterManager.EVENTBUS_ADAPTERS;
import se.liquidbytes.jel.system.adapter.AdapterSettings;
import se.liquidbytes.jel.system.plugin.PluginDesc;

/**
 *
 * @author Henrik Östman
 */
public class JelServiceImpl implements JelServiceProxy {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Method for starting up service.
   */
  @Override
  public void start() {
    JelService.adapterManager().start();
    JelService.pluginManager().start();
  }

  /**
   * Method for stopping service, must be called upon during application shutdown.
   */
  @Override
  public void stop() {
    if (JelService.adapterManager() != null) {
      Future<Void> future = Future.future();
      future.setHandler(res -> {
        if (JelService.pluginManager() != null) {
          JelService.pluginManager().stop();
        }
      });

      JelService.adapterManager().stop(future);
    }
  }

  // Plugins
  @Override
  public void getInstalledPlugins(Handler<AsyncResult<JsonArray>> resultHandler) {

    List<PluginDesc> plugins = JelService.pluginManager().getInstalledPlugins();

    JsonArray list = new JsonArray();
    plugins.stream().forEach((plugin) -> {
      list.add(new JsonObject()
          .put("name", plugin.getName())
          .put("author", plugin.getAuthor())
          .put("category", plugin.getCategory().toString())
          .put("description", plugin.getDescription())
          .put("checksum", plugin.getFileChecksum())
          .put("homepage", plugin.getHomepage())
          .put("license", plugin.getLicence())
          .put("version", plugin.getVersion())
      );
    });

    resultHandler.handle(Future.succeededFuture(list));
  }

  @Override
  public void getAvailablePluginsToInstall(Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getAvailablePluginsToUpdate(Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void installPlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void updatePlugins(JsonObject plugins, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void uninstallPlugin(String name, Handler<AsyncResult<Void>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  // Sites
  @Override
  public void createSite(JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void listSites(Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void retrieveSite(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void updateSite(String id, JsonObject site, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteSite(String id, Handler<AsyncResult<Void>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  // Devices
  @Override
  public void listUnboundDevices(Handler<AsyncResult<JsonArray>> resultHandler) {
    // TODO:
    // 1. For every adapter in list of adapters, send a "listDevices"-message.
    // 2. Collect response from all adapters, and add all devices to one hashmap.
    // 3. Get all devices from all sites.
    // 4. Filter out so only devices in hashmap that don't exists in list of devices from sites are left.
    // 5. Order list after adapter and devicename/id? and return list.

    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "listDevices");
    List<AdapterSettings> adapters = JelService.adapterManager().getAdapters();
    //TODO: If list is empty, call resultHandler.handle(Future.succeededFuture
    adapters.stream().forEach((_item) -> {
      JelService.vertx().eventBus().send(EVENTBUS_ADAPTERS, null, options, res -> {
        if (res.succeeded()) {
          //TODO: We must wait for alla responses before we call this!!!
          resultHandler.handle(Future.succeededFuture((JsonArray) res.result().body()));
        } else {
          resultHandler.handle(Future.failedFuture(res.cause()));
        }
      });
    });
  }

  @Override
  public void createUnboundDevice(JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void retrieveUnboundDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void updateUnboundDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteUnboundDevice(String id, Handler<AsyncResult<Void>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
