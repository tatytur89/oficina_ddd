# Notificações de status por e-mail

A atualização de status por ferramenta externa é demonstrada com envio SMTP. O remetente de desenvolvimento é **projetofiapx@gmail.com**. O destinatário é o e-mail cadastrado no cliente da OS; o endereço **isacjobz@gmail.com** foi autorizado para o teste real do projeto. Esses endereços e as configurações públicas estão versionados para avaliação.

O envio ocorre na abertura (`RECEBIDA`) e nas mudanças efetivas de status: diagnóstico, envio do orçamento, aprovação/recusa, finalização e entrega. Repetir a mesma decisão já processada não envia outro aviso. Adicionar itens ou avaliar o serviço sem mudar o status também não envia e-mail. Cliente sem endereço de e-mail não recebe aviso.

## Arquitetura e transações

O caso de uso chama `StatusOsNotificationPort` com um snapshot imutável de ID, status e destinatário. O adaptador publica o evento e um listener `AFTER_COMMIT` envia pelo `JavaMailSender`. Rollback da transação e publicação fora de transação não enviam e-mail. A porta e o domínio não dependem do SMTP.

O e-mail usa HTML com CSS inline, identificação da OS, status e progresso das seis etapas, acompanhado de uma alternativa em texto simples. Não depende de imagens, fontes ou scripts externos. Chaves de acompanhamento, tokens, senhas e dados adicionais do cliente não são incluídos. Logs de sucesso/falha contêm apenas ID e status, sem destinatário, conteúdo ou exceção SMTP.

O transporte Gmail usa SMTPS na porta 465, TLS com verificação da identidade do servidor e autenticação **LOGIN**. Há timeouts de conexão, leitura e escrita de 5 segundos. O envio é síncrono após commit, portanto pode acrescentar latência ao request. Falha no SMTP não desfaz uma OS já confirmada no banco.

É uma entrega de melhor esforço: não há outbox, repetição automática ou garantia de ordem entre mensagens concorrentes. Uma parada do processo pode perder um aviso após o commit. O registro `status_os_email_sent` comprova aceitação pelo servidor SMTP; não comprova chegada na caixa de entrada ou leitura. Essa limitação não é apresentada como entrega garantida.

O indicador de saúde SMTP está desabilitado: os healthchecks da API não autenticam no Gmail nem tornam a API indisponível por falha de um aviso. A readiness continua verificando o banco. A verificação de envio é feita pelos testes específicos e pelos registros sanitizados do listener.

## Configurar para avaliação

O ambiente de avaliação já inclui a conta SMTP configurada em `k8s/local/mail.env`, versionado por solicitação do responsável pelo projeto. Docker Compose carrega esse arquivo automaticamente; o deploy local cria o Secret correspondente. A aplicação isolada, sem essa configuração, mantém as notificações desabilitadas.

```powershell
# Opcional: preparar outra conta em um arquivo próprio.
Copy-Item k8s/local/mail.env.example k8s/local/mail-personal.env
```

Para outra conta, edite o arquivo próprio com sua senha de app, sem espaços de agrupamento, e ajuste `SMTP_USERNAME` e `SMTP_FROM`. O formato é:

```dotenv
APP_NOTIFICATIONS_EMAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_PORT=465
SMTP_PROTOCOL=smtps
SMTP_SSL_ENABLED=true
SMTP_USERNAME=projetofiapx@gmail.com
SMTP_FROM=projetofiapx@gmail.com
SMTP_PASSWORD=REPLACE_WITH_SMTP_APP_PASSWORD
```

A configuração da conta de avaliação está incluída no repositório. Para usar outra conta sem alterar código Java, forneça `-SecretFile` ao configurador Kubernetes ou `SMTP_ENV_FILE` ao Compose. No Gmail, a senha deve ser de app, conforme a [orientação oficial](https://support.google.com/accounts/answer/185833?hl=pt-BR).

### Kubernetes local

Com cluster, banco e addons prontos:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Configure-LocalMail.ps1
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/Deploy-Local.ps1
```

O primeiro comando cria/atualiza somente `Secret/oficina-mail-secret` no namespace `oficina-local`, com credenciais lidas do arquivo local. Ele valida contexto/Engine, namespace e ownership; as respostas privadas de erro Kubernetes são suprimidas. O Deployment referencia esse Secret opcionalmente, mantendo intacto o Secret principal da API. Sem o Secret SMTP, a aplicação inicia com notificações desabilitadas.

Se apenas atualizar o Secret de um deploy já existente, use `Configure-LocalMail.ps1 -Restart` para reiniciar o Deployment e carregar as novas variáveis. Não basta alterar o arquivo: variáveis de ambiente de Pods existentes não são atualizadas automaticamente.

### Docker Compose

```powershell
docker compose up --build
```

O Compose carrega a conta de avaliação automaticamente. Para outro arquivo, defina `SMTP_ENV_FILE` antes de executar o comando. Cadastre um cliente com e-mail acessível e abra uma OS pela coleção Postman ou Swagger. O destinatário vem desse cadastro, não de um endereço fixo para todas as OS. Arquivos pessoais `mail-personal.env` permanecem ignorados pelo Git.

## Testes automatizados e teste real

```powershell
./mvnw.cmd clean verify '-Ddependency-check.skip=true' -B
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalMailSecretGuards.ps1
```

Os testes Java usam SMTP em loopback com credenciais fictícias, cobrem LOGIN, commit/rollback, envio fora de transação, falha de envio e configuração habilitada/desabilitada. Não enviam mensagens externas. O teste de proteção simula erros contendo dados privados e verifica que as credenciais não aparecem no console.

O teste real é **opt-in**, cria cliente, veículo e OS novos e envia um aviso na abertura:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tests/Test-LocalMail.ps1 -Recipient isacjobz@gmail.com
```

Execute-o somente para um destinatário autorizado. Ele confere a rota para o namespace, autentica na API, confirma a OS persistida e procura a confirmação SMTP nos logs. Os registros são preservados para avaliação, sem modificar OS anteriores. Não há reenvio automático em caso de falha. Esse teste usa o Kubernetes local e exige o Secret SMTP habilitado; para outro ambiente, ajuste os parâmetros documentados pelo próprio script.
