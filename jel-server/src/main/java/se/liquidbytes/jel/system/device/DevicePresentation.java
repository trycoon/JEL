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

import java.util.Map;

/**
 *
 * @author Henrik Östman
 */
public class DevicePresentation {

  private String type;
  private Map<String, String> settings;

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the settings
   */
  public Map<String, String> getSettings() {
    return settings;
  }

  /**
   * @param settings the settings to set
   */
  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }
}
