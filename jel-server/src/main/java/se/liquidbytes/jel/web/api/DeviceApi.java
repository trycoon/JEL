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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import se.liquidbytes.jel.Settings;
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
        context.response().end(r.result().encodePrettily());
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
        context.response().end(r.result().encodePrettily());
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
        context.response().end(r.result().encodePrettily());
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
    String adapterId = request.getParam("adapterId");
    String deviceId = request.getParam("deviceId");

    if (adapterId == null || deviceId == null) {
      context.fail(400);
      return;
    }

    service.retrieveDeviceValue(adapterId, deviceId, (r) -> {
      if (r.succeeded()) {
        context.response().end(r.result().encodePrettily());
      } else {
        context.fail(r.cause());
      }
    });
  }

  public void setDeviceValue(RoutingContext context) {
    HttpServerRequest request = context.request();
    String adapterId = request.getParam("adapterId");
    String deviceId = request.getParam("deviceId");

    if (adapterId == null || deviceId == null) {
      context.fail(400);
      return;
    }

    service.updateDeviceValue(adapterId, deviceId, context.getBodyAsJson(), (r) -> {
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
