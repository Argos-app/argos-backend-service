# Contributing Guide

Guia de configuração e fluxo de trabalho para novos desenvolvedores no projeto.

---

## Índice

- [Pré-requisitos](#pré-requisitos)
- [Configuração do ambiente local](#configuração-do-ambiente-local)
- [Subindo os serviços](#subindo-os-serviços)
- [Rodando a aplicação](#rodando-a-aplicação)
- [Conventional Commits](#conventional-commits)
- [Fluxo de branches](#fluxo-de-branches)
- [Abrindo um Pull Request](#abrindo-um-pull-request)

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- [Node.js](https://nodejs.org) (necessário para os hooks de commit)
- [Java 21](https://adoptium.net)
- [IntelliJ IDEA](https://www.jetbrains.com/idea)
- [Git](https://git-scm.com)

---

## Configuração do ambiente local

### 1. Clone o repositório

```bash
git clone https://github.com/Argos-app/argos-backend-service.git
cd argos-backend-service
```

### 2. Configure as variáveis de ambiente

```bash
cp .env.example .env
```

Abra o `.env` e preencha os valores. Nunca compartilhe ou versione esse arquivo.

### 3. Instale as dependências Node

Mesmo sendo um projeto Java, o repositório usa Node para os hooks de commit via Husky.

```bash
npm install
```

> Este passo é obrigatório. Sem ele, o commitlint não vai funcionar e seus commits serão bloqueados.

### 4. Configure o `application-local.properties`

Crie o arquivo em `src/main/resources/application-local.properties` com os valores do seu `.env`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/app_db
spring.datasource.username=app_user
spring.datasource.password=sua_senha

spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=sua_senha

spring.data.mongodb.uri=mongodb://app_user:sua_senha@localhost:27017/app_db?authSource=admin
```

> Este arquivo está no `.gitignore` e não deve ser versionado.

### 5. Configure o perfil local no IntelliJ

1. Abra **Run > Edit Configurations**
2. Selecione `BackendServicesApplication`
3. No campo **Active profiles**, digite: `local`
4. Clique em **OK**

---

## Subindo os serviços

O ambiente local roda PostgreSQL, Redis e MongoDB via Docker Compose.

**Abra o PowerShell na raiz do projeto** e use o script `dev.ps1`:

| Comando | O que faz |
|---|---|
| `.\scripts\dev.ps1 up` | Sobe todos os serviços em background |
| `.\scripts\dev.ps1 down` | Para todos os serviços |
| `.\scripts\dev.ps1 restart` | Reinicia todos os serviços |
| `.\scripts\dev.ps1 logs` | Exibe logs em tempo real |
| `.\scripts\dev.ps1 ps` | Lista o status dos containers |
| `.\scripts\dev.ps1 clean` | Remove containers e volumes (reset completo) |
| `.\scripts\dev.ps1 run` | Inicializa a aplicação Spring Boot |
| `.\scripts\dev.ps1 test` | Roda todos os testes da aplicação |
| `.\scripts\dev.ps1 format` | Formata automaticamente o código (Spotless) |
| `.\scripts\dev.ps1 lint` | Análise estática de código (PMD) |

> **Atenção:** `clean` apaga todos os dados locais. Use com cuidado.

Se o Windows bloquear a execução do script, rode uma vez no PowerShell como administrador:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

> **Observação:** Se você estiver utilizando uma distribuição **Linux**, utilize o script Makefile. Por exemplo:

```Makefile
make run
```

---

## Rodando a aplicação

Com os serviços no ar (`.\scripts\dev.ps1 ps` mostrando `healthy`), rode a aplicação direto pelo IntelliJ clicando em **Run** na classe `BackendServicesApplication`.

---

## Conventional Commits

Este repositório segue o padrão [Conventional Commits](https://www.conventionalcommits.org). O formato é obrigatório e validado automaticamente pelo commitlint via Husky ao fazer commit.

### Formato

```
<tipo>(<escopo>): <descrição curta>
```

### Tipos aceitos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `refactor` | Refatoração sem mudança de comportamento |
| `chore` | Tarefas de manutenção, dependências, configs |
| `docs` | Documentação |
| `test` | Testes |
| `perf` | Melhoria de performance |
| `ci` | Mudanças na pipeline de CI/CD |

### Exemplos

```bash
git commit -m "feat(auth): adiciona autenticação com JWT"
git commit -m "fix(product): corrige cálculo de desconto para quantidade zero"
git commit -m "chore(deps): atualiza spring-boot para 3.5.0"
git commit -m "docs: atualiza guia de configuração local"
```

> Commits fora desse padrão serão rejeitados automaticamente.

---

## Fluxo de branches

```
feature/minha-feature → develop → main
```

| Branch | Propósito |
|---|---|
| `main` | Código em produção |
| `develop` | Integração antes de ir para produção |
| `feature/*` | Desenvolvimento de novas funcionalidades |
| `fix/*` | Correções de bugs |
| `chore/*` | Manutenção e configurações |

### Criando uma branch

```bash
git checkout develop
git pull
git checkout -b feature/numero-da-feature
```

---

## Abrindo um Pull Request

1. Faça push da sua branch para o repositório remoto
2. Abra um Pull Request apontando para `develop`
3. Preencha o template de PR completamente
4. Aguarde ao menos uma aprovação antes de fazer merge
5. Use **Squash Merge**, o título do PR vira o commit de merge e deve seguir o padrão Conventional Commits
