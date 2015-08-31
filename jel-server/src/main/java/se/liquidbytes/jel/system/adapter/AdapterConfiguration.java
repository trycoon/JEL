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
import java.util.Objects;

/**
 * Class holds the configuration for a physical adapter (these are stored in adapters.json).
 *
 * @author Henrik Östman
 */
public final class AdapterConfiguration {

  private String type;
  private String address;
  private int port;

  /**
   * Default constructor
   */
  public AdapterConfiguration() {
    // Nothing here.
  }

  /**
   * Constructor
   *
   * @param type Type of adapter
   * @param address IP-address the adapter is listening to
   * @param port port adapter is listening on
   */
  public AdapterConfiguration(String type, String address, int port) {
    this.setType(type);
    this.setAddress(address);
    this.setPort(port);
  }

  /**
   * Type of adapter, unique but human readable name of adapter
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Type of adapter, unique but human readable name of adapter
   *
   * @param type the name to set
   */
  public void setType(String type) {
    if (type == null || type.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid adapter type-name.");
    }

    this.type = type.trim().toLowerCase();
  }

  /**
   * Address of adapter
   *
   * @return the address
   */
  public String getAddress() {
    return address;
  }

  /**
   * Address of adapter, could be a network TCP/IP address, but also the type of a physical port e.g. "/dev/ttyS0".
   *
   * @param address the address to set
   */
  public void setAddress(String address) {
    if (address == null || address.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid adapter address.");
    }

    this.address = address.trim().toLowerCase();
  }

  /**
   * Port of adapter
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Port of adapter This is optional, and mostly required by network based adapters.
   *
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Information about this object in a public API-friendly way.
   *
   * @return Information about this object.
   */
  public JsonObject toApi() {
    JsonObject obj = new JsonObject()
        .put("type", type)
        .put("address", type)
        .put("port", type);

    return obj;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other instanceof AdapterConfiguration) {
      AdapterConfiguration otherConfig = ((AdapterConfiguration) other);

      if (otherConfig.getType().equals(this.getType())
          && otherConfig.getAddress().equals(this.getAddress())
          && otherConfig.getPort() == this.getPort()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 73 * hash + Objects.hashCode(this.type);
    hash = 73 * hash + Objects.hashCode(this.address);
    hash = 73 * hash + this.port;
    return hash;
  }
}
