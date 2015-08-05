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

/**
 * Class repressent a physical adapter JEL use for communication with devices connected to the adpater.
 *
 * @author Henrik Östman
 */
public final class Adapter {

  private String name;
  private String address;
  private int port;

  /**
   * Constructor
   *
   * @param name Name of adapter
   * @param address IP-address the adapter is listening to
   * @param port port adapter is listening on
   */
  public Adapter(String name, String address, int port) {
    this.setName(name);
    this.setAddress(address);
    this.setPort(port);
  }

  /**
   * Name of adapter
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Name of adapter
   *
   * @param name the name to set
   */
  public void setName(String name) {
    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid adapter name.");
    }

    this.name = name.trim().toLowerCase();
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
   * Address of adapter
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
   * Port of adapter
   *
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

}
