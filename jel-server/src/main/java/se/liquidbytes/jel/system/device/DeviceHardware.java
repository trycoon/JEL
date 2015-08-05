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
package se.liquidbytes.jel.system.device;

/**
 *
 * @author Henrik Östman
 */
public class DeviceHardware {

  private String id;
  private String vendorId;
  private String port;

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the vendorId
   */
  public String getVendorId() {
    return vendorId;
  }

  /**
   * @param vendorId the vendorId to set
   */
  public void setVendorId(String vendorId) {
    this.vendorId = vendorId;
  }

  /**
   * @return the port
   */
  public String getPort() {
    return port;
  }

  /**
   * @param port the port to set
   */
  public void setPort(String port) {
    this.port = port;
  }
}
