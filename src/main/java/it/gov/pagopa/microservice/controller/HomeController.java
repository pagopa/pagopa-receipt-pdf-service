package it.gov.pagopa.microservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
public class HomeController {

  @Value("${server.servlet.context-path}")
  String basePath;


  /**
   * @return redirect to Swagger page documentation
   */
  @Hidden
  @GetMapping("")
  public RedirectView home() {
    if (!basePath.endsWith("/")) {
      basePath += "/";
    }
    return new RedirectView(basePath + "swagger-ui.html");
  }

}
