package com.example.reservas.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Recurso no encontrado. Se mapea a 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) { super(message); }
  public NotFoundException(String message, Throwable cause) { super(message, cause); }
}