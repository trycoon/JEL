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

import static se.liquidbytes.jel.system.JelService.EVENTBUS;

/**
 * Base interface implemented by all plugins for JEL. Plugins enhance the functionality of JEL, they can add support for additional adapters and devices, they
 * can represent new visual components or add interfaces to third-party softwares. Plugins live in the <tt>plugins</tt> directory of the <tt>storage</tt>
 * directory. Plugins are automatically expanded into directories when detected. A plugin directory should resemble the following structure:
 *
 * <pre>[plugin-dir]
 *    | -- se/liquidbytes/jel/testplugin/
 *    | --    Testplugin.class
 *    | --    SomeSupportClass.class
 *    | --    SomeMoreClass.class
 *    | -- lib/
 *    | -- plugin.json</pre>
 *
 * The <tt>lib</tt> directory are optional, any files in this will be added to the classpath of the plugin. The
 * <tt>plugin.json</tt> file is required, and contains necessary information about the plugin. The JSON file should resemble the following:
 *
 * <pre>
 * {
 *  "name": "[unique name of plugin]",
 *  "category": "[type of plugin, e.g. adapter]",
 *  "version": "[semantic-formatted version]",
 *  "author": "[name or e-mail and name]",
 *  "homepage": "[URL to plugin webpage]",
 *  "minServerVersion": "[minimum version of JEL required by plugin]",
 *  "mainClass": "[fully-qualified name of a class the implement this interface, e.g. se.liquidbytes.jel.owfs.OwfsAdapter",
 *  "description": "[optional description of the plugin]",
 *  "requiredPlugins": [ [optional list of plugin names that are required by this plugin] ],
 *  "license": "[optional license information]"
 * }</pre>
 *
 * <p>
 * Each plugin will be loaded in its own class loader, unless the plugin is configured with a parent plugin.
 * </p>
 *
 * @author Henrik Östman
 */
public interface Plugin {

  /**
   * Namespace for communicating plugins events over the eventbus.
   */
  public final static String EVENTBUS_PLUGINS = EVENTBUS + ".plugins";

  /**
   * Events
   */
  public final static String EVENT_PLUGIN_INSTALLED = "PLUGIN_INSTALLED";
  public final static String EVENT_PLUGIN_UNINSTALLED = "PLUGIN_UNINSTALLED";
  public final static String EVENT_PLUGIN_STARTED = "PLUGIN_STARTED";
  public final static String EVENT_PLUGIN_STOPPED = "PLUGIN_STOPPED";

  /**
   * Method will be invoked when plugin is installed into the system.
   */
  public void pluginInstall();

  /**
   * Method will be invoked when plugin is uninstalled from the system.
   *
   */
  public void pluginUninstall();

  /**
   * Method will be invoked every time the plugin is loaded, this normaly happens once every time the application is started up.
   */
  public void pluginStart();

  /**
   * Method will be invoked every time the plugin is unloaded, this normaly happens once every time the application is shutting down.
   */
  public void pluginStop();
}
