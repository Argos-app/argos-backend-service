# Validação local no Kubernetes

Estas instruções são para executar o backend localmente no Kubernetes do Docker Desktop. Use somente credenciais de desenvolvimento ou teste; não use credenciais de produção.

## Pré-requisitos

- Docker Desktop com Kubernetes habilitado e contexto `docker-desktop` ativo.
- Imagem local `argos-backend-service:latest` disponível para o cluster.
- PostgreSQL local rodando e publicado na porta `5432` do host.
- Credenciais de teste do PostgreSQL e de um projeto Firebase de desenvolvimento.

Execute os comandos a partir da raiz do repositório, no PowerShell.

## Criar o namespace e o Secret

O Deployment referencia o Secret obrigatório `argos-backend-secrets`. Crie o namespace primeiro:

```powershell
kubectl config current-context
kubectl apply -f .\kubernetes\base\namespace.yaml
```

Crie fora do repositório um arquivo de ambiente reduzido, por exemplo `%TEMP%\argos-backend-k8s.env`, contendo somente as chaves abaixo. Preencha-as com valores locais/de teste. Não use diretamente o `.env` completo do projeto: ele pode conter variáveis extras ou credenciais de produção.

```dotenv
DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/app_db
DATASOURCE_USER=<usuario-local-do-postgres>
DATASOURCE_PASSWORD=<senha-local-do-postgres>
JWT_SECRET=<segredo-local-para-assinatura>
FIREBASE_SERVICE_ACCOUNT_JSON=<json-de-conta-de-servico-do-projeto-firebase-de-teste-em-uma-linha>
```

O pod não consegue usar `localhost` para alcançar o PostgreSQL do host. `host.docker.internal` direciona a conexão para o host do Docker Desktop; ajuste a porta e o nome do banco se seu Compose local usar outros valores.

No PowerShell, informe o caminho do arquivo e crie/atualize o Secret sem imprimir seus valores:

```powershell
$secretFile = Join-Path $env:TEMP 'argos-backend-k8s.env'
if (-not (Test-Path -LiteralPath $secretFile)) { throw "Arquivo não encontrado: $secretFile" }

kubectl create secret generic argos-backend-secrets `
  --namespace argos-backend `
  --from-env-file="$secretFile" `
  --dry-run=client -o yaml |
  kubectl apply -f -
```

Confirme apenas a existência do Secret, sem exibir os valores:

```powershell
kubectl -n argos-backend get secret argos-backend-secrets
```

Se o endpoint que você pretende validar também usar MongoDB, inclua no mesmo arquivo reduzido a variável `MONGODB_URI` apontando para uma instância local que o pod consiga alcançar. Isso é opcional para endpoints que não usam MongoDB.

## Aplicar e conferir o Deployment

Com a imagem local disponível e o Secret criado:

```powershell
kubectl apply -k .\kubernetes\base
kubectl -n argos-backend rollout status deployment/argos-backend --timeout=120s
kubectl -n argos-backend get pods
```

Se o rollout falhar, consulte os eventos e logs do pod antes de reiniciar o Deployment:

```powershell
kubectl -n argos-backend describe pod <nome-do-pod>
kubectl -n argos-backend logs <nome-do-pod> --previous
```

Esta documentação cobre as dependências do Secret para validação local. Ela não define registry nem pipeline de publicação; a referência de imagem publicada por commit ou digest solicitada na revisão continua sendo tratada separadamente.
