package com.example.reservas.service.cache;

import java.time.LocalDate;

public final class CacheKeys {
  private CacheKeys() {}
  public static String availKey(Long resourceId, LocalDate date) {
    return "avail:" + resourceId + ":" + date;
  }
}