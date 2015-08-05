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
public class Sensor extends Device {

  private DeviceValue maxValue;
  private DeviceValue minValue;
  private long sampleDelay;
  private String valueTransformation;

    //ACL_Rights [string]("role1", "role2")

  /**
   * @return the maxValue
   */
  public DeviceValue getMaxValue() {
    return maxValue;
  }

  /**
   * @param maxValue the maxValue to set
   */
  public void setMaxValue(DeviceValue maxValue) {
    this.maxValue = maxValue;
  }

  /**
   * @return the minValue
   */
  public DeviceValue getMinValue() {
    return minValue;
  }

  /**
   * @param minValue the minValue to set
   */
  public void setMinValue(DeviceValue minValue) {
    this.minValue = minValue;
  }

  /**
   * @return the sampleDelay
   */
  public long getSampleDelay() {
    return sampleDelay;
  }

  /**
   * @param sampleDelay the sampleDelay to set
   */
  public void setSampleDelay(long sampleDelay) {
    this.sampleDelay = sampleDelay;
  }

  /**
   * @return the valueTransformation
   */
  public String getValueTransformation() {
    return valueTransformation;
  }

  /**
   * @param valueTransformation the valueTransformation to set
   */
  public void setValueTransformation(String valueTransformation) {
    this.valueTransformation = valueTransformation;
  }

}
