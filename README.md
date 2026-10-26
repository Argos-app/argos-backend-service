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

### Carga local de dados de teste

Com o arquivo `.env` local preenchido com `POSTGRES_DB`, `DATASOURCE_USER` e `DATASOURCE_PASSWORD`, inicie o PostgreSQL e a aplicação ao menos uma vez para o Flyway aplicar as migrations. Depois carregue os dados de teste usando `make seed` ou, no Windows, `.\scripts\dev.ps1 seed`:

```bash
make seed
```

O Compose instala o Python e o driver dentro de um container temporário; não é necessário instalar essas dependências na máquina. A carga cria registros fictícios com e-mails `@argos.test`, deve ser executada uma vez por banco e falha com uma mensagem clara se os dados já estiverem presentes.

---

## Deploy no Render

Use o `Dockerfile` da raiz. O container lê `PORT` e inicia com o perfil `prod` por padrão.
Se `SPRING_PROFILES_ACTIVE` já estiver definido no serviço, ajuste seu valor para `prod`.

Configure `DATASOURCE_URL`, `DATASOURCE_USER`, `DATASOURCE_PASSWORD`, `JWT_SECRET` e
`FIREBASE_SERVICE_ACCOUNT_JSON` no ambiente do serviço. Na interface de variáveis,
o valor do JSON deve começar com `{` e terminar com `}`, sem aspas simples externas.
O arquivo de credenciais Firebase local não é enviado ao contexto Docker.

O login é `POST /api/v1/auth/signin`, com `Authorization: Bearer <Firebase ID token>`.
O usuário precisa existir no Firebase e na tabela `app_user` do banco usado pelo serviço.
Um usuário ausente nesse banco recebe `404`; o login não exclui sua conta Firebase.
O JWT devolvido por essa chamada é usado nas demais rotas protegidas.

Os logs mostram `firebase_initialized` com o projeto e a origem das credenciais,
`signin_request` e `signin_response` com método e status, e `firebase_token_rejected`
com o código do Firebase. Nenhum desses logs contém tokens, e-mails ou chaves privadas.

## Notificações de fim de carência

O aplicativo deve enviar seu token FCM em `POST /api/v1/devices/sync` após o login
e quando o token for renovado. No logout, deve chamar `DELETE /api/v1/devices/{deviceId}`.
O backend usa as mesmas credenciais Firebase Admin da autenticação para enviar push.

A rotina `argos.routines.withdrawal.cron` (padrão: 8h, no fuso
`argos.routines.time-zone`) encontra aplicações cuja carência termina no dia e
cujo lote está liberado. Ela cria um aviso em `alert` para o usuário da aplicação
e uma entrega `PUSH` em `notification_delivery` para cada dispositivo ativo. O
payload FCM contém `type=CARENCIA`, `alertId` e `lotId`; o texto é enviado como
notificação visível. O alerta de início da carência criado pelo trigger do banco
é um evento separado.

Entregas pendentes ou com falha são reprocessadas a cada 15 minutos por
`argos.routines.withdrawal.retry-cron` (até três tentativas). Tokens confirmados
como não registrados pelo FCM são desativados. Os IDs estáveis de aviso e entrega
impedem registros duplicados quando a rotina executa novamente. Como o FCM e o
banco não compartilham uma transação, uma interrupção após o envio e antes da
confirmação no banco pode resultar em novo push na tentativa seguinte.

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

