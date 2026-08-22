# Decisões Arquiteturais

Este documento registra as principais decisões de arquitetura e design do projeto, no formato de ADR (*Architecture Decision Record*): contexto, decisão tomada, justificativa e alternativas consideradas. O objetivo é explicitar o raciocínio por trás de cada escolha, não apenas descrever o que foi implementado.

---

## 1. Arquitetura Hexagonal (Ports & Adapters) com DDD tático

**Contexto:** o sistema precisa expor regras de negócio (cadastro, orçamento, controle de estoque, máquina de estados de OS) através de uma API REST, persistindo em um banco relacional, sem acoplar essas regras aos frameworks de infraestrutura.

**Decisão:** o domínio (`domain/entities`, `domain/valueobjects`) não depende de nenhuma anotação de framework (nada de `@Entity`, `@RestController` etc.). A comunicação com o mundo externo passa por portas (`ports/in` para casos de uso, `ports/out` para persistência), implementadas por adaptadores (`adapters/in/web`, `adapters/out/persistence`, `adapters/out/security`).

**Justificativa:** isso permite testar toda a lógica de negócio (ex.: `OrdemServicoTest`, `PecaTest`) sem subir um contexto Spring, sem banco e sem HTTP — os testes de domínio deste projeto rodam em milissegundos. Também isola o impacto de uma troca de framework web ou de banco: a regra "não é possível adicionar peça numa OS que já passou de diagnóstico" (`OrdemServico.adicionarPeca`) não sabe que existe um Spring MVC ou um PostgreSQL.

**Alternativas consideradas:** uma arquitetura em camadas mais simples (Controller → Service → Repository, com entidades JPA sendo usadas diretamente como modelo de domínio) foi descartada porque misturaria anotações de persistência com regras de negócio, dificultando testar a lógica isoladamente e tornando qualquer entidade "anêmica" por natureza — o oposto do que o edital pedia (DDD).

---

## 2. Banco de dados: PostgreSQL

**Contexto:** o domínio da oficina é fortemente relacional — Cliente → Veículo → Ordem de Serviço → Peças/Serviços, com múltiplas dependências que precisam de integridade referencial (ex.: não se pode excluir um Cliente com Veículos vinculados).

**Decisão:** PostgreSQL como banco relacional, acessado via Spring Data JPA/Hibernate.

**Justificativa:** um banco não-relacional (documento ou chave-valor) exigiria reimplementar na aplicação verificações que um banco relacional já oferece nativamente (chaves únicas, tipos numéricos exatos para valores monetários via `NUMERIC`). O PostgreSQL também é gratuito, amplamente documentado no ecossistema Spring Boot e é o banco relacional padrão de fato em produção Java — reduzindo custo de operação e curva de aprendizado da equipe.

**Alternativas consideradas:** MongoDB foi descartado por não se adequar bem a um domínio com tantas relações e integridade referencial forte; MySQL foi considerado equivalente em maturidade, mas o suporte a tipos numéricos precisos e o alinhamento com o restante da stack pesaram a favor do PostgreSQL.

---

## 3. Autenticação stateless com JWT

**Contexto:** a API precisa proteger rotas administrativas, mas também expor um endpoint público (acompanhamento de OS pelo cliente), sem guardar sessão no servidor.

**Decisão:** autenticação via JWT (`TokenService`), validado em cada requisição por um `SecurityFilter` (`OncePerRequestFilter`), sem uso de sessão HTTP (`SessionCreationPolicy.STATELESS`).

**Justificativa:** uma API stateless escala horizontalmente sem exigir sessão compartilhada entre instâncias (nenhum servidor precisa "lembrar" quem fez login). O token carrega o `username` como *subject*, é assinado com HMAC256 e expira em 2 horas — suficiente para uma sessão de trabalho administrativo sem exigir novo login a cada poucos minutos, mas curto o bastante para limitar o impacto de um token vazado.

**Alternativas consideradas:** sessão tradicional baseada em cookie foi descartada por acoplar o cliente a um servidor específico (ou exigir armazenamento de sessão compartilhado, como Redis) — complexidade desnecessária para o escopo do MVC.

---

## 4. Máquina de estados explícita para Ordem de Serviço

**Contexto:** a OS passa por status sequenciais (Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue), e transições fora dessa ordem (ex.: pular de Recebida direto para Entregue) não podem ser permitidas.

**Decisão:** o enum `StatusOS` centraliza a tabela de transições válidas (`proximosStatusValidos()`), e a entidade `OrdemServico.transicionarPara(novoStatus)` é o único ponto que aplica essa regra.

**Justificativa:** inicialmente o projeto tinha métodos individuais por transição (`aprovar()`, `iniciarExecucao()`, `concluir()`...), cada um com sua própria checagem de status via `if` — duas fontes de verdade para a mesma regra, que podiam divergir (e de fato divergiam: um permitia cancelar em um estado que o outro não permitia). Consolidar numa tabela única no enum elimina essa duplicação: qualquer transição nova passa a exigir só uma entrada na tabela, não um método novo com sua própria validação.

**Alternativas consideradas:** manter os métodos individuais foi descartado justamente por ter causado a inconsistência observada durante o desenvolvimento — ver item correspondente no histórico de correções do projeto.

---

## 5. Agregados com snapshot desnormalizado (`ServicoOS`/`PecaOS`)

**Contexto:** quando um Serviço ou Peça do catálogo é adicionado a uma OS, e o preço desse item muda depois (ou o item é removido do catálogo), o valor já orçado na OS não pode mudar retroativamente.

**Decisão:** `OrdemServico` não guarda uma referência viva a `Servico`/`Peca` — ela guarda uma cópia dos dados relevantes no momento da adição (`ServicoOS`, `PecaOS`: nome, preço unitário, valor total), tirados via `adicionarServico`/`adicionarPeca`.

**Justificativa:** isso é o padrão de agregado do DDD — a OS é o agregado raiz e delimita sua própria consistência; itens de catálogo (Serviço, Peça) evoluem independentemente. Uma consequência direta e testada dessa decisão: um Serviço ou Peça pode ser excluído do catálogo mesmo depois de ter sido usado numa OS antiga, sem corromper o histórico (diferente de Cliente/Veículo, que são referenciados por ID vivo e por isso têm exclusão bloqueada quando há dependência).

**Alternativas consideradas:** referenciar `Servico`/`Peca` por ID vivo (como é feito com `Cliente`/`Veículo`) foi descartado porque exigiria também bloquear a exclusão de qualquer item de catálogo já usado em qualquer OS — mesmo orçamentos de anos atrás —, o que não reflete o comportamento esperado de um catálogo de preços que muda com o tempo.

---

## 6. Tratamento de erro centralizado com exceções tipadas

**Contexto:** a API precisa devolver o status HTTP correto (400/401/404/409) e uma mensagem consistente para cada tipo de falha, sem duplicar esse mapeamento em cada controller.

**Decisão:** exceções de negócio tipadas (`ResourceNotFoundException`, `ResourceAlreadyExistsException`, `ResourceInUseException`, `InvalidCredentialsException`) lançadas pelos *services*, capturadas centralmente por um único `@ControllerAdvice` (`GlobalExceptionHandler`) que as traduz para o status HTTP e formato de resposta corretos.

**Justificativa:** os controllers ficam livres de `try/catch` — cada um lança a exceção certa e confia que o handler global cuida da tradução para HTTP. Isso também corrigiu um bug real do projeto: o `AuthController` originalmente capturava `RuntimeException` genérica no próprio controller e retornava 403 com corpo vazio, escondendo até erros internos não relacionados a credenciais inválidas como se fossem falha de login.

**Alternativas consideradas:** tratar cada exceção manualmente em cada controller (`try/catch` local) foi descartado por gerar exatamente o tipo de duplicação e inconsistência encontrada e corrigida durante o projeto (mesma falha de "não encontrado" sendo tratada como 400 num service e 404 em outro, antes da padronização).

---

## 7. DTOs como Java Records, separados por recurso

**Contexto:** a API precisa validar entrada (Bean Validation) e documentar contratos (Swagger) sem vazar a entidade de domínio para a camada HTTP.

**Decisão:** cada recurso tem seus próprios DTOs de entrada/saída como `record` (ex.: `ClienteCreateRequest`, `ClienteUpdateRequest`, `ClienteResponse`), organizados em `adapters/in/web/DTO/<Recurso>/`, convertidos de/para o domínio por uma classe *mapper* dedicada.

**Justificativa:** records são imutáveis por padrão e eliminam boilerplate de getters/setters/construtor para objetos que só carregam dados. Separar `CreateRequest` de `UpdateRequest` (em vez de reutilizar um DTO genérico) deixa explícito quando as regras de criação e atualização divergem (ex.: `PecaUpdateRequest` não expõe `quantidadeEstoque` de propósito, porque esse campo só pode mudar pelos endpoints dedicados de estoque).

**Alternativas consideradas:** usar a própria entidade de domínio como corpo de request/response foi descartado desde o início do projeto (boa prática já presente na arquitetura hexagonal) — acoplaria o contrato HTTP a mudanças internas do domínio.

---

## 8. Integridade referencial garantida na camada de aplicação, não em FK de banco

**Contexto:** excluir um Cliente que tem Veículos, ou um Veículo com Ordens de Serviço, deixaria registros órfãos se não houvesse alguma checagem.

**Decisão:** a checagem é feita explicitamente nos *services* (`ClienteService.excluirCliente`, `VeiculoService.excluirVeiculo`, `PecaService.excluirPeca`), lançando `ResourceInUseException` (409) quando há dependência — em vez de depender de uma constraint `FOREIGN KEY` no banco.

**Justificativa:** uma constraint de FK devolveria uma `DataIntegrityViolationException` genérica do banco, sem contexto de negócio (qual dependência impediu a exclusão). Fazer a checagem explícita na aplicação permite uma mensagem de erro específica ("Cliente possui veículos vinculados e não pode ser excluído") e mantém a regra de negócio visível no código, não escondida em DDL.

**Alternativas consideradas:** usar `ON DELETE RESTRICT` nas colunas de FK do schema foi considerado, mas rejeitado por gerar mensagens de erro pouco úteis para quem consome a API e por depender de o Hibernate gerar o schema com essa constraint exatamente como esperado (o projeto usa `ddl-auto=update`, que não é garantidamente idempotente para esse tipo de alteração).

---

## 9. Ponto de baixa de estoque: início da execução, não a criação do orçamento

**Contexto:** peças entram no orçamento de uma OS antes de o cliente aprovar o serviço — se o estoque fosse baixado nesse momento, um orçamento recusado deixaria estoque "reservado" incorretamente.

**Decisão:** o estoque só é baixado (`PecaUseCase.baixarEstoque`) quando a OS transiciona para `EM_EXECUCAO` (`OrdemServicoService.atualizarStatus`), não quando a peça é adicionada à OS (`RECEBIDA`/`EM_DIAGNOSTICO`).

**Justificativa:** entre "peça orçada" e "peça fisicamente usada" existem duas etapas que podem não se concretizar (diagnóstico pode não confirmar a necessidade, cliente pode recusar o orçamento). Baixar o estoque só quando a execução realmente começa evita descontar estoque de peças que nunca chegam a ser fisicamente retiradas.

**Alternativas consideradas:** baixar o estoque no momento em que a peça é adicionada à OS foi descartado pelo motivo acima; baixar apenas quando a OS é finalizada foi descartado porque, a essa altura, a peça já foi fisicamente usada há tempo — o estoque ficaria desatualizado durante toda a execução do serviço.

---

## 10. Estratégia de testes por camada

**Contexto:** o edital exige cobertura mínima de 80% nos domínios críticos, mas testar tudo com o mesmo tipo de teste seria lento e testaria a coisa errada em cada camada.

**Decisão:** testes unitários puros para domínio e mappers (sem contexto Spring), `@DataJpaTest` com H2 para os adaptadores de persistência (testando o mapeamento objeto-relacional de verdade), `@WebMvcTest` com o *use case* mockado para os controllers (testando roteamento, validação e serialização, não a regra de negócio), e testes unitários com Mockito para os *services* (testando a regra de negócio isolada do HTTP e do banco).

**Justificativa:** cada camada é testada no nível de abstração certo — um teste de controller não deveria falhar por causa de uma regra de negócio, e um teste de service não deveria depender de um banco H2 subindo. Essa separação também tornou os testes rápidos: a suíte completa (286 testes) roda em menos de 20 segundos.

**Alternativas consideradas:** testes de ponta a ponta (`@SpringBootTest` completo, com banco e segurança reais) para todos os cenários foram descartados como estratégia principal por serem lentos e por acoplar teste de uma camada a todas as outras — um teste assim ficaria vermelho por motivos alheios ao que está sendo verificado. Ainda assim, esse tipo de teste continua sendo útil de forma pontual (ver observação sobre teste de integração de segurança nas pendências do projeto).
