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
package se.liquidbytes.jel.system.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.eventbus.DeliveryOptions;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.FileUtils;
import se.liquidbytes.jel.JelException;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.SystemInfo;
import se.liquidbytes.jel.system.JelService;
import static se.liquidbytes.jel.system.plugin.Plugin.EVENTBUS_PLUGINS;

/**
 * Class that loads and keeps track of all availablePlugins (extensions to JEL).
 *
 * @author Henrik Östman
 */
public final class PluginManager {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Name of file in each plugin-directory that contains metadata about the plugin.
   */
  private final static String META_FILENAME = "plugin.json";
  /**
   * Name of Readme-file that should be generated in plugin-directory.
   */
  private final static String PLUGIN_README_FILE = "PLUGIN-README";
  /**
   * Official repository of plugins to JEL.
   */
  private final static String PLUGIN_REPOSITORY = "http://plugins.mylogger.net";
  /**
   * Path to plugin directory.
   */
  private final Path pluginsPath;
  /**
   * Classloader for loading Java-based plugins.
   */
  public final ClassLoader classLoader;
  /**
   * Thread executor for plugin manager.
   */
  private ExecutorService filewatchExecutorService;
  /**
   * Collection of installed plugins (metadata).
   */
  private final Map<String, PluginDesc> installedPlugins;
  /**
   * Collection of loaded plugins. These are references to the actual plugin instances.
   */
  private final Map<PluginDesc, Plugin> loadedPlugins;
  /**
   * Collection of instantiated plugins classloaders.
   */
  private final Map<PluginDesc, PluginClassLoader> classloaders;

  /**
   * PluginDesc manager constructor.
   *
   * @param storagePath path to storage-directory.
   * @throws JelException Throws exception if vital directories for the plugin manager could not be created.
   */
  public PluginManager(String storagePath) throws JelException {
    logger.debug("Initializing Plugin Manager...");

    try {
      // Check and create "plugins"-directory if necessary.
      this.pluginsPath = Files.createDirectories(Paths.get(storagePath, "plugins"));
    } catch (IOException ex) {
      throw new JelException(String.format("Failed to create plugin directory, %s", Paths.get(storagePath, "plugins")), ex);
    }

    this.installedPlugins = new LinkedHashMap<>();
    this.loadedPlugins = new LinkedHashMap<>();
    this.classloaders = new LinkedHashMap<>();
    this.classLoader = Thread.currentThread().getContextClassLoader();

    logger.debug("Plugin Manager has been initalized successfully.");
  }

  /**
   * Method for starting plugin manager, should be called once during application start up.
   */
  public void start() {
    logger.info("Starting Plugin Manager.");

    createPlugReadme();
    scanForExistingPlugins();

    if (filewatchExecutorService == null) {
      setupDirectoryWatch();
    }
  }

  /**
   * Method for stopping plugin manager, should be called during application shut down to free resources.
   */
  public void stop() {
    logger.info("Stopping Plugin Manager.");

    if (filewatchExecutorService != null) {
      filewatchExecutorService.shutdownNow();
      filewatchExecutorService = null;
    }

    for (String pluginName : installedPlugins.keySet()) {
      try {
        PluginDesc desc = installedPlugins.get(pluginName);
        Plugin plugin = loadedPlugins.get(desc);

        loadedPlugins.remove(desc);
        installedPlugins.remove(pluginName);

        if (plugin != null) {
          plugin.pluginStop();
          JelService.vertx().eventBus().publish(EVENTBUS_PLUGINS, plugin, new DeliveryOptions().addHeader("action", Plugin.EVENT_PLUGIN_STOPPED));
        }

      } catch (Exception ex) {
        logger.warn("Failed to stop plugin('{}') during application shutdown.", pluginName, ex);
      }
    }

    // Clear out references that we may have missed (failed to stop), just to be safe.
    loadedPlugins.clear();
    installedPlugins.clear();

    for (PluginDesc desc : classloaders.keySet()) {
      try {
        try (PluginClassLoader classloader = classloaders.get(desc)) {
          classloader.unloadJarFiles();
        }

        classloaders.remove(desc);

      } catch (Exception ex) {
        logger.warn("Failed to unload resources for plugin('{}') during application shutdown.", desc.getName(), ex);
      }
    }

    // Clear out references that we may have missed (failed to unload), just to be safe.
    classloaders.clear();
  }

  /**
   * Create README-file in plugin directory.
   */
  private void createPlugReadme() {
    Path readmePath = Paths.get(this.pluginsPath.toString(), "README");

    if (!Files.exists(readmePath, LinkOption.NOFOLLOW_LINKS)) {
      URL readmeUrl = Settings.class.getClassLoader().getResource(PLUGIN_README_FILE);
      if (readmeUrl == null) {
        throw new JelException(String.format("Missing %s-file, corrupt installation?", PLUGIN_README_FILE));
      }

      try (
          BufferedReader reader = new BufferedReader(new InputStreamReader(readmeUrl.openStream()));
          PrintWriter writer = new PrintWriter(new FileWriter(readmePath.toFile()))) {
        String line = reader.readLine();

        while (line != null) {
          writer.println(line);
          line = reader.readLine();
        }
      } catch (IOException ex) {
        throw new JelException(String.format("Failed to generate %s-file.", readmePath.toString()), ex);
      }
    }
  }

  /**
   * Scan plugin directory for already installed plugins. This should be done at startup of plugin manager to setup its internal state.
   */
  private void scanForExistingPlugins() {
    logger.debug("Scanning plugin-directory '{}' for installed plugins.", pluginsPath);

    try {
      // Scan plugin-directory for existing plugins.
      Files.list(pluginsPath).forEach(pluginPath -> {
        // Sub-directories are already expanded/deployed plugins, these should be verified and loaded.
        if (Files.isDirectory(pluginPath, LinkOption.NOFOLLOW_LINKS)) {

          Path originalFile = getOriginalFileFromDirPath(pluginPath);

          if (originalFile != null) {

            try {

              PluginDesc plugin = parseAndVerifyPlugin(pluginPath, originalFile);

              startPlugin(plugin, false);

            } catch (IllegalArgumentException | JelException ex) {
              // Catch exeption here to prevent it from propagating and stopping us from loading other plugins.
              logger.error("Failed to load plugin '{}'.", pluginPath, ex);
            }
          } else {
            // If a plugin directory exists but no compressed plugin-file with this name in the root directory, then this plugin should be considered uninstalled and directory should be deleted.
            logger.info("Removing stale plugin directory: {}", pluginPath);

            try {
              FileUtils.deleteDirectory(pluginPath);
            } catch (IOException ex) {
              logger.warn("Failed to remove stale plugin directory({}), ignoring it for the moment. Please try to remove it manually if possible.", pluginPath, ex);
            }
          }
        } else if (Files.isRegularFile(pluginPath, LinkOption.NOFOLLOW_LINKS)) {
          String fileName = pluginPath.getFileName().toString();

          // Files in the plugin-directory of type jar or zip are plugins that should be expanded, verified, and loaded. Other files are ignored.
          if (!fileName.startsWith("~")
              && !fileName.startsWith(".")
              && (fileName.endsWith(".jar") || fileName.endsWith(".zip"))) {

            Path directoryPath = getDirectoryPathFromFile(pluginPath, false);

            // We are only interested in compressed files that have no directory with the same name in the plugin-directory, these are the new plugins that should be expanded, verified, and loaded. Files with directories are handled by the code above.
            if (directoryPath == null) {
              directoryPath = expandPlugin(pluginPath);
              // If expansion of plugin was successfull, verify and load plugin, otherwise skip plugin.
              if (directoryPath != null) {
                try {

                  PluginDesc plugin = parseAndVerifyPlugin(directoryPath, pluginPath);

                  startPlugin(plugin, true);

                } catch (IllegalArgumentException | JelException ex) {
                  logger.error("Failed to load plugin '{}'.", pluginPath, ex);
                }
              }
            }
          }
        }
      });
    } catch (IOException ex) {
      throw new JelException(ex);
    }

    logger.debug("Successfully scanned for available plugins. Found {} plugins.", installedPlugins.size());
  }

  /**
   * Get the compressed plugin-file from the path of a expanded plugin-directory. Returns null if no compressed plugin-file exists.
   *
   * @param directoryPath path to plugin-directory.
   * @return path to compressed plugin-file.
   */
  private Path getOriginalFileFromDirPath(Path directoryPath) {
    Path originalFile = Paths.get(directoryPath.getParent().toString(), directoryPath.getFileName().toString() + ".jar");

    if (!Files.exists(originalFile, LinkOption.NOFOLLOW_LINKS)) {
      originalFile = Paths.get(directoryPath.getParent().toString(), directoryPath.getFileName().toString() + ".zip");

      if (!Files.exists(originalFile, LinkOption.NOFOLLOW_LINKS)) {
        originalFile = null;
      }
    }

    return originalFile;
  }

  /**
   * Get the directory path from the compressed plugin-file path. Returns null if no directory path exists.
   *
   * @param pluginPath path to plugin-file.
   * @param skipCheck skip check if directory exists in filesystem.
   * @return path to expanded plugin-directory.
   */
  private Path getDirectoryPathFromFile(Path pluginPath, boolean skipCheck) {
    String filename = pluginPath.getFileName().toString();
    filename = filename.substring(0, filename.lastIndexOf("."));

    Path directory = Paths.get(pluginPath.getParent().toString(), filename);

    if (!skipCheck) {
      if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
        directory = null;
      }
    }

    return directory;
  }

  /**
   * Parse and verifies that the plugin contains all required components of a valid plugin.
   *
   * @param pluginPath path to existing plugin-directory.
   * @param originalFile path to original compressed file containing the plugin.
   * @return metadata about the parsed and verified plugin.
   * @throws JelException if parsing or verification of plugin fails.
   */
  private PluginDesc parseAndVerifyPlugin(Path pluginPath, Path originalFile) {

    Path metaPath = Paths.get(pluginPath.toString(), META_FILENAME);
    // Check if we have a "plugin.json"-file containing information about the plugin.
    if (!Files.exists(metaPath, LinkOption.NOFOLLOW_LINKS)) {
      throw new JelException(String.format("Missing description-file('%s') in directory '%s', this is not a plugin.", META_FILENAME, pluginPath));
    }
    // Parse the plugin.json-file.
    byte[] jsonData = null;
    try {
      jsonData = Files.readAllBytes(metaPath);
    } catch (IOException ex) {
      throw new JelException(String.format("Failed to read description-file('%s') for plugin '%s'.", META_FILENAME, pluginPath), ex);
    }

    if (jsonData == null || jsonData.length == 0) {
      throw new JelException(String.format("Description-file('%s') for plugin '%s' appears to be empty.", META_FILENAME, pluginPath));
    }

    ObjectMapper objectMapper = new ObjectMapper();
    PluginDesc plugin = null;

    try {
      plugin = objectMapper.readValue(jsonData, PluginDesc.class);
    } catch (IOException ex) {
      throw new JelException(String.format("Failed to parse description-file('%s') for plugin '%s'.", META_FILENAME, pluginPath), ex);
    }

    logger.debug("Found plugin '{}' in directory {}.", plugin.getName(), pluginPath);

    // Set these as they doesn't come with the file.
    plugin.setDirectoryPath(pluginPath);
    plugin.setOriginalFile(originalFile);
    plugin.setFileChecksum(FileUtils.fileChecksum(originalFile.toFile(), "SHA-1"));
    // Check if all required fields are set.
    plugin.validate();

    if (!plugin.getName().equals(pluginPath.getFileName().toString())) {
      logger.warn("Directory name({}) and original file name({}) do not match the plugin name('{}') found in description-file('{}'), this should be fixed by plugin author!", pluginPath, plugin.getOriginalFile(), plugin.getName(), META_FILENAME);
    }

    if (SystemInfo.compareToServerVersion(plugin.getMinServerVersion()) > 0) {
      throw new JelException(String.format("Your current installation of JEL is to old for this plugin. Plugin requires atleast version %s, you have version %s. Skipping plugin.", plugin.getMinServerVersion(), SystemInfo.getVersion()));
    }

    return plugin;
  }

  /**
   * Start an existing and decompressed plugin. This method will add the plugin to the classpath and call upon its start-method in the main class.
   *
   * @param plugin the plugin to start.
   * @param newInstalled flag that signals that the plugin has just been installed.
   * @throws JelExeption if startup fails.
   */
  private void startPlugin(PluginDesc plugin, boolean newInstalled) {

    if (plugin.getMainClass().endsWith(".js")) {
      // TODO: For Javascript-plugins, get a script-instance and execute scripts start-function.
    } else {
      try {
        PluginClassLoader pluginClassloader = new PluginClassLoader(classLoader);
        pluginClassloader.addDirectory(plugin.getDirectoryPath());
        // Fault finding.
        if (logger.isTraceEnabled()) {
          printClassLoaderTree(classLoader);
        }

        Class pluginClass = pluginClassloader.loadClass(plugin.getMainClass());
        Plugin pluginInstance = (Plugin) pluginClass.newInstance();

        if (newInstalled) {
          pluginInstance.pluginInstall();
          JelService.vertx().eventBus().publish(EVENTBUS_PLUGINS, plugin.getName(), new DeliveryOptions().addHeader("action", Plugin.EVENT_PLUGIN_INSTALLED));
        }

        pluginInstance.pluginStart();

        this.classloaders.put(plugin, pluginClassloader);
        this.installedPlugins.put(plugin.getName(), plugin);
        this.loadedPlugins.put(plugin, pluginInstance);

        JelService.vertx().eventBus().publish(EVENTBUS_PLUGINS, plugin.getName(), new DeliveryOptions().addHeader("action", Plugin.EVENT_PLUGIN_STARTED));

        // Special care if plugin is an adapter, register it using the adapter manager.
        if (plugin.getCategory() == PluginDesc.Category.ADAPTER) {
          JelService.adapterManager().registerAdapterTypePlugin(plugin);
        }
      } catch (ClassNotFoundException cnfe) {
        throw new JelException(String.format("Failed to create an instance of the plugin, %s, the mainClass-property('%s') in the descriptionfile may point a non-existing class.", plugin.getDirectoryPath(), plugin.getMainClass()), cnfe);
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException ex) {
        throw new JelException(ex.getMessage(), ex);
      }
    }

    logger.info("Started plugin '{}'.", plugin.getName());
  }

  /**
   * Print out what's in the classloader tree being used.
   *
   * @param cl classloader
   * @return depth
   */
  private static int printClassLoaderTree(ClassLoader cl) {
    int depth = 0;
    if (cl.getParent() != null) {
      depth = printClassLoaderTree(cl.getParent()) + 1;
    }
    StringBuffer indent = new StringBuffer();
    for (int i = 0; i < depth; i++) {
      indent.append("  ");
    }
    if (cl instanceof URLClassLoader) {
      URLClassLoader ucl = (URLClassLoader) cl;
      logger.trace(indent + cl.getClass().getName() + " {");
      URL[] urls = ucl.getURLs();
      for (URL url : urls) {
        logger.trace(indent + "  " + url);
      }
      logger.trace(indent + "}");
    } else {
      logger.trace(indent + cl.getClass().getName());
    }
    return depth;
  }

  /**
   * Setup monitor for filechanges in plugin-directory.
   */
  private void setupDirectoryWatch() {

    filewatchExecutorService = Executors.newSingleThreadExecutor();
    filewatchExecutorService.execute(() -> {
      try {
        logger.debug("Setting up monitor of filechanges in plugin-directory '{}'.", pluginsPath);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

          pluginsPath.register(watchService,
              StandardWatchEventKinds.ENTRY_CREATE,
              StandardWatchEventKinds.ENTRY_MODIFY,
              StandardWatchEventKinds.ENTRY_DELETE);

          List<String> processedFiles = new ArrayList<>();

          while (true) {
            final WatchKey key = watchService.take();
            processedFiles.clear(); // Clear list every time we detect changes, since already processed files may have been changed again.

            Thread.sleep(250);  // This delay should not be necessary , but adding it proved to give more accurate readings.

            // Iterate over pending events.
            for (WatchEvent<?> watchEvent : key.pollEvents()) {

              final Kind<?> kind = watchEvent.kind();

              // Skip OVERFLOW-event
              if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue;
              }

              final WatchEvent<Path> watchEventPath = (WatchEvent<Path>) watchEvent;
              final Path fullPath = ((Path) key.watchable()).resolve(watchEventPath.context());
              String filename = fullPath.toString();

              // We only support/are intressted in these. We also add a check if the plugin already has been processed, since sometimes file changes triggers both a file-create and a file-modify.
              if ((filename.endsWith(".jar") || filename.endsWith(".zip")) && !processedFiles.contains(filename)) {

                logger.debug("Detected change in plugin-directory, kind: {}, file: {}", kind.toString(), filename);

                PluginDesc existingPlugin = null;
                Optional<PluginDesc> possiblePlugin = this.installedPlugins.values().stream().filter(
                    x -> x.getOriginalFile().equals(fullPath)
                ).findFirst();

                if (possiblePlugin.isPresent()) {
                  existingPlugin = possiblePlugin.get();
                }

                // Changing an existing file does many times generate a ENTRY-CREATE-event so we capture both here and find out later if it's a new plugin or if it's an update.
                if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                  if (existingPlugin == null) {
                    logger.info("Plugin({}) detected in plugin-directory.", fullPath);
                    installPlugin(fullPath);
                  } else {
                    logger.info("Updated plugin({}) detected in plugin-directory.", existingPlugin.getOriginalFile());
                    updatePlugin(existingPlugin);
                  }
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                  // If plugin-file has been removed but we don't find the plugin in our internal list of installed plugins, skip uninstall step.
                  if (existingPlugin != null) {
                    logger.info("Removed plugin({}) detected in plugin-directory.", existingPlugin.getOriginalFile());
                    uninstallPlugin(existingPlugin);
                  }
                }
                // Record that we have processed this file to skip subsequent events for this file.
                processedFiles.add(filename);
              }

              // Putting the Key Back in Ready State. reset the key. If not we won't get any new notifications.
              key.reset();

              // Exit loop if the key is not valid (if the directory was deleted, for example)
              if (!key.isValid()) {
                break;
              }
            }
          }
        } catch (InterruptedException exception) {
          // Do nothing.
        }
      } catch (IOException ex) {
        throw new JelException("An error occured while monitoring changes of the plugin-directory.", ex);
      }
    });
  }

  /**
   * Expands (decompresses) a plugin-file and create a directory with it's name Returns the expanded plugin path, or null if expansion failed.
   *
   * @param pluginPath plugin to expand.
   * @return path to new directory.
   */
  private Path expandPlugin(Path pluginPath) {
    Path targetDirectory = null;

    try {

      targetDirectory = getDirectoryPathFromFile(pluginPath, true);
      FileUtils.unzip(pluginPath, targetDirectory);

    } catch (IOException ex) {
      logger.error("Failed to expand plugin '{}', skipping plugin. But first we try to remove any resources that may have been expanded.", pluginPath, ex);

      if (targetDirectory != null) {
        try {
          FileUtils.deleteDirectory(targetDirectory);
        } catch (IOException ex1) {
          logger.error("Failed to remove parts of an expanded plugin. Please try to remove this directory manually: {}", targetDirectory, ex1);
        }
      }

      targetDirectory = null;
    }

    return targetDirectory;
  }

  /**
   * Installs a new plugin. File should exist in the plugin-directory, this method decompresses files, collect metadata about plugin, and instantiate plugin. A
   * existing version of this plugin must not already be installed!
   *
   * @param filePath Path to plugin-file to install.
   * @throws JelException if installation fails.
   */
  private void installPlugin(Path pluginPath) {

    Path existingPluginDirectory = getDirectoryPathFromFile(pluginPath, false);

    // If an old directory already exists, remove it first before expanding the new plugin.
    if (existingPluginDirectory != null && Files.exists(existingPluginDirectory, LinkOption.NOFOLLOW_LINKS)) {
      logger.info("Removing stale plugin directory: {}", existingPluginDirectory);

      try {
        FileUtils.deleteDirectory(existingPluginDirectory);
      } catch (IOException ex) {
        throw new JelException(String.format("Failed to remove stale plugin directory(%s), aborting installation of plugin!", existingPluginDirectory), ex);
      }
    }

    Path directoryPath = expandPlugin(pluginPath);
    // If expansion of plugin was successfull, verify and load plugin, otherwise skip plugin.
    if (directoryPath != null) {
      try {

        PluginDesc plugin = parseAndVerifyPlugin(directoryPath, pluginPath);

        startPlugin(plugin, true);

      } catch (IllegalArgumentException | JelException ex) {
        // Note: we don't delete the directory of the expanded plugin even though it fails to load, this is to save startup time. If a lot of non-working plugins exists only as compressed files, we are trying to decompress them at startup every time, which makes the startup slow.
        logger.error("Failed to load plugin '{}'.", pluginPath, ex);
      }
    }
  }

  /**
   * Updates already installed plugin. File should exist in the plugin-directory, this method decompresses files, updates metadata about plugin, and
   * reinstantiates plugin.
   *
   * @param plugin PluginDesc to update
   */
  private void updatePlugin(PluginDesc plugin) {
    //TODO: Implement!
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Uninstalls an installed plugin.
   *
   * @param plugin PluginDesc to uninstall
   */
  private void uninstallPlugin(PluginDesc desc) {

    try {
      Plugin plugin = loadedPlugins.get(desc);

      if (plugin != null) {
        loadedPlugins.remove(desc);

        plugin.pluginStop();
        Thread.sleep(500);  // Give it some time to stop.
        JelService.vertx().eventBus().publish(EVENTBUS_PLUGINS, desc.getName(), new DeliveryOptions().addHeader("action", Plugin.EVENT_PLUGIN_STOPPED));

        plugin.pluginUninstall();
      }

      installedPlugins.remove(desc.getName());

      try (PluginClassLoader classloader = classloaders.get(desc)) {
        if (classloader != null) {
          classloader.unloadJarFiles();
          classloaders.remove(desc);
        }
      }

      Thread.sleep(1500); // Give it some time to uninstall before we remove all files.

      System.gc();

      // Remove files in the filesystem.
      Files.deleteIfExists(desc.getOriginalFile());
      FileUtils.deleteDirectory(desc.getDirectoryPath());

      logger.info("Plugin '{}' successfully uninstalled.", desc.getName());
      JelService.vertx().eventBus().publish(EVENTBUS_PLUGINS, desc.getName(), new DeliveryOptions().addHeader("action", Plugin.EVENT_PLUGIN_UNINSTALLED));

    } catch (InterruptedException | IOException ex) {
      logger.warn("Failed to uninstall plugin '{}'.", desc.getName(), ex);
    }
  }

  /**
   * Get the list of installed plugins
   *
   * @return List of installed plugins.
   */
  public synchronized List<PluginDesc> getInstalledPlugins() {
    return new ArrayList(this.installedPlugins.values());
  }

  /**
   * Get list of loaded plugins instances
   *
   * @return List of plugins instances.
   */
  public synchronized List<Plugin> getLoadedPlugins() {
    return new ArrayList(this.loadedPlugins.values());
  }

  /**
   * Get plugin instance from plugin description
   *
   * @param desc plugin description
   * @param copy return a copy of the plugin instance.
   * @return plugin instance.
   */
  public synchronized Plugin getPluginsInstance(PluginDesc desc, boolean copy) {
    Plugin plugin = this.loadedPlugins.get(desc);

    if (copy) {
      try {
        Class<?> c = plugin.getClass();
        Constructor<?> cons = c.getConstructor();
        plugin = (Plugin) cons.newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex) {
        logger.error("Failed to create a new instance of a plugin.", ex);
      }
    }

    return plugin;
  }

  public List<Plugin> getAvailablePluginsToInstall() {
    //Get list of all remote plugins, filter out those that's already installed.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public List<Plugin> getAvailablePluginsToUpdate() {
    //Get list of remote plugins that are newer than already installed plugins.
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Install provided plugins
   *
   * @param plugins Plugins to install.
   */
  public void installPlugins(List<PluginDesc> plugins) {
    // Enter a list of remote plugins to install. Download them from repository, and install them. Return those who was successfully installed.
    // Also load required parent-plugins!  This should be done in a synchronized-block I guess.
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Update provided plugins (if newer versions available
   *
   * @param plugins Plugins to update.
   */
  public void updatePlugins(List<PluginDesc> plugins) {
    // Enter a list of remote plugins to update. Check if newer versions is available. Download them from repository, and install them. Return those who was successfully updated.
    // Also update required parent-plugins!  This should be done in a synchronized-block I guess.
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   * Uninstall provided plugins.
   *
   * @param plugins Plugins to uninstall.
   */
  public void uninstallPlugin(List<PluginDesc> plugins) {
    if (plugins == null || plugins.isEmpty()) {
      return;
    }

    for (PluginDesc plugin : plugins) {
      uninstallPlugin(plugin);
    }
  }
}
