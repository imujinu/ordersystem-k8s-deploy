package com.order.ordersystem.common.service;

import com.order.ordersystem.common.dto.CommonErrorDto;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice
@Slf4j
public class CommonExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalException(IllegalArgumentException e){
        log.error(e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),e.getMessage()),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> ValidationException(ValidationException e){
        log.error(e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(),e.getMessage()),HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> MethodArgumentNotValidException(MethodArgumentNotValidException e){
        String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        log.error(e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), errorMessage),HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> EntityNotFoundException(EntityNotFoundException e){
        log.error(e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.NOT_FOUND.value(),e.getMessage()),HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> error(Exception e){
        String errorMessage = e.getMessage();
        log.error(e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage),HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
