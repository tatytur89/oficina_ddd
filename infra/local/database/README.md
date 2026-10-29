# PostgreSQL e schema no cluster local

Esta raiz Terraform independente instala PostgreSQL 18.4, com imagem fixada por digest, e executa as migrações versionadas de `src/main/resources/db/migration`. Requer o cluster criado por `infra/local/`, seu kubeconfig dedicado e o namespace da aplicação. Não instala a aplicação nem acessa AWS.

## Provisionar

Execute na raiz do repositório, com Terraform e kubectl no PATH:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/scripts/check.ps1
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local apply -f k8s/local/namespace.yaml
Copy-Item infra/local/database/terraform.tfvars.example infra/local/database/terraform.tfvars
terraform -chdir=infra/local/database init -input=false
terraform -chdir=infra/local/database plan '-out=database.tfplan'
terraform -chdir=infra/local/database apply database.tfplan
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local get statefulset,pods,pvc,service,jobs
```

O exemplo contém somente uma senha de desenvolvimento. Ajuste `database_password` no arquivo real antes do primeiro apply e mantenha o mesmo valor em `SPRING_DATASOURCE_PASSWORD` de `k8s/local/secret.env` para o futuro deploy. Usuário e banco são `oficina_user` e `oficina_db`. A aplicação usará `jdbc:postgresql://postgres:5432/oficina_db`, no mesmo namespace.

O provider usa kubeconfig e contexto explícitos; a configuração exige API em `127.0.0.1`. Para outro cluster de laboratório, informe `kubeconfig_path` absoluto e `context` no arquivo de variáveis. O namespace deve existir previamente. O provider e seu lock ficam separados dos da raiz do cluster.

`terraform.tfvars`, planos e estados são ignorados pelo Git. **O estado Terraform contém a senha do Secret**, mesmo com a variável marcada como sensível; proteja o arquivo e não publique seu conteúdo. Não execute `terraform show -json` em logs públicos.

## Recursos e persistência

- StatefulSet `postgres`, uma réplica, probes TCP locais e limites de CPU/memória.
- Service interno `postgres`, porta 5432, sem publicação na máquina ou na internet.
- Secret `postgres-secret`, consumido pelo banco e pelo Job.
- PVC `data-postgres-0`, 1 GiB, usando a StorageClass padrão do kind.
- ConfigMap com SQL e runner; Job identificado pelo hash desse conteúdo. O apply aguarda o Job concluir e falha se a migração falhar.

O PostgreSQL 18 usa `PGDATA=/var/lib/postgresql/18/docker`; o volume é montado em `/var/lib/postgresql`, conforme a [imagem oficial](https://github.com/docker-library/docs/blob/master/postgres/README.md#pgdata). Reiniciar ou recriar o pod preserva o PVC. A retenção do StatefulSet mantém o volume ao remover o StatefulSet ou executar destroy nesta raiz; o namespace também não pertence a esta raiz.

O armazenamento do kind é local ao nó Docker. Excluir o namespace/PVC ou destruir/substituir o cluster remove os dados; retenção não substitui backup. Uma réplica não oferece alta disponibilidade. O usuário do exemplo é o administrador criado pela imagem, adequado somente ao laboratório.

Alterar a senha no Secret não altera automaticamente a senha de um PostgreSQL já inicializado no PVC. Uma alteração do Secret força nova execução do Job, que falha se a senha não corresponder à do banco. Rotação automática não é implementada. Para rotação, altere a credencial no banco e atualize os Secrets correspondentes de forma coordenada. Não apague o volume para resolver divergência de senha.

## Migrações

`database/migrate.sh` executa os SQLs `V001__descricao.sql`, `V002__descricao.sql`, em ordem. O runner usa uma única sessão psql e transação, com `ON_ERROR_STOP`, lock consultivo e timeouts; DDL e histórico são confirmados juntos. A [documentação psql](https://www.postgresql.org/docs/current/app-psql.html) descreve o tratamento de erros utilizado.

O histórico `public.oficina_schema_history` registra versão, arquivo, checksum SHA-256 e data. Reexecutar o mesmo conteúdo não modifica dados nem reaplica DDL. Alterar uma migração instalada, renomeá-la ou retirar uma versão existente do conjunto produz erro e rollback. Para evoluir o schema, acrescente uma nova versão; não edite a instalada. SQLs não devem conter comandos psql nem controle explícito de transação. Os arquivos são normalizados para LF ao montar o ConfigMap.

O primeiro schema inclui todas as tabelas JPA e as colunas de decisão do orçamento. É destinado a banco vazio. Não use esse baseline para adotar automaticamente um banco existente criado por Hibernate `update`; migre esse banco com backup e reconciliação prévia. O antigo `docs/sql/orcamento-decisao.sql` continua sendo apenas uma atualização incremental para bancos anteriores.

O Job é recriado quando SQL/runner mudam. Um Job concluído não é reexecutado por um apply sem alterações. Para repetir a validação do histórico sem alterar os SQLs:

```powershell
terraform -chdir=infra/local/database plan '-replace=kubernetes_job_v1.schema' '-out=database.tfplan'
terraform -chdir=infra/local/database apply database.tfplan
```

## Remover recursos mantendo dados

```powershell
terraform -chdir=infra/local/database plan -destroy '-out=database-destroy.tfplan'
terraform -chdir=infra/local/database apply database-destroy.tfplan
```

O PVC e o namespace permanecem. Um novo apply reutiliza o volume com suas credenciais originais. A exclusão definitiva de PVC/namespace é uma operação separada que apaga os dados e exige backup prévio. Remova os recursos desta raiz antes de destruir o cluster, para o provider ainda conseguir acessar a API.

## Testar

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalDatabase.ps1
```

O teste usa um namespace temporário exclusivo, valida PostgreSQL real, migrações e persistência, e remove somente seus próprios recursos. A validação Hibernate usa Java 21 e Maven Wrapper, sem iniciar servidor web. A aplicação mantém `ddl-auto=validate` no perfil `lab`; ela não cria nem atualiza o schema no startup.
