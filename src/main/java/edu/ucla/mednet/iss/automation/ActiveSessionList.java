package edu.ucla.mednet.iss.automation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

public class ActiveSessionList implements IActiveSessionList {
  private List<Session> sessionList = new ArrayList();


  @Override
  public void setSession(String id, int port) {

    Date date=new Date();

    boolean existingEntry=false;
    ListIterator<Session> iterator = sessionList.listIterator();
    while (iterator.hasNext()) {
      Session session = iterator.next();
      if (session.getPort()==port) {
        // already exists
        existingEntry=true;
        session.setPort(port);
        session.setRouteId(id);
        session.setTimestamp(date);
        iterator.set(session);
      }
    }
    if(!existingEntry) {
      Session session = new Session();
      session.setPort(port);
      session.setRouteId(id);
      session.setTimestamp(date);
      sessionList.add(session);
    }

  }

  @Override
  public void removeSession(int port) {
    for (int i = 0; i < sessionList.size(); i++) {
      if (sessionList.get(i).getPort() == port) {
        sessionList.remove(i);
        break;
      }
    }
  }

  @Override
  public void removeSession(String id) {
    for (int i = 0; i < sessionList.size(); i++) {
      if (sessionList.get(i).getRouteId().equalsIgnoreCase(id)) {
        sessionList.remove(i);
        break;
      }
    }
  }

  @Override
  public boolean containsSession(int port) {
    for (int i = 0; i < sessionList.size(); i++) {
      if (sessionList.get(i).getPort() == port) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsSession(String id) {
    for (int i = 0; i < sessionList.size(); i++) {
      if (sessionList.get(i).getRouteId().equalsIgnoreCase(id)) {
        return true;
      }
    }
    return false;
  }


  @Override
  public List<Session> getSessions() {
    return this.sessionList;
  }


}
