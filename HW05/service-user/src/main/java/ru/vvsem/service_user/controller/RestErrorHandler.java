package ru.vvsem.service_user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class RestErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {

        if (ex.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                && ex.getReason() != null
                && ex.getReason().contains("Random error")) {

            log.warn("Random error triggered: {}", ex.getReason());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getReason());
    }
}
