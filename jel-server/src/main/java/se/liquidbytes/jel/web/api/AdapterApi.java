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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import se.liquidbytes.jel.Settings;
import se.liquidbytes.jel.system.JelServiceProxy;

/**
 *
 * @author Henrik Östman
 */
public class AdapterApi {

  private final Vertx vertx;
  private final JelServiceProxy service;

  /**
   * Constructor
   *
   * @param vertx Vertx-instance
   */
  public AdapterApi(Vertx vertx) {
    this.vertx = vertx;
    service = JelServiceProxy.createProxy(this.vertx, Settings.EVENTBUS_NAME);
  }

  public void listAdaptertypes(RoutingContext context) {
    service.listAvailableAdapterTypes((r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void add(RoutingContext context) {
    JsonObject config = context.getBodyAsJson();

    service.addAdapter(config, (r) -> {
      if (r.succeeded()) {
        context.response().end();
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void list(RoutingContext context) {
    service.listAdapters((r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void remove(RoutingContext context) {
    JsonObject config = context.getBodyAsJson();

    service.removeAdapter(config, (r) -> {
      if (r.succeeded()) {
        context.response().end();
      } else {
        context.fail(r.cause());
      }
    });
  }
}
