<div align="center">

# Argos - argos.backend.service 

Backend da plataforma Argos.

<img alt="Java" src="https://img.shields.io/badge/Java-%2321-%23ED8B00?logo=openjdk" />
<img alt="Spring Boot" src="https://img.shields.io/badge/spring--boot_3.3.0-6DB33F?logo=springboot&logoColor=white" />
<img alt="GitHub Actions" src="https://img.shields.io/badge/ci-GitHub_Actions-2088FF?logo=githubactions&logoColor=white" />
<img alt="License" src="https://img.shields.io/github/license/Argos-app/argos-backend-service" />
<img alt="Last Commit" src="https://img.shields.io/github/last-commit/Argos-app/argos-backend-service" />
<img alt="Repo Size" src="https://img.shields.io/github/languages/code-size/Argos-app/argos-backend-service" />

</div>

---

## Começando

Consulte o [CONTRIBUTING.md](./CONTRIBUTING.md) para instruções completas de configuração do ambiente local, padrão de commits e fluxo de trabalho.

---

## Deploy no Discloud

Este projeto está configurado para deploy na [Discloud](https://discloud.com) como uma aplicação `site` (API).

### Pré-requisitos

- Instalar a [Discloud CLI](https://docs.discloud.com/en/how-to-host-using/cli):
  ```bash
  npm install -g discloud-cli
  discloud --login
  ```
- Registrar um subdomínio `argos-backend.discloud.app` no painel da Discloud (Plano Platinum ou superior é necessário para `TYPE=site`).
- Definir as variáveis de ambiente de produção no painel da Discloud (Dashboard → aplicação → Environment). **Não** commit o `.env` de produção no repositório:
  | Variável | Descrição |
  | --- | --- |
  | `DATASOURCE_URL` | JDBC URL do PostgreSQL |
  | `DATASOURCE_USER` | Usuário do PostgreSQL |
  | `DATASOURCE_PASSWORD` | Senha do PostgreSQL |
  | `MONGODB_URI` | Connection string do MongoDB |

### Deploy

```bash
make deploy
```

O alvo `deploy` executa `scripts/discloud-deploy.sh`, que builda o projeto (`./mvnw clean package`), gera o `app.jar` na raiz, publica via `discloud up` e limpa o jar temporário.

### Configuração

O arquivo [`discloud.config`](./discloud.config) define a aplicação na Discloud:

| Chave | Valor | Descrição |
| --- | --- | --- |
| `NAME` | `argos-backend-service` | Nome da aplicação |
| `TYPE` | `site` | Aplicação web/API |
| `ID` | `argos-backend` | Subdomínio registrado |
| `MAIN` | `app.jar` | Arquivo JAR principal (gerado com `finalName=app` no `pom.xml`) |
| `START` | `java -Dspring.profiles.active=prod -jar app.jar` | Comando de inicialização (perfil `prod`) |
| `RAM` | `1024` | Memória alocada em MB |
| `VERSION` | `latest` | Versão do runtime Java |

A aplicação escuta em `0.0.0.0:8080` (exigido pela Discloud para sites) conforme o [`application-prod.properties`](./src/main/resources/application-prod.properties).

