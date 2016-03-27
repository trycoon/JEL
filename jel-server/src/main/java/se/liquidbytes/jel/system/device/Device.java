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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.json.JsonObject;
import se.liquidbytes.jel.system.Identifiable;

/**
 * Abstract baseclass for devices, contains general stuff most inheritage devices would want to implement.
 *
 * @author Henrik Östman
 */
public abstract class Device implements Identifiable {

  private String id;
  private String name;
  private String description;
  private String[] gps; // e.g. "[57.6378669, 18.284855]" , latitude-longitude
  private boolean isPresent;
  private DeviceHardware hardware;
  private DeviceValue currentValue;
  private DeviceValue previousValue;
  private String valueType;
  private DevicePresentation smallPresentation;
  private DevicePresentation mediumPresentation;
  private DevicePresentation largePresentation;

  /**
   * Our internal Id and NOT the device own hardware id.
   *
   * @return the id
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Our internal Id and NOT the device own hardware id.
   *
   * @param id the id to set
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * The name we have given the device, e.g. "Outside temperature", or "Garage switch".
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * The name we have given the device, e.g. "Outside temperature", or "Garage switch".
   *
   * @param name the name to set
   */
  void setName(String name) {
    this.name = name;
  }

  /**
   * An optional longer description of the device than the name.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * An optional longer description of the device than the name.
   *
   * @param description the description to set
   */
  void setDescription(String description) {
    this.description = description;
  }

  /**
   * Optional GPS coordinates of the placement of the device.
   *
   * @return the gps
   */
  public String[] getGps() {
    return gps;
  }

  /**
   * Optional GPS coordinates of the placement of the device.
   *
   * @param gps the gps to set
   */
  void setGps(String[] gps) {
    this.gps = gps;
  }

  /**
   * If the device currently is present/connected.
   *
   * @return if present
   */
  public boolean isPresent() {
    return isPresent;
  }

  /**
   * If the device currently is present/connected.
   *
   * @param isPresent if present
   */
  void isPresent(boolean isPresent) {
    this.isPresent = isPresent;
  }

  /**
   * Get hardware configuration.
   *
   * @return the hardware
   */
  public DeviceHardware getHardware() {
    return hardware;
  }

  /**
   * Set hardware configuration.
   *
   * @param hardware the hardware to set
   */
  void setHardware(DeviceHardware hardware) {
    this.hardware = hardware;
  }

  /**
   * Get last device reading.
   *
   * @return the currentValue
   */
  public DeviceValue getCurrentValue() {
    return currentValue;
  }

  /**
   * Set last device reading.
   *
   * @param currentValue the currentValue to set
   */
  void setCurrentValue(DeviceValue currentValue) {
    this.currentValue = currentValue;
  }

  /**
   * Get previous device reading (the one before currentValue).
   *
   * @return the previous value
   */
  public DeviceValue getPreviousValue() {
    return previousValue;
  }

  /**
   * Set previous device reading (the one before currentValue).
   *
   * @param previousValue the previous value to set
   */
  void setPreviousValue(DeviceValue previousValue) {
    this.previousValue = previousValue;
  }

  /**
   * Get the datatype for the device reading, e.g. should it be parsed as a boolean, an number, a string, and so on.
   *
   * @return the value type
   */
  public String getValueType() {
    return valueType;
  }

  /**
   * Get the datatype for the device reading, e.g. should it be parsed as a boolean, an number, a string, and so on.
   *
   * @param valueType the value type to set
   */
  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  /**
   * Get the prefered visual representation of the device when shown on a small display.
   *
   * @return the small presentation
   */
  public DevicePresentation getSmallPresentation() {
    return smallPresentation;
  }

  /**
   * Set the prefered visual representation of the device when shown on a small display.
   *
   * @param smallPresentation the small presentation to set
   */
  public void setSmallPresentation(DevicePresentation smallPresentation) {
    this.smallPresentation = smallPresentation;
  }

  /**
   * Get the prefered visual representation of the device when shown on a medium size display.
   *
   * @return the medium presentation
   */
  public DevicePresentation getMediumPresentation() {
    return mediumPresentation;
  }

  /**
   * Set the prefered visual representation of the device when shown on a medium size display.
   *
   * @param mediumPresentation the medium presentation to set
   */
  public void setMediumPresentation(DevicePresentation mediumPresentation) {
    this.mediumPresentation = mediumPresentation;
  }

  /**
   * Get the prefered visual representation of the device when shown on a large size display.
   *
   * @return the large presentation
   */
  public DevicePresentation getLargePresentation() {
    return largePresentation;
  }

  /**
   * Set the prefered visual representation of the device when shown on a large size display.
   *
   * @param largePresentation the large presentation to set
   */
  public void setLargePresentation(DevicePresentation largePresentation) {
    this.largePresentation = largePresentation;
  }

  /**
   * Information about this object in a public API-friendly way.
   *
   * @return Information about this object.
   * @throws com.fasterxml.jackson.core.JsonProcessingException
   */
  public abstract JsonObject toApi() throws JsonProcessingException;
}
