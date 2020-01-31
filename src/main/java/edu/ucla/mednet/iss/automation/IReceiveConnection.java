package edu.ucla.mednet.iss.automation;


import java.util.List;

public interface IReceiveConnection {

  void stopCamelRoute() throws Exception;
  void startCamelRoute(int port) throws Exception;

  void processAction(List action, String listenPort, String sessionId) throws Exception;

  String getPortMessage();
  List getMessage();

}
