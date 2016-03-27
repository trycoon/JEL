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
public class DeviceApi {

  private final Vertx vertx;
  private final JelServiceProxy service;

  /**
   * Constructor
   *
   * @param vertx Vertx-instance
   */
  public DeviceApi(Vertx vertx) {
    this.vertx = vertx;
    service = JelServiceProxy.createProxy(this.vertx, Settings.EVENTBUS_NAME);
  }

  public void listAllDevices(RoutingContext context) {
    service.listAllDevices((r) -> {
      if (r.succeeded()) {
        JsonArray deviceList = r.result();

        /*deviceList.forEach(d -> {
          JsonObject device = (JsonObject) d;
          device.put("currentValue", String.format("%s/adapters/%s/devices/%s/value", API_ENDPOINT, adapterId, device.getString("id")));
        });*/
        context.response().end(deviceList.encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void retrieveSupportedAdapterDevices(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.listSupportedAdapterDevices(adapterId, (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void listAdapterDevices(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.listAdapterDevices(adapterId, (r) -> {
      if (r.succeeded()) {
        JsonArray deviceList = r.result();

        deviceList.forEach(d -> {
          JsonObject device = (JsonObject) d;
          device.put("currentValue", String.format("%s/adapters/%s/devices/%s/value", API_ENDPOINT, adapterId, device.getString("id")));
        });
        context.response().end(deviceList.encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void createAdapterDevice(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.createAdapterDevice(adapterId, context.getBodyAsJson(), (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void retrieveAdapterDevice(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    String deviceId = request.getParam("deviceId");

    if (adapterId == null || deviceId == null) {
      context.fail(400);
      return;
    }
// As device id is unique we don't really need adapter id.
    service.retrieveAdapterDevice(deviceId, (r) -> {
      if (r.succeeded()) {
        JsonObject device = (JsonObject) r.result();
        device.put("currentValue", String.format("%s/adapters/%s/devices/%s/value", API_ENDPOINT, adapterId, deviceId));
        context.response().end(device.encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void updateAdapterDevice(RoutingContext context) {
    HttpServerRequest request = context.request();
    String deviceId = request.getParam("deviceId");

    if (deviceId == null) {
      context.fail(400);
      return;
    }
    // As device id is unique we don't really need adapter id.
    service.updateAdapterDevice(deviceId, context.getBodyAsJson(), (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void deleteAdapterDevice(RoutingContext context) {
    HttpServerRequest request = context.request();
    String deviceId = request.getParam("deviceId");

    if (deviceId == null) {
      context.fail(400);
      return;
    }
// As device id is unique we don't really need adapter id.
    service.deleteAdapterDevice(deviceId, (r) -> {
      if (r.succeeded()) {
        context.response().end();
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void getDeviceValue(RoutingContext context) {
    HttpServerRequest request = context.request();
    String deviceId = request.getParam("deviceId");

    if (deviceId == null) {
      context.fail(400);
      return;
    }

    service.retrieveDeviceValue(deviceId, (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void setDeviceValue(RoutingContext context) {
    HttpServerRequest request = context.request();
    String deviceId = request.getParam("deviceId");
    JsonObject body = context.getBodyAsJson();
    String value = body.getString("value");

    if (deviceId == null) {
      context.fail(400);
      return;
    }

    service.updateDeviceValue(deviceId, value, (r) -> {
      if (r.succeeded()) {
        context.response().end();
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void addToSite(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    JsonObject user = context.get("user");

    context.response().end("TODO");
  }

  public void listOnSite(RoutingContext context) {
    HttpServerRequest request = context.request();
    String siteId = request.getParam("siteId");
    if (siteId == null) {
      context.fail(400);
      return;
    }

    service.listSiteDevices(siteId, (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void retrieveOnSite(RoutingContext context) {
    HttpServerRequest request = context.request();
    String siteId = request.getParam("siteId");
    if (siteId == null) {
      context.fail(400);
      return;
    }

    context.response().end("TODO");
  }

  public void updateOnSite(RoutingContext context) {
    context.response().end("TODO");
  }

  public void deleteFromSite(RoutingContext context) {
    context.response().end("TODO");
  }

  public void listSupportedDevices(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    if (adapterId == null) {
      context.fail(400);
      return;
    }

    service.listSupportedAdapterDevices(adapterId, (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }
}
