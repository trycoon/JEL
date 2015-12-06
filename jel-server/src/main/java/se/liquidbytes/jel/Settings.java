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
package se.liquidbytes.jel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henrik Östman
 */
public final class Settings {

  private final static String SETTINGS_FILE = "jel.properties";
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  // Cache some frequent used settings.
  private static Path storagePath;
  private static String serverEndpoint;
  private static Properties props;
  private static boolean isDebug;
  /**
   * Globaly used name on Eventbus, should be used by all verticles in this application to be able to communicate with each other.
   */
  public final static String EVENTBUS_NAME = "jel.eventbus";

  /**
   * Private default constructor. Prevent creating instanses of this class, all access is made through static methods.
   */
  private Settings() {
    // Nothing
  }

  /**
   * Initialize basic application settings. This must be run once at the very beginning of the application startup!
   *
   * @param args Arguments passed to the process at startup.
   */
  public static synchronized void init(String[] args) {
    // Check if we have already been initialized.
    if (props == null) {
      logger.info("Loading configuration and parsing settings");
      loadConfiguration(args);
    }
  }

  /**
   * Load configuration from file and process start-arguments.
   */
  private static void loadConfiguration(String[] args) {

    final ProcessArguments cliArguments = new ProcessArguments();
    final CmdLineParser parser = new CmdLineParser(cliArguments);
    try {
      parser.parseArgument(args);
    } catch (CmdLineException ex) {
      // Ignore unknown options.
    }

    if (cliArguments.help) {
      parser.printUsage(System.err);
      System.exit(0);
    }

    isDebug = cliArguments.isDebug;

    props = new Properties();
    InputStream inputStream = null;

    try {
      URL url = Settings.class.getClassLoader().getResource(SETTINGS_FILE);

      if (url == null) {
        throw new FileNotFoundException();
      }

      inputStream = url.openStream();
      props.load(inputStream);

      // Commandline settings override configfile.
      if (cliArguments.portNumber > 0) {
        props.setProperty("port", String.valueOf(cliArguments.portNumber));
      }

      if (cliArguments.storage != null && cliArguments.storage.length() > 0) {
        props.setProperty("storagepath", cliArguments.storage);
      }

      if (cliArguments.skipweb) {
        props.setProperty("skipweb", "true");
      }

      if (cliArguments.skipapi) {
        props.setProperty("skipapi", "true");
      }

      if (cliArguments.serverEndpoint != null && !cliArguments.serverEndpoint.isEmpty()) {
        serverEndpoint = cliArguments.serverEndpoint;
      } else {
        serverEndpoint = props.getProperty("serverEndpoint");
        if (serverEndpoint == null || serverEndpoint.isEmpty()) {
          serverEndpoint = String.format("%s%s:%s", "http://", SystemInfo.getIP(), props.getProperty("port"));
        }
      }
      props.setProperty("serverEndpoint", serverEndpoint);

      logger.info("Successfully loaded settings from file {}", url.toExternalForm());

    } catch (IOException ex) {
      throw new JelException(String.format("Failed to read settings from property file '%s', make sure it exists in class-path and are readable.", SETTINGS_FILE), ex);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          // Ignore
        }
      }
    }

    //
    // Check that vital setting has valid values before we allow server to start up.
    //
    if (props.getProperty("port") == null || !props.getProperty("port").matches("^[0-9]+$") || props.getProperty("port").equals("0")) {
      throw new JelException("No valid port-setting has been specified, please set the port-value in the jel.properties-file or start the application with the 'port'-parameter set.");
    }

    if (props.getProperty("storagepath") == null || props.getProperty("storagepath").length() == 0) {
      throw new JelException("No valid storagepath-setting has been specified, please set the storagepath-path in the jel.properties-file or start the application with the 'storagepath'-parameter set.");
    } else {
      try {
        storagePath = Paths.get(props.getProperty("storagepath"));
      } catch (InvalidPathException ex) {
        throw new JelException("Storagepath-setting had not a valid path-expression, make sure its correctly set.");
      }
    }

    if (!new File(storagePath.toString()).exists()) {
      try {
        Files.createDirectories(storagePath);
      } catch (Throwable ex) {
        throw new JelException(String.format("Failed to create missing storage-directory '%s', please check write-permissions to parent directory.", storagePath.toString()), ex);
      }
    } else {
      if (!new File(storagePath.toString()).canWrite() || !new File(storagePath.toString()).canRead()) {
        throw new JelException(String.format("Missing access-permissions to storage-directory '%s'. This directory and ALL its subdirectories MUST have Read and Write-permissions for the user(%s) running this application!", storagePath.toString(), SystemInfo.getUserName()));
      }
    }
  }

  /**
   * Return the config-value of the requested name.
   *
   * @param name Name of the setting
   * @return value
   */
  public static String get(String name) {
    return props.getProperty(name);
  }

  /**
   * Return the config-value of the requested name.
   *
   * @param name Name of the setting
   * @param defaultValue Default-value if no value found
   * @return value
   */
  public static String get(String name, String defaultValue) {
    return props.getProperty(name, defaultValue);
  }

  /**
   * Return the path where the application is supposed to store all its data-files (eg. database, uploaded files, and more..)
   *
   * @return Path
   */
  public static final Path getStoragePath() {
    return storagePath;
  }

  /**
   * Return the server endpoint that the webserver and API should be exposed under.
   *
   * @return endpoint
   */
  public static final String getServerEndpoint() {
    return serverEndpoint;
  }

  /**
   * If we are running in development/debug-mode
   *
   * @return If running in debug-mode
   */
  public static final boolean isDebug() {
    return isDebug;
  }
}
