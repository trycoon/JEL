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
//
// Please note!
// Much of the code below is borrowed from https://github.com/Pi4J/pi4j/blob/master/pi4j-core/src/main/java/com/pi4j/system/SystemInfo.java
//
//TODO: Use https://support.hyperic.com/display/SIGAR/Home
package se.liquidbytes.jel;

import com.pi4j.system.SystemInfo.BoardType;
import static com.pi4j.system.SystemInfo.getRevision;
import com.pi4j.util.ExecUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 *
 * @author Henrik Östman
 */
public class SystemInfo {

  private final static String VERSION_FILE = "VERSION";
  private final static String BUILDNUMBER_FILE = "buildNumber.properties";
  private final static String uptimeStart = LocalDateTime.now().toString();
  // Cache some frequent used information.
  private static Map<String, String> cpuInfo;
  private static String buildNumber;
  private static String jelVersion;

  /**
   * Default constructor. Prevent creating instanses of this class, all access is made through static methods.
   */
  private SystemInfo() {
    // Nothing.
  }

  //--------------------------------------------------------------------------
  //---- Application specific
  //--------------------------------------------------------------------------
  /**
   * Get version of JEL. eg. "1.0.123" (semver.org)
   *
   * @return version-string
   */
  public static String getVersion() throws JelException {
    if (jelVersion != null) {
      return jelVersion;
    } else {
      synchronized (Settings.class) {
        if (jelVersion != null) {
          return jelVersion;
        } else {

          URL url = Settings.class.getClassLoader().getResource(VERSION_FILE);

          if (url == null) {
            throw new JelException(String.format("Missing %s-file, corrupt installation?", VERSION_FILE));
          }

          try (
              BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            jelVersion = reader.readLine();

          } catch (Throwable ex) {
            throw new JelException(String.format("Failed to read application version from %s-file.", VERSION_FILE), ex);
          }

          return jelVersion;
        }
      }
    }
  }

  /**
   * Get buildnumber (Git SHA-1) for this build of JEL. eg. "35d99574ef081d6a00ec126db776b87ce0a66c22".
   *
   * @return buildnumber-string
   */
  public static String getBuildNumber() throws JelException {
    if (buildNumber != null) {
      return buildNumber;
    } else {
      synchronized (Settings.class) {
        if (buildNumber != null) {
          return buildNumber;
        } else {

          InputStream inputStream = Settings.class.getClassLoader().getResourceAsStream(BUILDNUMBER_FILE);

          try {
            Properties properties = new Properties();
            properties.load(inputStream);
            buildNumber = properties.getProperty("git-sha-1");
          } catch (IOException ex) {
            throw new JelException(String.format("Failed to read buildnumber value from %s-file.", BUILDNUMBER_FILE), ex);
          } finally {
            if (inputStream != null) {
              try {
                inputStream.close();
              } catch (IOException ex) {
                // Ignore
              }
            }
          }

          return buildNumber;
        }
      }
    }
  }

  /**
   * Compare the provided version to the JEL server version.
   *
   * @param version semver string (semver.org) , e.g. "1.0.0" or "2.10.1".
   * @return negative number if the provided version is less than server version, positive number if greater, and 0 if equal.
   */
  public static int compareToServerVersion(String version) {
    DefaultArtifactVersion serverVersion = new DefaultArtifactVersion(getVersion());
    DefaultArtifactVersion testVersion = new DefaultArtifactVersion(version);

    return testVersion.compareTo(serverVersion);
  }

  /**
   * Get epoc-timestamp on application start.
   *
   * @return readable datetime string.
   */
  public static String GetApplicationStarttime() {
    return uptimeStart;
  }

  /**
   * Generate a startup splashscreen with useful information about the system.
   *
   * @return System information on a long string with lot's of linebreaks.
   */
  public static String getStartupInformation() {
    StringBuilder builder = new StringBuilder();
    builder.append("\n");

    builder.append("     ██╗███████╗██╗     ").append("\n");
    builder.append("     ██║██╔════╝██║     ").append("\n");
    builder.append("     ██║█████╗  ██║     ").append("\n");
    builder.append("██   ██║██╔══╝  ██║     ").append("\n");
    builder.append("╚█████╔╝███████╗███████╗").append("\n");
    builder.append(" ╚════╝ ╚══════╝╚══════╝").append("\n");
    builder.append("\n");
    builder.append("__[SYSTEM INFORMATION]____________________________").append("\n").append("\n");
    builder.append("JEL version: ").append(getVersion()).append("\n");
    builder.append("JEL buildnumber: ").append(getBuildNumber()).append("\n");
    builder.append("\n");
    builder.append("OS name: ").append(getOsName()).append("\n");
    builder.append("OS architecture: ").append(getOsArchitecture()).append("\n");
    builder.append("OS description: ").append(getOsDescription()).append("\n");
    builder.append("OS version: ").append(getOsVersion()).append("\n");
    builder.append("Java virtual machine: ").append(getJavaVirtualMachine()).append("\n");
    builder.append("Java vendor: ").append(getJavaVendor()).append("\n");
    builder.append("Java version: ").append(getJavaVersion()).append("\n");
    builder.append("Java home: ").append(getJavaHome()).append("\n");
    builder.append("Available CPUs: ").append(getAvailableCPUs()).append("\n");
    builder.append("Free Java memory: ").append(getJavaFreeMemory() / 1048576).append(" MiB of ").append(getJavaTotalMemory() / 1048576).append(" MiB").append("\n");
    builder.append("Free system memory: ").append(getMemoryFree() / 1048576).append(" MiB of ").append(getMemoryTotal() / 1048576).append(" MiB").append("\n");
    builder.append("User language: ").append(getUserLanguage()).append("\n");
    builder.append("IP address: ").append(getIP()).append("\n");
    builder.append("Gateway address: ").append(getGatewayIP()).append("\n");
    builder.append("Server endpoint: ").append(Settings.getServerEndpoint()).append("\n");
    builder.append("==================================================").append("\n");

    return builder.toString();
  }

  /**
   * Get useful system information. This information is mostly static (does not change frequently).
   *
   * @return Document with system information.
   */
  public static JsonObject getSystemInformation() {
    JsonObject info = new JsonObject();

    info.put("applicationVersion", getVersion());
    info.put("applicationBuildnumber", getBuildNumber());
    info.put("applicationStarttime", GetApplicationStarttime());
    info.put("serverCurrenttime", LocalDateTime.now().toString());

    JsonObject java = new JsonObject();
    java.put("virtualMachine", getJavaVirtualMachine());
    java.put("vendor", getJavaVendor());
    java.put("version", getJavaVersion());
    java.put("javaHome", getJavaHome());
    java.put("runtime", getJavaRuntime());
    java.put("specificationName", getJavaSpecificationName());
    info.put("java", java);

    JsonObject os = new JsonObject();
    os.put("name", getOsName());
    os.put("architecture", getOsArchitecture());
    os.put("description", getOsDescription());
    os.put("version", getOsVersion());
    info.put("os", os);

    JsonObject hardware = new JsonObject();
    hardware.put("availableCPUs", getAvailableCPUs());
    hardware.put("ipAddress", getIP());
    hardware.put("gatewayAddress", getGatewayIP());
    hardware.put("serverEndpoint", Settings.getServerEndpoint());
    hardware.put("bogoMIPS", getBogoMIPS());
    info.put("hardware", hardware);

    if (isRaspberryPi()) {
      JsonObject raspberry = new JsonObject();
      raspberry.put("boardType", getRaspberryBoardType());
      raspberry.put("clockFrequencyArm", getRaspberryClockFrequencyArm());
      raspberry.put("clockFrequencyCore", getRaspberryClockFrequencyCore());
      raspberry.put("clockFrequencyH264", getRaspberryClockFrequencyH264());
      raspberry.put("clockFrequencyPWM", getRaspberryClockFrequencyPWM());
      raspberry.put("clockFrequencyUART", getRaspberryClockFrequencyUART());
      raspberry.put("codecH264Enabled", getRaspberryCodecH264Enabled());
      raspberry.put("codecMPG2Enabled", getRaspberryCodecMPG2Enabled());
      raspberry.put("codecWVC1Enabled", getRaspberryCodecWVC1Enabled());
      raspberry.put("cpuArchitecture", getRaspberryCpuArchitecture());

      JsonArray features = new JsonArray();
      for (String feature : getRaspberryCpuFeatures()) {
        features.add(feature);
      }
      raspberry.put("cpuFeatures", features);

      raspberry.put("cpuImplementer", getRaspberryCpuImplementer());
      raspberry.put("cpuPart", getRaspberryCpuPart());
      raspberry.put("cpuRevision", getRaspberryCpuRevision());
      raspberry.put("cpuVariant", getRaspberryCpuVariant());
      raspberry.put("cpuVoltage", getRaspberryCpuVoltage());
      raspberry.put("hardware", getRaspberryHardware());
      raspberry.put("osFirmwareBuild", getRaspberryOsFirmwareBuild());
      raspberry.put("osFirmwareDate", getRaspberryOsFirmwareDate());
      raspberry.put("revision", getRaspberryRevision());
      raspberry.put("serial", getRaspberrySerial());
      hardware.put("raspberry", raspberry);
    }

    return info;
  }

  /**
   * Get information about system resources (CPU, memory, disk...). This information DOES change frequently.
   *
   * @return Document with information about system resources.
   */
  public static JsonObject getSystemResources() {
    JsonObject resources = new JsonObject();

    JsonObject java = new JsonObject();
    java.put("freeMemory", getJavaFreeMemory());
    java.put("totalMemory", getJavaTotalMemory());
    resources.put("java", java);

    JsonObject memory = new JsonObject();
    memory.put("free", getMemoryFree());
    memory.put("total", getMemoryTotal());
    resources.put("memory", memory);

    JsonObject disk = new JsonObject();
    disk.put("fullness", getDiskFull());
    resources.put("disk", disk);

    JsonObject cpu = new JsonObject();
    cpu.put("temperature", getCpuTemperature());
    cpu.put("loadAverage", getLoadAverage());
    resources.put("cpu", cpu);

    return resources;
  }

  //--------------------------------------------------------------------------
  //---- Java
  //--------------------------------------------------------------------------
  /**
   * The directory in which Java is installed
   *
   * @return
   */
  public static String getJavaHome() {
    return System.getProperty("java.home");
  }

  /**
   *
   * @return
   */
  public static String getJavaVirtualMachine() {
    return System.getProperty("java.vm.name");
  }

  /**
   *
   * @return
   */
  public static String getJavaRuntime() {
    return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("java.runtime.name"));
  }

  /**
   *
   * @return
   */
  public static String getJavaVersion() {
    return System.getProperty("java.version");
  }

  /**
   *
   * @return
   */
  public static String getJavaVendor() {
    return System.getProperty("java.vm.vendor");
  }

  /**
   *
   * @return
   */
  public static String getJavaVendorUrl() {
    return System.getProperty("java.vendor.url");
  }

  /**
   *
   * @return
   */
  public static String getJavaSpecificationName() {
    return System.getProperty("java.specification.name");
  }

  /**
   *
   * @return
   */
  public static String getJavaEndorsedDirectory() {
    return System.getProperty("java.endorsed.dirs");
  }

  /**
   *
   * @return
   */
  public static long getJavaTotalMemory() {
    return Runtime.getRuntime().totalMemory();
  }

  /**
   *
   * @return
   */
  public static long getJavaFreeMemory() {
    return Runtime.getRuntime().freeMemory();
  }

  //--------------------------------------------------------------------------
  //---- Operating system
  //--------------------------------------------------------------------------
  /**
   *
   * @return
   */
  public static String getOsArchitecture() {
    return System.getProperty("os.arch");
  }

  /**
   *
   * @return
   */
  public static String getOsName() {
    return System.getProperty("os.name");
  }

  /**
   *
   * @return
   */
  public static boolean isLinux() {
    return (getOsName().toLowerCase().contains("linux"));
  }

  /**
   *
   * @return
   */
  public static boolean isRaspberryPi() {
    // TODO: We need a better way!
    return (isLinux() && Files.exists(Paths.get("/opt/vc/bin/vcgencmd"), LinkOption.NOFOLLOW_LINKS));
  }

  /**
   *
   * @return
   */
  public static boolean isWindows() {
    return (getOsName().toLowerCase().contains("windows"));
  }

  /**
   *
   * @return
   */
  public static boolean isMacOSX() {
    String osName = getOsName().toLowerCase();
    return (osName.contains("mac os x") || osName.contains("darwin"));
  }

  /**
   *
   * @return
   */
  public static boolean isSolaris() {
    String name = getOsName().toLowerCase();
    return ((name.contains("sunos")) || (name.contains("solaris")));
  }

  /**
   *
   * @return
   */
  public static String getOsVersion() {
    return System.getProperty("os.version");
  }

  /**
   * Get a human readable description of the operatingsystem.
   *
   * @return description
   */
  public static String getOsDescription() {
    String desc = "";

    if (isLinux()) {
      try {
        //TODO: Support Windows,Mac,Raspberry.
        String[] result = ExecUtil.execute("lsb_release -d");
        if (result != null && result.length > 0) {
          desc = result[0].split("Description:")[1].trim();  //  Description:	Ubuntu 14.04.2 LTS
        }
      } catch (IOException | InterruptedException ex) {
        //Ignore
      }
    }

    return desc;
  }

  /**
   * Returns the systems loadaverage. A negative number is returned if not supported by platform.
   *
   * @return Average systemload.
   */
  public static double getLoadAverage() {
    return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
  }

  /**
   * The current working directory when the properties were initialized
   *
   * @return
   */
  public static String getWorkingDirectory() {
    return System.getProperty("user.dir");
  }

  /**
   * The directory in which java should create temporary files
   *
   * @return
   */
  public static String getTempDirectory() {
    return System.getProperty("java.io.tmpdir");
  }

  /**
   * The home directory of the current user
   *
   * @return
   */
  public static String getUserHome() {
    return System.getProperty("user.home");
  }

  /**
   * The username of the current user
   *
   * @return
   */
  public static String getUserName() {
    return System.getProperty("user.name");
  }

  /**
   * The two-letter language code of the default locale
   *
   * @return
   */
  public static String getUserLanguage() {
    return System.getProperty("user.language");
  }

  /**
   *
   * @return
   */
  public static String getUserCountry() {
    return System.getProperty("user.country");
  }

  /**
   * The default time zone
   *
   * @return
   */
  public static String getUserTimezone() {
    return System.getProperty("user.timezone");
  }

  /**
   * The value of the CLASSPATH environment variable
   *
   * @return
   */
  public static String getClassPath() {
    return System.getProperty("java.class.path");
  }

  /**
   * Get your servers local IP-address. May return null if not found.
   *
   * @return IP-address
   */
  public static String getIP() {
    // If using a Linux system get address from ip-command, it will give you the address used to reach Internet.
    if (SystemInfo.isLinux()) {
      try {
        String[] result = ExecUtil.execute("ip route get 8.8.8.8");
        // 8.8.8.8 via 192.168.10.254 dev eth0  src 192.168.10.100
        if (result != null && result.length > 0) {
          String[] parts = result[0].split(" ");
          return parts[7];
        }
      } catch (IOException | InterruptedException ex) {
        //Ignore
      }
    } else {
      // For other systems, use a more general but less accurate way (if we have more than one NIC we may end up getting the wrong one).
      Enumeration<NetworkInterface> nics;
      try {
        nics = NetworkInterface.getNetworkInterfaces();
      } catch (SocketException ex) {
        return null;
      }
      NetworkInterface netinf;
      while (nics.hasMoreElements()) {
        netinf = nics.nextElement();

        Enumeration<InetAddress> addresses = netinf.getInetAddresses();

        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (!address.isAnyLocalAddress() && !address.isLoopbackAddress()
              && !address.isMulticastAddress() && !(address instanceof Inet6Address)) {
            return address.getHostAddress();
          }
        }
      }
    }

    return null;
  }

  /**
   * Get your gateways internal IP-address. May return null if not found.
   *
   * @return IP-address
   */
  public static String getGatewayIP() {
    String address = null;

    if (SystemInfo.isLinux() || SystemInfo.isSolaris()) {
      try {
        String[] result = ExecUtil.execute("ip route get 8.8.8.8");
        if (result != null && result.length > 0) {
          String[] parts = result[0].split(" ");
          address = parts[2];
        }
      } catch (IOException | InterruptedException ex) {
        //Ignore
      }

      if (address == null) {
        try {
          String[] result = ExecUtil.execute("netstat -rn | grep \"0.0.0.0\" | awk '{print $2}'");
          if (result != null && result.length > 0) {
            address = result[0];
          }
        } catch (IOException | InterruptedException ex) {
          //Ignore
        }
      }
    } else if (SystemInfo.isMacOSX()) {
      try {
        String[] result = ExecUtil.execute("netstat -rn | grep \"default\" | awk '{print $2}'");
        if (result != null && result.length > 0) {
          address = result[0];
        }
      } catch (IOException | InterruptedException ex) {
        //Ignore
      }
    }

    return address;
  }

  /**
   * Get your servers/gateways public IP-address (the one visible from Internet). May return null if not found.
   *
   * @return IP-address
   */
  public static String getExternalIP() {
    String address = null;

    try {
      //TODO: Support Windows.
      String[] result = ExecUtil.execute("curl ipecho.net/plain");
      if (result != null && result.length > 0) {
        address = result[0];
      }
    } catch (IOException | InterruptedException ex) {
      //Ignore
    }

    return address;
  }

  //--------------------------------------------------------------------------
  //---- Hardware
  //--------------------------------------------------------------------------
  /**
   *
   * @param target
   * @param throwExceptionIfMissing
   * @return
   */
  private static String getCpuInfo(String target, boolean throwExceptionIfMissing) {
    // if the CPU data has not been previously acquired, then acquire it now
    if (cpuInfo == null) {
      try {
        cpuInfo = new HashMap<>();

        if (isLinux() || isMacOSX() || isSolaris()) {
          // This is UNIX-specific, return empty for Windows.
          String result[] = ExecUtil.execute("cat /proc/cpuinfo");
          if (result != null) {
            for (String line : result) {
              String parts[] = line.split(":", 2);
              if (parts.length >= 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                cpuInfo.put(parts[0].trim(), parts[1].trim());
              }
            }
          }
        }
      } catch (IOException | InterruptedException ex) {
        throw new JelException("Failed to get information about the systems processor.", ex);
      }
    }

    if (cpuInfo.containsKey(target)) {
      return cpuInfo.get(target);
    } else if (throwExceptionIfMissing) {
      throw new JelException(String.format("Missing target '%s' when quering cpuinfo.", target));
    } else {
      return "-";
    }
  }

  /**
   *
   * @param target
   * @return
   */
  private static String getCpuInfo(String target) {
    return getCpuInfo(target, false);
  }

  /**
   *
   * @return
   */
  public static String getBogoMIPS() {
    if (isRaspberryPi()) {
      return getCpuInfo("BogoMIPS");
    } else if (isLinux()) {
      return getCpuInfo("bogomips");
    } else {
      return "-";
    }
  }

  /**
   *
   * @return
   */
  public static float getCpuTemperature() {
    if (isRaspberryPi()) {
      // CPU temperature is in the form
      // pi@mypi$ /opt/vc/bin/vcgencmd measure_temp
      // temp=42.3'C
      // Support for this was added around firmware version 3357xx per info
      // at http://www.raspberrypi.org/phpBB3/viewtopic.php?p=169909#p169909
      try {
        String result[] = ExecUtil.execute("/opt/vc/bin/vcgencmd measure_temp");
        if (result != null) {
          for (String line : result) {
            String parts[] = line.split("[=']", 3);
            return Float.parseFloat(parts[1]);
          }
        }
      } catch (InterruptedException | IOException ex) {
        // Ignore
      }
    } else if (isLinux()) {
      //TODO: Fix
    }

    return -1;
  }

  /**
   *
   * @return
   */
  public static int getAvailableCPUs() {
    return Runtime.getRuntime().availableProcessors();
  }

  /**
   *
   * @return
   */
  private static List<Long> getMemory() {
    // Memory information is in the form
    // root@mypi:/home/pi# free -b
    //              total       used       free     shared    buffers     cached
    // Mem:     459771904  144654336  315117568          0   21319680   63713280
    // -/+ buffers/cache:   59621376  400150528
    // Swap:    104853504          0  104853504
    List<Long> values = new ArrayList<>();
    try {
      String result[] = ExecUtil.execute("free -b");
      if (result != null) {
        for (String line : result) {
          if (line.startsWith("Mem:")) {
            String parts[] = line.split(" ");
            for (String part : parts) {
              part = part.trim();
              if (!part.isEmpty() && !part.equalsIgnoreCase("Mem:")) {
                values.add(new Long(part));
              }
            }
          }
        }
      }
    } catch (InterruptedException | IOException ex) {
      // Ignore
    }
    return values;
  }

  /**
   *
   * @return
   */
  public static long getMemoryTotal() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 0) {
      return values.get(0); // total memory value is in first position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getMemoryUsed() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 1) {
      return values.get(1); // used memory value is in second position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getMemoryFree() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 2) {
      return values.get(2); // free memory value is in third position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getMemoryShared() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 3) {
      return values.get(3); // shared memory value is in fourth position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getMemoryBuffers() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 4) {
      return values.get(4); // buffers memory value is in fifth position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getMemoryCached() {
    List<Long> values = getMemory();
    if (!values.isEmpty() && values.size() > 5) {
      return values.get(5); // cached memory value is in sixth position
    }
    return -1;
  }

  /**
   *
   * @return
   */
  public static long getTotalDiskSpace() {
    return new File(getUserHome()).getTotalSpace();
  }

  /**
   *
   * @return
   */
  public static long getFreeDiskSpace() {
    return new File(getUserHome()).getUsableSpace();
  }

  /**
   * Get fill-percentage of disk, in two decimals.
   *
   * @return Percentage, eg. 78.01.
   */
  public static double getDiskFull() {
    long freeSpace = getFreeDiskSpace();
    long totalSpace = getTotalDiskSpace();
    long usedSpace = totalSpace - freeSpace;

    if (totalSpace == 0) // Avoid divide by zero.
    {
      return 100;
    } else {

      return new BigDecimal((double) usedSpace / totalSpace * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
  }

  //--------------------------------------------------------------------------
  //---- Raspberry Pi - Hardware specific
  //--------------------------------------------------------------------------
  /**
   *
   * @return
   */
  public static String[] getRaspberryCpuFeatures() {
    return getCpuInfo("Features").split(" ");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryCpuImplementer() {
    return getCpuInfo("CPU implementer");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryCpuArchitecture() {
    return getCpuInfo("CPU architecture");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryCpuVariant() {
    return getCpuInfo("CPU variant");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryCpuPart() {
    return getCpuInfo("CPU part");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryCpuRevision() {
    return getCpuInfo("CPU revision");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryHardware() {
    return getCpuInfo("Hardware");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryRevision() {
    return getCpuInfo("Revision");
  }

  /**
   *
   * @return
   */
  public static String getRaspberrySerial() {
    return getCpuInfo("Serial");
  }

  /**
   *
   * @return
   */
  public static String getRaspberryOsFirmwareBuild() {
    String build = "-";
    try {
      String result[] = ExecUtil.execute("/opt/vc/bin/vcgencmd version");
      if (result != null) {
        for (String line : result) {
          if (line.startsWith("version ")) {
            build = line.substring(8);
          }
        }
      }
    } catch (InterruptedException | IOException ex) {
      // Ignore
    }
    return build;
  }

  /**
   *
   * @return
   */
  public static String getRaspberryOsFirmwareDate() {
    String date = "-";
    try {
      String result[] = ExecUtil.execute("/opt/vc/bin/vcgencmd version");
      if (result != null) {
        for (String line : result) {
          date = line; // return 1st line
        }
      }
    } catch (InterruptedException | IOException ex) {
      // Ignore
    }
    return date;
  }

  /**
   *
   * @return
   */
  public static BoardType getRaspberryBoardType() {
    // The following info obtained from:
    // http://www.raspberrypi.org/archives/1929
    // http://raspberryalphaomega.org.uk/?p=428
    // http://www.raspberrypi.org/phpBB3/viewtopic.php?p=281039#p281039
    try {
      switch (getRevision()) {
        case "0002":  // Model B Revision 1
        case "0003":  // Model B Revision 1 + Fuses mod and D14 removed
          return BoardType.ModelB_Rev1;
        case "0004":  // Model B Revision 2 256MB (Sony)
        case "0005":  // Model B Revision 2 256MB (Qisda)
        case "0006":  // Model B Revision 2 256MB (Egoman)
          return BoardType.ModelB_Rev2;
        case "0007":  // Model A 256MB (Egoman)
        case "0008":  // Model A 256MB (Sony)
        case "0009":  // Model A 256MB (Qisda)
          return BoardType.ModelA_Rev0;
        case "000d":  // Model B Revision 2 512MB (Egoman)
        case "000e":  // Model B Revision 2 512MB (Sony)
        case "000f":  // Model B Revision 2 512MB (Qisda)
          return BoardType.ModelB_Rev2;
        default:
          return BoardType.UNKNOWN;
      }
    } catch (InterruptedException | IOException ex) {
      return BoardType.UNKNOWN;
    }
  }

  /**
   *
   * @param id
   * @return
   */
  private static float getRaspberryVoltage(String id) {
    try {
      String result[] = ExecUtil.execute("/opt/vc/bin/vcgencmd measure_volts " + id);
      if (result != null) {
        for (String line : result) {
          String parts[] = line.split("[=V]", 3);
          return Float.parseFloat(parts[1]);
        }
      }
    } catch (InterruptedException | IOException ex) {
      // Ignore
    }

    return -1;
  }

  /**
   *
   * @return
   */
  public static float getRaspberryCpuVoltage() {
    return getRaspberryVoltage("core");
  }

  /**
   *
   * @return
   */
  public static float getRaspberryMemoryVoltageSDRam_C() {
    return getRaspberryVoltage("sdram_c");
  }

  /**
   *
   * @return
   */
  public static float getRaspberryMemoryVoltageSDRam_I() {
    return getRaspberryVoltage("sdram_i");
  }

  /**
   *
   * @return
   */
  public static float getRaspberryMemoryVoltageSDRam_P() {
    return getRaspberryVoltage("sdram_p");
  }

  /**
   *
   * @return
   */
  public static boolean getRaspberryCodecH264Enabled() {
    try {
      return com.pi4j.system.SystemInfo.getCodecH264Enabled();
    } catch (InterruptedException | IOException ex) {
      return false;
    }
  }

  /**
   *
   * @return
   */
  public static boolean getRaspberryCodecMPG2Enabled() {
    try {
      return com.pi4j.system.SystemInfo.getCodecMPG2Enabled();
    } catch (InterruptedException | IOException ex) {
      return false;
    }
  }

  /**
   *
   * @return
   */
  public static boolean getRaspberryCodecWVC1Enabled() {
    try {
      return com.pi4j.system.SystemInfo.getCodecWVC1Enabled();
    } catch (InterruptedException | IOException ex) {
      return false;
    }
  }

  /**
   *
   * @param target
   * @return
   */
  private static long getRaspberryClockFrequency(String target) {
    try {
      String result[] = ExecUtil.execute("/opt/vc/bin/vcgencmd measure_clock " + target.trim());
      if (result != null) {
        for (String line : result) {
          String parts[] = line.split("=", 2);
          return Long.parseLong(parts[1].trim());
        }
      }
    } catch (InterruptedException | IOException ex) {
      // Ignore
    }

    return -1;

  }

  /**
   *
   * @return
   */
  public static long getRaspberryClockFrequencyArm() {
    return getRaspberryClockFrequency("arm");
  }

  /**
   *
   * @return
   */
  public static long getRaspberryClockFrequencyCore() {
    return getRaspberryClockFrequency("core");
  }

  /**
   *
   * @return
   */
  public static long getRaspberryClockFrequencyH264() {
    return getRaspberryClockFrequency("h264");
  }

  /**
   *
   * @return
   */
  public static long getRaspberryClockFrequencyUART() {
    return getRaspberryClockFrequency("uart");
  }

  /**
   *
   * @return
   */
  public static long getRaspberryClockFrequencyPWM() {
    return getRaspberryClockFrequency("pwm");
  }
}
