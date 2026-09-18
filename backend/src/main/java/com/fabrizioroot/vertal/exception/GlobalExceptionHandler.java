package com.fabrizioroot.vertal.exception;

import com.fabrizioroot.vertal.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ErrorResponseDto> notFound(ResourceNotFoundException e, HttpServletRequest r) { return response(HttpStatus.NOT_FOUND, e, r); }
    @ExceptionHandler(UnauthorizedOperationException.class) ResponseEntity<ErrorResponseDto> forbidden(UnauthorizedOperationException e, HttpServletRequest r) { return response(HttpStatus.FORBIDDEN, e, r); }
    @ExceptionHandler({BadRequestException.class, MethodArgumentNotValidException.class}) ResponseEntity<ErrorResponseDto> badRequest(Exception e, HttpServletRequest r) { return response(HttpStatus.BAD_REQUEST, e, r); }
    @ExceptionHandler(ConflictException.class) ResponseEntity<ErrorResponseDto> conflict(ConflictException e, HttpServletRequest r) { return response(HttpStatus.CONFLICT, e, r); }
    private ResponseEntity<ErrorResponseDto> response(HttpStatus s, Exception e, HttpServletRequest r) { return ResponseEntity.status(s).body(new ErrorResponseDto(Instant.now(), s.value(), s.getReasonPhrase(), e.getMessage(), r.getRequestURI())); }
}