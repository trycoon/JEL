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
package se.liquidbytes.jel.owfs;

import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.time.LocalDateTime;
import static java.util.Comparator.comparing;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.owfs.jowfsclient.Enums;
import org.owfs.jowfsclient.OwfsConnection;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.adapter.AbstractAdapter;
import static se.liquidbytes.jel.system.adapter.AdapterManager.EVENTBUS_ADAPTERS;
import se.liquidbytes.jel.system.device.DeviceManager;
import static se.liquidbytes.jel.system.device.DeviceManager.EVENTBUS_DEVICES;
import se.liquidbytes.jel.system.plugin.PluginException;

/**
 * Owfs-adapter for Dallas/Maxim 1-wire system
 *
 * @author Henrik Östman
 */
public class OwfsAdapter extends AbstractAdapter {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Consecutive attempts before request fails.
   */
  private final static int MAX_ATTEMPTS = 5;
  /**
   * Delay between polling 1-wire bus for available devices.
   */
  private final static int POLL_PRESENCE_DELAY = 3000;
  /**
   * Delay between polling 1-wire bus for value-changes.
   */
  private final static int POLL_VALUE_DELAY = 4000; //TODO: Set to 1000 when "simultaneous" is implemented.
  /**
   * Connection to Owfs-driver
   */
  private OwfsConnection owfs;
  /**
   * Host-setting for Owfs
   */
  private String host;
  /**
   * Port-setting for Owfs
   */
  private int port;
  /**
   * Presence pollhandler id, saved to be able to cancel pollhandler when we should shutdown.
   */
  private long pollPresenceHandlerId;
  /**
   * Value pollhandler id, saved to be able to cancel pollhandler when we should shutdown.
   */
  private long pollValueHandlerId;
  /**
   * Device Id to device lookup table.
   */
  private Map<String, JsonObject> deviceLookup;
  /**
   * Device read values.
   */
  private Map<String, JsonObject> deviceReadings;
  /**
   * Attempt counter
   */
  private int attempts;

  /**
   * Start method for adapter, will be called upon when adapter is expected to start up
   */
  @Override
  public void start() {
    attempts = 0;
    deviceLookup = new ConcurrentHashMap<>();
    deviceReadings = new ConcurrentHashMap<>();
    this.setId(context.config().getString("adapterId"));

    setupOwfsConnection();
    scanAvailableDevices();

    // Periodic poll buss for device changes (added/removed).
    pollPresenceHandlerId = vertx.setPeriodic(POLL_PRESENCE_DELAY, id -> {
      scanAvailableDevices();
    });

    // Periodic poll buss for current device values.
    pollValueHandlerId = vertx.setPeriodic(POLL_VALUE_DELAY, id -> {
      simultaneousReadValues();
    });

    // Register this adapter to the eventbus so we could take requests and send notifications.
    // We register this adapter serveral times at different addresses on the eventbus, this is because there could be several instances of the adapter running on different IP-addresses and ports,
    // and we may want to send a command to a specific instance, to all instances of an adapter-type, and to ALL adapters.
    EventBus eb = vertx.eventBus();
    MessageConsumer<String> consumer;
    consumer = eb.consumer(String.format("%s.%s", EVENTBUS_ADAPTERS, "_all"));
    consumer.handler(message -> {
      attempts = 0;
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s", EVENTBUS_ADAPTERS, "owfs"));
    consumer.handler(message -> {
      attempts = 0;
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, "owfs", this.host, this.port));
    consumer.handler(message -> {
      attempts = 0;
      handleRequest(message);
    });

    consumer.completionHandler(res -> {
      if (res.succeeded()) {
        logger.info("Owfs-adapter initialized to owserver running at {}:{}.", this.host, this.port);
      } else {
        logger.error("Failed to start Owserver-adapter, connected to owserver running at {}:{}.", this.host, this.port, res.cause());
        throw new PluginException("Failed to start Owserver-adapter.", res.cause());
      }
    });
  }

  /**
   * Stop method for adapter, will be called upon when adapter should shutdown.
   */
  @Override
  public void stop() {
    if (pollValueHandlerId > 0) {
      vertx.cancelTimer(pollValueHandlerId);
      pollValueHandlerId = 0;
    }

    if (pollPresenceHandlerId > 0) {
      vertx.cancelTimer(pollPresenceHandlerId);
      pollPresenceHandlerId = 0;
    }

    if (deviceLookup != null) {
      deviceLookup.clear();
      deviceLookup = null;
    }

    if (deviceReadings != null) {
      deviceReadings.clear();
      deviceReadings = null;
    }

    if (this.owfs != null) {
      try {
        this.owfs.disconnect();
      } catch (IOException ex) {
        logger.warn("Failed to disconnect from Owserver running at {}:{} when stopping Owserver-adapter.", this.host, this.port, ex);
      }
      this.owfs = null;
    }
  }

  /**
   * Get human-readable description about adapter.
   *
   * @return
   */
  @Override
  public String getDescription() {
    return "Adapter for supporting Dallas/Maxim 1-wire system using the open source library OWFS, http://owfs.org/";
  }

  /**
   * Whether adapter supports autodetection of connected devices.
   *
   * @return
   */
  @Override
  public boolean isDevicesAutodetected() {
    return true;
  }

  /**
   * Main method for parsing the different requests that are sent to this adapter.
   *
   * @param message
   */
  private void handleRequest(Message message) {
    String action = message.headers().get("action");
    logger.debug("Executing action \"{}\" on Owserver with id \"{}\" running at {}:{}.", action, this.getId(), this.host, this.port);

    try {
      switch (action) {
        case "listSupportedDevices":
          this.getSupportedDevices(message);
          break;
        case "listDevices":
          this.getAvailableDevices(message);
          break;
        case "retrieveDeviceValue":
          this.getDeviceValue(message);
          break;
        case "updateDeviceValue":
          this.setDeviceValue(message);
          break;
        default:
          logger.info("Received a request for a non-implemented action '{}'. Ignoring action.", action);
      }

      logger.debug("Done executing action \"{}\" on Owserver with id \"{}\" running at {}:{}.", action, this.getId(), this.host, this.port);
    } catch (DeviceMissingException ex) {
      logger.info("Trying to perform an action on a non existing device ({}) on Owserver with id \"{}\" running at {}:{}.", ex.getDeviceId(), this.getId(), this.host, this.port);
      message.fail(404, ex.getMessage());
    } catch (SocketException ex) {
      if (attempts > MAX_ATTEMPTS) {
        logger.error("Failed to execute action \"{}\" on Owserver with id \"{}\" running at {}:{}, connection seems down. Done trying to reconnect.", action, this.getId(), this.host, this.port, ex);
        message.fail(500, ex.getMessage());
      } else {
        attempts++;
        logger.warn("Failed to execute action \"{}\" on Owserver with id \"{}\" running at {}:{}, connection seems down. Reconnect attempt# {}.", action, this.getId(), this.host, this.port, attempts, ex);
        setupOwfsConnection();
        // Wait one sec, to not SPAM us to death.
        vertx.setTimer(1000, timeoutId -> {
          handleRequest(message);
        });
      }
    } catch (OwfsException ex) {
      logger.error("Failed to execute action \"{}\" on Owserver with id \"{}\" running at {}:{}, got errorcode", action, this.getId(), this.host, this.port, ex.getErrorCode(), ex);
      message.fail(500, ex.getMessage());
    } catch (IOException ex) {
      logger.error("Failed to execute action \"{}\" on Owserver with id \"{}\" running at {}:{}.", action, this.getId(), this.host, this.port, ex);
      message.fail(500, ex.getMessage());
    }
  }

  /**
   * Constructs a replymessage to send back over the bus. This should contain the result of the executed request, but also information about which adapter that
   * responded (nice for the client to have when the request was broadcasted to multiple adapters).
   *
   * @param result the result of the processed request.
   * @return replymessage to send back.
   */
  private JsonObject constructReply(Object result) {
    JsonObject msgReply = new JsonObject();
    msgReply.put("adapterId", this.getId());
    msgReply.put("host", this.host);
    msgReply.put("port", this.port);
    msgReply.put("result", result);

    return msgReply;
  }

  /**
   * Setup the connection to the Owfs-driver, this may be called upon several times in case we drop connections.
   */
  private void setupOwfsConnection() {
    if (this.owfs != null) {
      try {
        owfs.disconnect();
      } catch (IOException ex) {
        // Ignore.
      }
      owfs = null;
    }

    try {
      this.host = context.config().getString("address", "127.0.0.1");
      this.port = context.config().getInteger("port", 4304);
      OwfsConnectionFactory factory = new OwfsConnectionFactory(this.host, this.port);
      OwfsConnectionConfig config = factory.getConnectionConfig();
      config.setDeviceDisplayFormat(Enums.OwDeviceDisplayFormat.F_DOT_I); // Display devices in format "10.67C6697351FF" (family-code.id)
      config.setTemperatureScale(Enums.OwTemperatureScale.CELSIUS);       // Celsius is the default temperature-scale we use, we convert it in JEL to other formats if needed.
      config.setPersistence(Enums.OwPersistence.ON);                      // User a persistent connection to owserver if possible
      config.setBusReturn(Enums.OwBusReturn.OFF);                         // Only show the devices in directory-listings, we don't want to iterate over other directories (like "structure", "settings"...)
      this.owfs = factory.createNewConnection();
    } catch (Exception ex) {
      logger.error("Error while trying to setup Owserver-connection to {}:{} for adapter with id \"{}\".", this.host, this.port, this.getId(), ex);
      throw ex;
    }
  }

  /**
   * Scan for available devices on 1-wire bus. And updates our internal list of available devices.
   */
  private void scanAvailableDevices() {
    try {
      String deviceId, deviceType, deviceFamily;
      Set<String> foundDeviceIds = new HashSet<>();
      EventBus eb = vertx.eventBus();
      JsonObject device, childDevice, broadcastDevice;

      List<String> owDevices = owfs.listDirectoryAll("/uncached");  // Bypass owservers internal cache, so we are sure we get fresh info.

      for (String owDevice : owDevices) {
        deviceId = owfs.read(owDevice + "/id");
        deviceType = owfs.read(owDevice + "/type");
        deviceFamily = owfs.read(owDevice + "/family");
        foundDeviceIds.add(deviceId);

        device = deviceLookup.get(deviceId);
        // Device not found in existing list, this must be a newly added device. Add it to the collection and broadcast its existence.
        if (device == null) {
          if (DeviceDatabase.getDeviceTypeInfo(deviceType) != null) {
            JsonObject typeInfo = DeviceDatabase.getDeviceTypeInfo(deviceType);

            // For devices that need to be setup in a special state to be usable, run their init commands when added to list of available devices.
            if (typeInfo.containsKey("initCommands")) {
              for (Iterator it = typeInfo.getJsonArray("initCommands").iterator(); it.hasNext();) {
                JsonObject command = (JsonObject) it.next();
                String path = owDevice + command.getString("path");
                logger.debug("Running initcommand (path '{}', value '{}') for device '{}' on Owserver at {}:{} with id \"{}\".", path, command.getString("value"), deviceId, this.host, this.port, this.getId());

                owfs.write(path, command.getString("value"));
              }
            }

            device = new JsonObject();
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("name", typeInfo.getString("name"));
            device.put("family", deviceFamily);
            device.put("path", owDevice);
            device.put("typeInfo", typeInfo);

            deviceLookup.put(deviceId, device);
            logger.info("New device found during scan of Owserver at {}:{} with id \"{}\". Device id: {}, type: {}, family: {}.", this.host, this.port, this.getId(), deviceId, deviceType, deviceFamily);

            broadcastDevice = new JsonObject()
                .put("adapterId", this.getId())
                .put("port", this.port)
                .put("host", this.host)
                .put("deviceId", deviceId)
                .put("type", deviceType)
                .put("name", typeInfo.getString("name"));
            eb.publish(EVENTBUS_DEVICES, broadcastDevice, new DeliveryOptions().addHeader("action", DeviceManager.EVENTBUS_DEVICES_ADDED));

            // Check if this device is an container for other "child-devices". In that case, add all the children too, their Id will be <parent#childnumber>.
            if (typeInfo.containsKey("childDevices")) {
              for (Iterator it = typeInfo.getJsonArray("childDevices").iterator(); it.hasNext();) {
                JsonObject childType = (JsonObject) it.next();
                String childId = String.format("%s#%s", deviceId, childType.getString("idSuffix"));

                childDevice = new JsonObject();
                childDevice.put("id", childId);
                childDevice.put("type", deviceType);
                childDevice.put("name", String.format("%s-%s", typeInfo.getString("name"), childType.getString("name")));

                deviceLookup.put(childId, childDevice);
                logger.info("New childdevice for device {} found during scan of Owserver at {}:{} with id \"{}\". Device id: {}, type: {}, family: {}.", deviceId, this.host, this.port, this.getId(), childId, deviceType, deviceFamily);

                broadcastDevice = new JsonObject()
                    .put("adapterId", this.getId())
                    .put("port", this.port)
                    .put("host", this.host)
                    .put("deviceId", childId)
                    .put("type", deviceType)
                    .put("name", childDevice.getString("name"));
                eb.publish(EVENTBUS_DEVICES, broadcastDevice, new DeliveryOptions().addHeader("action", DeviceManager.EVENTBUS_DEVICES_ADDED));
              }
            }
          } else {
            logger.info("Found unsupported devicetype for device with id '{}' on Owserver at {}:{} with id \"{}\". Device will be ignored! Please notify developers and provide: type={}, family={}.", deviceId, this.host, this.port, this.getId(), deviceType, deviceFamily);
          }
        }
      }

      // Remove all devices in device list that was no longer found during this scan. They has been disconnected from the 1-wire bus.
      Set<String> tempSet = new HashSet(deviceLookup.keySet());
      tempSet.removeAll(foundDeviceIds);

      for (String removeId : tempSet) {
        // Check that it's not a childdevice, we are not interested in them right now.
        if (!removeId.contains("#")) {
          List<String> childDevicesId = tempSet.stream().filter(d -> d.startsWith(removeId + "#")).collect(Collectors.toList());
          for (String childDeviceId : childDevicesId) {
            removeDeviceFromLookup(childDeviceId, eb);
          }
        }
      }
    } catch (SocketException ex) {
      logger.warn("Failed to scan Owserver at {}:{} with id \"{}\" for available devices, connection seems down. Trying to reconnect.", this.host, this.port, this.getId(), ex);
      try {
        setupOwfsConnection();
      } catch (Exception ex2) {
        // Ignore.
      }
    } catch (OwfsException ex) {
      logger.error("Error while trying to scan Owserver at {}:{} with id \"{}\" for available devices. Received errorcode: {}", this.host, this.port, this.getId(), ex.getErrorCode(), ex);
    } catch (IOException ex) {
      logger.error("Error while trying to scan Owserver at {}:{} with id \"{}\" for available devices.", this.host, this.port, this.getId(), ex);
    }
  }

  /**
   * Get only parent devices from the deviceLookup-
   *
   * @return List of "parent" devices.
   */
  private List<JsonObject> getParentDevicesOnly() {
    return deviceLookup.values().stream().filter(d -> !d.getString("id").contains("#")).collect(Collectors.toList());
  }

  /**
   * Get child devices for a parent device, empty list if no ones exists.
   *
   * @return List of child devices.
   */
  private List<JsonObject> getChildDevicesOnly(String parentId) {
    return deviceLookup.values().stream().filter(d -> d.getString("id").contains(parentId + "#"))
        .sorted((d1, d2) -> d1.getString("id").compareTo(d2.getString("id")))
        .collect(Collectors.toList());
  }

  /**
   * Remove a device from deviceLookup. Also signal it's departure on the bus
   *
   * @param removeId remmoved device id
   * @param eb eventbus instance
   */
  private void removeDeviceFromLookup(String removeId, EventBus eb) {
    deviceLookup.remove(removeId);
    logger.info("Device with id: {}, not found after last scan of Owserver at {}:{} with id \"{}\". Device has been removed from bus.", removeId, this.host, this.port, this.getId());

    JsonObject broadcastDevice = new JsonObject()
        .put("adapterId", this.getId())
        .put("port", this.port)
        .put("host", this.host)
        .put("deviceId", removeId);

    eb.publish(EVENTBUS_DEVICES, broadcastDevice, new DeliveryOptions().addHeader("action", DeviceManager.EVENTBUS_DEVICES_REMOVED));
  }

  /**
   * Returns a list of all supported devices by this adapter.
   *
   * @param message eventbus message.
   */
  private void getSupportedDevices(Message message) {

    JsonArray result = new JsonArray();

    for (JsonObject device : DeviceDatabase.getSuportedDeviceTypes()) {
      result.add(
          new JsonObject()
          .put("typeId", device.getString("typeId"))
          .put("name", device.getString("name"))
          .put("description", device.getString("description"))
          .put("manufacturer", device.getJsonObject("manufacturer"))
      );
    }

    message.reply(constructReply(result));
  }

  /**
   * Returns a list of all available devices on 1-wire bus.
   *
   * @param message eventbus message.
   */
  private void getAvailableDevices(Message message) {
    message.reply(constructReply(getAvailableDevices()));
  }

  /**
   * Read the current value from a device.
   *
   * @param device Existing device objekt.
   * @return Current value.
   */
  private String readValue(JsonObject device) {
    String id = device.getString("id");

    try {

      JsonObject typeInfo = device.getJsonObject("typeInfo");
      if (!typeInfo.containsKey("valueReadPath")) {
        // We can't read this kind of device.
        return null;
      }

      String path = device.getString("path") + typeInfo.getString("valueReadPath");
      String value = owfs.read(path).trim();

      return value;
    } catch (DeviceMissingException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s', device is reported missing.", ex.getDeviceId()), ex);
    } catch (IOException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s'.", id), ex);
    } catch (OwfsException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s', got errorcode: ", id, ex.getErrorCode()), ex);
    }
  }

  /**
   * Read current values from all active devices simultaneously.
   */
  private void simultaneousReadValues() {
    List<JsonObject> devices = getParentDevicesOnly();

    //TODO: echo "1" > /simultaneous/temperature, and others.
    for (JsonObject device : devices) {
      // Execute each device reading in a background thread, this to not lockup this thread waiting for responses.
      vertx.executeBlocking(future -> {
        try {
          String id = device.getString("id");

          // Do a blocking reading.
          String value = readValue(device);
          logger.trace("Read value '{}' from device with id '{}' on Owserver at {}:{} with id \"{}\".", value, id, this.host, this.port, this.getId());

          // Construct returnvalue.
          future.complete(new JsonObject()
              .put("id", id)
              .put("time", LocalDateTime.now().toString())
              .put("value", value)
          );
        } catch (Exception ex) {
          future.fail(ex);
        }
      }, res -> {
        if (res.succeeded()) {
          JsonObject readings;
          JsonObject reading = (JsonObject) res.result();
          String id = reading.getString("id");
          String value = reading.getString("value");

          // Get hold of array of recorded readings for this specific device.
          if (!deviceReadings.containsKey(id)) {
            readings = new JsonObject()
                .put("lastReading", new JsonObject()
                    .put("value", (Object) null) // Set property to prevent a possible nullpointer exception later.
                );

            deviceReadings.put(id, readings);
          } else {
            readings = deviceReadings.get(id);
          }

          String lastValue = readings.getJsonObject("lastReading").getString("value");
          // Only add this reading to list of readings for device if the value of the reading has changed since the last time we did a reading. We save a lot of space and memory by doing this!
          if (!value.equals(lastValue)) {
            readings.put("lastReading", reading);
            logger.info("Recorded new value '{}' at time '{}' for device with id '{}' on Owserver at {}:{} with id \"{}\".", value, reading.getString("time"), id, this.host, this.port, this.getId());

            List<JsonObject> childs = getChildDevicesOnly(id);

            if (childs.isEmpty()) {
              // This device has no children, so we just notify that this device has a value that has changed.
              JsonObject broadcast = new JsonObject()
                  .put("adapterId", this.getId())
                  .put("port", this.port)
                  .put("host", this.host)
                  .put("reading", reading);

              vertx.eventBus().publish(EVENTBUS_DEVICES, broadcast, new DeliveryOptions().addHeader("action", DeviceManager.EVENTBUS_DEVICE_NEWREADING));
            } else {
              // This is a parent device so we must check which of its children  that has changed and notify each and every one of them on the bus.
              String[] childValues = value.split(",");
              String[] lastChildValues;

              if (lastValue == null) {
                lastChildValues = new String[childValues.length];
              } else {
                lastChildValues = lastValue.split(",");
              }

              for (int i = 0; i < childValues.length; i++) {
                if (!childValues[i].equals(lastChildValues[i])) {
                  JsonObject broadcast = new JsonObject()
                      .put("adapterId", this.getId())
                      .put("port", this.port)
                      .put("host", this.host)
                      .put("reading", new JsonObject()
                          .put("id", childs.get(i).getString("id"))
                          .put("time", reading.getString("time"))
                          .put("value", childValues[i])
                      );

                  vertx.eventBus().publish(EVENTBUS_DEVICES, broadcast, new DeliveryOptions().addHeader("action", DeviceManager.EVENTBUS_DEVICE_NEWREADING));
                }
              }
            }
          }
        } else {
          logger.warn("Failed to read current value from device.", res.cause());
        }
      });
    }
  }

  /**
   * Returns a list of all available devices on 1-wire bus.
   *
   * @return list of devices.
   */
  private JsonArray getAvailableDevices() {
    JsonArray result = new JsonArray();

    List<JsonObject> deviceList = deviceLookup.values().stream().sorted(comparing((JsonObject d) -> d.getString("type"))
        .thenComparing((JsonObject d) -> d.getString("name")))
        .collect(Collectors.toList());
    for (JsonObject device : deviceList) {
      result.add(
          new JsonObject()
          .put("id", device.getString("id"))
          .put("type", device.getString("type"))
          .put("name", device.getString("name"))
      );
    }

    return result;
  }

  /**
   * Read value from device with specified id.
   *
   * @param message eventbus message.
   * @throws DeviceMissingException throws exception if specified device does not exist.
   */
  private void getDeviceValue(Message message) throws DeviceMissingException, IOException, OwfsException {
    // Validate and extract action-specific parameters.
    if (message.body() == null) {
      message.fail(400, "Missing parameters.");
      return;
    }

    JsonObject params = (JsonObject) message.body();
    String deviceId = params.getString("deviceId");

    if (deviceId == null || deviceId.isEmpty()) {
      message.fail(400, "Missing parameter 'deviceId'.");
      return;
    }

    message.reply(this.constructReply(this.getDeviceValue(deviceId)));
  }

  /**
   * Read value from device with specified id.
   *
   * @param deviceId Id on device
   * @return device last recorded value.
   * @throws DeviceMissingException throws exception if specified device does not exist.
   */
  private JsonObject getDeviceValue(String deviceId) throws DeviceMissingException, IOException, OwfsException {

    if (deviceId == null || !deviceLookup.containsKey(deviceId)) {
      throw new DeviceMissingException("Trying to perform a action on a non existing device.", deviceId);
    }

    // Check if child device.
    if (!deviceId.contains("#")) {
      JsonObject reading = this.deviceReadings.get(deviceId);

      JsonObject response = new JsonObject()
          .put("reading", reading);

      return response;
    } else {
      JsonObject reading = this.deviceReadings.get(deviceId.split("#")[0]); // Get value from parent.
      int index = Integer.parseInt(deviceId.split("#")[1]) - 1;         // Get child idsuffix.
      String value = reading.getJsonObject("lastReading").getString("value").split(",")[index];  // Get child reading from parent reading.

      JsonObject response = new JsonObject()
          .put("reading", new JsonObject()
              .put("id", deviceId)
              .put("time", reading.getJsonObject("lastReading").getString("time"))
              .put("value", value)
          );

      return response;
    }
  }

  /**
   * Set value on device with specified id.
   *
   * @param message eventbus message.
   * @throws DeviceMissingException throws exception if specified device does not exist.
   */
  private void setDeviceValue(Message message) throws DeviceMissingException, IOException, OwfsException {
    // Validate and extract action-specific parameters.
    if (message.body() == null) {
      message.fail(400, "Missing parameters.");
      return;
    }

    JsonObject params = (JsonObject) message.body();
    String deviceId = params.getString("deviceId");
    String value = params.getString("value");

    if (deviceId == null || deviceId.isEmpty()) {
      message.fail(400, "Missing parameter 'deviceId'.");
      return;
    }
    if (value == null || value.isEmpty()) {
      message.fail(400, "Missing parameter 'value'.");
      return;
    }

    this.setDeviceValue(deviceId, value);
  }

  /**
   * Set value on device with specified id.
   *
   * @param deviceId Id on device
   * @param value value to set on device.
   * @throws DeviceMissingException throws exception if specified device does not exist.
   */
  private void setDeviceValue(String deviceId, String value) throws DeviceMissingException, IOException, OwfsException {

    if (deviceId == null || !deviceLookup.containsKey(deviceId)) {
      throw new DeviceMissingException("Trying to perform a action on a non existing device.", deviceId);
    }

    JsonObject device = deviceLookup.get(deviceId);
    JsonObject typeInfo = device.getJsonObject("typeInfo");

    // Check if this type of device is writable.
    if (typeInfo.containsKey("valueWritePath")) {
      String path = device.getString("path") + typeInfo.getString("valueWritePath");

      logger.debug("Write value {} to device '{}'.", value, deviceId);

      owfs.write(path, value);
    }
  }
}
