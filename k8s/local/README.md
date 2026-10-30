# Deploy da aplicação no kind

Prepare primeiro o [cluster](../../infra/local/README.md) e o [PostgreSQL com migrações](../../infra/local/database/README.md). Use Windows PowerShell 5.1+, Git, Docker, kind, kubectl e `tar` no PATH. O `tar` do Windows é usado para conferir a configuração da imagem no arquivo carregado nos nós.

## Build e deploy

Execute na raiz do repositório:

```powershell
# Configure JAVA_HOME para Java 21 e execute o gate antes do build.
./mvnw.cmd clean verify '-Ddependency-check.skip=true' -B
Copy-Item k8s/local/secret.env.example k8s/local/secret.env
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Deploy-Local.ps1
```

Não substitua um `secret.env` já configurado. Os valores do exemplo são somente para desenvolvimento. A senha do banco deve corresponder à definida no Terraform do PostgreSQL. O script exige as seis chaves do exemplo, sem chaves adicionais que sobrescrevam o perfil ou o comportamento de criação de schema. Segredos são fornecidos em runtime e ficam fora do contexto Docker.

O script verifica propriedade/Engine do cluster, nós Ready, namespace existente, PostgreSQL disponível e Job de migração concluído. Depois constrói uma imagem com tag exclusiva, marca a origem Git em `APP_COMMIT_SHA` e carrega o mesmo arquivo de imagem nos dois nós. Se houver alterações não commitadas, a origem recebe o sufixo `-dirty`.

O deploy usa uma cópia temporária de `k8s/base` e `k8s/local`, com namespace e imagem explícitos. Mantém `imagePullPolicy: Never`, perfil `lab` e uma réplica. A anotação do Pod registra o ID da imagem e força novo rollout quando um artefato de uma tag fornecida muda. O script aguarda disponibilidade e confere imagem e perfil. Ele não aplica recursos de banco, cria namespace ou publica imagem em registry.

O container executa como UID/GID 1001, sem elevação de privilégio, capabilities ou token da ServiceAccount e com seccomp `RuntimeDefault`. As probes distinguem liveness de readiness; a readiness inclui o banco. O schema deve existir, pois Hibernate usa `validate`.

Para reutilizar uma imagem já construída, forneça sua tag local exclusiva e metadados Git válidos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Deploy-Local.ps1 -Image oficina-ddd:local-<identificador>
```

Esse modo também carrega a imagem e confere sua identidade nos dois nós. A imagem base deve ser construída a partir do Dockerfile do projeto, com `--build-arg APP_COMMIT_SHA=<sha-completo>`; use o sufixo `-dirty` quando houver alterações locais.

## Acessar e verificar

```powershell
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local get deployment,pods,service
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local port-forward --address=127.0.0.1 service/oficina-api 8089:80
```

Mantenha o port-forward aberto enquanto usa a API:

- Swagger: `http://127.0.0.1:8089/swagger-ui/index.html`
- Origem da imagem e Pod: `http://127.0.0.1:8089/api/v1/info`
- Liveness: `http://127.0.0.1:8089/actuator/health/liveness`
- Readiness: `http://127.0.0.1:8089/actuator/health/readiness`

Faça login em `POST /api/v1/auth/login` com `{"usuario":"<ADMIN_USER>","senha":"<ADMIN_PASSWORD>"}` e utilize o token Bearer. Essas credenciais criam o administrador somente quando ele ainda não existe; alterar o Secret não rotaciona a senha de um usuário persistido.

O Service é interno. Ingress e HPA locais serão preparados na próxima etapa; as portas reservadas no cluster ainda não oferecem acesso HTTP à aplicação.

## Falhas e rollback

Falha de build ou de preflight interrompe o script antes de aplicar manifests. Falha de rollout retorna erro e preserva os recursos para diagnóstico; não há exclusão automática ou rollback implícito. Consulte o estado sem publicar logs que contenham dados reais:

```powershell
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local rollout status deployment/oficina-api
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local logs deployment/oficina-api --tail=100
```

Se existir uma revisão anterior funcional e compatível com o schema atual, faça rollback explícito:

```powershell
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local rollout undo deployment/oficina-api
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local rollout status deployment/oficina-api
```

O script não usa prune: ConfigMaps/Secrets antigos e imagens anteriores permanecem para rollback. Eles exigem limpeza posterior, após confirmar quais revisões devem ser preservadas. Rollback da aplicação não reverte migrações do banco.

## Testar o deploy

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalDeployment.ps1 -Image oficina-ddd:local-<identificador> -ExpectedCommit <sha-completo-ou-sha-dirty>
```

O teste cria banco e aplicação em namespace exclusivo, verifica a API real e remove somente o ambiente de teste. Não utiliza nem modifica as OS do namespace de desenvolvimento. Os testes Maven opt-in de PostgreSQL são executados separadamente pelo teste de banco; esta etapa exercita a aplicação por HTTP.
