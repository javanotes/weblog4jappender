/* ============================================================================
*
* FILE: LogRequests.java
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

import java.util.ArrayList;
import java.util.List;


public class LogRequests {

  public LogRequests() {
    super();
  }

  public LogRequests(List<LogRequest> batch) {
    super();
    this.batch = batch;
  }



  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("{\"batch\":[");
    if(batch != null)
    {
      for(LogRequest b : batch)
      {
        s.append(b).append(',');
      }
    }
    if(s.toString().endsWith(","))
    {
      s.deleteCharAt(s.lastIndexOf(","));
    }
    s.append("]}");
    return s.toString();
  }

  List<LogRequest> getBatch() {
    return batch;
  }

  void setBatch(List<LogRequest> batch) {
    this.batch.addAll(batch);
  }

  private List<LogRequest> batch = new ArrayList<>();
}
