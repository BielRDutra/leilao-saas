package com.leilao.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configurações do motor de score.
 * Lidas do application.properties via prefixo "leilao.score".
 * Equivalente ao score/config.py do Python.
 */
@Configuration
@ConfigurationProperties(prefix = "leilao.score")
@Getter @Setter
public class ScoreConfig {

    // ── Pesos das dimensões (devem somar 1.0) ─────────────────────────────────
    private double pesoDesconto      = 0.40;
    private double pesoFinanciamento = 0.25;
    private double pesoLocalizacao   = 0.20;
    private double pesoRisco         = 0.15;

    // ── Dimensão: Desconto ────────────────────────────────────────────────────
    private double descontoMinPct = 10.0;
    private double descontoMaxPct = 50.0;

    // ── Dimensão: Financiamento ───────────────────────────────────────────────
    private double bonusFinanciamento = 50.0;
    private double bonusFgts          = 30.0;
    private double bonusCaixa         = 20.0;

    // ── Dimensão: Localização ─────────────────────────────────────────────────
    private List<String> estadosPremium = List.of("SP","RJ","MG","PR","SC","RS");
    private double bonusEstadoPremium   = 20.0;
    private List<String> cidadesPremium = List.of(
        "São Paulo","Rio de Janeiro","Curitiba","Porto Alegre",
        "Belo Horizonte","Florianópolis","Campinas","Brasília"
    );
    private double bonusCidadePremium   = 20.0;

    // ── Dimensão: Risco ───────────────────────────────────────────────────────
    private double penaltOcupado       = 40.0;
    private double penaltSemAvaliacao  = 20.0;
    private double penaltSegundaPraca  = 10.0;
    private double bonusJudicial       = 10.0;

    // ── Geral ─────────────────────────────────────────────────────────────────
    private double minimoListagem = 30.0;
}
