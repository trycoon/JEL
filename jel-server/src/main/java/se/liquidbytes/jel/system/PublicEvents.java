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
 * Enum-like class that holds the list of all events that could be emitted from JEL to connected clientst.
 *
 * @author Henrik Östman
 */
public final class PublicEvents {

  /**
   * Namespace for communicating events over the public eventbus.
   */
  public static final String EVENTBUS_PUBLIC = JelService.EVENTBUS + ".public";

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
