<p align="center">
  <img src="docs/logo.svg" alt="Gestão de Manutenção" width="96" />
</p>

<h1 align="center">Gestão de Manutenção</h1>

<p align="center">
  Sistema web para gestão do ciclo completo de manutenção industrial e predial:<br/>
  abertura de chamados, execução pelos técnicos, compras e indicadores de desempenho.
</p>

<p align="center">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-orange" />
  <img alt="JSF 2.3" src="https://img.shields.io/badge/JSF-2.3-blue" />
  <img alt="PrimeFaces 12" src="https://img.shields.io/badge/PrimeFaces-12-blue" />
  <img alt="Hibernate 5.6" src="https://img.shields.io/badge/Hibernate-5.6-59666C" />
  <img alt="MySQL 8" src="https://img.shields.io/badge/MySQL-8-4479A1" />
  <img alt="Licença MIT" src="https://img.shields.io/badge/licen%C3%A7a-MIT-green" />
</p>

---

## Sobre

Desenvolvido como projeto freelance para uma empresa do setor industrial, onde está **em uso em produção**. Esta é a versão pública do código, sem dados, marca ou infraestrutura do cliente.

**O problema:** o controle de manutenções era feito em planilhas e comunicação informal. Não havia rastreabilidade do que foi feito, quanto tempo levou e quanto custou, nem base para decidir entre reparar ou substituir um equipamento.

**A solução:** um sistema que centraliza o fluxo, do chamado ao fechamento, e transforma o histórico operacional em indicadores.

Em números: cerca de 21 mil linhas de Java em 67 classes e 27 telas JSF.

---

## Funcionalidades

### Solicitações de manutenção
- Chamados para **máquinas/equipamentos** ou **manutenção predial**
- Foto do problema anexada na abertura
- Classificação em **corretiva**, **preventiva** e **preditiva**
- Controle de status durante a execução (a fazer, em andamento, aguardando compra, concluída, cancelada)
- Apontamento de períodos de trabalho, com cálculo de horas por manutenção

### Compras de materiais e serviços
Quando uma manutenção depende de material ou serviço externo, o técnico abre uma solicitação de compra e a manutenção fica **travada automaticamente**. O administrador processa a compra (informando o valor, separado entre material e serviço) ou a cancela, e o sistema **devolve a manutenção** ao técnico. O custo entra direto nos relatórios.

### Cronograma de preventivas
Programação das preventivas por equipamento, com histórico de reprogramações.

### Indicadores e relatórios
- **MTTR** (tempo médio para reparo) e **MTBF** (tempo médio entre falhas)
- **Confiabilidade** por máquina e por categoria (%)
- **Custo** por máquina, por categoria e total do período
- Tempo total e tempo médio por reparo
- Visualização alternável entre gráfico e lista

### Notificações por e-mail
Envio automático para administradores e para o solicitante a cada evento relevante, com destinatários configuráveis pela própria interface.

### Cadastros e perfis de acesso
- Usuários, locais e máquinas/equipamentos, com foto e ativação/desativação
- Três perfis com permissões distintas: **Administrador**, **Técnico** e **Funcionário**
- Valor-hora por técnico, diferenciado entre manutenção de máquina e predial

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 17 |
| Web / View | JSF 2.3 (Mojarra) + PrimeFaces 12 |
| Injeção de dependências | CDI 2.0 (Weld 3.1) |
| Persistência | JPA 2.2 + Hibernate 5.6 |
| Pool de conexões | c3p0 |
| Banco de dados | MySQL 8 |
| Segurança | jBCrypt |
| E-mail | JavaMail |
| Build | Maven (`.war`) |
| Servidor | Apache Tomcat 9 |
| Log | SLF4J + Log4j |

---

## Arquitetura

Arquitetura em camadas, com injeção de dependências via CDI:

```
src/main/java/com/augustopalhardi/manutencao/
├── model/       Entidades JPA e enums
├── dao/         Acesso a dados (consultas JPQL)
├── service/     Regras de negócio e notificações
├── view/        Managed beans do JSF (um por tela)
├── filter/      Autenticação e autorização por perfil
├── listener/    Inicialização da aplicação (fuso horário)
├── servlet/     Entrega das fotos enviadas pelos usuários
└── util/        Configuração, uploads, validação de e-mail, mensagens
    └── jpa/     Produtor de EntityManager e interceptor transacional

src/main/webapp/
├── home/                    Painel inicial de cada perfil
├── solicitacaoManutencao/   Abertura, listagem e execução dos chamados
├── compras/                 Solicitações de compra
├── cronograma/              Cronograma de preventivas
├── relatorios/              Dashboard e relatório de indicadores
├── maquinaEquipamento/, local/, usuario/, tecnico/   Cadastros e consultas
├── conta/, email/           Minha conta e configuração de notificações
├── error/                   Páginas 404 e 500
└── resources/               CSS e imagens
```

### Decisões técnicas

**Controle transacional declarativo.** Um interceptor CDI (`@Transactional`) abre e fecha a transação em volta dos métodos de serviço, sem `begin`/`commit`/`rollback` repetidos em cada operação.

**Credenciais fora do código.** Nenhuma senha de banco ou de SMTP existe no repositório. A classe `Config` lê um `config.properties` externo, cujo caminho é definido na inicialização da JVM. O mesmo `.war` roda em qualquer ambiente sem recompilar.

**E-mail assíncrono e em lote.** A primeira versão abria uma conexão SMTP por destinatário dentro da thread da requisição: quatro destinatários, quatro handshakes TLS, com o usuário esperando. Hoje a requisição só monta as mensagens e retorna; o envio acontece em segundo plano reaproveitando uma única conexão, e cada destinatário recebe sua própria mensagem.

**Autorização por allow-list.** O filtro de autenticação confere cada URI contra a lista de páginas permitidas para o perfil da sessão. O padrão é negar.

**Uploads fora da aplicação.** As fotos ficam em um diretório externo (`uploads.dir`) e são servidas por um servlet que exige sessão autenticada. Um novo deploy não apaga os arquivos. Gravação, nomes seguros e exclusão ficam concentrados na classe `Uploads`.

**Senhas com BCrypt.** As senhas são gravadas como hash BCrypt e conferidas com `BCrypt.checkpw`, tanto no login quanto na troca de senha.

**Identificação padronizada de equipamentos.** Havia oito formatos diferentes de exibir uma máquina espalhados pelo sistema. Foram unificados em um único método na entidade, usado em telas, e-mails e relatórios.

**Validação de e-mail com sugestão de correção.** Detecta erros comuns de digitação no domínio (`gmial.com`, `hotmial.com`, `.con`) por distância de edição e sugere a correção, sem "corrigir" domínios corporativos legítimos.

---

## Como executar

**Pré-requisitos:** JDK 17, Maven 3.8+, MySQL 8 e Apache Tomcat 9.

**1. Banco de dados.** Crie um banco vazio. As tabelas são criadas pelo Hibernate no primeiro start (`hbm2ddl.auto=update`).

```sql
CREATE DATABASE gestao_manutencao CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**2. Configuração.** Copie [`config.properties.example`](config.properties.example) para um local **fora do projeto**, renomeie para `config.properties` e preencha banco, SMTP e, se quiser, a pasta de uploads.

**3. Aponte a JVM do Tomcat para o arquivo:**

```
-Dconfig.file=/caminho/para/config.properties
```

No Eclipse: *Servers → Tomcat → Open launch configuration → Arguments → VM arguments*. Sem o parâmetro, o caminho padrão é `/etc/manutencao/config.properties`.

**4. Build e deploy:**

```bash
mvn clean package
```

Copie `target/gestao-manutencao.war` para a pasta `webapps/` do Tomcat. A aplicação responde em `http://localhost:8080/gestao-manutencao`.

**5. Primeiro acesso.** Com o banco vazio, a tela de login mostra o botão **CRIAR CONTA**, que cadastra o primeiro usuário.

---

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

## Autor

**Augusto de Barros Palhardi**
Desenvolvedor Java · [github.com/augustopalhardi](https://github.com/augustopalhardi)
