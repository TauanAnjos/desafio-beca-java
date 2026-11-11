# API de Gestão Financeira

Projeto do **Desafio Beca Java (NTT DATA)**: duas APIs REST em Java que cadastram usuários e registram transações financeiras (depósito, retirada, transferência e compra), com regras de aprovação, câmbio, análise de despesas e relatório em CSV.

## Visão geral

| Serviço | Porta | Banco (porta no seu computador) | Função |
|---|---|---|---|
| **MS1 — usuários** (`ms1-usuarios`) | 8081 | `user-db` (5434) | Cadastro, importação por CSV e verificação de login |
| **MS2 — transações** (`ms2-transacoes`) | 8082 | `transaction-db` (5435) | Transações, saldo, análise de despesas e relatório |

O MS2 consulta duas APIs externas: a **API Mock** (saldo e limites do usuário, hospedada no mockapi.io) e a **BrasilAPI** (câmbio).

```
Cliente --(Basic Auth)--> MS2 ---(POST /auth/login + chave interna)--> MS1 --> user-db
                           |--> API Mock  (saldo e limites)
                           |--> BrasilAPI (cotação USD e EUR)
                           '--> transaction-db
```

## Tecnologias

Java 21, Spring Boot 4.1.2, Maven, PostgreSQL 16, Flyway (migrations), Spring Security (MS2), Springdoc/Swagger, JUnit 5, Mockito, JaCoCo e Docker Compose. Cada serviço segue a **arquitetura hexagonal**: `domain` (regras, sem framework), `application` (casos de uso e portas) e `adapter` (web, banco e clientes HTTP).

## Como subir o ambiente do zero

**Pré-requisitos:** Docker Desktop. Para rodar pela IDE, JDK 21. Para os dados de exemplo, PowerShell (Windows).

**1. API Mock (uma vez):** no mockapi.io, crie um projeto com o recurso `accounts` e os campos `userId` (número), `balance`, `dailyLimit` e `perTransactionLimit` (números). Guarde a URL base (termina em `/api/v1`).

**2. Variáveis de ambiente:** copie `.env.example` para `.env` e preencha.

| Variável | Para que serve |
|---|---|
| `USER_DB_NAME`, `USER_DB_USER`, `USER_DB_PASSWORD`, `USER_DB_PORT` | Banco do MS1 |
| `TRANSACTION_DB_NAME`, `TRANSACTION_DB_USER`, `TRANSACTION_DB_PASSWORD`, `TRANSACTION_DB_PORT` | Banco do MS2 |
| `MOCK_API_BASE_URL` | URL base da API Mock (sem `/accounts` no final) |
| `INTERNAL_API_KEY` | Chave que só o MS2 envia ao MS1 no login. **Obrigatória**, use um valor longo e aleatório |
| `USERS_SERVICE_URL` | Onde o MS2 encontra o MS1. Padrão `http://localhost:8081`; no Compose é `http://ms1-usuarios:8081` |
| `BRASILAPI_BASE_URL` | Opcional. Padrão `https://brasilapi.com.br/api` |

**3. Subir tudo com um comando:**

```
docker compose up -d --build
```

Sobe os dois bancos, o MS1 e o MS2. O Flyway cria as tabelas sozinho. Os dados dos bancos ficam em volumes do Docker: `docker compose down` mantém os dados e `docker compose down -v` apaga tudo.

**4. Dados de exemplo (opcional, recomendado):**

```
powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo.ps1
```

Cria três usuários (`ana@demo.com`, `bruno@demo.com` e `carla@demo.com`, senha `Senha@1234`), as contas deles na API Mock (se faltarem) e transações de exemplo em todas as categorias, com saídas aprovadas e rejeitadas. Pode ser rodado várias vezes: o que já existe é reaproveitado.

**Rodando pela IDE (alternativa):** suba só os bancos (`docker compose up -d user-db transaction-db`) e rode as duas aplicações. O IntelliJ não lê o `.env`: coloque as variáveis na *Run Configuration* de cada serviço.

## Documentação interativa (Swagger)

| Serviço | Endereço |
|---|---|
| MS1 | http://localhost:8081/swagger-ui/index.html |
| **MS2** | **http://localhost:8082/swagger-ui.html** |

No MS2, clique em **Authorize** e informe o e-mail e a senha de um usuário. Observações de uso:

- Abra o Swagger por **localhost** (e não pelo IP da máquina), senão o navegador pode abrir o próprio prompt de login.
- Para baixar o CSV, use o Swagger (resposta da rota `/reports/transactions`) ou uma janela anônima. Se digitar a senha no prompt do navegador, ele a guarda e o Authorize pode ficar confuso.

## Autenticação

- O MS2 usa **HTTP Basic** (e-mail e senha do usuário cadastrado no MS1). Sem credenciais ou com senha errada: **401**.
- O MS2 valida as credenciais chamando `POST /auth/login` do MS1, que confere a senha (BCrypt). E-mail inexistente e senha errada devolvem a mesma resposta, e o tempo de resposta é igualado para não revelar quais e-mails existem.
- O login do MS1 é **interno**: exige o cabeçalho `X-Internal-Key` com a `INTERNAL_API_KEY`. Sem a chave: **403**.
- O dono da transação é sempre o usuário autenticado (o corpo da requisição não traz usuário).
- Abertos sem login: `/actuator/health` e a documentação (Swagger) do MS2, e no MS1 o cadastro de usuário.
- MS1 fora do ar ou lento: **503** no MS2.

## Endpoints do MS1 (usuários)

| Método e rota | O que faz |
|---|---|
| `POST /users` | Cadastra um usuário (e-mail único; senha guardada com BCrypt e nunca devolvida) |
| `GET /users` e `GET /users/{id}` | Lista e consulta |
| `PUT /users/{id}` | Atualiza (a senha é opcional) |
| `DELETE /users/{id}` | Remove |
| `POST /users/import` | Importa usuários de um CSV (`name,email,password`) e devolve o resumo. Exemplo em `exemplos/usuarios.csv` |
| `POST /auth/login` | Verificação de credenciais (interna, usada pelo MS2) |

## Endpoints do MS2 (transações)

Todos exigem Basic Auth.

| Método e rota | O que faz |
|---|---|
| `POST /transactions` | Cria uma transação (aprovada ou rejeitada com o motivo) |
| `GET /transactions` | Lista as do usuário. Filtros: `from`, `to` (AAAA-MM-DD) e `type`. `userId` de outro usuário: **403** |
| `GET /transactions/{id}` | Consulta uma transação (de outro usuário: 404) |
| `GET /balance` | Saldo atual da API Mock e saldo disponível do dia |
| `GET /expenses/summary` | Despesas: total geral e por categoria, por dia e por mês. Sem período, o mês atual |
| `GET /reports/transactions` | Relatório em CSV. Sem período, o mês atual |

Erros seguem o formato `ProblemDetail` (JSON com `title`, `status` e `detail`); o 400 de validação traz a lista de campos em `fields`.

## Regras das transações

- **Tipos:** `DEPOSIT`, `WITHDRAWAL`, `TRANSFER` e `PURCHASE`. São despesas (saídas) a retirada, a transferência enviada e a compra.
- **Categorias:** `FOOD`, `TRANSPORT`, `HOUSING`, `HEALTH`, `EDUCATION`, `LEISURE`, `SHOPPING` e `OTHER`. Obrigatória na compra; nos outros tipos vale `OTHER`.
- **Validações:** valor maior que zero com no máximo 2 casas decimais; descrição obrigatória (até 255 caracteres); na transferência, o destinatário é obrigatório e diferente do dono.
- **Aprovação das saídas** (nesta ordem): saldo disponível, limite por transação e limite diário. A saída que não passa é gravada como `REJECTED` com o motivo (`insufficient balance`, `amount exceeds the per-transaction limit`, `daily limit exceeded` ou `account not found for the user`). Depósitos não consultam a API Mock.
- **Saldo disponível** = saldo da API Mock + depósitos aprovados do dia − despesas aprovadas do dia. O limite diário conta só despesas.
- **"Dia" é o dia de São Paulo** (`America/Sao_Paulo`), independente do fuso do servidor.
- **API Mock fora do ar ou lenta** (timeout de 3 s): a saída não é aprovada às cegas; responde **503** e nada é gravado.

## Câmbio

- Fonte: BrasilAPI, `GET /cambio/v1/cotacao/{moeda}/{data}` (boletins PTAX do Banco Central). Usa a cotação de **venda** do boletim de **fechamento** de ontem (a fonte não expõe o dia corrente; em fim de semana e feriado, a própria BrasilAPI devolve o último dia útil).
- A cotação de **USD** e **EUR** é **guardada na transação no momento da criação** (tabela `transaction_exchange_rates`). Ler a transação nunca chama a BrasilAPI.
- A resposta traz `conversionAvailable` e a lista `conversions` (moeda, taxa, data da taxa e valor convertido). `conversionAvailable` só é `true` quando as **duas moedas** foram obtidas.
- BrasilAPI fora do ar ou lenta (timeout de 2 s): a transação é criada normalmente, sem taxas, com `conversionAvailable: false`.

## Relatório em CSV

`GET /reports/transactions` devolve um arquivo para o Excel em português: UTF-8 com BOM (acentos corretos), separador ponto e vírgula, vírgula decimal e datas `dd/MM/yyyy HH:mm:ss`. Colunas: data, tipo, categoria, descrição, valor em R$, status, motivo da rejeição, valor em USD e valor em EUR. Descrições que começam com `=`, `+`, `-` ou `@` são neutralizadas (proteção contra injeção de fórmula).

## Testes e cobertura

Em cada serviço (`ms1-usuarios` e `ms2-transacoes`):

```
.\mvnw test
```

O build gera o relatório de cobertura (JaCoCo) em `target/site/jacoco/index.html`. Cobertura medida em 10/11/2026:

| Serviço | Instruções | Ramos |
|---|---|---|
| MS2 | 86% | 87% |
| MS1 | 51% | 64% |

No MS1 os casos de uso estão em 97%; o que reduz o total são os controllers e a camada de persistência. No MS2, a camada de persistência (JPA) não tem testes automatizados, porque exigiria um banco real (Testcontainers).

## Decisões e limitações conhecidas

- **Saldo disponível considera apenas o dia atual.** Não há extrato acumulado de dias anteriores: o saldo base é sempre o da API Mock.
- **A transferência recebida ainda não é creditada** ao destinatário, e o destinatário não é conferido no MS1.
- **Não há rota para apagar nem alterar transações.** A correção por estorno (nova transação ligada à original) foi decidida, mas não implementada neste prazo.
- **O login não limita tentativas** (força bruta não coberta). Ele é protegido pela chave interna, mas dentro do sistema não há bloqueio por excesso de tentativas.
- A cada requisição o MS2 consulta o MS1 para autenticar (sem cache).
- Com a BrasilAPI fora do ar, cada criação espera até o timeout (2 s por moeda) antes de seguir sem as taxas.
- O script de dados de exemplo é em PowerShell (Windows).

## Estrutura do repositório

```
ms1-usuarios/      serviço de usuários (porta 8081)
ms2-transacoes/    serviço de transações (porta 8082)
scripts/           seed-demo.ps1 (dados de exemplo)
exemplos/          usuarios.csv (importação)
docker-compose.yml bancos, MS1 e MS2
.env.example       modelo das variáveis de ambiente
```