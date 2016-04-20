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

import com.theoryinpractise.halbuilder.api.Representation;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import se.liquidbytes.jel.Settings;
import static se.liquidbytes.jel.system.JelService.API_ENDPOINT;
import se.liquidbytes.jel.system.JelServiceProxy;
import se.liquidbytes.jel.web.PresentationFactory;

/**
 *
 * @author Henrik Östman
 */
public class SystemApi {

  private final Vertx vertx;
  private final JelServiceProxy service;

  /**
   * Constructor
   *
   * @param vertx Vertx-instance
   */
  public SystemApi(Vertx vertx) {
    this.vertx = vertx;
    service = JelServiceProxy.createProxy(this.vertx, Settings.EVENTBUS_NAME);
  }

  public void systemInformation(RoutingContext context) {
    service.systemInformation((r) -> {
      if (r.succeeded()) {
        JsonObject source = r.result();
        Representation rep = PresentationFactory.getRepresentation(API_ENDPOINT + "/system/info")
            .withProperty("applicationVersion", source.getString("applicationVersion"))
            .withProperty("applicationStarttime", source.getString("applicationStarttime"))
            .withProperty("serverCurrenttime", source.getString("serverCurrenttime"))
            .withProperty("applicationBuildnumber", source.getString("applicationBuildnumber"))
            .withRepresentation("java", PresentationFactory.getRepresentation()
                .withProperty("virtualMachine", source.getJsonObject("java").getString("virtualMachine"))
                .withProperty("runtime", source.getJsonObject("java").getString("runtime"))
                .withProperty("version", source.getJsonObject("java").getString("version"))
                .withProperty("vendor", source.getJsonObject("java").getString("vendor"))
                .withProperty("specificationName", source.getJsonObject("java").getString("specificationName"))
                .withProperty("javaHome", source.getJsonObject("java").getString("javaHome"))
            )
            .withRepresentation("os", PresentationFactory.getRepresentation()
                .withProperty("name", source.getJsonObject("os").getString("name"))
                .withProperty("description", source.getJsonObject("os").getString("description"))
                .withProperty("version", source.getJsonObject("os").getString("version"))
                .withProperty("architecture", source.getJsonObject("os").getString("architecture"))
            )
            .withRepresentation("hardware", PresentationFactory.getRepresentation()
                .withProperty("availableCPUs", source.getJsonObject("hardware").getInteger("availableCPUs"))
                .withProperty("ipAddress", source.getJsonObject("hardware").getString("ipAddress"))
                .withProperty("gatewayAddress", source.getJsonObject("hardware").getString("gatewayAddress"))
                .withProperty("serverEndpoint", source.getJsonObject("hardware").getString("serverEndpoint"))
                .withProperty("bogoMIPS", source.getJsonObject("hardware").getString("bogoMIPS"))
                .withProperty("details", source.getJsonObject("hardware").getJsonObject("details"))
            );

        context.response().end(rep.toString(context.get("__content-type")));

      } else {
        context.fail(r.cause());
      }
    });
  }

  public void systemResources(RoutingContext context) {
    service.systemResources((r) -> {
      if (r.succeeded()) {
        JsonObject source = r.result();
        Representation rep = PresentationFactory.getRepresentation(API_ENDPOINT + "/system/resources")
            .withRepresentation("cpu", PresentationFactory.getRepresentation()
                .withProperty("temperature", source.getJsonObject("cpu").getFloat("temperature"))
                .withProperty("loadAverage", source.getJsonObject("cpu").getFloat("loadAverage"))
            )
            .withRepresentation("disk", PresentationFactory.getRepresentation()
                .withProperty("fullness", source.getJsonObject("disk").getFloat("fullness"))
            )
            .withRepresentation("java", PresentationFactory.getRepresentation()
                .withProperty("freeMemory", source.getJsonObject("java").getLong("freeMemory"))
                .withProperty("totalMemory", source.getJsonObject("java").getLong("totalMemory"))
            )
            .withRepresentation("memory", PresentationFactory.getRepresentation()
                .withProperty("free", source.getJsonObject("memory").getLong("free"))
                .withProperty("total", source.getJsonObject("memory").getLong("total"))
            );

        context.response().end(rep.toString(context.get("__content-type")));
      } else {
        context.fail(r.cause());
      }
    });
  }
}
