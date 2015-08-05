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

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;

/**
 * ClassLoader for plugins. It searches the plugin directory for classes, Jar, and Zip-files, then constructs a class loader for the resources found.
 *
 * @author Henrik Östman
 */
public class PluginClassLoader extends URLClassLoader {

  /**
   * loggerghandler instance
   */
  private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final List<JarURLConnection> cachedJarFiles = new ArrayList<>();

  /**
   * Constructor
   *
   * @param classloader parent classloader to use.
   */
  public PluginClassLoader(ClassLoader classloader) {
    super(new URL[]{}, classloader);
  }

  /**
   * Add a plugin-directory to the class loader.
   *
   * @param directory the directory.
   * @throws JelException if walking directory-path fails.
   */
  public void addDirectory(Path directory) throws JelException {
    try {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{jar,zip}");

      // Find all Jar and Zip-files and add them to classloader.
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws MalformedURLException {
          Path name = file.getFileName();
          if (name != null && !name.startsWith("~") && !name.startsWith(".") && matcher.matches(name)) { // Guess we should use a regexp instead of glob.
            addJarFile(new URL("jar", "", -1, file.toAbsolutePath().toUri().toString() + "!/"));
          }
          return FileVisitResult.CONTINUE;
        }
      });
      // Add plugin-basedirectory to classloader, this should add all classfiles.
      addURL(directory.toUri().toURL());
    } catch (IOException ex) {
      throw new JelException(ex.getMessage(), ex);
    }
  }

  /**
   * Add the given URL to the classpath for this class loader, caching the JAR file connection so it can be unloaded later
   *
   * @param file URL for the JAR file or directory to append to classpath
   */
  public void addJarFile(URL file) {
    try {
      // open and cache JAR file connection
      URLConnection uc = file.openConnection();
      if (uc instanceof JarURLConnection) {
        uc.setUseCaches(true);
        ((JarURLConnection) uc).getManifest();
        cachedJarFiles.add((JarURLConnection) uc);
      }
    } catch (Exception e) {
      logger.warn("Failed to cache plugin JAR file: " + file.toExternalForm());
    }

    addURL(file);
  }

  /**
   * Unload any JAR files that have been cached by this plugin
   */
  public void unloadJarFiles() {
    for (JarURLConnection url : cachedJarFiles) {
      try {
        logger.debug("Unloading plugin JAR file " + url.getJarFile().getName());
        url.getJarFile().close();
      } catch (Exception ex) {
        logger.error("Failed to unload JAR file", ex);
      }
    }
  }
}
