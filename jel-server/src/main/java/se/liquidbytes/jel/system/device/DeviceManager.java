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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static se.liquidbytes.jel.system.JelService.EVENTBUS;

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
  public List<? extends Device> getSiteDevices(String siteId) {
    List<? extends Device> devices = new ArrayList<>();

    if (siteDevices.containsKey(siteId)) {
      devices = siteDevices.get(siteId);
    }

    return devices;
  }
}
