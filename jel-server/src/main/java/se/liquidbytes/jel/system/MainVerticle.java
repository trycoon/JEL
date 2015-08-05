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
package se.liquidbytes.jel.system;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ProxyHelper;
import se.liquidbytes.jel.Settings;

/**
 *
 * @author Henrik Östman
 */
public class MainVerticle extends AbstractVerticle {

  private JelService service;

  /**
   * Method for starting up service.
   * @throws java.lang.Exception
   */
  @Override
  public void start() throws Exception {
    service = JelService.create(vertx);
    service.start();

    // Register service to eventbus once it's started, it can now receive requests.
    ProxyHelper.registerService(JelService.class, vertx, service, Settings.EVENTBUS_NAME);
  }

  /**
   * Method for stopping service, must be called upon during application shutdown.
   * @throws java.lang.Exception
   */
  @Override
  public void stop() throws Exception {
    service.stop();
  }
}
