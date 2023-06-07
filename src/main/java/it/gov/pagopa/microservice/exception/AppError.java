package it.gov.pagopa.microservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public enum AppError {
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
      "Something was wrong");

  public final HttpStatus httpStatus;
  public final String title;
  public final String details;


  AppError(HttpStatus httpStatus, String title, String details) {
    this.httpStatus = httpStatus;
    this.title = title;
    this.details = details;
  }
}


