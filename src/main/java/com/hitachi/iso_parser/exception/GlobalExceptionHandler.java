package com.hitachi.iso_parser.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hitachi.iso_parser.dto.IsoParseResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IsoParseException.class)
    public ResponseEntity<IsoParseResponse> handleIsoParse(IsoParseException e) {
        IsoParseResponse response = new IsoParseResponse();
        response.setSuccess(false);
        response.setMessage("ISO parse failed: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<IsoParseResponse> handleBadRequest(HttpMessageNotReadableException e) {
        IsoParseResponse response = new IsoParseResponse();
        response.setSuccess(false);
        response.setMessage("Invalid request body. Send raw ISO HEX as text/plain.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(XmlParseException.class)
    public ResponseEntity<IsoParseResponse> handleXmlParse(XmlParseException e) {
        IsoParseResponse response = new IsoParseResponse();
        response.setSuccess(false);
        response.setMessage("XML parse failed: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<IsoParseResponse> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        String msg = "";
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) msg = msg + "; ";
            msg = msg + errors.get(i).getDefaultMessage();
        }
        IsoParseResponse response = new IsoParseResponse();
        response.setSuccess(false);
        response.setMessage("Validation failed: " + msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<IsoParseResponse> handleGeneric(Exception e) {
        IsoParseResponse response = new IsoParseResponse();
        response.setSuccess(false);
        response.setMessage("Processing failed: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
