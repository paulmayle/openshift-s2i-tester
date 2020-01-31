package edu.ucla.mednet.iss.automation;


import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.camel.Route;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
  private ApplicationContext appContext;
  private IActiveSessionList activeSessionList;


  @Autowired
  public void setActiveSessionList(IActiveSessionList activeSessionList) {
    this.activeSessionList = activeSessionList;
  }

  @Autowired
  public void setAppContext(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);


  @Scheduled(fixedRate = 30000)
  public void cleanStaleListeners() throws ExecutionException, InterruptedException {
    //log.info("Cleanup time {}", dateFormat.format(new Date()));

    SpringCamelContext camelContext = (SpringCamelContext) appContext.getBean("camelContext");

    List<Session> activeSessions = activeSessionList.getSessions();

    /*  First go through the active web routes
    The javascript status update will keep the timestamp updated.
    If the browser session goes away, the timestamp will not be updated, so after 30 seconds we kill the camel route
     */
    Iterator<Session> iterator = activeSessions.iterator();
    while (iterator.hasNext()) {
      Session session = iterator.next();
      log.debug("WEB ACTIVE Route ID " + session.getRouteId() + "  PORT: " + session.getPort() + "  ");
      LocalDateTime timeNow = (new Date()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
      LocalDateTime lastUpdate = session.getTimestamp().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
      if (lastUpdate.plusSeconds(30).isBefore(timeNow)) {
        // time to kill the listener
        iterator.remove();
      }
    }

    /* The Camel Route is still running at this point as we have just removed the web session entry from the list.
    This next loop compares running Camel routes with active web sessions and if there is not a match stops and removes the Camel Route /*
     */
    for (Route route : camelContext.getRoutes()) {
      log.info("CAMEL RUNNING Route ID " + route.getId() + "  URI: " + route.getEndpoint().getEndpointUri());
      if (!activeSessionList.containsSession(route.getId())) {
        log.info("We have a route running with no session ");
        stopCamelRoute(route);
      }
    }


  }


  void stopCamelRoute(Route route) {

    if (route != null) {
      try {
        SpringCamelContext camelContext = (SpringCamelContext) route.getCamelContext();
        log.info("stopping route " + route.getId());
        String port = route.getEndpoint().getEndpointUri().substring(15);  // bit of a hack to get the port from the URI
        String fileName = "mllp-received-" + port + ".hl7";
        log.info("file to delete [" + fileName + "]");
        camelContext.stopRoute(route.getId());
        camelContext.removeRoute(route);
        // Delete the file:
        new File("/tmp/" + fileName).delete();
      } catch (Exception e) {
        log.info("Failed to stop Camel Route " + e);
        e.printStackTrace();
      }
    }
  }


}