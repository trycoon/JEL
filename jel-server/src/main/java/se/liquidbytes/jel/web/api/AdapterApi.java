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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import se.liquidbytes.jel.Settings;
import static se.liquidbytes.jel.system.JelService.API_ENDPOINT;
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
        /*r.result().forEach(a -> {
          JsonObject adapter = (JsonObject) a;
          adapter.put("supportedDevices", String.format("%s/adapters/%s/supportedDevices", API_ENDPOINT, adapter.getString("id")));
          adapters.add(adapter);
        });*/
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
        JsonArray adapters = new JsonArray();
        r.result().forEach(a -> {
          JsonObject adapter = (JsonObject) a;
          adapter.put("devices", String.format("%s/adapters/%s/devices", API_ENDPOINT, adapter.getString("id")));
          adapter.put("supportedDevices", String.format("%s/adapters/%s/supportedDevices", API_ENDPOINT, adapter.getString("id")));
          adapters.add(adapter);
        });

        JsonObject result = new JsonObject()
            .put("devices", API_ENDPOINT + "/adapters/devices")
            .put("adapters", adapters);

        context.response().end(result.encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void retrieve(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.retrieveAdapter(adapterId, (r) -> {
      if (r.succeeded()) {
        JsonObject adapter = r.result();
        adapter.put("devices", String.format("%s/adapters/%s/devices", API_ENDPOINT, adapter.getString("id")));
        adapter.put("supportedDevices", String.format("%s/adapters/%s/supportedDevices", API_ENDPOINT, adapter.getString("id")));

        context.response().end(adapter.encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void remove(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.removeAdapter(adapterId, (r) -> {
      if (r.succeeded()) {
        context.response().end();
      } else {
        context.fail(r.cause());
      }
    });
  }
}
