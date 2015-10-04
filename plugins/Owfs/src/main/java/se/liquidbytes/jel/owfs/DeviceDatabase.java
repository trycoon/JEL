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

import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
  public List<JsonObject> getSuportedDevicesTypes() {
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

    JsonObject type = new JsonObject()
        .put("typeId", "DS18S20")
        .put("familyId", "10")
        .put("name", "DS18S20")
        .put("description", "High-Precision 1-Wire Digital Thermometer")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS18S20.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "125")
        .put("minValue", "-55")
        .put("minSampleDelay", "1000")
        .put("valueReadPath", "/temperature");
    db.put("DS18S20", type);

    type = new JsonObject()
        .put("typeId", "DS18B20")
        .put("familyId", "28")
        .put("name", "DS18B20")
        .put("description", "High-Precision 1-Wire Digital Thermometer")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf")
        )
        .put("valueType", "number")
        .put("maxValue", "125")
        .put("minValue", "-55")
        .put("minSampleDelay", "1000")
        .put("valueReadPath", "/temperature");
    db.put(type.getString("typeId"), type);

    type = new JsonObject()
        .put("typeId", "DS2408")
        .put("familyId", "29")
        .put("name", "DS2408")
        .put("description", "1-Wire 8 Channel Addressable Switch")
        .put("manufacturer", new JsonObject()
            .put("name", "maxim integrated")
            .put("homepage", "https://www.maximintegrated.com")
            .put("datasheets", "https://datasheets.maximintegrated.com/en/ds/DS2408.pdf")
        )
        .put("valueType", "number")
        .put("minSampleDelay", "1000")
        .put("valueReadPath", "/sensed.ALL")
        .put("valueWritePath", "/PIO.ALL");
    db.put(type.getString("typeId"), type);

    return db;
  }
}
