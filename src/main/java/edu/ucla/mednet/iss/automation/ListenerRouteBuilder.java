package edu.ucla.mednet.iss.automation;

import static org.apache.camel.language.simple.SimpleLanguage.expression;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;

public class ListenerRouteBuilder {
  String mllpHost = "0.0.0.0";

  public void createRoute(CamelContext context, Integer port, String tempFileName, String routeId) throws Exception {
    RouteBuilder builder = new RouteBuilder() {

      public void configure() {
        fromF("mllp://%s:%d", mllpHost, port)
            .routeId(routeId)
            .autoStartup(false)
            .setExchangePattern(ExchangePattern.InOnly)
            .setHeader(Exchange.FILE_NAME, expression(tempFileName))
            .to("file:/tmp")
            .setHeader(Exchange.FILE_NAME, expression("mllp-received-${date:now:yyyyMMdd-HHmmss}.hl7"))
            .to("file:/tmp")
            .id(routeId);
      }
    };

    builder.addRoutesToCamelContext(context);
  }

}
