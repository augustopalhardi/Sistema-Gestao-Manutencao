package com.augustopalhardi.manutencao.view;

import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.primefaces.event.SelectEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroLocalService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.service.NotificacaoService;
import com.augustopalhardi.manutencao.util.Mensagens;
import com.augustopalhardi.manutencao.util.Uploads;
import com.augustopalhardi.manutencao.util.ValidadorEmail;

@Named
@ViewScoped
public class SolicitacaoManutencaoPredialBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SolicitacaoManutencaoPredialBean.class);

	/**
	 * Pasta (relativa a /uploads/) onde o CadastroLocalBean grava as fotos
	 * dos locais. Mesma constante usada lá para montar a URL servida pelo
	 * ImageServlet.
	 */
	private static final String PASTA_FOTOS_LOCAIS = Uploads.LOCAIS;

	/** Máximo de sugestões devolvidas pela busca rápida por nome do local. */
	private static final int MAX_SUGESTOES = 15;

	// A conferência de e-mail agora vive em com.augustopalhardi.manutencao.util.ValidadorEmail, que
	// além do formato sabe sugerir correção de domínio digitado errado.

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterSolicitacaoManutencaoPredialService;

	@Inject
	private ManterCadastroLocalService manterCadastroLocalService;

	@Inject
	private NotificacaoService notificacaoService;

	// Dialog GLOBAL de imagem (/includes/visualizarImagemDialog.xhtml)
	@Inject
	private VisualizarImagemBean visualizarImagemBean;

	private SolicitacaoManutencaoPredial solicitacaoManutencaoPredial;
	private List<SolicitacaoManutencaoPredial> solicitacoesManutencaoPredial = new ArrayList<>();
	private List<CadastroLocal> locaisDisponiveis;
	private Long localSelecionadoId;
	private UploadedFile uploadedPhoto;
	private CadastroUsuario usuarioLogado;

	// ==========================================
	// Busca rápida e confirmação do local
	//
	// Diferente da tela de máquinas, aqui NÃO existe etapa de categoria:
	// CadastroLocal não tem um campo equivalente ao "tipo" do equipamento.
	// A busca rápida é por nome do local, e só.
	// ==========================================

	/** Texto digitado na busca rápida por nome do local. */
	private String termoBuscaLocal;

	/**
	 * Rótulo da sugestão -> id do local. Preenchido a cada consulta do
	 * autocomplete para não depender de um Converter de entidade.
	 */
	private Map<String, Long> mapaSugestoes = new LinkedHashMap<>();

	/**
	 * E-mail para onde vai a confirmação da solicitação.
	 *
	 * Os funcionários usam UMA conta compartilhada para abrir manutenções,
	 * então a confirmação iria sempre para a caixa dessa conta e nunca para
	 * quem de fato registrou o chamado. Administradores e técnicos têm conta
	 * individual, portanto para eles o e-mail da própria conta já está certo e
	 * o campo nem aparece na tela.
	 *
	 * Em branco, o comportamento antigo é mantido (e-mail da conta logada).
	 */
	private String emailConfirmacao;

	/**
	 * Aviso mostrado embaixo do campo de e-mail, conferido assim que a
	 * pessoa sai do campo — e não só na hora de salvar.
	 */
	private String avisoEmail;

	/** Classe CSS do aviso: "erro" (bloqueia) ou "alerta" (só avisa). */
	private String classeAvisoEmail;

	/**
	 * Endereço corrigido sugerido quando o domínio parece ter erro de digitação
	 * (ex.: digitou "@gmial.com", sugere "@gmail.com"). Null quando não há
	 * sugestão.
	 */
	private String sugestaoEmail;

	// Parametros recebidos via URL quando a solicitacao vem de uma manutencao
	// preventiva que identificou outro problema (ver f:viewParam na página)
	private Long localId;
	private Long origemPreventivaId;

	// Campos para o cancelamento
	private String motivoCancelamento;
	private SolicitacaoManutencaoPredial solicitacaoSelecionada;

	@PostConstruct
	public void inicializar() {
		try {
			// Obter usuário logado da sessão
			usuarioLogado = obterUsuarioDaSessao();

			if (usuarioLogado == null) {
				LOGGER.error("Usuário não está logado!");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Usuário não está logado."));
				return;
			}

			LOGGER.info("Usuário logado: {}", usuarioLogado.getNome());

			this.solicitacaoManutencaoPredial = new SolicitacaoManutencaoPredial();
			this.localSelecionadoId = null;
			this.motivoCancelamento = null;
			this.solicitacaoSelecionada = null;
			this.solicitacoesManutencaoPredial = manterSolicitacaoManutencaoPredialService.buscarTodos();
			// Buscar apenas locais ATIVOS para o select de solicitação
			this.locaisDisponiveis = manterCadastroLocalService.buscarAtivos();
			LOGGER.info("Bean inicializado com sucesso. Total de solicitações: {}",
					solicitacoesManutencaoPredial.size());
		} catch (Exception e) {
			LOGGER.error("Erro em inicializar(): ", e);
			this.locaisDisponiveis = new ArrayList<>();
			this.solicitacoesManutencaoPredial = new ArrayList<>();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao inicializar: " + e.getMessage(), null));
		}
	}

	private CadastroUsuario obterUsuarioDaSessao() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) context.getExternalContext().getSession(false);

		if (session != null) {
			return (CadastroUsuario) session.getAttribute("usuarioLogado");
		}
		return null;
	}

	// ==========================================================
	// BUSCA E CONFIRMAÇÃO DO LOCAL
	// ==========================================================

	/**
	 * Normaliza um texto para comparação: remove acentos, espaços das pontas e
	 * diferenças de maiúsculas/minúsculas. Assim "Refeitório" é encontrado
	 * digitando "refeitorio".
	 */
	private static String chaveComparacao(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcentos.toUpperCase(Locale.ROOT);
	}

	/**
	 * Rótulo do local no select: somente o nome. A área não entra aqui — ela
	 * aparece na ficha do painel de confirmação, logo abaixo, e no select só
	 * deixava a lista mais poluída.
	 */
	public String montarRotuloLocal(CadastroLocal local) {
		if (local == null) {
			return "";
		}

		if (local.getNome() == null || local.getNome().trim().isEmpty()) {
			return "Local #" + local.getId();
		}

		return local.getNome().trim();
	}

	/**
	 * Busca rápida SOMENTE por nome do local, conforme combinado — área e
	 * observações não entram na pesquisa nem no texto da sugestão.
	 */
	public List<String> completarLocal(String query) {
		List<String> sugestoes = new ArrayList<>();
		mapaSugestoes = new LinkedHashMap<>();

		if (locaisDisponiveis == null) {
			return sugestoes;
		}

		String termo = chaveComparacao(query);

		for (CadastroLocal local : locaisDisponiveis) {
			if (sugestoes.size() >= MAX_SUGESTOES) {
				break;
			}

			if (termo.isEmpty() || chaveComparacao(local.getNome()).contains(termo)) {
				String rotulo = (local.getNome() == null || local.getNome().trim().isEmpty())
						? ("Local #" + local.getId())
						: local.getNome().trim();

				// Garante rótulo único: dois locais com o mesmo nome
				// apontariam para a mesma entrada no mapa.
				if (mapaSugestoes.containsKey(rotulo)) {
					rotulo = rotulo + " #" + local.getId();
				}

				mapaSugestoes.put(rotulo, local.getId());
				sugestoes.add(rotulo);
			}
		}

		return sugestoes;
	}

	/** Aplica o local escolhido na busca rápida ao select do formulário. */
	public void selecionarLocalPorBusca(SelectEvent<?> event) {
		try {
			Object selecionado = event.getObject();

			if (selecionado == null) {
				return;
			}

			Long id = mapaSugestoes.get(String.valueOf(selecionado));

			if (id == null) {
				LOGGER.warn("Sugestão '{}' não encontrada no mapa de busca rápida", selecionado);
				Mensagens.aviso("Local não encontrado",
						"Não foi possível identificar o local escolhido. Selecione-o na lista.");
				return;
			}

			aplicarLocal(id);
			termoBuscaLocal = null;

		} catch (Exception e) {
			LOGGER.error("Erro ao selecionar local pela busca rápida: ", e);
			Mensagens.erro("Não foi possível selecionar o local pela busca. Selecione-o na lista.");
		}
	}

	/** Seleciona um local pelo id, se ele estiver entre os locais ativos. */
	private void aplicarLocal(Long id) {
		this.localSelecionadoId = id;

		if (getLocalSelecionado() == null) {
			LOGGER.warn("Local #{} não está entre os locais ativos", id);
			this.localSelecionadoId = null;
			Mensagens.aviso("Local desativado", "O local #" + id + " está desativado no cadastro e não pode"
					+ " receber solicitações. Reative-o em Locais ou escolha outro.");
			return;
		}

		LOGGER.info("Local #{} aplicado", id);
	}

	/** Listener do select de local — usado só para atualizar o painel. */
	public void aoTrocarLocal() {
		CadastroLocal local = getLocalSelecionado();
		LOGGER.info("Local selecionado: {}", local != null ? local.getNome() : "nenhum");
	}

	/** Local correspondente ao id escolhido, ou null. */
	public CadastroLocal getLocalSelecionado() {
		if (localSelecionadoId == null || locaisDisponiveis == null) {
			return null;
		}
		for (CadastroLocal local : locaisDisponiveis) {
			if (localSelecionadoId.equals(local.getId())) {
				return local;
			}
		}
		return null;
	}

	/** Usado pelo rendered do painel de confirmação. */
	public boolean isLocalEscolhido() {
		return getLocalSelecionado() != null;
	}

	/**
	 * Contrário de isLocalEscolhido(). Existe só para a página não precisar do
	 * operador "not" no EL — o validador do Eclipse reclama dele.
	 */
	public boolean isLocalNaoEscolhido() {
		return !isLocalEscolhido();
	}

	/**
	 * Área do local já formatada, ou "—" quando não informada.
	 *
	 * O campo é int primitivo na entidade, então nunca é nulo: um local sem
	 * área cadastrada chega aqui como 0, e mostrar "0 m²" confundiria.
	 */
	public String getAreaLocalSelecionado() {
		CadastroLocal local = getLocalSelecionado();

		if (local == null || local.getArea() <= 0) {
			return "—";
		}

		return local.getArea() + " m²";
	}

	/**
	 * URL da foto do local, no formato servido pelo ImageServlet. Devolve null
	 * quando o local não tem foto, para o painel exibir o placeholder no lugar
	 * da miniatura.
	 */
	public String getUrlFotoLocalSelecionado() {
		CadastroLocal local = getLocalSelecionado();

		if (local == null || local.getUrlFoto() == null || local.getUrlFoto().trim().isEmpty()) {
			return null;
		}

		String foto = local.getUrlFoto().trim();

		if (foto.startsWith("/")) {
			return foto;
		}

		return Uploads.URL_BASE + PASTA_FOTOS_LOCAIS + foto;
	}

	/**
	 * Abre o dialog GLOBAL de imagem com a foto e a ficha do local escolhido,
	 * para o solicitante conferir antes de registrar a solicitação.
	 */
	public void verDetalhesLocalSelecionado() {
		CadastroLocal local = getLocalSelecionado();

		if (local == null) {
			visualizarImagemBean.abrir("Detalhes do Local", null);
			visualizarImagemBean.setMensagemSemFoto("Selecione um local para ver os detalhes.");
			return;
		}

		// Sem foto, o caminho precisa ir NULO — concatenar a pasta com um
		// urlFoto nulo faria o dialog tentar exibir "locais/null" e mostrar
		// imagem quebrada em vez da mensagem de "sem foto".
		String caminhoFoto = null;

		if (local.getUrlFoto() != null && !local.getUrlFoto().trim().isEmpty()) {
			caminhoFoto = PASTA_FOTOS_LOCAIS + local.getUrlFoto().trim();
		}

		visualizarImagemBean.abrir("Detalhes do Local", local.getNome(), caminhoFoto);

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a este local.");
		visualizarImagemBean.addInfo("Nome", local.getNome());
		visualizarImagemBean.addInfo("Área", getAreaLocalSelecionado());
		visualizarImagemBean.addInfo("Código no sistema", local.getId());
		visualizarImagemBean.addInfo("Observações", local.getObservacoes());

		LOGGER.info("Detalhes do local #{} abertos para conferência", local.getId());
	}

	// ==========================================================
	// E-MAIL DE CONFIRMAÇÃO
	// ==========================================================

	/**
	 * O campo de e-mail só aparece para FUNCIONARIO, que é o perfil da conta
	 * compartilhada. Administrador e técnico têm conta individual: o e-mail da
	 * própria conta já é o certo, então mostrar o campo para eles só criaria
	 * mais um lugar para errar.
	 */
	public boolean isExibirCampoEmail() {
		return usuarioLogado != null && usuarioLogado.getTipoUsuario() == TipoUsuario.FUNCIONARIO;
	}

	/**
	 * Confere o e-mail assim que a pessoa sai do campo.
	 *
	 * São três situações diferentes, e só a primeira impede o salvamento:
	 *
	 * 1. Endereço malformado ("joao@", "joao.com") → ERRO.
	 * 2. Domínio parecido com um conhecido ("@gmial.com") → ALERTA com sugestão
	 *    de correção em um clique. É o caso que motivou tudo isso: o endereço é
	 *    válido na forma, então nenhuma regex pegaria, e o e-mail saía do
	 *    sistema para nunca chegar em ninguém.
	 * 3. Domínio desconhecido e sem parecido → ALERTA apenas, para a pessoa
	 *    reler. Não bloqueia: pode ser um domínio corporativo legítimo que a
	 *    lista não cobre.
	 */
	public void validarEmailInformado() {
		avisoEmail = null;
		classeAvisoEmail = null;
		sugestaoEmail = null;

		if (emailConfirmacao == null || emailConfirmacao.trim().isEmpty()) {
			return;
		}

		String email = emailConfirmacao.trim();

		if (!ValidadorEmail.formatoValido(email)) {
			avisoEmail = "Este e-mail não parece válido. Confira se não faltou o @ ou o final do endereço.";
			classeAvisoEmail = "erro";
			return;
		}

		String sugestao = ValidadorEmail.sugerirCorrecao(email);

		if (sugestao != null) {
			sugestaoEmail = sugestao;
			avisoEmail = "Você quis dizer " + sugestao + "?";
			classeAvisoEmail = "alerta";
			LOGGER.info("Possível erro de digitação no e-mail '{}'. Sugerido: {}", email, sugestao);
			return;
		}

		if (!ValidadorEmail.dominioConhecido(email)) {
			avisoEmail = "Não reconhecemos este domínio de e-mail. Confira se está escrito corretamente.";
			classeAvisoEmail = "alerta";
		}
	}

	/** Aplica no campo a correção sugerida. */
	public void aplicarSugestaoEmail() {
		if (sugestaoEmail != null) {
			LOGGER.info("Usuário aceitou a correção de e-mail: {} -> {}", emailConfirmacao, sugestaoEmail);
			emailConfirmacao = sugestaoEmail;
		}
		validarEmailInformado();
	}

	/** Há aviso a exibir embaixo do campo de e-mail? */
	public boolean isPossuiAvisoEmail() {
		return avisoEmail != null && !avisoEmail.isEmpty();
	}

	/** Há uma correção pronta para aplicar em um clique? */
	public boolean isPossuiSugestaoEmail() {
		return sugestaoEmail != null && !sugestaoEmail.isEmpty();
	}

	/**
	 * Define para quem vai a confirmação: o e-mail digitado na tela quando há
	 * um, senão o da conta logada (comportamento anterior). Devolve null quando
	 * não há e-mail algum — aí só os administradores são notificados.
	 */
	private String resolverEmailSolicitante() {
		if (isExibirCampoEmail() && emailConfirmacao != null && !emailConfirmacao.trim().isEmpty()) {
			LOGGER.info("Confirmação será enviada ao e-mail informado na tela: {}", emailConfirmacao.trim());
			return emailConfirmacao.trim();
		}

		if (usuarioLogado != null && usuarioLogado.getEmail() != null
				&& !usuarioLogado.getEmail().trim().isEmpty()) {
			LOGGER.info("Confirmação será enviada ao e-mail da conta logada: {}", usuarioLogado.getEmail().trim());
			return usuarioLogado.getEmail().trim();
		}

		LOGGER.warn("Nenhum e-mail disponível para o solicitante. "
				+ "A confirmação não será enviada (os administradores continuam sendo notificados).");
		return null;
	}

	/**
	 * Chamado via <f:event type="preRenderView"> após os view params (localId e
	 * origemPreventivaId) serem processados. Pré-preenche o formulário quando a
	 * solicitação vem de uma manutenção preventiva que identificou outro
	 * problema no local. Só roda em GET (não em postback), para não sobrescrever
	 * o que o técnico já tiver digitado.
	 */
	public void aplicarPreenchimentoAutomatico() {
		if (FacesContext.getCurrentInstance().isPostback()) {
			return;
		}

		if (localId == null) {
			return;
		}

		try {
			// Passa pelo aplicarLocal para o painel de confirmação
			// já abrir preenchido com a ficha do local vindo da URL.
			aplicarLocal(localId);

			this.solicitacaoManutencaoPredial.setTipoManutencao(TipoManutencao.CORRETIVA);
			this.solicitacaoManutencaoPredial.setDataManutencao(new Date());
			this.solicitacaoManutencaoPredial
					.setSolicitante(usuarioLogado != null ? usuarioLogado.getNome() : "");

			if (origemPreventivaId != null) {
				this.solicitacaoManutencaoPredial.setObservacoes(
						"Esta manutenção foi aberta devido a uma falha encontrada durante a manutenção preventiva predial #"
								+ origemPreventivaId);
			}

			LOGGER.info("Formulario pre-preenchido a partir da manutencao preventiva #{} (local #{})",
					origemPreventivaId, localId);
		} catch (Exception e) {
			LOGGER.error("Erro ao pre-preencher solicitacao a partir da preventiva: ", e);
			Mensagens.aviso("Preenchimento automático falhou",
					"Não foi possível preencher os dados da preventiva. Preencha o formulário manualmente.");
		}
	}

	public void salvar() {
		try {
			LOGGER.info("=== INICIANDO SALVAMENTO DE SOLICITAÇÃO DE MANUTENÇÃO PREDIAL ===");

			// Validações
			if (localSelecionadoId == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Selecione um local."));
				return;
			}

			if (solicitacaoManutencaoPredial.getSolicitante() == null
					|| solicitacaoManutencaoPredial.getSolicitante().trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Informe o nome do solicitante (quem teve o problema)."));
				return;
			}

			if (usuarioLogado == null) {
				LOGGER.error("Usuário não está logado!");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Usuário não está logado."));
				return;
			}

			// Risco marcado exige descrição.
			//
			// Não dá para usar required="#{...riscoSeguranca}" no campo: o
			// atributo required é avaliado na fase de validação, ANTES de o
			// valor do checkbox chegar ao bean, então ele leria sempre o valor
			// antigo. Por isso a conferência é feita aqui.
			if (solicitacaoManutencaoPredial.isRiscoSeguranca()
					&& (solicitacaoManutencaoPredial.getDescrRisco() == null
							|| solicitacaoManutencaoPredial.getDescrRisco().trim().isEmpty())) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Você marcou que há risco de segurança. Descreva qual é o risco."));
				return;
			}

			// E-mail informado precisa ser um e-mail válido
			// Só o formato bloqueia. Domínio suspeito vira aviso na tela (ver
			// validarEmailInformado): barrar por domínio seria arriscado, já que
			// a lista de domínios conhecidos nunca cobre todos os legítimos.
			if (isExibirCampoEmail() && emailConfirmacao != null && !emailConfirmacao.trim().isEmpty()
					&& !ValidadorEmail.formatoValido(emailConfirmacao)) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"O e-mail para confirmação parece inválido. Confira o endereço digitado."));
				return;
			}

			// Validação: Funcionários só podem lançar manutenções corretivas
			if (usuarioLogado.getTipoUsuario() == TipoUsuario.FUNCIONARIO
					&& solicitacaoManutencaoPredial.getTipoManutencao() != null
					&& solicitacaoManutencaoPredial.getTipoManutencao() != TipoManutencao.CORRETIVA) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Funcionários só podem registrar manutenções corretivas."));
				return;
			}

			// Preencher automaticamente o usuário que registrou
			solicitacaoManutencaoPredial.setUsuarioRegistro(usuarioLogado.getNome());
			LOGGER.info("Usuário que registrou: {}", usuarioLogado.getNome());
			LOGGER.info("Solicitante (quem teve o problema): {}", solicitacaoManutencaoPredial.getSolicitante());

			// Buscar local selecionado
			CadastroLocal local = manterCadastroLocalService.buscarPorId(localSelecionadoId);
			if (local != null) {
				solicitacaoManutencaoPredial.setLocal(local);
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Local selecionado não encontrado."));
				return;
			}

			// UPLOAD DE FOTO (se houver)
			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().isEmpty()) {
				try {
					String caminhoWeb = Uploads.url(Uploads.SOLICITACOES_PREDIAL) + Uploads.salvar(uploadedPhoto, Uploads.SOLICITACOES_PREDIAL);
					solicitacaoManutencaoPredial.setUrlFoto(caminhoWeb);
					LOGGER.info("URL salva no banco: {}", caminhoWeb);

				} catch (IOException e) {
					LOGGER.error("Erro ao fazer upload da foto", e);
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
							"Atenção", "Erro ao salvar foto, mas solicitação será cadastrada"));
				}
			}

			// Salvar solicitação
			SolicitacaoManutencaoPredial novaSolicitacao = manterSolicitacaoManutencaoPredialService
					.salvar(solicitacaoManutencaoPredial);

			LOGGER.info("Solicitação salva com sucesso - ID: {}", novaSolicitacao.getId());

			String nomeLocal = local != null && local.getNome() != null ? local.getNome() : "Não informado";

			String solicitante = novaSolicitacao.getSolicitante() != null ? novaSolicitacao.getSolicitante()
					: "Não informado";

			String descricao = novaSolicitacao.getProblema() != null && !novaSolicitacao.getProblema().trim().isEmpty()
					? novaSolicitacao.getProblema()
					: "Sem descrição";

			// ========================================
			// OBTER EMAIL DO SOLICITANTE
			//
			// Prioriza o e-mail digitado na tela (conta compartilhada dos
			// funcionários) e cai para o e-mail da conta logada quando não
			// houver nada informado.
			// ========================================
			String emailSolicitante = resolverEmailSolicitante();

			boolean administradoresAvisados = false;
			try {
				administradoresAvisados = notificacaoService.notificarNovaManutencao("Manutenção Predial", descricao, nomeLocal,
						solicitante, emailSolicitante);

				LOGGER.info("Notificações agendadas (administradores avisados: {})", administradoresAvisados);

			} catch (Exception e) {
				LOGGER.error("Erro ao enviar notificações por email (processo principal não foi afetado)", e);
			}

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso!",
					"Solicitação de manutenção predial cadastrada com sucesso!"));

			if (!administradoresAvisados) {
				Mensagens.aviso("E-mail não enviado", "A solicitação foi registrada, mas nenhum administrador foi"
						+ " avisado por e-mail. Verifique a Configuração de E-mail.");
			}

			LOGGER.info("Solicitação cadastrada - Solicitante: {}, Registrado por: {}", solicitante,
					usuarioLogado.getNome());

			limpar();

			solicitacoesManutencaoPredial = manterSolicitacaoManutencaoPredialService.buscarTodos();

			LOGGER.info("=== SALVAMENTO CONCLUÍDO COM SUCESSO ===");

		} catch (Exception e) {
			LOGGER.error("Erro ao cadastrar solicitação de manutenção predial", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao cadastrar solicitação de manutenção predial: " + e.getMessage()));
		}
	}

	public void prepararCancelamento(SolicitacaoManutencaoPredial solicitacao) {
		LOGGER.info("Preparando cancelamento para solicitacao ID: {}", solicitacao.getId());
		this.solicitacaoSelecionada = solicitacao;
		this.motivoCancelamento = "";
	}

	public void confirmarCancelamento() {
		try {
			LOGGER.info("Iniciando confirmação de cancelamento");

			if (motivoCancelamento == null || motivoCancelamento.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage("dialogForm:motivoCancel", new FacesMessage(
						FacesMessage.SEVERITY_ERROR, "Erro", "O motivo do cancelamento é obrigatório."));
				LOGGER.warn("Tentativa de cancelamento sem motivo");
				return;
			}

			if (solicitacaoSelecionada == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma solicitação selecionada."));
				LOGGER.error("solicitacaoSelecionada é null");
				return;
			}

			LOGGER.info("Cancelando solicitação ID: {}", solicitacaoSelecionada.getId());

			SolicitacaoManutencaoPredial solicitacaoAtualizada = manterSolicitacaoManutencaoPredialService
					.buscarPorId(solicitacaoSelecionada.getId());

			if (solicitacaoAtualizada == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Solicitação não encontrada no banco."));
				LOGGER.error("Solicitação não encontrada no banco. ID: {}", solicitacaoSelecionada.getId());
				return;
			}

			solicitacaoAtualizada.setCancelada(true);
			solicitacaoAtualizada.setMotivoCancelamento(motivoCancelamento.trim());
			solicitacaoAtualizada.setDataCancelamento(new Date());

			LOGGER.info("Salvando solicitação cancelada. Motivo: {}", motivoCancelamento);

			manterSolicitacaoManutencaoPredialService.salvar(solicitacaoAtualizada);

			solicitacoesManutencaoPredial = manterSolicitacaoManutencaoPredialService.buscarTodos();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Solicitação #" + solicitacaoAtualizada.getId() + " cancelada com sucesso!"));

			LOGGER.info("Solicitação cancelada com sucesso. ID: {}", solicitacaoAtualizada.getId());

			motivoCancelamento = "";
			solicitacaoSelecionada = null;

		} catch (Exception e) {
			LOGGER.error("Erro ao cancelar solicitação", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao cancelar solicitação", "Erro: " + e.getMessage()));
		}
	}

	public void limpar() {
		solicitacaoManutencaoPredial = new SolicitacaoManutencaoPredial();
		localSelecionadoId = null;
		uploadedPhoto = null;
		motivoCancelamento = "";
		solicitacaoSelecionada = null;

		// Volta a busca rápida e o e-mail ao estado inicial
		termoBuscaLocal = null;
		emailConfirmacao = null;
		avisoEmail = null;
		classeAvisoEmail = null;
		sugestaoEmail = null;
		mapaSugestoes = new LinkedHashMap<>();

		LOGGER.info("Campos de solicitação limpos");
	}

	public void selecionarParaDetalhes(SolicitacaoManutencaoPredial solicitacao) {
		this.solicitacaoSelecionada = solicitacao;
	}

	// ========================================
	// GETTERS E SETTERS
	// ========================================

	public SolicitacaoManutencaoPredial getSolicitacaoManutencaoPredial() {
		return solicitacaoManutencaoPredial;
	}

	public void setSolicitacaoManutencaoPredial(SolicitacaoManutencaoPredial solicitacaoManutencaoPredial) {
		this.solicitacaoManutencaoPredial = solicitacaoManutencaoPredial;
	}

	public List<SolicitacaoManutencaoPredial> getSolicitacoesManutencaoPredial() {
		return solicitacoesManutencaoPredial;
	}

	public void setSolicitacoesManutencaoPredial(List<SolicitacaoManutencaoPredial> solicitacoesManutencaoPredial) {
		this.solicitacoesManutencaoPredial = solicitacoesManutencaoPredial;
	}

	public List<CadastroLocal> getLocaisDisponiveis() {
		return locaisDisponiveis;
	}

	public void setLocaisDisponiveis(List<CadastroLocal> locaisDisponiveis) {
		this.locaisDisponiveis = locaisDisponiveis;
	}

	// GETTERS/SETTERS

	public String getTermoBuscaLocal() {
		return termoBuscaLocal;
	}

	public void setTermoBuscaLocal(String termoBuscaLocal) {
		this.termoBuscaLocal = termoBuscaLocal;
	}

	public String getEmailConfirmacao() {
		return emailConfirmacao;
	}

	public void setEmailConfirmacao(String emailConfirmacao) {
		this.emailConfirmacao = emailConfirmacao;
	}

	public String getAvisoEmail() {
		return avisoEmail;
	}

	public String getClasseAvisoEmail() {
		return classeAvisoEmail;
	}

	public String getSugestaoEmail() {
		return sugestaoEmail;
	}

	public Long getLocalSelecionadoId() {
		return localSelecionadoId;
	}

	public void setLocalSelecionadoId(Long localSelecionadoId) {
		this.localSelecionadoId = localSelecionadoId;
	}

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getOrigemPreventivaId() {
		return origemPreventivaId;
	}

	public void setOrigemPreventivaId(Long origemPreventivaId) {
		this.origemPreventivaId = origemPreventivaId;
	}

	public UploadedFile getUploadedPhoto() {
		return uploadedPhoto;
	}

	public void setUploadedPhoto(UploadedFile uploadedPhoto) {
		this.uploadedPhoto = uploadedPhoto;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public SolicitacaoManutencaoPredial getSolicitacaoSelecionada() {
		return solicitacaoSelecionada;
	}

	public void setSolicitacaoSelecionada(SolicitacaoManutencaoPredial solicitacaoSelecionada) {
		this.solicitacaoSelecionada = solicitacaoSelecionada;
	}

	public CadastroUsuario getUsuarioLogado() {
		return usuarioLogado;
	}
}