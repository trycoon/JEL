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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;
import se.liquidbytes.jel.SystemInfo;
import se.liquidbytes.jel.system.JelService;
import se.liquidbytes.jel.system.JelServiceProxy;
import se.liquidbytes.jel.system.adapter.AdapterConfiguration;
import se.liquidbytes.jel.system.adapter.DeployedAdapter;
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
    // Wait a while to let stuff get into place before talking to the adapters.
    JelService.vertx().setTimer(500, h -> {
      JelService.deviceManager().start();
    });
  }

  /**
   * Method for stopping service, must be called upon during application shutdown.
   */
  @Override
  public void stop() {
    if (JelService.deviceManager() != null) {
      JelService.deviceManager().stop();
    }

    JelService.vertx().setTimer(500, h -> {
      if (JelService.adapterManager() != null) {
        Future<Void> future = Future.future();
        future.setHandler(res -> {
          if (JelService.pluginManager() != null) {
            JelService.pluginManager().stop();
          }
        });

        JelService.adapterManager().stop(future);
      }
    });
  }

  // System
  @Override
  public void systemInformation(Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      resultHandler.handle(Future.succeededFuture(SystemInfo.getSystemInformation()));
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void systemResources(Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      resultHandler.handle(Future.succeededFuture(SystemInfo.getSystemResources()));
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  // Plugins
  @Override
  public void listInstalledPlugins(Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      List<PluginDesc> plugins = JelService.pluginManager().getInstalledPlugins();

      JsonArray list = new JsonArray();
      plugins.stream().forEach((plugin) -> {
        list.add(plugin.toApi());
      });

      resultHandler.handle(Future.succeededFuture(list));
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listAvailablePluginsToInstall(Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void listAvailablePluginsToUpdate(Handler<AsyncResult<JsonArray>> resultHandler) {
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

  // Adapters
  @Override
  public void listAvailableAdapterTypes(Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      List<PluginDesc> adapterTypes = JelService.adapterManager().getAvailableAdapterTypes();

      JsonArray list = new JsonArray();
      adapterTypes.stream().forEach((adapterType) -> {
        list.add(adapterType.toApi());
      });

      resultHandler.handle(Future.succeededFuture(list));
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listAdapters(Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      List<DeployedAdapter> adapters = JelService.adapterManager().getAdapters();

      JsonArray list = new JsonArray();
      adapters.stream().forEach((adapter) -> {
        list.add(adapter.toApi());
      });

      resultHandler.handle(Future.succeededFuture(list));
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void retrieveAdapter(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      DeployedAdapter adapter = JelService.adapterManager().getAdapter(id);
      if (adapter != null) {
        resultHandler.handle(Future.succeededFuture(adapter.toApi()));
      } else {
        resultHandler.handle(Future.succeededFuture(null));
      }
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void addAdapter(JsonObject adapter, Handler<AsyncResult<Void>> resultHandler) {
    try {
      AdapterConfiguration config = new AdapterConfiguration();
      config.setType(adapter.getString("type"));
      config.setAddress(adapter.getString("address"));
      config.setPort(adapter.getInteger("port"));

      JelService.adapterManager().addAdapter(config);
      resultHandler.handle(Future.succeededFuture());
    } catch (IllegalArgumentException | JelException ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void removeAdapter(String id, Handler<AsyncResult<Void>> resultHandler) {
    try {
      JelService.adapterManager().removeAdapter(id);
      resultHandler.handle(Future.succeededFuture());
    } catch (IllegalArgumentException | JelException ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
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
  public void createAdapterDevice(String adapterId, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      JelService.deviceManager().createAdapterDevice(adapterId, device, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listAdapterDevices(String adapterId, Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      JelService.deviceManager().listAdapterDevices(adapterId, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void retrieveAdapterDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      JelService.deviceManager().retrieveAdapterDevice(id, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void updateAdapterDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      JelService.deviceManager().updateAdapterDevice(id, device, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void deleteAdapterDevice(String id, Handler<AsyncResult<Void>> resultHandler) {
    try {
      JelService.deviceManager().deleteAdapterDevice(id, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listSupportedAdapterDevices(String id, Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      JelService.deviceManager().listSupportedAdapterDevices(id, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listAllDevices(Handler<AsyncResult<JsonArray>> resultHandler) {
    try {
      JelService.deviceManager().listAllDevices((onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void listSiteDevices(String siteId, Handler<AsyncResult<JsonArray>> resultHandler) {
    /*try {      //TODO: Hmm this should be done in devicemanager and here we sohuld only work with JSON.
     List<? extends Device> devices = JelService.deviceManager().listSiteDevices(siteId);

     JsonArray list = new JsonArray();
     devices.stream().forEach((device) -> {
     try {
     list.add(device.toApi());
     } catch (JsonProcessingException ex) {
     logger.warn(String.format("Fail to serialize device(id=%s, name=%s) to JSON.", device.getId(), device.getName()), ex);
     }
     });

     resultHandler.handle(Future.succeededFuture(list));
     } catch (JelException ex) {
     resultHandler.handle(Future.failedFuture(ex.getMessage()));
     }*/
  }

  @Override
  public void retrieveDeviceValue(String adapterId, String deviceId, Handler<AsyncResult<JsonObject>> resultHandler) {
    try {
      JelService.deviceManager().retrieveDeviceValue(adapterId, deviceId, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }

  @Override
  public void updateDeviceValue(String adapterId, String deviceId, JsonObject value, Handler<AsyncResult<Void>> resultHandler) {
    try {
      JelService.deviceManager().updateDeviceValue(adapterId, deviceId, value, (onResult) -> {
        if (onResult.succeeded()) {
          resultHandler.handle(Future.succeededFuture(onResult.result()));
        } else {
          resultHandler.handle(Future.failedFuture(onResult.cause().getMessage()));
        }
      });
    } catch (Exception ex) {
      resultHandler.handle(Future.failedFuture(ex.getMessage()));
    }
  }
}
