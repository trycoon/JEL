/*
 * Copyright (c) 2015, Henrik Östman, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package se.liquidbytes.jel.database.impl.actions;

/**
 *
 * @author Henrik Östman
 */
public class NewClass {
    /*
     ===========================================================================
     | SITE - implementation
     ===========================================================================
     */
    /*
     @Override
    public void getSite(String id, Handler<AsyncResult<String>> resultHandler) {

        logger.debug("getSite(id=%s)", id);
        try {            OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>("select from Site where id = ?");
         List<ODocument> list = dbDoc.command(query).execute(id);

         String result = "";

         if (!result.isEmpty()) {
         result = list.get(0).toJSON();
         }

         resultHandler.handle(Future.succeededFuture(result));
         } catch (Exception ex) {
         resultHandler.handle(Future.failedFuture(new JelException("Failed to get site with id=" + id, ex)));
         }
        resultHandler.handle(Future.succeededFuture("test"));
    }

    @Override
    public void getSites(Handler<AsyncResult<String>> resultHandler) {
        logger.debug("getSites()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addSite(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("addSite()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
         ODocument site = new ODocument("Sites");
         site.field("name", "My site");
         site.field("description", "");
         site.field("gpsPosition", "");
         site.field("sensors", new ODocument("City").field("name", "Rome").field("country", "Italy"));

         site.save();

         
         Id [string]("1234567890")
         Name [string]("solar1")
         Description [string]("My solar panel sensor")
         GPS-position [string]("57.6378669 18.284855")
         ACL_UserRoles [
         userId:string
         roles ["role1", "role2"]
         ACL_Rights [string-array]("role1", "role2")
         PublicVisible [boolean]
         Sensors [
         Id [string]("1234567890")
         Name [string]("solar1")
         Description [string]("My solar panel sensor")
         GPS-position [string]("57.6378669 18.284855")
         CurrentValue
         Value [number(depending on ValueType)](24.0)
         Time [date]("2014-12-24 18:23:20.212")
         PreviousValue
         Value [number(depending on ValueType)](24.3)
         Time [date]("2014-12-24 18:23:20.212")
         MaxValue
         Value [number(depending on ValueType)](123.5)
         Time [date]("2014-12-24 18:23:20.212")
         MinValue
         Value [number(depending on ValueType)](-55.0)
         Time [date]("2014-12-24 18:23:20.212")
         State [string]("connected"|"disconnected")
         SampleDelay [number](10000)
         Hardware
         Id [string]
         VendorId [string]
         Port [string] ("/dev/USB2"|"/dev/ttyS0"|..)
         ValueTransformation [string]("=i*0.5")
         ValueType [string]("number"|"string")
         ACL_Rights [string]("role1", "role2")
         SmallPresentation:
         Type [string]("text"|"gauge"|...)
         Settings
         MediumPresentation:
         Type [string]("text"|"gauge"|...)
         Settings
         LargePresentation:
         Type [string]("text"|"gauge"|...)
         Settings
         ]
         Actuators [
         Id [string]("1234567890")
         Name [string]("motor1")
         Description [string]("My solar panel pump")
         GPS-position [string]("57.6378669 18.284855")
         CurrentValue
         Value [number(depending on ValueType)]("on")
         Time [date]("2014-12-24 18:23:20.212")
         PreviousValue
         Value [number(depending on ValueType)]("off")
         Time [date]("2014-12-24 18:23:20.212")
         State [string]("connected"|"disconnected")
         Hardware
         Id [string]
         VendorId [string]
         Port [string] ("/dev/USB2"|"/dev/ttyS0"|..)
         ValueType [string]("number"|"string")
         ACL_Rights [string]("role1", "role2")
         SmallPresentation:
         Type [string]("text"|"switch"|...)
         Settings
         MediumPresentation:
         Type [string]("text"|"switch"|...)
         Settings
         LargePresentation:
         Type [string]("text"|"switch"|...)
         Settings
         ]
         
    }

    @Override
    public void updateSite(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("updateSite()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeSite(String id, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("removeSite()");
        
         OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>("select from Site where id = ?");
         List<ODocument> list = dbDoc.command(query).execute(id);
         Iterator<ODocument> iter = list.iterator();
         while (iter.hasNext()) {
         iter.next().delete();
         }
        resultHandler.handle(Future.succeededFuture("test"));
    }*/

    /*
     ===========================================================================
     | USER - implementation
     ===========================================================================
     */
    /* @Override
     public void getUser(String id, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("getUser()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getUserByUsername(String username, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("getUserByUsername()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void getUsers(Handler<AsyncResult<String>> resultHandler) {
        logger.debug("getUsers()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addUser(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("addUser()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateUser(JsonObject document, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("updateUser()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeUser(String id, Handler<AsyncResult<String>> resultHandler) {
        logger.debug("removeUser()");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }*/
}
