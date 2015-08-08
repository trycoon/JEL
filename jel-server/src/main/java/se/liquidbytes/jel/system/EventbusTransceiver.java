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
package se.liquidbytes.jel.system;

/**
 * Interface implemented by all central components of JEL that communicates with the eventbus.
 *
 * @author Henrik Östman
 */
public interface EventbusTransceiver {

  /**
   * Namespace of eventbus.
   */
  public final static String EVENTBUS = "jel.eventbus";

  /**
   * Namespace for plugins communicating over the eventbus.
   */
  //public final static String EVENTBUS_PLUGINS = "jel.eventbus.plugins";

  /**
   * Namespace for plugin events.
   */
  /*public enum Eventbus_Plugins {

    PLUGIN_INSTALLED("PLUGIN_INSTALLED"),
    PLUGIN_UNINSTALLED("PLUGIN_UNINSTALLED"),
    PLUGIN_STARTED("PLUGIN_STARTED"),
    PLUGIN_STOPPED("PLUGIN_STOPPED");

    private final String value;

    private Eventbus_Plugins(String value) {
      this.value = value;
    }
   }*/
}
