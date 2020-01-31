package edu.ucla.mednet.iss.automation;


import java.util.List;

public interface IActiveSessionList {

  void setSession(String id, int port);
  void removeSession(int port);
  void removeSession(String id);

  boolean containsSession(int port);
  boolean containsSession(String id);

  List<Session> getSessions();

}
