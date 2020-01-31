package edu.ucla.mednet.iss.automation;


import java.nio.file.Path;
import java.util.List;

public interface ISendConnection {


  boolean openConnection();

  /**
   * Tests the connection.
   *
   * @return true if connected
   */
  boolean isConnected();

  String getPort();
  String getHost();

  void closeConnection();

  void setPort(String port);

  void setHost(String host);

  void setUploadFileName(String fileName);

  void setUploadFileIndex(int offset);

  void setUploadTargetLocation(Path targetLocation);

  Path getUploadTargetLocation();

  int getAckCount();

  String getStatus();


  String getUploadFileName();

  int getUploadFileIndex();

  String sendMessage();

  String sendFileMessage(List<String> messages);

  void sendFileMessageAsync(List<String> hl7Messages);

  void setHl7Message(String hl7Message);


  String getConnection();

  boolean getConnectedRequestState();

  void setConnectedRequestState(boolean c);

  boolean isMultiMessageMode();

  void setMultiMessageMode();

  void setMultiMessageMode(boolean mode);
}
