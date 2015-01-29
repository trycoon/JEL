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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;

/**
 *
 * @author Henrik Östman
 */
public class GetSite extends AbstractAction<JsonObject> {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String id;

    /**
     *
     * @param vertx
     * @param conn
     * @param id
     */
    public GetSite(Vertx vertx, ODatabaseDocumentTx conn, String id) {
        super(vertx, conn);
        this.id = id;
    }

    /**
     *
     * @param conn
     * @return
     * @throws JelException
     */
    @Override
    protected JsonObject execute(ODatabaseDocumentTx conn) throws JelException {
        logger.debug("getSite()");

        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>("select from Site where id=?");
        List<ODocument> list = conn.command(query).execute(this.id);
        JsonObject result = null;

        if (!list.isEmpty()) {
            result = new JsonObject(list.get(0).toJSON());
        }

        return result;
    }

}
