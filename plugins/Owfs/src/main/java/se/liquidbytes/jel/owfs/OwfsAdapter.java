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

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.util.HashSet;
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
  private final static int POLL_DELAY = 3000;
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
   * Pollhandler id, saved to be able to cancel pollhandler when we should shutdown.
   */
  private long pollHandlerId;
  /**
   * Device Id to device lookup table.
   */
  private Map<String, JsonObject> deviceLookup;
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

    setupOwfsConnection();
    scanAvailableDevices();
    // Periodic poll buss for device changes (added/removed).
    pollHandlerId = vertx.setPeriodic(POLL_DELAY, id -> {
      scanAvailableDevices();
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
    if (pollHandlerId > 0) {
      vertx.cancelTimer(pollHandlerId);
      pollHandlerId = 0;
    }

    if (deviceLookup != null) {
      deviceLookup.clear();
      deviceLookup = null;
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
    logger.debug("Executing action \"{}\" on Owserver running at {}:{}.", action, this.host, this.port);

    try {
      switch (action) {
        case "listDevices":
          message.reply(constructReply(getAvailableDevices()));
          break;
        case "getDeviceValue":
          message.reply(constructReply(getDeviceValue("")));
          break;
        case "setDeviceValue":
          setDeviceValue("", "1");
          message.reply(constructReply("OK"));
          break;
        default:
          logger.info("Received a request for a non-implemented action '{}'. Ignoring action.", action);
      }

      logger.debug("Done executing action \"{}\" on Owserver running at {}:{}.", action, this.host, this.port);
    } catch (DeviceMissingException ex) {
      logger.info("Trying to perform an action on a non existing device ({}) on Owserver running at {}:{}.", ex.getDeviceId(), this.host, this.port);
      message.fail(404, ex.getMessage());
    } catch (SocketException ex) {
      if (attempts > MAX_ATTEMPTS) {
        logger.error("Failed to execute action \"{}\" on Owserver running at {}:{}, connection seems down. Done trying to reconnect.", action, this.host, this.port, ex);
        message.fail(500, ex.getMessage());
      } else {
        attempts++;
        logger.warn("Failed to execute action \"{}\" on Owserver running at {}:{}, connection seems down. Reconnect attempt# {}.", action, this.host, this.port, attempts, ex);
        setupOwfsConnection();
        // Wait one sec, to not SPAM us to death.
        vertx.setTimer(1000, timeoutId -> {
          handleRequest(message);
        });
      }
    } catch (OwfsException ex) {
      logger.error("Failed to execute action \"{}\" on Owserver running at {}:{}, got errorcode", action, this.host, this.port, ex.getErrorCode(), ex);
      message.fail(500, ex.getMessage());
    } catch (IOException ex) {
      logger.error("Failed to execute action \"{}\" on Owserver running at {}:{}.", action, this.host, this.port, ex);
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
      logger.error("Error while trying to setup Owserver-connection to {}:{}.", this.host, this.port, ex);
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
      JsonObject device;

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
            device = new JsonObject();
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("family", deviceFamily);
            device.put("path", owDevice);
            device.put("typeInfo", DeviceDatabase.getDeviceTypeInfo(deviceType));

            deviceLookup.put(deviceId, device);

            logger.info("New device found during scan of Owserver at {}:{}. Id: {}, type: {}, family: {}.", this.host, this.port, deviceId, deviceType, deviceFamily);
            //TODO: Broadcast new device.
          } else {
            logger.info("Found unsupported device type for device with id '{}' on Owserver at {}:{}. Device will be ignored! Please notify developers and provide: type={}, family={}.", deviceId, this.host, this.port, deviceType, deviceFamily);
          }
        }
      }

      // Remove all devices in device list that was no longer found during this scan. They has been disconnected from the 1-wire bus.
      Set<String> tempSet = new HashSet(deviceLookup.keySet());
      tempSet.removeAll(foundDeviceIds);
      for (String removeId : tempSet) {
        deviceLookup.remove(removeId);
        logger.info("Device with id: {}, not found after last scan of Owserver at {}:{}. Device has been removed from bus.", removeId, this.host, this.port);
        //TODO: Broadcast delete device
      }

    } catch (SocketException ex) {
      logger.warn("Failed to scan Owserver at {}:{} for available devices, connection seems down. Trying to reconnect.", this.host, this.port, ex);
      try {
        setupOwfsConnection();
      } catch (Exception ex2) {
        // Ignore.
      }
    } catch (OwfsException ex) {
      logger.error("Error while trying to scan Owserver at {}:{} for available devices. Received errorcode: {}", this.host, this.port, ex.getErrorCode(), ex);
    } catch (IOException ex) {
      logger.error("Error while trying to scan Owserver at {}:{} for available devices.", this.host, this.port, ex);
    }
  }

  /**
   * Returns a list of all available devices on 1-wire bus.
   *
   * @return list of devices.
   */
  private JsonArray getAvailableDevices() {
    JsonArray result = new JsonArray();

    List<JsonObject> deviceList = deviceLookup.values().stream().sorted((d1, d2) -> d1.getString("type").compareTo(d2.getString("type"))).collect(Collectors.toList());

    for (JsonObject device : deviceList) {
      result.add(
          new JsonObject()
          .put("id", device.getString("id"))
          .put("type", device.getString("type"))
          .put("typeInfo", device.getJsonObject("typeInfo"))
      );
    }

    return result;
  }

  /**
   * Read value from having device having specified id.
   *
   * @param deviceId Id on device
   * @return device current value.
   * @throws DeviceMissingException throws exception if specified device does not exist.
   */
  private String getDeviceValue(String deviceId) throws DeviceMissingException, IOException, OwfsException {

    if (deviceId == null || !deviceLookup.containsKey(deviceId)) {
      throw new DeviceMissingException("Trying to perform a action on a non existing device.", deviceId);
    }

    String value = null;
    JsonObject device = deviceLookup.get(deviceId);
    JsonObject typeInfo = device.getJsonObject("typeInfo");

    // Check if this type of device is readable.
    if (typeInfo.containsKey("valueReadPath")) {
      String path = device.getString("path") + typeInfo.getString("valueReadPath");
      value = owfs.read(path).trim();
    }

    return value;
  }

  /**
   * Set value on device having specified id.
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
      owfs.write(path, value);
    }
  }
}
