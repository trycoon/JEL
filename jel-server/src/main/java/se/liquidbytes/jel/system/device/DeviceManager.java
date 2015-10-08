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
   * Collection of all devices for a specific site.
   */
  private final Map<String, List<? extends Device>> siteDevices;

  /**
   * Default constructor.
   */
  public DeviceManager() {
    siteDevices = new ConcurrentHashMap<>();

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
   * Get all devices bound to a specified site.
   *
   * @param siteId site to get devices for
   * @return collection of devices
   */
  public List<? extends Device> listSiteDevices(String siteId) {
    List<? extends Device> devices = new ArrayList<>();

    if (siteDevices.containsKey(siteId)) {
      devices = siteDevices.get(siteId);
    }

    return devices;
  }

  /**
   * Returns a list of all available devices that has not yet been bound to a site.
   *
   * @param resultHandler Promise will give the list of devices or a error if one has occured.
   */
  public void listUnboundDevices(Handler<AsyncResult<JsonArray>> resultHandler) {
    // TODO:
    // 1. For every adapter in list of adapters, send a "listDevices"-message.
    // 2. Collect response from all adapters, and add all devices to one hashmap.
    // 3. Get all devices from all sites.
    // 4. Filter out so only devices in hashmap that don't exists in list of devices from sites are left.
    // 5. Order list after adapter and devicename/id? and return list.

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

        //TODO: Figure out which devices that are bound and filter them out from this collection.
        resultHandler.handle(Future.succeededFuture(devices));
      }).except((onError) -> {
        resultHandler.handle(Future.failedFuture(onError.getString("errorMessage")));
      }).eval();
    }
  }
}
