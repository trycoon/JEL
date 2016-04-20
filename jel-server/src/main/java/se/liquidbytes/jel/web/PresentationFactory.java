/*
 * Copyright 2016 Henrik Östman.
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
package se.liquidbytes.jel.web;

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

/**
 *
 * @author Henrik Östman
 */
public final class PresentationFactory {
  // http://www.gotohal.net/halbuilder.html
  private final static RepresentationFactory FACTORY = new StandardRepresentationFactory()
      .withFlag(RepresentationFactory.PRETTY_PRINT)
      .withFlag(RepresentationFactory.COALESCE_ARRAYS);

  /**
   *
   * @param basePath
   * @return
   */
  public final static Representation getRepresentation(String basePath) {
    return FACTORY.newRepresentation(basePath);
  }

  /**
   *
   * @return
   */
  public final static Representation getRepresentation() {
    return FACTORY.newRepresentation();
  }
}
