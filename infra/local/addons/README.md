# Ingress e métricas do laboratório local

Esta raiz Terraform independente instala somente Traefik e Metrics Server no cluster kind existente. Não recria o cluster, não altera banco ou aplicação e não acessa AWS. Os estados do cluster, banco e addons ficam em diretórios separados.

## Versões e exposição

| Componente | Versão fixada |
|---|---|
| Provider Helm | 2.17.0 |
| Chart Traefik | 42.1.3 |
| Traefik Proxy | 3.7.16, imagem fixada por digest |
| Chart Metrics Server | 3.14.0 |
| Metrics Server | 0.9.1, imagem fixada por digest |

Traefik é instalado como `Deployment/oficina-traefik` no namespace `oficina-ingress`. Usa a classe **traefik**, que não é marcada como classe padrão. O controlador observa Ingresses de todos os namespaces, mas processa apenas os que selecionam essa classe e contêm a label `app.kubernetes.io/part-of: oficina-local`. Isso permite namespaces de teste isolados sem reconfigurar o controlador nem capturar Ingresses sem essa identificação.

O pod roda no control plane marcado `ingress-ready=true`, com tolerância à taint correspondente. Seus ports 8000/8443 são mapeados por hostPort para 80/443 dentro do nó kind, sem exigir processo root. A raiz do cluster já publica esses ports na máquina exclusivamente em `127.0.0.1:8090` e `127.0.0.1:8443` por padrão. Para uma configuração de cluster com portas personalizadas, consulte os outputs daquela raiz.

O rollout Traefik usa `Recreate`: há um único nó para esses hostPorts e um segundo pod simultâneo não conseguiria ocupar as mesmas portas. Uma atualização do controlador pode causar uma breve interrupção de acesso HTTP. API/dashboard e access log ficam desativados; o provider de CRDs e a instalação de CRDs também ficam desativados, pois a aplicação usa o recurso Ingress padrão.

A porta HTTPS fica preparada, mas esta etapa não configura certificado confiável, ACME nem TLS de produção. O laboratório usa o endereço HTTP para seus testes de aplicação.

Metrics Server é instalado como `Deployment/oficina-metrics-server` em `kube-system`, com APIService `v1beta1.metrics.k8s.io`. Somente neste laboratório, `--kubelet-insecure-tls` permite coletar dos kubelets kind; o APIService também aceita o certificado gerado pelo Metrics Server sem verificar uma CA externa. Essas opções não devem ser copiadas para a configuração AWS ou de produção.

## Instalar e verificar

Pré-requisitos: cluster local pronto, kubeconfig dedicado, Terraform e kubectl no PATH e acesso aos repositórios oficiais de charts/imagens. Execute na raiz do projeto:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File infra/local/scripts/check.ps1
terraform -chdir=infra/local/addons init -input=false
terraform -chdir=infra/local/addons plan '-out=addons.tfplan'
terraform -chdir=infra/local/addons apply addons.tfplan
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n oficina-ingress rollout status deployment/oficina-traefik --timeout=300s
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local -n kube-system rollout status deployment/oficina-metrics-server --timeout=300s
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local wait --for=condition=Available apiservice/v1beta1.metrics.k8s.io --timeout=120s
kubectl --kubeconfig infra/local/.generated/kubeconfig --context kind-oficina-local top nodes
```

As primeiras amostras de métricas podem levar alguns ciclos de coleta; readiness do Deployment, disponibilidade do APIService e `top nodes` são verificações distintas. A existência de métricas não comprova que o HPA já escalou a aplicação; isso exige a carga e a observação de réplicas do teste específico.

Os dois releases aguardam readiness e usam instalação/upgrade atômico. Essa atomicidade é por release, não uma transação que inclua ambos: se um componente falhar, o outro pode ter sido instalado com sucesso. Reaplique após corrigir a causa; não adote releases ou APIService alheios quando ocorrer conflito de ownership Helm.

O kubeconfig padrão é `../.generated/kubeconfig`, relativo à raiz addons, e o contexto é `kind-oficina-local`. Para outro cluster local, informe `kubeconfig_path` e `context` em `terraform.tfvars`, ignorado pelo Git. O contexto deve ter prefixo `kind-oficina-local` e apontar para `https://127.0.0.1:<porta>`; endpoints externos são rejeitados.

O arquivo de lock do provider é versionado. Planos, estado, variáveis reais e diretório `.terraform` não devem ser publicados. Nenhum output contém certificado, chave privada ou conteúdo do kubeconfig.

## Contrato do Ingress da aplicação

O Ingress e o HPA da aplicação são aplicados separadamente. O Ingress precisa destes campos, além do backend Service correto:

```yaml
metadata:
  labels:
    app.kubernetes.io/part-of: oficina-local
spec:
  ingressClassName: traefik
```

O host/path do Ingress deve corresponder ao pedido HTTP enviado ao laboratório. A classe e a label não substituem as regras de host e roteamento. A aplicação continua com Service interno; o serviço Traefik também é ClusterIP, pois o acesso ao host é feito pelos hostPorts do controlador.

## Remoção

```powershell
terraform -chdir=infra/local/addons plan -destroy '-out=addons-destroy.tfplan'
terraform -chdir=infra/local/addons apply addons-destroy.tfplan
```

Isso remove os releases e os recursos que eles gerenciam, incluindo a classe Ingress e a API de métricas. Não remove o cluster, o banco, seus volumes ou o Deployment da aplicação. O HPA deixa de receber métricas e o Ingress deixa de atender quando os respectivos componentes são removidos. O namespace `oficina-ingress`, criado pelo mecanismo `create_namespace` do Helm, pode permanecer; não há limpeza genérica de namespaces.

## Referências oficiais

- [Traefik chart 42.1.3 e compatibilidade Kubernetes](https://github.com/traefik/traefik-helm-chart/blob/v42.1.3/traefik/Chart.yaml).
- [Parâmetros do chart Traefik 42.1.3](https://github.com/traefik/traefik-helm-chart/blob/v42.1.3/traefik/values.yaml).
- [Metrics Server 0.9.1: requisitos e suporte a Kubernetes 1.34+](https://github.com/kubernetes-sigs/metrics-server/blob/v0.9.1/README.md).
- [Chart Metrics Server 3.14.0](https://github.com/kubernetes-sigs/metrics-server/blob/metrics-server-helm-chart-3.14.0/charts/metrics-server/Chart.yaml).
