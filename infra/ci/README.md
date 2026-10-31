# Entrega efêmera Linux

O workflow `CI Pipeline` executa em push/PR para `main` e `dev`, ou manualmente sem parâmetros. A branch `isac` não inicia execuções em push. O job `verify` executa Maven antes da construção Docker; o job seguinte cria um cluster kind independente em um runner Ubuntu descartável.

Esta raiz Terraform gerencia apenas `oficina-local-ci`, com dois nós Kubernetes 1.34.11, imagem por digest e kubeconfig exclusivo em `.generated/kubeconfig`. O helper aceita somente Docker Linux local, verifica engine/propriedade e rejeita adoção de clusters existentes. Não usa a raiz Windows nem recursos AWS. Não altere/mova o state ou os caminhos durante o ciclo de vida.

A entrega reutiliza `infra/local/database` (PostgreSQL e migrações) e `infra/local/addons` (Traefik/metrics-server), passando kubeconfig absoluto e contexto `kind-oficina-local-ci`. Publica manifests locais com imagem do commit testado carregada no kind; mantém Ingress e HPA. As verificações HTTP consultam somente health/info e conferem o commit. Não criam ordens de serviço nem enviam mensagens: SMTP permanece explicitamente desabilitado, sem leitura de `mail.env` ou exigência de credenciais SMTP. Credenciais de banco/admin/JWT são geradas por execução e mascaradas.

`NVD_API_KEY` é opcional; sem ela desabilita-se somente Dependency Check, mantendo Maven verify. Nenhuma credencial AWS ou registry é necessária. Não se publica imagem externa neste fluxo.

Evidências não incluem secrets, state, kubeconfig ou logs da aplicação. O cleanup `always()` tenta destruir addons, banco e cluster, inclusive recurso tainted mediante helper protegido. Uma falha parcial de `kind create` preserva containers sem prova de propriedade; o runner hospedado é descartado ao término. Cancelamento forçado ou indisponibilidade do runner pode impedir steps `always()`. A entrega é temporária e não constitui hospedagem persistente.
