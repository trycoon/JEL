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

import io.vertx.core.DeploymentOptions;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.system.JelService;

/**
 * Class that loads and keeps track of all adapterDescriptions.
 *
 * @author Henrik Östman
 */
public final class AdapterManager {

  /**
   * File of all adapterDescriptions. Loaded at applicationstart, and updated every time we add a new AdapterDesc.
   */
  private final static String ADAPTERS_FILE = "adapters.json";
  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Collection of all adapters.
   */
  private final List<AbstractAdapter> adapters;
  /**
   * Collection of all adapter descriptions.
   */
  private final Map<AbstractAdapter, AdapterDesc> adapterDescriptions;
  /**
   * Collection of verticle deployment id for every adapter.
   */
  private final Map<AbstractAdapter, String> deploymentIds;

  /**
   * Default constructor.
   */
  public AdapterManager() {
    adapters = new ArrayList<>();
    adapterDescriptions = new HashMap<>();
    deploymentIds = new HashMap<>();
  }

  /**
   * Register a new adapter.
   *
   * @param adapter A adapter
   */
  public void registerAdapter(AbstractAdapter adapter) {
    DeploymentOptions deployOptions = new DeploymentOptions();
    deployOptions.setInstances(1);
    deployOptions.setWorker(true);

    JelService.vertx().deployVerticle(adapter, deployOptions, res -> {
      if (res.failed()) {
        logger.error("Failed to deploy verticle for adapter '{}'.", adapter.getName(), res.cause());
        // Maybe this method should have a callback to notify adapter of deployment status. Simply throwing a exception here won't work.
      } else {
        adapters.add(adapter);
        adapterDescriptions.put(adapter, null); //TODO: Sett null here to a valid adapter description!!!
        deploymentIds.put(adapter, res.result());

        logger.info("Successfully deployed verticle for adapter '{}'.", adapter.getName());
      }
    });
  }

  /**
   * Unregister a existing adapter.
   *
   * @param adapter A adapter
   */
  public void unregisterAdapter(AbstractAdapter adapter) {
    String deploymentId = deploymentIds.get(adapter);

    if (deploymentId != null) {
      JelService.vertx().undeploy(deploymentId);
    }
  }

  /*private void loadAdapters() {

   Path path = Paths.get(Settings.get("storagepath") + File.separator + ADAPTERS_FILE);

   logger.info("Successfully loaded list of available adapterDescriptions.");
   }*/
  /**
   * Returns the list with information of all adapters.
   *
   * @return All adapters
   */
  public synchronized List<AdapterDesc> getAdapters() {
    return new ArrayList(adapterDescriptions.values());
  }
}
