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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.vertx.core.json.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Metadata with information about a available plugin.
 *
 * @author Henrik Östman
 */
public class PluginDesc {

  private String name;
  private String description;
  private Path directoryPath;
  private Path originalFile;
  private String fileChecksum;
  private Category category;
  private String mainClass;
  private String version;
  private String minServerVersion;
  private String author;
  private String homepage;
  private String licence;
  private List<String> requiredPlugins;

  /**
   * Default constructor.
   */
  public PluginDesc() {
    requiredPlugins = new ArrayList<>();
  }

  /**
   * Get name of plugin.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Set a short and uniquely identifying name of this plugin.
   *
   * @param name the name to set
   */
  void setName(String name) {
    if (name == null || name.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid name-parameter");
    }

    this.name = name.trim().toLowerCase();
  }

  /**
   * Get description of plugin [optional].
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set description of plugin [optional]. It should in a few sentences describe the purpose of the plugin.
   *
   * @param description the description to set
   */
  void setDescription(String description) {
    if (description != null) {
      this.description = description.trim();
    }
  }

  /**
   * Get the path in the filesystem to the expanded plugins directory
   *
   * @return the path
   */
  public Path getDirectoryPath() {
    return directoryPath;
  }

  /**
   * Set the path in the filesystem to the expanded plugins directory
   *
   * @param path the path to set
   */
  void setDirectoryPath(Path path) {
    if (path == null) {
      throw new IllegalArgumentException("Not a valid directoryPath-parameter");
    }

    this.directoryPath = path;
  }

  /**
   * Get the path in the filesystem to the original uncompressed plugin-file
   *
   * @return the path
   */
  public Path getOriginalFile() {
    return originalFile;
  }

  /**
   * Set the path in the filesystem to the original uncompressed plugin-file
   *
   * @param file the path to set
   */
  void setOriginalFile(Path file) {
    if (file == null) {
      throw new IllegalArgumentException("Not a valid originalFile-parameter");
    }

    this.originalFile = file;
  }

  /**
   * Get the checksum of the original uncompressed plugin-file
   *
   * @return the checksum of the plugin-file
   */
  public String getFileChecksum() {
    return fileChecksum;
  }

  /**
   * Set the checksum of the original uncompressed plugin-file
   *
   * @param checksum checksum of the plugin-file
   */
  void setFileChecksum(String checksum) {
    if (checksum == null || checksum.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid fileChecksum-parameter");
    }

    this.fileChecksum = checksum.trim();
  }

  /**
   * Get the category this plugin belongs to
   *
   * @return the category
   */
  public Category getCategory() {
    return category;
  }

  /**
   * Set the category this plugin belongs to
   *
   * @param category the category
   */
  void setCategory(Category category) {
    if (category == null) {
      throw new IllegalArgumentException("Not a valid category-parameter");
    }

    this.category = category;
  }

  /**
   * Get the startingpoint of execution of this plugin.
   *
   * @return the mainClass
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the startingpoint of execution of this plugin, it should be the class that takes care of initializing and also shutting down the plugin. The class must
   * implement the se.liquidbytes.jel.plugins.PluginDesc-interface.
   *
   * @param mainClass the mainClass to set
   */
  void setMainClass(String mainClass) {
    if (mainClass == null || mainClass.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid mainClass-parameter");
    }

    this.mainClass = mainClass.trim();
  }

  /**
   * Get the version of the plugin
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Set the plugin version according to the <a href="http://semver.org/">Semver-format</a>
   *
   * @param version the version to set
   */
  void setVersion(String version) {
    if (version == null || version.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid version-parameter");
    }

    this.version = version.trim();
  }

  /**
   * Get the minimum version of server compatible with the plugin.
   *
   * @return the version
   */
  public String getMinServerVersion() {
    return minServerVersion;
  }

  /**
   * Set the minimum version of server compatible with the plugin, using the <a href="http://semver.org/">Semver-format</a>
   *
   * @param minServerVersion the version to confirm to
   */
  void setMinServerVersion(String minServerVersion) {
    if (minServerVersion == null || minServerVersion.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid minServerVersion-parameter");
    }

    this.minServerVersion = minServerVersion.trim();
  }

  /**
   * Get the author of this plugin
   *
   * @return the author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Set the author of this plugin. These different formats are valid ones: "Henrik Östman" or "<trycoon@gmail.com>Henrik Östman"
   *
   * @param author the author to set
   */
  void setAuthor(String author) {
    if (author == null || author.trim().length() == 0) {
      throw new IllegalArgumentException("Not a valid author-parameter");
    }

    this.author = author.trim();
  }

  /**
   * Get the homepage of this plugin [optional].
   *
   * @return the homepage
   */
  public String getHomepage() {
    return homepage;
  }

  /**
   * Set the homepage of this plugin [optional].
   *
   * @param homepage the homepage to set
   */
  public void setHomepage(String homepage) {
    if (homepage != null) {
      this.homepage = homepage.trim();
    }
  }

  /**
   * Get license-information for plugin [optional].
   *
   * @return the licence
   */
  public String getLicence() {
    return licence;
  }

  /**
   * Set license-information for plugin [optional]. If no license-information is provided, then the plugin is free to use with no rights reserved.
   *
   * @param licence the licence to set
   */
  public void setLicence(String licence) {
    if (licence != null && licence.trim().length() > 0) {
      this.licence = licence.trim();
    } else {
      this.licence = "Free to use, no rights reserved";
    }
  }

  /**
   * Get list of required plugins (parent plugins)
   *
   * @return the required plugins
   */
  public Collection<String> getRequiredPlugins() {
    return Collections.unmodifiableCollection(this.requiredPlugins);
  }

  /**
   * Set list of required plugins (parent plugins). The list should contain the plugins name.
   *
   * @param requiredPlugins the requiredPlugins to set
   */
  public void setRequiredPlugins(List<String> requiredPlugins) {
    this.requiredPlugins = requiredPlugins;
  }

  /**
   * Category of plugin, this is for categorizing all plugins when listing them and let different manager-classes handle their specific type of plugins.
   */
  public enum Category {

    ADAPTER("adapter"),
    DEVICE("device"),
    TEST("test");

    private final String value;

    Category(String v) {
      value = v;
    }

    @JsonValue
    public String value() {
      return value;
    }

    @JsonCreator
    public static Category fromValue(String typeCode) {
      for (Category c : Category.values()) {
        if (c.value.equals(typeCode)) {
          return c;
        }
      }
      throw new IllegalArgumentException("Invalid Category name: " + typeCode);

    }
  }

  /**
   * Validate if all mandatory field are set correctly.
   *
   * @throws IllegalArgumentException if a field is not set correct.
   */
  void validate() {
    this.setName(this.getName());
    this.setDescription(this.getDescription());
    this.setDirectoryPath(this.getDirectoryPath());
    this.setOriginalFile(this.getOriginalFile());
    this.setFileChecksum(this.getFileChecksum());
    this.setCategory(this.getCategory());
    this.setMainClass(this.getMainClass());
    this.setVersion(this.getVersion());
    this.setMinServerVersion(this.getMinServerVersion());
    this.setAuthor(this.getAuthor());
    this.setHomepage(this.getHomepage());
    this.setLicence(this.getLicence());
  }

  /**
   * Information about this object in a public API-friendly way.
   *
   * @return Information about this object.
   */
  public JsonObject toApi() {
    JsonObject obj = new JsonObject()
        .put("name", this.name)
        .put("description", this.description)
        .put("directoryPath", this.directoryPath.toString())
        .put("originalFile", this.originalFile.toString())
        .put("fileChecksum", this.fileChecksum)
        .put("category", this.category.toString())
        .put("mainClass", this.mainClass)
        .put("version", this.version)
        .put("minServerVersion", this.minServerVersion)
        .put("author", this.author)
        .put("homepage", this.homepage)
        .put("license", this.licence);

    return obj;
  }
}
