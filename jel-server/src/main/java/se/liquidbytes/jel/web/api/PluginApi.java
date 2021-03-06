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
package se.liquidbytes.jel.web.api;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.system.JelServiceProxy;

/**
 *
 * @author Henrik Östman
 */
public class PluginApi {

  private final Vertx vertx;
  private final JelServiceProxy service;

  /**
   * Constructor
   *
   * @param vertx Vertx-instance
   */
  public PluginApi(Vertx vertx) {
    this.vertx = vertx;
    service = JelServiceProxy.createProxy(this.vertx, Settings.EVENTBUS_NAME);
  }

  public void install(RoutingContext context) {
    context.response().end("TODO");
  }

  public void listInstalled(RoutingContext context) {
    service.listInstalledPlugins((r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void update(RoutingContext context) {
    HttpServerRequest request = context.request();
    String name = request.getParam("name");
    if (name == null) {
      context.fail(400);
      return;
    }

    context.response().end("TODO");
  }

  public void uninstall(RoutingContext context) {
    HttpServerRequest request = context.request();
    String name = request.getParam("name");
    if (name == null) {
      context.fail(400);
      return;
    }

    /* vertx.executeBlocking(future -> {
     try {
     service.uninstallPlugin(name, null);
     future.complete(conn);
     } catch (Throwable e) {
     future.fail(e);
     }
     }, handler::handle);*/
    context.response().end("TODO");
  }

  public void listRepoPlugins(RoutingContext context) {
    HttpServerRequest request = context.request();
    String filter = request.getParam("filter");
    if (filter == null) {
      context.fail(400);
    } else {
      if (filter.equals("update")) {
        service.listAvailablePluginsToInstall((r) -> {
          if (r.succeeded()) {
            context.response().end(r.result().encodePrettily());
          } else {
            context.fail(r.cause());
          }
        });
      } else {
        service.listAvailablePluginsToUpdate((r) -> {
          if (r.succeeded()) {
            context.response().end(r.result().encodePrettily());
          } else {
            context.fail(r.cause());
          }
        });
      }
    }
  }
}
