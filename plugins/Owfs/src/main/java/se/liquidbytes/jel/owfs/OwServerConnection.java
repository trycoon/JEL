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
package se.liquidbytes.jel.owfs;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.owfs.jowfsclient.Enums;
import org.owfs.jowfsclient.OwfsConnectionConfig;
import org.owfs.jowfsclient.OwfsConnectionFactory;
import org.owfs.jowfsclient.OwfsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class handling the direct communication to/from owserver.
 *
 * @author Henrik Östman
 */
public class OwServerConnection {

  /**
   * Logghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  /**
   * Consecutive attempts before request fails.
   */
  private final static int MAX_ATTEMPTS = 5;
  /**
   * Connection to Owfs-driver
   */
  private org.owfs.jowfsclient.OwfsConnection owfs;
  /**
   * Host-setting for Owfs
   */
  private final String host;
  /**
   * Port-setting for Owfs
   */
  private final int port;
  /**
   * Attempt counter, used together with MAX_ATTEMPTS.
   */
  private int attempts;

  /**
   * Default constructor
   *
   * @param host Owserver host
   * @param port Owserver port
   */
  public OwServerConnection(String host, int port) {
    if (host == null || host.isEmpty()) {
      throw new OwServerUnhandledException("Missing host-parameter.");
    }

    if (port < 1) {
      throw new OwServerUnhandledException("Missing port-parameter.");
    }

    this.host = host;
    this.port = port;
    attempts = 1;
  }

  /**
   * Setup connection to owfs (owserver)
   */
  public void connect() {
    // Close existing connection if any.
    if (this.owfs != null) {
      try {
        owfs.disconnect();
      } catch (IOException ex) {
        // Ignore.
      }
      owfs = null;
    }

    OwfsConnectionFactory factory = new OwfsConnectionFactory(this.host, this.port);
    OwfsConnectionConfig config = factory.getConnectionConfig();
    config.setDeviceDisplayFormat(Enums.OwDeviceDisplayFormat.F_DOT_I); // Display devices in format "10.67C6697351FF" (family-code.id)
    config.setTemperatureScale(Enums.OwTemperatureScale.CELSIUS);       // Celsius is the default temperature-scale we use, we convert it in JEL to other formats if needed.
    config.setPersistence(Enums.OwPersistence.ON);                      // User a persistent connection to owserver if possible
    config.setBusReturn(Enums.OwBusReturn.OFF);                         // Only show the devices in directory-listings, we don't want to iterate over other directories (like "structure", "settings"...)
    factory.setConnectionConfig(config);
    this.owfs = factory.createNewConnection();
  }

  /**
   * Close connection to owfs (owserver)
   */
  public void close() {
    if (this.owfs != null) {
      try {
        this.owfs.disconnect();
      } catch (IOException ex) {
        logger.warn("Failed to disconnect from Owserver running at {}:{} when stopping Owserver-adapter.", this.host, this.port, ex);
      }
      this.owfs = null;
    }
  }

  /**
   * List all available devices on 1-wire bus.
   * @param useCache use Owservers internal cache, much faster but may contain old values.
   * @throws OwServerConnectionException if execution fails
   * @return Returns list of path for all found devices.
   */
  public List<String> listDirectory(boolean useCache) {
    List<String> owDevices = new ArrayList<>();

    if (owfs == null) {
      throw new OwServerUnhandledException(String.format("You forgot to run Connect() on OwServerConnection at %s:%s.", this.host, this.port));
    }

    try {
      owDevices = owfs.listDirectory(useCache ? "/" : "/uncached")
          .stream().filter(d -> d != null) // Filter out possible null-values.
          .map(d -> !d.contains("/uncached") ? "/uncached" + d : d) // Make sure we return the uncached path to the devices regardless of whether we list devices using cache or not.
          .collect(Collectors.toList());
      attempts = 1;
    } catch (SocketException ex) {
      if (attempts > MAX_ATTEMPTS) {
        throw new OwServerConnectionException(String.format("Failed to execute action \"listDirectory\" on Owserver running at %s:%s, connection seems down. Done trying to reconnect after %n attempts.", this.host, this.port, MAX_ATTEMPTS), ex);
      } else {
        attempts++;
        logger.warn("Failed to execute action \"listDirectory\" on Owserver running at {}:{}, connection seems down. Reconnect attempt# {}.", this.host, this.port, attempts, ex);

        connect();
        try {
          // Wait one sec, to not SPAM us to death.
          Thread.sleep(1000);
          return listDirectory(useCache);
        } catch (InterruptedException ex1) {
          // Do nothing.
        }
      }
    } catch (OwfsException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute action \"listDirectory\" on Owserver running at %s:%s, got errorcode", this.host, this.port, ex.getErrorCode()), ex);
    } catch (IOException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute action \"listDirectory\" on Owserver running at %s:%s.", this.host, this.port), ex);
    }

    return owDevices;
  }

  /**
   * Read value from device using path.
   *
   * @param path Path to read.
   * @return Read value.
   */
  public String read(String path) {
    String value = null;

    if (owfs == null) {
      throw new OwServerUnhandledException(String.format("You forgot to run Connect() on OwServerConnection at %s:%s.", this.host, this.port));
    }

    try {
      value = owfs.read(path);
      attempts = 1;
    } catch (SocketException ex) {
      if (attempts > MAX_ATTEMPTS) {
        throw new OwServerConnectionException(String.format("Failed to execute action \"read\" on Owserver running at %s:%s, connection seems down. Done trying to reconnect after %n attempts.", this.host, this.port, MAX_ATTEMPTS), ex);
      } else {
        attempts++;
        logger.warn("Failed to execute action \"read\" on Owserver running at {}:{}, connection seems down. Reconnect attempt# {}.", this.host, this.port, attempts, ex);

        connect();
        try {
          // Wait one sec, to not SPAM us to death.
          Thread.sleep(1000);
          return read(path);
        } catch (InterruptedException ex1) {
          // Do nothing.
        }
      }
    } catch (OwfsException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute action \"read\" on Owserver running at %s:%s, got errorcode", this.host, this.port, ex.getErrorCode()), ex);
    } catch (IOException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute action \"read\" on Owserver running at %s:%s.", this.host, this.port), ex);
    }

    return value;
  }

  /**
   * Set value on device using path.
   *
   * @param path Path to write to.
   * @param value Value to write/set.
   */
  public void write(String path, String value) {

    if (owfs == null) {
      throw new OwServerUnhandledException(String.format("You forgot to run Connect() on OwServerConnection at %s:%s.", this.host, this.port));
    }

    try {
      owfs.write(path, value);
      attempts = 1;
    } catch (SocketException ex) {
      if (attempts > MAX_ATTEMPTS) {
        throw new OwServerConnectionException(String.format("Failed to execute write to path '%s' with value '%s' on Owserver running at %s:%s, connection seems down. Done trying to reconnect after %n attempts.", path, value, this.host, this.port, MAX_ATTEMPTS), ex);
      } else {
        attempts++;
        logger.warn("Failed to execute write to path '%s' with value '%s' on Owserver running at {}:{}, connection seems down. Reconnect attempt# {}.", path, value, this.host, this.port, attempts, ex);

        connect();
        try {
          // Wait one sec, to not SPAM us to death.
          Thread.sleep(1000);
          write(path, value);
        } catch (InterruptedException ex1) {
          // Do nothing.
        }
      }
    } catch (OwfsException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute write to path '%s' with value '%s' on Owserver running at %s:%s, got errorcode", path, value, this.host, this.port, ex.getErrorCode()), ex);
    } catch (IOException ex) {
      throw new OwServerConnectionException(String.format("Failed to execute write to path '%s' with value '%s' on Owserver running at %s:%s.", path, value, this.host, this.port), ex);
    }
  }
}
