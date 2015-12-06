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
package se.liquidbytes.jel;

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

  @Option(name = "-d", aliases = "--debug", hidden = true, usage = "run application in development/debug-mode")
  boolean isDebug;

  @Option(name = "--skipweb", usage = "skip deploying webserver, if you want to use your own client and not expose the one provided by JEL out of the box.")
  boolean skipweb;

  @Option(name = "--skipapi", usage = "skip deploying the REST-API provided by JEL. This also disables the webserver(\"skipweb\"-setting)")
  boolean skipapi;

  @Option(name = "--endpoint", usage = "endpoint that this service should be accessable from, including protocol(http/https) and portnumber(if not standard). If none specified then \"http://<servers ip-address>:<portnumber>\"  is used.")
  String serverEndpoint;

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
