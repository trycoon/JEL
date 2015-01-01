/*
 * Copyright (c) 2014, Henrik Östman, All rights reserved.
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
package se.liquidbytes.jel.system;

import java.io.File;
import org.kohsuke.args4j.Option;

/**
 *
 * @author Henrik Östman
 */
final class ProcessArguments {

    @Option(name = "-h", aliases = "--help", usage = "print this message")
    boolean help = false;

    @Option(name = "-p", aliases = "--port", metaVar = "80", usage = "network port the application should bind to")
    int portNumber;

    @Option(name = "-s", aliases = "--storagepath", metaVar = "./storage", usage = "folder where the application stores databases and files")
    String storage;

    @Option(name = "-d", aliases = "--debug", hidden = true, usage = "run application in debug-mode")
    boolean debugMode;

    /*Map<String, String> properties = new HashMap<>();

    @Option(name = "-D", metaVar = "<property>=<value>", usage = "additional settings")
    private void setProperty(final String property) throws CmdLineException {
        String[] arr = property.split("=");
        if (arr.length != 2) {
            throw new JelException("Properties must be specified in the form: <property>=<value>");
        }
        
        properties.put(arr[0], arr[1]);
    }*/
}
