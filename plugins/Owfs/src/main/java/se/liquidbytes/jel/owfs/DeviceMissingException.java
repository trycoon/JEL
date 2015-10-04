package se.liquidbytes.jel.owfs;

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

/**
 * Exception that is thrown when trying to perform a action on a device that does not exist.
 */
public class DeviceMissingException extends RuntimeException {
  /**
   * Requested device id
   */
  private String deviceId;

  /**
   * Default constructor for exception class
   */
  public DeviceMissingException() {
    super();
  }

  /**
   * Constructor for exception class
   *
   * @param message Message
   * @param deviceId Device id
   */
  public DeviceMissingException(String message, String deviceId) {
    super(message);
    this.deviceId = deviceId;
  }

  /**
   * Constructor for exception class
   *
   * @param cause Inner exception
   */
  public DeviceMissingException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for exception class
   *
   * @param message Message
   * @param cause Inner exception
   */
  public DeviceMissingException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Set device id.
   *
   * @param deviceId
   */
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
   * Get device id.
   *
   * @return device id
   */
  public String getDeviceId() {
    return this.deviceId;
  }
}
