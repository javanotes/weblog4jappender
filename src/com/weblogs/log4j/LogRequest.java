/* ============================================================================
*
* FILE: LogRequest.java
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

import java.lang.reflect.Array;
import java.lang.reflect.Field;

public class LogRequest {

  
  public LogRequest() {
    super();
  }
  public LogRequest(String applicationId, String logText) {
    super();
    this.applicationId = applicationId;
    this.logText = logText;
    this.searchTerms = new String[0];
  }
  
  String getApplicationId() {
    return applicationId;
  }
  void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
  
  String getLogText() {
    return logText;
  }
  void setLogText(String logText) {
    this.logText = logText;
  }
  
  private String level;
  public String[] getSearchTerms() {
    return searchTerms;
  }
  public void setSearchTerms(String[] searchTerms) {
    this.searchTerms = searchTerms;
  }

  //from Jettison
  private static String escapeChars(String string)
 {
    if (string == null || string.length() == 0) {
      return "\"\"";
    }

    char c = 0;
    int i;
    int len = string.length();
    StringBuilder sb = new StringBuilder(len + 4);
    String t;

    //sb.append('"');
    for (i = 0; i < len; i += 1) {
      c = string.charAt(i);
      switch (c) {
      case '\\':
      case '"':
        sb.append('\\');
        sb.append(c);
        break;
      case '/':
        // if (b == '<') {
        sb.append('\\');
        // }
        sb.append(c);
        break;
      case '\b':
        sb.append("\\b");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case '\f':
        sb.append("\\f");
        break;
      case '\r':
        sb.append("\\r");
        break;
      default:
        if (c < ' ') {
          t = "000" + Integer.toHexString(c);
          sb.append("\\u" + t.substring(t.length() - 4));
        } else {
          sb.append(c);
        }
      }
    }
    //sb.append('"');
    return sb.toString();
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("{");
    for(Field f : getClass().getDeclaredFields())
    {
      try 
      {
        if(!f.isAccessible())
          f.setAccessible(true);
        
        Object a = f.get(this);
        if(f.getType().isArray())
        {
          s.append("\"").append(f.getName()).append("\":").append("[");
          
          if(a != null)
          {
            for(int i=0; i<Array.getLength(a); i++)
            {
              s.append("\"").append(Array.get(a, i) != null ? escapeChars(Array.get(a, i).toString()) : "").append("\",");
            }
          }
          if(s.toString().endsWith(","))
          {
            s.deleteCharAt(s.lastIndexOf(","));
          }
          s.append("],");
        }
        else
        {
          s.append("\"").append(f.getName()).append("\":\"").append(a != null ? escapeChars(a.toString()) : "").append("\",");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if(s.toString().endsWith(","))
    {
      s.deleteCharAt(s.lastIndexOf(","));
    }
    s.append("}");
    return s.toString();
  }

  public static void main(String[] args) {
    LogRequests rr = new LogRequests();
    LogRequest lr = new LogRequest();
    rr.getBatch().add(lr);
    System.out.println(lr);
    lr = new LogRequest("applicationId", "logText");
    lr.setSearchTerms(new String[]{"test","x"});
    rr.getBatch().add(lr);
    System.out.println(lr);
    System.out.println(rr);
  }

  public String getLevel() {
    return level;
  }
  public void setLevel(String level) {
    this.level = level;
  }

  private String applicationId;//not null
  
  private String logText;
  private String[] searchTerms;
}
