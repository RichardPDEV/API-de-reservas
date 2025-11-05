package com.example.reservas.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Error de validación de negocio. Se mapea a 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends RuntimeException {
  public ValidationException(String message) { super(message); }
  public ValidationException(String message, Throwable cause) { super(message, cause); }
}