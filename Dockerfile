# ─────────────────────────────────────────────────────────────
# STAGE 1: Build
# Compila o projeto com Maven e gera o JAR
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copia o pom.xml primeiro (cache de dependências do Maven)
COPY pom.xml .
RUN mvn -f pom.xml dependency:go-offline -q 2>/dev/null || true

# Copia o código-fonte e compila
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q && \
    echo "Build concluído."

# ─────────────────────────────────────────────────────────────
# STAGE 2: Runtime
# Imagem final leve — só JRE + o JAR compilado
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Instala o Chromium para o Selenium (scrapers com JS)
RUN apk add --no-cache \
    chromium \
    chromium-chromedriver \
    nss \
    freetype \
    harfbuzz \
    ca-certificates \
    ttf-freefont \
    && echo "Chrome instalado."

# Variável para o WebDriverManager encontrar o chromedriver do Alpine
ENV CHROME_BIN=/usr/bin/chromium-browser
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

# Usuário não-root por segurança
RUN addgroup -S leilao && adduser -S leilao -G leilao
USER leilao

WORKDIR /app

# Copia o JAR do stage de build
COPY --from=build /app/target/leilao-saas-*.jar app.jar

# Porta da API REST
EXPOSE 8080

# Ponto de entrada
ENTRYPOINT ["java", "-jar", "app.jar"]

# Argumento padrão — pode ser sobrescrito via docker-compose
# CMD ["--executar-agora"]  ← descomente para rodar imediatamente
