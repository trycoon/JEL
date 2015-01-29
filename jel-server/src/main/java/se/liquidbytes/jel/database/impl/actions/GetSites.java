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
import io.vertx.core.json.JsonArray;
import java.lang.invoke.MethodHandles;
import org.slf4j.LoggerFactory;
import se.liquidbytes.jel.JelException;

/**
 *
 * @author Henrik Östman
 */
public class GetSites extends AbstractAction<JsonArray> {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     *
     * @param vertx
     * @param conn
     */
    public GetSites(Vertx vertx, ODatabaseDocumentTx conn) {
        super(vertx, conn);
    }

    /**
     *
     * @param conn
     * @return
     * @throws JelException
     */
    @Override
    protected JsonArray execute(ODatabaseDocumentTx conn) throws JelException {
        logger.debug("getSites()");

        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>("select from Site");
        JsonArray list = conn.command(query).execute();

        return list;
    }

}
