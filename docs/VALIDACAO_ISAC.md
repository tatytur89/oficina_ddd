# Validação dos fluxos da Fase 2 — branch isac

Base: `dev` em `edacd5c`. Implementação e revisão em 28/10/2026.

## Escopo entregue

- Autenticação desacoplada de adapters concretos por portas de geração de token e verificação de senha. Teste de arquitetura protege os limites entre camadas.
- Abertura de OS aceita serviços e peças do catálogo, valida vínculo cliente/veículo e quantidades, calcula snapshots e totais e retorna identificação. Continua em RECEBIDA sem consumir estoque.
- Callback externo de aprovação/recusa usa chave da OS e versão do orçamento. A mesma decisão repetida é idempotente; versão antiga e decisão contraditória são rejeitadas.
- Recusa retorna ao diagnóstico preservando itens; aprovação inicia execução. O PATCH genérico não contorna a aprovação.
- Alterações da mesma OS usam bloqueio transacional, incluindo callbacks simultâneos.
- Fila ordenada por Execução, Aguardando Aprovação, Diagnóstico e Recebida, depois data crescente e ID. Finalizadas/entregues são ocultadas apenas da fila, preservando histórico e métricas.

## Testes e avaliação

As implementações foram verificadas por testes automatizados e revisão técnica. Os problemas bloqueadores encontrados foram corrigidos antes da publicação da etapa correspondente.

Comando final:

```bash
./mvnw verify -Ddependency-check.skip=true -B
```

No Windows, o comando equivalente usa `mvnw.cmd` e Java 21. Também foi executado `clean verify` antes do commit de aprovação/recusa.

Resultado da implementação funcional em 28/10/2026: **420 testes, zero falhas, zero erros e nenhum ignorado**. Pacote gerado e gate JaCoCo aprovado. Cobertura no escopo configurado do relatório: **97,60% das linhas (1502/1539)** e **84,15% dos branches (292/347)**.

Cobertura funcional inclui:

- Abertura com e sem itens, totais, UUID, IDs e vínculo inválidos, catálogo inexistente e ausência de baixa na abertura.
- Quantidade zero/negativa diretamente no domínio, inclusive preço zero, e bloqueio de inclusão inicial após persistência ou diagnóstico.
- Prioridade, antiguidade, desempate estável, ocultação sem exclusão e preservação das consultas históricas.
- Aprovação e recusa persistidas, replay após recarregar o banco, versões obsoletas e decisões contraditórias.
- Rollback da primeira baixa quando outra peça do orçamento está sem estoque.
- Duas aprovações simultâneas da mesma OS com uma única baixa de estoque.
- Integração HTTP com filtros reais, login, rotas administrativas sem token retornando 403 e callback público validando a chave.
- Fluxo HTTP completo: abertura com itens → diagnóstico → orçamento → aprovação/repetição → finalização → entrega → saída da fila e consulta histórica.

## Limites e implantação

- A integração usa H2 em modo PostgreSQL; não substitui homologação em PostgreSQL real.
- Dependency-Check foi explicitamente desativado nesses comandos. Não há conclusão de auditoria de vulnerabilidades nesta validação.
- Não foi feito deploy em Kubernetes/AWS nem demonstração de HPA nesta etapa. Os workflows existentes continuam com seus gatilhos de main; os testes relatados foram executados localmente.
- Antes do rollout em banco PostgreSQL existente com `ddl-auto=validate`, aplicar `docs/sql/orcamento-decisao.sql`. É atualização incremental de duas colunas, não criação do schema inicial ou automação do banco.
- O lock serializa alterações da mesma OS. Controle de concorrência de estoque entre OS diferentes continua como pendência separada.
- As listas de itens na abertura são opcionais para compatibilidade. O diagnóstico acrescenta itens; reenviar os já solicitados os duplica, conforme documentado no README.

## Separação dos ambientes local e AWS — 29/10/2026

Configuração Kubernetes compartilhada em `k8s/base/`, com ambientes explícitos em `k8s/local/` e `k8s/aws/`. O Terraform AWS manteve a raiz existente; `infra/local/` possui backend local independente, ainda sem recursos de cluster. Os perfis Spring foram separados entre desenvolvimento (`local`/`docker`) e ambientes com configuração externa obrigatória (`lab`/`aws`).

Validações executadas:

```bash
./mvnw clean verify -Ddependency-check.skip=true -B
terraform -chdir=infra/local fmt -check
terraform -chdir=infra/local init -backend=false -input=false
terraform -chdir=infra/local validate
bash -n scripts/destroy.sh
```

No Windows, as verificações de isolamento dos arquivos podem ser repetidas em PowerShell com Docker e kubectl disponíveis no PATH:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-EnvironmentSeparation.ps1
```

Resultados: **438 testes Java aprovados**, incluindo **18 testes de resolução real dos perfis**, e **61 verificações de isolamento aprovadas**. Pacote e gate de cobertura aprovados; cobertura de linhas permanece em 97,60% no escopo configurado.

As verificações de isolamento renderizam os dois ambientes em cópia temporária, comprovam falha local sem arquivo de configuração do Secret e reproduzem o patch do CD para confirmar que o SHA testado sobrevive à transformação Kustomize. Conferem nomes, namespaces, referências de Secret/ConfigMap, Compose, exclusão de arquivos sensíveis e preservação de contextos, configurações e containers existentes.

Não houve `apply`, criação de cluster/banco/container ou execução de API AWS nesta etapa. O Terraform local ainda não provisiona recursos; sua validação não comprova funcionamento de cluster. A configuração AWS foi revisada e renderizada, sem homologação de deploy no EKS. Dependency-Check continuou desativado no comando Maven.

## Cluster Kubernetes local — 29/10/2026

A raiz `infra/local/` provisiona kind por `terraform_data` e CLI 0.33.0, com um control plane e um worker Kubernetes 1.34.11. A imagem é fixada por digest; API e portas reservadas para Ingress são publicadas somente em loopback. O kubeconfig fica isolado em `.generated/`, sem alterar o contexto padrão.

Validações executadas:

```powershell
terraform -chdir=infra/local fmt -check -recursive
terraform -chdir=infra/local validate
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-EnvironmentSeparation.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalCluster.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalClusterGuards.ps1
```

Resultados: **65 verificações de separação**, **43 verificações do ciclo real do cluster** e **26 verificações simuladas de proteção**, aprovadas. O cluster temporário foi removido, seu estado ficou sem recursos e os containers/configurações alheios permaneceram inalterados. O ciclo real comprovou criação, dois nós Ready, API pronta, plano sem alterações, recusa de remoção sem propriedade, recusa de IDs desconhecidos e detecção de um worker removido fora do Terraform.

Os testes simulados comprovaram recusa de Engine diferente e endpoints remotos, serialização de criações concorrentes entre raízes, uso obrigatório do provider Docker, preservação de containers não comprovados após falha e limpeza de nós cuja criação teve sucesso e propriedade registrada.

O cluster de desenvolvimento `oficina-local` foi criado e verificado separadamente. Não foram provisionados recursos AWS, banco, aplicação, Ingress ou HPA. Os testes Java não foram repetidos nesta alteração de infraestrutura; os 438 testes da etapa anterior permanecem como evidência da aplicação. Um plano Terraform vazio não detecta drift externo deste recurso; a verificação operacional explícita continua obrigatória, conforme `infra/local/README.md`.

## PostgreSQL local e migrações — 29/10/2026

A raiz independente `infra/local/database/` provisiona PostgreSQL 18.4 por provider Kubernetes, com StatefulSet, Service interno, Secret, PVC retido e Job de migração. SQL versionado e runner criam as oito tabelas JPA, incluindo decisão e versão do orçamento. O perfil `lab` continua com Hibernate `validate`, sem criação automática de schema.

Validações executadas:

```powershell
terraform -chdir=infra/local/database fmt -check
terraform -chdir=infra/local/database validate
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-EnvironmentSeparation.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalDatabase.ps1
./mvnw.cmd clean verify '-Ddependency-check.skip=true' -B
```

Resultados: **70 verificações de isolamento**, **40 verificações reais do banco** e **3 testes JUnit contra PostgreSQL real** aprovados. A suíte completa posterior passou com **438 testes executados** e **3 testes PostgreSQL opt-in ignorados**, já aprovados na execução dedicada; nenhum erro ou falha. Build, pacote e gate de cobertura aprovados, com **97,60% de cobertura de linhas** no escopo configurado. Dependency-Check permaneceu desativado.

O teste independente criou namespace temporário exclusivo, verificou autenticação válida/inválida pelo Service, aplicação inicial e reexecução sem alterar histórico, rejeição de checksum alterado e rollback de DDL/histórico após SQL inválido. A linha sentinela persistiu após recriar o pod. O Hibernate validou todas as entidades contra o schema migrado; round-trip de OS/itens/estoque e unicidade de documento foram exercitados sem iniciar aplicação ou servidor web.

O destroy do banco preservou namespace e PVC Bound. A limpeza posterior removeu somente o namespace criado pelo teste, após conferir seu UID. Nove snapshots externos permaneceram iguais. O banco de desenvolvimento em `oficina-local` permaneceu ativo, com PVC Bound, Job concluído e plano Terraform sem alterações.

Não houve deploy da aplicação, Ingress, HPA ou recursos AWS. O armazenamento é local ao nó kind: excluir PVC/namespace ou substituir o cluster perde dados. Rotação automática de senha e adoção de banco existente não são implementadas; os procedimentos e limites estão em `infra/local/database/README.md`.
