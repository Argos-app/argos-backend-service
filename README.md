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
make deploy            # production (argos-backend.discloud.app)
make deploy-homolog    # homologation (argos-backend-hmg.discloud.app)
```

No Windows (PowerShell):

```powershell
.\scripts\dev.ps1 deploy            # production (argos-backend.discloud.app)
.\scripts\dev.ps1 deploy-homolog    # homologation (argos-backend-hmg.discloud.app)
```

Os alvos de deploy executam `scripts/discloud-deploy.sh` (Linux/macOS) ou `scripts/discloud-deploy.ps1` (Windows), que buildam o projeto (`./mvnw clean package`), geram o `app.jar` na raiz e publicam na Discloud:

- `discloud app commit <id>` quando a aplicação já existe;
- `discloud app up` como fallback na primeira subida (cria a aplicação).

### Configuração
O arquivo [`discloud.config`](./discloud.config) define a aplicação na Discloud (produção) e [`discloud.config.homolog`](./discloud.config.homolog) define a de homologação. Cada ambiente possui seu próprio subdomínio e suas próprias variáveis de ambiente:

| Ambiente | Subdomínio | Config | Comando (Linux/macOS) | Comando (Windows) |
| --- | --- | --- | --- | --- |
| Produção | `argos-backend.discloud.app` | `discloud.config` | `make deploy` | `.\scripts\dev.ps1 deploy` |
| Homologação | `argos-backend-hmg.discloud.app` | `discloud.config.homolog` | `make deploy-homolog` | `.\scripts\dev.ps1 deploy-homolog` |

Ambos os ambientes usam o perfil Spring `prod`, portanto os dois leem as mesmas variáveis (`DATASOURCE_URL`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD`, `MONGODB_URI`) — **defina valores diferentes para cada aplicação** no painel da Discloud (Dashboard → aplicação → Environment), apontando para bancos separados.

| Chave | Descrição |
| --- | --- |
| `NAME` | Nome da aplicação |
| `TYPE` | `site` (aplicação web/API) |
| `ID` | Subdomínio registrado |
| `MAIN` | Arquivo JAR principal (gerado com `finalName=app` no `pom.xml`) |
| `START` | `java -Dspring.profiles.active=prod -jar app.jar` |
| `RAM` | Memória alocada em MB |
| `VERSION` | Versão do runtime Java |

A aplicação escuta em `0.0.0.0:8080` (exigido pela Discloud para sites) conforme o [`application-prod.properties`](./src/main/resources/application-prod.properties).

