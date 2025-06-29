package br.dev.leandro.spring.cloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        String exception
) {}
