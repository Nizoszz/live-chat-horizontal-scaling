# Escala horizontal com WebSockets

Chat em tempo real em Java 21 e Spring Boot, com três instâncias da API atrás do Nginx e Redis Pub/Sub para propagar mensagens entre elas.

## Arquitetura

- **Frontend:** HTML e JavaScript usando WebSocket nativo.
- **Nginx:** serve o frontend e distribui conexões `/chatHub` em round-robin.
- **Chat API:** Spring Boot 4.1.1, organizada em `domain`, `application` e `infra`.
- **Redis:** canal Pub/Sub `chat:messages`, compartilhado pelas instâncias.

Cada conexão permanece na instância escolhida. Ao receber uma mensagem, a API publica no Redis. Todas as instâncias, inclusive a publicadora, recebem o evento e o distribuem às sessões locais. Isso garante uma entrega por cliente sem sticky session.

Consulte os [diagramas C4](docs/README.md).

## Executar

```bash
docker compose up --build
```

Acesse [http://localhost:8080](http://localhost:8080) em várias abas. Para encerrar, execute `docker compose down`.

## Contrato WebSocket

Endpoint: `/chatHub`

```json
{"type":"SEND_MESSAGE","user":"Alice","message":"Olá"}
{"type":"SERVER_INFO","serverId":"Server-1","connectionId":"..."}
{"type":"RECEIVE_MESSAGE","user":"Alice","text":"Olá","serverId":"Server-1","timestamp":"2026-09-04T12:00:00Z"}
{"type":"ERROR","code":"INVALID_MESSAGE","message":"text must not be blank"}
```

## Camadas

```text
domain       Regras e modelo ChatMessage; Java puro
application  Casos de uso e portas de entrada/saída
infra        Spring Boot, WebSocket, Redis, JSON e configuração
```

As dependências apontam para dentro: `infra` implementa portas de `application`, e `application` utiliza `domain`. Domínio e aplicação não conhecem Spring, Redis ou WebSocket.

## Configuração

| Variável | Default | Uso |
|---|---|---|
| `SERVER_ID` | `Unknown` | Identifica a instância de origem |
| `REDIS_HOST` | `localhost` | Host Redis |
| `REDIS_PORT` | `6379` | Porta Redis |
| `REDIS_CHANNEL` | `chat:messages` | Canal Pub/Sub |

## Testes

```bash
cd backend
mvn verify
```

A suíte cobre domínio, casos de uso, contrato WebSocket e integração Redis. O teste Testcontainers é ignorado automaticamente quando Docker não está disponível.

## Teste de carga externo com k6

O k6 roda diretamente na máquina host como um cliente externo. Ele se conecta a `ws://localhost:8080/chatHub`, passa pelo Nginx e mede a distribuição das conexões entre as três instâncias. O k6 não faz parte do Docker Compose.

Instale o k6 no Windows:

```powershell
winget install k6 --source winget
```

Com a aplicação Docker em execução, faça primeiro o smoke test de 30 segundos:

```powershell
.\run-k6.ps1 -Smoke
```

Depois execute o perfil completo, que sobe progressivamente de zero a 1.000 usuários durante 15 minutos:

```powershell
.\run-k6.ps1
```

Parâmetros opcionais:

```powershell
.\run-k6.ps1 `
  -WsUrl "ws://localhost:8080/chatHub" `
  -MessageIntervalMs 10000 `
  -DeliveryTimeoutMs 5000 `
  -LatencyP95Ms 1000
```

Cada usuário envia uma mensagem a cada 10 segundos. Com 1.000 conexões, são aproximadamente 100 publicações por segundo e até 100 mil entregas por segundo por causa do broadcast. A máquina do gerador de carga pode se tornar o gargalo e deve ter CPU, memória e rede monitoradas.

Os resultados são gravados em:

- `reports/k6-websocket-report.html`: relatório visual gerado por k6-reporter.
- `reports/k6-summary.json`: métricas agregadas para automação e análise.

O teste falha quando existem erros de conexão ou payload, a entrega das mensagens próprias fica abaixo de 99%, a latência p95 excede o limite ou algum servidor recebe menos de 25% ou mais de 42% das conexões. A geração do HTML usa módulos remotos e requer acesso à internet no início da execução.
