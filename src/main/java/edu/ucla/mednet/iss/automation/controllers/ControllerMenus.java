package edu.ucla.mednet.iss.automation.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.logging.Logger;

@Controller
public class ControllerMenus {
  private static Logger log = Logger.getLogger(ControllerMenus.class.getName());

  @Value("${pac4j.centralLogout.defaultUrl:#{null}}")
  String defaultUrl;

  @Value("${pac4j.centralLogout.logoutUrlPattern:#{null}}")
  String logoutUrlPattern;

  @RequestMapping({"/","/home", "/index.html", "/index"})
  public String index(Model model) {
    String hostAddress="not set";
    String hostName="not set";
    try {
       hostAddress = InetAddress.getLocalHost().getHostAddress();
       hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    model.addAttribute("hostAddress", hostAddress);
    model.addAttribute("hostName", hostName);
    return "index";
  }

  @GetMapping("/fragments")
  public String getHome() {
    return "fragments.html";
  }


}
