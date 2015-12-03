/* ============================================================================
*
* FILE: LogResponse.java
*
* MODULE DESCRIPTION:
* See class description
*
* Copyright (C) 2015 by
* ERICSSON
*
* The program may be used and/or copied only with the written
* permission from Ericsson Inc, or in accordance with
* the terms and conditions stipulated in the agreement/contract
* under which the program has been supplied.
*
* All rights reserved
*
* ============================================================================
*/
package com.weblogs.log4j;


public class LogResponse {
  String status;
  String getStatus() {
    return status;
  }
  void setStatus(String status) {
    this.status = status;
  }
  String getMessage() {
    return message;
  }
  void setMessage(String message) {
    this.message = message;
  }
  String message;
  public LogResponse(String status, String message) {
    super();
    this.status = status;
    this.message = message;
  }
  public LogResponse() {
    super();
  }
}
