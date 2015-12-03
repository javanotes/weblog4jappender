/* ============================================================================
*
* FILE: LogGenerator.java
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
package com.weblogs.log4j.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.weblogs.log4j.WeblogsAppender;

public class LogGenerator {

  static void initLogger(String applicationId, String serviceUrl, int batchSize)
  {
    ConsoleAppender console = new ConsoleAppender(); //create appender
    //configure the appender
    String PATTERN = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
    console.setLayout(new PatternLayout(PATTERN)); 
    console.setThreshold(Level.DEBUG);
    console.activateOptions();
    //add appender to any Logger (here is root)
    Logger.getRootLogger().addAppender(console);
    
    WeblogsAppender webappender = new WeblogsAppender();
    webappender.setThreshold(Level.DEBUG);
    webappender.setLayout(new PatternLayout(PATTERN));
    webappender.setApplicationId(applicationId);
    webappender.setServiceUrl(serviceUrl);
    webappender.setBatchSize(batchSize);
    webappender.activateOptions();
    Logger.getRootLogger().addAppender(webappender);
    
  }
  private static final List<String> argList = new ArrayList<>();
  
  private static void mRaiseEx(int i) throws Exception
  {
    if(i == 0)
      throw new Exception("This is a deep exception stack trace!");
    mRaiseEx(--i);
  }
  
  public static void main(String[] args) {
    argList.addAll(Arrays.asList(args));
    
    String serviceUrl = argList.contains("-u") ? argList.get(argList.indexOf("-u")+1) : "http://localhost:8080/weblogs/api/ingestbatch";
    int batchSize = argList.contains("-s") ? Integer.valueOf(argList.get(argList.indexOf("-s")+1)) : 10;
    String applicationId = argList.contains("-i") ? argList.get(argList.indexOf("-i")+1) : "$appID";;
    initLogger(applicationId, serviceUrl, batchSize);
    
    Logger log = Logger.getLogger(LogGenerator.class);
    
    int iteration = argList.contains("-n") ? Integer.valueOf(argList.get(argList.indexOf("-n")+1)) : 100;

    for (int i = 0; i < iteration; i++) {
      if(i % 20 == 0){
        try {
          mRaiseEx(20);
        } catch (Exception e) {
          log.error("This is a exception log at  ["+i+"] ", e);
        }
      }
      else if(i % 4 == 0)
        log.info("This is a info log at  ["+i+"]");
      else
        log.debug("This is a debug log at  ["+i+"]");
      
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        
      }
    }
  }

}
