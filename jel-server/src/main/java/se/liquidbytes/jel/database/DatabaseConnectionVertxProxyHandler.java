/*
* Copyright 2014 Red Hat, Inc.
*
* Red Hat licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/

package se.liquidbytes.jel.database;

import se.liquidbytes.jel.database.DatabaseConnection;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.ArrayList;import java.util.HashSet;import java.util.List;import java.util.Map;import java.util.Set;import java.util.UUID;
import io.vertx.serviceproxy.ProxyHelper;
import io.vertx.serviceproxy.ProxyHandler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AsyncResult;
import se.liquidbytes.jel.database.DatabaseConnection;
import io.vertx.core.Handler;

/*
  Generated Proxy code - DO NOT EDIT
  @author Roger the Robot
*/
public class DatabaseConnectionVertxProxyHandler extends ProxyHandler {

  private final Vertx vertx;
  private final DatabaseConnection service;
  private final String address;

  public DatabaseConnectionVertxProxyHandler(Vertx vertx, DatabaseConnection service, String address) {
    this.vertx = vertx;
    this.service = service;
    this.address = address;
  }

  public void handle(Message<JsonObject> msg) {
    JsonObject json = msg.body();
    String action = msg.headers().get("action");
    if (action == null) {
      throw new IllegalStateException("action not specified");
    }
    switch (action) {
      case "getSite": {
        service.getSite((java.lang.String)json.getValue("id"), createHandler(msg));
        break;
      }
      case "getSites": {
        service.getSites(createHandler(msg));
        break;
      }
      case "addSite": {
        service.addSite((io.vertx.core.json.JsonObject)json.getValue("document"), createHandler(msg));
        break;
      }
      case "updateSite": {
        service.updateSite((io.vertx.core.json.JsonObject)json.getValue("document"), createHandler(msg));
        break;
      }
      case "removeSite": {
        service.removeSite((java.lang.String)json.getValue("id"), createHandler(msg));
        break;
      }
      case "getUser": {
        service.getUser((java.lang.String)json.getValue("id"), createHandler(msg));
        break;
      }
      case "getUserByUsername": {
        service.getUserByUsername((java.lang.String)json.getValue("username"), createHandler(msg));
        break;
      }
      case "getUsers": {
        service.getUsers(createHandler(msg));
        break;
      }
      case "addUser": {
        service.addUser((io.vertx.core.json.JsonObject)json.getValue("document"), createHandler(msg));
        break;
      }
      case "updateUser": {
        service.updateUser((io.vertx.core.json.JsonObject)json.getValue("document"), createHandler(msg));
        break;
      }
      case "removeUser": {
        service.removeUser((java.lang.String)json.getValue("id"), createHandler(msg));
        break;
      }
      default: {
        throw new IllegalStateException("Invalid action: " + action);
      }
    }
  }
  private <T> Handler<AsyncResult<T>> createHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        msg.fail(-1, res.cause().getMessage());
      } else {
        msg.reply(res.result());
      }
    };
  }
  private <T> Handler<AsyncResult<List<T>>> createListHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        msg.fail(-1, res.cause().getMessage());
      } else {
        msg.reply(new JsonArray(res.result()));
      }
    };
  }
  private <T> Handler<AsyncResult<Set<T>>> createSetHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        msg.fail(-1, res.cause().getMessage());
      } else {
        msg.reply(new JsonArray(new ArrayList<>(res.result())));
      }
    };
  }
  private Handler<AsyncResult<List<Character>>> createListCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        msg.fail(-1, res.cause().getMessage());
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int)chr);
        }
        msg.reply(arr);
      }
    };
  }
  private Handler<AsyncResult<Set<Character>>> createSetCharHandler(Message msg) {
    return res -> {
      if (res.failed()) {
        msg.fail(-1, res.cause().getMessage());
      } else {
        JsonArray arr = new JsonArray();
        for (Character chr: res.result()) {
          arr.add((int)chr);
        }
        msg.reply(arr);
      }
    };
  }
  private <T> Map<String, T> convertMap(Map map) {
    return (Map<String, T>)map;
  }
  private <T> List<T> convertList(List list) {
    return (List<T>)list;
  }
  private <T> Set<T> convertSet(List list) {
    return new HashSet<T>((List<T>)list);
  }
}