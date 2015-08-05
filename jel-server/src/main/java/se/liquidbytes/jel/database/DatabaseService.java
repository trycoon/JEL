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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.database.impl.DatabaseServiceImpl;

/**
 *
 * @author Henrik Östman
 */
//@ProxyGen // Generate the proxy and handler
public interface DatabaseService {

  /**
   *
   * @param vertx
   * @param config
   * @return
   */
  static DatabaseService create(Vertx vertx, String config) {
    return new DatabaseServiceImpl(vertx, config);
  }

  static DatabaseService createProxy(Vertx vertx, String address) {
    return ProxyHelper.createProxy(DatabaseService.class, vertx, address);
  }

  /**
   * Returns a connection that can be used to perform database operations on. It's important to remember to close the connection when you are done, so it is
   * returned to the pool.
   *
   * @param handler the handler which is called when the <code>DatabaseConnection</code> object is ready for use.
   */
  void getConnection(Handler<AsyncResult<DatabaseConnection>> handler);

  /**
   * Normally invoked by the <code>DatabaseServiceVerticle</code> to start the service when deployed. This is usually not called by the user.
   */
  public void start();

  /**
   * Normally invoked by the <code>DatabaseServiceVerticle</code> to stop the service when the verticle is stopped/undeployed. This is usually not called by the
   * user.
   */
  public void stop();

}
