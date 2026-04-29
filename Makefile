# Makefile — atalhos para o LeilãoSaaS
# Uso: make <comando>

.PHONY: help up down logs build restart coleta status limpar

# Exibe ajuda
help:
	@echo ""
	@echo "  LeilãoSaaS — Comandos disponíveis"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  make up          Sobe banco + app em background"
	@echo "  make tools       Sobe banco + app + pgAdmin"
	@echo "  make down        Para todos os containers"
	@echo "  make logs        Acompanha logs da app em tempo real"
	@echo "  make build       Reconstrói a imagem da app"
	@echo "  make restart     Reinicia só a app (sem recompilar)"
	@echo "  make coleta      Executa coleta de leilões agora"
	@echo "  make status      Mostra status dos containers"
	@echo "  make limpar      Remove containers, volumes e imagens"
	@echo "  make prod        Sobe em modo produção"
	@echo "  make testes      Roda os testes unitários"
	@echo ""

# Sobe o ambiente de desenvolvimento
up:
	@cp -n .env.example .env 2>/dev/null || true
	docker compose up -d
	@echo ""
	@echo "  App rodando em:     http://localhost:8080"
	@echo "  API ranking:        http://localhost:8080/api/v1/lotes/ranking"
	@echo "  Health:             http://localhost:8080/actuator/health"
	@echo ""

# Sobe incluindo o pgAdmin
tools:
	@cp -n .env.example .env 2>/dev/null || true
	docker compose --profile tools up -d
	@echo ""
	@echo "  pgAdmin:            http://localhost:5050"
	@echo "  Login:              admin@leilao.com / admin123"
	@echo ""

# Para tudo
down:
	docker compose --profile tools down

# Logs da app em tempo real
logs:
	docker compose logs -f app

# Reconstrói a imagem (após mudança no código)
build:
	docker compose build --no-cache app
	docker compose up -d app

# Reinicia só a app sem recompilar
restart:
	docker compose restart app

# Executa a coleta de leilões imediatamente
coleta:
	docker compose exec app java -jar app.jar --executar-agora
	@echo "Coleta finalizada."

# Status dos containers
status:
	docker compose ps
	@echo ""
	@echo "Health da API:"
	@curl -s http://localhost:8080/actuator/health 2>/dev/null | python3 -m json.tool || echo "  App ainda não está respondendo."

# Produção
prod:
	docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Remove tudo (CUIDADO: apaga os dados do banco)
limpar:
	@echo "ATENÇÃO: isso vai apagar os dados do banco."
	@read -p "Confirma? (s/N): " confirm && [ "$$confirm" = "s" ] || exit 1
	docker compose --profile tools down -v --rmi local
	@echo "Ambiente limpo."

# Roda testes unitários (sem Docker)
testes:
	mvn test -q
	@echo "Testes concluídos."
