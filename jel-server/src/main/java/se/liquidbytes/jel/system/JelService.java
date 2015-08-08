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

import io.vertx.core.Vertx;
import se.liquidbytes.jel.system.adapter.AdapterManager;
import se.liquidbytes.jel.system.plugin.PluginManager;

/**
 * Class with static references to common used instances needed by JEL components.
 *
 * @author Henrik Östman
 */
public final class JelService {

  /**
   * Vertx instance   *
   */
  private static Vertx vertx;

  /**
   * Plugin Manager instance
   */
  private static PluginManager pluginManager;

  /**
   * Adapter Manager instance
   */
  private static AdapterManager adapterManager;

  /*
   * Private constructor, to prevent instances from being created.
   */
  private JelService() {
    // Do nothing here!
  }

  /**
   * Get vertx instance.
   *
   * @return vertx instance.
   */
  public static Vertx vertx() {
    return vertx;
  }

  /**
   * Set vertx instance.
   *
   * @param vertx
   */
  static void vertx(Vertx vertx) {
    JelService.vertx = vertx;
  }

  /**
   * Get plugin manager instance.
   *
   * @return plugin manager instance.
   */
  public static PluginManager pluginManager() {
    return pluginManager;
  }

  /**
   * Set plugin manager instance.
   *
   * @param pluginManager
   */
  static void pluginManager(PluginManager pluginManager) {
    JelService.pluginManager = pluginManager;
  }

  /**
   * Get adapter manager instance.
   *
   * @return adapter manager instance.
   */
  public static AdapterManager adapterManager() {
    return adapterManager;
  }

  /**
   * Set adapter manager instance.
   *
   * @param adapterManager
   */
  static void adapterManager(AdapterManager adapterManager) {
    JelService.adapterManager = adapterManager;
  }
}
