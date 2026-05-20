package com.leilao.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento global de erros da API.
 * Usa o padrão RFC 9457 (ProblemDetail) — resposta padronizada em JSON.
 *
 * Exemplo de resposta para 404:
 * {
 *   "type":   "https://leilao.com/erros/nao-encontrado",
 *   "title":  "Recurso não encontrado",
 *   "status": 404,
 *   "detail": "Lote não encontrado: 99"
 * }
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — Lote não encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        log.warn("[api] 404: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso não encontrado");
        pd.setType(URI.create("https://leilao.com/erros/nao-encontrado"));
        return pd;
    }

    // 400 — Parâmetros inválidos (@Valid falhou)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "inválido",
                (a, b) -> a
            ));

        log.warn("[api] 400 validação: {}", erros);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Parâmetros inválidos");
        pd.setType(URI.create("https://leilao.com/erros/validacao"));
        pd.setProperty("campos", erros);
        return pd;
    }

    // 500 — Erro interno inesperado
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("[api] 500: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Erro interno. Tente novamente mais tarde."
        );
        pd.setTitle("Erro interno");
        pd.setType(URI.create("https://leilao.com/erros/interno"));
        return pd;
    }
}
