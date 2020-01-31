package edu.ucla.mednet.iss.automation.controllers;

import edu.ucla.mednet.iss.automation.IReceiveConnection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ControllerFuseMessageReceive {

  private static Logger log = Logger.getLogger(ControllerFuseMessageReceive.class.getName());
  private IReceiveConnection ReceiveConnection;

  @Autowired
  public void setReceiveConnection(IReceiveConnection receiveConnection) {
    ReceiveConnection = receiveConnection;
  }

  @RequestMapping(value = "/listen", produces = {MediaType.TEXT_HTML_VALUE})
  public String start(

      HttpSession session,
      @RequestParam(name = "listenPort", required = false, defaultValue = "") String listenPort,
      @RequestParam(name = "submit", required = false, defaultValue = "none") ArrayList action,
      Model model) throws Exception {

    ReceiveConnection.processAction(action,listenPort, session.getId());

    String hostAddress="not set";
    String hostName="not set";
    try {
      hostAddress = InetAddress.getLocalHost().getHostAddress();
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }

    model.addAttribute("listenPort", listenPort);
    model.addAttribute("hostAddress", hostAddress);
    model.addAttribute("hostName", hostName);
    return "mllp-receive-display";
  }


  /**
   * This is called frequently by the browser (500mS) to get the latest message received
   * @param model
   * @return
   */

  @RequestMapping("/lastMessage")
  public String getlastMessage(Model model) {
    model.addAttribute("message", ReceiveConnection.getMessage());
    model.addAttribute("portMessage", ReceiveConnection.getPortMessage());

    return "fragments :: lastMessage";
  }
}
