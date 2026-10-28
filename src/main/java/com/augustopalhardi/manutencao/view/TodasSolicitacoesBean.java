package com.augustopalhardi.manutencao.view;

import com.augustopalhardi.manutencao.util.Uploads;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterHistoricoReprogramacaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;

@Named
@ViewScoped
public class TodasSolicitacoesBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TodasSolicitacoesBean.class);

	// ==========================================
	// INJEÇÕES DE DEPENDÊNCIA
	// ==========================================
	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterSolicitacaoManutencaoMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterSolicitacaoManutencaoPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusManutencaoService;

	@Inject
	private ManterHistoricoReprogramacaoService manterHistoricoReprogramacaoService;

	@Inject
	private ManterPeriodoTrabalhoManutencaoService manterPeriodoTrabalhoManutencaoService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterSolicitacaoCompraService;

	// ==========================================
	// ATRIBUTOS PRINCIPAIS
	// ==========================================
	private List<SolicitacaoManutencaoMaquina> solicitacoesMaquina = new ArrayList<>();
	private List<SolicitacaoManutencaoPredial> solicitacoesPredial = new ArrayList<>();
	private CadastroUsuario usuarioLogado;

	// ==========================================
	// CACHE DE PERFORMANCE
	// ==========================================
	// Evita consultar o banco de dados uma vez por solicitação (N+1) para
	// descobrir o status de cada uma. Carregado em bloco (1 única consulta)
	// sempre que as solicitações são (re)carregadas.
	private Map<String, ControleStatusManutencao> cacheControles = new HashMap<>();

	// Memoiza a lista unificada (máquina + predial) já com o status
	// resolvido, para não reconstruí-la do zero a cada chamada do getter —
	// o JSF/PrimeFaces costuma chamar o getter da tabela várias vezes por
	// requisição.
	private List<Map<String, Object>> cacheSolicitacoesUnificadas;

	// ==========================================
	// ATRIBUTOS DE FILTROS
	// ==========================================
	private String filtroTipoManutencao = "TODAS";
	private String filtroCategoria = "TODAS";
	private Date filtroDataInicio;
	private Date filtroDataFim;
	private String filtroBuscaId = "";
	private boolean ordenarPorIdCrescente = false;
	// ==========================================
	// ATRIBUTOS PARA CANCELAMENTO
	// ==========================================
	private String motivoCancelamento;
	private Object solicitacaoSelecionada;
	private String tipoSolicitacaoSelecionada;
	private boolean cancelamentoPermitido = false;

	// ==========================================
	// ATRIBUTOS PARA REPROGRAMAÇÃO
	// ==========================================
	private Date novaDataManutencao;
	private String motivoReprogramacao;
	private List<HistoricoReprogramacao> historicoReprogramacoes = new ArrayList<>();

	// ==========================================
	// ATRIBUTOS PARA DETALHES DA MANUTENCAO
	// ==========================================
	private ControleStatusManutencao controleStatusSelecionado;
	private List<PeriodoTrabalhoManutencao> periodosTrabalhoSelecionado = new ArrayList<>();
	private List<SolicitacaoCompraMateriaisServicos> comprasAssociadas = new ArrayList<>();

	// ==========================================
	// INICIALIZAÇÃO
	// ==========================================
	@PostConstruct
	public void inicializar() {
		try {
			usuarioLogado = obterUsuarioDaSessao();

			if (usuarioLogado == null) {
				LOGGER.error("Usuário não está logado!");
				adicionarMensagemErro("Erro", "Usuário não está logado.");
				return;
			}

			LOGGER.info("Usuário logado: {} - Tipo: {}", usuarioLogado.getNome(), usuarioLogado.getTipoUsuario());

			carregarSolicitacoes();
			LOGGER.info("Bean inicializado com sucesso. Solicitações de máquina: {}, Solicitações prediais: {}",
					solicitacoesMaquina.size(), solicitacoesPredial.size());
		} catch (Exception e) {
			LOGGER.error("Erro em inicializar(): ", e);
			this.solicitacoesMaquina = new ArrayList<>();
			this.solicitacoesPredial = new ArrayList<>();
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

	private void carregarSolicitacoes() {
		if (usuarioLogado == null) {
			this.solicitacoesMaquina = new ArrayList<>();
			this.solicitacoesPredial = new ArrayList<>();
			carregarCacheControles();
			return;
		}

		TipoUsuario tipoUsuario = usuarioLogado.getTipoUsuario();

		if (tipoUsuario == TipoUsuario.FUNCIONARIO) {
			carregarSolicitacoesDoUsuario();
			LOGGER.info("Carregadas solicitações do funcionário: {}", usuarioLogado.getNome());
		} else {
			carregarTodasSolicitacoes();
			LOGGER.info("Carregadas todas as solicitações para usuário tipo: {}", tipoUsuario);
		}

		carregarCacheControles();
	}

	/**
	 * Carrega TODOS os registros de status de manutenção em uma única
	 * consulta e monta um cache em memória (chave = tipo + id). Isso evita
	 * que cada solicitação precise de uma consulta individual ao banco só
	 * para saber seu status — o que, com muitas solicitações, gerava
	 * centenas/milhares de consultas numa única abertura da página.
	 */
	private void carregarCacheControles() {
		cacheControles = new HashMap<>();
		cacheSolicitacoesUnificadas = null;
		try {
			List<ControleStatusManutencao> todos = manterControleStatusManutencaoService.buscarTodos();
			for (ControleStatusManutencao c : todos) {
				cacheControles.put(chaveControle(c.getSolicitacaoId(), c.getTipoManutencao()), c);
			}
			LOGGER.debug("Cache de controles de status carregado: {} registros", cacheControles.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar cache de controles de status: ", e);
		}
	}

	private String chaveControle(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		return tipo + "_" + solicitacaoId;
	}

	/**
	 * Busca o controle de status a partir do cache em memória, em vez de
	 * consultar o banco a cada chamada.
	 */
	private ControleStatusManutencao buscarControleCache(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		return cacheControles.get(chaveControle(solicitacaoId, tipo));
	}

	private void carregarTodasSolicitacoes() {
		this.solicitacoesMaquina = manterSolicitacaoManutencaoMaquinaService.buscarTodos();
		this.solicitacoesPredial = manterSolicitacaoManutencaoPredialService.buscarTodos();
	}

	private void carregarSolicitacoesDoUsuario() {
		String nomeUsuarioLogado = usuarioLogado.getNome();

		List<SolicitacaoManutencaoMaquina> todasMaquinas = manterSolicitacaoManutencaoMaquinaService.buscarTodos();
		this.solicitacoesMaquina = todasMaquinas.stream().filter(s -> nomeUsuarioLogado.equals(s.getUsuarioRegistro()))
				.collect(Collectors.toList());

		List<SolicitacaoManutencaoPredial> todasPrediais = manterSolicitacaoManutencaoPredialService.buscarTodos();
		this.solicitacoesPredial = todasPrediais.stream().filter(s -> nomeUsuarioLogado.equals(s.getUsuarioRegistro()))
				.collect(Collectors.toList());

		LOGGER.info("Filtradas {} solicitações de máquina e {} prediais registradas pelo usuário: {}",
				this.solicitacoesMaquina.size(), this.solicitacoesPredial.size(), nomeUsuarioLogado);
	}

	// ==========================================
	// MÉTODOS DE FILTRO
	// ==========================================

	public void aplicarFiltros() {
		LOGGER.info("Aplicando filtros - Tipo Manutenção: {}, Categoria: {}", filtroTipoManutencao, filtroCategoria);
	}

	public void limparFiltros() {
		this.filtroTipoManutencao = "TODAS";
		this.filtroCategoria = "TODAS";
		this.filtroDataInicio = null;
		this.filtroDataFim = null;
		this.filtroBuscaId = null;
		this.ordenarPorIdCrescente = false;
		LOGGER.info("Filtros limpos");
	}

	public void toggleOrdenarPorId() {
		this.ordenarPorIdCrescente = !this.ordenarPorIdCrescente;
		LOGGER.info("Ordenação por ID crescente: {}", ordenarPorIdCrescente);
	}

	private List<Map<String, Object>> aplicarOrdenacao(List<Map<String, Object>> lista) {
		if (ordenarPorIdCrescente) {
			lista.sort((s1, s2) -> {
				Long id1 = (Long) s1.get("id");
				Long id2 = (Long) s2.get("id");
				return Long.compare(id1, id2);
			});
		}
		return lista;
	}

	private List<Map<String, Object>> aplicarFiltrosNaLista(List<Map<String, Object>> lista) {
		return lista.stream().filter(item -> {
			// Filtro por busca de ID
			if (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty()) {
				String busca = filtroBuscaId.trim().replaceAll("[^0-9]", "");
				if (!busca.isEmpty()) {
					Long id = (Long) item.get("id");
					if (!String.valueOf(id).contains(busca)) {
						return false;
					}
				}
			}

			// Filtro por categoria (MÁQUINA / PREDIAL)
			if (!"TODAS".equals(filtroCategoria)) {
				String tipo = (String) item.get("tipo");
				if (("MAQUINA".equals(filtroCategoria) && !"MÁQUINA".equals(tipo))
						|| ("PREDIAL".equals(filtroCategoria) && !"PREDIAL".equals(tipo))) {
					return false;
				}
			}

			// Filtro por tipo de manutenção (CORRETIVA / PREVENTIVA / PREDITIVA)
			if (!"TODAS".equals(filtroTipoManutencao)) {
				TipoManutencao tipoManutencao = (TipoManutencao) item.get("tipoManutencao");
				if ((tipoManutencao == null) || !filtroTipoManutencao.equals(tipoManutencao.name())) {
					return false;
				}
			}

			// Filtro por data início
			if (filtroDataInicio != null) {
				Date dataManutencao = (Date) item.get("dataManutencao");
				if (dataManutencao == null || dataManutencao.before(filtroDataInicio)) {
					return false;
				}
			}

			// Filtro por data fim
			if (filtroDataFim != null) {
				Date dataManutencao = (Date) item.get("dataManutencao");
				if (dataManutencao == null) {
					return false;
				}
				Calendar calFim = Calendar.getInstance();
				calFim.setTime(filtroDataFim);
				calFim.set(Calendar.HOUR_OF_DAY, 23);
				calFim.set(Calendar.MINUTE, 59);
				calFim.set(Calendar.SECOND, 59);
				if (dataManutencao.after(calFim.getTime())) {
					return false;
				}
			}

			return true;
		}).collect(Collectors.toList());
	}

	public String getDescricaoFiltrosAtivos() {
		List<String> filtrosAtivos = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

		if (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty()) {
			filtrosAtivos.add("ID: " + filtroBuscaId.trim());
		}
		if (!"TODAS".equals(filtroCategoria)) {
			filtrosAtivos.add("MAQUINA".equals(filtroCategoria) ? "Máquinas" : "Predial");
		}
		if (!"TODAS".equals(filtroTipoManutencao)) {
			filtrosAtivos.add(filtroTipoManutencao.substring(0, 1) + filtroTipoManutencao.substring(1).toLowerCase());
		}
		if (filtroDataInicio != null) {
			filtrosAtivos.add("De: " + sdf.format(filtroDataInicio));
		}
		if (filtroDataFim != null) {
			filtrosAtivos.add("Até: " + sdf.format(filtroDataFim));
		}
		if (ordenarPorIdCrescente) {
			filtrosAtivos.add("Ordem: ID crescente");
		}

		return filtrosAtivos.isEmpty() ? "" : "Filtros: " + String.join(", ", filtrosAtivos);
	}

	public boolean isFiltroAtivo() {
		return !"TODAS".equals(filtroTipoManutencao) || !"TODAS".equals(filtroCategoria) || filtroDataInicio != null
				|| filtroDataFim != null || (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty())
				|| ordenarPorIdCrescente;
	}

	// ==========================================
	// MÉTODOS PRINCIPAIS
	// ==========================================

	public List<Map<String, Object>> getSolicitacoesAtivasUnificadas() {
		List<Map<String, Object>> ativas = getSolicitacoesUnificadas().stream().filter(item -> {
			Boolean cancelada = (Boolean) item.get("cancelada");
			String status = getStatusFromItem(item);
			return !cancelada && !"CONCLUÍDA".equals(status);
		}).collect(Collectors.toList());

		return aplicarOrdenacao(aplicarFiltrosNaLista(ativas));
	}

	public List<Map<String, Object>> getSolicitacoesConcluidasUnificadas() {
		List<Map<String, Object>> concluidas = getSolicitacoesUnificadas().stream().filter(item -> {
			Boolean cancelada = (Boolean) item.get("cancelada");
			String status = getStatusFromItem(item);
			return !cancelada && "CONCLUÍDA".equals(status);
		}).sorted((s1, s2) -> {
			Date data1 = (Date) s1.get("dataManutencao");
			Date data2 = (Date) s2.get("dataManutencao");
			if (data1 != null && data2 != null) {
				return data2.compareTo(data1);
			}
			return 0;
		}).collect(Collectors.toList());

		return aplicarOrdenacao(aplicarFiltrosNaLista(concluidas));
	}

	public List<Map<String, Object>> getSolicitacoesCanceladasUnificadas() {
		List<Map<String, Object>> canceladas = getSolicitacoesUnificadas().stream()
				.filter(item -> (Boolean) item.get("cancelada")).sorted((s1, s2) -> {
					Date data1 = (Date) s1.get("dataCancelamento");
					Date data2 = (Date) s2.get("dataCancelamento");
					if (data1 != null && data2 != null) {
						return data2.compareTo(data1);
					}
					return 0;
				}).collect(Collectors.toList());

		return aplicarOrdenacao(aplicarFiltrosNaLista(canceladas));
	}

	private String getStatusFromItem(Map<String, Object> item) {
		try {
			Long id = (Long) item.get("id");
			String tipo = (String) item.get("tipo");

			TipoManutencaoReferencia tipoControle = "MÁQUINA".equals(tipo)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			ControleStatusManutencao controle = buscarControleCache(id, tipoControle);

			if (controle != null && controle.getStatus() != null) {
				switch (controle.getStatus()) {
				case PARA_FAZER:
					return "PARA FAZER";
				case EM_ANDAMENTO:
					return "EM ANDAMENTO";
				case CONCLUIDA:
					return "CONCLUÍDA";
				case CANCELADA:
					return "CANCELADA";
				default:
					return "PARA FAZER";
				}
			}
			return "PARA FAZER";
		} catch (Exception e) {
			return "PARA FAZER";
		}
	}

	public List<Map<String, Object>> getSolicitacoesUnificadas() {
		if (cacheSolicitacoesUnificadas != null) {
			return cacheSolicitacoesUnificadas;
		}

		List<Map<String, Object>> unificadas = new ArrayList<>();

		adicionarSolicitacoesMaquina(unificadas);
		adicionarSolicitacoesPredial(unificadas);
		ordenarSolicitacoes(unificadas);

		cacheSolicitacoesUnificadas = unificadas;
		return unificadas;
	}

	private void adicionarSolicitacoesMaquina(List<Map<String, Object>> unificadas) {
		for (SolicitacaoManutencaoMaquina maquina : solicitacoesMaquina) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", maquina.getId());
			item.put("idFormatado", maquina.getId() + " M");
			item.put("tipo", "MÁQUINA");
			// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
			// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
			// na entidade CadastroMaquinaEquipamento.
			item.put("descricaoLocal", maquina.getMaquina() != null ? maquina.getMaquina().getIdentificacao() : "");
			item.put("problema", maquina.getFalha());
			item.put("tipoManutencao", maquina.getTipoManutencao());
			item.put("dataManutencao", maquina.getDataManutencao());
			item.put("cancelada", maquina.isCancelada());
			item.put("dataCancelamento", maquina.getDataCancelamento());
			item.put("motivoCancelamento", maquina.getMotivoCancelamento());
			item.put("observacoes", maquina.getObservacoes());
			item.put("operando", maquina.isOperando());
			item.put("risco", maquina.isRisco());
			item.put("solicitante", maquina.getSolicitante());
			item.put("urgente", false);
			item.put("riscoSeguranca", false);
			unificadas.add(item);
		}
	}

	private void adicionarSolicitacoesPredial(List<Map<String, Object>> unificadas) {
		for (SolicitacaoManutencaoPredial predial : solicitacoesPredial) {
			Map<String, Object> item = new HashMap<>();
			item.put("id", predial.getId());
			item.put("idFormatado", predial.getId() + " P");
			item.put("tipo", "PREDIAL");
			item.put("descricaoLocal", predial.getLocal() != null ? predial.getLocal().getNome() : "");
			item.put("problema", predial.getProblema());
			item.put("tipoManutencao", predial.getTipoManutencao());
			item.put("dataManutencao", predial.getDataManutencao());
			item.put("cancelada", predial.isCancelada());
			item.put("dataCancelamento", predial.getDataCancelamento());
			item.put("motivoCancelamento", predial.getMotivoCancelamento());
			item.put("observacoes", predial.getObservacoes());
			item.put("urgente", predial.isUrgente());
			item.put("riscoSeguranca", predial.isRiscoSeguranca());
			item.put("solicitante", predial.getSolicitante());
			item.put("operando", false);
			item.put("risco", false);
			unificadas.add(item);
		}
	}

	private void ordenarSolicitacoes(List<Map<String, Object>> unificadas) {
		unificadas.sort((s1, s2) -> {
			Boolean cancelada1 = (Boolean) s1.get("cancelada");
			Boolean cancelada2 = (Boolean) s2.get("cancelada");

			if (!cancelada1.equals(cancelada2)) {
				return Boolean.compare(cancelada1, cancelada2);
			}

			Date data1 = (Date) s1.get("dataManutencao");
			Date data2 = (Date) s2.get("dataManutencao");
			if (data1 != null && data2 != null) {
				int dateComparison = data1.compareTo(data2);
				if (dateComparison != 0) {
					return dateComparison;
				}
			} else if (data1 != null) {
				return -1;
			} else if (data2 != null) {
				return 1;
			}

			Long id1 = (Long) s1.get("id");
			Long id2 = (Long) s2.get("id");
			return Long.compare(id1, id2);
		});
	}

	// ==========================================
	// MÉTODOS DE REPROGRAMAÇÃO
	// ==========================================

	public void prepararReprogramacao(Map<String, Object> item) {
		try {
			Long id = (Long) item.get("id");
			String tipo = (String) item.get("tipo");
			Date dataAtual = (Date) item.get("dataManutencao");

			LOGGER.info("Preparando reprogramação para {} ID: {}", tipo, id);

			this.tipoSolicitacaoSelecionada = tipo;
			this.solicitacaoSelecionada = buscarSolicitacaoPorTipoEId(tipo, id);
			this.novaDataManutencao = dataAtual;
			this.motivoReprogramacao = "";

			carregarHistoricoReprogramacao(id, tipo);

		} catch (Exception e) {
			LOGGER.error("Erro ao preparar reprogramação: ", e);
			adicionarMensagemErro("Erro", "Erro ao preparar reprogramação: " + e.getMessage());
		}
	}

	private void carregarHistoricoReprogramacao(Long id, String tipo) {
		TipoManutencaoReferencia tipoRef = "MÁQUINA".equals(tipo) ? TipoManutencaoReferencia.MAQUINA
				: TipoManutencaoReferencia.PREDIAL;

		this.historicoReprogramacoes = manterHistoricoReprogramacaoService.buscarHistoricoPorSolicitacao(id, tipoRef);
	}

	public void confirmarReprogramacao() {
		try {
			if (!validarReprogramacao()) {
				return;
			}

			boolean sucesso = false;
			Date dataAnterior = null;

			if ("MÁQUINA".equals(tipoSolicitacaoSelecionada)) {
				SolicitacaoManutencaoMaquina maquina = (SolicitacaoManutencaoMaquina) solicitacaoSelecionada;
				dataAnterior = maquina.getDataManutencao();
				sucesso = reprogramarSolicitacaoMaquina(maquina);
			} else if ("PREDIAL".equals(tipoSolicitacaoSelecionada)) {
				SolicitacaoManutencaoPredial predial = (SolicitacaoManutencaoPredial) solicitacaoSelecionada;
				dataAnterior = predial.getDataManutencao();
				sucesso = reprogramarSolicitacaoPredial(predial);
			}

			if (sucesso) {
				registrarHistoricoReprogramacao(dataAnterior);
				finalizarReprogramacao();
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao reprogramar solicitação: ", e);
			adicionarMensagemErro("Erro ao reprogramar solicitação", e.getMessage());
		}
	}

	private boolean validarReprogramacao() {
		if (motivoReprogramacao == null || motivoReprogramacao.trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage("rescheduleForm:motivoReprog",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "O motivo da reprogramação é obrigatório."));
			return false;
		}

		if (novaDataManutencao == null) {
			FacesContext.getCurrentInstance().addMessage("rescheduleForm:novaData",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "A nova data é obrigatória."));
			return false;
		}

		if (solicitacaoSelecionada == null) {
			adicionarMensagemErro("Erro", "Nenhuma solicitação selecionada.");
			return false;
		}

		Date dataAtual = null;
		if (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina) {
			dataAtual = ((SolicitacaoManutencaoMaquina) solicitacaoSelecionada).getDataManutencao();
		} else if (solicitacaoSelecionada instanceof SolicitacaoManutencaoPredial) {
			dataAtual = ((SolicitacaoManutencaoPredial) solicitacaoSelecionada).getDataManutencao();
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		if (dataAtual != null && sdf.format(novaDataManutencao).equals(sdf.format(dataAtual))) {
			adicionarMensagemErro("Erro", "A nova data deve ser diferente da data atual.");
			return false;
		}

		return true;
	}

	private boolean reprogramarSolicitacaoMaquina(SolicitacaoManutencaoMaquina maquina) {
		SolicitacaoManutencaoMaquina maquinaAtualizada = manterSolicitacaoManutencaoMaquinaService
				.buscarPorId(maquina.getId());

		if (maquinaAtualizada == null) {
			return false;
		}

		maquinaAtualizada.setDataManutencao(novaDataManutencao);
		manterSolicitacaoManutencaoMaquinaService.salvar(maquinaAtualizada);
		return true;
	}

	private boolean reprogramarSolicitacaoPredial(SolicitacaoManutencaoPredial predial) {
		SolicitacaoManutencaoPredial predialAtualizada = manterSolicitacaoManutencaoPredialService
				.buscarPorId(predial.getId());

		if (predialAtualizada == null) {
			return false;
		}

		predialAtualizada.setDataManutencao(novaDataManutencao);
		manterSolicitacaoManutencaoPredialService.salvar(predialAtualizada);
		return true;
	}

	private void registrarHistoricoReprogramacao(Date dataAnterior) {
		try {
			Long solicitacaoId = null;
			TipoManutencaoReferencia tipoRef = null;

			if (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina) {
				solicitacaoId = ((SolicitacaoManutencaoMaquina) solicitacaoSelecionada).getId();
				tipoRef = TipoManutencaoReferencia.MAQUINA;
			} else if (solicitacaoSelecionada instanceof SolicitacaoManutencaoPredial) {
				solicitacaoId = ((SolicitacaoManutencaoPredial) solicitacaoSelecionada).getId();
				tipoRef = TipoManutencaoReferencia.PREDIAL;
			}

			if (solicitacaoId != null && tipoRef != null) {
				manterHistoricoReprogramacaoService.registrarReprogramacao(solicitacaoId, tipoRef, dataAnterior,
						novaDataManutencao, motivoReprogramacao.trim(), usuarioLogado.getNome());
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao registrar histórico: ", e);
		}
	}

	private void finalizarReprogramacao() {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		String tipoFormatado = tipoSolicitacaoSelecionada.equals("MÁQUINA") ? "M" : "P";
		Long id = (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina)
				? ((SolicitacaoManutencaoMaquina) solicitacaoSelecionada).getId()
				: ((SolicitacaoManutencaoPredial) solicitacaoSelecionada).getId();

		adicionarMensagemSucesso(String.format("Solicitação #%d %s reprogramada para %s!", id, tipoFormatado,
				sdf.format(novaDataManutencao)));

		carregarSolicitacoes();
		limparCamposReprogramacao();
	}

	private void limparCamposReprogramacao() {
		motivoReprogramacao = "";
		novaDataManutencao = null;
		historicoReprogramacoes = new ArrayList<>();
	}

	public boolean podeSerReprogramada(Map<String, Object> item) {
		try {
			Boolean cancelada = (Boolean) item.get("cancelada");
			if (cancelada) {
				return false;
			}

			TipoManutencao tipoManutencao = (TipoManutencao) item.get("tipoManutencao");
			if (tipoManutencao != TipoManutencao.PREVENTIVA) {
				return false;
			}

			return "PARA FAZER".equals(getStatusFromItem(item));
		} catch (Exception e) {
			return false;
		}
	}

	// ==========================================
	// MÉTODOS DE CANCELAMENTO E DETALHES
	// ==========================================

	public void prepararDetalhes(Map<String, Object> item) {
		try {
			Long id = (Long) item.get("id");
			String tipo = (String) item.get("tipo");
			this.tipoSolicitacaoSelecionada = tipo;
			this.solicitacaoSelecionada = buscarSolicitacaoPorTipoEId(tipo, id);
			carregarHistoricoReprogramacao(id, tipo);
			carregarDadosManutencao(id, tipo);
		} catch (Exception e) {
			LOGGER.error("Erro ao preparar detalhes: ", e);
		}
	}

	private void carregarDadosManutencao(Long id, String tipo) {
		try {
			TipoManutencaoReferencia tipoControle = "MÁQUINA".equals(tipo)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			this.controleStatusSelecionado = buscarControleCache(id, tipoControle);

			TipoManutencaoReferencia tipoPeriodo = "MÁQUINA".equals(tipo)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			this.periodosTrabalhoSelecionado = manterPeriodoTrabalhoManutencaoService.buscarPorSolicitacao(id,
					tipoPeriodo);

			// Carregar todas as compras associadas
			TipoManutencaoReferencia tipoRef = "MÁQUINA".equals(tipo)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			List<SolicitacaoCompraMateriaisServicos> lista = manterSolicitacaoCompraService
					.buscarPorSolicitacaoManutencao(id, tipoRef);
			this.comprasAssociadas = (lista != null) ? lista : new ArrayList<>();

		} catch (Exception e) {
			LOGGER.error("Erro ao carregar dados da manutenção: ", e);
			this.controleStatusSelecionado = null;
			this.periodosTrabalhoSelecionado = new ArrayList<>();
			this.comprasAssociadas = new ArrayList<>();
		}
	}

	public String getSolucaoAplicada() {
		if (controleStatusSelecionado != null && controleStatusSelecionado.getSolucao() != null
				&& !controleStatusSelecionado.getSolucao().trim().isEmpty()) {
			return controleStatusSelecionado.getSolucao();
		}
		return null;
	}

	public String getObservacoesFinaisManutencao() {
		if (controleStatusSelecionado != null && controleStatusSelecionado.getObservacoesFinais() != null
				&& !controleStatusSelecionado.getObservacoesFinais().trim().isEmpty()) {
			return controleStatusSelecionado.getObservacoesFinais();
		}
		return null;
	}

	public String getTecnicosResponsaveis() {
		if (periodosTrabalhoSelecionado == null || periodosTrabalhoSelecionado.isEmpty()) {
			return null;
		}
		return periodosTrabalhoSelecionado.stream().filter(p -> p.getTecnicoResponsavel() != null)
				.map(p -> p.getTecnicoResponsavel().getNome()).distinct().collect(Collectors.joining(", "));
	}

	public double getTotalHorasTrabalhadasSelecionado() {
		if (periodosTrabalhoSelecionado == null || periodosTrabalhoSelecionado.isEmpty()) {
			return 0.0;
		}
		return periodosTrabalhoSelecionado.stream().mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum();
	}

	public String formatarHorasTrabalhadasHumano(double totalHoras) {
		if (totalHoras <= 0) {
			return "0h";
		}
		int horas = (int) totalHoras;
		int minutos = (int) ((totalHoras - horas) * 60);
		return minutos == 0 ? horas + "h" : horas + "h " + minutos + "min";
	}

	public boolean isManutencaoConcluida() {
		return controleStatusSelecionado != null
				&& controleStatusSelecionado.getStatus() == ControleStatusManutencao.StatusManutencao.CONCLUIDA;
	}

	public boolean temDadosManutencaoTecnica() {
		// Mostra a aba se houver qualquer dado técnico OU compras associadas
		boolean temDadosTecnicos = controleStatusSelecionado != null && (controleStatusSelecionado.getDataInicio() != null
				|| controleStatusSelecionado.getDataConclusao() != null
				|| (controleStatusSelecionado.getSolucao() != null
						&& !controleStatusSelecionado.getSolucao().trim().isEmpty())
				|| (periodosTrabalhoSelecionado != null && !periodosTrabalhoSelecionado.isEmpty()));
		boolean temCompras = comprasAssociadas != null && !comprasAssociadas.isEmpty();
		return temDadosTecnicos || temCompras;
	}

	public List<SolicitacaoCompraMateriaisServicos> getComprasAssociadas() {
		return comprasAssociadas;
	}

	public boolean isBbsNearMissItemSelecionado() {
		try {
			if (solicitacaoSelecionada == null) {
				return false;
			}
			Long id = (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina)
					? ((SolicitacaoManutencaoMaquina) solicitacaoSelecionada).getId()
					: ((SolicitacaoManutencaoPredial) solicitacaoSelecionada).getId();
			TipoManutencaoReferencia tipo = (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			ControleStatusManutencao controle = buscarControleCache(id, tipo);
			return controle != null && controle.isBbsNearMiss();
		} catch (Exception e) {
			LOGGER.warn("Erro ao verificar BBS/Near Miss: {}", e.getMessage());
			return false;
		}
	}

	public void prepararCancelamento(Map<String, Object> item) {
		Long id = (Long) item.get("id");
		String tipo = (String) item.get("tipo");
		this.tipoSolicitacaoSelecionada = tipo;
		this.motivoCancelamento = "";
		this.solicitacaoSelecionada = buscarSolicitacaoPorTipoEId(tipo, id);
		this.cancelamentoPermitido = false;

		if (solicitacaoSelecionada == null) {
			FacesContext.getCurrentInstance().validationFailed();
			return;
		}

		String status = getStatusManutencao(solicitacaoSelecionada, tipo);

		// Verifica se está em andamento
		if ("EM ANDAMENTO".equals(status)) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
					"Cancelamento não permitido", "Esta manutenção não pode ser cancelada porque está em andamento."));
			this.solicitacaoSelecionada = null;
			FacesContext.getCurrentInstance().validationFailed();
			return;
		}

		// Verifica se está em aguardando compra
		if ("AGUARDANDO COMPRA".equals(status)) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Cancelamento não permitido",
							"Esta manutenção não pode ser cancelada porque está aguardando compra."));
			this.solicitacaoSelecionada = null;
			FacesContext.getCurrentInstance().validationFailed();
			return;
		}

		// Verifica se está concluída e o usuário é funcionário
		if ("CONCLUIDA".equals(status) && isFuncionario()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Cancelamento não permitido",
							"Você não pode cancelar esta manutenção que já foi concluída porque não tem permissão."));
			this.solicitacaoSelecionada = null;
			FacesContext.getCurrentInstance().validationFailed();
			return;
		}

		this.cancelamentoPermitido = true;
	}

	public String getStatusManutencao(Object solicitacao, String tipo) {
		try {
			if (solicitacao == null) {
				return "INDEFINIDO";
			}
			Long id = (solicitacao instanceof SolicitacaoManutencaoMaquina)
					? ((SolicitacaoManutencaoMaquina) solicitacao).getId()
					: ((SolicitacaoManutencaoPredial) solicitacao).getId();
			boolean cancelada = (solicitacao instanceof SolicitacaoManutencaoMaquina)
					? ((SolicitacaoManutencaoMaquina) solicitacao).isCancelada()
					: ((SolicitacaoManutencaoPredial) solicitacao).isCancelada();

			if (cancelada) {
				return "CANCELADA";
			}

			TipoManutencaoReferencia tc = "MÁQUINA".equals(tipo)
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			ControleStatusManutencao controle = buscarControleCache(id, tc);
			return (controle != null && controle.getStatus() != null)
					? controle.getStatus().toString().replace("_", " ")
					: "PARA FAZER";
		} catch (Exception e) {
			return "ERRO";
		}
	}

	public boolean podeSerCancelada(Object solicitacao, String tipo) {
		String status = getStatusManutencao(solicitacao, tipo);
		boolean jaCancelada = (solicitacao instanceof SolicitacaoManutencaoMaquina)
				? ((SolicitacaoManutencaoMaquina) solicitacao).isCancelada()
				: ((SolicitacaoManutencaoPredial) solicitacao).isCancelada();

		if (jaCancelada) {
			return false;
		}

		if ("PARA FAZER".equals(status)) {
			return true;
		}

		// Concluídas só podem ser canceladas por quem NÃO for funcionário
		if ("CONCLUIDA".equals(status)) {
			return !isFuncionario();
		}

		return false;
	}

	private Object buscarSolicitacaoPorTipoEId(String tipo, Long id) {
		return "MÁQUINA".equals(tipo) ? manterSolicitacaoManutencaoMaquinaService.buscarPorId(id)
				: manterSolicitacaoManutencaoPredialService.buscarPorId(id);
	}

	public void confirmarCancelamento() {
		if (!validarCancelamento()) {
			return;
		}

		Long solicitacaoId = null;
		TipoManutencaoReferencia tipoControle = null;

		if ("MÁQUINA".equals(tipoSolicitacaoSelecionada)) {
			SolicitacaoManutencaoMaquina m = (SolicitacaoManutencaoMaquina) solicitacaoSelecionada;
			m.setCancelada(true);
			m.setMotivoCancelamento(motivoCancelamento.trim());
			m.setDataCancelamento(new Date());
			manterSolicitacaoManutencaoMaquinaService.salvar(m);
			solicitacaoId = m.getId();
			tipoControle = TipoManutencaoReferencia.MAQUINA;
		} else {
			SolicitacaoManutencaoPredial p = (SolicitacaoManutencaoPredial) solicitacaoSelecionada;
			p.setCancelada(true);
			p.setMotivoCancelamento(motivoCancelamento.trim());
			p.setDataCancelamento(new Date());
			manterSolicitacaoManutencaoPredialService.salvar(p);
			solicitacaoId = p.getId();
			tipoControle = TipoManutencaoReferencia.PREDIAL;
		}

		// Atualiza o ControleStatusManutencao para CANCELADA
		// para que o Painel de Manutencoes reflita o cancelamento
		try {
			manterControleStatusManutencaoService.criarOuAtualizar(solicitacaoId, tipoControle,
					ControleStatusManutencao.StatusManutencao.CANCELADA);
			LOGGER.info("ControleStatusManutencao atualizado para CANCELADA - ID: {}, Tipo: {}", solicitacaoId,
					tipoControle);
		} catch (Exception e) {
			LOGGER.error("Erro ao atualizar ControleStatusManutencao para CANCELADA: ", e);
		}

		finalizarCancelamento();
	}

	private boolean validarCancelamento() {
		if (motivoCancelamento == null || motivoCancelamento.trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage("cancelForm:motivoCancel",
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Motivo obrigatório."));
			return false;
		}
		return podeSerCancelada(solicitacaoSelecionada, tipoSolicitacaoSelecionada);
	}

	private void finalizarCancelamento() {
		carregarSolicitacoes();
		limparCamposCancelamento();
	}

	private void limparCamposCancelamento() {
		motivoCancelamento = "";
		solicitacaoSelecionada = null;
	}

	private void adicionarMensagemSucesso(String mensagem) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", mensagem));
	}

	private void adicionarMensagemErro(String titulo, String mensagem) {
		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, titulo, mensagem));
	}

	public String getUrlFotoCorrigida() {
		if (solicitacaoSelecionada == null) {
			return null;
		}
		String urlFoto = (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina)
				? ((SolicitacaoManutencaoMaquina) solicitacaoSelecionada).getUrlFoto()
				: ((SolicitacaoManutencaoPredial) solicitacaoSelecionada).getUrlFoto();

		if (urlFoto == null || urlFoto.trim().isEmpty()) {
			return null;
		}

		String urlCorrigida = urlFoto.replace('\\', '/');
		if (urlCorrigida.startsWith("C:/")) {
			String dir = (solicitacaoSelecionada instanceof SolicitacaoManutencaoMaquina)
					? Uploads.url(Uploads.SOLICITACOES_MAQUINAS)
					: Uploads.url(Uploads.SOLICITACOES_PREDIAL);
			return dir + urlCorrigida.substring(urlCorrigida.lastIndexOf('/') + 1);
		}
		return urlCorrigida;
	}

	public boolean isFuncionario() {
		return usuarioLogado != null && usuarioLogado.getTipoUsuario() == TipoUsuario.FUNCIONARIO;
	}

	public String getTituloPagina() {
		return isFuncionario() ? "Minhas Solicitações de Manutenção" : "Todas as Solicitações de Manutenção";
	}

	// ==========================================
	// GETTERS E SETTERS
	// ==========================================

	public String getFiltroTipoManutencao() {
		return filtroTipoManutencao;
	}

	public void setFiltroTipoManutencao(String filtroTipoManutencao) {
		this.filtroTipoManutencao = filtroTipoManutencao;
	}

	public String getFiltroCategoria() {
		return filtroCategoria;
	}

	public void setFiltroCategoria(String filtroCategoria) {
		this.filtroCategoria = filtroCategoria;
	}

	public List<SolicitacaoManutencaoMaquina> getSolicitacoesMaquina() {
		return solicitacoesMaquina;
	}

	public List<SolicitacaoManutencaoPredial> getSolicitacoesPredial() {
		return solicitacoesPredial;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public Object getSolicitacaoSelecionada() {
		return solicitacaoSelecionada;
	}

	public void setSolicitacaoSelecionada(Object solicitacaoSelecionada) {
		this.solicitacaoSelecionada = solicitacaoSelecionada;
	}

	public String getTipoSolicitacaoSelecionada() {
		return tipoSolicitacaoSelecionada;
	}

	public void setTipoSolicitacaoSelecionada(String t) {
		this.tipoSolicitacaoSelecionada = t;
	}

	public Date getNovaDataManutencao() {
		return novaDataManutencao;
	}

	public void setNovaDataManutencao(Date d) {
		this.novaDataManutencao = d;
	}

	public String getMotivoReprogramacao() {
		return motivoReprogramacao;
	}

	public void setMotivoReprogramacao(String m) {
		this.motivoReprogramacao = m;
	}

	public List<HistoricoReprogramacao> getHistoricoReprogramacoes() {
		return historicoReprogramacoes;
	}

	public ControleStatusManutencao getControleStatusSelecionado() {
		return controleStatusSelecionado;
	}

	public List<PeriodoTrabalhoManutencao> getPeriodosTrabalhoSelecionado() {
		return periodosTrabalhoSelecionado;
	}

	public String getFiltroBuscaId() {
		return filtroBuscaId;
	}

	public void setFiltroBuscaId(String filtroBuscaId) {
		this.filtroBuscaId = filtroBuscaId;
	}

	public Date getFiltroDataInicio() {
		return filtroDataInicio;
	}

	public void setFiltroDataInicio(Date filtroDataInicio) {
		this.filtroDataInicio = filtroDataInicio;
	}

	public Date getFiltroDataFim() {
		return filtroDataFim;
	}

	public void setFiltroDataFim(Date filtroDataFim) {
		this.filtroDataFim = filtroDataFim;
	}

	public boolean isOrdenarPorIdCrescente() {
		return ordenarPorIdCrescente;
	}

	public void setOrdenarPorIdCrescente(boolean ordenarPorIdCrescente) {
		this.ordenarPorIdCrescente = ordenarPorIdCrescente;
	}

	public boolean isCancelamentoPermitido() {
		return cancelamentoPermitido;
	}
}