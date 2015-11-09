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
package se.liquidbytes.jel.system.device;

import com.cyngn.vertx.async.promise.Promise;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.JelService;
import static se.liquidbytes.jel.system.JelService.EVENTBUS;
import static se.liquidbytes.jel.system.adapter.AdapterManager.EVENTBUS_ADAPTERS;
import se.liquidbytes.jel.system.adapter.DeployedAdapter;

/**
 * Class that manages all devices (sensors/actuators).
 *
 * @author Henrik Östman
 */
public final class DeviceManager {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Namespace for communicating adapter events over the eventbus.
   */
  public final static String EVENTBUS_DEVICES = EVENTBUS + ".devices";

  /**
   * Events
   */
  public final static String EVENTBUS_DEVICES_ADDED = "DEVICE_ADDED";
  public final static String EVENTBUS_DEVICES_REMOVED = "DEVICE_REMOVED";
  public final static String EVENTBUS_DEVICE_NEWREADING = "DEVICE_NEWREADING";

  /**
   * Collection of all devices for a specific site.
   */
  private final Map<String, List<? extends Device>> siteDevices;

  /**
   * Default constructor.
   */
  public DeviceManager() {
    siteDevices = new ConcurrentHashMap<>();
  }

  /**
   * Method for starting device manager, should be called upon application startup.
   */
  public void start() {
    //TODO: hardcoded!
    List<Device> devices = new ArrayList<>();
    Sensor sensor = new Sensor();
    sensor.setId("123");
    sensor.setHardware(new DeviceHardware());
    sensor.setCurrentValue(new DeviceValue());
    sensor.setPreviousValue(new DeviceValue());
    sensor.setMaxValue(new DeviceValue());
    sensor.setMinValue(new DeviceValue());
    sensor.setLargePresentation(new DevicePresentation());
    sensor.setMediumPresentation(new DevicePresentation());
    sensor.setSmallPresentation(new DevicePresentation());
    devices.add(sensor);

    sensor = new Sensor();
    sensor.setId("456");
    sensor.setHardware(new DeviceHardware());
    sensor.setCurrentValue(new DeviceValue());
    sensor.setPreviousValue(new DeviceValue());
    sensor.setMaxValue(new DeviceValue());
    sensor.setMinValue(new DeviceValue());
    sensor.setLargePresentation(new DevicePresentation());
    sensor.setMediumPresentation(new DevicePresentation());
    sensor.setSmallPresentation(new DevicePresentation());
    devices.add(sensor);

    siteDevices.put("1", devices);

  }

  /**
   * Method for stopping device manager, should be called upon application shutdown.
   */
  public void stop() {
    logger.info("Shutting down devicemanager...");

    siteDevices.clear();
  }

  public void createAdapterDevice(String adapterId, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public void listAdapterDevices(String adapterId, Handler<AsyncResult<JsonArray>> resultHandler) {
    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "listDevices");
    DeployedAdapter adapter = JelService.adapterManager().getAdapter(adapterId);

    if (adapter == null) {
      resultHandler.handle(Future.failedFuture(
          String.format("Adapter with id %s does not exist.", adapterId))
      );
    } else {
      // Send message to adapter to report back its devices.
      JelService.vertx().eventBus().send(
          String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
          null, options, res -> {
            if (res.succeeded()) {
              JsonArray devices = new JsonArray();

              JsonObject result = (JsonObject) res.result().body();
              // TODO: we should maybe keep track which adapter returned which device, so in the future we could send a message to a specific device (and then we need to know which adapter that are the owner).
              devices.addAll(result.getJsonArray("result"));

              resultHandler.handle(Future.succeededFuture(devices));
            } else {
              resultHandler.handle(Future.failedFuture(res.cause()));
            }
          });
    }
  }

  public void retrieveAdapterDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public void updateAdapterDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public void deleteAdapterDevice(String id, Handler<AsyncResult<Void>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Returns a list of all supported devices for a specific adapter.
   *
   * @param adapterId id for adapter to query.
   * @param resultHandler Promise will give the list of supported devices.
   */
  public void listSupportedAdapterDevices(String adapterId, Handler<AsyncResult<JsonArray>> resultHandler) {
    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "listSupportedDevices");
    DeployedAdapter adapter = JelService.adapterManager().getAdapter(adapterId);

    if (adapter == null) {
      resultHandler.handle(Future.failedFuture(
          String.format("Adapter with id %s does not exist.", adapterId))
      );
    } else {
      JelService.vertx().eventBus().send(
          String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
          null, options, res -> {
            if (res.succeeded()) {
              JsonObject result = (JsonObject) res.result().body();
              resultHandler.handle(Future.succeededFuture(result.getJsonArray("result")));
            } else {
              resultHandler.handle(Future.failedFuture(res.cause()));
            }
          });
    }
  }

  /**
   * Returns a list of all available devices for all adapters.
   *
   * @param resultHandler Promise will give the list of devices or a error if one has occured.
   */
  public void listAllDevices(Handler<AsyncResult<JsonArray>> resultHandler) {
    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "listDevices");
    List<DeployedAdapter> adapters = JelService.adapterManager().getAdapters();

    if (adapters.isEmpty()) {
      resultHandler.handle(Future.succeededFuture(new JsonArray()));
    } else {
      Promise promise = JelService.promiseFactory().create();
      adapters.stream().forEach((_item) -> {
        // Send message to all adapters to report back their devices.
        promise.then((context, onResult) -> {
          JelService.vertx().eventBus().send(
              String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, _item.config().getType(), _item.config().getAddress(), _item.config().getPort()),
              null, options, res -> {
                if (res.succeeded()) {
                  // All adapters fills in turn a json-array named "devices".
                  JsonArray devices = context.getJsonArray("devices");
                  // If we are the first adapter to report back, create the array.
                  if (devices == null) {
                    devices = new JsonArray();
                  }

                  JsonObject result = (JsonObject) res.result().body();
                  // TODO: we should maybe keep track which adapter returned which device, so in the future we could send a message to a specific device (and then we need to know which adapter that are the owner).
                  devices.addAll(result.getJsonArray("result"));

                  context.put("devices", devices);
                  onResult.accept(true);
                } else {
                  context.put("errorMessage", res.cause().toString());
                  onResult.accept(false);
                }
              });
        });
      });

      promise.done((onSuccess) -> {
        // When we are done, all adapters devices should be here.
        JsonArray devices = onSuccess.getJsonArray("devices");

        resultHandler.handle(Future.succeededFuture(devices));
      }).except((onError) -> {
        resultHandler.handle(Future.failedFuture(onError.getString("errorMessage")));
      }).eval();
    }
  }

  public void listSiteDevices(String siteId, Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public void retrieveDeviceValue(String adapterId, String deviceId, Handler<AsyncResult<JsonObject>> resultHandler) {
    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "retrieveDeviceValue");
    DeployedAdapter adapter = JelService.adapterManager().getAdapter(adapterId);

    if (adapter == null) {
      resultHandler.handle(Future.failedFuture(
          String.format("Adapter with id %s does not exist.", adapterId))
      );
    } else {
      // Send message to adapter to report back its devices.
      JelService.vertx().eventBus().send(
          String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
          null, options, res -> {
            if (res.succeeded()) {
              JsonObject result = (JsonObject) res.result().body();
              resultHandler.handle(Future.succeededFuture(result));
            } else {
              resultHandler.handle(Future.failedFuture(res.cause()));
            }
          });
    }
  }

  public void updateDeviceValue(String adapterId, String deviceId, JsonObject value, Handler<AsyncResult<Void>> resultHandler) {

  }

  /**
   * Get all devices bound to a specified site.
   *
   * @param siteId site to get devices for
   * @return collection of devices
   */
  /*public List<? extends Device> listSiteDevices(String siteId) {
   List<? extends Device> devices = new ArrayList<>();

   if (siteDevices.containsKey(siteId)) {
   devices = siteDevices.get(siteId);
   }

   return devices;
   }*/
}
