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

Agentes separados implementaram testes e revisaram a qualidade. Os bloqueadores encontrados na revisão foram corrigidos antes da publicação da etapa correspondente. Commits e pushes foram feitos em etapas na branch isac.

Comando final:

```bash
./mvnw verify -Ddependency-check.skip=true -B
```

No Windows, o comando equivalente usa `mvnw.cmd` e Java 21. Também foi executado `clean verify` antes do commit de aprovação/recusa.

Resultado final: **420 testes, zero falhas, zero erros e nenhum ignorado**. Pacote gerado e gate JaCoCo aprovado. Cobertura no escopo configurado do relatório: **97,60% das linhas (1502/1539)** e **84,15% dos branches (292/347)**.

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
