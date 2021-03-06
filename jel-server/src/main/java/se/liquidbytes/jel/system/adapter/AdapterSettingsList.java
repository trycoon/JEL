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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * List of adapters settings, usually loaded and stored to the filesystem ("adapters.json").
 *
 * @author Henrik Östman
 */
public class AdapterSettingsList {
  private List<AdapterConfiguration> settings;

  /**
   * Default constructor.
   */
  public AdapterSettingsList() {
    settings = new CopyOnWriteArrayList<>();
  }

  /**
   * Get adapters settings
   *
   * @return the settings
   */
  public List<AdapterConfiguration> getAdapters() {
    return settings;
  }

  /**
   * Set adapters settings
   *
   * @param settings the settings to set
   */
  public void setAdapters(List<AdapterConfiguration> settings) {
    this.settings = settings;
  }
}
