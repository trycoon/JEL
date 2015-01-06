/*
 * Copyright (c) 2014, Henrik Östman, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package se.liquidbytes.jel.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
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
import se.liquidbytes.jel.JelException;

/**
 *
 * @author Henrik Östman
 */
public final class Settings {

    private final static String VERSION_FILE = "VERSION";
    private final static String SETTINGS_FILE = "jel.properties";
    private final static long uptimeStart = System.currentTimeMillis();
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // Cache some frequent used settings.
    private static String jelVersion;
    private static Path storagePath;
    private static boolean debugmode;
    private static Properties props;

    /**
     * Default constructor. Prevent creating instanses of this class, all access
     * is made through static methods.
     */
    private Settings() {
        // Nothing
    }

    /**
     * Initialize basic application settings. This must be run once at the very
     * beginning of the application startup!
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

        debugmode = cliArguments.debugMode;

        props = new Properties();
        InputStream inputStream = null;

        try {
            URL url = Settings.class.getClassLoader().getResource(SETTINGS_FILE);
            inputStream = url.openStream();
            props.load(inputStream);

            // Commandline settings override configfile.
            if (cliArguments.portNumber > 0) {
                props.setProperty("port", String.valueOf(cliArguments.portNumber));
            }

            if (cliArguments.storage != null && cliArguments.storage.length() > 0) {
                props.setProperty("storagepath", cliArguments.storage);
            }

            logger.info("Successfully loaded settings from file {}", url.toURI().getPath());

        } catch (IOException | URISyntaxException ex) {
            throw new JelException(String.format("Failed to read settings from property file '%s', make sure it exists in class-path and are readable. Message: %s.", SETTINGS_FILE, ex.getMessage()), ex.getCause());
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
                throw new JelException(String.format("Missing access-permissions to storage-directory '%s'. This directory and ALL its subdirectories MUST have Read and Write-permissions for the user(%s) running this application!", storagePath.toString(), Settings.getUserName()));
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
     * Get version of JEL. eg. "1.0.123"
     *
     * @return version-string
     */
    public static String getVersion() {
        if (jelVersion != null) {
            return jelVersion;
        } else {
            synchronized (Settings.class) {
                if (jelVersion != null) {
                    return jelVersion;
                } else {

                    URL url = Settings.class.getClassLoader().getResource(VERSION_FILE);

                    if (url == null) {
                        throw new JelException("Missing VERSION-file, corrupt installation?");
                    }

                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                        jelVersion = reader.readLine();

                    } catch (Throwable ex) {
                        throw new JelException("Failed to read application version from VERSION-file.", ex);
                    }

                    return jelVersion;
                }
            }
        }
    }

    /**
     * Return the path where the application is supposed to store all its
     * data-files (eg. database, uploaded files, and more..)
     *
     * @return Path
     */
    public static final Path getStoragePath() {
        return storagePath;
    }

    /**
     * Return if application runs in debug-mode
     *
     * @return debug-mode enabled
     */
    public static boolean isDebugmode() {
        return debugmode;
    }

    /**
     * Returns the systems loadaverage. A negative number is returned if not
     * supported by platform.
     *
     * @return Average systemload.
     */
    public static double getLoadAverage() {
        return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }

    /**
     * The directory in which Java is installed
     *
     * @return
     */
    public static String getJavaHome() {
        return System.getProperties().getProperty("java.home");
    }

    /**
     *
     * @return
     */
    public static String getVMName() {
        return System.getProperties().getProperty("java.vm.name");
    }

    /**
     *
     * @return
     */
    public static String getVMVersion() {
        return System.getProperties().getProperty("java.vm.version");
    }

    /**
     *
     * @return
     */
    public static String getVMVendor() {
        return System.getProperties().getProperty("java.vm.vendor");
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
    public static String getOSArchitecture() {
        return System.getProperties().getProperty("os.arch");
    }

    /**
     *
     * @return
     */
    public static String getOSName() {
        return System.getProperties().getProperty("os.name");
    }

    /**
     *
     * @return
     */
    public static boolean isLinux() {
        return (getOSName().toLowerCase().contains("linux"));
    }

    /**
     * 
     * @return 
     */
    public static boolean isRaspberryPi() {
        return (getOSName().toLowerCase().contains("raspberrypi"));
    }

    /**
     *
     * @return
     */
    public static boolean isWindows() {
        return (getOSName().toLowerCase().contains("windows"));
    }

    /**
     *
     * @return
     */
    public static boolean isMacOSX() {
        return (getOSName().toLowerCase().contains("mac"));
    }

    /**
     *
     * @return
     */
    public static boolean isSolaris() {
        String name = getOSName().toLowerCase();
        return ((name.contains("sunos")) || (name.contains("solaris")));
    }

    /**
     *
     * @return
     */
    public static String getOSVersion() {
        return System.getProperties().getProperty("os.version");
    }

    /**
     * The current working directory when the properties were initialized
     *
     * @return
     */
    public static String getWorkingDirectory() {
        return System.getProperties().getProperty("user.dir");
    }

    /**
     *
     * @return
     */
    public static String getJavaEndorsedDirectory() {
        return System.getProperties().getProperty("java.endorsed.dirs");
    }

    /**
     * The directory in which java should create temporary files
     *
     * @return
     */
    public static String getTempDirectory() {
        return System.getProperties().getProperty("java.io.tmpdir");
    }

    /**
     * The home directory of the current user
     *
     * @return
     */
    public static String getUserHome() {
        return System.getProperties().getProperty("user.home");
    }

    /**
     * The username of the current user
     *
     * @return
     */
    public static String getUserName() {
        return System.getProperties().getProperty("user.name");
    }

    /**
     * The two-letter language code of the default locale
     *
     * @return
     */
    public static String getUserLanguage() {
        return System.getProperties().getProperty("user.language");
    }

    /**
     *
     * @return
     */
    public static String getUserCountry() {
        return System.getProperties().getProperty("user.country");
    }

    /**
     * The default time zone
     *
     * @return
     */
    public static String getUserTimezone() {
        return System.getProperties().getProperty("user.timezone");
    }

    /**
     * The value of the CLASSPATH environment variable
     *
     * @return
     */
    public static String getClassPath() {
        return System.getProperties().getProperty("java.class.path");
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
    public static long getTotalJvmMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     *
     * @return
     */
    public static long getFreeJvmMemory() {
        return Runtime.getRuntime().freeMemory();
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
            return new BigDecimal(usedSpace / totalSpace * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }

    /**
     * Get epoc-timestamp on application start.
     *
     * @return timestamp in milliseconds.
     */
    public static long GetUptimeStart() {
        return uptimeStart;
    }

    /**
     *
     * @return
     */
    public static String getInformationString() {
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
        builder.append("\n");
        builder.append("OS name: ").append(getOSName()).append("\n");
        builder.append("OS architecture: ").append(getOSArchitecture()).append("\n");
        builder.append("OS version: ").append(getOSVersion()).append("\n");
        builder.append("Java virtual machine: ").append(getVMName()).append("\n");
        builder.append("Java vendor: ").append(getVMVendor()).append("\n");
        builder.append("Java version: ").append(getVMVersion()).append("\n");
        builder.append("Java home: ").append(getJavaHome()).append("\n");
        builder.append("Available CPUs: ").append(getAvailableCPUs()).append("\n");
        builder.append("Free java memory: ").append(getFreeJvmMemory() / 1024).append(" KiB of ").append(getTotalJvmMemory() / 1024).append(" KiB").append("\n");
        builder.append("User language: ").append(getUserLanguage()).append("\n");
        builder.append("==================================================").append("\n");

        return builder.toString();
    }
}
