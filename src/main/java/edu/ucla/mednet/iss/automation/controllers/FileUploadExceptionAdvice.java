package edu.ucla.mednet.iss.automation.controllers;

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class FileUploadExceptionAdvice {
  private static Logger log = Logger.getLogger(FileUploadExceptionAdvice.class.getName());

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ModelAndView handleMaxSizeException(
      MaxUploadSizeExceededException exc,
      HttpServletRequest request,
      HttpServletResponse response) {

    ModelAndView modelAndView = new ModelAndView("mllp-multi-send-display");
    log.info("Exception triggered");
    modelAndView.getModel().put("message", "File too large!");
    return modelAndView;
  }
}