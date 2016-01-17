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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.owfs.jowfsclient.device.SwitchAlarmingDeviceListener;

/**
 * Class that contains a "database" of all supported devices an their features.
 *
 * @author Henrik Östman
 */
public final class DeviceDatabase {

  private static final Map<String, JsonObject> devices = createDatabase();
  private static final List<JsonObject> devicesList = createDeviceList();

  /**
   * Default constructor
   */
  private DeviceDatabase() {
    // Prevent instanses.
  }

  /**
   * Get information about a device type.
   *
   * @param typeId id of device type.
   * @return information about device type, or null if not suported.
   */
  public static JsonObject getDeviceTypeInfo(String typeId) {
    return devices.get(typeId);
  }

  /**
   * Returns a list of supported device types.
   *
   * @return
   */
  public static List<JsonObject> getSuportedDeviceTypes() {
    return Collections.unmodifiableList(devicesList);
  }

  /**
   * Creates a list of all supported device types. Should only be needed to run once.
   *
   * @return
   */
  private static List<JsonObject> createDeviceList() {
    return devices.values().stream().sorted(
        (d1, d2) -> d1.getString("typeId").compareTo(d2.getString("typeId"))
    ).collect(Collectors.toList());
  }

  /**
   * Create lookup table of supported device types. Should only be needed to run once.
   *
   * @return
   */
  private static Map<String, JsonObject> createDatabase() {
    Map<String, JsonObject> db = new ConcurrentHashMap<>();

    //--- TEMPERATURE DEVICES ---------------------------------------------------------------------
    JsonObject type = new JsonObject()
        .put("typeId", "DS18S20")
        .put("familyId", "10")
        .put("name", "DS18S20")
        .put("description", "High-Precision 1-Wire Digital Thermometer. It has an operating temperature range of -55°C to +125°C and is accurate to ±0.5°C over the range of -10°C to +85°C.")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS18S20.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "125")
        .put("minValue", "-55")
        .put("minSampleDelay", "1000")
        .put("temperatureSensor", true)
        .put("valueReadPath", "/temperature");
    db.put("DS18S20", type);

    type = new JsonObject()
        .put("typeId", "DS18B20")
        .put("familyId", "28")
        .put("name", "DS18B20")
        .put("description", "High-Precision 1-Wire Digital Thermometer. It has an operating temperature range of -55°C to +125°C and is accurate to ±0.5°C over the range of -10°C to +85°C.")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "125")
        .put("minValue", "-55")
        .put("minSampleDelay", "1000")
        .put("temperatureSensor", true)
        .put("valueReadPath", "/temperature");
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS1825")
        .put("familyId", "3B")
        .put("name", "DS1825")
        .put("description", "High-Precision 1-Wire Digital Thermometer. It has an operating temperature range of -55°C to +125°C and is accurate to ±0.5°C over the range of -10°C to +85°C.")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS1825.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "125")
        .put("minValue", "-55")
        .put("minSampleDelay", "1000")
        .put("temperatureSensor", true)
        .put("valueReadPath", "/temperature");
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS1920")
        .put("familyId", "10")
        .put("name", "DS1920")
        .put("description", "iButton 1-Wire Digital Thermometer.")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS1920.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "100")
        .put("minValue", "-55")
        .put("minSampleDelay", "300")
        .put("temperatureSensor", true)
        .put("valueReadPath", "/temperature");
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS1822")
        .put("familyId", "22")
        .put("name", "DS1822")
        .put("description", "1-Wire Digital Thermometer.")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS1822.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "100")
        .put("minValue", "-55")
        .put("minSampleDelay", "300")
        .put("temperatureSensor", true)
        .put("valueReadPath", "/temperature");
    db.put(type.getString("typeId"), type);

    //--- SWITCH DEVICES ---------------------------------------------------------------------
    type = new JsonObject()
        .put("typeId", "DS2408")
        .put("familyId", "29")
        .put("name", "DS2408")
        .put("description", "1-Wire 8 channel addressable switch")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2408.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "100")
        .put("valueReadPath", "/sensed.ALL")
        .put("valueWritePath", "/PIO.ALL")
        .put("alarmingMask", SwitchAlarmingDeviceListener.ALARMING_MASK_8_SWITCHES)
        .put("initCommands",
            new JsonArray().add(
                new JsonObject()
                .put("path", "/strobe") //TODO: This should accually only be run when using this device as an output.
                .put("value", "1")
            )
        )
        .put("childDevices",
            new JsonArray()
            .add(
                new JsonObject()
                .put("idSuffix", "1")
                .put("name", "port 1")
                .put("valueReadPath", "/sensed.0")
                .put("valueWritePath", "/PIO.0")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "2")
                .put("name", "port 2")
                .put("valueReadPath", "/sensed.1")
                .put("valueWritePath", "/PIO.1")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "3")
                .put("name", "port 3")
                .put("valueReadPath", "/sensed.2")
                .put("valueWritePath", "/PIO.2")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "4")
                .put("name", "port 4")
                .put("valueReadPath", "/sensed.3")
                .put("valueWritePath", "/PIO.3")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "5")
                .put("name", "port 5")
                .put("valueReadPath", "/sensed.4")
                .put("valueWritePath", "/PIO.4")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "6")
                .put("name", "port 6")
                .put("valueReadPath", "/sensed.5")
                .put("valueWritePath", "/PIO.5")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "7")
                .put("name", "port 7")
                .put("valueReadPath", "/sensed.6")
                .put("valueWritePath", "/PIO.6")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "8")
                .put("name", "port 8")
                .put("valueReadPath", "/sensed.7")
                .put("valueWritePath", "/PIO.7")
            )
        );
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS2406")
        .put("familyId", "12")
        .put("name", "DS2406")
        .put("description", "1-Wire dual-channel adressable switch with 1kbit Memory")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2406.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "100")
        .put("valueReadPath", "/sensed.ALL")
        .put("valueWritePath", "/PIO.ALL")
        .put("alarmingMask", SwitchAlarmingDeviceListener.ALARMING_MASK_2_SWITCHES)
        .put("childDevices",
            new JsonArray()
            .add(
                new JsonObject()
                .put("idSuffix", "1")
                .put("name", "port A")
                .put("valueReadPath", "/sensed.A")
                .put("valueWritePath", "/PIO.A")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "2")
                .put("name", "port B")
                .put("valueReadPath", "/sensed.B")
                .put("valueWritePath", "/PIO.B")
            )
        );
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS2413")
        .put("familyId", "3A")
        .put("name", "DS2413")
        .put("description", "1-Wire dual-channel adressable switch")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2413.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "100")
        .put("valueReadPath", "/sensed.ALL")
        .put("valueWritePath", "/PIO.ALL")
        .put("alarmingMask", SwitchAlarmingDeviceListener.ALARMING_MASK_2_SWITCHES)
        .put("childDevices",
            new JsonArray()
            .add(
                new JsonObject()
                .put("idSuffix", "1")
                .put("name", "port A")
                .put("valueReadPath", "/sensed.A")
                .put("valueWritePath", "/PIO.A")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "2")
                .put("name", "port B")
                .put("valueReadPath", "/sensed.B")
                .put("valueWritePath", "/PIO.B")
            )
        );
    db.put(type.getString("typeId"), type);

    //--- VOLTAGE DEVICES ---------------------------------------------------------------------
    type = new JsonObject()
        .put("typeId", "DS2450")
        .put("familyId", "20")
        .put("name", "DS2450")
        .put("description", "1-Wire Quad A/D Converter")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2450.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "100")
        .put("valueReadPath", "/volt.ALL")
        .put("childDevices",
            new JsonArray()
            .add(
                new JsonObject()
                .put("idSuffix", "1")
                .put("name", "voltage input A")
                .put("valueReadPath", "/volt.A")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "2")
                .put("name", "voltage input B")
                .put("valueReadPath", "/volt.B")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "3")
                .put("name", "voltage input C")
                .put("valueReadPath", "/volt.C")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "4")
                .put("name", "voltage input D")
                .put("valueReadPath", "/volt.D")
            )
        );
    db.put(type.getString("typeId"), type);
    //--- COUNTER DEVICES ---------------------------------------------------------------------
    type = new JsonObject()
        .put("typeId", "DS2423")
        .put("familyId", "1D")
        .put("name", "DS2423")
        .put("description", "4kbit 1-Wire RAM with Counter")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2423.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "100")
        .put("valueReadPath", "/counters.ALL")
        .put("childDevices",
            new JsonArray()
            .add(
                new JsonObject()
                .put("idSuffix", "1")
                .put("name", "counter input A")
                .put("valueReadPath", "/counters.A")
            )
            .add(
                new JsonObject()
                .put("idSuffix", "2")
                .put("name", "counter input B")
                .put("valueReadPath", "/counters.B")
            )
        );
    db.put(type.getString("typeId"), type);

    return db;
  }
}
