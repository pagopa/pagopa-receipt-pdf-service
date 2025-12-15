package it.gov.pagopa.receipt.pdf.service.producer;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Qualifier
@Retention(RUNTIME)
@Target({PARAMETER, FIELD, METHOD, TYPE})
public @interface CartContainer {}
