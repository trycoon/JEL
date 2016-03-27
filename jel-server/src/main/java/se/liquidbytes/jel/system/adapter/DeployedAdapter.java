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

import io.vertx.core.json.JsonObject;
import se.liquidbytes.jel.system.plugin.PluginDesc;

/**
 * Class contains all information about a deployed adapter.
 *
 * @author Henrik Östman
 */
public class DeployedAdapter {
  private String id;
  private String deploymentId;
  private AdapterConfiguration config;
  private PluginDesc pluginDescription;

  /**
   * Returns the verticles deployment Id
   *
   * @return the deploymentId
   */
  public String deploymentId() {
    return deploymentId;
  }

  /**
   * Returns a unique adapter id for this adapter. Could only be called upon after config(config) has been run!
   *
   * @return the adapter id.
   */
  public String Id() {
    return id;
  }

  /**
   * Set the verticles deployment Id
   *
   * @param deploymentId the deploymentId to set
   */
  protected void deploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  /**
   * Get the adapters config (from adapters.json)
   *
   * @return the config
   */
  public AdapterConfiguration config() {
    return config;
  }

  /**
   * Set adapters config
   *
   * @param config the config to set
   */
  protected void config(AdapterConfiguration config) {
    this.config = config;
    this.id = String.valueOf(this.hashCode());
  }

  /**
   * Get adapters description (from plugin.json)
   *
   * @return the pluginDescription
   */
  public PluginDesc getPluginDescription() {
    return pluginDescription;
  }

  /**
   * Set adapters description
   *
   * @param pluginDescription the pluginDescription to set
   */
  protected void setPluginDescription(PluginDesc pluginDescription) {
    this.pluginDescription = pluginDescription;
  }

  /**
   * Information about this object in a public API-friendly way.
   *
   * @return Information about this object.
   */
  public JsonObject toApi() {
    JsonObject result = new JsonObject();
    result.put("id", this.id);
    result.put("config", this.config.toApi());
    result.put("pluginInformation", this.pluginDescription.toApi());

    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof DeployedAdapter) {
      AdapterConfiguration otherConfig = ((DeployedAdapter) other).config();

      if (otherConfig.getType().equals(config.getType())
          && otherConfig.getAddress().equals(config.getAddress())
          && otherConfig.getPort() == config.getPort()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Compare two adapterconfigurations to see if they have the same settings, thus is equal.
   *
   * @param other Other adapterconfiguration
   * @return if equal
   */
  public boolean equals(AdapterConfiguration other) {
    // I guess I could have put this code in the above equals-method, but that would have made it too magical.
    if (other == null) {
      return false;
    }

    return other.getType().equals(config.getType())
        && other.getAddress().equals(config.getAddress())
        && other.getPort() == config.getPort();
  }

  /**
   * Get hashvalue representing this adapterconfiguration
   *
   * @return hashvalue
   */
  @Override
  public int hashCode() {
    return Math.abs(java.util.Objects.hash(this.config.getType(), this.config.getAddress(), this.config.getPort()));
  }
}
