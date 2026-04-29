# LeilãoSaaS — Java + Spring Boot

Versão Java do sistema de filtragem inteligente de leilões.

## Estrutura do projeto

```
src/main/java/com/leilao/
├── LeilaoSaasApplication.java     ← ponto de entrada (@SpringBootApplication)
├── config/
│   ├── ScoreConfig.java           ← pesos e parâmetros do motor de score
│   └── ScraperConfig.java         ← delay, timeout, UFs da Caixa
├── model/
│   ├── Lote.java                  ← entidade JPA central (@Entity)
│   ├── TipoLote.java              ← enum: IMOVEL_RESIDENCIAL, VEICULO...
│   ├── OrigemLeilao.java          ← enum: JUDICIAL, EXTRAJUDICIAL
│   └── StatusLote.java            ← enum: DISPONIVEL, ENCERRADO...
├── repository/
│   └── LoteRepository.java        ← Spring Data JPA + queries JPQL
├── scraper/
│   ├── ScraperBase.java           ← classe abstrata com Selenium e helpers
│   ├── CaixaScraper.java          ← CSV público por UF (OkHttp, sem browser)
│   ├── SuperbidScraper.java       ← Selenium + Jsoup
│   ├── SoldScraper.java           ← API JSON (OkHttp + Jackson)
│   └── ParqueLeiloesScraper.java  ← Selenium + Jsoup (leilões judiciais)
├── score/
│   ├── DimensoesScore.java        ← 4 dimensões: Desconto, Financ., Local., Risco
│   ├── MotorScore.java            ← @Service orquestrador do score
│   ├── ResultadoDimensao.java     ← record Java com resultado por dimensão
│   └── ResultadoScore.java        ← record Java com score final + classificação
└── scheduler/
    ├── ScraperService.java        ← orquestra scrapers + upsert + score
    └── ScraperScheduler.java      ← @Scheduled (coleta diária) + --executar-agora

src/main/resources/
├── application.properties         ← toda configuração (banco, score, scrapers)
└── db/migration/
    ├── V1__cria_tabela_lotes.sql  ← Flyway: cria tabela + ENUMs + índices
    └── V2__score_e_auditoria.sql  ← Flyway: índice score_calculado_em

src/test/java/com/leilao/
├── model/LoteTest.java            ← testes da entidade e desconto calculado
├── score/MotorScoreTest.java      ← 20+ testes das 4 dimensões e motor completo
└── scraper/ScraperBaseTest.java   ← testes dos helpers de parsing
```

## Pré-requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ rodando localmente ou via Docker
- Google Chrome instalado (para os scrapers com Selenium)

## Setup

### 1. Banco com Docker (opcional)

```bash
docker run -d \
  --name leiloes-db \
  -e POSTGRES_DB=leiloes \
  -e POSTGRES_USER=usuario \
  -e POSTGRES_PASSWORD=senha \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Configurar o banco em application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/leiloes
spring.datasource.username=usuario
spring.datasource.password=senha
```

### 3. Compilar e rodar

```bash
# Compilar
mvn clean package -DskipTests

# Rodar (aguarda o cron das 06:00)
java -jar target/leilao-saas-1.0.0.jar

# Executar coleta imediatamente
java -jar target/leilao-saas-1.0.0.jar --executar-agora
```

### 4. Testes

```bash
mvn test
```

## Flyway — Migrations

As migrations rodam automaticamente ao iniciar a aplicação.
Para verificar o status:

```bash
mvn flyway:info
mvn flyway:migrate   # forçar manualmente
mvn flyway:repair    # reparar checksum em caso de edição acidental
```


## Docker

### Subir o ambiente completo

```bash
# 1. Copie o arquivo de variáveis
cp .env.example .env

# 2. Sobe banco + app (primeira vez faz o build automaticamente)
make up
# ou sem Make:
docker compose up -d

# App disponível em: http://localhost:8080
# API ranking:       http://localhost:8080/api/v1/lotes/ranking
```

### Comandos do dia a dia

```bash
make logs      # acompanha logs em tempo real
make coleta    # executa scraping agora
make restart   # reinicia a app após mudança
make build     # reconstrói a imagem após mudança de código
make status    # estado dos containers + health da API
make tools     # sobe também o pgAdmin (http://localhost:5050)
make down      # para tudo
```

### Perfis

| Perfil | Comando | Quando usar |
|--------|---------|-------------|
| Desenvolvimento | `make up` | Dia a dia local |
| Com pgAdmin | `make tools` | Inspecionar o banco visualmente |
| Produção | `make prod` | VPS/servidor — banco sem porta exposta |

### Estrutura dos arquivos Docker

```
Dockerfile                 ← build multi-stage (JDK → JRE + Chrome)
docker-compose.yml         ← serviços: db + app + pgAdmin
docker-compose.override.yml ← sobrescritas de desenvolvimento
docker-compose.prod.yml    ← sobrescritas de produção
.env.example               ← variáveis de ambiente (copiar para .env)
.dockerignore              ← arquivos excluídos do build
docker/
  init-db.sql              ← script de inicialização do banco
  pgadmin-servers.json     ← configuração automática do pgAdmin
```

## Equivalência Python → Java

| Python                        | Java                          |
|-------------------------------|-------------------------------|
| `SQLAlchemy Base + Column`    | `@Entity + @Column` (JPA)     |
| `Alembic migrations`          | `Flyway V*.sql`               |
| `Playwright async`            | `Selenium WebDriver`          |
| `httpx`                       | `OkHttp`                      |
| `APScheduler cron`            | `@Scheduled(cron="...")`      |
| `python-dotenv / os.getenv`   | `application.properties`      |
| `@dataclass`                  | `record` (Java 17+)           |
| `pytest`                      | `JUnit 5 + Mockito`           |

## Adicionando um novo portal

```java
// 1. Crie a classe herdando ScraperBase
@Component
public class MeuPortalScraper extends ScraperBase {

    public MeuPortalScraper(ScraperConfig config) { super(config); }

    @Override public String getNome() { return "meu_portal"; }

    @Override
    public List<Lote> coletar() {
        WebDriver driver = criarDriver();
        try {
            // ... lógica de scraping ...
            return lotes;
        } finally {
            driver.quit(); // sempre fechar!
        }
    }
}
// 2. Pronto! O Spring injeta automaticamente no ScraperService.
```

## Próximo passo — Fase 3

- API REST com Spring Web (endpoints de listagem e busca por score)
- Interface web (Next.js ou Thymeleaf)
- Sistema de alertas (e-mail / WhatsApp)
