# Desafio Beca Java: API de Gestão Financeira

Projeto do desafio da Beca Bradesco Java (NTT DATA). A API de gestão financeira é formada por dois microsserviços que sobem juntos com Docker Compose.

| Serviço | O que faz | Situação |
|---|---|---|
| **MS1 User Service** (`ms1-usuarios/`) | Cadastro e manutenção de usuários, importação em massa por CSV | Em desenvolvimento (CRUD, CSV, senha com BCrypt, testes e Swagger prontos) |
| **MS2 Transaction Service** (`ms2-transacoes/`) | Transações, saldo e limites, câmbio, análise de despesas, relatório e segurança | A fazer |

O acompanhamento das tarefas está no quadro Kanban do Jira (espaço **Desafio Beca Java**, chave `DBJ`). Os commits trazem a chave do cartão na mensagem.

## Tecnologias

- Java 21 e Spring Boot 4 (Web, Data JPA, Validation, Actuator)
- PostgreSQL 16 (um banco por serviço) e Docker Compose
- BCrypt (`spring-security-crypto`) para as senhas
- Apache Commons CSV para a importação
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito e AssertJ nos testes

## Arquitetura do MS1 (hexagonal)

O domínio não conhece Spring, JPA nem HTTP. A dependência sempre aponta para dentro: `adapter` → `application` → `domain`.

```
ms1-usuarios/src/main/java/br/com/tauan/ms1_usuarios/
├── domain/
│   ├── model/                  User (Java puro, com changeData)
│   └── exception/              UserNotFoundException, EmailAlreadyRegisteredException, InvalidCsvFileException
├── application/
│   ├── port/in/                Casos de uso (uma porta por operação) e SaveUserCommand
│   ├── port/out/               UserRepositoryPort, PasswordEncoderPort
│   └── service/                UserService, ImportUsersService
└── adapter/
    ├── in/web/                 Controllers REST, DTOs, CsvUserParser, tratamento de erros, OpenAPI
    └── out/
        ├── persistence/        Entidade JPA, repositório Spring Data e UserPersistenceAdapter
        └── security/           BcryptPasswordEncoderAdapter
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

| Variável | Para que serve |
|---|---|
| `USER_DB_NAME` | Nome do banco do MS1 |
| `USER_DB_USER` | Usuário do banco do MS1 |
| `USER_DB_PASSWORD` | Senha do banco do MS1 |
| `USER_DB_PORT` | Porta do banco do MS1 no seu computador (padrão `5434`) |
| `TRANSACTION_DB_NAME`, `TRANSACTION_DB_USER`, `TRANSACTION_DB_PASSWORD`, `TRANSACTION_DB_PORT` | Mesmas variáveis para o banco do MS2 (porta padrão `5435`) |

### 2. Subir os bancos

```bash
docker compose up -d
docker compose ps
```

Os dois containers (`user-db` e `transaction-db`) devem ficar `healthy`. O Compose lê o `.env` sozinho.

> Se você mudar o usuário ou a senha no `.env` depois que o banco já foi criado, o Postgres continua com os valores antigos. Recrie os volumes (apaga os dados): `docker compose down -v` e `docker compose up -d`.

### 3. Rodar o MS1

O MS1 lê `USER_DB_NAME`, `USER_DB_USER` e `USER_DB_PASSWORD` do ambiente. A URL do banco aponta para `localhost:5434`, a porta publicada pelo Compose.

#### Pelo IntelliJ IDEA

O IntelliJ não lê o `.env` sozinho. Configure uma vez:

1. **Run → Edit Configurations → +  → Application**
2. **Name:** `Ms1UsuariosApplication`
3. **Main class:** `br.com.tauan.ms1_usuarios.Ms1UsuariosApplication`
4. **Module (`-cp`):** `ms1-usuarios`
5. **Modify options → Environment variables** e preencha com os valores do seu `.env`:
   ```
   USER_DB_NAME=...;USER_DB_USER=...;USER_DB_PASSWORD=...
   ```
6. Rode com o botão ▶

Os valores ficam na pasta `.idea`, que está no `.gitignore`.

#### Pelo terminal (carregando o `.env`)

PowerShell, a partir de `ms1-usuarios/`:

```powershell
Get-Content ..\.env | Where-Object { $_ -match '^\s*[^#\s].*=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$($name.Trim())" -Value $value.Trim()
}
.\mvnw spring-boot:run
```

Bash (Git Bash, Linux, macOS), a partir de `ms1-usuarios/`:

```bash
set -a; source ../.env; set +a
./mvnw spring-boot:run
```

O MS1 sobe em `http://localhost:8081`.

## Documentação da API (Swagger)

Com o MS1 no ar: `http://localhost:8081/swagger-ui/index.html`

A especificação OpenAPI em JSON fica em `http://localhost:8081/v3/api-docs`.

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

## Testes

Na pasta `ms1-usuarios/`:

```bash
./mvnw test        # Windows PowerShell: .\mvnw test
```

Os testes unitários (JUnit 5 e Mockito) cobrem o `UserService` e o `ImportUsersService`, sem precisar de banco. O relatório de cobertura será configurado em uma tarefa própria do quadro (JaCoCo).

## Segurança

- Nenhum segredo no repositório: senhas e usuários do banco vêm de variáveis de ambiente, com o `.env.example` versionado só com valores fictícios
- O `.env` e a pasta `.idea` estão no `.gitignore`
- Autenticação com usuário e senha (Spring Security) está planejada nas tarefas de login do MS1 e das rotas do MS2

## Fluxo de trabalho

- Uma branch por atividade, com o código do Jira no nome (`feature/DBJ-39-...`) e nos commits (`feat: DBJ-39 ...`)
- Pull Request aberto e revisado antes de entrar na `main`