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
package se.liquidbytes.jel.system.site;

import java.time.Clock;

/**
 *
 * @author Henrik Östman
 */
public class User {

  private String id;
  private String username;
  private String firstName;
  private String lastName;
  private String description;
  private Clock created;
  private boolean disabled;
  private String email;
  private String passwordHash;
  private String lostPasswordHash;
  private Clock lastLoggedIn;
  private String locallity;

  public User() {
    // Nothing here!
  }

  /**
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return the username
   */
  public String getUsername() {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * @return the firstName
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * @param firstName the firstName to set
   */
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * @return the lastName
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * @param lastName the lastName to set
   */
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the created
   */
  public Clock getCreated() {
    return created;
  }

  /**
   * @param created the created to set
   */
  public void setCreated(Clock created) {
    this.created = created;
  }

  /**
   * @return the disabled
   */
  public boolean isDisabled() {
    return disabled;
  }

  /**
   * @param disabled the disabled to set
   */
  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  /**
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * @param email the email to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return the passwordHash
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  /**
   * @param passwordHash the passwordHash to set
   */
  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * @return the lostPasswordHash
   */
  public String getLostPasswordHash() {
    return lostPasswordHash;
  }

  /**
   * @param lostPasswordHash the lostPasswordHash to set
   */
  public void setLostPasswordHash(String lostPasswordHash) {
    this.lostPasswordHash = lostPasswordHash;
  }

  /**
   * @return the lastLoggedIn
   */
  public Clock getLastLoggedIn() {
    return lastLoggedIn;
  }

  /**
   * @param lastLoggedIn the lastLoggedIn to set
   */
  public void setLastLoggedIn(Clock lastLoggedIn) {
    this.lastLoggedIn = lastLoggedIn;
  }

  /**
   * @return the locallity
   */
  public String getLocallity() {
    return locallity;
  }

  /**
   * @param locallity the locallity to set
   */
  public void setLocallity(String locallity) {
    this.locallity = locallity;
  }
}
