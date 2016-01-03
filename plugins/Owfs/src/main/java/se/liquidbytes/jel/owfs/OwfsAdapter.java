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
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import static java.util.Comparator.comparing;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
   * Delay between polling 1-wire bus for available devices (milliseconds).
   */
  private final static int POLL_PRESENCE_DELAY = 15000;
  /**
   * Delay between polling 1-wire bus for value-changes (milliseconds). This is the delay we are aiming for, it may take longer as a slow bus with lot's of slow
   * devices may delay a full bus-poll for many many seconds. And there is not that much we could do about it here.
   */
  private final static int POLL_VALUE_DELAY = 4000;
  /**
   * Character that separates a parent id from a child id. (Must be URL compatible)
   */
  private final static String CHILDSEPARATOR = "_";
  /**
   * Size of threadpools, should always be 1.
   */
  private final static int THREAD_POOL_SIZE = 1;
  /**
   * Host-setting for Owfs
   */
  private String host;
  /**
   * Port-setting for Owfs
   */
  private int port;

  // Below we use several connections to owserver, this is to allow to send simultaneous request to owserver , e.g. set a value on a device at the same time we are scanning the bus for new devices. For some harwarecombinations this seems possible.
  /**
   * Connection used for polling 1-wire bus for new/departed devices.
   */
  private OwServerConnection presenceConnection;
  /**
   * Connection used for polling devices on 1-wire bus for new readings(samples).
   */
  private OwServerConnection pollDevicesConnection;
  /**
   * Connection used for setting values on devices on 1-wire bus.
   */
  private OwServerConnection setDeviceValueConnection;

  /**
   * Device Id to device lookup table.
   */
  private Map<String, JsonObject> deviceLookup;
  /**
   * Device read values.
   */
  private Map<String, JsonObject> deviceReadings;
  /**
   * Timestamp when presence of devices was run.
   */
  private long lastPresenceRun;
  /**
   * Timestamp when last sampling of devices values was run.
   */
  private long lastDeviceSamplingRun;
  /**
   * Thread pool (with one thread) used for running the thread that polls for present devices.
   */
  private ScheduledThreadPoolExecutor pollPresenceExecutor;
  /**
   * Thread pool (with one thread) used for running the thread that poll for device value changes.
   */
  private ScheduledThreadPoolExecutor pollDevicesValueExecutor;

  /**
   * Start method for adapter, will be called upon when adapter is expected to start up
   */
  @Override
  public void start() {
    deviceLookup = new ConcurrentHashMap<>();
    deviceReadings = new ConcurrentHashMap<>();
    this.setId(context.config().getString("adapterId"));

    pollPresenceExecutor = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
    pollPresenceExecutor.setThreadFactory((Runnable r) -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("owfsAdapter-presence-" + thread.getName());
      return thread;
    });
    pollPresenceExecutor.setMaximumPoolSize(THREAD_POOL_SIZE);

    pollDevicesValueExecutor = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
    pollDevicesValueExecutor.setThreadFactory((Runnable r) -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("owfsAdapter-devicesvalues" + thread.getName());
      return thread;
    });
    pollDevicesValueExecutor.setMaximumPoolSize(THREAD_POOL_SIZE);

    setupOwfsConnections();

    // Register this adapter to the eventbus so we could take requests and send notifications.
    // We register this adapter serveral times at different addresses on the eventbus, this is because there could be several instances of the adapter running on different IP-addresses and ports,
    // and we may want to send a command to a specific instance, to all instances of an adapter-type, and to ALL adapters.
    EventBus eb = vertx.eventBus();
    MessageConsumer<String> consumer;
    consumer = eb.consumer(String.format("%s.%s", EVENTBUS_ADAPTERS, "_all"));
    consumer.handler(message -> {
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s", EVENTBUS_ADAPTERS, "owfs"));
    consumer.handler(message -> {
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s@%s:%d", EVENTBUS_ADAPTERS, "owfs", this.host, this.port));
    consumer.handler(message -> {
      handleRequest(message);
    });

    scanAvailableDevices();
    lastPresenceRun = lastDeviceSamplingRun = System.currentTimeMillis();
    pollPresenceExecutor.scheduleAtFixedRate(pollPresenceTask(), POLL_PRESENCE_DELAY, POLL_PRESENCE_DELAY, TimeUnit.MILLISECONDS);
    pollDevicesValueExecutor.scheduleAtFixedRate(pollDevicesValueTask(), 100, POLL_VALUE_DELAY, TimeUnit.MILLISECONDS);

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

    logger.info("Owfs-adapter for owserver running at {}:{} is shutting down.", this.host, this.port);

    pollPresenceExecutor.shutdown();
    pollDevicesValueExecutor.shutdown();

    try {
      pollPresenceExecutor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // Do nothing.
    } finally {
      pollPresenceExecutor.shutdownNow();
      logger.debug("PollPresence-threadpool for Owfs-adapter for owserver running at {}:{} is now shutdown.", this.host, this.port);
      pollPresenceExecutor = null;
    }

    try {
      pollDevicesValueExecutor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // Do nothing.
    } finally {
      pollDevicesValueExecutor.shutdownNow();
      logger.debug("PollDevicesValue-threadpool for Owfs-adapter for owserver running at {}:{} is now shutdown.", this.host, this.port);
      pollDevicesValueExecutor = null;
    }

    if (deviceLookup != null) {
      deviceLookup.clear();
      deviceLookup = null;
    }

    if (deviceReadings != null) {
      deviceReadings.clear();
      deviceReadings = null;
    }

    if (this.presenceConnection != null) {
      this.presenceConnection.close();
      this.presenceConnection = null;
    }
    if (this.pollDevicesConnection != null) {
      this.pollDevicesConnection.close();
      this.pollDevicesConnection = null;
    }
    if (this.setDeviceValueConnection != null) {
      this.setDeviceValueConnection.close();
      this.setDeviceValueConnection = null;
    }
  }

  /**
   * Get human-readable description about adapter.
   *
   * @return
   */
  @Override
  public String getDescription() {
    return "Adapter for supporting Dallas/Maxim 1-wire system using the Open Source library OWFS, http://owfs.org/";
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
    } catch (OwServerConnectionException ex) {
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
   * Setup the connections to the Owserver.
   */
  private void setupOwfsConnections() {
    try {
      this.host = context.config().getString("address", "127.0.0.1");
      this.port = context.config().getInteger("port", 4304);
      this.presenceConnection = new OwServerConnection(this.host, this.port);
      this.presenceConnection.connect();

      this.pollDevicesConnection = new OwServerConnection(this.host, this.port);
      this.pollDevicesConnection.connect();

      this.setDeviceValueConnection = new OwServerConnection(this.host, this.port);
      this.setDeviceValueConnection.connect();
    } catch (Exception ex) {
      logger.error("Error while trying to setup Owserver-connection to {}:{} for adapter with id \"{}\".", this.host, this.port, this.getId(), ex);
      throw ex;
    }
  }

  /**
   * Internal running thread for polling onewire bus for presence of devices.
   *
   * @return Task to run in threadpool.
   */
  private Runnable pollPresenceTask() {

    Runnable task = () -> {

      long currentTime = System.currentTimeMillis();
      long presenceDelayOverdue = currentTime - lastPresenceRun - POLL_PRESENCE_DELAY;

      if (presenceDelayOverdue >= POLL_PRESENCE_DELAY) {
        logger.warn("Onewire adapter has been delayed({} ms) for too long between presence-detection runs on Owserver with id \"{}\" running at {}:{}. This could be the result of too many devices on a single Onewire adapter, or many slow parasitic powered devices.", presenceDelayOverdue, this.getId(), this.host, this.port);
      }

      lastPresenceRun = currentTime;
      scanAvailableDevices();
    };

    return task;
  }

  /**
   * Internal running thread for polling onewire bus for device-value changes..
   *
   * @return Task to run in threadpool.
   */
  private Runnable pollDevicesValueTask() {

    Runnable task = () -> {

      long currentTime = System.currentTimeMillis();
      long sampleDelayOverdue = currentTime - lastDeviceSamplingRun - POLL_VALUE_DELAY;

      if (sampleDelayOverdue >= POLL_VALUE_DELAY) {
        logger.warn("Onewire adapter has been delayed({} ms) for too long between samples runs on Owserver with id \"{}\" running at {}:{}. This could be the result of too many devices on a single Onewire adapter, or many slow parasitic powered devices.", sampleDelayOverdue, this.getId(), this.host, this.port);
      }

      lastDeviceSamplingRun = currentTime;
      simultaneousReadValues();
    };

    return task;
  }

  /**
   * Scan for available devices on 1-wire bus. And updates our internal list of available devices. Note this is a blocking call!
   *
   */
  private void scanAvailableDevices() {
    try {
      String deviceId, deviceType, deviceFamily, isPowered;
      Set<String> foundDeviceIds = new HashSet<>();
      EventBus eb = vertx.eventBus();
      JsonObject device, childDevice, broadcastDevice;

      Instant startExecutionTime = Instant.now();
      logger.debug("Scanning for available devices on Owserver at {}:{} with id \"{}\".", this.host, this.port, this.getId());
      List<String> owDevices = presenceConnection.listDirectory(false);
      logger.debug("Found {} devices on Owserver at {}:{} with id \"{}\".", owDevices.size(), this.host, this.port, this.getId());

      for (String owDevice : owDevices) {
        deviceId = presenceConnection.read(owDevice + "/id");
        deviceType = presenceConnection.read(owDevice + "/type");
        deviceFamily = presenceConnection.read(owDevice + "/family");

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

                presenceConnection.write(path, command.getString("value"));
                // TODO: After a hardware reset we need to rerun this procedure, is there a way for us to detect a hardware reset (owfs statistics?)?
              }
            }

            try {
              isPowered = presenceConnection.read(owDevice + "/power");
              if (isPowered != null && isPowered.equals("0")) {
                logger.warn("Device '{}' of type '{}' on Owserver at {}:{} with id \"{}\" is running on parasitic power, this will slow down the 1-wire network and is less reliable than a powered device.", deviceId, deviceType, this.host, this.port, this.getId());
              }
            } catch (OwServerConnectionException ex) {
              // Ignore. Devices that don't support the power-property will throw an error, so we just ignore this device.
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

            // Check if this device is an container for other "child-devices". In that case, add all the children too, their Id will be <parent_childnumber>.
            if (typeInfo.containsKey("childDevices")) {
              for (Iterator it = typeInfo.getJsonArray("childDevices").iterator(); it.hasNext();) {
                JsonObject childType = (JsonObject) it.next();
                String childId = String.format("%s%s%s", deviceId, CHILDSEPARATOR, childType.getString("idSuffix"));

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
        if (!removeId.contains(CHILDSEPARATOR)) {
          // If device has children, remove these first.
          List<String> childDevicesId = tempSet.stream().filter(d -> d.startsWith(removeId + CHILDSEPARATOR)).collect(Collectors.toList());
          for (String childDeviceId : childDevicesId) {
            removeDeviceFromLookup(childDeviceId, eb);
          }
          // Then remove device.
          removeDeviceFromLookup(removeId, eb);
        }
      }

      logger.debug("Scanning bus for devices took {}ms on Owserver at {}:{} with id \"{}\".", Duration.between(startExecutionTime, Instant.now()).toMillis(), this.host, this.port, this.getId());
    } catch (OwServerConnectionException ex) {
      logger.error("Error while trying to scan Owserver at {}:{} with id \"{}\" for available devices.", this.host, this.port, this.getId(), ex);
    }
  }

  /**
   * Get only parent devices from the deviceLookup-
   *
   * @return List of "parent" devices.
   */
  private List<JsonObject> getParentDevicesOnly() {
    return deviceLookup.values().stream().filter(d -> !d.getString("id").contains(CHILDSEPARATOR)).collect(Collectors.toList());
  }

  /**
   * Get child devices for a parent device, empty list if no ones exists.
   *
   * @return List of child devices.
   */
  private List<JsonObject> getChildDevicesOnly(String parentId) {
    return deviceLookup.values().stream().filter(d -> d.getString("id").contains(parentId + CHILDSEPARATOR))
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
      String value = this.pollDevicesConnection.read(path).trim();

      return value;
    } catch (DeviceMissingException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s', device is reported missing.", ex.getDeviceId()), ex);
    } catch (OwServerConnectionException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s'.", id), ex);
    }
  }

  /**
   * Read current values from all active devices simultaneously.
   */
  private void simultaneousReadValues() {
    List<JsonObject> devices = getParentDevicesOnly();

    String id = null;
    String time;
    String value;
    JsonObject reading;
    JsonObject readings;
    Instant startExecutionTime = Instant.now();

    try {
      /*
                Ok so what is all this you say?
                We have just fetched a list of all our 1-wire devices and are just about to iterate thought them all to collect their current values, but there is something we may want to do first...
                Most 1-wire devices are fairly fast to read from, the exception is temperature sensors and A/D-converters. The common used DS18S20 temperature sensor takes about 700 ms for each reading,
                normally they are all read by OWFS in a serial fassion which means that we query a sensor for its current readings, the query is blocked for the conversiontime of 700 ms and then we receive the result.
                For a 1-wire network of nineteen DS18S20 sensors a full reading of all devices will take at least 19x700 ms, which is quite a long time if you expect fast visual feedback when temperature changes rapidly.
                To ease this, the 1-wire protocol support the "Skip ROM" command which enable OWFS to start the conversion of ALL termperature sensors (and A/D-converters a.k.a DS2450) at the same time.
                We then need to wait about 700 ms to let the conversion take place and then we can iterate throught all sensors and collect all readings, this makes this a more O(1) operation than a O(n), in best cases at least.

                So what's the catch, well we have to write to "/simultaneous/temperature" and "/simultaneous/voltage" each time we want to start initiate a conversion if there exists any temperature sensors or A/D  converters on the 1-wire bus.
                For the simultaneous reading to work all temperature sensors NEED to be powered, having the Vcc, Data, and GND-lines connected. OWFS will scan the bus and if any temperature sensors a running in "parasitic"-mode then all reading will happen in serial (take many seconds).
                A fairly new version of OWFS is needed to be installed for this to work.
       */
      // Simultaneous conversion: http://owfs-developers.1086194.n5.nabble.com/Missing-data-td10904i20.html

      List<String> simultaneousPaths = devices.stream().map(d -> d.getJsonObject("typeInfo").getString("simultaneousPath")).filter(d -> d != null && !d.isEmpty()).distinct().collect(Collectors.toList());

      for (String path : simultaneousPaths) {
        this.pollDevicesConnection.write(path, "1");
      }
    } catch (OwServerConnectionException ex) {
      logger.warn("Failed to initiate simultaneous readings of devices on Owserver at {}:{} with adapter id \"{}\".", this.host, this.port, this.getId(), ex);
    }

    for (JsonObject device : devices) {
      try {
        id = device.getString("id");

        // Do a blocking reading.
        value = readValue(device);
        time = LocalDateTime.now().toString();
        logger.trace("Read value '{}' from device with id '{}' on Owserver at {}:{} with adapter id \"{}\".", value, id, this.host, this.port, this.getId());

        reading = new JsonObject()
            .put("id", id)
            .put("value", value)
            .put("time", time);

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
          logger.debug("Recorded new value '{}' at time '{}' for device with id '{}' on Owserver at {}:{} with id \"{}\".", value, time, id, this.host, this.port, this.getId());

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
      } catch (Exception ex) {
        logger.error("Failed to poll device '{}' for value on Owserver at {}:{} with adapter id \"{}\".", id, this.host, this.port, this.getId());
      }
    }

    logger.debug("Reading all current device values took {}ms.", Duration.between(startExecutionTime, Instant.now()).toMillis());
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
  private void getDeviceValue(Message message) throws DeviceMissingException {
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
  private JsonObject getDeviceValue(String deviceId) throws DeviceMissingException {

    if (deviceId == null || !deviceLookup.containsKey(deviceId)) {
      throw new DeviceMissingException("Trying to perform a action on a non existing device.", deviceId);
    }

    // Check if child device.
    if (!deviceId.contains(CHILDSEPARATOR)) {
      JsonObject reading = this.deviceReadings.get(deviceId);

      JsonObject response = new JsonObject()
          .put("reading", reading);

      return response;
    } else {
      JsonObject reading = this.deviceReadings.get(deviceId.split(CHILDSEPARATOR)[0]); // Get value from parent.
      int index = Integer.parseInt(deviceId.split(CHILDSEPARATOR)[1]) - 1;         // Get child idsuffix.
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
   * @throws OwServerConnectionException throws exception if command fails for any reason.
   */
  private void setDeviceValue(Message message) throws DeviceMissingException, OwServerConnectionException {
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
   * @throws OwServerConnectionException throws exception if command fails for any reason.
   */
  private void setDeviceValue(String deviceId, String value) throws DeviceMissingException, OwServerConnectionException {

    if (deviceId == null || !deviceLookup.containsKey(deviceId)) {
      throw new DeviceMissingException("Trying to perform a action on a non existing device.", deviceId);
    }

    JsonObject device = deviceLookup.get(deviceId);
    JsonObject typeInfo = device.getJsonObject("typeInfo");

    // Check if this type of device is writable.
    if (typeInfo.containsKey("valueWritePath")) {
      String path = device.getString("path") + typeInfo.getString("valueWritePath");

      logger.debug("Write value {} to device '{}'.", value, deviceId);

      this.setDeviceValueConnection.write(path, value);
    }
  }
}
