package com.augustopalhardi.manutencao.view;

import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroMaquinaEquipamentoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.NotificacaoService;
import com.augustopalhardi.manutencao.util.Uploads;
import com.augustopalhardi.manutencao.util.ValidadorEmail;

@Named
@ViewScoped
public class SolicitacaoManutencaoMaquinaBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SolicitacaoManutencaoMaquinaBean.class);

	/**
	 * Pasta (relativa a /uploads/) onde o CadastroMaquinaEquipamentoBean
	 * grava as fotos dos equipamentos. Mesma constante usada lá para montar a
	 * URL servida pelo ImageServlet.
	 */
	private static final String PASTA_FOTOS_MAQUINAS = Uploads.MAQUINAS;

	/** Rótulo da categoria usada para máquinas cadastradas sem "tipo". */
	private static final String SEM_CATEGORIA = "Sem categoria";

	/** Máximo de sugestões devolvidas pela busca rápida por patrimônio. */
	private static final int MAX_SUGESTOES = 15;

	// A conferência de e-mail agora vive em com.augustopalhardi.manutencao.util.ValidadorEmail, que
	// além do formato sabe sugerir correção de domínio digitado errado.

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterSolicitacaoManutencaoMaquinaService;

	@Inject
	private ManterCadastroMaquinaEquipamentoService manterCadastroMaquinaEquipamentoService;

	@Inject
	private NotificacaoService notificacaoService;

	// Dialog GLOBAL de imagem (/includes/visualizarImagemDialog.xhtml)
	@Inject
	private VisualizarImagemBean visualizarImagemBean;

	private SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina;
	private List<SolicitacaoManutencaoMaquina> solicitacoesManutencaoMaquina = new ArrayList<>();
	private List<CadastroMaquinaEquipamento> maquinasDisponiveis;
	private Long maquinaSelecionadaId;
	private UploadedFile uploadedPhoto;
	private CadastroUsuario usuarioLogado;

	// ==========================================
	// Seleção em duas etapas (categoria -> máquina)
	// ==========================================

	/** Categoria escolhida no primeiro select. É o "tipo" do equipamento. */
	private String categoriaSelecionada;

	/** Categorias distintas montadas a partir do tipo das máquinas ativas. */
	private List<String> categoriasDisponiveis = new ArrayList<>();

	/** Máquinas exibidas no segundo select, já filtradas pela categoria. */
	private List<CadastroMaquinaEquipamento> maquinasFiltradas = new ArrayList<>();

	/** Texto digitado na busca rápida por patrimônio/fabricante/modelo. */
	private String termoBuscaMaquina;

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

	/**
	 * Rótulo da sugestão -> id da máquina. Preenchido a cada consulta do
	 * autocomplete para não depender de um Converter de entidade.
	 */
	private Map<String, Long> mapaSugestoes = new LinkedHashMap<>();

	// Parametros recebidos via URL quando a solicitacao vem de uma manutencao
	// preventiva que identificou outro problema (ver f:viewParam na página)
	private Long maquinaId;
	private Long origemPreventivaId;

	// Campos para o cancelamento
	private String motivoCancelamento;
	private SolicitacaoManutencaoMaquina solicitacaoSelecionada;

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

			this.solicitacaoManutencaoMaquina = new SolicitacaoManutencaoMaquina();
			this.maquinaSelecionadaId = null;
			this.motivoCancelamento = null;
			this.solicitacaoSelecionada = null;
			this.solicitacoesManutencaoMaquina = manterSolicitacaoManutencaoMaquinaService.buscarTodos();
			// Buscar apenas máquinas ATIVAS para o select de solicitação
			this.maquinasDisponiveis = manterCadastroMaquinaEquipamentoService.buscarAtivos();

			// Monta as categorias e já deixa a lista de máquinas pronta
			montarCategorias();
			aplicarFiltroCategoria();

			LOGGER.info(
					"Bean inicializado com sucesso. Total de solicitações: " + solicitacoesManutencaoMaquina.size());
		} catch (Exception e) {
			LOGGER.error("Erro em inicializar(): ", e);
			this.maquinasDisponiveis = new ArrayList<>();
			this.solicitacoesManutencaoMaquina = new ArrayList<>();
			this.categoriasDisponiveis = new ArrayList<>();
			this.maquinasFiltradas = new ArrayList<>();
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
	// CATEGORIA -> MÁQUINA
	// ==========================================================

	/**
	 * Normaliza um texto para comparação: remove acentos, espaços das pontas e
	 * diferenças de maiúsculas/minúsculas.
	 *
	 * Necessário porque "tipo" é texto livre no cadastro do equipamento, então
	 * o banco pode ter "Torno", "torno" e "TORNO" convivendo. Sem isso, a lista
	 * de categorias apareceria com a mesma categoria repetida três vezes.
	 */
	private static String chaveComparacao(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcentos.toUpperCase(Locale.ROOT);
	}

	/**
	 * Monta a lista de categorias a partir do campo "tipo" das máquinas ativas.
	 *
	 * Agrupa variações de escrita sob uma única entrada e usa como rótulo a
	 * primeira grafia encontrada. Máquinas sem tipo preenchido caem em
	 * "Sem categoria", para que nenhuma delas fique inacessível na tela.
	 */
	private void montarCategorias() {
		// TreeMap para já sair em ordem alfabética pela chave normalizada
		Map<String, String> categoriasPorChave = new TreeMap<>();
		boolean existeSemCategoria = false;

		if (maquinasDisponiveis != null) {
			for (CadastroMaquinaEquipamento maquina : maquinasDisponiveis) {
				String tipo = maquina.getTipo();

				if (tipo == null || tipo.trim().isEmpty()) {
					existeSemCategoria = true;
					continue;
				}

				categoriasPorChave.putIfAbsent(chaveComparacao(tipo), tipo.trim());
			}
		}

		this.categoriasDisponiveis = new ArrayList<>(categoriasPorChave.values());

		if (existeSemCategoria) {
			this.categoriasDisponiveis.add(SEM_CATEGORIA);
		}

		LOGGER.info("Categorias de equipamento montadas: {}", categoriasDisponiveis.size());
	}

	/**
	 * Recalcula a lista do segundo select conforme a categoria escolhida.
	 * Sem categoria escolhida, mostra todas as máquinas ativas (comportamento
	 * anterior da tela, preservado para quem já está acostumado).
	 */
	private void aplicarFiltroCategoria() {
		this.maquinasFiltradas = new ArrayList<>();

		if (maquinasDisponiveis == null) {
			return;
		}

		if (categoriaSelecionada == null || categoriaSelecionada.trim().isEmpty()) {
			this.maquinasFiltradas.addAll(maquinasDisponiveis);
			return;
		}

		boolean filtrandoSemCategoria = SEM_CATEGORIA.equals(categoriaSelecionada);
		String chaveCategoria = chaveComparacao(categoriaSelecionada);

		for (CadastroMaquinaEquipamento maquina : maquinasDisponiveis) {
			String tipo = maquina.getTipo();
			boolean tipoVazio = (tipo == null || tipo.trim().isEmpty());

			if (filtrandoSemCategoria) {
				if (tipoVazio) {
					this.maquinasFiltradas.add(maquina);
				}
				continue;
			}

			if (!tipoVazio && chaveComparacao(tipo).equals(chaveCategoria)) {
				this.maquinasFiltradas.add(maquina);
			}
		}
	}

	/**
	 * Listener do select de categoria. Refaz a lista de máquinas e descarta a
	 * máquina que estava escolhida caso ela não pertença mais à categoria —
	 * assim é impossível salvar uma solicitação com categoria e máquina
	 * incoerentes.
	 */
	public void aoTrocarCategoria() {
		aplicarFiltroCategoria();

		if (maquinaSelecionadaId != null && !maquinaPertenceAoFiltro(maquinaSelecionadaId)) {
			LOGGER.info("Máquina #{} descartada: não pertence à categoria '{}'", maquinaSelecionadaId,
					categoriaSelecionada);
			maquinaSelecionadaId = null;
		}

		LOGGER.info("Categoria '{}' selecionada. {} máquina(s) disponível(is)", categoriaSelecionada,
				maquinasFiltradas.size());
	}

	private boolean maquinaPertenceAoFiltro(Long id) {
		if (maquinasFiltradas == null) {
			return false;
		}
		for (CadastroMaquinaEquipamento maquina : maquinasFiltradas) {
			if (maquina.getId() != null && maquina.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}

	/** Listener do select de máquina — usado só para atualizar o painel. */
	public void aoTrocarMaquina() {
		CadastroMaquinaEquipamento maquina = getMaquinaSelecionada();
		LOGGER.info("Máquina selecionada: {}",
				maquina != null ? maquina.getNumeroPatrimonio() : "nenhuma");
	}

	/** Máquina correspondente ao id escolhido, ou null. */
	public CadastroMaquinaEquipamento getMaquinaSelecionada() {
		if (maquinaSelecionadaId == null || maquinasDisponiveis == null) {
			return null;
		}
		for (CadastroMaquinaEquipamento maquina : maquinasDisponiveis) {
			if (maquinaSelecionadaId.equals(maquina.getId())) {
				return maquina;
			}
		}
		return null;
	}

	/** Usado pelo rendered do painel de confirmação. */
	public boolean isMaquinaEscolhida() {
		return getMaquinaSelecionada() != null;
	}

	/**
	 * Contrário de isMaquinaEscolhida(). Existe só para a página não precisar
	 * do operador "not" no EL — o validador do Eclipse reclama dele.
	 */
	public boolean isMaquinaNaoEscolhida() {
		return !isMaquinaEscolhida();
	}

	/**
	 * Ano de fabricação da máquina já formatado, ou "—" quando não informado.
	 *
	 * Formatar aqui evita usar <f:convertDateTime> junto com o operador
	 * "empty" em cima de um Date na página: "empty" só tem sentido para
	 * String, Map, array e coleção, e o validador de EL do Eclipse marca o
	 * uso em datas como sempre falso.
	 */
	public String getAnoFabricacaoMaquinaSelecionada() {
		CadastroMaquinaEquipamento maquina = getMaquinaSelecionada();

		if (maquina == null || maquina.getAnoFabricacao() == null) {
			return "—";
		}

		return new SimpleDateFormat("yyyy").format(maquina.getAnoFabricacao());
	}

	/**
	 * URL da foto da máquina selecionada, no formato servido pelo ImageServlet.
	 * Devolve null quando a máquina não tem foto, para o painel exibir o
	 * placeholder no lugar da miniatura.
	 */
	public String getUrlFotoMaquinaSelecionada() {
		CadastroMaquinaEquipamento maquina = getMaquinaSelecionada();

		if (maquina == null || maquina.getUrlFoto() == null || maquina.getUrlFoto().trim().isEmpty()) {
			return null;
		}

		String foto = maquina.getUrlFoto().trim();

		if (foto.startsWith("/")) {
			return foto;
		}

		return Uploads.URL_BASE + PASTA_FOTOS_MAQUINAS + foto;
	}

	/** Identificação base da máquina: "PATRIMÔNIO - Fabricante Modelo". */
	private String montarRotuloBase(CadastroMaquinaEquipamento maquina) {
		if (maquina == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		if (maquina.getNumeroPatrimonio() != null && !maquina.getNumeroPatrimonio().trim().isEmpty()) {
			sb.append(maquina.getNumeroPatrimonio().trim()).append(" - ");
		}

		if (maquina.getFabricante() != null && !maquina.getFabricante().trim().isEmpty()) {
			sb.append(maquina.getFabricante().trim()).append(" ");
		}

		if (maquina.getModelo() != null && !maquina.getModelo().trim().isEmpty()) {
			sb.append(maquina.getModelo().trim());
		}

		String rotulo = sb.toString().trim();
		return rotulo.isEmpty() ? ("Equipamento #" + maquina.getId()) : rotulo;
	}

	/**
	 * Rótulo da máquina no select. Traz a localização junto porque, dentro de
	 * uma mesma categoria, é comum haver equipamentos iguais em setores
	 * diferentes — e é aí que o usuário erra.
	 */
	public String montarRotuloSelect(CadastroMaquinaEquipamento maquina) {
		if (maquina == null) {
			return "";
		}

		String rotulo = montarRotuloBase(maquina);

		if (maquina.getLocalizacao() != null && !maquina.getLocalizacao().trim().isEmpty()) {
			rotulo = rotulo + " | " + maquina.getLocalizacao().trim();
		}

		return rotulo;
	}

	/**
	 * Rótulo da busca rápida. Mostra a categoria (que ainda não foi escolhida)
	 * e NÃO mostra a localização, para a sugestão ficar curta e legível.
	 */
	public String montarRotuloBusca(CadastroMaquinaEquipamento maquina) {
		if (maquina == null) {
			return "";
		}

		String tipo = (maquina.getTipo() == null || maquina.getTipo().trim().isEmpty()) ? SEM_CATEGORIA
				: maquina.getTipo().trim();

		return montarRotuloBase(maquina) + " (" + tipo + ")";
	}

	/**
	 * Busca rápida por patrimônio, fabricante, modelo ou categoria. Devolve
	 * rótulos (String) em vez das entidades para não precisar de um Converter;
	 * o id fica guardado em mapaSugestoes e é recuperado no itemSelect.
	 */
	public List<String> completarMaquina(String query) {
		List<String> sugestoes = new ArrayList<>();
		mapaSugestoes = new LinkedHashMap<>();

		if (maquinasDisponiveis == null) {
			return sugestoes;
		}

		String termo = chaveComparacao(query);

		for (CadastroMaquinaEquipamento maquina : maquinasDisponiveis) {
			if (sugestoes.size() >= MAX_SUGESTOES) {
				break;
			}

			// A localização não entra na busca: o usuário procura pelo
			// equipamento (patrimônio, fabricante, modelo, categoria), e o
			// local só polui a sugestão.
			String alvo = chaveComparacao(maquina.getNumeroPatrimonio()) + " " + chaveComparacao(maquina.getFabricante())
					+ " " + chaveComparacao(maquina.getModelo()) + " " + chaveComparacao(maquina.getTipo());

			if (termo.isEmpty() || alvo.contains(termo)) {
				String rotulo = montarRotuloBusca(maquina);

				// Garante rótulo único: sem isso duas máquinas com os mesmos
				// dados apontariam para a mesma entrada no mapa.
				if (mapaSugestoes.containsKey(rotulo)) {
					rotulo = rotulo + " #" + maquina.getId();
				}

				mapaSugestoes.put(rotulo, maquina.getId());
				sugestoes.add(rotulo);
			}
		}

		return sugestoes;
	}

	/**
	 * Aplica a máquina escolhida na busca rápida: ajusta a categoria para a
	 * dela e seleciona a máquina nos dois selects, mantendo a tela coerente.
	 */
	public void selecionarMaquinaPorBusca(SelectEvent<?> event) {
		try {
			Object selecionado = event.getObject();

			if (selecionado == null) {
				return;
			}

			Long id = mapaSugestoes.get(String.valueOf(selecionado));

			if (id == null) {
				LOGGER.warn("Sugestão '{}' não encontrada no mapa de busca rápida", selecionado);
				return;
			}

			aplicarMaquina(id);
			termoBuscaMaquina = null;

		} catch (Exception e) {
			LOGGER.error("Erro ao selecionar máquina pela busca rápida: ", e);
		}
	}

	/**
	 * Seleciona uma máquina pelo id, alinhando a categoria com o tipo dela.
	 * Usado tanto pela busca rápida quanto pelo pré-preenchimento via URL.
	 */
	private void aplicarMaquina(Long id) {
		this.maquinaSelecionadaId = id;

		CadastroMaquinaEquipamento maquina = getMaquinaSelecionada();

		if (maquina == null) {
			LOGGER.warn("Máquina #{} não está entre as máquinas ativas", id);
			this.maquinaSelecionadaId = null;
			return;
		}

		String tipo = maquina.getTipo();

		if (tipo == null || tipo.trim().isEmpty()) {
			this.categoriaSelecionada = SEM_CATEGORIA;
		} else {
			// Usa o rótulo canônico da lista de categorias, não a grafia crua
			// do registro, senão o select não casa com nenhuma opção.
			this.categoriaSelecionada = tipo.trim();
			String chave = chaveComparacao(tipo);

			for (String categoria : categoriasDisponiveis) {
				if (chaveComparacao(categoria).equals(chave)) {
					this.categoriaSelecionada = categoria;
					break;
				}
			}
		}

		aplicarFiltroCategoria();

		LOGGER.info("Máquina #{} aplicada (categoria '{}')", id, categoriaSelecionada);
	}

	/**
	 * Abre o dialog GLOBAL de imagem com a foto e a ficha da máquina escolhida,
	 * para o solicitante conferir antes de registrar a solicitação.
	 */
	public void verDetalhesMaquinaSelecionada() {
		CadastroMaquinaEquipamento maquina = getMaquinaSelecionada();

		if (maquina == null) {
			visualizarImagemBean.abrir("Detalhes do Equipamento", null);
			visualizarImagemBean.setMensagemSemFoto("Selecione uma máquina para ver os detalhes.");
			return;
		}

		String descricao = montarRotuloSelect(maquina);

		// Sem foto, o caminho precisa ir NULO — concatenar a pasta com um
		// urlFoto nulo faria o dialog tentar exibir "maquinas_equipamentos/null"
		// e mostrar imagem quebrada em vez da mensagem de "sem foto".
		String caminhoFoto = null;

		if (maquina.getUrlFoto() != null && !maquina.getUrlFoto().trim().isEmpty()) {
			caminhoFoto = PASTA_FOTOS_MAQUINAS + maquina.getUrlFoto().trim();
		}

		visualizarImagemBean.abrir("Detalhes do Equipamento", descricao, caminhoFoto);

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a este equipamento.");
		visualizarImagemBean.addInfo("Nº Patrimônio", maquina.getNumeroPatrimonio());
		visualizarImagemBean.addInfo("Categoria (Tipo)", maquina.getTipo());
		visualizarImagemBean.addInfo("Fabricante", maquina.getFabricante());
		visualizarImagemBean.addInfo("Modelo", maquina.getModelo());
		visualizarImagemBean.addInfo("Localização", maquina.getLocalizacao());
		visualizarImagemBean.addInfo("Potência", maquina.getPotencia());
		visualizarImagemBean.addInfo("Lubrificação", maquina.getLubrificacao());
		visualizarImagemBean.addInfo("Observações", maquina.getObservacoes());

		LOGGER.info("Detalhes da máquina #{} abertos para conferência", maquina.getId());
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
	 * Chamado via <f:event type="preRenderView"> após os view params (maquinaId
	 * e origemPreventivaId) serem processados. Pré-preenche o formulário quando
	 * a solicitação vem de uma manutenção preventiva que identificou outro
	 * problema na máquina. Só roda em GET (não em postback), para não
	 * sobrescrever o que o técnico já tiver digitado.
	 */
	public void aplicarPreenchimentoAutomatico() {
		if (FacesContext.getCurrentInstance().isPostback()) {
			return;
		}

		if (maquinaId == null) {
			return;
		}

		try {
			// Alinha também a categoria, senão o select de máquina
			// abriria vazio para a máquina que veio pela URL.
			aplicarMaquina(maquinaId);

			this.solicitacaoManutencaoMaquina.setTipoManutencao(TipoManutencao.CORRETIVA);
			this.solicitacaoManutencaoMaquina.setDataManutencao(new Date());
			this.solicitacaoManutencaoMaquina
					.setSolicitante(usuarioLogado != null ? usuarioLogado.getNome() : "");

			if (origemPreventivaId != null) {
				this.solicitacaoManutencaoMaquina.setObservacoes(
						"Esta manutenção foi aberta devido a uma falha encontrada durante a manutenção preventiva de maquinas ou equipamentos #"
								+ origemPreventivaId);
			}

			LOGGER.info("Formulario pre-preenchido a partir da manutencao preventiva #{} (maquina #{})",
					origemPreventivaId, maquinaId);
		} catch (Exception e) {
			LOGGER.error("Erro ao pre-preencher solicitacao a partir da preventiva: ", e);
		}
	}

	public void salvar() {
		try {
			LOGGER.info("=== INICIANDO SALVAMENTO DE SOLICITAÇÃO DE MANUTENÇÃO DE MÁQUINA ===");

			// Validações
			if (maquinaSelecionadaId == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Selecione uma máquina."));
				return;
			}

			if (solicitacaoManutencaoMaquina.getSolicitante() == null
					|| solicitacaoManutencaoMaquina.getSolicitante().trim().isEmpty()) {
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
			// Não dá para usar required="#{...risco}" no campo: o atributo
			// required é avaliado na fase de validação, ANTES de o valor do
			// checkbox chegar ao bean, então ele leria sempre o valor antigo.
			// Por isso a conferência é feita aqui.
			if (solicitacaoManutencaoMaquina.isRisco()
					&& (solicitacaoManutencaoMaquina.getDescrRisco() == null
							|| solicitacaoManutencaoMaquina.getDescrRisco().trim().isEmpty())) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Você marcou que há risco de segurança. Descreva qual é o risco."));
				return;
			}

			// e-mail informado precisa ser um e-mail válido.
			//
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
					&& solicitacaoManutencaoMaquina.getTipoManutencao() != null
					&& solicitacaoManutencaoMaquina.getTipoManutencao() != TipoManutencao.CORRETIVA) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Funcionários só podem registrar manutenções corretivas."));
				return;
			}

			// Preencher automaticamente o usuário que registrou
			solicitacaoManutencaoMaquina.setUsuarioRegistro(usuarioLogado.getNome());
			LOGGER.info("Usuário que registrou: {}", usuarioLogado.getNome());
			LOGGER.info("Solicitante (quem teve o problema): {}", solicitacaoManutencaoMaquina.getSolicitante());

			// Buscar máquina selecionada
			CadastroMaquinaEquipamento maquina = manterCadastroMaquinaEquipamentoService
					.buscarPorId(maquinaSelecionadaId);
			if (maquina != null) {
				solicitacaoManutencaoMaquina.setMaquina(maquina);
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Máquina selecionada não encontrada."));
				return;
			}

			// UPLOAD DE FOTO (se houver)
			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().isEmpty()) {
				try {
					String caminhoWeb = Uploads.url(Uploads.SOLICITACOES_MAQUINAS) + Uploads.salvar(uploadedPhoto, Uploads.SOLICITACOES_MAQUINAS);
					solicitacaoManutencaoMaquina.setUrlFoto(caminhoWeb);
					LOGGER.info("URL salva no banco: {}", caminhoWeb);

				} catch (IOException e) {
					LOGGER.error("Erro ao fazer upload da foto", e);
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
							"Atenção", "Erro ao salvar foto, mas solicitação será cadastrada"));
				}
			}

			// Salvar solicitação
			SolicitacaoManutencaoMaquina novaSolicitacao = manterSolicitacaoManutencaoMaquinaService
					.salvar(solicitacaoManutencaoMaquina);

			LOGGER.info("Solicitação salva com sucesso - ID: {}", novaSolicitacao.getId());

			// ========================================
			// PREPARAR DADOS PARA NOTIFICAÇÃO
			// ========================================

			String nomeMaquina = (maquina.getFabricante() != null ? maquina.getFabricante() : "") + " "
					+ (maquina.getModelo() != null ? maquina.getModelo() : "Não informado");
			nomeMaquina = nomeMaquina.trim();

			String solicitante = novaSolicitacao.getSolicitante() != null ? novaSolicitacao.getSolicitante()
					: "Não informado";

			String descricao = novaSolicitacao.getFalha() != null && !novaSolicitacao.getFalha().trim().isEmpty()
					? novaSolicitacao.getFalha()
					: "Sem descrição";

			// ========================================
			// OBTER EMAIL DO SOLICITANTE
			//
			// Prioriza o e-mail digitado na tela (conta compartilhada dos
			// funcionários) e cai para o e-mail da conta logada quando não
			// houver nada informado.
			// ========================================
			String emailSolicitante = resolverEmailSolicitante();

			// ========================================
			// ENVIAR NOTIFICAÇÕES
			// ========================================
			try {
				notificacaoService.notificarNovaManutencao("Manutenção de Máquina", descricao, nomeMaquina, solicitante,
						emailSolicitante);

				LOGGER.info("Notificações enviadas com sucesso (administradores e solicitante)");

			} catch (Exception e) {
				LOGGER.error("Erro ao enviar notificações por email (processo principal não foi afetado)", e);
			}

			// ========================================
			// FINALIZAÇÃO
			// ========================================

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso!",
					"Solicitação de manutenção de máquina cadastrada com sucesso!"));

			LOGGER.info("Solicitação cadastrada - Solicitante: {}, Registrado por: {}", solicitante,
					usuarioLogado.getNome());

			// Limpar campos
			limpar();

			// Recarregar lista
			solicitacoesManutencaoMaquina = manterSolicitacaoManutencaoMaquinaService.buscarTodos();

			LOGGER.info("=== SALVAMENTO CONCLUÍDO COM SUCESSO ===");

		} catch (Exception e) {
			LOGGER.error("Erro ao cadastrar solicitação de manutenção de máquina", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao cadastrar solicitação de manutenção de máquina: " + e.getMessage()));
		}
	}

	public void prepararCancelamento(SolicitacaoManutencaoMaquina solicitacao) {
		LOGGER.info("Preparando cancelamento para solicitacao ID: {}", solicitacao.getId());
		this.solicitacaoSelecionada = solicitacao;
		this.motivoCancelamento = "";
	}

	public void confirmarCancelamento() {
		try {
			LOGGER.info("Iniciando confirmação de cancelamento");

			if (motivoCancelamento == null || motivoCancelamento.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage("formDialog:motivoCancel", new FacesMessage(
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

			SolicitacaoManutencaoMaquina solicitacaoAtualizada = manterSolicitacaoManutencaoMaquinaService
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

			manterSolicitacaoManutencaoMaquinaService.salvar(solicitacaoAtualizada);

			solicitacoesManutencaoMaquina = manterSolicitacaoManutencaoMaquinaService.buscarTodos();

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
		solicitacaoManutencaoMaquina = new SolicitacaoManutencaoMaquina();
		maquinaSelecionadaId = null;
		uploadedPhoto = null;
		motivoCancelamento = "";
		solicitacaoSelecionada = null;

		// Volta a seleção em duas etapas ao estado inicial
		categoriaSelecionada = null;
		termoBuscaMaquina = null;
		emailConfirmacao = null;
		avisoEmail = null;
		classeAvisoEmail = null;
		sugestaoEmail = null;
		mapaSugestoes = new LinkedHashMap<>();
		aplicarFiltroCategoria();

		LOGGER.info("Campos de solicitação limpos");
	}

	public void selecionarParaDetalhes(SolicitacaoManutencaoMaquina solicitacao) {
		this.solicitacaoSelecionada = solicitacao;
	}

	// ========================================
	// GETTERS E SETTERS
	// ========================================

	public SolicitacaoManutencaoMaquina getSolicitacaoManutencaoMaquina() {
		return solicitacaoManutencaoMaquina;
	}

	public void setSolicitacaoManutencaoMaquina(SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina) {
		this.solicitacaoManutencaoMaquina = solicitacaoManutencaoMaquina;
	}

	public List<SolicitacaoManutencaoMaquina> getSolicitacoesManutencaoMaquina() {
		return solicitacoesManutencaoMaquina;
	}

	public void setSolicitacoesManutencaoMaquina(List<SolicitacaoManutencaoMaquina> solicitacoesManutencaoMaquina) {
		this.solicitacoesManutencaoMaquina = solicitacoesManutencaoMaquina;
	}

	public List<CadastroMaquinaEquipamento> getMaquinasDisponiveis() {
		return maquinasDisponiveis;
	}

	public void setMaquinasDisponiveis(List<CadastroMaquinaEquipamento> maquinasDisponiveis) {
		this.maquinasDisponiveis = maquinasDisponiveis;
	}

	// GETTERS/SETTERS DA SELEÇÃO EM DUAS ETAPAS

	public String getCategoriaSelecionada() {
		return categoriaSelecionada;
	}

	public void setCategoriaSelecionada(String categoriaSelecionada) {
		this.categoriaSelecionada = categoriaSelecionada;
	}

	public List<String> getCategoriasDisponiveis() {
		return categoriasDisponiveis;
	}

	public void setCategoriasDisponiveis(List<String> categoriasDisponiveis) {
		this.categoriasDisponiveis = categoriasDisponiveis;
	}

	public List<CadastroMaquinaEquipamento> getMaquinasFiltradas() {
		return maquinasFiltradas;
	}

	public void setMaquinasFiltradas(List<CadastroMaquinaEquipamento> maquinasFiltradas) {
		this.maquinasFiltradas = maquinasFiltradas;
	}

	public String getTermoBuscaMaquina() {
		return termoBuscaMaquina;
	}

	public void setTermoBuscaMaquina(String termoBuscaMaquina) {
		this.termoBuscaMaquina = termoBuscaMaquina;
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

	public Long getMaquinaSelecionadaId() {
		return maquinaSelecionadaId;
	}

	public void setMaquinaSelecionadaId(Long maquinaSelecionadaId) {
		this.maquinaSelecionadaId = maquinaSelecionadaId;
	}

	public Long getMaquinaId() {
		return maquinaId;
	}

	public void setMaquinaId(Long maquinaId) {
		this.maquinaId = maquinaId;
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

	public SolicitacaoManutencaoMaquina getSolicitacaoSelecionada() {
		return solicitacaoSelecionada;
	}

	public void setSolicitacaoSelecionada(SolicitacaoManutencaoMaquina solicitacaoSelecionada) {
		this.solicitacaoSelecionada = solicitacaoSelecionada;
	}

	public CadastroUsuario getUsuarioLogado() {
		return usuarioLogado;
	}
}