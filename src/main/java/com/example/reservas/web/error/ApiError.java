package com.example.reservas.web.error;

import java.util.Map;

public record ApiError(String timestamp, String path, int status, String code, String message, Map<String, String> details) {}
