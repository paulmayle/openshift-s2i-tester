package edu.ucla.mednet.iss.automation;

import com.google.gson.annotations.Expose;

public class SendResult {

  @Expose()
  public int ackCount;

  @Expose()
  public String message = null;

}
