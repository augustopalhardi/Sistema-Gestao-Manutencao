package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroUsuarioService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterHistoricoReprogramacaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.service.NotificacaoService;
import com.augustopalhardi.manutencao.util.Uploads;
import com.augustopalhardi.manutencao.view.dto.ItemManutencao;
import com.augustopalhardi.manutencao.view.dto.PeriodoTrabalho;

@Named
@ViewScoped
public class PainelManutencaoBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PainelManutencaoBean.class);

	@Inject
	private ManterCadastroUsuarioService manterCadastroUsuarioService;

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterSolicitacaoManutencaoMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterSolicitacaoManutencaoPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusManutencaoService;

	@Inject
	private ManterPeriodoTrabalhoManutencaoService manterPeriodoTrabalhoManutencaoService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterSolicitacaoCompraService;

	@Inject
	private ManterHistoricoReprogramacaoService manterHistoricoReprogramacaoService;

	@Inject
	private NotificacaoService notificacaoService;

	// Listas unificadas
	private List<ItemManutencao> todosItens = new ArrayList<>();
	// Alterado o nome do mapa para refletir a remocao de acentos/cedilhas
	private Map<String, List<ItemManutencao>> manutencoesFiltradas = new HashMap<>();

	// Filtros
	private String filtroTipo = "";
	private String filtroPrioridade = "";
	private Date filtroData;
	private String filtroBuscaId = "";
	private TipoManutencao tipoManutencaoFiltro;

	// Controles de fluxo
	private ItemManutencao itemSelecionado;
	private String solucaoAplicada;
	private String observacoesFinais;

	// Histórico de reprogramações da solicitação selecionada (para o diálogo
	// de detalhes)
	private List<HistoricoReprogramacao> historicoReprogramacoesSelecionado = new ArrayList<>();

	// Lista de periodos de trabalho para finalizacao
	private List<PeriodoTrabalho> periodosTrabalho = new ArrayList<>();
	private List<CadastroUsuario> tecnicosDisponiveis = new ArrayList<>();

	// Campos para solicitacao de compra
	private SolicitacaoCompraMateriaisServicos.TipoSolicitacao tipoSolicitacaoCompra;
	private String descricaoSolicitacaoCompra;
	private boolean confirmacaoSolicitacao = false;
	private Long tecnicoSolicitanteId;

	private Double horimetroFalha;

	private Boolean manutencaoEletrica;

	// Campo BBS/Near Miss
	private boolean bbsNearMiss = false;

	// Checkbox exibido apenas na Preventiva: ao finalizar, se marcado, abre uma
	// nova solicitação de manutenção Corretiva já pré-preenchida
	private boolean abrirCorretivaAposFinalizar = false;

	// Campos para reinício de manutenção concluída
	private boolean primeiraConfirmacaoReinicio = false;

	// Campo para busca de manutenções concluídas
	private String filtroBuscaConcluida = "";
	private boolean buscaConcluidaAtiva = false;
	// ========== CLASSE INTERNA: PERIODO TRABALHO ==========

	// ========== CLASSE INTERNA: ITEM MANUTENCAO ==========

	// ========== INICIALIZACAO ==========

	@PostConstruct
	public void inicializar() {
		try {
			// Alterado o nome do mapa para refletir a remocao de acentos/cedilhas
			manutencoesFiltradas = new HashMap<>();
			manutencoesFiltradas.put("PARA_FAZER", new ArrayList<>());
			manutencoesFiltradas.put("EM_ANDAMENTO", new ArrayList<>());
			manutencoesFiltradas.put("CONCLUIDA", new ArrayList<>());
			manutencoesFiltradas.put("CANCELADA", new ArrayList<>());
			manutencoesFiltradas.put("AGUARDANDO_COMPRA", new ArrayList<>());

			carregarTecnicos();
			carregarDados();
			inicializarCamposCompra();

			LOGGER.info("PainelManutencaoBean inicializado com sucesso. Total de itens: " + todosItens.size()
					+ ", Tecnicos: " + tecnicosDisponiveis.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao inicializar PainelManutencaoBean: ", e);

			if (manutencoesFiltradas == null) {
				manutencoesFiltradas = new HashMap<>();
			}
			manutencoesFiltradas.putIfAbsent("PARA_FAZER", new ArrayList<>());
			manutencoesFiltradas.putIfAbsent("EM_ANDAMENTO", new ArrayList<>());
			manutencoesFiltradas.putIfAbsent("CONCLUIDA", new ArrayList<>());
			manutencoesFiltradas.putIfAbsent("CANCELADA", new ArrayList<>());
			manutencoesFiltradas.putIfAbsent("AGUARDANDO_COMPRA", new ArrayList<>());

			if (tecnicosDisponiveis == null) {
				tecnicosDisponiveis = new ArrayList<>();
			}

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao inicializar painel", "Alguns dados podem não estar disponíveis."));
		}
	}

	// ========== METODOS DE VALIDACAO ==========

	public void validarDataFim(PeriodoTrabalho periodo) {
		if (periodo != null) {
			periodo.validarDataFim();
		}
	}

	public void validarDataInicio(PeriodoTrabalho periodo) {
		if (periodo != null) {
			periodo.validarDataInicio();
		}
	}

	public String formatarHorasTrabalhadasHumano(double horas) {
		if (horas <= 0) {
			return "0min";
		}
		long totalMinutos = Math.round(horas * 60);
		long horasInteiras = totalMinutos / 60;
		long minutosRestantes = totalMinutos % 60;

		if (horasInteiras > 0 && minutosRestantes > 0) {
			return String.format("%dh %dmin", horasInteiras, minutosRestantes);
		} else if (horasInteiras > 0) {
			return String.format("%dh", horasInteiras);
		} else {
			return String.format("%dmin", minutosRestantes);
		}
	}

	public String getTotalHorasTrabalhadasFormatado() {
		double totalHoras = 0.0;
		for (PeriodoTrabalho periodo : periodosTrabalho) {
			if (periodo.isDuracaoValida()) {
				totalHoras += periodo.getDuracaoEmHoras();
			}
		}
		return formatarHorasTrabalhadasHumano(totalHoras);
	}

	public boolean validarTodosPeriodos() {
		boolean todosValidos = true;

		for (int i = 0; i < periodosTrabalho.size(); i++) {
			PeriodoTrabalho periodo = periodosTrabalho.get(i);
			periodo.validarDataInicio();
			periodo.validarDataFim();

			if (periodo.getMensagemValidacao() != null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Erro no Periodo " + (i + 1), periodo.getMensagemValidacao()));
				todosValidos = false;
			}

			if (periodo.getDataHoraInicio() != null && periodo.getDataHoraFim() != null) {
				if (periodo.getDataHoraInicio().equals(periodo.getDataHoraFim())) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Erro no Periodo " + (i + 1), "Não é permitido lançar 0 horas de trabalho."));
					todosValidos = false;
				}
			}
		}

		boolean temPeriodoValido = false;
		for (PeriodoTrabalho periodo : periodosTrabalho) {
			if (periodo.isDuracaoValida()) {
				temPeriodoValido = true;
				break;
			}
		}

		if (!temPeriodoValido) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"É necessário ao menos um período de trabalho válido com duração maior que 0."));
			todosValidos = false;
		}

		return todosValidos;
	}

	// ========== METODOS DE CARREGAMENTO DE DADOS ==========

	private void carregarTecnicos() {
		try {
			tecnicosDisponiveis = manterCadastroUsuarioService.buscarTodos().stream()
					.filter(usuario -> TipoUsuario.TECNICO.equals(usuario.getTipoUsuario()))
					.collect(Collectors.toList());
			LOGGER.info("Tecnicos carregados: {}", tecnicosDisponiveis.size());
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar técnicos: {}", e.getMessage());
			tecnicosDisponiveis = new ArrayList<>();
		}
	}

	private void carregarDados() {
		todosItens.clear();

		try {
			// ========== CARREGAMENTO EM LOTE (evita N+1 consultas) ==========
			// Em vez de consultar status/horas/compra individualmente para
			// cada manutenção (3 consultas × N itens), carregamos tudo de
			// uma vez (3 consultas no total) e montamos mapas em memória
			// para consulta rápida dentro do loop.
			Map<String, ControleStatusManutencao> mapControles = carregarMapaControles();
			Map<String, List<PeriodoTrabalhoManutencao>> mapPeriodos = carregarMapaPeriodos();
			Map<String, List<SolicitacaoCompraMateriaisServicos>> mapCompras = carregarMapaCompras();

			List<SolicitacaoManutencaoMaquina> maquinas = manterSolicitacaoManutencaoMaquinaService.buscarTodos();
			for (SolicitacaoManutencaoMaquina maquina : maquinas) {
				try {
					ControleStatusManutencao controle = mapControles.get(chave("MAQUINA", maquina.getId()));

					Double totalHoras = somarHoras(
							mapPeriodos.getOrDefault(chave("MAQUINA", maquina.getId()), new ArrayList<>()));

					ItemManutencao item = new ItemManutencao(maquina, controle, totalHoras);

					// ADICIONADO: buscar e popular dados de solicitação de compra
					try {
						List<SolicitacaoCompraMateriaisServicos> solicitacoesCompra = mapCompras
								.getOrDefault(chave("MAQUINA", maquina.getId()), new ArrayList<>());

						if (!solicitacoesCompra.isEmpty()) {
							// Pegar a solicitacao mais recente
							SolicitacaoCompraMateriaisServicos solicitacao = solicitacoesCompra
									.get(solicitacoesCompra.size() - 1);
							item.setDataSolicitacaoCompra(solicitacao.getDataSolicitacao());
							item.setDescricaoSolicitacaoCompra(solicitacao.getDescricaoDetalhada()); // Usar
																										// getDescricaoDetalhada()
																										// que eh o
																										// campo com a
																										// informacao
																										// detalhada.
						}
					} catch (Exception e) {
						LOGGER.warn("Erro ao buscar solicitação de compra para manutenção {}: {}", maquina.getId(),
								e.getMessage());
					}

					if (!maquina.isCancelada() && (tipoManutencaoFiltro == null || (maquina.getTipoManutencao() != null
							&& maquina.getTipoManutencao().equals(tipoManutencaoFiltro)))) {
						todosItens.add(item);
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao processar maquina ID {}: {}", maquina.getId(), e.getMessage());
				}
			}

			List<SolicitacaoManutencaoPredial> prediais = manterSolicitacaoManutencaoPredialService.buscarTodos();
			for (SolicitacaoManutencaoPredial predial : prediais) {
				try {
					ControleStatusManutencao controle = mapControles.get(chave("PREDIAL", predial.getId()));

					Double totalHoras = somarHoras(
							mapPeriodos.getOrDefault(chave("PREDIAL", predial.getId()), new ArrayList<>()));

					ItemManutencao item = new ItemManutencao(predial, controle, totalHoras);

					// ADICIONADO: buscar e popular dados de solicitação de compra
					try {
						List<SolicitacaoCompraMateriaisServicos> solicitacoesCompra = mapCompras
								.getOrDefault(chave("PREDIAL", predial.getId()), new ArrayList<>());

						if (!solicitacoesCompra.isEmpty()) {
							// Pegar a solicitacao mais recente
							SolicitacaoCompraMateriaisServicos solicitacao = solicitacoesCompra
									.get(solicitacoesCompra.size() - 1);
							item.setDataSolicitacaoCompra(solicitacao.getDataSolicitacao());
							item.setDescricaoSolicitacaoCompra(solicitacao.getDescricaoDetalhada()); // Usar
																										// getDescricaoDetalhada()
						}
					} catch (Exception e) {
						LOGGER.warn("Erro ao buscar solicitação de compra para manutenção {}: {}", predial.getId(),
								e.getMessage());
					}

					if (!predial.isCancelada() && (tipoManutencaoFiltro == null || (predial.getTipoManutencao() != null
							&& predial.getTipoManutencao().equals(tipoManutencaoFiltro)))) {
						todosItens.add(item);
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao processar predial ID {}: {}", predial.getId(), e.getMessage());
				}
			}

			aplicarFiltros();

		} catch (Exception e) {
			LOGGER.error("Erro ao carregar dados: ", e);
		}
	}

	/**
	 * Monta a chave usada nos mapas de cache (ex: "MAQUINA_123").
	 */
	private String chave(String tipo, Long id) {
		return tipo + "_" + id;
	}

	/**
	 * Carrega todos os registros de status de manutenção em uma única
	 * consulta e monta um mapa (chave = tipo + id) para consulta rápida.
	 */
	private Map<String, ControleStatusManutencao> carregarMapaControles() {
		Map<String, ControleStatusManutencao> mapa = new HashMap<>();
		try {
			for (ControleStatusManutencao c : manterControleStatusManutencaoService.buscarTodos()) {
				String tipo = c.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA ? "MAQUINA" : "PREDIAL";
				mapa.put(chave(tipo, c.getSolicitacaoId()), c);
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar mapa de controles de status: {}", e.getMessage());
		}
		return mapa;
	}

	/**
	 * Carrega todos os períodos de trabalho em uma única consulta e os
	 * agrupa por manutenção (chave = tipo + id) para consulta rápida.
	 */
	private Map<String, List<PeriodoTrabalhoManutencao>> carregarMapaPeriodos() {
		Map<String, List<PeriodoTrabalhoManutencao>> mapa = new HashMap<>();
		try {
			for (PeriodoTrabalhoManutencao p : manterPeriodoTrabalhoManutencaoService.buscarTodos()) {
				String tipo = p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA ? "MAQUINA"
						: "PREDIAL";
				mapa.computeIfAbsent(chave(tipo, p.getSolicitacaoId()), k -> new ArrayList<>()).add(p);
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar mapa de períodos de trabalho: {}", e.getMessage());
		}
		return mapa;
	}

	/**
	 * Soma a duração (em horas) de uma lista de períodos de trabalho —
	 * mesma lógica que antes era calculada por solicitação individual, mas
	 * operando sobre uma lista já carregada em memória.
	 */
	private Double somarHoras(List<PeriodoTrabalhoManutencao> periodos) {
		if (periodos == null || periodos.isEmpty()) {
			return 0.0;
		}
		double totalHoras = 0.0;
		for (PeriodoTrabalhoManutencao periodo : periodos) {
			if (periodo.getDataHoraInicio() != null && periodo.getDataHoraFim() != null) {
				long diferencaMs = periodo.getDataHoraFim().getTime() - periodo.getDataHoraInicio().getTime();
				totalHoras += diferencaMs / (1000.0 * 60.0 * 60.0);
			}
		}
		return totalHoras;
	}

	/**
	 * Carrega todas as solicitações de compra em uma única consulta e as
	 * agrupa por manutenção (chave = tipo + id), já ordenadas da mais
	 * antiga para a mais recente — mesmo critério (ORDER BY dataSolicitacao
	 * DESC, pegando o último da lista) usado na consulta individual
	 * original.
	 */
	private Map<String, List<SolicitacaoCompraMateriaisServicos>> carregarMapaCompras() {
		Map<String, List<SolicitacaoCompraMateriaisServicos>> mapa = new HashMap<>();
		try {
			for (SolicitacaoCompraMateriaisServicos c : manterSolicitacaoCompraService.buscarTodas()) {
				if (c.getSolicitacaoManutencaoId() == null || c.getTipoManutencaoReferencia() == null) {
					continue;
				}
				String tipo = c.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA
						? "MAQUINA"
						: "PREDIAL";
				mapa.computeIfAbsent(chave(tipo, c.getSolicitacaoManutencaoId()), k -> new ArrayList<>()).add(c);
			}
			// Ordena cada lista da mais antiga para a mais recente, para que
			// list.get(list.size() - 1) continue pegando o mesmo registro
			// que a consulta original (ORDER BY dataSolicitacao DESC, index 0
			// = mais recente).
			for (List<SolicitacaoCompraMateriaisServicos> lista : mapa.values()) {
				lista.sort(Comparator.comparing(SolicitacaoCompraMateriaisServicos::getDataSolicitacao,
						Comparator.nullsFirst(Comparator.naturalOrder())));
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar mapa de solicitações de compra: {}", e.getMessage());
		}
		return mapa;
	}

	// ========== METODOS DE FILTROS ==========

	public void aplicarFiltros() {
		List<ItemManutencao> itensFiltrados = new ArrayList<>(todosItens);

		// Filtro por busca de ID
		if (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty()) {
			String busca = filtroBuscaId.trim().replaceAll("[^0-9]", "");
			if (!busca.isEmpty()) {
				itensFiltrados.removeIf(item -> !String.valueOf(item.getId()).contains(busca));
			}
		}

		if (!filtroTipo.isEmpty()) {
			itensFiltrados.removeIf(item -> !item.getTipo().equals(filtroTipo));
		}

		if (!filtroPrioridade.isEmpty()) {
			switch (filtroPrioridade) {
			case "ALTA":
				itensFiltrados.removeIf(item -> !((item.getTipo().equals("MAQUINA")
						&& (item.isRisco() || item.isOperando()))
						|| (item.getTipo().equals("PREDIAL") && (item.isRiscoSeguranca() || item.isUrgente()))));
				break;
			case "NORMAL":
				itensFiltrados
						.removeIf(item -> (item.getTipo().equals("MAQUINA") && (item.isRisco() || item.isOperando()))
								|| (item.getTipo().equals("PREDIAL") && (item.isRiscoSeguranca() || item.isUrgente())));
				break;
			}
		}

		if (filtroData != null) {
			itensFiltrados.removeIf(
					item -> item.getDataManutencao() == null || !isSameDay(item.getDataManutencao(), filtroData));
		}

		manutencoesFiltradas.clear();
		manutencoesFiltradas.put("PARA_FAZER", new ArrayList<>());
		manutencoesFiltradas.put("EM_ANDAMENTO", new ArrayList<>());
		manutencoesFiltradas.put("CONCLUIDA", new ArrayList<>());
		manutencoesFiltradas.put("CANCELADA", new ArrayList<>());
		manutencoesFiltradas.put("AGUARDANDO_COMPRA", new ArrayList<>());

		for (ItemManutencao item : itensFiltrados) {
			manutencoesFiltradas.get(item.getStatus().name()).add(item);
		}

		// Ordenar todas as listas por data de manutenção em ordem CRESCENTE (mais
		// antigas primeiro)
		for (List<ItemManutencao> lista : manutencoesFiltradas.values()) {
			lista.sort((a, b) -> {
				Date dataA = a.getDataManutencao();
				Date dataB = b.getDataManutencao();
				if (dataA == null && dataB == null) {
					return 0;
				}
				if (dataA == null) {
					return 1;
				}
				if (dataB == null) {
					return -1;
				}
				return dataA.compareTo(dataB);
			});
		}
	}

	private boolean isSameDay(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return false;
		}
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
				&& cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
	}

	public void limparFiltros() {
		filtroTipo = "";
		filtroPrioridade = "";
		filtroData = null;
		filtroBuscaId = "";
		aplicarFiltros();
	}

	public void buscarPorId() {
		aplicarFiltros();
	}

	public void limparBuscaId() {
		filtroBuscaId = "";
		aplicarFiltros();
	}

	// ========== METODOS DE CONTROLE DE MANUTENCAO ==========

	public void prepararInicioManutencao(ItemManutencao item) {
		this.itemSelecionado = item;
		this.horimetroFalha = null;
		this.manutencaoEletrica = null;
	}

	public void confirmarInicioManutencao() {
		try {
			TipoManutencaoReferencia tipo = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			// Se for manutenção de máquina, atualiza o horímetro e
			// manutenção
			// elétrica
			// antes de mudar o status
			if (tipo == TipoManutencaoReferencia.MAQUINA && horimetroFalha != null) {
				// Busca a solicitação original para atualizar os campos
				SolicitacaoManutencaoMaquina solicitacao = manterSolicitacaoManutencaoMaquinaService
						.buscarPorId(itemSelecionado.getId());

				if (solicitacao != null) {
					// Atualiza o horímetro
					solicitacao.setHorimetroFalha(horimetroFalha);

					// Atualiza se é manutenção elétrica
					solicitacao.setManutencaoEletrica(manutencaoEletrica != null ? manutencaoEletrica : false);

					// Salva a solicitação atualizada
					manterSolicitacaoManutencaoMaquinaService.salvar(solicitacao);
				}
			}

			// Continua com o fluxo normal de atualização de status
			// Usa iniciarManutencao para garantir que a dataInicio seja gravada,
			// permitindo que a aba "Manutenção Técnica" apareça enquanto a
			// manutenção estiver em andamento (antes de ser concluída)
			manterControleStatusManutencaoService.iniciarManutencao(itemSelecionado.getId(), tipo, new Date());

			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Manutencao #" + itemSelecionado.getId() + " iniciada!"));

			LOGGER.info("Manutencao iniciada: ID " + itemSelecionado.getId());

			// Limpa os campos e o item selecionado
			horimetroFalha = null;
			manutencaoEletrica = false;
			itemSelecionado = null;

		} catch (Exception e) {
			LOGGER.error("Erro ao iniciar manutenção: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao iniciar manutenção: " + e.getMessage()));
		}
	}

	public void prepararFinalizacaoManutencao(ItemManutencao item) {
		this.itemSelecionado = item;
		this.solucaoAplicada = "";
		this.observacoesFinais = "";
		this.bbsNearMiss = false;
		this.abrirCorretivaAposFinalizar = false;

		if (tecnicosDisponiveis == null || tecnicosDisponiveis.isEmpty()) {
			carregarTecnicos();
		}

		this.periodosTrabalho = new ArrayList<>();

		try {
			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(item.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			List<PeriodoTrabalhoManutencao> periodosExistentes = manterPeriodoTrabalhoManutencaoService
					.buscarPorSolicitacao(item.getId(), tipoPeriodo);

			if (!periodosExistentes.isEmpty()) {
				for (PeriodoTrabalhoManutencao p : periodosExistentes) {
					PeriodoTrabalho novoPeriodo = new PeriodoTrabalho();
					novoPeriodo.setDataHoraInicio(p.getDataHoraInicio());
					novoPeriodo.setDataHoraFim(p.getDataHoraFim());
					novoPeriodo.setDescricaoAtividade(p.getDescricaoAtividade());
					novoPeriodo.setUtilizadoParaDiagnostico(p.getUtilizadoParaDiagnostico());

					if (p.getTecnicoResponsavel() != null) {
						novoPeriodo.setTecnicoResponsavel(p.getTecnicoResponsavel());
						novoPeriodo.setTecnicoResponsavelId(p.getTecnicoResponsavel().getId());
					}

					this.periodosTrabalho.add(novoPeriodo);
				}
			} else {
				this.periodosTrabalho.add(new PeriodoTrabalho());
			}

			LOGGER.info("Preparação para finalização. Item: " + item.getId() + " - Periodos carregados: "
					+ this.periodosTrabalho.size());

			// Carregar bbsNearMiss do controle existente, se houver
			TipoManutencaoReferencia tipoControle = "MAQUINA".equals(item.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			ControleStatusManutencao controleExistente = manterControleStatusManutencaoService
					.buscarPorSolicitacao(item.getId(), tipoControle);
			if (controleExistente != null) {
				this.bbsNearMiss = controleExistente.isBbsNearMiss();
				this.solucaoAplicada = controleExistente.getSolucao() != null ? controleExistente.getSolucao() : "";
				this.observacoesFinais = controleExistente.getObservacoesFinais() != null
						? controleExistente.getObservacoesFinais()
						: "";
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao preparar finalização da manutenção: ", e);
			if (this.periodosTrabalho.isEmpty()) {
				this.periodosTrabalho.add(new PeriodoTrabalho());
			}
		}
	}

	public String confirmarFinalizacaoManutencao() {
		try {
			LOGGER.info("Iniciando finalização da manutenção ID: " + itemSelecionado.getId());

			if (!validarTodosPeriodos()) {
				LOGGER.warn("Validação de períodos falhou - não prosseguindo com finalização");
				FacesContext.getCurrentInstance().validationFailed();
				return null;
			}

			if (solucaoAplicada == null || solucaoAplicada.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "A solução aplicada é obrigatória."));
				FacesContext.getCurrentInstance().validationFailed();
				return null;
			}

			List<PeriodoTrabalho> periodosValidos = periodosTrabalho.stream()
					.filter(p -> p.getDataHoraInicio() != null && p.getDataHoraFim() != null
							&& p.getDataHoraFim().after(p.getDataHoraInicio()) && p.getMensagemValidacao() == null)
					.collect(Collectors.toList());

			if (periodosValidos.isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro de Validacao",
								"Nenhum período de trabalho válido encontrado. Verifique as datas informadas."));
				FacesContext.getCurrentInstance().validationFailed();
				return null;
			}

			for (int i = 0; i < periodosValidos.size(); i++) {
				PeriodoTrabalho p = periodosValidos.get(i);
				if (p.getTecnicoResponsavelId() == null) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Erro de Validacao", "Periodo " + (i + 1) + ": Tecnico responsavel deve ser selecionado."));
					FacesContext.getCurrentInstance().validationFailed();
					return null;
				}
			}

			TipoManutencaoReferencia tipoControle = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(itemSelecionado.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			manterPeriodoTrabalhoManutencaoService.excluirTodosPorSolicitacao(itemSelecionado.getId(), tipoPeriodo);

			for (PeriodoTrabalho periodo : periodosValidos) {
				CadastroUsuario tecnicoSelecionado = buscarTecnicoPorId(periodo.getTecnicoResponsavelId());

				if (tecnicoSelecionado == null) {
					LOGGER.warn("Técnico com ID {} não encontrado", periodo.getTecnicoResponsavelId());
				}

				manterPeriodoTrabalhoManutencaoService.adicionarPeriodoComTecnicoComDiagnostico(itemSelecionado.getId(),
						tipoPeriodo, periodo.getDataHoraInicio(), periodo.getDataHoraFim(),
						periodo.getDescricaoAtividade(), tecnicoSelecionado, periodo.getUtilizadoParaDiagnostico());
			}

			Date dataFimMaisRecente = periodosValidos.stream().map(PeriodoTrabalho::getDataHoraFim).max(Date::compareTo)
					.orElse(new Date());

			manterControleStatusManutencaoService.finalizarManutencao(itemSelecionado.getId(), tipoControle,
					dataFimMaisRecente, solucaoAplicada.trim(), observacoesFinais, bbsNearMiss);

			// Captura os dados necessarios para eventual abertura de corretiva
			// ANTES de limpar o itemSelecionado
			boolean deveAbrirCorretiva = abrirCorretivaAposFinalizar;
			Long idFinalizada = itemSelecionado.getId();
			String tipoFinalizada = itemSelecionado.getTipo();
			Long maquinaId = ("MAQUINA".equals(tipoFinalizada) && itemSelecionado.getSolicitacaoMaquina() != null
					&& itemSelecionado.getSolicitacaoMaquina().getMaquina() != null)
							? itemSelecionado.getSolicitacaoMaquina().getMaquina().getId()
							: null;
			Long localId = ("PREDIAL".equals(tipoFinalizada) && itemSelecionado.getSolicitacaoPredial() != null
					&& itemSelecionado.getSolicitacaoPredial().getLocal() != null)
							? itemSelecionado.getSolicitacaoPredial().getLocal().getId()
							: null;

			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Manutencao #" + itemSelecionado.getId() + " finalizada!"));

			LOGGER.info("Manutencao finalizada: ID " + itemSelecionado.getId());

			itemSelecionado = null;
			solucaoAplicada = "";
			observacoesFinais = "";
			bbsNearMiss = false;
			abrirCorretivaAposFinalizar = false;
			periodosTrabalho = new ArrayList<>();

			if (deveAbrirCorretiva) {
				if ("MAQUINA".equals(tipoFinalizada) && maquinaId != null) {
					return "/solicitacaoManutencao/solicitacaoManutencaoMaquina.xhtml?faces-redirect=true&maquinaId=" + maquinaId
							+ "&origemPreventivaId=" + idFinalizada;
				} else if ("PREDIAL".equals(tipoFinalizada) && localId != null) {
					return "/solicitacaoManutencao/solicitacaoManutencaoPredial.xhtml?faces-redirect=true&localId=" + localId
							+ "&origemPreventivaId=" + idFinalizada;
				}
			}

			return null;

		} catch (Exception e) {
			LOGGER.error("Erro ao finalizar manutenção: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao finalizar manutenção: " + e.getMessage()));
			FacesContext.getCurrentInstance().validationFailed();
			return null;
		}
	}

	public void salvarPeriodosSemFinalizar() {
		try {
			LOGGER.info("Iniciando salvamento de períodos sem finalizar - ID: " + itemSelecionado.getId());

			if (!validarTodosPeriodos()) {
				LOGGER.warn("Validação de períodos falhou - não prosseguindo com salvamento");
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			List<PeriodoTrabalho> periodosValidos = periodosTrabalho.stream()
					.filter(p -> p.getDataHoraInicio() != null && p.getDataHoraFim() != null
							&& p.getDataHoraFim().after(p.getDataHoraInicio()) && p.getMensagemValidacao() == null)
					.collect(Collectors.toList());

			if (periodosValidos.isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro de Validacao",
								"Nenhum período de trabalho válido encontrado. Verifique as datas informadas."));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			for (int i = 0; i < periodosValidos.size(); i++) {
				PeriodoTrabalho p = periodosValidos.get(i);
				if (p.getTecnicoResponsavelId() == null) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Erro de Validacao", "Periodo " + (i + 1) + ": Tecnico responsavel deve ser selecionado."));
					FacesContext.getCurrentInstance().validationFailed();
					return;
				}
			}

			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(itemSelecionado.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			manterPeriodoTrabalhoManutencaoService.excluirTodosPorSolicitacao(itemSelecionado.getId(), tipoPeriodo);

			for (PeriodoTrabalho periodo : periodosValidos) {
				CadastroUsuario tecnicoSelecionado = buscarTecnicoPorId(periodo.getTecnicoResponsavelId());

				if (tecnicoSelecionado == null) {
					LOGGER.warn("Técnico com ID {} não encontrado", periodo.getTecnicoResponsavelId());
				}

				manterPeriodoTrabalhoManutencaoService.adicionarPeriodoComTecnicoComDiagnostico(itemSelecionado.getId(),
						tipoPeriodo, periodo.getDataHoraInicio(), periodo.getDataHoraFim(),
						periodo.getDescricaoAtividade(), tecnicoSelecionado, periodo.getUtilizadoParaDiagnostico());
			}

			// Salvar bbsNearMiss no controle de status
			TipoManutencaoReferencia tipoControle = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			manterControleStatusManutencaoService.salvarBbsNearMiss(itemSelecionado.getId(), tipoControle, bbsNearMiss);

			// Salvar solução aplicada e observações finais informadas até agora,
			// sem finalizar a manutenção (o técnico pode continuar depois)
			manterControleStatusManutencaoService.salvarSolucaoObservacoes(itemSelecionado.getId(), tipoControle,
					solucaoAplicada, observacoesFinais);

			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Periodos de trabalho salvos! Manutencao #" + itemSelecionado.getId() + " continua em andamento."));

			LOGGER.info("Períodos salvos para manutenção ID: " + itemSelecionado.getId());

			itemSelecionado = null;
			solucaoAplicada = "";
			observacoesFinais = "";
			bbsNearMiss = false;
			periodosTrabalho = new ArrayList<>();

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar períodos: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao salvar períodos: " + e.getMessage()));
			FacesContext.getCurrentInstance().validationFailed();
		}
	}

	private CadastroUsuario buscarTecnicoPorId(Long tecnicoId) {
		if (tecnicoId == null) {
			return null;
		}
		return tecnicosDisponiveis.stream().filter(t -> t.getId().equals(tecnicoId)).findFirst().orElse(null);
	}

	public void adicionarPeriodoTrabalho() {
		try {
			LOGGER.info("Adicionando período. Lista atual tem: " + periodosTrabalho.size() + " períodos");
			PeriodoTrabalho novoPeriodo = new PeriodoTrabalho();
			periodosTrabalho.add(novoPeriodo);
			LOGGER.info("Periodo adicionado com sucesso. Nova lista tem: " + periodosTrabalho.size() + " períodos");
		} catch (Exception e) {
			LOGGER.error("Erro ao adicionar período: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao adicionar novo período de trabalho: " + e.getMessage()));
		}
	}

	public void removerPeriodoTrabalho(int index) {
		try {
			LOGGER.info("Tentando remover período no índice: " + index + ". Lista atual tem: " + periodosTrabalho.size()
					+ " períodos");

			if (periodosTrabalho == null || periodosTrabalho.isEmpty()) {
				LOGGER.warn("Lista de períodos está vazia ou nula");
				return;
			}

			if (periodosTrabalho.size() <= 1) {
				LOGGER.warn("Não é possível remover. Deve ter pelo menos um período");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "Aviso",
						"Deve ter pelo menos um período de trabalho"));
				return;
			}

			if (index < 0 || index >= periodosTrabalho.size()) {
				LOGGER.warn("Índice inválido: " + index + ". Tamanho da lista: " + periodosTrabalho.size());
				return;
			}

			periodosTrabalho.remove(index);
			LOGGER.info("Periodo removido com sucesso. Lista agora tem: " + periodosTrabalho.size() + " períodos");

		} catch (Exception e) {
			LOGGER.error("Erro ao remover período: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao remover período de trabalho: " + e.getMessage()));
		}
	}

	public void cancelarManutencao(ItemManutencao item) {
		try {
			TipoManutencaoReferencia tipo = "MAQUINA".equals(item.getTipo()) ? TipoManutencaoReferencia.MAQUINA : TipoManutencaoReferencia.PREDIAL;

			// Reseta o controle de status (status, data de início, data de
			// conclusão, solução, observações finais, BBS/Near Miss)
			manterControleStatusManutencaoService.cancelarManutencao(item.getId(), tipo);

			// Apaga todos os períodos de trabalho registrados até agora
			// (isso também zera, automaticamente, os técnicos responsáveis
			// e o total de horas, já que são calculados a partir dos
			// períodos). As solicitações de compra NÃO são tocadas.
			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(item.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			manterPeriodoTrabalhoManutencaoService.excluirTodosPorSolicitacao(item.getId(), tipoPeriodo);

			// Se for máquina, limpa também o horímetro da falha e a marcação
			// de manutenção elétrica preenchidos ao iniciar a manutenção
			if ("MAQUINA".equals(item.getTipo()) && item.getSolicitacaoMaquina() != null) {
				SolicitacaoManutencaoMaquina solicitacao = item.getSolicitacaoMaquina();
				solicitacao.setHorimetroFalha(null);
				solicitacao.setManutencaoEletrica(false);
				manterSolicitacaoManutencaoMaquinaService.salvar(solicitacao);
			}

			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Manutencao #" + item.getId()
							+ " cancelada e retornada para 'Para Fazer'! Os dados técnicos preenchidos (período de trabalho, horímetro, técnicos, etc.) foram apagados. As solicitações de compra foram mantidas."));

			LOGGER.info("Manutencao cancelada e retornada: ID " + item.getId());
		} catch (Exception e) {
			LOGGER.error("Erro ao cancelar manutenção: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao cancelar manutenção: " + e.getMessage()));
		}
	}

	public void reativarManutencao(ItemManutencao item) {
		try {
			if ("MAQUINA".equals(item.getTipo()) && item.getSolicitacaoMaquina() != null) {
				SolicitacaoManutencaoMaquina solicitacao = item.getSolicitacaoMaquina();
				solicitacao.setCancelada(false);
				solicitacao.setMotivoCancelamento(null);
				solicitacao.setDataCancelamento(null);
				manterSolicitacaoManutencaoMaquinaService.salvar(solicitacao);
			} else if ("PREDIAL".equals(item.getTipo()) && item.getSolicitacaoPredial() != null) {
				SolicitacaoManutencaoPredial solicitacao = item.getSolicitacaoPredial();
				solicitacao.setCancelada(false);
				solicitacao.setMotivoCancelamento(null);
				solicitacao.setDataCancelamento(null);
				manterSolicitacaoManutencaoPredialService.salvar(solicitacao);
			}

			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Manutencao #" + item.getId() + " reativada!"));

			LOGGER.info("Manutencao reativada: ID " + item.getId());

		} catch (Exception e) {
			LOGGER.error("Erro ao reativar manutenção: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao reativar manutenção: " + e.getMessage()));
		}
	}

	// ========== METODOS PARA O MODAL DE DETALHES ==========

	public void prepararDetalhes(ItemManutencao item) {
		this.itemSelecionado = item;
		LOGGER.info("Preparando detalhes para item ID: " + item.getId());

		try {
			TipoManutencaoReferencia tipoRef = "MAQUINA".equals(item.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			List<HistoricoReprogramacao> historico = manterHistoricoReprogramacaoService
					.buscarHistoricoPorSolicitacao(item.getId(), tipoRef);
			this.historicoReprogramacoesSelecionado = (historico != null) ? historico : new ArrayList<>();
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar historico de reprogramacoes: ", e);
			this.historicoReprogramacoesSelecionado = new ArrayList<>();
		}
	}

	public List<HistoricoReprogramacao> getHistoricoReprogramacoesSelecionado() {
		return historicoReprogramacoesSelecionado;
	}

	public String truncateText(String text, int maxLength) {
		if (text == null || text.length() <= maxLength) {
			return text;
		}
		return text.substring(0, maxLength);
	}

	public boolean needsTruncation(String text, int maxLength) {
		return text != null && text.length() > maxLength;
	}

	public String getStatusLabel(StatusManutencao status) {
		if (status == null) {
			return "INDEFINIDO";
		}

		switch (status) {
		case PARA_FAZER:
			return "PARA FAZER";
		case EM_ANDAMENTO:
			return "EM ANDAMENTO";
		case CONCLUIDA:
			return "CONCLUÍDA";
		case CANCELADA:
			return "CANCELADA";
		case AGUARDANDO_COMPRA:
			return "AGUARDANDO COMPRA";
		default:
			return status.name();
		}
	}

	public int getRemainingChars(String text, int maxLength) {
		int currentLength = (text != null) ? text.length() : 0;
		return Math.max(0, maxLength - currentLength);
	}

	// ========== METODOS PARA BUSCAR TECNICOS RESPONSAVEIS ==========

	public String getTecnicosResponsaveis(ItemManutencao item) {
		try {
			if (item == null || item.getId() == null) {
				return "Não informado";
			}

			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(item.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			List<PeriodoTrabalhoManutencao> periodos = manterPeriodoTrabalhoManutencaoService
					.buscarPorSolicitacao(item.getId(), tipoPeriodo);

			if (periodos == null || periodos.isEmpty()) {
				return "Não informado";
			}

			Set<String> tecnicosUnicos = new LinkedHashSet<>();

			for (PeriodoTrabalhoManutencao periodo : periodos) {
				if (periodo.getTecnicoResponsavel() != null) {
					String nomeCompleto = periodo.getTecnicoResponsavel().getNome();
					String registro = periodo.getTecnicoResponsavel().getNuRegistro();

					if (registro != null && !registro.trim().isEmpty()) {
						tecnicosUnicos.add(nomeCompleto + " (" + registro + ")");
					} else {
						tecnicosUnicos.add(nomeCompleto);
					}
				}
			}

			if (tecnicosUnicos.isEmpty()) {
				return "Não informado";
			}

			if (tecnicosUnicos.size() == 1) {
				return tecnicosUnicos.iterator().next();
			} else {
				return String.join(", ", tecnicosUnicos);
			}

		} catch (Exception e) {
			LOGGER.warn("Erro ao buscar técnicos responsáveis para manutenção {}: {}",
					item != null ? item.getId() : "null", e.getMessage());
			return "Erro ao carregar";
		}
	}

	public List<String> getTecnicosResponsaveisList(ItemManutencao item) {
		try {
			if (item == null || item.getId() == null) {
				return new ArrayList<>();
			}

			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(item.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			List<PeriodoTrabalhoManutencao> periodos = manterPeriodoTrabalhoManutencaoService
					.buscarPorSolicitacao(item.getId(), tipoPeriodo);

			if (periodos == null || periodos.isEmpty()) {
				return new ArrayList<>();
			}

			Set<String> tecnicosUnicos = new LinkedHashSet<>();

			for (PeriodoTrabalhoManutencao periodo : periodos) {
				if (periodo.getTecnicoResponsavel() != null) {
					String nomeCompleto = periodo.getTecnicoResponsavel().getNome();
					String registro = periodo.getTecnicoResponsavel().getNuRegistro();

					if (registro != null && !registro.trim().isEmpty()) {
						tecnicosUnicos.add(nomeCompleto + " (" + registro + ")");
					} else {
						tecnicosUnicos.add(nomeCompleto);
					}
				}
			}

			return new ArrayList<>(tecnicosUnicos);

		} catch (Exception e) {
			LOGGER.warn("Erro ao buscar lista de técnicos responsáveis para manutenção {}: {}",
					item != null ? item.getId() : "null", e.getMessage());
			return new ArrayList<>();
		}
	}

	public boolean hasMultiplosTecnicos(ItemManutencao item) {
		return getTecnicosResponsaveisList(item).size() > 1;
	}

	public int getQuantidadeTecnicos(ItemManutencao item) {
		return getTecnicosResponsaveisList(item).size();
	}

	// ========== METODOS PARA SOLICITACAO DE COMPRA ==========

	public void enviarSolicitacaoCompra() {
		try {
			LOGGER.info("Iniciando envio de solicitacao de compra");

			if (itemSelecionado == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma manutenção selecionada"));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			if (tipoSolicitacaoCompra == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Selecione o tipo de necessidade (Material, Servico ou Ambos)"));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			if (descricaoSolicitacaoCompra == null || descricaoSolicitacaoCompra.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Descreva detalhadamente os materiais/serviços necessários"));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			if (!confirmacaoSolicitacao) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Voce deve confirmar que as informacoes estao completas"));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			if (tecnicoSolicitanteId == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Selecione o técnico que está solicitando a compra"));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			CadastroUsuario tecnicoSolicitante = buscarTecnicoPorId(tecnicoSolicitanteId);
			if (tecnicoSolicitante == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Técnico selecionado não encontrado. Tente novamente."));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			TipoManutencaoReferencia tipoManutencaoRef = "MAQUINA".equals(
					itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
							: TipoManutencaoReferencia.PREDIAL;

			boolean jaExistePendente = manterSolicitacaoCompraService
					.existeSolicitacaoPendentePorManutencao(itemSelecionado.getId(), tipoManutencaoRef);

			if (jaExistePendente) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_WARN, "Atencao",
								"Já existe uma solicitação de compra pendente para esta manutenção. "
										+ "Aguarde o processamento pelo administrador."));
				FacesContext.getCurrentInstance().validationFailed();
				return;
			}

			SolicitacaoCompraMateriaisServicos novaSolicitacao = manterSolicitacaoCompraService.criarSolicitacao(
					itemSelecionado.getId(), tipoManutencaoRef, tecnicoSolicitante, tipoSolicitacaoCompra,
					descricaoSolicitacaoCompra.trim());

			if (novaSolicitacao != null) {
				atualizarStatusManutencaoParaAguardandoCompra();

				// ADICIONAR ESTE BLOCO - ENVIO DE NOTIFICAÇÃO POR EMAIL
				try {
					String maquinaOuLocal = obterNomeMaquinaOuLocal();

					notificacaoService.notificarSolicitacaoCompra(tipoSolicitacaoCompra.getDescricao(),
							descricaoSolicitacaoCompra.trim(), tecnicoSolicitante.getNome(), itemSelecionado.getId(),
							itemSelecionado.getTipo(), maquinaOuLocal);

					LOGGER.info("Notificacao de solicitacao de compra enviada por email");
				} catch (Exception emailEx) {
					// Não falha o processo principal se o email falhar
					LOGGER.error("Erro ao enviar notificação por email (processo principal não foi afetado): ",
							emailEx);
				}
				// FIM DO BLOCO DE NOTIFICAÇÃO

				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Solicitação Enviada com Sucesso!",
						"ID da Solicitacao: " + novaSolicitacao.getId() + ". "
								+ "A manutenção foi pausada e aguarda processamento da compra pelo administrador."));

				LOGGER.info("Solicitacao de compra criada com sucesso - ID: {}, Manutencao: {} ({}), Tecnico: {}",
						novaSolicitacao.getId(), itemSelecionado.getId(), itemSelecionado.getTipo(),
						tecnicoSolicitante.getNome());

				limparCamposSolicitacaoCompra();
				carregarDados();

			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Falha ao criar solicitacao de compra. Tente novamente."));
				FacesContext.getCurrentInstance().validationFailed();
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao enviar solicitacao de compra: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro Interno", "Erro ao processar solicitacao: " + e.getMessage()));
			FacesContext.getCurrentInstance().validationFailed();
		}
	}

	private void atualizarStatusManutencaoParaAguardandoCompra() {
		try {
			if (itemSelecionado == null) {
				LOGGER.warn("itemSelecionado é null, não é possível atualizar status");
				return;
			}

			TipoManutencaoReferencia tipo = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			manterControleStatusManutencaoService.criarOuAtualizar(itemSelecionado.getId(), tipo,
					StatusManutencao.AGUARDANDO_COMPRA);

			itemSelecionado.setStatus(StatusManutencao.AGUARDANDO_COMPRA);

			LOGGER.info("Status da manutenção {} alterado para AGUARDANDO_COMPRA", itemSelecionado.getId());

		} catch (Exception e) {
			LOGGER.error("Erro ao atualizar status da manutenção para AGUARDANDO_COMPRA: ", e);
		}
	}

	public void limparCamposSolicitacaoCompra() {
		this.tipoSolicitacaoCompra = null;
		this.descricaoSolicitacaoCompra = null;
		this.confirmacaoSolicitacao = false;
		this.tecnicoSolicitanteId = null; // ADICIONAR ESTA LINHA
		LOGGER.debug("Campos de solicitacao de compra limpos");
	}

	public void prepararSolicitacaoCompra(ItemManutencao item) {
		this.itemSelecionado = item;
		limparCamposSolicitacaoCompra();

		LOGGER.debug("Formulario de solicitacao de compra preparado para item {}",
				item != null ? item.getId() : "null");
	}

	public boolean isAguardandoCompra(ItemManutencao item) {
		if (item == null) {
			return false;
		}
		return StatusManutencao.AGUARDANDO_COMPRA.equals(item.getStatus());
	}

	public String getTextoStatusFormatado(ItemManutencao item) {
		if (item == null || item.getStatus() == null) {
			return "Desconhecido";
		}

		switch (item.getStatus()) {
		case PARA_FAZER:
			return "📋 Para Fazer";
		case EM_ANDAMENTO:
			return "⚙️ Em Andamento";
		case AGUARDANDO_COMPRA:
			return "🛒 Aguardando Compra";
		case CONCLUIDA:
			return "Concluída";
		case CANCELADA:
			return "❌ Cancelada";
		default:
			return item.getStatus().toString();
		}
	}

	public boolean existemSolicitacoesCompra() {
		if (itemSelecionado == null) {
			return false;
		}

		try {
			TipoManutencaoReferencia tipoManutencaoRef = "MAQUINA".equals(
					itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
							: TipoManutencaoReferencia.PREDIAL;

			List<SolicitacaoCompraMateriaisServicos> solicitacoes = manterSolicitacaoCompraService
					.buscarPorSolicitacaoManutencao(itemSelecionado.getId(), tipoManutencaoRef);

			return !solicitacoes.isEmpty();

		} catch (Exception e) {
			LOGGER.error("Erro ao verificar solicitacoes de compra: ", e);
			return false;
		}
	}

	public List<SolicitacaoCompraMateriaisServicos> getSolicitacoesCompraAtual() {
		if (itemSelecionado == null) {
			return new ArrayList<>();
		}

		try {
			TipoManutencaoReferencia tipoManutencaoRef = "MAQUINA".equals(
					itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
							: TipoManutencaoReferencia.PREDIAL;

			return manterSolicitacaoCompraService.buscarPorSolicitacaoManutencao(itemSelecionado.getId(),
					tipoManutencaoRef);

		} catch (Exception e) {
			LOGGER.error("Erro ao buscar solicitacoes de compra: ", e);
			return new ArrayList<>();
		}
	}

	public SolicitacaoCompraMateriaisServicos.TipoSolicitacao[] getTiposSolicitacaoCompra() {
		return SolicitacaoCompraMateriaisServicos.TipoSolicitacao.values();
	}

	public boolean isPodeSolicitarCompra() {
		return itemSelecionado != null && !itemSelecionado.isCancelada();
	}

	public long getContadorSolicitacoesPendentes() {
		if (itemSelecionado == null) {
			return 0;
		}

		try {
			List<SolicitacaoCompraMateriaisServicos> solicitacoes = getSolicitacoesCompraAtual();
			return solicitacoes.stream().filter(
					s -> s.getStatusSolicitacao() == SolicitacaoCompraMateriaisServicos.StatusSolicitacao.PENDENTE)
					.count();
		} catch (Exception e) {
			LOGGER.error("Erro ao contar solicitacoes pendentes: ", e);
			return 0;
		}
	}

	public void onSolicitacaoCompraProcessada(Long solicitacaoCompraId) {
		try {
			SolicitacaoCompraMateriaisServicos solicitacao = manterSolicitacaoCompraService
					.buscarPorId(solicitacaoCompraId);

			if (solicitacao != null && solicitacao
					.getStatusSolicitacao() == SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Materiais/Servicos Disponiveis",
								"A solicitação de compra foi processada. Você pode continuar a manutenção."));

				LOGGER.info("Solicitacao de compra {} processada. Manutencao {} pode continuar.", solicitacaoCompraId,
						itemSelecionado.getId());
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao processar callback de solicitacao de compra: ", e);
		}
	}

	public void inicializarCamposCompra() {
		limparCamposSolicitacaoCompra();
	}

	// ========== METODOS DE FILTROS PARA AGUARDANDO COMPRA ==========

	public List<ItemManutencao> getManutencoesEmAndamentoDisponiveis() {
		List<ItemManutencao> manutencoesEmAndamento = manutencoesFiltradas.getOrDefault("EM_ANDAMENTO",
				new ArrayList<>());

		if (manutencoesEmAndamento == null || manutencoesEmAndamento.isEmpty()) {
			return new ArrayList<>();
		}

		return manutencoesEmAndamento.stream()
				.filter(item -> !StatusManutencao.AGUARDANDO_COMPRA.equals(item.getStatus()))
				.collect(Collectors.toList());
	}

	public List<ItemManutencao> getManutencoesAguardandoCompra() {
		List<ItemManutencao> aguardandoCompra = manutencoesFiltradas.getOrDefault("AGUARDANDO_COMPRA",
				new ArrayList<>());

		if (aguardandoCompra != null && !aguardandoCompra.isEmpty()) {
			return aguardandoCompra;
		}

		List<ItemManutencao> manutencoesEmAndamento = manutencoesFiltradas.getOrDefault("EM_ANDAMENTO",
				new ArrayList<>());

		if (manutencoesEmAndamento == null || manutencoesEmAndamento.isEmpty()) {
			return new ArrayList<>();
		}

		return manutencoesEmAndamento.stream()
				.filter(item -> StatusManutencao.AGUARDANDO_COMPRA.equals(item.getStatus()))
				.collect(Collectors.toList());
	}

	private String obterNomeMaquinaOuLocal() {
		try {
			if (itemSelecionado == null) {
				return "Não informado";
			}

			if ("MAQUINA".equals(itemSelecionado.getTipo())) {
				SolicitacaoManutencaoMaquina manutencaoMaquina = manterSolicitacaoManutencaoMaquinaService
						.buscarPorId(itemSelecionado.getId());

				if (manutencaoMaquina != null && manutencaoMaquina.getMaquina() != null) {
					CadastroMaquinaEquipamento maquina = manutencaoMaquina.getMaquina();
					// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
					// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
					// na entidade CadastroMaquinaEquipamento.
					return maquina.getIdentificacao();
				}

			} else if ("PREDIAL".equals(itemSelecionado.getTipo())) {
				SolicitacaoManutencaoPredial manutencaoPredial = manterSolicitacaoManutencaoPredialService
						.buscarPorId(itemSelecionado.getId());

				if (manutencaoPredial != null && manutencaoPredial.getLocal() != null) {
					return manutencaoPredial.getLocal().getNome();
				}
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao obter nome da maquina/local: ", e);
		}

		return "Não informado";
	}

	public String getUrlFotoCorrigida() {
		if (itemSelecionado == null) {
			return null;
		}

		String urlFoto = null;

		// 1. Obtém a URL da foto baseada no tipo de solicitação
		if ("MAQUINA".equals(itemSelecionado.getTipo()) && itemSelecionado.getSolicitacaoMaquina() != null) {
			urlFoto = itemSelecionado.getSolicitacaoMaquina().getUrlFoto();
		} else if ("PREDIAL".equals(itemSelecionado.getTipo()) && itemSelecionado.getSolicitacaoPredial() != null) {
			urlFoto = itemSelecionado.getSolicitacaoPredial().getUrlFoto();
		}

		if (urlFoto == null || urlFoto.trim().isEmpty()) {
			return null;
		}

		// 2. Corrige barras invertidas
		String urlCorrigida = urlFoto.replace('\\', '/');

		// 3. TRATAMENTO SIMPLIFICADO PARA URLs ANTIGAS (C:/...)
		if (urlCorrigida.startsWith("C:/")) {
			// Extrai apenas o nome do arquivo
			String nomeArquivo = urlCorrigida.substring(urlCorrigida.lastIndexOf('/') + 1);

			// Determina o diretório correto
			String diretorioBase = "MAQUINA".equals(itemSelecionado.getTipo()) ? Uploads.url(Uploads.SOLICITACOES_MAQUINAS)
					: Uploads.url(Uploads.SOLICITACOES_PREDIAL);

			// IMPORTANTE: Retorna a URL que será processada pelo servlet
			// O servlet está mapeado em /uploads/*
			return diretorioBase + nomeArquivo;
		}

		// 4. Para URLs já corretas (/uploads/...)
		if (urlCorrigida.startsWith("/uploads/")) {
			// Apenas retorna - o servlet vai processar
			return urlCorrigida;
		}

		// 5. Qualquer outro caso
		return urlCorrigida;
	}

	/**
	 * Verifica se o item selecionado tem uma imagem anexada
	 */
	public boolean temImagemAnexada() {
		if (itemSelecionado == null) {
			return false;
		}

		String urlFoto = null;

		if ("MAQUINA".equals(itemSelecionado.getTipo()) && itemSelecionado.getSolicitacaoMaquina() != null) {
			urlFoto = itemSelecionado.getSolicitacaoMaquina().getUrlFoto();
		} else if ("PREDIAL".equals(itemSelecionado.getTipo()) && itemSelecionado.getSolicitacaoPredial() != null) {
			urlFoto = itemSelecionado.getSolicitacaoPredial().getUrlFoto();
		}

		return urlFoto != null && !urlFoto.trim().isEmpty();
	}

	// ========== MÉTODOS PARA REINÍCIO DE MANUTENÇÃO CONCLUÍDA ==========

	public void prepararReinicioManutencao(ItemManutencao item) {
		this.itemSelecionado = item;
		this.primeiraConfirmacaoReinicio = false;
		LOGGER.info("Preparando reinício para manutenção ID: {}", item.getId());
	}

	public void confirmarPrimeiroReinicio() {
		this.primeiraConfirmacaoReinicio = true;
		LOGGER.info("Primeira confirmação de reinício aceita para manutenção ID: {}",
				itemSelecionado != null ? itemSelecionado.getId() : "null");
	}

	public void confirmarReinicioManutencao() {
		try {
			if (itemSelecionado == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma manutenção selecionada."));
				return;
			}

			LOGGER.info("Executando reinício da manutenção ID: {}", itemSelecionado.getId());

			TipoManutencaoReferencia tipoControle = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			TipoManutencaoReferencia tipoPeriodo = "MAQUINA".equals(itemSelecionado.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;

			// 1. Excluir todos os períodos de trabalho (descrições de atividades)
			manterPeriodoTrabalhoManutencaoService.excluirTodosPorSolicitacao(itemSelecionado.getId(), tipoPeriodo);
			LOGGER.info("Períodos de trabalho excluídos para manutenção ID: {}", itemSelecionado.getId());

			// 2. Resetar o controle de status: voltar para PARA_FAZER, limpar datas e
			// solução
			ControleStatusManutencao controle = manterControleStatusManutencaoService
					.buscarPorSolicitacao(itemSelecionado.getId(), tipoControle);

			if (controle != null) {
				controle.setStatus(StatusManutencao.PARA_FAZER);
				controle.setDataInicio(null);
				controle.setDataConclusao(null);
				controle.setSolucao(null);
				controle.setObservacoesFinais(null);
				manterControleStatusManutencaoService.salvar(controle);
				LOGGER.info("Status da manutenção ID: {} resetado para PARA_FAZER", itemSelecionado.getId());
			}

			// 3. Recarregar dados
			carregarDados();

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Manutenção Reiniciada", "Manutenção #"
							+ itemSelecionado.getId()
							+ " foi reiniciada e voltou para 'Para Fazer'. Todos os períodos e soluções foram apagados."));

			// Limpar estado
			itemSelecionado = null;
			primeiraConfirmacaoReinicio = false;

		} catch (Exception e) {
			LOGGER.error("Erro ao reiniciar manutenção: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao reiniciar manutenção: " + e.getMessage()));
		}
	}

	public void cancelarReinicio() {
		this.primeiraConfirmacaoReinicio = false;
		this.itemSelecionado = null;
		LOGGER.info("Reinício de manutenção cancelado pelo usuário.");
	}

	// ========== MÉTODOS PARA BUSCA DE MANUTENÇÕES CONCLUÍDAS ==========

	public List<ItemManutencao> getManutencoesConcluidasComBusca() {
		List<ItemManutencao> concluidas = manutencoesFiltradas.getOrDefault("CONCLUIDA", new ArrayList<>());

		if (concluidas == null || concluidas.isEmpty()) {
			return new ArrayList<>();
		}

		// Se a busca está ativa, retorna TODAS as concluídas filtradas pelo texto
		if (buscaConcluidaAtiva && filtroBuscaConcluida != null && !filtroBuscaConcluida.trim().isEmpty()) {
			String busca = filtroBuscaConcluida.trim().toLowerCase();

			// Verificar se a busca é uma data no formato dd/MM/yyyy
			Date dataBusca = tentarParseData(busca);

			return concluidas.stream().filter(item -> {
				// Busca por data (dd/MM/yyyy)
				if (dataBusca != null) {
					// Compara com data de conclusão
					if (item.getDataConclusao() != null && isSameDay(item.getDataConclusao(), dataBusca)) {
						return true;
					}
					// Compara com data de manutenção
					if (item.getDataManutencao() != null && isSameDay(item.getDataManutencao(), dataBusca)) {
						return true;
					}
					// Compara com data de início
					if (item.getDataInicio() != null && isSameDay(item.getDataInicio(), dataBusca)) {
						return true;
					}
					return false;
				}

				// Busca por ID
				if (String.valueOf(item.getId()).contains(busca)) {
					return true;
				}
				// Busca por falha/problema
				if (item.getFalha() != null && item.getFalha().toLowerCase().contains(busca)) {
					return true;
				}
				if (item.getProblema() != null && item.getProblema().toLowerCase().contains(busca)) {
					return true;
				}
				// Busca por máquina/local
				if (item.getMaquina() != null && item.getMaquina().toLowerCase().contains(busca)) {
					return true;
				}
				if (item.getLocal() != null && item.getLocal().toLowerCase().contains(busca)) {
					return true;
				}
				// Busca por solução
				if (item.getSolucao() != null && item.getSolucao().toLowerCase().contains(busca)) {
					return true;
				}
				return false;
			}).collect(Collectors.toList());
		}

		// Comportamento padrão: apenas concluídas do dia
		Date hoje = new Date();
		Calendar calHoje = Calendar.getInstance();
		calHoje.setTime(hoje);

		return concluidas.stream().filter(item -> {
			if (item.getDataConclusao() == null) {
				return false;
			}

			Calendar calConclusao = Calendar.getInstance();
			calConclusao.setTime(item.getDataConclusao());

			return calHoje.get(Calendar.YEAR) == calConclusao.get(Calendar.YEAR)
					&& calHoje.get(Calendar.MONTH) == calConclusao.get(Calendar.MONTH)
					&& calHoje.get(Calendar.DAY_OF_MONTH) == calConclusao.get(Calendar.DAY_OF_MONTH);
		}).collect(Collectors.toList());
	}

	/**
	 * Tenta interpretar a string como uma data no formato dd/MM/yyyy. Retorna null
	 * se não for uma data válida.
	 */
	private Date tentarParseData(String texto) {
		if (texto == null || texto.trim().isEmpty()) {
			return null;
		}
		String textoLimpo = texto.trim();
		// Verifica se parece com data (contém /)
		if (!textoLimpo.contains("/")) {
			return null;
		}
		try {
			String[] partes = textoLimpo.split("/");
			if (partes.length != 3) {
				return null;
			}
			int dia = Integer.parseInt(partes[0].trim());
			int mes = Integer.parseInt(partes[1].trim());
			int ano = Integer.parseInt(partes[2].trim());

			if (dia < 1 || dia > 31 || mes < 1 || mes > 12 || ano < 2000 || ano > 2100) {
				return null;
			}

			Calendar cal = Calendar.getInstance();
			cal.setLenient(false);
			cal.set(ano, mes - 1, dia, 0, 0, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} catch (Exception e) {
			return null;
		}
	}

	public void buscarConcluidas() {
		if (filtroBuscaConcluida != null && !filtroBuscaConcluida.trim().isEmpty()) {
			buscaConcluidaAtiva = true;
			LOGGER.info("Busca de concluídas ativada com termo: {}", filtroBuscaConcluida);
		} else {
			buscaConcluidaAtiva = false;
		}
	}

	public void limparBuscaConcluidas() {
		filtroBuscaConcluida = "";
		buscaConcluidaAtiva = false;
		LOGGER.info("Busca de concluídas limpa.");
	}

	public int getTotalConcluidasComBusca() {
		return getManutencoesConcluidasComBusca().size();
	}

	public String getLabelConcluidas() {
		if (buscaConcluidaAtiva && filtroBuscaConcluida != null && !filtroBuscaConcluida.trim().isEmpty()) {
			return "Resultado da Busca";
		}
		return "Concluídas Hoje";
	}

	public List<ItemManutencao> getManutencoesConcluidas() {
		List<ItemManutencao> concluidas = manutencoesFiltradas.getOrDefault("CONCLUIDA", new ArrayList<>());

		if (concluidas == null || concluidas.isEmpty()) {
			return new ArrayList<>();
		}

		Date hoje = new Date();
		Calendar calHoje = Calendar.getInstance();
		calHoje.setTime(hoje);

		return concluidas.stream().filter(item -> {
			if (item.getDataConclusao() == null) {
				return false;
			}

			Calendar calConclusao = Calendar.getInstance();
			calConclusao.setTime(item.getDataConclusao());

			return calHoje.get(Calendar.YEAR) == calConclusao.get(Calendar.YEAR)
					&& calHoje.get(Calendar.MONTH) == calConclusao.get(Calendar.MONTH)
					&& calHoje.get(Calendar.DAY_OF_MONTH) == calConclusao.get(Calendar.DAY_OF_MONTH);
		}).collect(Collectors.toList());
	}

	public long getContadorAguardandoCompra() {
		return getManutencoesAguardandoCompra().size();
	}

	public int getTotalEmAndamentoReal() {
		return getManutencoesEmAndamentoDisponiveis().size();
	}

	// ========== CONTADORES, AUXILIARES E GETTERS/SETTERS ==========

	public int getTotalParaFazer() {
		return manutencoesFiltradas.getOrDefault("PARA_FAZER", new ArrayList<>()).size();
	}

	public int getTotalConcluidasHoje() {
		return getManutencoesConcluidas().size();
	}

	public int getTotalEmAndamento() {
		return manutencoesFiltradas.getOrDefault("EM_ANDAMENTO", new ArrayList<>()).size();
	}

	public int getTotalAguardandoCompra() {
		return manutencoesFiltradas.getOrDefault("AGUARDANDO_COMPRA", new ArrayList<>()).size();
	}

	public int getTotalConcluidas() {
		return manutencoesFiltradas.getOrDefault("CONCLUIDA", new ArrayList<>()).size();
	}

	public int getTotalCanceladas() {
		return manutencoesFiltradas.getOrDefault("CANCELADA", new ArrayList<>()).size();
	}

	public Long getTecnicoSolicitanteId() {
		return tecnicoSolicitanteId;
	}

	public void setTecnicoSolicitanteId(Long tecnicoSolicitanteId) {
		this.tecnicoSolicitanteId = tecnicoSolicitanteId;
	}

	public int getDescricaoAtividadeLength(String descricao) {
		return descricao != null ? descricao.length() : 0;
	}

	public int getSolucaoAplicadaLength() {
		return solucaoAplicada != null ? solucaoAplicada.length() : 0;
	}

	public int getObservacoesFinaisLength() {
		return observacoesFinais != null ? observacoesFinais.length() : 0;
	}

	public Date getDataAtual() {
		return new Date();
	}

	public void setTipoManutencaoParaFiltrar(String tipoStr) {
		try {
			this.tipoManutencaoFiltro = TipoManutencao.valueOf(tipoStr);
			LOGGER.info("Filtro de tipo de manutenção definido para: " + tipoManutencaoFiltro);
			carregarDados();
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Tipo de manutenção inválido: " + tipoStr);
			this.tipoManutencaoFiltro = null;
		}
	}

	// Getters e Setters (nomes das propriedades sem acento/cedilha)
	public List<ItemManutencao> getTodosItens() {
		return todosItens;
	}

	public void setTodosItens(List<ItemManutencao> todosItens) {
		this.todosItens = todosItens;
	}

	public Map<String, List<ItemManutencao>> getManutencoesFiltradas() {
		return manutencoesFiltradas;
	}

	public void setManutencoesFiltradas(Map<String, List<ItemManutencao>> manutencoesFiltradas) {
		this.manutencoesFiltradas = manutencoesFiltradas;
	}

	public String getFiltroTipo() {
		return filtroTipo;
	}

	public void setFiltroTipo(String filtroTipo) {
		this.filtroTipo = filtroTipo;
	}

	public String getFiltroPrioridade() {
		return filtroPrioridade;
	}

	public void setFiltroPrioridade(String filtroPrioridade) {
		this.filtroPrioridade = filtroPrioridade;
	}

	public Date getFiltroData() {
		return filtroData;
	}

	public void setFiltroData(Date filtroData) {
		this.filtroData = filtroData;
	}

	public ItemManutencao getItemSelecionado() {
		return itemSelecionado;
	}

	public void setItemSelecionado(ItemManutencao itemSelecionado) {
		this.itemSelecionado = itemSelecionado;
	}

	public String getSolucaoAplicada() {
		return solucaoAplicada;
	}

	public void setSolucaoAplicada(String solucaoAplicada) {
		this.solucaoAplicada = solucaoAplicada;
	}

	public String getObservacoesFinais() {
		return observacoesFinais;
	}

	public void setObservacoesFinais(String observacoesFinais) {
		this.observacoesFinais = observacoesFinais;
	}

	public List<PeriodoTrabalho> getPeriodosTrabalho() {
		return periodosTrabalho;
	}

	public void setPeriodosTrabalho(List<PeriodoTrabalho> periodosTrabalho) {
		this.periodosTrabalho = periodosTrabalho;
	}

	public TipoManutencao getTipoManutencaoFiltro() {
		return tipoManutencaoFiltro;
	}

	public void setTipoManutencaoFiltro(TipoManutencao tipoManutencaoFiltro) {
		this.tipoManutencaoFiltro = tipoManutencaoFiltro;
	}

	public List<CadastroUsuario> getTecnicosDisponiveis() {
		return tecnicosDisponiveis;
	}

	public void setTecnicosDisponiveis(List<CadastroUsuario> tecnicosDisponiveis) {
		this.tecnicosDisponiveis = tecnicosDisponiveis;
	}

	/**
	 * Monta o texto exibido nas listas de seleção de técnico, no formato
	 * "Nome (nuRegistro)" (ou só "Nome" quando não há registro).
	 */
	public String getLabelTecnico(CadastroUsuario tecnico) {
		if (tecnico == null) {
			return "";
		}
		if (tecnico.getNuRegistro() != null && !tecnico.getNuRegistro().trim().isEmpty()) {
			return tecnico.getNome() + " (" + tecnico.getNuRegistro() + ")";
		}
		return tecnico.getNome();
	}

	public SolicitacaoCompraMateriaisServicos.TipoSolicitacao getTipoSolicitacaoCompra() {
		return tipoSolicitacaoCompra;
	}

	public void setTipoSolicitacaoCompra(SolicitacaoCompraMateriaisServicos.TipoSolicitacao tipoSolicitacaoCompra) {
		this.tipoSolicitacaoCompra = tipoSolicitacaoCompra;
	}

	public String getDescricaoSolicitacaoCompra() {
		return descricaoSolicitacaoCompra;
	}

	public void setDescricaoSolicitacaoCompra(String descricaoSolicitacaoCompra) {
		this.descricaoSolicitacaoCompra = descricaoSolicitacaoCompra;
	}

	public boolean isConfirmacaoSolicitacao() {
		return confirmacaoSolicitacao;
	}

	public void setConfirmacaoSolicitacao(boolean confirmacaoSolicitacao) {
		this.confirmacaoSolicitacao = confirmacaoSolicitacao;
	}

	public Double getHorimetroFalha() {
		return horimetroFalha;
	}

	public void setHorimetroFalha(Double horimetroFalha) {
		this.horimetroFalha = horimetroFalha;
	}

	public Boolean getManutencaoEletrica() {
		return manutencaoEletrica;
	}

	public void setManutencaoEletrica(Boolean manutencaoEletrica) {
		this.manutencaoEletrica = manutencaoEletrica;
	}

	public String getFiltroBuscaId() {
		return filtroBuscaId;
	}

	public void setFiltroBuscaId(String filtroBuscaId) {
		this.filtroBuscaId = filtroBuscaId;
	}

	public boolean isPrimeiraConfirmacaoReinicio() {
		return primeiraConfirmacaoReinicio;
	}

	public void setPrimeiraConfirmacaoReinicio(boolean primeiraConfirmacaoReinicio) {
		this.primeiraConfirmacaoReinicio = primeiraConfirmacaoReinicio;
	}

	public String getFiltroBuscaConcluida() {
		return filtroBuscaConcluida;
	}

	public void setFiltroBuscaConcluida(String filtroBuscaConcluida) {
		this.filtroBuscaConcluida = filtroBuscaConcluida;
	}

	public boolean isBuscaConcluidaAtiva() {
		return buscaConcluidaAtiva;
	}

	public void setBuscaConcluidaAtiva(boolean buscaConcluidaAtiva) {
		this.buscaConcluidaAtiva = buscaConcluidaAtiva;
	}

	public boolean isBbsNearMiss() {
		return bbsNearMiss;
	}

	public void setBbsNearMiss(boolean bbsNearMiss) {
		this.bbsNearMiss = bbsNearMiss;
	}

	public boolean isAbrirCorretivaAposFinalizar() {
		return abrirCorretivaAposFinalizar;
	}

	public void setAbrirCorretivaAposFinalizar(boolean abrirCorretivaAposFinalizar) {
		this.abrirCorretivaAposFinalizar = abrirCorretivaAposFinalizar;
	}

	/**
	 * Retorna se o item atualmente nos detalhes é um BBS/Near Miss,
	 * consultando o controle de status no banco.
	 */
	public boolean isBbsNearMissItemSelecionado() {
		try {
			if (itemSelecionado == null) {
				return false;
			}
			TipoManutencaoReferencia tipo = "MAQUINA".equals(itemSelecionado.getTipo()) ? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			ControleStatusManutencao controle = manterControleStatusManutencaoService
					.buscarPorSolicitacao(itemSelecionado.getId(), tipo);
			return controle != null && controle.isBbsNearMiss();
		} catch (Exception e) {
			LOGGER.warn("Erro ao verificar BBS/Near Miss do item selecionado: {}", e.getMessage());
			return false;
		}
	}

	public java.util.List<SolicitacaoCompraMateriaisServicos> getComprasAssociadasItemSelecionado() {
		try {
			if (itemSelecionado == null) return new java.util.ArrayList<>();
			TipoManutencaoReferencia tipoRef =
					"MAQUINA".equals(itemSelecionado.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			java.util.List<SolicitacaoCompraMateriaisServicos> lista =
					manterSolicitacaoCompraService.buscarPorSolicitacaoManutencao(itemSelecionado.getId(), tipoRef);
			return lista != null ? lista : new java.util.ArrayList<>();
		} catch (Exception e) {
			LOGGER.warn("Erro ao buscar compras associadas: {}", e.getMessage());
			return new java.util.ArrayList<>();
		}
	}

	public java.util.List<PeriodoTrabalhoManutencao> getPeriodosTrabalhoItemSelecionado() {
		try {
			if (itemSelecionado == null) return new java.util.ArrayList<>();
			TipoManutencaoReferencia tipoPeriodo =
					"MAQUINA".equals(itemSelecionado.getTipo())
					? TipoManutencaoReferencia.MAQUINA
					: TipoManutencaoReferencia.PREDIAL;
			java.util.List<PeriodoTrabalhoManutencao> lista =
					manterPeriodoTrabalhoManutencaoService.buscarPorSolicitacao(itemSelecionado.getId(), tipoPeriodo);
			return lista != null ? lista : new java.util.ArrayList<>();
		} catch (Exception e) {
			LOGGER.warn("Erro ao buscar períodos de trabalho associados: {}", e.getMessage());
			return new java.util.ArrayList<>();
		}
	}

	// ========== MÉTODOS PARA ABRIR DIALOG INICIAR AUTOMATICAMENTE ==========

	private Long abrirIniciarId;
	private String abrirIniciarTipo;

	private boolean deveAbrirIniciar = false;

	public void verificarAbrirIniciar() {
		if (abrirIniciarId != null && abrirIniciarTipo != null) {
			try {
				// Buscar o item na lista de manutenções carregadas
				ItemManutencao itemEncontrado = null;
				for (ItemManutencao item : todosItens) {
					if (item.getId().equals(abrirIniciarId) && item.getTipo().equals(abrirIniciarTipo)) {
						itemEncontrado = item;
						break;
					}
				}
				if (itemEncontrado != null
						&& itemEncontrado.getStatus() == ControleStatusManutencao.StatusManutencao.PARA_FAZER) {
					prepararInicioManutencao(itemEncontrado);
					deveAbrirIniciar = true;
					LOGGER.info("Item preparado para iniciar automaticamente: ID " + abrirIniciarId);
				} else {
					deveAbrirIniciar = false;
					LOGGER.info("Item já iniciado ou não encontrado, não abrindo dialog: ID " + abrirIniciarId);
				}
			} catch (Exception e) {
				LOGGER.error("Erro ao preparar item para iniciar: ", e);
				deveAbrirIniciar = false;
			}
		}
		// Limpar parâmetros para não reabrir ao recarregar
		abrirIniciarId = null;
		abrirIniciarTipo = null;
	}

	public boolean isDeveAbrirIniciar() {
		return deveAbrirIniciar;
	}

	public Long getAbrirIniciarId() {
		return abrirIniciarId;
	}

	public void setAbrirIniciarId(Long abrirIniciarId) {
		this.abrirIniciarId = abrirIniciarId;
	}

	public String getAbrirIniciarTipo() {
		return abrirIniciarTipo;
	}

	public void setAbrirIniciarTipo(String abrirIniciarTipo) {
		this.abrirIniciarTipo = abrirIniciarTipo;
	}
}