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
import se.liquidbytes.jel.system.plugin.Plugin;

/**
 * Base class extended by all adapters
 *
 * @author Henrik Östman
 */
public abstract class AbstractAdapter extends AbstractVerticle implements Plugin {

  /**
   * Id for adapter.
   */
  private String id;

  public AbstractAdapter() {

  }

  /**
   * Get id for adapter.
   *
   * @return adapter id
   */
  public String getId() {
    return id;
  }

  /**
   * Set id for adapter.
   *
   * @param id id to assign adapter.
   */
  protected void setId(String id) {
    this.id = id;
  }

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
    // By default we do nothing, but derived classes may use this if something need to be run at plugin startup and before this verticle has started.
  }

  /**
   * Method will be invoked when plugin is stopped by plugin manager.
   */
  @Override
  public void pluginStop() {
    // By default we do nothing, but derived classes may use this if something need to be run at plugin shutdown and after this verticle has been stopped.
  }
}
