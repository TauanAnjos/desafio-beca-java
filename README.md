# Desafio Beca Java: API de Gestão Financeira

Projeto do desafio da Beca Bradesco Java (NTT DATA). A API de gestão financeira é formada por dois microsserviços que, ao final, sobem juntos com Docker Compose.

| Serviço | O que faz | Situação |
|---|---|---|
| **MS1 User Service** (`ms1-usuarios/`) | Cadastro e manutenção de usuários, importação em massa por CSV | CRUD, CSV, senha com BCrypt, Flyway, testes e Swagger prontos. Falta o login (Spring Security) |
| **MS2 Transaction Service** (`ms2-transacoes/`) | Transações, saldo e limites, câmbio, análise de despesas, relatório e segurança | Em desenvolvimento: domínio, banco, integração com a API Mock e regras de aprovação prontos. Endpoints a caminho |

O acompanhamento das tarefas está no quadro Kanban do Jira (espaço **Desafio Beca Java**, chave `DBJ`). Os commits trazem a chave do cartão na mensagem.

## Tecnologias

- Java 21 e Spring Boot 4 (Web, Data JPA, Validation, Actuator)
- PostgreSQL 16 (um banco por serviço) e Docker Compose
- Flyway (migrations do banco)
- BCrypt (`spring-security-crypto`) para as senhas
- Apache Commons CSV para a importação
- `RestClient` (Spring) para as APIs externas
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito e AssertJ nos testes

## Arquitetura (hexagonal)

Nos dois serviços o domínio não conhece Spring, JPA nem HTTP. A dependência sempre aponta para dentro: `adapter` → `application` → `domain`.

### MS1

```
ms1-usuarios/src/main/java/br/com/tauan/ms1_usuarios/
├── domain/
│   ├── model/                  User (Java puro, com changeData e changePasswordHash)
│   └── exception/              UserNotFoundException, EmailAlreadyRegisteredException, InvalidCsvFileException
├── application/
│   ├── port/in/                Casos de uso (uma porta por operação) e comandos
│   ├── port/out/               UserRepositoryPort, PasswordEncoderPort
│   └── service/                UserService, ImportUsersService
└── adapter/
    ├── in/web/                 Controllers REST, DTOs, CsvUserParser, tratamento de erros, OpenAPI
    └── out/
        ├── persistence/        Entidade JPA, repositório Spring Data e UserPersistenceAdapter
        └── security/           BcryptPasswordEncoderAdapter
```

### MS2

```
ms2-transacoes/src/main/java/br/com/tauan/ms2_transacoes/
├── domain/
│   ├── model/                  Transaction (imutável), TransactionPolicy, TransactionDecision,
│   │                           AccountInfo, enums (TransactionType, TransactionStatus, Category)
│   └── exception/              InvalidTransactionException, AccountServiceUnavailableException
├── application/
│   ├── port/in/                Casos de uso (em construção)
│   ├── port/out/               TransactionRepositoryPort, AccountPort
│   └── service/                Implementação dos casos de uso (em construção)
└── adapter/
    ├── in/web/                 DTOs e tratamento de erros (400 e 503)
    └── out/
        ├── persistence/        Entidade JPA, repositório Spring Data e adapter
        └── client/             Cliente HTTP da API Mock (saldo e limites)
```

## Como rodar

### Pré-requisitos

- JDK 21
- Docker Desktop (com o Docker Compose)
- Git

### 1. Clonar e configurar as variáveis de ambiente

```bash
git clone git@github.com:TauanAnjos/desafio-beca-java.git
cd desafio-beca-java
cp .env.example .env
```

Edite o `.env` e troque os valores de exemplo por valores reais. **Nunca suba o `.env` para o GitHub** (ele está no `.gitignore`).

| Variável | Quem usa | Para que serve |
|---|---|---|
| `USER_DB_NAME`, `USER_DB_USER`, `USER_DB_PASSWORD` | MS1 e Compose | Banco de usuários |
| `USER_DB_PORT` | Compose | Porta do banco de usuários no seu computador (padrão `5434`) |
| `TRANSACTION_DB_NAME`, `TRANSACTION_DB_USER`, `TRANSACTION_DB_PASSWORD` | MS2 e Compose | Banco de transações |
| `TRANSACTION_DB_PORT` | MS2 e Compose | Porta do banco de transações no seu computador (padrão `5435`) |
| `MOCK_API_BASE_URL` | MS2 | URL base da API Mock de saldo e limites (veja a seção da API Mock) |

### 2. Subir os bancos

```bash
docker compose up -d
docker compose ps
```

Os dois containers (`user-db` e `transaction-db`) devem ficar `healthy`. O Compose lê o `.env` sozinho.

> O esquema de cada banco é criado pelo Flyway quando o serviço sobe (arquivos em `src/main/resources/db/migration` de cada serviço). Se você mudar usuário ou senha no `.env` depois que o banco já foi criado, o Postgres mantém os valores antigos: recrie os volumes (apaga os dados) com `docker compose down -v` e `docker compose up -d`.

### 3. Configurar a API Mock (MS2)

O saldo e os limites da conta não ficam no nosso banco: vêm de um serviço simulado no [mockapi.io](https://mockapi.io).

1. Crie uma conta gratuita e um projeto
2. Crie o recurso **`accounts`** com os campos:

| Campo | Tipo | Exemplo |
|---|---|---|
| `userId` | Number | `1` |
| `balance` | Number | `1000.00` |
| `dailyLimit` | Number | `5000.00` |
| `perTransactionLimit` | Number | `2000.00` |

3. Crie pelo menos um registro por usuário, por exemplo:

```json
{ "userId": 1, "balance": 1000.00, "dailyLimit": 5000.00, "perTransactionLimit": 2000.00 }
```

4. Coloque no `.env` a URL base do projeto, **sem** `/accounts` no final:

```
MOCK_API_BASE_URL=https://SEU_ID.mockapi.io/api/v1
```

O MS2 consulta `GET {MOCK_API_BASE_URL}/accounts?userId={id}`, com timeout de 3 segundos.

### 4. Rodar um serviço

Cada serviço lê suas variáveis do ambiente:

| Serviço | Variáveis | Porta |
|---|---|---|
| MS1 | `USER_DB_NAME`, `USER_DB_USER`, `USER_DB_PASSWORD` (a URL aponta para `localhost:5434`) | 8081 |
| MS2 | `TRANSACTION_DB_NAME`, `TRANSACTION_DB_USER`, `TRANSACTION_DB_PASSWORD`, `TRANSACTION_DB_PORT` (opcional, padrão 5435), `MOCK_API_BASE_URL` | 8082 |

#### Pelo IntelliJ IDEA

O IntelliJ não lê o `.env` sozinho. Configure uma vez para cada serviço:

1. **Run → Edit Configurations → +  → Application**
2. **Name:** `Ms1UsuariosApplication` (ou `Ms2TransacoesApplication`)
3. **Main class:** `br.com.tauan.ms1_usuarios.Ms1UsuariosApplication` (ou `br.com.tauan.ms2_transacoes.Ms2TransacoesApplication`)
4. **Module (`-cp`):** `ms1-usuarios` (ou `ms2-transacoes`)
5. **Modify options → Environment variables** e preencha com os valores do seu `.env`, separados por `;`:
   ```
   USER_DB_NAME=...;USER_DB_USER=...;USER_DB_PASSWORD=...
   ```
   ou, no MS2:
   ```
   TRANSACTION_DB_NAME=...;TRANSACTION_DB_USER=...;TRANSACTION_DB_PASSWORD=...;MOCK_API_BASE_URL=https://SEU_ID.mockapi.io/api/v1
   ```
6. Rode com o botão ▶

Os valores ficam na pasta `.idea`, que está no `.gitignore`.

#### Pelo terminal (carregando o `.env`)

PowerShell, a partir de `ms1-usuarios/` ou `ms2-transacoes/`:

```powershell
Get-Content ..\.env | Where-Object { $_ -match '^\s*[^#\s].*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
}
.\mvnw spring-boot:run
```

Bash (Git Bash, Linux, macOS), a partir de `ms1-usuarios/` ou `ms2-transacoes/`:

```bash
set -a; source ../.env; set +a
./mvnw spring-boot:run
```

## Documentação da API (Swagger)

Com o MS1 no ar: `http://localhost:8081/swagger-ui/index.html`

A especificação OpenAPI em JSON fica em `http://localhost:8081/v3/api-docs`. O Swagger do MS2 entra junto com os endpoints dele.

## Endpoints do MS1

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| POST | `/users` | Cria usuário | 201, 400, 409 |
| GET | `/users` | Lista usuários | 200 |
| GET | `/users/{id}` | Busca por id | 200, 404 |
| PUT | `/users/{id}` | Atualiza usuário | 200, 400, 404, 409 |
| DELETE | `/users/{id}` | Exclui usuário | 204, 404 |
| POST | `/users/import` | Importa usuários por CSV (`multipart/form-data`, campo `file`) | 200, 400 |

Regras:
- O e-mail é normalizado (sem espaços, em minúsculas) no cadastro, na atualização e na importação, e é único.
- A senha tem de 8 a 72 caracteres, é guardada com BCrypt e **nunca** aparece nas respostas.
- No `PUT` a senha é opcional: se vier, é recriptografada; se não vier, a atual é mantida.
- Erros usam `ProblemDetail`. Validação responde 400 com a lista de campos em `fields`. E-mail duplicado, inclusive em requisições simultâneas, responde 409.

## Importação em massa (CSV)

Arquivo de exemplo: [`exemplos/usuarios.csv`](exemplos/usuarios.csv)

```csv
name,email,password
Ana Souza,ana.souza@exemplo.com,Senha@1234
"Souza, Carla Dias",carla.dias@exemplo.com,Senha@1234
```

- Cabeçalho obrigatório: `name,email,password`
- Separador: vírgula ou ponto e vírgula (detectado pelo cabeçalho). O BOM do Excel é tratado e nomes entre aspas podem ter vírgula
- Cada linha é validada. Linhas inválidas **não derrubam** o arquivo: a resposta traz `imported`, `skipped` e a lista `errors` com a linha e o motivo
- E-mail repetido dentro do arquivo ou já cadastrado é falha só daquela linha

Exemplo de resposta:

```json
{
  "imported": 4,
  "skipped": 1,
  "errors": [{ "line": 3, "reason": "invalid email" }]
}
```

## Regras de negócio do MS2

Toda transação termina em **APPROVED** ou **REJECTED** (com o motivo) e nunca é apagada.

| Tipo | Regra |
|---|---|
| Depósito | Sempre aprovado. Não consulta a API Mock |
| Retirada, transferência e compra | Consultam saldo e limites na API Mock. Sem conta: `account not found for the user` |
| | 1º saldo suficiente, senão `insufficient balance` |
| | 2º valor dentro do limite por transação, senão `amount exceeds the per-transaction limit` |
| | 3º gasto do dia mais o valor dentro do limite diário, senão `daily limit exceeded` |

Consistência da transação, validada no domínio:
- Valor maior que zero, com no máximo 2 casas decimais
- Descrição obrigatória, até 255 caracteres
- Transferência exige usuário de destino diferente do dono. Nos outros tipos, o destino é proibido
- Compra exige categoria. Nos outros tipos, sem categoria vira `OTHER`
- Categorias (lista fixa): `FOOD`, `TRANSPORT`, `HOUSING`, `HEALTH`, `EDUCATION`, `LEISURE`, `SHOPPING`, `OTHER`
- Transação rejeitada sempre guarda o motivo; aprovada não tem motivo

## Decisões de projeto

- **Câmbio:** a taxa será guardada na transação, no momento da criação (snapshot), e não consultada a cada leitura. Assim o valor convertido não muda depois e a consulta nunca depende da BrasilAPI. Se a BrasilAPI estiver fora do ar na criação, a transação é criada sem taxas e a resposta sinaliza que a conversão não está disponível
- **Migrations:** o esquema de cada banco é versionado com Flyway e o Hibernate só valida (`ddl-auto=validate`)
- **Pasta das migrations:** `src/main/resources/db/migration` (uma pasta `db` com `migration` dentro, não um pacote com ponto)
- **Correção de transação sem apagar:** em definição (estorno como nova transação)

## Testes

Em cada serviço (`ms1-usuarios/` ou `ms2-transacoes/`):

```bash
./mvnw test        # Windows PowerShell: .\mvnw test
```

- MS1: testes unitários do `UserService` e do `ImportUsersService`
- MS2: testes do domínio (`Transaction`, `TransactionPolicy`), do tratamento de erros e do adapter da API Mock (servidor HTTP simulado)

Todos rodam sem banco e sem internet. O relatório de cobertura será configurado em uma tarefa própria do quadro (JaCoCo).

## Segurança

- Nenhum segredo no repositório: senhas, usuários do banco e a URL da API Mock vêm de variáveis de ambiente, com o `.env.example` versionado só com valores fictícios
- O `.env` e a pasta `.idea` estão no `.gitignore`
- Autenticação com usuário e senha (Spring Security) está planejada nas tarefas de login do MS1 e das rotas do MS2

## Fluxo de trabalho

- Uma branch por atividade, com o código do Jira no nome (`feature/DBJ-39-...`) e nos commits (`feat: DBJ-39 ...`)
- Pull Request aberto e revisado antes de entrar na `main`