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
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.InternalEvents;
import se.liquidbytes.jel.system.JelService;
import se.liquidbytes.jel.system.PublicEvents;
import se.liquidbytes.jel.system.adapter.AdapterEvents;
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
   * Referense to eventbus subscription for result-events, just so that we could unsubscibe when shutting down.
   */
  private MessageConsumer deviceEventConsumer;

  /**
   * Referense to eventbus subscription for adapter-events, just so that we could unsubscibe when shutting down.
   */
  private MessageConsumer adapterEventConsumer;

  /**
   * Collection of all devices bound to a site and unbound. Key is result id.
   */
  private final Map<String, JsonObject> allDevices;

  /**
   * Collection of all devices not yet bound to a site. When they are "unbound" they have no settings available and no history of past readings are saved. Key
   * is result id.
   */
  private final Map<String, JsonObject> unboundDevices;

  /**
   * Collection of all devices for a specific site. This does not necessary mean that the devices are present, it only lists the devices that we expect to be
   * present on a given site. These lists are persisted in the database. Key is site id.
   */
  private final Map<String, Map<String, ? extends Device>> siteDevices;

  /**
   * Default constructor.
   */
  public DeviceManager() {
    allDevices = new ConcurrentHashMap<>();
    unboundDevices = new ConcurrentHashMap<>();
    siteDevices = new ConcurrentHashMap<>();

    // TODO: Remove these when database is in place!
    Map<String, Device> devices = new ConcurrentHashMap<>();
    String adapterId = "833142300";

    Sensor sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "7F0560010800"));
    sensor.setName("Varmvatten ut");
    sensor.setDescription("Varmvatten ut(gamla värmecentral)");
    //sensor.setHardware(new DeviceHardware());
    /*sensor.setCurrentValue(new DeviceValue());
    sensor.setPreviousValue(new DeviceValue());
    sensor.setMaxValue(new DeviceValue());
    sensor.setMinValue(new DeviceValue());
    sensor.setLargePresentation(new DevicePresentation());
    sensor.setMediumPresentation(new DevicePresentation());
    sensor.setSmallPresentation(new DevicePresentation());*/
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "969D98010800"));
    sensor.setName("Varmvatten tillbaka");
    sensor.setDescription("Varmvatten tillbaka(gamla värmecentral)");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "93D848010800"));
    sensor.setName("Förådet");
    sensor.setDescription("Förådet(i ladan)");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "7174B5010800"));
    sensor.setName("Utomhus");
    sensor.setDescription("Utomhus(vid elmätaren vid ladan)");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "454849010800"));
    sensor.setName("Pumphuset");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "9D9A98010800"));
    sensor.setName("Hönshuset");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "B74C8A010800"));
    sensor.setName("Vardagsrummet");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "C9E69E010800"));
    sensor.setName("Hall(toalett/pannrum)");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "158DB5010800"));
    sensor.setName("Kök");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "AC1A60010800"));
    sensor.setName("Nya entren");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "0C92B5010800"));
    sensor.setName("Badrum");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "52A9B5010800"));
    sensor.setName("Arbetsrum");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "2A8DB5010800"));
    sensor.setName("Leias sovrum");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "18A4B5010800"));
    sensor.setName("Hall/toalett på övervåning");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "22A9B5010800"));
    sensor.setName("Caspers sovrum");
    devices.put(sensor.getId(), sensor);

    sensor = new Sensor();
    sensor.setId(this.generateDeviceId(adapterId, "407F98010800"));
    sensor.setName("Vårt sovrum");
    devices.put(sensor.getId(), sensor);

    Actuator actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "340501000000"));
    actuator.setName("Pumphuset(vattenpumps mätare)");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "4D4D13000000_2"));
    actuator.setName("Hall och pannrum");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "4D4D13000000_3"));
    actuator.setName("Vardagsrum");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "4D4D13000000_4"));
    actuator.setName("Kök");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "3E4D13000000_1"));
    actuator.setName("Nya hallen");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "3E4D13000000_2"));
    actuator.setName("Badrum");
    devices.put(actuator.getId(), actuator);

    actuator = new Actuator();
    actuator.setId(this.generateDeviceId(adapterId, "3E4D13000000_3"));
    actuator.setName("Arbetsrum");
    devices.put(actuator.getId(), actuator);

    siteDevices.put("1", devices);
  }

  /**
   * Method for starting result manager, should be called upon application startup.
   */
  public void start() {

    logger.info("Starting devicemanager.");

    subscribeOnDeviceEvents();
    subscribeOnAdapterEvents();
  }

  /**
   * Method for stopping result manager, should be called upon application shutdown.
   */
  public void stop() {
    logger.info("Shutting down devicemanager.");

    if (deviceEventConsumer != null && deviceEventConsumer.isRegistered()) {
      deviceEventConsumer.unregister();
    }

    if (adapterEventConsumer != null && adapterEventConsumer.isRegistered()) {
      adapterEventConsumer.unregister();
    }

    siteDevices.clear();
    unboundDevices.clear();
    allDevices.clear();
  }

  public void createAdapterDevice(String adapterId, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet.");
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
          String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
          null, options, res -> {
            if (res.succeeded()) {
              JsonArray devices = new JsonArray();

              JsonObject result = (JsonObject) res.result().body();
              result.getJsonArray("result").forEach((d) -> {
                JsonObject tmpDevice = (JsonObject) d;

                // Sync found devices with allDevices-collection in case we are out of sync and have missed an event!
                addToDeviceCollections(
                    new JsonObject()
                    .put("adapterId", adapterId)
                    .put("name", tmpDevice.getString("name"))
                    .put("type", tmpDevice.getString("type"))
                    .put("hwId", tmpDevice.getString("hwId"))
                );

                devices.add(
                    new JsonObject()
                    .put("deviceId", generateDeviceId(adapterId, tmpDevice.getString("hwId")))
                    .put("type", tmpDevice.getString("type"))
                    .put("name", tmpDevice.getString("name"))
                );
              });

              resultHandler.handle(Future.succeededFuture(devices));
            } else {
              resultHandler.handle(Future.failedFuture(res.cause()));
            }
          });
    }
  }

  public void retrieveAdapterDevice(String id, Handler<AsyncResult<JsonObject>> resultHandler) {
    resultHandler.handle(Future.failedFuture(new UnsupportedOperationException("Not supported yet.")));
  }

  public void updateAdapterDevice(String id, JsonObject device, Handler<AsyncResult<JsonObject>> resultHandler) {
    resultHandler.handle(Future.failedFuture(new UnsupportedOperationException("Not supported yet.")));
  }

  public void deleteAdapterDevice(String id, Handler<AsyncResult<Void>> resultHandler) {
    resultHandler.handle(Future.failedFuture(new UnsupportedOperationException("Not supported yet.")));
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
          String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
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
              String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, _item.config().getType(), _item.config().getAddress(), _item.config().getPort()),
              null, options, res -> {
                if (res.succeeded()) {
                  // All adapters fills in turn a json-array named "adapterDevices".
                  JsonArray adapterDevices = context.getJsonArray("adapterDevices");

                  // If we are the first adapter to report back, create the array.
                  if (adapterDevices == null) {
                    adapterDevices = new JsonArray();
                  }

                  JsonObject result = (JsonObject) res.result().body();

                  adapterDevices.add(new JsonObject()
                      .put("adapterId", result.getString("adapterId"))
                      .put("devices", new JsonArray().addAll(result.getJsonArray("result")))
                  );

                  context.put("adapterDevices", adapterDevices);
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
        JsonArray adapterDevices = onSuccess.getJsonArray("adapterDevices");
        JsonArray devices = new JsonArray();

        adapterDevices.forEach((adl) -> {

          JsonObject adapterDeviceList = (JsonObject) adl;
          String adapterId = adapterDeviceList.getString("adapterId");

          adapterDeviceList.getJsonArray("devices").forEach((ad) -> {
            JsonObject tmpDevice = (JsonObject) ad;

            devices.add(
                new JsonObject()
                .put("deviceId", generateDeviceId(adapterId, tmpDevice.getString("hwId")))
                .put("type", tmpDevice.getString("type"))
                .put("name", tmpDevice.getString("name"))
            );

            // Sync found devices with allDevices-collection in case we are out of sync and have missed an event!
            addToDeviceCollections(
                new JsonObject()
                .put("adapterId", adapterId)
                .put("name", tmpDevice.getString("name"))
                .put("type", tmpDevice.getString("type"))
                .put("hwId", tmpDevice.getString("hwId"))
            );
          });
        });

        resultHandler.handle(Future.succeededFuture(devices));
      }).except((onError) -> {
        resultHandler.handle(Future.failedFuture(onError.getString("errorMessage")));
      }).eval();
    }
  }

  public void listSiteDevices(String siteId, Handler<AsyncResult<JsonArray>> resultHandler) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Retrieve the current value of an result using specified result id.
   *
   * @param deviceId id of existing result.
   * @param resultHandler
   */
  public void retrieveDeviceValue(String deviceId, Handler<AsyncResult<JsonObject>> resultHandler) {
    DeliveryOptions options = new DeliveryOptions();
    options.addHeader("action", "retrieveDeviceValue");

    JsonObject device = allDevices.get(deviceId);
    if (device == null) {
      resultHandler.handle(Future.failedFuture(
          String.format("Device with id %s does not exist.", deviceId))
      );
      return;
    }

    DeployedAdapter adapter = JelService.adapterManager().getAdapter(device.getString("adapterId"));

    if (adapter == null) {
      resultHandler.handle(Future.failedFuture(
          String.format("Adapter with id %s does not exist.", device.getString("adapterId")))
      );
      return;
    }

    // Send message to adapter to report back its devices.
    JelService.vertx().eventBus().send(
        String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
        new JsonObject().put("hwId", device.getString("hwId")), options, res -> {
      if (res.succeeded()) {
        JsonObject reading = ((JsonObject) res.result().body()).getJsonObject("result").getJsonObject("reading");

        String time = null, value = null;

        if (reading != null) {
          time = reading.getJsonObject("lastReading").getString("time");
          value = reading.getJsonObject("lastReading").getString("value");
        }

        JsonObject result = new JsonObject()
            .put("deviceId", device.getString("deviceId"))
            .put("time", time)
            .put("value", value);

        resultHandler.handle(Future.succeededFuture(result));
      } else {
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
  }

  /**
   * Update the value of an existing result using specified id and value.
   *
   * @param deviceId id of existing result.
   * @param value value to set.
   * @param resultHandler
   */
  public void updateDeviceValue(String deviceId, String value, Handler<AsyncResult<Void>> resultHandler) {
    JsonObject device = allDevices.get(deviceId);

    if (device == null) {
      resultHandler.handle(Future.failedFuture(String.format("No device with that id(%s) exists.", deviceId)));
    } else {
      String adapterId = device.getString("adapterId");
      DeliveryOptions options = new DeliveryOptions();
      options.addHeader("action", "updateDeviceValue");
      DeployedAdapter adapter = JelService.adapterManager().getAdapter(adapterId);

      if (adapter == null) {
        resultHandler.handle(Future.failedFuture(
            String.format("Adapter with id %s does not exist.", adapterId))
        );
      } else {
        JsonObject data = new JsonObject()
            .put("hwId", device.getString("hwId"))
            .put("value", value);

        logger.info("Setting new value('{}') on device with id: {} and hwId: {}.", value, device.getString("deviceId"), device.getString("hwId"));

        JelService.vertx().eventBus().send(
            String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort()),
            data, options);

        resultHandler.handle(Future.succeededFuture());
      }
    }
  }

  /**
   * Start subscribe on result-events triggered by adapters.
   */
  private void subscribeOnDeviceEvents() {
    deviceEventConsumer = JelService.vertx().eventBus().consumer(AdapterEvents.EVENTBUS_ADAPTERS, (r) -> {
      String action = r.headers().get("action");

      if (action != null) {
        JsonObject result;

        switch (action) {
          case AdapterEvents.EVENT_DEVICES_ADDED:
            result = (JsonObject) r.body();
            JsonObject newDevice = new JsonObject()
                .put("adapterId", result.getString("adapterId"))
                .put("name", result.getString("name"))
                .put("type", result.getString("type"))
                .put("hwId", result.getString("hwId"));

            addToDeviceCollections(newDevice);
            break;
          case AdapterEvents.EVENT_DEVICES_REMOVED:
            result = (JsonObject) r.body();

            removeDeviceFromCollections(result);
            break;

          case AdapterEvents.EVENT_DEVICE_NEWREADING:
            result = (JsonObject) r.body();

            handleNewDeviceReading(result);
            break;
        }

      }
    });
  }

  /**
   * Start subscribe on adapter-events triggered by adapters.
   */
  private void subscribeOnAdapterEvents() {
    adapterEventConsumer = JelService.vertx().eventBus().consumer(InternalEvents.EVENTBUS_INTERNAL, (r) -> {
      String action = r.headers().get("action");

      if (action != null) {
        switch (action) {
          case InternalEvents.EVENT_ADAPTER_STARTED: {
            JsonObject adapter = (JsonObject) r.body();
            // For newly added adapter, scan for all its connected devices. But first give it some time to settle down and find all devices.
            JelService.vertx().setTimer(3500, (Void) -> {
              this.listAdapterDevices(adapter.getString("id"), (deviceList) -> {
                // Do nothing here. This execution will trigger the population of devices within listAdapterDevices.
              });
            });
            break;
          }
          case InternalEvents.EVENT_ADAPTER_STOPPED: {
            JsonObject adapter = (JsonObject) r.body();
            // For removed adapter, update result collections to reflect the changes.
            removeAdapterDevicesFromCollections(adapter.getString("id"));
            break;
          }
        }
      }
    });
  }

  /**
   * Validate a result JSON-object.
   *
   * @param device JSON-object o validate.
   * @return result.
   */
  private boolean validateDeviceObject(JsonObject device) {
    if (device == null) {
      return false;
    }
    try {
      String adapterId = device.getString("adapterId");
      if (adapterId == null || adapterId.length() == 0) {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }
    try {
      String name = device.getString("name");
      if (name == null || name.length() == 0) {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }
    try {
      String type = device.getString("type");
      if (type == null || type.length() == 0) {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }

    try {
      String hwId = device.getString("hwId");
      if (hwId == null || hwId.length() == 0) {
        return false;
      }
    } catch (Exception ex) {
      return false;
    }

    return true;
  }

  /**
   * Add a new result to all result collections and broadcast events. Use this method instead of modifying the allDevices-collections by yourself!
   *
   * @param device JSON-object containing: adapterId, name, type, hwId.
   */
  private void addToDeviceCollections(JsonObject device) {
    // Makde sure we don't add junk that will crash us later.
    if (validateDeviceObject(device)) {
      String deviceId = this.generateDeviceId(device.getString("adapterId"), device.getString("hwId"));
      device.put("deviceId", deviceId); // Make sure this is set, and with a correct value.

      // This method may be called upon serveral times with the same result, make sure we only add it once!
      if (!this.allDevices.containsKey(deviceId)) {
        this.allDevices.put(deviceId, device);
      }

      Device siteDevice = null;

      for (Map<String, ? extends Device> deviceLists : siteDevices.values()) {
        for (Device tmpDevice : deviceLists.values()) {
          if (tmpDevice.getId().equals(deviceId)) {
            siteDevice = tmpDevice;
            break;
          }
        }
      }

      if (siteDevice == null) {
        if (!this.unboundDevices.containsKey(deviceId)) {
          this.unboundDevices.put(deviceId, device);
          JsonObject broadcast = new JsonObject()
              .put("adapterId", device.getString("adapterId"))
              .put("name", device.getString("name"))
              .put("type", device.getString("type"))
              .put("deviceId", device.getString("deviceId"));

          JelService.vertx().eventBus().publish(InternalEvents.EVENTBUS_INTERNAL, broadcast, new DeliveryOptions().addHeader("action", InternalEvents.EVENT_DEVICES_ADDED));
          JelService.vertx().eventBus().publish(PublicEvents.EVENTBUS_PUBLIC, broadcast, new DeliveryOptions().addHeader("action", PublicEvents.EVENT_DEVICES_ADDED));
        }
      } else if (!siteDevice.isPresent()) {
        // If not already present, set as present and broadcast change.
        siteDevice.isPresent(true);

        JsonObject broadcast = new JsonObject()
            .put("adapterId", device.getString("adapterId"))
            .put("name", device.getString("name"))
            .put("type", device.getString("type"))
            .put("deviceId", device.getString("deviceId"));
        JelService.vertx().eventBus().publish(InternalEvents.EVENTBUS_INTERNAL, broadcast, new DeliveryOptions().addHeader("action", InternalEvents.EVENT_DEVICE_PRESENT));
        JelService.vertx().eventBus().publish(PublicEvents.EVENTBUS_PUBLIC, broadcast, new DeliveryOptions().addHeader("action", PublicEvents.EVENT_DEVICE_PRESENT));
      }
    } else {
      logger.debug("Device failed validation, skipping adding device to collection.");
    }
  }

  /**
   * Removes all devices from collections belonging to specified adapter.
   *
   * @param adapterId Id of adapter that has been removed
   */
  private void removeAdapterDevicesFromCollections(String adapterId) {

    //TODO: if in sitelist, set result as not present and broadcast that to clients.
  }

  /**
   * Remove specified result from collections.
   *
   * @param device JSON-object containing: adapterId, name, type, hwId.
   */
  private void removeDeviceFromCollections(JsonObject device) {

    //TODO: if in sitelist, set result as not present and broadcast that to clients.
  }

  /**
   * Generate a unique result id from adapter and result settings.
   *
   * @param adapterId adapter id.
   * @param deviceId result id.
   * @return
   */
  private String generateDeviceId(String adapterId, String deviceId) {
    return String.valueOf(Math.abs(java.util.Objects.hash(adapterId, deviceId)));
  }

  /**
   * Method takes action on a new result reading.
   *
   * @param deviceReading
   */
  private void handleNewDeviceReading(JsonObject deviceReading) {
    JsonObject reading = deviceReading.getJsonObject("reading");
    String deviceId = this.generateDeviceId(deviceReading.getString("adapterId"), reading.getString("hwId"));
    Map<String, ? extends Device> devices = siteDevices.get("1"); //TODO: hardcoded.
    Device siteDevice = devices.get(deviceId);

    int lowLimit = 22;
    int highLimit = 24;

    if (siteDevice != null) {
      double temp = Double.parseDouble(reading.getString("value"));
      logger.info("Sensor: '{}' with id: {} and hwid: {}, temp: {}.", siteDevice.getName(), deviceId, reading.getString("hwId"), temp);

      switch (reading.getString("hwId")) {
        case "B74C8A010800": {  // Vardagsrummet
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_3"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_3"), "0", (r) -> {
            });
          }
          break;
        }
        case "C9E69E010800": {  // Hall(toalett/pannrum)"
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_2"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_2"), "0", (r) -> {
            });
          }
          break;
        }
        case "158DB5010800": {  // kök
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_4"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "4D4D13000000_4"), "0", (r) -> {
            });
          }
          break;
        }
        case "AC1A60010800": {  // Nya entren
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_1"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_1"), "0", (r) -> {
            });
          }
          break;
        }
        case "52A9B5010800": {  // Arbetsrum
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_3"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_3"), "0", (r) -> {
            });
          }
          break;
        }
        case "0C92B5010800": {  // Badrum
          if (temp < lowLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_2"), "1", (r) -> {
            });
          }
          if (temp > highLimit) {
            this.updateDeviceValue(this.generateDeviceId(deviceReading.getString("adapterId"), "3E4D13000000_2"), "0", (r) -> {
            });
          }
          break;
        }
      }

      // TODO: update result last readings, and current value in deviceLists. possible broadcast to clients dependings on samplerate.
      JsonObject newReading = new JsonObject()
          .put("adapterId", deviceReading.getString("adapterId"))
          .put("deviceId", reading.getInteger("deviceId"))
          .put("time", reading.getString("time"))
          .put("value", reading.getString("value"));

      JelService.vertx().eventBus().publish(InternalEvents.EVENTBUS_INTERNAL, newReading, new DeliveryOptions().addHeader("action", InternalEvents.EVENT_DEVICE_NEWREADING));
      JelService.vertx().eventBus().publish(PublicEvents.EVENTBUS_PUBLIC, newReading, new DeliveryOptions().addHeader("action", PublicEvents.EVENT_DEVICE_NEWREADING));
    }
  }
}
