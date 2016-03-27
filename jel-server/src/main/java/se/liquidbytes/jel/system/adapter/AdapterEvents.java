/*
 * Copyright 2016 Henrik Östman.
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
package se.liquidbytes.jel.system.adapter;

import se.liquidbytes.jel.system.JelService;

/**
 * Enum-like class that holds the list of all events an implementing adapter should support and could emit. Used by adapters to notify JEL about changes.
 *
 * @author Henrik Östman
 */
public final class AdapterEvents {

  /**
   * Namespace for communicating adapter events over the eventbus.
   */
  public static final String EVENTBUS_ADAPTERS = JelService.EVENTBUS + ".adapters";

  /**
   * A new device has been added to adapter. The event has a JSON-object payload containing: {"adapterId" - Id of adapter upon the new device has been added,
   * "port" - port of adapter, "host" - DNS-name/IP of adapter, "deviceId" - hardware id of device, "type" - type of device, "name" - name of device }
   */
  public static final String EVENT_DEVICES_ADDED = "DEVICE_ADDED";
  /**
   * A device has been removed from adapter. The event has a JSON-object payload containing: {"adapterId" - Id of adapter from where the device has been
   * removed, "port" - port of adapter, "host" - DNS-name/IP of adapter, "deviceId" - hardware id of device }
   */
  public static final String EVENT_DEVICES_REMOVED = "DEVICE_REMOVED";
  /**
   * New readings for a device has been collected. The event has a JSON-object payload containing: {"adapterId" - Id of adapter, "port" - port of adapter,
   * "host" - DNS-name/IP of adapter, "reading" - JSON-object containing the readings }.
   */
  public static final String EVENT_DEVICE_NEWREADING = "DEVICE_NEWREADING";
}
