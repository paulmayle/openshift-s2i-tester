package edu.ucla.mednet.iss.automation;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.scheduling.annotation.Async;

public class SendConnection implements ISendConnection {
  private static Logger log = Logger.getLogger(SendConnection.class.getName());
  private Socket clientSocket;
  private String hl7Message;
  private String host;
  private String uploadFileName;
  private String asyncStatus = "";
  private Path targetLocation;
  private int ackCount = 0;
  private int uploadFileOffset = 0;
  private int port = -1;
  private boolean connectedRequestState = false;
  private boolean multiMessageMode = false;

  @Override
  public int getAckCount() {
    return ackCount;
  }

  public void setHl7Message(String hl7Message) {
    this.hl7Message = hl7Message;
  }

  @Override
  public void setHost(String host) {

    log.info("Set host: " + host.trim());
    if (this.host != host.trim()) {
      // host has changed so disconnect
      closeConnection();
      this.host = host.trim();
      setStatus("host changed");
    }
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public String getPort() {
    return String.valueOf(port);
  }

  @Override
  public boolean isMultiMessageMode() {
    return multiMessageMode;
  }

  @Override
  public void setMultiMessageMode() {
    setMultiMessageMode(true);
  }

  @Override
  public void setMultiMessageMode(boolean mode) {
    multiMessageMode = mode;
  }

  @Override
  public void setUploadFileName(String fileName) {
    uploadFileName = fileName;
  }

  @Override
  public void setUploadFileIndex(int offset) {
    uploadFileOffset = offset;
  }

  @Override
  public void setUploadTargetLocation(Path targetLocation) {
    this.targetLocation = targetLocation;
  }

  @Override
  public Path getUploadTargetLocation() {
    return targetLocation;
  }

  @Override
  public String getUploadFileName() {
    return uploadFileName;
  }

  @Override
  public int getUploadFileIndex() {
    return uploadFileOffset;
  }


  @Override
  public void setPort(String port) {

    log.info("Set port: " + port);

    int iPort = 0;
    try {
      iPort = Integer.parseInt(port.trim());
    } catch (NumberFormatException nfe) {
    }
    if (iPort != this.port) {
      // port has changed so disconnect
      closeConnection();
      this.port = iPort;
      setStatus("port changed");
    }
  }


  public String getConnection() {
    return host + ":" + port;
  }

  @Override
  public boolean getConnectedRequestState() {
    return connectedRequestState;
  }

  @Override
  public void setConnectedRequestState(boolean c) {
    connectedRequestState = c;
  }

  @Override
  public String sendMessage() {
    if (host == null || host.isEmpty() || port < 1) {
      log.info("error - can not send without a host and port");
      return "host and port need to be set";
    }
    DataOutputStream out;
    BufferedReader in;
    String resp = "nothing received";
    if (openConnection()) {
      try {
        clientSocket.setSoTimeout(15000);  // wait for 15 seconds
        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        log.fine("Send message: " + hl7Message.replace("\r", "\n"));
        out.write(wrapMessage(hl7Message));
        out.flush();
        log.fine("Wait up to 15 seconds for reply");

        resp = readReply(in);
        log.fine("Got reply: " + resp.replace("\r", "\n"));
      } catch (IOException e) {
        log.info("IO error " + e);
      }
    } else {
      resp = "not connected";
    }
    return resp;
  }


  @Override
  public String sendFileMessage(List<String> hl7Messages) {
    sendFileMessageAsync(hl7Messages);
    return asyncStatus;
  }


  void setStatus(String status) {
    asyncStatus = status;
  }

  @Override
  public String getStatus() {

    return asyncStatus;

  }


  String readReply(BufferedReader in) throws IOException {
    //log.info("read the  reply");
    StringBuilder ack = new StringBuilder();
    int ch;
    while ((ch = in.read()) != 0x1c) {
      if (ch == -1) {
        throw new IOException("lost connection");
      }
      ack.append((char) ch);
    }
    return unwrapResponse(ack.toString());
  }


  @Override
  @Async
  public void sendFileMessageAsync(List<String> hl7Messages) {
    log.info("Sending ....");

    if (host == null || host.isEmpty() || port < 1) {
      log.info("error - can not send without a host and port");
      setStatus("Set host and Port");
      return;
    }

    String failureMessage = "";
    int expectedCount = hl7Messages.size();

    if (openConnection()) {
      try {
        //log.info("Wait up to 15 seconds for reply");
        clientSocket.setSoTimeout(15000);  // wait for 5 seconds
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        ackCount = 0;

        for (String message : hl7Messages) {
          //log.info("Send message: " + message.replace("\r", "\n"));
          out.write(wrapMessage(message));
          out.flush();

          String resp = readReply(in);
          int msa = resp.indexOf("\rMSA|");
          if (msa > 0) {
            String ack2 = resp.substring(msa + 5, msa + 7);
            if ("AA".equalsIgnoreCase(ack2) || "CA".equalsIgnoreCase(ack2)) {
              // good ack
              ackCount++;
            } else {
              failureMessage = resp.replace("\r", "\n");
              break;
            }
          } else {
            failureMessage = "Error failed to find the MSA Segment: \n" + resp.replace("\r", "\n");
            break;
          }
        }
      } catch (IOException e) {
        log.info("Never received a reply - waited for 15 seconds ...");
        log.info("IO error " + e);
        failureMessage = "Timed out waiting for ack!";
      }
    } else {
      failureMessage = "Not connected!";
    }
    if (ackCount > 0) {
      if (failureMessage != null && !failureMessage.isEmpty()) {
        setStatus("ERROR: \n Only received " + ackCount + " good replies.  Expected " + expectedCount + " \n" + failureMessage);
      } else {
        if (expectedCount == ackCount) {
          setStatus("Success : \n Received " + ackCount + " good replies, and we expected " + expectedCount + " \n");
        } else {
          setStatus("Error: \n Received " + ackCount + " good replies, but we expected " + expectedCount + " \n");
        }
      }
    } else {
      setStatus("Failed!: \n Received no good replies.   Expected " + expectedCount + " \n" + failureMessage);
    }
    ackCount = -1;
  }


  void printInHex(String arg) {
    for (int i = 0; i < arg.length(); i++) {
      String ch = arg.substring(i, i + 1);
      System.out.print(String.format("%02x ", new BigInteger(1, ch.getBytes())));
    }

  }

  private String unwrapResponse(String readLine) {

    //printInHex(readLine);
    readLine = readLine.substring(1);
    return readLine;
  }

  private byte[] wrapMessage(String hl7Message) {
    byte[] b = hl7Message.replace("\n", "\r").replace("\r\r", "\r").getBytes();
    byte[] c = new byte[b.length + 3];
    c[0] = 0x0b;
    for (int i = 0; i < b.length; i++) {
      c[i + 1] = b[i];
    }
    c[b.length + 1] = 0x1c;
    c[b.length + 2] = '\r';

    return c;
  }


  @Override
  public boolean openConnection() {
    boolean connected = false;

    if (host == null || host.isEmpty() || port == -1) {
      log.fine("wait for both host and port to be set");
    } else {
      log.fine("=================open connection ===============   ");
      if (isConnected()) {
        log.fine("already connected.....   ");
        connected = true;
      } else {
        try {
          log.fine("  opening a new connection .....   ");
          clientSocket = new Socket(host, port);
          connected = true;
        } catch (IOException ex) {
          log.fine("CLOSE socket due to : " + ex);
          closeConnection();
        }
      }
    }
    return connected;
  }

  @Override
  public void closeConnection() {

    try {
      if (clientSocket != null) {
        clientSocket.close();
      }
    } catch (Exception e) {
      log.info("Close failed " + e);
    }
  }

  @Override
  public boolean isConnected() {

    InputStream ip = null;
    boolean connectState = true;
    log.fine(" checking connection ");
    if (clientSocket == null) {
      log.fine(" clientSocket is null  ");
      connectState = false;
    } else if (!clientSocket.isConnected()) {
      log.fine(" not connected ");
      connectState = false;
    } else if (clientSocket.isClosed()) {
      log.fine("clientSocket is closed  ");
      connectState = false;
    }

    if (connectState != false) {  // looks like we are connected so try a read to be sure ...
      try {
        ip = clientSocket.getInputStream();
        clientSocket.setSoTimeout(10);  // not really expecting to read anything so just wait a few milliseconds
        log.fine(" .. read .. ");
        int q = ip.read();
        if (q == -1) {  // this is the real test, as -1 means the other end has dropped the connection
          log.fine("connect state is down");
          connectState = false;
        }
        log.fine("read=" + q);
      } catch (Exception e) {
        log.fine("Exception=" + e);
      }
    }
    log.fine("Returning connect state = " + connectState);
    return connectState;
  }
}
