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
package se.liquidbytes.jel.database;

import io.vertx.core.AbstractVerticle;
import java.lang.invoke.MethodHandles;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henrik Östman
 */
public class DatabaseServiceVerticle extends AbstractVerticle {

  // https://github.com/orientechnologies/orientdb/wiki/Embedded-Server
  // http://www.orientechnologies.com/docs/2.0/orientdb.wiki/Tutorial-Document-and-graph-model.html
  // http://www.orientechnologies.com/docs/last/orientdb.wiki/SQL-Insert.html
  // https://github.com/orientechnologies/orientdb/wiki/Document-Database#asynchronous-query
  // http://www.orientechnologies.com/docs/last/orientdb.wiki/Time-series-use-case.html
  private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  protected DatabaseService service;

  /**
   * Default constructor.
   */
  public DatabaseServiceVerticle() {
    // Nothing
  }

  /**
   *
   * @throws java.lang.Exception
   */
  @Override
  public void start() throws Exception {

    // Setup Vertx-service-proxy that acts as the API-router for the database against the rest of the application.
    service = DatabaseService.create(vertx, generateConfig());
    //ProxyHelper.registerService(DatabaseService.class, vertx, service, "jel.database");

    service.start();

    // TEST, remove later
    DatabaseService proxy = DatabaseService.create(vertx, "jel.database");
    proxy.getConnection(res -> {
      if (res.succeeded()) {
        res.result().getSites(res2 -> {
          if (res2.succeeded()) {
            logger.info(res2.result().toString());
          }
        });
      }
    });
  }

  /**
   *
   * @throws java.lang.Exception
   */
  @Override
  public void stop() throws Exception {
    if (service != null) {
      service.stop();
      service = null;
    }
  }

  /**
   *
   * @return
   */
  private String generateConfig() {

    StringBuilder builder = new StringBuilder();
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
      .append("<orient-server>")
      .append("  <network>")
      .append("    <protocols>")
      .append("       <protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>")
      .append("       <protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>")
      .append("    </protocols>")
      .append("    <listeners>")
      .append("      <listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>")
      .append("      <listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>")
      .append("    </listeners>")
      .append("  </network>")
      .append("  <users>")
      .append("    <user name=\"root\" password=\"OEVkk5t7!\" resources=\"*\"/>")
      .append("  </users>")
      .append("  <properties>")
      .append("    <entry name=\"log.console.level\" value=\"info\"/>")
      .append("    <entry name=\"log.file.level\" value=\"fine\"/>")
      //The following is required to eliminate an error or warning, "Error on resolving property: ORIENTDB_HOME"
      .append("    <entry name=\"plugin.dynamic\" value=\"false\"/>")
      .append("  </properties>")
      .append("</orient-server>");

    return builder.toString();
  }

}
