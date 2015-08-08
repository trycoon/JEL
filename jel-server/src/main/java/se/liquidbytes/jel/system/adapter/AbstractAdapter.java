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
package se.liquidbytes.jel.system.adapter;

import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import se.liquidbytes.jel.system.JelService;
import se.liquidbytes.jel.system.plugin.Plugin;
import se.liquidbytes.jel.system.plugin.PluginException;

/**
 * Base class extended by all adapters
 *
 * @author Henrik Östman
 */
public abstract class AbstractAdapter extends AbstractVerticle implements Plugin {

  /**
   * Namespace for adapters communicating over the eventbus.
   */
  public final static String EVENTBUS_ADAPTERS = "jel.eventbus.adapters";

  /**
   * Namespace for adapter events.
   */
  public enum Eventbus_Adapters {

    ADAPTER_STARTED("ADAPTER_STARTED"),
    ADAPTER_STOPPED("ADAPTER_STOPPED");

    private final String value;

    private Eventbus_Adapters(String value) {
      this.value = value;
    }
  }

  /**
   * Logghandler instance
   */
  private Logger logger;

  /**
   * Deployment id for this verticle.
   */
  private String deploymentId;

  /**
   * Get unique and human readable name of adapter
   *
   * @return Short name
   */
  public abstract String getName();

  /**
   * Get a more informative description of the adapter, Optional.
   *
   * @return Long description
   */
  public String getDescription() {
    return "";
  }

  /**
   * If adapter is capable of automatically detecting connected devices. If not, then devices has to manually be bound and unbound to adapter.
   *
   * @return Has support for autodetection
   */
  public abstract boolean isDevicesAutodetected();

  /**
   * Method will be invoked when plugin is installed into the system.
   */
  @Override
  public void pluginInstall() {
    // By default we do nothing, but derived classes may use this during installation for setting up needed files and directories.
  }

  /**
   * Method will be invoked when plugin is uninstalled from the system.
   */
  @Override
  public void pluginUninstall() {
    // By default we do nothing, but derived classes may use this during uninstallation for removing previous installed and created files and directories.
  }

  /**
   * Method will be invoked when plugin is started by plugin manager.
   */
  @Override
  public void pluginStart() {
    // Register us as a new adapter to adapter manager, it will start up this verticle.
    try {
      JelService.adapterManager().registerAdapter(this);
    } catch (Exception ex) {
      throw new PluginException(String.format("Failed to start adapter '%s'.", this.getName()), ex);
    }
  }

  /**
   * Method will be invoked when plugin is stopped by plugin manager.
   */
  @Override
  public void pluginStop() {
    // Unregister us from  adapter manager, it will stop up this verticle.
    try {
      JelService.adapterManager().unregisterAdapter(this);
    } catch (Exception ex) {
      throw new PluginException(String.format("Failed to stop adapter '%s'.", this.getName()), ex);
    }
  }
}
