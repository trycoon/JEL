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

import com.cyngn.vertx.async.Latch;
import com.cyngn.vertx.async.promise.Promise;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
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
   * File of all adapterDescriptions. Loaded at applicationstart, and updated every time we add a new AdapterConfiguration.
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
  private final Map<String, List<DeployedAdapter>> adapters;  // Adaptertype name as key.

  /**
   * Collection of all plugins of adapter-type.
   */
  private final Map<String, PluginDesc> adapterTypes; // Adaptertype name as key.

  /**
   * ObjectMapper instance for loading and storing settings-files.
   */
  private final ObjectMapper objectMapper;

  /**
   * Collection of settings for adapters.
   */
  private AdapterSettingsList adaptersSettings;

  /**
   * Path to file containing configuration about all adapters that should be loaded (adapters.json).
   */
  private Path adapterFilePath;

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

    adapterFilePath = Paths.get(Settings.getStoragePath().toString(), File.separator, ADAPTERS_FILE);

    if (Files.exists(adapterFilePath, LinkOption.NOFOLLOW_LINKS)) {
      logger.info("Loading adapters settings from '{}'", adapterFilePath);

      try {
        byte[] fileContent = Files.readAllBytes(adapterFilePath);
        adaptersSettings = objectMapper.readValue(fileContent, AdapterSettingsList.class);
      } catch (IOException ex) {
        throw new JelException(String.format("Failed to load adapters settings from '%s'", adapterFilePath), ex);
      }
    } else {
      try {
        // "adapters.json"-file does not exist, so we create it.
        objectMapper.writeValue(adapterFilePath.toFile(), adaptersSettings);
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

    logger.info("Shutting down adapters and adaptermanager...");

    Latch latch = new Latch(adapters.values().size(), () -> {
      logger.debug("All adapters stopped.");
      if (stopFuture != null) {
        stopFuture.complete();
      }
    });

    for (List<DeployedAdapter> adapterType : adapters.values()) {
      for (DeployedAdapter adapter : adapterType) {
        JelService.vertx().undeploy(adapter.deploymentId(), res -> {
          if (res.succeeded()) {
            logger.info("Stopped verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort());
          } else {
            logger.warn("Failed to stop verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort(), res.cause());
          }

          latch.complete();
        });
      }
    }
  }

  /**
   * Get available adaptertypes.
   *
   * @return collection of adaptertypes.
   */
  public List<PluginDesc> getAvailableAdapterTypes() {
    synchronized (adapterTypes) {
      return new CopyOnWriteArrayList(adapterTypes.values());
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

        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapterType.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTERTYPE_ADDED));

        startAdapters(adapterType);
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

        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapterType.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTERTYPE_REMOVED));

        stopAdapters(adapterType);
      }
    }
  }

  /**
   * Returns the list with information of all adapters.
   *
   * @return All adapters
   */
  public synchronized List<AdapterConfiguration> getAdapters() {
    return new CopyOnWriteArrayList(adaptersSettings.getAdapters());
  }

  /**
   * Add config for and start a new adapter
   *
   * @param config config for adapter to start
   * @throws JelException if failed to add adapter.
   */
  public void addAdapter(AdapterConfiguration config) {
    if (config == null) {
      throw new JelException("No initialized config provided.");
    }
    if (config.getType() == null || config.getType().isEmpty()) {
      throw new JelException("No adaptertype provided.");
    }
    if (config.getAddress() == null || config.getAddress().isEmpty()) {
      throw new JelException("No adapteraddress provided.");
    }

    List<AdapterConfiguration> existingAdapters = adaptersSettings.getAdapters();

    if (!existingAdapters.contains(config)) {
      existingAdapters.add(config);

      try {
        objectMapper.writeValue(adapterFilePath.toFile(), existingAdapters);
        startAdapter(config);

        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, config.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_ADDED));
      } catch (IOException ex) {
        existingAdapters.remove(config);
        throw new JelException(String.format("Failed to add adapterconfiguration to %s.", adapterFilePath), ex);
      }
    }
  }

  /**
   * Remove config for and stopp a existing adapter.
   *
   * @param config config for adapter.
   * @throws JelException if failed to remove adapter.
   */
  public void removeAdapter(AdapterConfiguration config) {
    if (config == null) {
      throw new JelException("No initialized config provided.");
    }

    List<AdapterConfiguration> existingAdapters = adaptersSettings.getAdapters();

    if (existingAdapters.contains(config)) {
      existingAdapters.remove(config);

      try {
        objectMapper.writeValue(adapterFilePath.toFile(), existingAdapters);
        stopAdapter(config);

        JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, config.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_REMOVED));
      } catch (IOException ex) {
        existingAdapters.add(config);
        throw new JelException(String.format("Failed to removed adapterconfiguration from %s.", adapterFilePath), ex);
      }
    }
  }

  /**
   * Stops verticles for adaptertype. We take care of stopping all affected verticles running this/these adapters.
   *
   * @param adapterType if a adaptertype (plugin) has been unregistered, pass its plugin description to this method.
   */
  private void stopAdapters(PluginDesc adapterType) {
    if (adapterType != null) {
      List<DeployedAdapter> adapterList = adapters.get(adapterType.getName());

      if (adapterList != null) {
        for (DeployedAdapter adapter : adapterList) {
          JelService.vertx().undeploy(adapter.deploymentId(), res -> {
            if (res.succeeded()) {
              adapterList.remove(adapter);
              logger.info("Stopped verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort());
              JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapter.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_STOPPED));
            } else {
              logger.error("Failed to stop verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort(), res.cause());
            }
          });
        }
      }
    }
  }

  /**
   * Stops verticle for adapter. We take care of stopping the affected verticle running this adapter.
   *
   * @param adapterSettings if a single adapter has been removed then pass its adapterSetting to this method.
   */
  private void stopAdapter(AdapterConfiguration adapterSettings) {
    if (adapterSettings != null) {
      for (List<DeployedAdapter> adapterInstances : adapters.values()) {
        for (DeployedAdapter adapter : adapterInstances) {
          if (adapter.equals(adapterSettings)) {
            JelService.vertx().undeploy(adapter.deploymentId(), res -> {
              if (res.succeeded()) {
                adapterInstances.remove(adapter);
                logger.info("Stopped verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort());
                JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapter.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_STOPPED));
              } else {
                logger.error("Failed to stop verticle for adapter '{}' using addess '{}' and port '{}'.", adapter.config().getType(), adapter.config().getAddress(), adapter.config().getPort(), res.cause());
              }
            });
          }
        }
      }
    }
  }

  /**
   * Starts verticles for adaptertypes based upon the available adaptertypes and which adapters that should be running according to the adapters.json-file.
   *
   * @param adapterType if a adaptertype (plugin) has been registered, pass its plugin description to this method.
   */
  private void startAdapters(PluginDesc adapterType) {
    if (adapterType != null) {
      List<AdapterConfiguration> configurations = adaptersSettings.getAdapters();

      if (!adapters.containsKey(adapterType.getName())) {
        adapters.put(adapterType.getName(), new CopyOnWriteArrayList<>());
      }

      List<DeployedAdapter> adapterList = adapters.get(adapterType.getName());

      Promise promise = JelService.promiseFactory().create();

      for (AdapterConfiguration adapterConfig : configurations) {

        promise.then((context, onResult) -> {
          AdapterConfiguration adapterToStart = adapterConfig;

          for (DeployedAdapter adapter : adapterList) {
            if (adapter.equals(adapterConfig)) {
              adapterToStart = null;  // An adapter with these settings are already running, so set this variable to not start.
              break;
            }
          }

          if (adapterToStart != null) {

            try {
              AbstractAdapter adapterInstance = (AbstractAdapter) JelService.pluginManager().getPluginsInstance(adapterType, true);

              JsonObject config = new JsonObject();
              config.put("type", adapterToStart.getType());          // Name of adaptertype.
              config.put("address", adapterToStart.getAddress());    // Address of adapter, could be a network TCP/IP address, but also the type of a physical port e.g. "/dev/ttyS0".
              config.put("port", adapterToStart.getPort());          // Optional port of adapter, most commonly used by networked based adapter.

              DeploymentOptions deployOptions = new DeploymentOptions();
              deployOptions.setConfig(config);
              deployOptions.setInstances(1);
              deployOptions.setWorker(true);

              JelService.vertx().deployVerticle(adapterInstance, deployOptions, res -> {
                AdapterConfiguration deployedConfig = new AdapterConfiguration(config.getString("type"), config.getString("address"), config.getInteger("port"));

                if (res.succeeded()) {

                  logger.info("Successfully deployed verticle with id: {}, for adapter '{}' using address '{}' and port '{}'.",
                      res.result(), deployedConfig.getType(), deployedConfig.getAddress(), deployedConfig.getPort());

                  DeployedAdapter adapter = new DeployedAdapter();
                  adapter.deploymentId(res.result());
                  adapter.config(deployedConfig);
                  adapter.setPluginDescription(adapterType);
                  adapterList.add(adapter);

                  JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapter.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_STARTED));
                  onResult.accept(true);
                } else {
                  logger.error("Failed to deploy verticle for adapter '{}' using address '{}' and port '{}'.",
                      deployedConfig.getType(), deployedConfig.getAddress(), deployedConfig.getPort(), res.cause());
                  // Continue deploying other adapters.
                  onResult.accept(true);
                }
              });
            } catch (ClassCastException exception) {
              logger.error("Plugin, {}, is said to be a adapter but does not inheritage from AbstractAdapter-class, this must be fixed by plugin developer! Adapter will not be started.", adapterType.getName(), exception);
              // Continue deploying other adapters.
              onResult.accept(true);
            }
          } else {
            // This adapter is already running so continue deploying other adapters.
            onResult.accept(true);
          }
        });
      }
      // Start deploying adapters serially.
      promise.eval();
    }
  }

  /**
   * Starts verticle for adapter based upon which adapters that should be running according to the adapters.json-file.
   *
   * @param adapterConfig configuration of adapter.
   */
  private void startAdapter(AdapterConfiguration adapterConfig) {
    if (adapterConfig != null) {
      List<AdapterConfiguration> configurations = adaptersSettings.getAdapters();

      Optional<PluginDesc> plugin = JelService.pluginManager().getLoadedPlugins().stream().filter(a -> a.getName().equals(adapterConfig.getType())).findFirst();
      if (!plugin.isPresent()) {
        logger.debug("No adaptertype with name '{}' registered yet, skipping this adapter.", adapterConfig.getType());
      } else {
        try {
          AbstractAdapter adapterInstance = (AbstractAdapter) JelService.pluginManager().getPluginsInstance(plugin.get(), true);

          JsonObject config = new JsonObject();
          config.put("type", adapterConfig.getType());          // Name of adaptertype.
          config.put("address", adapterConfig.getAddress());    // Address of adapter, could be a network TCP/IP address, but also the type of a physical port e.g. "/dev/ttyS0".
          config.put("port", adapterConfig.getPort());          // Optional port of adapter, most commonly used by networked based adapter.

          DeploymentOptions deployOptions = new DeploymentOptions();
          deployOptions.setConfig(config);
          deployOptions.setInstances(1);
          deployOptions.setWorker(true);

          JelService.vertx().deployVerticle(adapterInstance, deployOptions, res -> {
            AdapterConfiguration deployedConfig = new AdapterConfiguration(config.getString("type"), config.getString("address"), config.getInteger("port"));

            if (res.succeeded()) {
              logger.info("Successfully deployed verticle with id: {}, for adapter '{}' using address '{}' and port '{}'.",
                  res.result(), deployedConfig.getType(), deployedConfig.getAddress(), deployedConfig.getPort());

              if (!adapters.containsKey(deployedConfig.getType())) {
                adapters.put(deployedConfig.getType(), new CopyOnWriteArrayList<>());
              }

              List<DeployedAdapter> adapterList = adapters.get(deployedConfig.getType());

              DeployedAdapter adapter = new DeployedAdapter();
              adapter.deploymentId(res.result());
              adapter.config(deployedConfig);
              adapter.setPluginDescription(plugin.get());
              adapterList.add(adapter);

              JelService.vertx().eventBus().publish(EVENTBUS_ADAPTERS, adapter.toApi(), new DeliveryOptions().addHeader("action", AdapterManager.EVENT_ADAPTER_STARTED));
            } else {
              logger.error("Failed to deploy verticle for adapter '{}' using address '{}' and port '{}'.",
                  deployedConfig.getType(), deployedConfig.getAddress(), deployedConfig.getPort(), res.cause());
            }
          });
        } catch (ClassCastException exception) {
          logger.error("Plugin, {}, is said to be a adapter but does not inheritage from AbstractAdapter-class, this must be fixed by plugin developer! Adapter will not be started.", plugin.get().getName(), exception);
        }
      }
    }
  }
}
