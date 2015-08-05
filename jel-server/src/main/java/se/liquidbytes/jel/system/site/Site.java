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
package se.liquidbytes.jel.system.site;

import se.liquidbytes.jel.system.device.Actuator;
import se.liquidbytes.jel.system.device.Sensor;
import java.util.List;
import se.liquidbytes.jel.system.Identifiable;

/**
 *
 * @author Henrik Östman
 */
public class Site implements Identifiable {

  private String id;
  private String name;
  private String description;
  private String[] gps; //"57.6378669 18.284855"
  private SiteACL siteAcl;
  private boolean publicVisible;
  private List<Sensor> sensors;
  private List<Actuator> actuators;

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
   * @return the siteAcl
   */
  public SiteACL getSiteAcl() {
    return siteAcl;
  }

  /**
   * @param siteAcl the siteAcl to set
   */
  public void setSiteAcl(SiteACL siteAcl) {
    this.siteAcl = siteAcl;
  }

  /**
   * @return the publicVisible
   */
  public boolean isPublicVisible() {
    return publicVisible;
  }

  /**
   * @param publicVisible the publicVisible to set
   */
  public void setPublicVisible(boolean publicVisible) {
    this.publicVisible = publicVisible;
  }

  /**
   * @return the sensors
   */
  public List<Sensor> getSensors() {
    return sensors;
  }

  /**
   * @param sensors the sensors to set
   */
  public void setSensors(List<Sensor> sensors) {
    this.sensors = sensors;
  }

  /**
   * @return the actuators
   */
  public List<Actuator> getActuators() {
    return actuators;
  }

  /**
   * @param actuators the actuators to set
   */
  public void setActuators(List<Actuator> actuators) {
    this.actuators = actuators;
  }
}
