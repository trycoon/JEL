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
package se.liquidbytes.jel.plugins;

/**
 * Interface implemented by all plugins and systems that communicates with the JEL central message bus.
 *
 * @author Henrik Östman
 */
public interface EventbusTransceiver {

  /**
   * Namespace of eventbus, used by plugins that require to talk to other plugins and the JEL-system using its central message bus.
   */
  public final static String EVENTBUS = "jel.eventbus";
  /**
   * Namespace for adapters communicating over the eventbus.
   */
  public final static String EVENTBUS_ADAPTERS = "jel.eventbus.adapters";
  /**
   * Namespace for external clients connecting to JEL using the eventbus.
   */
  public final static String EVENTBUS_API = "jel.eventbus.api";
}
