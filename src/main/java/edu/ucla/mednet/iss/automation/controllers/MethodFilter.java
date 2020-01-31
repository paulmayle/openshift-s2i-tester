package edu.ucla.mednet.iss.automation.controllers;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


@Component
public class MethodFilter extends OncePerRequestFilter {
  private static Logger log = Logger.getLogger(MethodFilter.class.getName());

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS
    log.fine("REQUEST: "+request.getMethod());
    switch (request.getMethod()) {
      case "POST":
      case "GET":
        filterChain.doFilter(request, response);
        break;

      case "DELETE":
      case "OPTIONS":
      case "PATCH":
      case "PUT":
      case "HEAD":
      default:
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

    }

  }
}