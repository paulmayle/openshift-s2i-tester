package edu.ucla.mednet.iss.automation;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.camel.Route;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ReceiveConnection implements IReceiveConnection {

  private ApplicationContext appContext;
  private IActiveSessionList activeSessionList;
  private String routeId;
  private String sessionIdFileName;
  private int currentPort = 0;
  private String portMessage;  // message concerning the port connection state
  private static Logger log = Logger.getLogger(IReceiveConnection.class.getName());

  @Autowired
  public void setAppContext(ApplicationContext appContext) {
    this.appContext = appContext;
  }

  @Autowired
  public void setActiveSessionList(IActiveSessionList activeSessionList) {
    this.activeSessionList = activeSessionList;
  }


  @Override
  public void processAction(List action, String listenPort, String sessionId) throws Exception {

    if (action.size() > 1) {
      switch (((String) action.get(1)).toLowerCase()) {

        case "none":
          log.info("Action=none");
          portMessage = "Select the port to use";
          break;
        case "stop":
          log.info("Action=stop");
          stopCamelRoute();
          portMessage = "Select the port to use";
          break;
        case "port":
          int port = 0;
          try {
            port = Integer.parseInt(listenPort);
          } catch (NumberFormatException nfe) {
            port = 0;
          }

          log.info("Action=port->" + port);
          if (port < 1000) {
            portMessage = "Port " + port + " is reserved by the operation system - try a port over 1000";
          } else {
            currentPort = port;
            this.routeId = "mllp-in-" + port;
            this.sessionIdFileName = "mllp-received-" + port + ".hl7";
            if (isPortAvailable(port)) {
              startCamelRoute(port);
            } else {
              if (isCamelRouteRunning(port)) {
                // another browser is already listening on this port so we can piggy back on this session
                portMessage = "Port " + port + " is already listening - sharing the session";
              } else {
                // not us that is using the port
                log.info("Port in use");
                portMessage = "Port " + port + " is in use. Try another port";
              }
            }
          }
      }
    }
  }


  @Override
  public String getPortMessage() {
    return portMessage;
  }

  @Override
  public void stopCamelRoute() throws Exception {

    SpringCamelContext camelContext = (SpringCamelContext) appContext.getBean("camelContext");
    Route route = camelContext.getRoute(routeId);

    log.info("stopping route " + routeId);
    if (route != null) {
      log.info("Stopping  listener on port " + currentPort);
      camelContext.stopRoute(routeId);
      camelContext.removeRoute(route);
    }
    deleteFile(sessionIdFileName);
  }


  boolean isCamelRouteRunning(int port) {
    SpringCamelContext camelContext = (SpringCamelContext) appContext.getBean("camelContext");

    List<Route> routes = camelContext.getRoutes();
    for (Route route1 : routes) {
      log.fine("Route ID " + route1.getId() + "  URI: " + route1.getEndpoint().getEndpointUri());
      activeSessionList.setSession(route1.getId(), port);
      if (route1.getEndpoint().getEndpointUri().contains("mllp://0.0.0.0:" + port)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void startCamelRoute(int port) throws Exception {

    if (!isCamelRouteRunning(port)) {
      // No existing route - so start a new one
      SpringCamelContext camelContext = (SpringCamelContext) appContext.getBean("camelContext");
      ListenerRouteBuilder listenerRouteBuilder = new ListenerRouteBuilder();
      log.info("starting route " + routeId);
      listenerRouteBuilder.createRoute(camelContext, port, sessionIdFileName, routeId);
      Route route = camelContext.getRoute(routeId);
      log.info("Starting  listener on port " + port);
      portMessage = "Listening on port " + port;
      camelContext.addRoute(route);
      camelContext.startRoute(routeId);
    }

  }

  boolean isPortAvailable(int port) {
    try (Socket ignored = new Socket("localhost", port)) {
      return false;
    } catch (IOException ignored) {
      return true;
    }
  }

  /*
       called every 500mS by the browser
   */
  @Override
  public List getMessage() {
    List message = new ArrayList();
    message.add("--- waiting for message to be received ---");
    try {
      message = readFileInList(sessionIdFileName);
    } catch (Exception ex) {

    }
    if (message == null) {
      log.info("Message is null ");
      message = new ArrayList();
      message.add("--- waiting for message to be received ---");
    }
    try {
      if (currentPort != 0) {
        if (!isCamelRouteRunning(currentPort)) {
          portMessage = "No Longer Listening";
        }
      } else {
        portMessage = "No Longer Listening select new port ";
      }
    } catch (Exception e) {
      //message.add("Exception " + e.getMessage());
      portMessage = e.getMessage();
      e.getMessage();
    }
    return message;
  }


  List<String> readFileInList(String fileName) {

    List<String> lines = Collections.emptyList();
    try {
      String dir = "/tmp";
      lines =
          Files.readAllLines(Paths.get(dir, fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {

    }
    return lines;
  }


  void deleteFile(String fileName) {

    new File("/tmp/" + fileName).delete();
  }


}
