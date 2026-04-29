package com.leilao.score;

import java.util.Map;

/**
 * Resultado de uma dimensão individual do score.
 * Equivalente ao @dataclass ResultadoDimensao do Python.
 * Usa record Java 17+ — imutável por padrão.
 */
public record ResultadoDimensao(
    String nome,
    double valor,          // 0.0–100.0
    Map<String, Object> detalhes  // explicação de como o valor foi calculado
) {}
