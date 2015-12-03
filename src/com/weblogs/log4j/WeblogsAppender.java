/* ============================================================================
*
* FILE: WeblogsAppender.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class WeblogsAppender extends AppenderSkeleton {
      
  public void close() {
    
    try {
      doPost();
    } catch (IOException e) {
      LogLog.error("WeblogsAppender -- Stackrace", e);
    }
  }
  /*
   * --- Properties --
   */
  private String serviceUrl = "";
  private String applicationId = "applicationId";
  private int batchSize = 10;
  private long flushSecs = 5;
  /* ------------------ */
  
  public String getServiceUrl() {
    return serviceUrl;
  }
  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }
  public String getApplicationId() {
    return applicationId;
  }
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
  
  public int getBatchSize() {
    return batchSize;
  }
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }
  
  private BlockingQueue<LogRequest> requests;
  //from synchronized block
  private boolean doPost() throws IOException
  {
    //LogLog.debug("================= Weblogging =================");
    URL url = new URL(serviceUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    
    BufferedReader br = null;
    try
    {
      
      conn.setDoOutput(true);
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "application/json");
      
      LogRequests req = new LogRequests();
      requests.drainTo(req.getBatch());
      String input = req.toString();
      //System.out.println(input);
      
      OutputStream os = conn.getOutputStream();
      os.write(input.getBytes());
      os.flush();
      
      if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException("Weblogging failed : HTTP code : "
          + conn.getResponseCode());
      }
      
      StringBuilder output = new StringBuilder();
      String line;
      
      br = new BufferedReader(new InputStreamReader(
          (conn.getInputStream())));
      
      while ((line = br.readLine()) != null) {
        output.append(line);
      }
      
      String lr = output.toString();
      //System.out.println(lr);
      boolean success = lr != null && lr.contains("SUCCESS");
      if(!success)
        throw new IOException("Weblogging failed : ["+lr+"] HTTP code : "
            + conn.getResponseCode());
        
      if(success){
        requests.clear();
        lastCleared = System.currentTimeMillis();
      }
      
      return success;
      
    }
    finally
    {
      if(br != null)
        br.close();
      conn.disconnect();
    }

    
  }
  private long lastCleared;
  public void activateOptions() {
   if(layout == null)
     layout = new SimpleLayout();
   
   requests = new ArrayBlockingQueue<>(batchSize);
   threads = Executors.newCachedThreadPool(new ThreadFactory() {
    int n=0;
    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "WeblogsAppender-Async-Worker-"+(n++));
      t.setDaemon(true);
      return t;
    }
  });
   timer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
    
    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "WeblogsAppender-Scheduled-Cleaner");
      t.setDaemon(true);
      return t;
    }
  });
   timer.scheduleAtFixedRate(new Runnable() {
    
    @Override
    public void run() {
      if (lock.tryLock()) {
        try {
          if (!requests.isEmpty() && (System.currentTimeMillis() - lastCleared) > flushSecs) {
            doPost();
          } 
        }
        catch (IOException e) {
          LogLog.error("WeblogsAppender -- Stackrace", e);
        }
        finally{
          lock.unlock();
        }
      }
      
    }
  }, flushSecs, flushSecs, TimeUnit.SECONDS);
   
   Runtime.getRuntime().addShutdownHook(new Thread(){
     public void run()
     {
        threads.shutdown();
        try {
          threads.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {

        }

        timer.shutdownNow();
        try {
          timer.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e1) {

        }
        if (!requests.isEmpty()) {
          try {
            doPost();
          } catch (IOException e) {
            LogLog.error("WeblogsAppender -- Stackrace", e);
          } 
        }
        if (!requests.isEmpty()) {
          LogLog.warn("WeblogsAppender -- Not all logs have been weblogged!");
        }
      }
   });
  }
  public boolean requiresLayout() {
    return false;
  }

  @Override
  public void doAppend(LoggingEvent event) {
    if(closed) {
      LogLog.error("Attempted to append to closed appender named ["+name+"].");
      return;
    }
    
    if(!isAsSevereAsThreshold(event.getLevel())) {
      return;
    }

    Filter f = this.headFilter;
    
    FILTER_LOOP:
    while(f != null) {
      switch(f.decide(event)) {
      case Filter.DENY: return;
      case Filter.ACCEPT: break FILTER_LOOP;
      case Filter.NEUTRAL: f = f.getNext();
      }
    }
    
    this.append(event);    
  }
  private ExecutorService threads;
  private ScheduledExecutorService timer;
  
  private final Lock lock = new ReentrantLock();
  @Override
  protected void append(LoggingEvent event) {
    
    final LogRequest req = new LogRequest(applicationId, event.getRenderedMessage());
    if(event.getThrowableInformation() != null)
    {
      StringBuilder s = new StringBuilder(req.getLogText());
      for(String trace : event.getThrowableStrRep()){
        s.append("\n").append(trace);
      }
      req.setLogText(s.toString());
    }
    
    req.setLevel(event.getLevel().toString());
    //System.out.println("===========>>>>>>>>>>>>>>>>>"+req);
    
    boolean offered = requests.offer(req);
    if (!offered) {
      threads.submit(new Runnable() {

        @Override
        public void run() {
          boolean offered = false;
          do 
          {
            if (lock.tryLock()) {
              try {
                offered = requests.offer(req);
                if (!offered) {
                  try {
                    doPost();

                  } catch (IOException e) {
                    LogLog.error("WeblogsAppender -- Stackrace", e);
                  }
                }
              } finally{
                lock.unlock();
              }
            }
            else
              offered = requests.offer(req);
          } while (!offered);

        }
      });
    }
    
  }

}
