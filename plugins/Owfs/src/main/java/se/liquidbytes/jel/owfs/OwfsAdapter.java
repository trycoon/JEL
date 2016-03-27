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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import static java.util.Comparator.comparing;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.owfs.jowfsclient.OwfsException;
import org.owfs.jowfsclient.alarm.AlarmingDevicesScanner;
import org.owfs.jowfsclient.device.SwitchAlarmingDeviceEvent;
import org.owfs.jowfsclient.device.SwitchAlarmingDeviceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.adapter.AbstractAdapter;
import se.liquidbytes.jel.system.adapter.AdapterEvents;
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
  private final static int POLL_PRESENCE_DELAY = 60000;
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

  /**
   * Queue of commands to execute on Owserver.
   */
  private BlockingQueue<JsonObject> commandQueue;
  /**
   * Connection used for communicating with Owserver.
   */
  private OwServerConnection owserverConnection;
  /**
   * Device Id to device lookup table.
   */
  private Map<String, JsonObject> deviceLookup;
  /**
   * Device read values.
   */
  private Map<String, JsonObject> deviceReadings;
  /**
   * Timestamp when last scan of 1-wire bus for devices was run.
   */
  private long lastBusScanRun;
  /**
   * Thread pool (with one thread) used for running the main loop that detect and polls devices.
   */
  private ScheduledThreadPoolExecutor mainloopExecutor;
  /**
   * Counter to sum up the time we spent waiting on executing queued commmands to Owserver.
   */
  private Duration commandsWrittenDuration;

  /**
   * Start method for adapter, will be called upon when adapter is expected to start up
   */
  @Override
  public void start() {
    deviceLookup = new ConcurrentHashMap<>();
    deviceReadings = new ConcurrentHashMap<>();
    this.setId(context.config().getString("adapterId"));

    commandQueue = new LinkedBlockingQueue<>();

    mainloopExecutor = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
    mainloopExecutor.setThreadFactory((Runnable r) -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("owfsAdapter-mainloop-" + this.getId());
      return thread;
    });
    mainloopExecutor.setMaximumPoolSize(THREAD_POOL_SIZE);

    setupOwfsConnections();

    // Register this adapter to the eventbus so we could take requests and send notifications.
    // We register this adapter serveral times at different addresses on the eventbus, this is because there could be several instances of the adapter running on different IP-addresses and ports,
    // and we may want to send a command to a specific instance, to all instances of an adapter-type, and to ALL adapters.
    EventBus eb = vertx.eventBus();
    MessageConsumer<String> consumer;
    consumer = eb.consumer(String.format("%s.%s", AdapterEvents.EVENTBUS_ADAPTERS, "_all"));
    consumer.handler(message -> {
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s", AdapterEvents.EVENTBUS_ADAPTERS, "owfs"));
    consumer.handler(message -> {
      handleRequest(message);
    });
    consumer = eb.consumer(String.format("%s.%s@%s:%d", AdapterEvents.EVENTBUS_ADAPTERS, "owfs", this.host, this.port));
    consumer.handler(message -> {
      handleRequest(message);
    });

    logger.info("Owserver version \"{}\" running at {}:{}.", owserverConnection.read("/system/configuration/version"), this.host, this.port);

    // Get initial list af devices.
    scanAvailableDevices();

    lastBusScanRun = System.currentTimeMillis();
    mainloopExecutor.scheduleAtFixedRate(mainloopTask(), 1000, 50, TimeUnit.MILLISECONDS);

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

    mainloopExecutor.shutdown();

    try {
      mainloopExecutor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      // Do nothing.
    } finally {
      mainloopExecutor.shutdownNow();
      logger.debug("mainloopExecutor-threadpool for Owfs-adapter for owserver running at {}:{} is now shutdown.", this.host, this.port);
      mainloopExecutor = null;
    }

    if (this.owserverConnection != null) {
      this.owserverConnection.close();
      this.owserverConnection = null;
    }

    if (deviceLookup != null) {
      deviceLookup.clear();
      deviceLookup = null;
    }

    if (deviceReadings != null) {
      deviceReadings.clear();
      deviceReadings = null;
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
      this.owserverConnection = new OwServerConnection(this.host, this.port);
      this.owserverConnection.connect();
    } catch (Exception ex) {
      logger.error("Error while trying to setup Owserver-connection to {}:{} for adapter with id \"{}\".", this.host, this.port, this.getId(), ex);
      throw ex;
    }
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
      List<String> owDevices = owserverConnection.listDirectory(true);
      logger.debug("Found {} devices on Owserver at {}:{} with id \"{}\".", owDevices.size(), this.host, this.port, this.getId());

      for (String owDevice : owDevices) {
        deviceId = owserverConnection.read(owDevice + "/id");
        deviceType = owserverConnection.read(owDevice + "/type");
        deviceFamily = owserverConnection.read(owDevice + "/family");

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

                owserverConnection.write(path, command.getString("value"));
              }
            }

            try {
              isPowered = owserverConnection.read(owDevice + "/power");
              if (isPowered != null && isPowered.equals("0")) {
                logger.warn("Device '{}' of type '{}' on Owserver at {}:{} with id \"{}\" is running on parasitic power, this will slow down the 1-wire network and is less reliable than a powered device.", deviceId, deviceType, this.host, this.port, this.getId());
              }
            } catch (OwServerConnectionException ex) {
              // Ignore. Devices that don't support the power-property will throw an error, so we just ignore this.
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

            // For devices that supports it.
            //setupAlarmHandler(device);
            broadcastDevice = new JsonObject()
                .put("adapterId", this.getId())
                .put("port", this.port)
                .put("host", this.host)
                .put("deviceId", deviceId)
                .put("type", deviceType)
                .put("name", typeInfo.getString("name"));
            eb.publish(AdapterEvents.EVENTBUS_ADAPTERS, broadcastDevice, new DeliveryOptions().addHeader("action", AdapterEvents.EVENT_DEVICES_ADDED));

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
                eb.publish(AdapterEvents.EVENTBUS_ADAPTERS, broadcastDevice, new DeliveryOptions().addHeader("action", AdapterEvents.EVENT_DEVICES_ADDED));
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
   * Add alarm monitoring for device.
   *
   * @param device device to add alarm monitor for. Gets added only if device supports it.
   */
  private void setupAlarmHandler(JsonObject device) {

    String alarmingMask = device.getJsonObject("typeInfo").getString("alarmingMask");

    /* We might see these exceptions in the log:
            "org.owfs.jowfsclient.alarm.AlarmingDevicesReader - Exception occured java.lang.NullPointerException"
            The problem is that line #11, "if (devicePath.endsWith(SLASH)) {", in file OWFSUtils of the JOwfsClient-library is feed with a null devicePath.
            Probably Owserver delivers one or more nulls in the collection of alarming devices due to some sort of timeout.
     */
    if (alarmingMask != null && !alarmingMask.isEmpty()) {
      AlarmingDevicesScanner alarmingDevicesScanner = owserverConnection.getAlarmingDevicesScanner();

      SwitchAlarmingDeviceListener alarmingDeviceHandler = new SwitchAlarmingDeviceListener(
          device.getString("path"),
          alarmingMask
      ) {
        @Override
        public void handleAlarm(SwitchAlarmingDeviceEvent event) {
          logger.info("Alarm '" + getDeviceName() + "' : latch:;" + event.latchStatus + "', sensed:'" + event.sensedStatus + "'");
        }
      };

      try {
        alarmingDevicesScanner.addAlarmingDeviceHandler(alarmingDeviceHandler);
      } catch (OwfsException ex) {
        logger.error("Failed to setup alarm handler on device \"{}\" on Owserver running at {}:{}, got errorcode: {}.", device.getString("path"), this.host, this.port, ex.getErrorCode(), ex);
      } catch (IOException ex) {
        logger.error("Failed to setup alarm handler on device \"{}\" on Owserver running at {}:{}.", device.getString("path"), this.host, this.port, ex);
      }
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

    eb.publish(AdapterEvents.EVENTBUS_ADAPTERS, broadcastDevice, new DeliveryOptions().addHeader("action", AdapterEvents.EVENT_DEVICES_REMOVED));
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
      String value = this.owserverConnection.read(path).trim();

      return value;
    } catch (DeviceMissingException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s', device is reported missing.", ex.getDeviceId()), ex);
    } catch (OwServerConnectionException ex) {
      throw new PluginException(String.format("Failed to read value from device with id '%s'.", id), ex);
    }
  }

  /**
   * Execute all possible queued commands to be written to Owserver.
   */
  private void executeQueuedCommands() {
    JsonObject command;
    String path, value;

    while (commandQueue.size() > 0) {
      try {
        command = commandQueue.poll();
        path = command.getString("path");
        value = command.getString("value");

        logger.debug("Write value {} to device '{}'.", value, path);

        owserverConnection.write(path, value);
      } catch (OwServerConnectionException ex) {
        logger.error("Failed to execute queued command.", ex);
      }
    }
  }

  /**
   * Collect all readings from a list of devices. (result is saved in deviceReadings)
   *
   * @param devices List of devices to read from.
   */
  private void collectDevicesReadings(List<JsonObject> devices) {
    String id = null;
    String time;
    String value;
    JsonObject reading;
    JsonObject readings;
    Instant commandsWrittenTime;

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

            vertx.eventBus().publish(AdapterEvents.EVENTBUS_ADAPTERS, broadcast, new DeliveryOptions().addHeader("action", AdapterEvents.EVENT_DEVICE_NEWREADING));
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

                vertx.eventBus().publish(AdapterEvents.EVENTBUS_ADAPTERS, broadcast, new DeliveryOptions().addHeader("action", AdapterEvents.EVENT_DEVICE_NEWREADING));
              }
            }
          }
        }
      } catch (Exception ex) {
        logger.error("Failed to poll device '{}' for value on Owserver at {}:{} with adapter id \"{}\".", id, this.host, this.port, this.getId());
      }

      // Execute possible queued commands between each iteration. This is necessary since a reading from several parasitic devices could take many seconds to complete.
      commandsWrittenTime = Instant.now();
      executeQueuedCommands();
      commandsWrittenDuration.plus(Duration.between(commandsWrittenTime, Instant.now()));
    }
  }

  /**
   * Internal running thread for executing commands and polling 1-wire bus.
   *
   * @return Task to run in threadpool.
   */
  private Runnable mainloopTask() {

    Runnable task = () -> {

      /*
                Ok so what is all this you say?
                We have just fetched a list of all our 1-wire devices and are just about to iterate thought them all to collect their current values, but there is something we may want to do first...
                Most 1-wire devices are fairly fast to read from, the exception is temperature sensors and A/D-converters. The common used DS18S20 temperature sensor takes about 700 ms for each reading,
                normally they are all read by OWFS in a serial fassion which means that we query a sensor for its current readings, the query is blocked for the conversiontime of 700 ms and then we receive the result.
                For a 1-wire network of nineteen DS18S20 sensors a full reading of all devices will take at least 19x700 ms, which is quite a long time to hog the 1-wire bus if you have other queued operations to execute on the bus.
                To ease this, the 1-wire protocol support the "Skip ROM" command which enable OWFS to start the conversion of ALL termperature sensors (and A/D-converters a.k.a DS2450) at the same time.
                We then need to wait about 700 ms to let the conversion take place and after that we can iterate throught all sensors and collect all readings very fast, this makes this a more O(1) operation than a O(n), in best cases at least.

                So what's the catch, well we have to write to "/simultaneous/temperature" and "/simultaneous/voltage" each time we want to start initiate a conversion if there exists any temperature sensors or A/D  converters on the 1-wire bus.
                For the simultaneous reading to work all temperature sensors NEED to be powered, having the Vcc, Data, and GND-lines connected. OWFS will scan the bus and if ANY temperature sensors are running in "parasitic"-mode then ALL reading will happen in serial (take many seconds).
                A fairly new version of OWFS is needed to be installed for this to work. See: http://owfs-developers.1086194.n5.nabble.com/Missing-data-td10904i20.html

                This is the mainloop that constantly polls for devicereadings and execute queued commands on the 1-write bus.
                We first get all our devices that we have collected from busscans during earlier runs, then we separate them into different lists, one for slow temperature-sensors, one for slow voltage-sensors, and a list of other devices that we concider fast.
                We start by sending any queued commands to the bus, that is commands that change a pin or state on a device, this is a farily fast operation and this is usually a operation where users expect fast feedback/low delay.
                Then we trigger the start of conversion for thos slow temperature and voltage sensors, if we have any, then we read all fast devices while the slow ones doing their work.
                By now hopefully the slow devices have finished and are ready to be read, so we read all slow devices.
                Note that between many of the steps in the main loop we have put in checks for possible queued command that we should execute to get a fast responsetime.

                Even now and then we scan the bus for new/removed devices, a busscan is a slow fragile operation that we don't want to execute too often.
       */
      List<JsonObject> allDevices, temperatureDevices, voltageDevices, fastDevices;
      Instant startExecutionTime, stepTime;
      Duration simultaneousWrittenDuration, fastDevicesReadDuration, temperatureDevicesDuration, voltageDevicesDuration, busScanDuration;

      startExecutionTime = Instant.now();
      commandsWrittenDuration = Duration.ZERO;
      busScanDuration = Duration.ZERO;

      try {
        allDevices = getParentDevicesOnly();
        temperatureDevices = allDevices.stream().filter(d -> d.getJsonObject("typeInfo").containsKey("temperatureSensor") && d.getJsonObject("typeInfo").getBoolean("temperatureSensor")).collect(Collectors.toList());
        voltageDevices = allDevices.stream().filter(d -> d.getJsonObject("typeInfo").containsKey("voltageSensor") && d.getJsonObject("typeInfo").getBoolean("voltageSensor")).collect(Collectors.toList());

        fastDevices = allDevices.stream().filter(d -> !d.getJsonObject("typeInfo").containsKey("alarmingMask")).collect(Collectors.toList()); // We exclude devices that read their values using a alarm handler from this list.
        fastDevices.removeAll(temperatureDevices);
        fastDevices.removeAll(voltageDevices);
      } catch (Exception ex) {
        logger.error("Failed to parse list of devices in mainloop. Adapter will not function properly!", ex);
        throw ex;
      }

      stepTime = Instant.now();
      try {
        if (temperatureDevices.size() > 0) {
          // If temperature sensors exists, start a simultaneous conversion on all of them.
          owserverConnection.write("/simultaneous/temperature", "1");
        }

        if (voltageDevices.size() > 0) {
          // If voltage sensors exists, start a simultaneous conversion on all of them.
          owserverConnection.write("/simultaneous/voltage", "1");
        }
      } catch (OwServerConnectionException ex) {
        logger.warn("Failed to initiate simultaneous readings of devices on Owserver at {}:{} with adapter id \"{}\". This may slow down adapter readings alot!", this.host, this.port, this.getId(), ex);
      }
      simultaneousWrittenDuration = Duration.between(stepTime, Instant.now());

      // Execute possible queued commands.
      Instant commandsWrittenTime = Instant.now();
      executeQueuedCommands();
      commandsWrittenDuration.plus(Duration.between(commandsWrittenTime, Instant.now()));

      // Collect readings on all 1-wire devices that are quite fast
      stepTime = Instant.now();
      collectDevicesReadings(fastDevices);
      fastDevicesReadDuration = Duration.between(stepTime, Instant.now());

      // Make sure we wait 800 ms since we started conversion to be sure that sensors have sampled a new temperature.
      long timeLeftToConvert = 800 - Duration.between(startExecutionTime.plus(simultaneousWrittenDuration), Instant.now()).toMillis();
      if (timeLeftToConvert > 0) {
        logger.debug("Waiting additional {} miliseconds for conversion to finish on Owserver at {}:{} with adapter id \"{}\".", timeLeftToConvert, this.host, this.port, this.getId());
        try {
          Thread.sleep(timeLeftToConvert);
        } catch (InterruptedException ex) {
          // Ignore.
        }
      }

      // Collect readings on all temperature devices. Hopefully they are all done after the simultaneous conversion.
      stepTime = Instant.now();
      collectDevicesReadings(temperatureDevices);
      temperatureDevicesDuration = Duration.between(stepTime, Instant.now());

      // Collect readings on all voltage devices. Hopefully they are all done after the simultaneous conversion.
      stepTime = Instant.now();
      collectDevicesReadings(voltageDevices);
      voltageDevicesDuration = Duration.between(stepTime, Instant.now());

      if (System.currentTimeMillis() - lastBusScanRun > POLL_PRESENCE_DELAY) {
        lastBusScanRun = System.currentTimeMillis();
        stepTime = Instant.now();
        scanAvailableDevices();
        busScanDuration = Duration.between(stepTime, Instant.now());

        // Execute possible queued commands after a long bus scan.
        commandsWrittenTime = Instant.now();
        executeQueuedCommands();
        commandsWrittenDuration.plus(Duration.between(commandsWrittenTime, Instant.now()));
      }

      logger.debug("Mainloop execution statistics: total {}ms, command {}ms, simultaneous {}ms, fastdevices {}ms, temperaturedevices {}ms, voltagedevices {}ms, busscan {}ms.", Duration.between(startExecutionTime, Instant.now()), commandsWrittenDuration, simultaneousWrittenDuration, fastDevicesReadDuration, temperatureDevicesDuration, voltageDevicesDuration, busScanDuration);

      // If mainloop has run too fast, like when we have no devices connected, insert a artificial delay to not hog the CPU.
      if (Duration.between(startExecutionTime, Instant.now()).toMillis() < 100) {
        try {
          Thread.sleep(300);
        } catch (InterruptedException ex) {
          // Do nothing.
        }
      }
    };

    return task;
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

      JsonObject response;
      JsonObject reading = this.deviceReadings.get(deviceId.split(CHILDSEPARATOR)[0]); // Get value from parent.

      if (reading == null) {

        response = new JsonObject().put("reading", (JsonObject) null);

      } else {

        int index = Integer.parseInt(deviceId.split(CHILDSEPARATOR)[1]) - 1;         // Get child idsuffix.

        String value = reading.getJsonObject("lastReading").getString("value").split(",")[index];  // Get child reading from parent reading.

        response = new JsonObject()
            .put("reading", new JsonObject()
                .put("id", deviceId)
                .put("time", reading.getJsonObject("lastReading").getString("time"))
                .put("value", value)
            );
      }

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

    if (deviceId.contains(CHILDSEPARATOR)) {

      String parentId = deviceId.split(CHILDSEPARATOR)[0];
      String childSuffix = deviceId.split(CHILDSEPARATOR)[1];
      JsonObject parentDevice = deviceLookup.get(parentId);
      JsonObject typeInfo = parentDevice.getJsonObject("typeInfo");

      if (typeInfo == null) {
        throw new OwServerConnectionException(String.format("typeInfo missing for device with type '%s'", parentDevice.getString("type")));
      }

      JsonArray childDevices = typeInfo.getJsonArray("childDevices");
      if (childDevices != null && childDevices.size() > 0) {
        String writePath = childDevices.stream().filter(t -> t instanceof JsonObject).map(t -> (JsonObject) t).filter((d) -> d.getString("idSuffix").equals(childSuffix)).map((cd) -> cd.getString("valueWritePath")).findFirst().get();
        writePath = parentDevice.getString("path") + writePath;

        // Queue command. Record time so we could measure how long command has been queued before executed, if we want to.
        this.commandQueue.offer(new JsonObject().put("path", writePath).put("value", value).put("nanoTime", System.nanoTime()));
      }

    } else {

      JsonObject device = deviceLookup.get(deviceId);
      JsonObject typeInfo = device.getJsonObject("typeInfo");

      if (typeInfo == null) {
        throw new OwServerConnectionException(String.format("typeInfo missing for device with type '%s'", device.getString("type")));
      }
      // Check if this type of device is writable.
      if (typeInfo.containsKey("valueWritePath")) {
        String writePath = device.getString("path") + typeInfo.getString("valueWritePath");

        // Queue command. Record time so we could measure how long command has been queued before executed, if we want to.
        this.commandQueue.offer(new JsonObject().put("path", writePath).put("value", value).put("nanoTime", System.nanoTime()));
      }
    }
  }
}
