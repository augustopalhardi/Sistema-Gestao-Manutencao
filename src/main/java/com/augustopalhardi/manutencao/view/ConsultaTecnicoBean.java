package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroUsuarioService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterHistoricoReprogramacaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.util.Uploads;
import com.augustopalhardi.manutencao.view.dto.ItemManutencaoTecnico;

@Named
@ViewScoped
public class ConsultaTecnicoBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsultaTecnicoBean.class);

	@Inject
	private ManterCadastroUsuarioService manterCadastroUsuarioService;

	@Inject
	private ManterPeriodoTrabalhoManutencaoService manterPeriodoTrabalhoService;

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterCompraService;

	@Inject
	private ManterHistoricoReprogramacaoService manterHistoricoReprogramacaoService;

	// Lista de técnicos
	private List<CadastroUsuario> todosTecnicos = new ArrayList<>();
	private List<CadastroUsuario> tecnicosFiltrados = new ArrayList<>();
	private String filtroNome = "";
	private String filtroStatus = "TODOS";
	private int filtroPeriodoMeses = 0; // 0 = todos, 3, 6, 12, 24

	// Técnico selecionado
	private CadastroUsuario tecnicoSelecionado;

	// Estatísticas do técnico selecionado
	private double totalHorasTrabalhadas;
	private int totalManutencoes;
	private double horasDiagnostico;
	private double custoMaoDeObra;

	private double horasCorretiva;
	private int qtdCorretiva;
	private double horasPreventiva;
	private int qtdPreventiva;
	private double horasPreditiva;
	private int qtdPreditiva;
	private double horasMaquina;
	private double horasPredial;

	// Resumo dos cards dos técnicos (horas e qtd manutenções por técnico)
	private Map<Long, Double> horasPorTecnico = new LinkedHashMap<>();
	private Map<Long, Integer> manutencoesPorTecnico = new LinkedHashMap<>();

	// Lista de manutenções do técnico selecionado
	private List<ItemManutencaoTecnico> manutencoesTecnico = new ArrayList<>();

	// Lista de compras vinculadas
	private List<SolicitacaoCompraMateriaisServicos> comprasTecnico = new ArrayList<>();

	// Dialog de detalhes (reutilizando padrão do sistema)
	private ItemManutencaoTecnico itemSelecionado;
	private ControleStatusManutencao controleStatusSelecionado;
	private List<PeriodoTrabalhoManutencao> periodosItemSelecionado = new ArrayList<>();
	private List<SolicitacaoCompraMateriaisServicos> comprasItemSelecionado = new ArrayList<>();
	private List<HistoricoReprogramacao> historicoReprogramacoes = new ArrayList<>();

	// Todos os períodos (cache)
	private List<PeriodoTrabalhoManutencao> todosPeriodos = new ArrayList<>();

	// IDs de manutenções válidas: existem e não estão canceladas
	// (chave: "solicitacaoId-TIPO"). Um período só entra nos cálculos de
	// horas/manutenções se sua solicitação estiver neste conjunto — mesmo
	// critério do dashboard, que só soma períodos cuja solicitação existe,
	// não está cancelada e passa nos filtros de data. Isso também cobre o
	// caso de períodos "órfãos", cuja solicitação foi excluída do banco:
	// esses nunca entram (diferente da versão anterior, que deixava passar
	// quando a solicitação não era encontrada).
	private Set<String> idsValidas = new HashSet<>();

	// Data de registro (dataManutencao) de cada solicitação (chave: "solicitacaoId-TIPO")
	// Usada para replicar, no filtro de período desta página, o mesmo critério
	// usado no dashboard: a manutenção em si também precisa ter sido registrada
	// dentro do período selecionado (não só o período de trabalho lançado).
	private Map<String, Date> dataManutencaoPorChave = new HashMap<>();

	@PostConstruct
	public void init() {
		try {
			carregarTecnicos();
			carregarTodosPeriodos();
			carregarDadosManutencoesReferenciadas();
			calcularResumoCards();
		} catch (Exception e) {
			LOGGER.error("Erro ao inicializar ConsultaTecnicoBean: ", e);
		}
	}

	private void carregarTecnicos() {
		List<CadastroUsuario> todos = manterCadastroUsuarioService.buscarTodos();
		todosTecnicos = todos.stream()
				.filter(u -> u.getTipoUsuario() == TipoUsuario.TECNICO)
				.collect(Collectors.toList());
		aplicarFiltros();
	}

	private void carregarTodosPeriodos() {
		try {
			todosPeriodos = manterPeriodoTrabalhoService.buscarTodos();
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar períodos: ", e);
			todosPeriodos = new ArrayList<>();
		}
	}

	/**
	 * Carrega, para cada manutenção referenciada nos períodos de trabalho,
	 * se ela está cancelada e qual sua data de registro (dataManutencao).
	 * Mantém consistência com o dashboard (EstatisticasManutencaoBean), que
	 * exclui canceladas e filtra pela data de registro da manutenção.
	 */
	private void carregarDadosManutencoesReferenciadas() {
		idsValidas.clear();
		dataManutencaoPorChave.clear();
		int orfaos = 0;
		try {
			// Identificar manutenções distintas (máquina e predial) referenciadas nos períodos
			Set<Long> idsMaquina = todosPeriodos.stream()
					.filter(p -> p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA)
					.map(PeriodoTrabalhoManutencao::getSolicitacaoId)
					.collect(Collectors.toSet());

			Set<Long> idsPredial = todosPeriodos.stream()
					.filter(p -> p.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL)
					.map(PeriodoTrabalhoManutencao::getSolicitacaoId)
					.collect(Collectors.toSet());

			for (Long id : idsMaquina) {
				try {
					SolicitacaoManutencaoMaquina maq = manterMaquinaService.buscarPorId(id);
					String chave = id + "-" + TipoManutencaoReferencia.MAQUINA.name();
					if (maq != null) {
						if (!maq.isCancelada()) {
							idsValidas.add(chave);
						}
						if (maq.getDataManutencao() != null) {
							dataManutencaoPorChave.put(chave, maq.getDataManutencao());
						}
					} else {
						orfaos++;
						LOGGER.warn("Período órfão: solicitação de máquina {} não encontrada (excluída?)", id);
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao verificar manutenção máquina {}: {}", id, e.getMessage());
				}
			}

			for (Long id : idsPredial) {
				try {
					SolicitacaoManutencaoPredial pred = manterPredialService.buscarPorId(id);
					String chave = id + "-" + TipoManutencaoReferencia.PREDIAL.name();
					if (pred != null) {
						if (!pred.isCancelada()) {
							idsValidas.add(chave);
						}
						if (pred.getDataManutencao() != null) {
							dataManutencaoPorChave.put(chave, pred.getDataManutencao());
						}
					} else {
						orfaos++;
						LOGGER.warn("Período órfão: solicitação predial {} não encontrada (excluída?)", id);
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao verificar manutenção predial {}: {}", id, e.getMessage());
				}
			}

			LOGGER.info("Manutenções válidas (existentes e não canceladas): {} | Períodos órfãos detectados: {}",
					idsValidas.size(), orfaos);
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar dados das manutenções referenciadas: ", e);
		}
	}

	/**
	 * Verifica se o período pertence a uma manutenção válida: existe e não
	 * está cancelada. Períodos órfãos (solicitação excluída) são excluídos
	 * aqui também, mantendo consistência com o dashboard.
	 */
	private boolean isPeriodoDeManutencaoAtiva(PeriodoTrabalhoManutencao p) {
		String chave = p.getSolicitacaoId() + "-" + p.getTipoManutencao().name();
		return idsValidas.contains(chave);
	}

	private void calcularResumoCards() {
		horasPorTecnico.clear();
		manutencoesPorTecnico.clear();

		for (CadastroUsuario tecnico : todosTecnicos) {
			double horas = 0;
			long manutencoesDistintas = 0;

			try {
				List<PeriodoTrabalhoManutencao> periodosTecnico = todosPeriodos.stream()
						.filter(p -> p.getTecnicoResponsavel() != null
								&& p.getTecnicoResponsavel().getId().equals(tecnico.getId()))
						.filter(this::dentroDoPeríodo)
						.filter(this::isPeriodoDeManutencaoAtiva)
						.collect(Collectors.toList());

				horas = periodosTecnico.stream()
						.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras)
						.sum();

				manutencoesDistintas = periodosTecnico.stream()
						.map(p -> p.getSolicitacaoId() + "-" + p.getTipoManutencao().name())
						.distinct()
						.count();
			} catch (Exception e) {
				LOGGER.error("Erro ao calcular resumo para técnico {}: ", tecnico.getId(), e);
			}

			horasPorTecnico.put(tecnico.getId(), horas);
			manutencoesPorTecnico.put(tecnico.getId(), (int) manutencoesDistintas);
		}
	}

	public void aplicarFiltros() {
		tecnicosFiltrados = todosTecnicos.stream()
				.filter(t -> {
					if (filtroNome != null && !filtroNome.trim().isEmpty()) {
						String filtro = filtroNome.trim().toLowerCase();
						boolean nomeMatch = t.getNome() != null && t.getNome().toLowerCase().contains(filtro);
						boolean registroMatch = t.getNuRegistro() != null && t.getNuRegistro().toLowerCase().contains(filtro);
						if (!nomeMatch && !registroMatch) return false;
					}
					if ("ATIVOS".equals(filtroStatus)) {
						return t.isAtivo();
					} else if ("INATIVOS".equals(filtroStatus)) {
						return !t.isAtivo();
					}
					return true;
				})
				.collect(Collectors.toList());
	}

	public void selecionarTecnico(CadastroUsuario tecnico) {
		this.tecnicoSelecionado = tecnico;
		recarregarDadosTecnico();
	}

	public void aplicarFiltroPeriodo() {
		if (tecnicoSelecionado != null) {
			recarregarDadosTecnico();
		}
		calcularResumoCards();
	}

	private void recarregarDadosTecnico() {
		carregarEstatisticasTecnico();
		carregarManutencoesTecnico();
		carregarComprasTecnico();
	}

	private Date getDataLimitePeriodo() {
		if (filtroPeriodoMeses <= 0) return null;
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -filtroPeriodoMeses);
		return cal.getTime();
	}

	private boolean dentroDoPeríodo(PeriodoTrabalhoManutencao p) {
		Date limite = getDataLimitePeriodo();
		if (limite == null) return true;
		if (p.getDataHoraInicio() == null) return false;
		if (p.getDataHoraInicio().before(limite)) return false;

		// Limite superior: "agora", equivalente ao filtroDataFim do dashboard
		// quando um período pré-definido (ex: 24 meses) está selecionado.
		// Sem isso, um período ou uma manutenção com data futura nos dados
		// (erro de cadastro, dado de teste, etc.) entraria aqui mas ficaria
		// de fora do total do dashboard, que trava os dois limites.
		Date agora = new Date();
		if (p.getDataHoraInicio().after(agora)) return false;

		// Além do período de trabalho, a manutenção em si também precisa ter
		// sido registrada dentro do intervalo selecionado — mesmo critério
		// usado no dashboard (EstatisticasManutencaoBean.filtrarPorData
		// aplicado a m.getDataManutencao()). Sem isso, uma manutenção antiga
		// que recebeu um período de trabalho recente entraria aqui mas
		// ficaria de fora do total do dashboard.
		String chave = p.getSolicitacaoId() + "-" + p.getTipoManutencao().name();
		Date dataManutencao = dataManutencaoPorChave.get(chave);
		if (dataManutencao != null) {
			if (dataManutencao.before(limite)) return false;
			if (dataManutencao.after(agora)) return false;
		}

		return true;
	}

	private void carregarEstatisticasTecnico() {
		if (tecnicoSelecionado == null) return;

		try {
			List<PeriodoTrabalhoManutencao> periodos = todosPeriodos.stream()
					.filter(p -> p.getTecnicoResponsavel() != null
							&& p.getTecnicoResponsavel().getId().equals(tecnicoSelecionado.getId()))
					.filter(this::dentroDoPeríodo)
					.filter(this::isPeriodoDeManutencaoAtiva)
					.collect(Collectors.toList());

			// Total de horas
			totalHorasTrabalhadas = periodos.stream()
					.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras)
					.sum();

			// Total de manutenções distintas
			totalManutencoes = (int) periodos.stream()
					.map(p -> p.getSolicitacaoId() + "-" + p.getTipoManutencao().name())
					.distinct()
					.count();

			// Horas de diagnóstico
			horasDiagnostico = periodos.stream()
					.filter(PeriodoTrabalhoManutencao::isUtilizadoParaDiagnostico)
					.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras)
					.sum();

			// Custo de mão de obra
			custoMaoDeObra = 0;
			for (PeriodoTrabalhoManutencao p : periodos) {
				double horas = p.getDuracaoEmHoras();
				if (horas <= 0) continue;

				if (p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA) {
					Double valorHora = tecnicoSelecionado.getValorHoraMaquina();
					if (valorHora != null && valorHora > 0) {
						custoMaoDeObra += horas * valorHora;
					}
				} else if (p.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL) {
					Double valorHora = tecnicoSelecionado.getValorHoraPredial();
					if (valorHora != null && valorHora > 0) {
						custoMaoDeObra += horas * valorHora;
					}
				}
			}

			// Horas por tipo de manutenção
			horasCorretiva = 0; qtdCorretiva = 0;
			horasPreventiva = 0; qtdPreventiva = 0;
			horasPreditiva = 0; qtdPreditiva = 0;
			horasMaquina = 0; horasPredial = 0;

			// Agrupar por manutenção
			Map<String, List<PeriodoTrabalhoManutencao>> porManutencao = periodos.stream()
					.collect(Collectors.groupingBy(p -> p.getSolicitacaoId() + "-" + p.getTipoManutencao().name()));

			for (Map.Entry<String, List<PeriodoTrabalhoManutencao>> entry : porManutencao.entrySet()) {
				List<PeriodoTrabalhoManutencao> pList = entry.getValue();
				PeriodoTrabalhoManutencao primeiro = pList.get(0);
				double horasManut = pList.stream().mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum();

				// Por referência (máquina vs predial)
				if (primeiro.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA) {
					horasMaquina += horasManut;

					// Buscar tipo de manutenção (corretiva/preventiva/preditiva)
					try {
						SolicitacaoManutencaoMaquina maq = manterMaquinaService.buscarPorId(primeiro.getSolicitacaoId());
						if (maq != null && maq.getTipoManutencao() != null) {
							switch (maq.getTipoManutencao()) {
								case CORRETIVA: horasCorretiva += horasManut; qtdCorretiva++; break;
								case PREVENTIVA: horasPreventiva += horasManut; qtdPreventiva++; break;
								case PREDITIVA: horasPreditiva += horasManut; qtdPreditiva++; break;
							}
						}
					} catch (Exception e) {
						LOGGER.error("Erro ao buscar tipo de manutenção máquina: ", e);
					}
				} else if (primeiro.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL) {
					horasPredial += horasManut;

					try {
						SolicitacaoManutencaoPredial pred = manterPredialService.buscarPorId(primeiro.getSolicitacaoId());
						if (pred != null && pred.getTipoManutencao() != null) {
							switch (pred.getTipoManutencao()) {
								case CORRETIVA: horasCorretiva += horasManut; qtdCorretiva++; break;
								case PREVENTIVA: horasPreventiva += horasManut; qtdPreventiva++; break;
								case PREDITIVA: horasPreditiva += horasManut; qtdPreditiva++; break;
							}
						}
					} catch (Exception e) {
						LOGGER.error("Erro ao buscar tipo de manutenção predial: ", e);
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao carregar estatísticas do técnico: ", e);
		}
	}

	private void carregarManutencoesTecnico() {
		manutencoesTecnico.clear();
		if (tecnicoSelecionado == null) return;

		try {
			List<PeriodoTrabalhoManutencao> periodos = todosPeriodos.stream()
					.filter(p -> p.getTecnicoResponsavel() != null
							&& p.getTecnicoResponsavel().getId().equals(tecnicoSelecionado.getId()))
					.filter(this::dentroDoPeríodo)
					.collect(Collectors.toList());

			// Agrupar por manutenção
			Map<String, List<PeriodoTrabalhoManutencao>> porManutencao = periodos.stream()
					.collect(Collectors.groupingBy(p -> p.getSolicitacaoId() + "-" + p.getTipoManutencao().name()));

			for (Map.Entry<String, List<PeriodoTrabalhoManutencao>> entry : porManutencao.entrySet()) {
				List<PeriodoTrabalhoManutencao> pList = entry.getValue();
				PeriodoTrabalhoManutencao primeiro = pList.get(0);

				ItemManutencaoTecnico item = new ItemManutencaoTecnico();
				item.setSolicitacaoId(primeiro.getSolicitacaoId());
				item.setTipoManutencaoRef(primeiro.getTipoManutencao());

				double horasManut = pList.stream().mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum();
				item.setHorasTecnico(horasManut);

				// Buscar dados da manutenção
				if (primeiro.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA) {
					try {
						SolicitacaoManutencaoMaquina maq = manterMaquinaService.buscarPorId(primeiro.getSolicitacaoId());
						if (maq != null) {
							item.setDescricao(maq.getFalha());
							// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
							// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
							// na entidade CadastroMaquinaEquipamento.
							item.setEquipamentoLocal(maq.getMaquina() != null
									? maq.getMaquina().getIdentificacao()
									: "N/A");
							item.setTipoManutencao(maq.getTipoManutencao() != null ? maq.getTipoManutencao().name() : "N/A");
							item.setDataManutencao(maq.getDataManutencao());
							item.setCancelada(maq.isCancelada());
							item.setUrlFoto(maq.getUrlFoto());
							item.setSolicitante(maq.getSolicitante());
							item.setObservacoes(maq.getObservacoes());

							// Campos específicos de máquina
							item.setRisco(maq.isRisco());
							item.setDescrRisco(maq.getDescrRisco());
							item.setOperando(maq.isOperando());
							item.setDescrOperando(maq.getDescrOperando());
						}
					} catch (Exception e) {
						LOGGER.error("Erro ao buscar manutenção máquina {}: ", primeiro.getSolicitacaoId(), e);
					}
				} else if (primeiro.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL) {
					try {
						SolicitacaoManutencaoPredial pred = manterPredialService.buscarPorId(primeiro.getSolicitacaoId());
						if (pred != null) {
							item.setDescricao(pred.getProblema());
							item.setEquipamentoLocal(pred.getLocal() != null ? pred.getLocal().getNome() : "N/A");
							item.setTipoManutencao(pred.getTipoManutencao() != null ? pred.getTipoManutencao().name() : "N/A");
							item.setDataManutencao(pred.getDataManutencao());
							item.setCancelada(pred.isCancelada());
							item.setUrlFoto(pred.getUrlFoto());
							item.setSolicitante(pred.getSolicitante());
							item.setObservacoes(pred.getObservacoes());
						}
					} catch (Exception e) {
						LOGGER.error("Erro ao buscar manutenção predial {}: ", primeiro.getSolicitacaoId(), e);
					}
				}

				// Buscar status
				try {
					ControleStatusManutencao controle = manterControleStatusService
							.buscarPorSolicitacao(primeiro.getSolicitacaoId(), primeiro.getTipoManutencao());
					if (controle != null) {
						item.setStatus(controle.getStatus());
					} else if (item.isCancelada()) {
						item.setStatus(StatusManutencao.CANCELADA);
					} else {
						item.setStatus(StatusManutencao.PARA_FAZER);
					}
				} catch (Exception e) {
					LOGGER.error("Erro ao buscar status: ", e);
					item.setStatus(StatusManutencao.PARA_FAZER);
				}

				manutencoesTecnico.add(item);
			}

			// Ordenar por data mais recente primeiro
			manutencoesTecnico.sort((a, b) -> {
				if (a.getDataManutencao() == null && b.getDataManutencao() == null) return 0;
				if (a.getDataManutencao() == null) return 1;
				if (b.getDataManutencao() == null) return -1;
				return b.getDataManutencao().compareTo(a.getDataManutencao());
			});

		} catch (Exception e) {
			LOGGER.error("Erro ao carregar manutenções do técnico: ", e);
		}
	}

	private void carregarComprasTecnico() {
		comprasTecnico.clear();
		if (tecnicoSelecionado == null || manutencoesTecnico.isEmpty()) return;

		try {
			List<SolicitacaoCompraMateriaisServicos> todasCompras = manterCompraService.buscarTodas();
			for (ItemManutencaoTecnico item : manutencoesTecnico) {
				comprasTecnico.addAll(
						todasCompras.stream()
								.filter(c -> c.getSolicitacaoManutencaoId() != null
										&& c.getSolicitacaoManutencaoId().equals(item.getSolicitacaoId())
										&& c.getTipoManutencaoReferencia() == item.getTipoManutencaoRef())
								.collect(Collectors.toList())
				);
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar compras do técnico: ", e);
		}
	}

	// ===== DIALOG DE DETALHES =====

	public void abrirDetalhes(ItemManutencaoTecnico item) {
		this.itemSelecionado = item;
		this.controleStatusSelecionado = null;
		this.periodosItemSelecionado = new ArrayList<>();
		this.comprasItemSelecionado = new ArrayList<>();
		this.historicoReprogramacoes = new ArrayList<>();

		try {
			// Buscar controle de status
			controleStatusSelecionado = manterControleStatusService
					.buscarPorSolicitacao(item.getSolicitacaoId(), item.getTipoManutencaoRef());

			// Buscar períodos de trabalho
			periodosItemSelecionado = manterPeriodoTrabalhoService
					.buscarPorSolicitacao(item.getSolicitacaoId(), item.getTipoManutencaoRef());

			// Buscar compras
			List<SolicitacaoCompraMateriaisServicos> todasCompras = manterCompraService.buscarTodas();
			comprasItemSelecionado = todasCompras.stream()
					.filter(c -> c.getSolicitacaoManutencaoId() != null
							&& c.getSolicitacaoManutencaoId().equals(item.getSolicitacaoId())
							&& c.getTipoManutencaoReferencia() == item.getTipoManutencaoRef())
					.collect(Collectors.toList());

			// Buscar histórico de reprogramações
			historicoReprogramacoes = manterHistoricoReprogramacaoService
					.buscarHistoricoPorSolicitacao(item.getSolicitacaoId(), item.getTipoManutencaoRef());

		} catch (Exception e) {
			LOGGER.error("Erro ao abrir detalhes da manutenção: ", e);
		}
	}

	public String getUrlFotoCorrigida() {
		if (itemSelecionado == null || itemSelecionado.getUrlFoto() == null) return null;
		String foto = itemSelecionado.getUrlFoto();
		if (foto.startsWith("/")) return foto;
		return Uploads.URL_BASE + foto;
	}

	// ===== MÉTODOS UTILITÁRIOS =====

	public String getIniciais(CadastroUsuario tecnico) {
		if (tecnico == null || tecnico.getNome() == null) return "?";
		String[] partes = tecnico.getNome().trim().split("\\s+");
		if (partes.length >= 2) {
			return (partes[0].charAt(0) + "" + partes[partes.length - 1].charAt(0)).toUpperCase();
		}
		return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
	}

	public double getHorasResumo(CadastroUsuario tecnico) {
		return horasPorTecnico.getOrDefault(tecnico.getId(), 0.0);
	}

	public int getManutencoesResumo(CadastroUsuario tecnico) {
		return manutencoesPorTecnico.getOrDefault(tecnico.getId(), 0);
	}

	public String formatarHoras(double horas) {
		if (horas == 0) return "0h";
		int horasInteiras = (int) horas;
		int minutos = (int) ((horas - horasInteiras) * 60);
		if (minutos == 0) return horasInteiras + "h";
		return horasInteiras + "h" + String.format("%02d", minutos) + "m";
	}

	public String getStatusLabel(StatusManutencao status) {
		if (status == null) return "N/A";
		switch (status) {
			case PARA_FAZER: return "Para Fazer";
			case EM_ANDAMENTO: return "Em Andamento";
			case CONCLUIDA: return "Concluída";
			case CANCELADA: return "Cancelada";
			case AGUARDANDO_COMPRA: return "Aguardando Compra";
			default: return status.name();
		}
	}

	public String getStatusClass(StatusManutencao status) {
		if (status == null) return "";
		switch (status) {
			case PARA_FAZER: return "status-para-fazer";
			case EM_ANDAMENTO: return "status-andamento";
			case CONCLUIDA: return "status-concluida";
			case CANCELADA: return "status-cancelada";
			case AGUARDANDO_COMPRA: return "status-aguardando";
			default: return "";
		}
	}

	public String getTipoRefLabel(TipoManutencaoReferencia tipo) {
		if (tipo == null) return "N/A";
		switch (tipo) {
			case MAQUINA: return "Máquina";
			case PREDIAL: return "Predial";
			default: return tipo.name();
		}
	}

	public void voltarParaLista() {
		this.tecnicoSelecionado = null;
	}

	public String getPeriodoLabel() {
		switch (filtroPeriodoMeses) {
			case 3: return "Últimos 3 meses";
			case 6: return "Últimos 6 meses";
			case 12: return "Últimos 12 meses";
			case 24: return "Últimos 24 meses";
			default: return "Todo o período";
		}
	}

	// ===== GETTERS E SETTERS =====

	public List<CadastroUsuario> getTecnicosFiltrados() { return tecnicosFiltrados; }
	public String getFiltroNome() { return filtroNome; }
	public void setFiltroNome(String filtroNome) { this.filtroNome = filtroNome; }
	public String getFiltroStatus() { return filtroStatus; }
	public void setFiltroStatus(String filtroStatus) { this.filtroStatus = filtroStatus; }
	public int getFiltroPeriodoMeses() { return filtroPeriodoMeses; }
	public void setFiltroPeriodoMeses(int filtroPeriodoMeses) { this.filtroPeriodoMeses = filtroPeriodoMeses; }
	public CadastroUsuario getTecnicoSelecionado() { return tecnicoSelecionado; }
	public void setTecnicoSelecionado(CadastroUsuario tecnicoSelecionado) { this.tecnicoSelecionado = tecnicoSelecionado; }
	public double getTotalHorasTrabalhadas() { return totalHorasTrabalhadas; }
	public int getTotalManutencoes() { return totalManutencoes; }
	public double getHorasDiagnostico() { return horasDiagnostico; }
	public double getCustoMaoDeObra() { return custoMaoDeObra; }
	public double getHorasCorretiva() { return horasCorretiva; }
	public int getQtdCorretiva() { return qtdCorretiva; }
	public double getHorasPreventiva() { return horasPreventiva; }
	public int getQtdPreventiva() { return qtdPreventiva; }
	public double getHorasPreditiva() { return horasPreditiva; }
	public int getQtdPreditiva() { return qtdPreditiva; }
	public double getHorasMaquina() { return horasMaquina; }
	public double getHorasPredial() { return horasPredial; }
	public List<ItemManutencaoTecnico> getManutencoesTecnico() { return manutencoesTecnico; }
	public List<SolicitacaoCompraMateriaisServicos> getComprasTecnico() { return comprasTecnico; }
	public ItemManutencaoTecnico getItemSelecionado() { return itemSelecionado; }
	public ControleStatusManutencao getControleStatusSelecionado() { return controleStatusSelecionado; }
	public List<PeriodoTrabalhoManutencao> getPeriodosItemSelecionado() { return periodosItemSelecionado; }
	public List<SolicitacaoCompraMateriaisServicos> getComprasItemSelecionado() { return comprasItemSelecionado; }
	public List<HistoricoReprogramacao> getHistoricoReprogramacoes() { return historicoReprogramacoes; }

	// ===== CLASSE INTERNA =====

}