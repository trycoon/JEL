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

import io.vertx.core.Vertx;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.Settings;

/**
 * Class that loads and keeps track of all adapters.
 *
 * @author Henrik Östman
 */
public final class AdapterManager {

  /**
   * File of all adapters. Loaded at applicationstart, and updated every time we add a new Adapter.
   */
  private final static String ADAPTERS_FILE = "adapters.json";
  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Collection of all loaded adapters.
   */
  private List<Adapter> adapters;
  /**
   * Vertx instance
   */
  private final Vertx vertx;

  /**
   * Constructor
   *
   * @param vertx Vertx instance.
   */
  public AdapterManager(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Add a new adapter
   *
   * @param adapter A adapter
   */
  /*public void addAdapter(Adapter adapter) {    synchronized (adapters) {
   adapters.put(generateKey(adapter), adapter);
   }
   }*/
  private void loadAdapters() {

    Path path = Paths.get(Settings.get("storagepath") + File.separator + ADAPTERS_FILE);


    logger.info("Successfully loaded list of available adapters.");
  }

  /**
   * Returns the list of all loaded adapters.
   *
   * @return All adapters
   */
  public synchronized List<Adapter> getAdapters() {
    if (adapters == null) {
      adapters = new ArrayList<>();
      loadAdapters();
    }

    return Collections.<Adapter>unmodifiableList(adapters);
  }
}
