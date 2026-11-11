<#
Popula o ambiente com dados de exemplo para a demonstracao.
Uso (na raiz do repositorio, com o MS1 e o MS2 no ar):
    powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo.ps1
O script pode ser rodado de novo: usuarios, contas e transacoes que ja existem sao reaproveitados
(a transacao e reconhecida pela descricao). Para lancar tudo de novo, acrescente -Force.
#>
param(
    [string]$Ms1Url = "http://localhost:8081",
    [string]$Ms2Url = "http://localhost:8082",
    [string]$MockApiBaseUrl = $env:MOCK_API_BASE_URL,
    [string]$Password = "Senha@1234",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

# URL da API Mock: parametro, variavel de ambiente ou arquivo .env
if (-not $MockApiBaseUrl) {
    $envFile = Join-Path $PSScriptRoot "..\.env"
    if (Test-Path $envFile) {
        $line = Get-Content $envFile | Where-Object { $_ -match '^\s*MOCK_API_BASE_URL\s*=' } | Select-Object -First 1
        if ($line) { $MockApiBaseUrl = ($line -split '=', 2)[1].Trim() }
    }
}
if (-not $MockApiBaseUrl) {
    throw "Defina MOCK_API_BASE_URL (variavel de ambiente ou .env) ou passe -MockApiBaseUrl."
}
$MockApiBaseUrl = $MockApiBaseUrl.TrimEnd('/')

function Get-StatusCode($errorRecord) {
    if ($errorRecord.Exception.Response) { return [int]$errorRecord.Exception.Response.StatusCode }
    return 0
}

function Read-ErrorBody($errorRecord) {
    if ($errorRecord.ErrorDetails -and $errorRecord.ErrorDetails.Message) { return $errorRecord.ErrorDetails.Message }
    try {
        $stream = $errorRecord.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    } catch {
        return ""
    }
}

# Chamada HTTP com JSON. Em caso de erro, mostra o status e o corpo da resposta.
function Invoke-Json($method, $uri, $headers, $body) {
    $params = @{ Method = $method; Uri = $uri; UseBasicParsing = $true }
    if ($headers) { $params.Headers = $headers }
    if ($null -ne $body) {
        $params.ContentType = "application/json"
        $params.Body = ($body | ConvertTo-Json)
    }
    try {
        $response = Invoke-WebRequest @params
    } catch {
        $status = Get-StatusCode $_
        throw ("HTTP {0} em {1} {2}: {3}" -f $status, $method, $uri, (Read-ErrorBody $_))
    }
    if (-not $response.Content) { return $null }
    return ($response.Content | ConvertFrom-Json)
}

# Devolve os itens de uma resposta que pode ser lista ou pagina. Use sempre dentro de @( ... ).
function Get-JsonList($uri, $headers) {
    $parsed = Invoke-Json "Get" $uri $headers $null
    if ($null -eq $parsed) { return }
    if ($parsed.PSObject.Properties.Name -contains "content") { $parsed = $parsed.content }
    foreach ($item in @($parsed)) {
        if ($null -ne $item) { $item }
    }
}

function Get-OrCreate-User($name, $email) {
    $existing = @(Get-JsonList "$Ms1Url/users" $null) |
        Where-Object { $_.email -and ($_.email.ToLower() -eq $email.ToLower()) } |
        Select-Object -First 1
    if ($existing) { $user = $existing }
    else { $user = Invoke-Json "Post" "$Ms1Url/users" $null @{ name = $name; email = $email; password = $Password } }
    if (-not $user -or -not $user.id) {
        throw "Nao foi possivel obter o id do usuario $email."
    }
    return $user
}

function Ensure-Account($userId, $balance, $dailyLimit, $perTransactionLimit) {
    if (-not $userId) { throw "userId vazio: a conta na API Mock nao sera criada sem o id do usuario." }
    $existing = @()
    try {
        $existing = @(@(Get-JsonList "$MockApiBaseUrl/accounts?userId=$userId" $null) |
            Where-Object { "$($_.userId)" -eq "$userId" })
    } catch {
        if ($_.Exception.Message -notmatch "HTTP 404") { throw }
    }
    if ($existing.Count -gt 0) {
        $account = $existing[0]
        Write-Host "  conta ja existe (userId $userId): saldo $($account.balance), limite diario $($account.dailyLimit), limite por transacao $($account.perTransactionLimit)"
        return
    }
    Invoke-Json "Post" "$MockApiBaseUrl/accounts" $null @{
        userId = $userId; balance = $balance; dailyLimit = $dailyLimit; perTransactionLimit = $perTransactionLimit
    } | Out-Null
    Write-Host "  conta criada na API Mock (userId $userId, saldo $balance)"
}

function Get-AuthHeader($email) {
    $token = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${email}:$Password"))
    return @{ Authorization = "Basic $token" }
}

# Lanca so o que falta: a transacao e reconhecida pela descricao.
function Add-Transactions($email, $transactions) {
    $headers = Get-AuthHeader $email
    $existing = @(Get-JsonList "$Ms2Url/transactions" $headers) | ForEach-Object { $_.description }
    foreach ($transaction in $transactions) {
        if (-not $Force -and ($existing -contains $transaction.description)) {
            Write-Host ("  ja existe: {0}" -f $transaction.description)
            continue
        }
        $result = Invoke-Json "Post" "$Ms2Url/transactions" $headers $transaction
        $reason = ""
        if ($result.rejectionReason) { $reason = " ($($result.rejectionReason))" }
        Write-Host ("  {0,-10} {1,10} {2,-12} {3}{4}" -f $result.type, $result.amount, $result.status, $result.description, $reason)
    }
}

Write-Host "== Usuarios (MS1) =="
$ana = Get-OrCreate-User "Ana Souza" "ana@demo.com"
$bruno = Get-OrCreate-User "Bruno Lima" "bruno@demo.com"
$carla = Get-OrCreate-User "Carla Dias" "carla@demo.com"
Write-Host "  ana@demo.com (id $($ana.id)), bruno@demo.com (id $($bruno.id)), carla@demo.com (id $($carla.id))"

Write-Host "== Contas (API Mock) =="
Ensure-Account $ana.id 5000 8000 2000
Ensure-Account $bruno.id 1500 3000 1000
Ensure-Account $carla.id 800 2000 500

Write-Host "== Transacoes da Ana =="
Add-Transactions "ana@demo.com" @(
    @{ type = "DEPOSIT"; amount = 3000; description = "Salario de novembro" },
    @{ type = "PURCHASE"; amount = 120.50; description = "Supermercado"; category = "FOOD" },
    @{ type = "PURCHASE"; amount = 45.90; description = "Padaria e cafe"; category = "FOOD" },
    @{ type = "PURCHASE"; amount = 38; description = "Corrida de aplicativo"; category = "TRANSPORT" },
    @{ type = "PURCHASE"; amount = 89.90; description = "Farmacia"; category = "HEALTH" },
    @{ type = "PURCHASE"; amount = 150; description = "Cinema e jantar"; category = "LEISURE" },
    @{ type = "WITHDRAWAL"; amount = 200; description = "Saque no caixa" },
    @{ type = "TRANSFER"; amount = 300; description = "Pix para o Bruno"; destinationUserId = $bruno.id },
    @{ type = "WITHDRAWAL"; amount = 999999; description = "Saque acima do saldo" },
    @{ type = "PURCHASE"; amount = 2500; description = "Notebook"; category = "SHOPPING" }
)

Write-Host "== Transacoes do Bruno =="
Add-Transactions "bruno@demo.com" @(
    @{ type = "DEPOSIT"; amount = 800; description = "Pagamento de freelance" },
    @{ type = "PURCHASE"; amount = 60; description = "Almoco"; category = "FOOD" },
    @{ type = "PURCHASE"; amount = 35; description = "Metro"; category = "TRANSPORT" }
)

Write-Host "== Transacoes da Carla =="
Add-Transactions "carla@demo.com" @(
    @{ type = "DEPOSIT"; amount = 300; description = "Mesada" },
    @{ type = "PURCHASE"; amount = 120; description = "Consulta"; category = "HEALTH" },
    @{ type = "PURCHASE"; amount = 80; description = "Curso online"; category = "EDUCATION" }
)

Write-Host ""
Write-Host "Pronto. Login (Basic Auth no MS2): ana@demo.com, bruno@demo.com ou carla@demo.com, senha $Password"