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

import com.cyngn.vertx.async.promise.PromiseFactory;
import io.vertx.core.Vertx;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.system.adapter.AdapterManager;
import se.liquidbytes.jel.system.device.DeviceManager;
import se.liquidbytes.jel.system.plugin.PluginManager;
import se.liquidbytes.jel.web.WebserverVerticle;

/**
 * Class with static references to common used instances needed by JEL components.
 *
 * @author Henrik Östman
 */
public final class JelService {

  /**
   * Namespace of eventbus, used by components that require to talk to JEL-system using its central eventbus.
   */
  public final static String EVENTBUS = "jel.eventbus";
  /**
   * Endpoint for public REST API.
   */
  public final static String API_ENDPOINT = Settings.getServerEndpoint() + WebserverVerticle.REST_MOUNTPOINT;
  /**
   * Vertx instance *
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

  /**
   * Device Manager instance
   */
  private static DeviceManager deviceManager;

  /**
   * Promise factory instance
   */
  private static PromiseFactory promiseFactory;

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
    promiseFactory = new PromiseFactory(vertx);
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

  /**
   * Get device manager instance.
   *
   * @return adapter manager instance.
   */
  public static DeviceManager deviceManager() {
    return deviceManager;
  }

  /**
   * Set device manager instance.
   *
   * @param adapterManager
   */
  static void deviceManager(DeviceManager deviceManager) {
    JelService.deviceManager = deviceManager;
  }

  /**
   * Get promise factory for this vertx instance.
   *
   * @return promise factory instance.
   */
  public static PromiseFactory promiseFactory() {
    return promiseFactory;
  }
}
