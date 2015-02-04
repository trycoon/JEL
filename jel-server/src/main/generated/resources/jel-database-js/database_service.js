/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/** @module jel-database-js/database_service */
var utils = require('vertx-js/util/utils');
var DatabaseConnection = require('jel-database-js/database_connection');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JDatabaseService = se.liquidbytes.jel.database.DatabaseService;

/**

 @class
*/
var DatabaseService = function(j_val) {

  var j_databaseService = j_val;
  var that = this;

  /**
   Returns a connection that can be used to perform database operations on.
   It's important to remember to close the connection when you are done, so
   it is returned to the pool.

   @public
   @param handler {function} 
   */
  this.getConnection = function(handler) {
    var __args = arguments;
    if (__args.length === 1 && typeof __args[0] === 'function') {
      j_databaseService.getConnection(function(ar) {
      if (ar.succeeded()) {
        handler(new DatabaseConnection(ar.result()), null);
      } else {
        handler(null, ar.cause());
      }
    });
    } else utils.invalidArgs();
  };

  /**
   Normally invoked by the <code>DatabaseServiceVerticle</code> to start the
   service when deployed. This is usually not called by the user.

   @public

   */
  this.start = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_databaseService.start();
    } else utils.invalidArgs();
  };

  /**
   Normally invoked by the <code>DatabaseServiceVerticle</code> to stop the
   service when the verticle is stopped/undeployed. This is usually not
   called by the user.

   @public

   */
  this.stop = function() {
    var __args = arguments;
    if (__args.length === 0) {
      j_databaseService.stop();
    } else utils.invalidArgs();
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_databaseService;
};

/**

 @memberof module:jel-database-js/database_service
 @param vertx {Vertx} 
 @param config {string} 
 @return {DatabaseService}
 */
DatabaseService.create = function(vertx, config) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'string') {
    return new DatabaseService(JDatabaseService.create(vertx._jdel, config));
  } else utils.invalidArgs();
};

/**

 @memberof module:jel-database-js/database_service
 @param vertx {Vertx} 
 @param address {string} 
 @return {DatabaseService}
 */
DatabaseService.createEventBusProxy = function(vertx, address) {
  var __args = arguments;
  if (__args.length === 2 && typeof __args[0] === 'object' && __args[0]._jdel && typeof __args[1] === 'string') {
    return new DatabaseService(JDatabaseService.createEventBusProxy(vertx._jdel, address));
  } else utils.invalidArgs();
};

// We export the Constructor function
module.exports = DatabaseService;