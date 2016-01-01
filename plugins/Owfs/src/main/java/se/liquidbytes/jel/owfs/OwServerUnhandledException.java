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
package se.liquidbytes.jel.owfs;

/**
 * Exception that is thrown for generic errors like programmatic errors or faulty arguments. Should stop application execution.
 */
public class OwServerUnhandledException extends RuntimeException {

  /**
   * Default constructor for exception class
   */
  public OwServerUnhandledException() {
    super();
  }

  /**
   * Constructor for exception class
   *
   * @param message Message
   */
  public OwServerUnhandledException(String message) {
    super();
  }

  /**
   * Constructor for exception class
   *
   * @param cause Inner exception
   */
  public OwServerUnhandledException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructor for exception class
   *
   * @param message Message
   * @param cause Inner exception
   */
  public OwServerUnhandledException(String message, Throwable cause) {
    super(message, cause);
  }
}
