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
package se.liquidbytes.jel.system;

/**
 * Enum-like class that holds the list of all events the core parts of JEL supports and could emit. These are for internal communication only, see PublicEvents
 * for events that could be emitted to clients.
 *
 * @author Henrik Östman
 */
public final class InternalEvents {

  /**
   * Namespace for communicating events over the internal eventbus.
   */
  public static final String EVENTBUS_INTERNAL = JelService.EVENTBUS + ".internal";

  public static final String EVENT_ADAPTERTYPE_ADDED = "ADAPTERTYPE_ADDED";
  public static final String EVENT_ADAPTERTYPE_REMOVED = "ADAPTERTYPE_REMOVED";

  public static final String EVENT_ADAPTER_ADDED = "ADAPTER_ADDED";
  public static final String EVENT_ADAPTER_REMOVED = "ADAPTER_REMOVED";
  public static final String EVENT_ADAPTER_STARTED = "ADAPTER_STARTED";
  public static final String EVENT_ADAPTER_STOPPED = "ADAPTER_STOPPED";

  /**
   * When a new device has been added to the devicemanager.
   */
  public static final String EVENT_DEVICES_ADDED = "DEVICE_ADDED";
  /**
   * When a device has been removed from the devicemanager.
   */
  public static final String EVENT_DEVICES_REMOVED = "DEVICE_REMOVED";
  /**
   * When a device reading has been registred by the devicemanager.
   */
  public static final String EVENT_DEVICE_NEWREADING = "DEVICE_NEWREADING";
  /**
   * When a device has been registred as present/connected.
   */
  public static final String EVENT_DEVICE_PRESENT = "DEVICE_PRESENT";
  /**
   * When a device has been registred as not present/connected.
   */
  public static final String EVENT_DEVICE_NOTPRESENT = "DEVICE_NOTPRESENT";

}
