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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.system.JelService;
import static se.liquidbytes.jel.system.JelService.EVENTBUS;
import se.liquidbytes.jel.system.plugin.PluginDesc;

/**
 * Class that loads and keeps track of all adapterDescriptions.
 *
 * @author Henrik Östman
 */
public final class AdapterManager {

  /**
   * File of all adapterDescriptions. Loaded at applicationstart, and updated every time we add a new AdapterSettings.
   */
  private final static String ADAPTERS_FILE = "adapters.json";

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Namespace for communicating adapter events over the eventbus.
   */
  public final static String EVENTBUS_ADAPTERS = EVENTBUS + ".adapters";

  /**
   * Events
   */
  public final static String EVENT_ADAPTERTYPE_ADDED = "ADAPTERTYPE_ADDED";
  public final static String EVENT_ADAPTERTYPE_REMOVED = "ADAPTERTYPE_REMOVED";
  public final static String EVENT_ADAPTER_ADDED = "ADAPTER_ADDED";
  public final static String EVENT_ADAPTER_REMOVED = "ADAPTER_REMOVED";
  public final static String EVENT_ADAPTER_STARTED = "ADAPTER_STARTED";
  public final static String EVENT_ADAPTER_STOPPED = "ADAPTER_STOPPED";

  /**
   * Collection of all adapters.
   */
  private final Map<String, List<AbstractAdapter>> adapters;  // Adaptertype name as key.

  /**
   * Collection of all plugins of adapter-type.
   */
  private final Map<String, PluginDesc> adapterTypes; // Adaptertype name as key.

  /**
   * Collection of settings for adapters.
   */
  private AdapterSettingsList adaptersSettings;

  /**
   * ObjectMapper instance for loading and storing settings-files.
   */
  private final ObjectMapper objectMapper;

  /**
   * Default constructor.
   */
  public AdapterManager() {
    adapters = new LinkedHashMap<>();
    adapterTypes = new LinkedHashMap<>();
    adaptersSettings = new AdapterSettingsList();
    objectMapper = new ObjectMapper();
  }

  /**
   * Method for starting adapter manager, should be called upon application startup.
   */
  public void start() {

    Path path = Paths.get(Settings.getStoragePath().toString(), File.separator, ADAPTERS_FILE);

    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      logger.info("Loading adapters settings from '{}'", path);

      try {
        byte[] fileContent = Files.readAllBytes(path);
        adaptersSettings = objectMapper.readValue(fileContent, AdapterSettingsList.class);
      } catch (IOException ex) {
        throw new JelException(String.format("Failed to load adapters settings from '%s'", path), ex);
      }
    } else {
      try {
        // "adapters.json"-file does not exist, so we create it.
        objectMapper.writeValue(path.toFile(), adaptersSettings);
      } catch (IOException ex) {
        throw new JelException(String.format("Failed to create file '%s'.", ADAPTERS_FILE), ex);
      }
    }
  }

  /**
   * Method for stopping adapter manager, should be called upon application shutdown.
   *
   * @param stopFuture Optional future that will be called when shutdown has succeeded or failed. Set to null if not needed.
   */
  public void stop(Future<Void> stopFuture) {

    if (stopFuture != null) {
      //TODO: undeploy all adapters and clear list.
      stopFuture.complete();
    }
  }

  /**
   * Get available adaptertypes.
   *
   * @return collection of adaptertypes.
   */
  public List<PluginDesc> getAvailableAdapterTypes() {
    synchronized (adapterTypes) {
      return new ArrayList(adapterTypes.values());
    }
  }

  /**
   * Register a plugin of type adapter. This should only be used by the plugin manager.
   *
   * @param adapterType adapter-plugin.
   */
  public void registerAdapterTypePlugin(PluginDesc adapterType) {
    synchronized (adapterTypes) {
      if (adapterTypes.get(adapterType.getName()) == null) {

        logger.info("Adding new adaptertype '{}'.", adapterType.getName());
        adapterTypes.put(adapterType.getName(), adapterType);

        JsonObject event = new JsonObject()
            .put("name", adapterType.getName())
            .put("description", adapterType.getDescription())
            .put("version", adapterType.getVersion())
            .put("author", adapterType.getAuthor())
            .put("fileChecksum", adapterType.getFileChecksum())
            .put("homepage", adapterType.getHomepage())
            .put("license", adapterType.getLicence())
            .put("path", adapterType.getDirectoryPath().toString());
        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, event, new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTERTYPE_ADDED));

        startAdapters(adapterType, null);
      }
    }
  }

  /**
   * Unregister a plugin of type adapter. This should only be used by the plugin manager.
   *
   * @param adapterType adapter-plugin.
   */
  public void unregisterAdapterTypePlugin(PluginDesc adapterType) {
    synchronized (adapterTypes) {
      PluginDesc type = adapterTypes.get(adapterType.getName());

      if (type != null) {

        logger.info("Removing existing adaptertype '{}'.", type.getName());
        adapterTypes.remove(type.getName());

        JsonObject event = new JsonObject()
            .put("name", type.getName())
            .put("description", type.getDescription())
            .put("version", type.getVersion())
            .put("author", type.getAuthor())
            .put("fileChecksum", type.getFileChecksum())
            .put("homepage", type.getHomepage())
            .put("license", type.getLicence())
            .put("path", type.getDirectoryPath().toString());
        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, event, new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTERTYPE_REMOVED));

        stopAdapters(adapterType, null);
      }
    }
  }

  /**
   * Returns the list with information of all adapters.
   *
   * @return All adapters
   */
  public synchronized List<AdapterSettings> getAdapters() {
    return new ArrayList(adaptersSettings.getAdapters());
  }

  public void addAdapter(AdapterSettings settings) {

  }

  public void removeAdapter(AdapterSettings settings) {

  }

  /**
   * Stops verticles for adapter and adaptertypes. We take care of stopping all affected verticles running this/these adapters. Set unused paramters to null.
   *
   * @param adapterType if a adaptertype (plugin) has been unregistered, pass its plugin description to this method.
   * @param adapterSettings if a single adapter has been removed then pass its adapterSetting to this method.
   */
  private void stopAdapters(PluginDesc adapterType, AdapterSettings adapterSettings) {
    synchronized (adapterTypes) {
      List<AdapterSettings> settings = adaptersSettings.getAdapters();

      if (adapterType != null) {
        // Stop all adapters which adaptertype just has been unregistered.

        List<AbstractAdapter> adapterList = adapters.get(adapterType.getName());

        if (adapterList != null) {
          for (AbstractAdapter adapter : adapterList) {
            JelService.vertx().undeploy(adapter.deploymentID(), res -> {
              if (res.succeeded()) {
                logger.info("Stopped verticle for adapter '{}'", adapter.config().getString("type"));
                adapterList.remove(adapter);
              } else {
                logger.error("Failed to stop verticle for adapter '{}'", adapter.config().getString("type"), res.cause());
              }
            });
          }
        }
      }

      if (adapterSettings != null) {
        // Stop  adapter which just has been removed.
        for (List<AbstractAdapter> adapterInstances : adapters.values()) {
          for (AbstractAdapter adapter : adapterInstances) {
            if (adapter.config().getString("type").equals(adapterSettings.getType())
                && adapter.config().getString("address").equals(adapterSettings.getAddress())
                && adapter.config().getInteger("port") == adapterSettings.getPort()) {

              JelService.vertx().undeploy(adapter.deploymentID(), res -> {
                if (res.succeeded()) {
                  logger.info("Stopped verticle for adapter '{}'", adapter.config().getString("type"));
                  adapterInstances.remove(adapter);
                } else {
                  logger.error("Failed to stop verticle for adapter '{}'", adapter.config().getString("type"), res.cause());
                }
              });
            }
          }
        }
      }
    }
  }

  /**
   * Starts verticles for adapter and adaptertypes based upon the available adaptertypes and which adapters that should be running according to the
   * adapters.json-file.
   *
   * @param adapterType if a adaptertype (plugin) has been registered, pass its plugin description to this method.
   * @param adapterSettings if a single adapter has been added then pass its adapterSetting to this method.
   */
  private void startAdapters(PluginDesc adapterType, AdapterSettings adapterSettings) {
    synchronized (adapterTypes) {
      List<AdapterSettings> settings = adaptersSettings.getAdapters();

      if (adapterType != null) {
        // Start all adapters which adaptertype just has been registered.

        if (!adapters.containsKey(adapterType.getName())) {
          adapters.put(adapterType.getName(), new ArrayList<>());
        }

        List<AbstractAdapter> adapterList = adapters.get(adapterType.getName());

        AdapterSettings adapterToStart;

        for (AdapterSettings adapterSetting : settings) {
          adapterToStart = adapterSetting;

          for (AbstractAdapter adapter : adapterList) {
            if (adapterSetting.getType().equals(adapter.config().getString("type")) && adapterSetting.getAddress().equals(adapter.config().getString("address")) && adapterSetting.getPort() == adapter.config().getInteger("port")) {
              adapterToStart = null;  // An adapter with these settings are already running, so set this variable to not start.
              break;
            }
          }

          if (adapterToStart != null) {
            AbstractAdapter adapterInstance;

            try {
              adapterInstance = (AbstractAdapter) JelService.pluginManager().getPluginsInstance(adapterType, true);

              JsonObject config = new JsonObject();
              config.put("type", adapterToStart.getType());          // Name of adaptertype.
              config.put("address", adapterToStart.getAddress());    // Address of adapter, could be a network TCP/IP address, but also the type of a physical port e.g. "/dev/ttyS0".
              config.put("port", adapterToStart.getPort());          // Optional port of adapter, most commonly used by networked based adapter.

              DeploymentOptions deployOptions = new DeploymentOptions();
              deployOptions.setConfig(config);
              deployOptions.setInstances(1);
              deployOptions.setWorker(true);

              JelService.vertx().deployVerticle(adapterInstance, deployOptions, res -> {
                JsonObject adapterConfig = JelService.vertx().getOrCreateContext().config();

                if (res.succeeded()) {
                  logger.info("Successfully deployed verticle with id: {}, for adapter '{}' using address '{}' and port '{}'.",
                      res.result(), adapterConfig.getString("type"), adapterConfig.getString("address"), adapterConfig.getInteger("port"));

                  adapterList.add(adapterInstance);
                } else {
                  logger.error("Failed to deploy verticle for adapter '{}' using address '{}' and port '{}'.",
                      adapterConfig.getString("type"), adapterConfig.getString("address"), adapterConfig.getInteger("port"), res.cause());
                }
              });
            } catch (ClassCastException exception) {
              logger.error("Plugin, {}, is said to be a adapter but does not inheritage from AbstractAdapter-class, this must be fixed by plugin developer! Adapter will not be started.", adapterType.getName(), exception);
            }
          }
        }
      }

      if (adapterSettings != null) {
        // Start  adapter which just has been added.

        Optional<PluginDesc> plugin = JelService.pluginManager().getLoadedPlugins().stream().filter(a -> a.getName().equals(adapterSettings.getType())).findFirst();
        if (!plugin.isPresent()) {
          logger.debug("No adaptertype with name '{}' registered yet, skipping this adapter.", adapterSettings.getType());
        } else {
          try {
            AbstractAdapter adapterInstance = (AbstractAdapter) JelService.pluginManager().getPluginsInstance(plugin.get(), true);

            JsonObject config = new JsonObject();
            config.put("type", adapterSettings.getType());          // Name of adaptertype.
            config.put("address", adapterSettings.getAddress());    // Address of adapter, could be a network TCP/IP address, but also the type of a physical port e.g. "/dev/ttyS0".
            config.put("port", adapterSettings.getPort());          // Optional port of adapter, most commonly used by networked based adapter.

            DeploymentOptions deployOptions = new DeploymentOptions();
            deployOptions.setConfig(config);
            deployOptions.setInstances(1);
            deployOptions.setWorker(true);

            JelService.vertx().deployVerticle(adapterInstance, deployOptions, res -> {
              JsonObject adapterConfig = JelService.vertx().getOrCreateContext().config();

              if (res.succeeded()) {
                logger.info("Successfully deployed verticle with id: {}, for adapter '{}' using address '{}' and port '{}'.",
                    res.result(), adapterConfig.getString("type"), adapterConfig.getString("address"), adapterConfig.getInteger("port"));

                if (!adapters.containsKey(adapterConfig.getString("type"))) {
                  adapters.put(adapterConfig.getString("type"), new ArrayList<>());
                }

                List<AbstractAdapter> adapterList = adapters.get(adapterSettings.getType());
                adapterList.add(adapterInstance);
              } else {
                logger.error("Failed to deploy verticle for adapter '{}' using address '{}' and port '{}'.",
                    adapterConfig.getString("type"), adapterConfig.getString("address"), adapterConfig.getInteger("port"), res.cause());
              }
            });
          } catch (ClassCastException exception) {
            logger.error("Plugin, {}, is said to be a adapter but does not inheritage from AbstractAdapter-class, this must be fixed by plugin developer! Adapter will not be started.", plugin.get().getName(), exception);
          }
        }

      }
    }
  }
}
