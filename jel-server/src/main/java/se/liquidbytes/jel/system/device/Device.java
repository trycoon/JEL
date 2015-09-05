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
 *
 * @author Henrik Östman
 */
public abstract class Device implements Identifiable {

  private String id;
  private String name;
  private String description;
  private String[] gps; //"57.6378669 18.284855"
  private String state;
  private DeviceHardware hardware;
  private DeviceValue currentValue;
  private DeviceValue previousValue;
  private String valueType;
  private DevicePresentation smallPresentation;
  private DevicePresentation mediumPresentation;
  private DevicePresentation largePresentation;

  /**
   * @return the id
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  @Override
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the gps
   */
  public String[] getGps() {
    return gps;
  }

  /**
   * @param gps the gps to set
   */
  public void setGps(String[] gps) {
    this.gps = gps;
  }

  /**
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * @param state the state to set
   */
  public void setState(String state) {
    this.state = state;
  }

  /**
   * @return the hardware
   */
  public DeviceHardware getHardware() {
    return hardware;
  }

  /**
   * @param hardware the hardware to set
   */
  public void setHardware(DeviceHardware hardware) {
    this.hardware = hardware;
  }

  /**
   * @return the currentValue
   */
  public DeviceValue getCurrentValue() {
    return currentValue;
  }

  /**
   * @param currentValue the currentValue to set
   */
  public void setCurrentValue(DeviceValue currentValue) {
    this.currentValue = currentValue;
  }

  /**
   * @return the previousValue
   */
  public DeviceValue getPreviousValue() {
    return previousValue;
  }

  /**
   * @param previousValue the previousValue to set
   */
  public void setPreviousValue(DeviceValue previousValue) {
    this.previousValue = previousValue;
  }

  /**
   * @return the valueType
   */
  public String getValueType() {
    return valueType;
  }

  /**
   * @param valueType the valueType to set
   */
  public void setValueType(String valueType) {
    this.valueType = valueType;
  }

  /**
   * @return the smallPresentation
   */
  public DevicePresentation getSmallPresentation() {
    return smallPresentation;
  }

  /**
   * @param smallPresentation the smallPresentation to set
   */
  public void setSmallPresentation(DevicePresentation smallPresentation) {
    this.smallPresentation = smallPresentation;
  }

  /**
   * @return the mediumPresentation
   */
  public DevicePresentation getMediumPresentation() {
    return mediumPresentation;
  }

  /**
   * @param mediumPresentation the mediumPresentation to set
   */
  public void setMediumPresentation(DevicePresentation mediumPresentation) {
    this.mediumPresentation = mediumPresentation;
  }

  /**
   * @return the largePresentation
   */
  public DevicePresentation getLargePresentation() {
    return largePresentation;
  }

  /**
   * @param largePresentation the largePresentation to set
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
