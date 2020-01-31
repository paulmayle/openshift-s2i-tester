package edu.ucla.mednet.iss.automation.controllers;

import com.google.gson.Gson;
import edu.ucla.mednet.iss.automation.FileStorageException;
import edu.ucla.mednet.iss.automation.ISendConnection;
import edu.ucla.mednet.iss.automation.SendResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


@Controller
public class ControllerFuseMessageSend {
  private static Logger log = Logger.getLogger(ControllerFuseMessageSend.class.getName());

  final static String cannedMessage = "MSH|^~\\&|||||20180420150320.028-0700||ADT^A01^ADT_A01|1101|T|2.6\r";

  @Value("${spring.servlet.multipart.max-file-size}")
  private String maxFileSizeString;

  @Autowired
  // private FileStorage fileStorageService;
  private ISendConnection ISendConnection;


  public ControllerFuseMessageSend(ISendConnection ISendConnection) {
    log.fine("Constructor called");
    if (ISendConnection != null) {
      log.fine("Set  sendConnection");
      this.ISendConnection = ISendConnection;
    }
  }


  @RequestMapping(value = "/send", produces = {MediaType.TEXT_HTML_VALUE})
  public String start(

      @RequestParam(name = "sendPort", required = false, defaultValue = "") String sendPort,
      @RequestParam(name = "sendHost", required = false, defaultValue = "") String sendHost,
      @RequestParam(name = "startAt", required = false, defaultValue = "1") String uploadOffset,
      @RequestParam(name = "hl7Message", required = false, defaultValue = cannedMessage) String hl7Message,
      @RequestParam(name = "submit", required = false, defaultValue = "none") ArrayList action,
      Model model) throws Exception {

    ISendConnection.setHost(sendHost);
    ISendConnection.setPort(sendPort);
    ISendConnection.setHl7Message(hl7Message);
    log.info("**********************************\n" + action + "\noffset: " + uploadOffset);
    int uploadIndex;
    try {
      uploadIndex = Integer.parseInt(uploadOffset);
    } catch (NumberFormatException nfe) {
      uploadIndex = 1;
    }

    String response = "";
    String actionRequest;

    // TODO Find out why this is sometimes an array and not others?
    if (action.size() > 1) {
      actionRequest = ((String) action.get(1)).toLowerCase();
    } else {
      actionRequest = ((String) action.get(0)).toLowerCase();
    }

    List hl7Messages = new ArrayList();
    hl7Messages.add(hl7Message);
    log.info("Action request: " + actionRequest);

    switch (actionRequest) {
      case "init":
        log.info("Initialize");
        ISendConnection.closeConnection();
        ISendConnection.setConnectedRequestState(false);
        ISendConnection.setMultiMessageMode(false);
        ISendConnection.setHost("");
        ISendConnection.setPort("");

        break;
      case "connect":
        log.info("connection request");
        ISendConnection.closeConnection();
        ISendConnection.setConnectedRequestState(true);
        if (ISendConnection.isMultiMessageMode()) {
          hl7Messages = readFile(ISendConnection.getUploadTargetLocation());
          // the index (offset) at which to start displaying yhe next 10 records in the browser
          ISendConnection.setUploadFileIndex(uploadIndex);
        }
        break;
      case "disconnect":
        log.info("DIS connection request");
        ISendConnection.closeConnection();
        ISendConnection.setConnectedRequestState(false);
        break;
      case "send":
        ISendConnection.setConnectedRequestState(true);
        response = ISendConnection.sendMessage();
        log.info("Send request");
        break;
      case "updateview": // multi message view - change the index to display
        log.info("Update the multi message view ");
        hl7Messages = readFile(ISendConnection.getUploadTargetLocation());
        ISendConnection.setUploadFileIndex(uploadIndex);
        break;
      case "multisend":
        log.info("Send entire file");
        hl7Messages = readFile(ISendConnection.getUploadTargetLocation());
        ISendConnection.setConnectedRequestState(true);
        ISendConnection.sendFileMessageAsync(hl7Messages);
        response = "async upload started ";
        model.addAttribute("status", "sending");
        break;
    }

    String hostAddress="not set";
    String hostName="not set";
    try {
      hostAddress = InetAddress.getLocalHost().getHostAddress();
      hostName = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    model.addAttribute("sendPort", sendPort);
    model.addAttribute("sendHost", sendHost);
    model.addAttribute("hl7Response", response);
    model.addAttribute("uploadOffset", uploadOffset);
    model.addAttribute("hostAddress", hostAddress);
    model.addAttribute("hostName", hostName);
    return getMessageView(hl7Messages, model, uploadIndex);
  }


  @RequestMapping("/sendUpdate")
  public String getsendUpdate(Model model) {

    String fragment = "fragments :: disConnectState";
    if (ISendConnection == null) {
      log.fine("connection is NULL");
    } else {
      if (ISendConnection.getConnectedRequestState()) {

        ISendConnection.openConnection();
      } else {
        log.fine("requested state is disconnect ");
        if (ISendConnection.isConnected()) {
          ISendConnection.closeConnection();
        }
      }
      if (ISendConnection.isConnected()) {
        log.fine(" connected ");
        //message = "connected";
        fragment = "fragments :: connectState";
      } else {
        log.fine(" NOT connected ");
        //message = "not connected";
        fragment = "fragments :: disConnectState";
      }
    }

    log.fine(ISendConnection.getConnection() + " is returning " + fragment);
    return fragment;
  }

  @RequestMapping("/multisendcount")
  @ResponseBody
  public String multiSendAckCount(HttpServletRequest request, HttpServletResponse response, Model model) {

    int count = ISendConnection.getAckCount();
    Gson gson = new Gson();
    //log.info("Ack count = " + count);

    SendResult sendResult = new SendResult();
    sendResult.ackCount = count;
    sendResult.message = ISendConnection.getStatus();

    log.info("JSON: count=" + count + " message = " + ISendConnection.getStatus());

    return gson.toJson(sendResult);

  }


  @RequestMapping(value = "/upload", produces = {MediaType.TEXT_HTML_VALUE})
  public String uploadFile(

      @RequestParam(name = "sendPort", required = false, defaultValue = "") String sendPort,
      @RequestParam(name = "sendHost", required = false, defaultValue = "") String sendHost,
      @RequestParam(name = "hl7Message", required = false, defaultValue = cannedMessage) String hl7Message,
      @RequestParam("file") MultipartFile fileMessage,
      @RequestParam(name = "submit", required = false, defaultValue = "none") ArrayList action,
      Model model) throws Exception {

    ISendConnection.setHost(sendHost);
    ISendConnection.setPort(sendPort);
    ISendConnection.setHl7Message(hl7Message);
    long multiplier = 1;

    if (maxFileSizeString.endsWith("KB")) {
      multiplier = 1000;
    } else if (maxFileSizeString.endsWith("MB")) {
      multiplier = 1000000;
    } else if (maxFileSizeString.endsWith("GB")) {
      multiplier = 1000000000;
    }
    long maxFileSize = multiplier * Long.parseLong(maxFileSizeString.replaceAll("[^\\d.]", ""));


    long fileSize = fileMessage.getSize();
    log.info("File size=" + fileSize);
    List hl7Messages = new ArrayList();
    if (fileSize < maxFileSize) {

      log.info("uploaded file");
      Path targetLocation = saveUploadedFile(fileMessage);
      log.info("saved file " + fileMessage.getOriginalFilename());

      ISendConnection.setUploadTargetLocation(targetLocation);
      // Normalize file name
      String fileName = StringUtils.cleanPath(fileMessage.getOriginalFilename());
      log.info("cleaned saved file " + fileName);

      hl7Messages = readFile(targetLocation);
      if (hl7Messages.size() > 1) {
        log.info("multi message mode");
        ISendConnection.setMultiMessageMode();
      } else {
        log.info("single message mode");
        ISendConnection.setMultiMessageMode(false);
      }
      ISendConnection.setUploadFileName(fileName);
      ISendConnection.setUploadFileIndex(0);
    } else {
      log.info("file size too large");
      ISendConnection.setMultiMessageMode(false);
      model.addAttribute("hl7Response", "file too large to load - try less than 200M");
    }
    model.addAttribute("sendPort", ISendConnection.getPort());
    model.addAttribute("sendHost", ISendConnection.getHost());

    log.info("HOST: " + ISendConnection.getHost());
    log.info("PORT: " + ISendConnection.getPort());

    return getMessageView(hl7Messages, model, 1);
  }


  String getMessageView(List<String> hl7Messages, Model model, int offset) {
    String view;
    if (--offset < 0) {
      offset = 0;
    }
    int limit = 10; // max of 10 messages
    if (ISendConnection.isMultiMessageMode()) {

      // multi-message mode
      // copy the set we need
      List<String[]> cutDownList = new ArrayList<>();
      //log.info("offset=" + offset + " limit=" + limit);
      if (hl7Messages.size() < offset + limit) {
        offset = hl7Messages.size() - limit;
        if (offset < 0) {
          offset = 0;
        }
        //log.info("offset=" + offset + " limit=" + limit);
      }
      if (limit > hl7Messages.size()) {
        limit = hl7Messages.size();
        //log.info("offset=" + offset + " limit=" + limit);
      }
      for (int i = offset; i < offset + limit; i++) {
        //log.info("i=" + i + "  offset=" + offset + " limit=" + limit);
        String[] indexedMessage = new String[2];
        indexedMessage[0] = Integer.toString(i + 1);
        indexedMessage[1] = hl7Messages.get(i);
        cutDownList.add(indexedMessage);
      }
      model.addAttribute("hl7Messages", cutDownList);
      model.addAttribute("multiMessageSize", hl7Messages.size());
      view = "mllp-multi-send-display";
    } else {
      model.addAttribute("hl7Message", hl7Messages.get(0));
      view = "mllp-send-display";
    }
    return view;
  }


  private List<String> readFile(Path targetLocation) throws IOException {

    List<String> hl7Messages = new ArrayList<>();
    log.info("Reading file ");

    BufferedReader reader = Files.newBufferedReader(targetLocation);
    String line;
    StringBuilder message = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("MSH|") && message.length() != 0) {
        hl7Messages.add(message.toString());
        message.setLength(0);
      }
      message.append(line)
             .append("\r");
    }
    hl7Messages.add(message.toString());  // last one won't have a following MSH

    return hl7Messages;

  }


  Path saveUploadedFile(MultipartFile fileMessage) {

    Path fileStorageLocation = Paths.get("/tmp/hl7-uploads").toAbsolutePath().normalize();
    Path targetLocation;

    try {
      Files.createDirectories(fileStorageLocation);
    } catch (Exception ex) {
      throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
    }
    // Normalize file name
    String fileName = StringUtils.cleanPath(fileMessage.getOriginalFilename());

    try {
      // Check if the file's name contains invalid characters
      if (fileName.contains("..")) {
        throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
      }

      // Copy file to the target location (Replacing existing file with the same name)
      targetLocation = fileStorageLocation.resolve(fileName);
      Files.copy(fileMessage.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    } catch (IOException ex) {
      throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
    }
    return targetLocation;
  }


}
