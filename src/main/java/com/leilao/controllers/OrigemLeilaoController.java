package com.leilao.controllers;

import com.leilao.model.OrigemLeilao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Controller RESTful para exposição das origens de leilão disponíveis.
 * Ideal para alimentar filtros e dropdowns no frontend.
 */
@RestController
@RequestMapping("/api/v1/origens-leilao")
@CrossOrigin(origins = "*")
public class OrigemLeilaoController {

    @GetMapping
    public ResponseEntity<List<OrigemLeilao>> listarOrigens() {
        return ResponseEntity.ok(Arrays.asList(OrigemLeilao.values()));
    }
}
