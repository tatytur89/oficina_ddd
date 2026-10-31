# Deploy da aplicação no kind

A conta SMTP de avaliação está configurada no arquivo versionado `mail.env`. O script de deploy do namespace principal cria/atualiza `oficina-mail-secret` automaticamente. O modelo enviado inclui HTML/CSS e uma alternativa em texto simples. [Configuração e comportamento dos avisos](../../docs/EMAIL_STATUS.md).

Prepare primeiro o [cluster](../../infra/local/README.md), o [PostgreSQL com migrações](../../infra/local/database/README.md) e os [addons de entrada e métricas](../../infra/local/addons/README.md). Use Windows PowerShell 5.1+, Git, Docker, kind, kubectl e `tar` no PATH. O `tar` do Windows é usado para conferir a configuração da imagem no arquivo carregado nos nós.

## Build e deploy

Execute na raiz do repositório:

```powershell
# Configure JAVA_HOME para Java 21 e execute o gate antes do build.
./mvnw.cmd clean verify '-Ddependency-check.skip=true' -B
Copy-Item k8s/local/secret.env.example k8s/local/secret.env
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Deploy-Local.ps1
```

Não substitua um `secret.env` já configurado. Os valores do exemplo são somente para desenvolvimento. A senha do banco deve corresponder à definida no Terraform do PostgreSQL. O script exige as seis chaves do exemplo, sem chaves adicionais que sobrescrevam o perfil ou o comportamento de criação de schema. Segredos são fornecidos em runtime e ficam fora do contexto Docker.

O script verifica propriedade/Engine do cluster, nós Ready, namespace existente, PostgreSQL disponível, Job de migração concluído, metrics-server disponível e controlador Traefik pronto. Depois constrói uma imagem com tag exclusiva, marca a origem Git em `APP_COMMIT_SHA` e carrega o mesmo arquivo de imagem nos dois nós. Se houver alterações não commitadas, a origem recebe o sufixo `-dirty`.

O deploy usa uma cópia temporária de `k8s/base` e `k8s/local`, com namespace e imagem explícitos. Mantém `imagePullPolicy: Never` e perfil `lab`. O HPA controla de 2 a 5 réplicas, com alvo de CPU de 70% dos requests; o Deployment não fixa `replicas`, para preservar a escala escolhida pelo controlador em novos deploys. A anotação do Pod registra o ID da imagem e força novo rollout quando um artefato de uma tag fornecida muda. O script aguarda pelo menos o mínimo de réplicas disponíveis e confere imagem e perfil. Ele não aplica recursos de banco, cria namespace ou publica imagem em registry.

O container executa como UID/GID 1001, sem elevação de privilégio, capabilities ou token da ServiceAccount e com seccomp `RuntimeDefault`. As probes distinguem liveness de readiness; a readiness inclui o banco. O schema deve existir, pois Hibernate usa `validate`.

Para reutilizar uma imagem já construída, forneça sua tag local exclusiva e metadados Git válidos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Deploy-Local.ps1 -Image oficina-ddd:local-<identificador>
```

Esse modo também carrega a imagem e confere sua identidade nos dois nós. A imagem base deve ser construída a partir do Dockerfile do projeto, com `--build-arg APP_COMMIT_SHA=<sha-completo>`; use o sufixo `-dirty` quando houver alterações locais.

## Acesso HTTP pelo Ingress

O Traefik atende pela porta de loopback configurada ao criar o cluster (padrão 8090). O Ingress escolhe a aplicação pelo hostname `oficina.local`. Sem alterar DNS ou o arquivo hosts do computador, teste com o header explícito:

```powershell
curl.exe -H 'Host: oficina.local' http://127.0.0.1:8090/api/v1/info
curl.exe -H 'Host: oficina.local' http://127.0.0.1:8090/actuator/health/readiness
```

No Postman/Bruno, use a mesma URL de loopback e header `Host: oficina.local`. Para acessar pelo navegador com `http://oficina.local:8090/swagger-ui/index.html`, configure a resolução de `oficina.local` para `127.0.0.1` no seu ambiente. Os scripts não alteram DNS, hosts ou contextos globais. TLS com certificado confiável não é provisionado nesta etapa; o acesso demonstrado é HTTP restrito ao loopback.

Para namespaces temporários, o script usa `<namespace>.oficina.local` automaticamente. Pode-se fornecer `-IngressHost <nome>.oficina.local`; hosts já pertencentes a outro recurso são recusados. O Ingress tem classe `traefik` e label `app.kubernetes.io/part-of=oficina-local`, observado pelo controlador do laboratório.

## Verificar e acessar por port-forward

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

O Service continua interno; o Ingress encaminha HTTP para ele. O HPA usa métricas reais de CPU do metrics-server. A escala sobe no máximo dois Pods a cada 30 segundos; para reduzir, considera uma janela de estabilização de 60 segundos e remove no máximo um Pod a cada 30 segundos. Essas opções do laboratório não alteram o HPA AWS, que mantém sua janela de 300 segundos.

```powershell
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local get ingress,hpa,pods
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-local top pods
```

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

Para verificar a escala com requisições HTTP reais, use `scripts/tests/Test-LocalScaling.ps1` com os mesmos parâmetros `-Image` e `-ExpectedCommit`. O teste usa hostname e namespace próprios, faz login com senha incorreta para gerar carga de verificação de senha sem alterar OS, comprova o aumento e a redução das réplicas e remove a carga. Os addons devem estar instalados antes dos testes. O teste não força `replicas` da API nem altera o mínimo/máximo do HPA para simular sucesso.
