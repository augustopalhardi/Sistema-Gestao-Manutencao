package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.CalculoIndicadores;
import com.augustopalhardi.manutencao.service.ManterCadastroMaquinaEquipamentoService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.util.Json;
import com.augustopalhardi.manutencao.view.dto.ConfiabilidadeCategoria;
import com.augustopalhardi.manutencao.view.dto.ItemEstatistica;

@Named
@ViewScoped
public class EstatisticasManutencaoBean implements Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(EstatisticasManutencaoBean.class);
	private static final long serialVersionUID = 1L;

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterCompraService;

	@Inject
	private ManterPeriodoTrabalhoManutencaoService manterPeriodoService;

	@Inject
	private ManterCadastroMaquinaEquipamentoService manterMaquinaEquipService;

	// ==================== FILTROS ====================
	private Date filtroDataInicio;
	private Date filtroDataFim;
	private String filtroTipoManutencao;
	private String filtroStatus;
	private String filtroTipo;
	private String filtroTipoMaquina;
	private String filtroPeriodo;

	private List<CadastroMaquinaEquipamento> listaMaquinas;
	private List<String> listaTiposMaquina;

	// ==================== DADOS BRUTOS ====================
	private List<SolicitacaoManutencaoMaquina> solicitacoesMaquina;
	private List<SolicitacaoManutencaoPredial> solicitacoesPredial;
	private List<SolicitacaoCompraMateriaisServicos> todasCompras;
	private List<PeriodoTrabalhoManutencao> todosPeriodos;
	private List<ControleStatusManutencao> todosControles;

	private List<SolicitacaoManutencaoMaquina> maquinasFiltradas;
	private List<SolicitacaoManutencaoPredial> prediaisFiltradas;

	private List<SolicitacaoManutencaoMaquina> maquinasFiltradasSemCanceladas;
	private List<SolicitacaoManutencaoPredial> prediaisFiltradasSemCanceladas;

	private List<ItemEstatistica> ultimasManutencoes;

	// ==================== TOTALIZADORES ====================
	private long totalManutencoes;
	private long totalMaquinas;
	private long totalPredial;
	private long totalParaFazer;
	private long totalEmAndamento;
	private long totalConcluidas;
	private long totalAguardandoCompra;
	private long totalCanceladas;
	private long totalAtivas;
	private long totalComRisco;
	private long totalUrgente;
	private long totalNormal;

	// ==================== INDICADORES ====================
	private Map<String, Long> reparosPorMaquina;
	private double tempoMedioPorReparo;
	private double mttr;
	private double mtbf;
	private Map<String, Long> avariasPorCategoria;
	private Map<String, Map<String, Long>> avariasPorMaquinaPorMes;
	private Map<String, Double> custoPorTipoManutencao;
	private Map<String, Double> custoMensal;
	private Map<String, Double> custoTotalPorMaquina;
	private double custoTotalGeral;
	private double custoTotalMaquinas;
	private double custoTotalPredial;
	private double custoMaoObraMaquina;
	private double custoMaoObraPredial;

	// Compras por tipo (Material / Serviço / Material e Serviço)
	private long quantidadeComprasMaterial;
	private long quantidadeComprasServico;
	private long quantidadeComprasAmbos;
	private double custoComprasMaterial;
	private double custoComprasServico;
	private double custoComprasAmbos;
	// Custos separados do tipo AMBOS (parcela material e parcela serviço)
	private double custoComprasAmbosMaterial;
	private double custoComprasAmbosServico;
	private Map<String, Double> confiabilidadePorMaquina;
	private Map<String, Long> pioresAtores;
	private Map<String, Double> pioresAtoresPorcentagem;
	private Map<String, Long> pioresAtoresPorCategoria;
	private Map<String, Double> pioresAtoresCategoriaPorcentagem;
	private double tempoTotalHorasGastas;

	// Confiabilidade por categoria
	private List<ConfiabilidadeCategoria> confiabilidadePorCategoriaLista;
	private double mediaGeralConfiabilidade;

	// Média geral de confiabilidade por máquina
	private double mediaGeralConfiabilidadeMaquina;

	// Média geral de confiabilidade por categoria
	private double mediaGeralConfiabilidadeCategoria;

	// BBS / Near Miss
	private long totalBbsNearMiss;
	private long totalSemBbsNearMiss;
	private double percentualBbsNearMiss;

	private Map<String, Long> maquinasPorQuantidade;
	private Map<String, Long> locaisPorQuantidade;
	private Map<String, Double> manutencoesComMaisGastos;

	@PostConstruct
	public void init() {
		try {
			LOG.info("Inicializando EstatisticasManutencaoBean...");

			filtroTipo = "TODOS";
			filtroPeriodo = "";

			listaMaquinas = manterMaquinaEquipService.buscarTodos();
			carregarTiposMaquina();
			carregarDadosBrutos();
			aplicarFiltros();

			LOG.info("EstatisticasManutencaoBean inicializado com sucesso");
		} catch (Exception e) {
			LOG.error("Erro ao inicializar estatísticas: ", e);
		}
	}

	private void carregarTiposMaquina() {
		listaTiposMaquina = listaMaquinas.stream().map(CadastroMaquinaEquipamento::getTipo)
				.filter(t -> t != null && !t.trim().isEmpty()).map(String::trim).distinct().sorted()
				.collect(Collectors.toList());
	}

	private void carregarDadosBrutos() {
		solicitacoesMaquina = manterMaquinaService.buscarTodos();
		solicitacoesPredial = manterPredialService.buscarTodos();
		todasCompras = manterCompraService.buscarTodas();
		todosPeriodos = manterPeriodoService.buscarTodos();
		todosControles = manterControleService.buscarTodos();
	}

	/**
	 * Resolve o período selecionado antes de aplicar os filtros: se um
	 * período predefinido (1/3/6/12/24 meses) estiver selecionado, calcula
	 * as datas de início/fim a partir dele. Se estiver em "Personalizado"
	 * (valor vazio), mantém as datas que o usuário digitou manualmente em
	 * Data Início/Fim, sem zerá-las. Usado pelo botão único "Aplicar
	 * Filtros", já que os filtros só devem entrar em vigor nesse clique.
	 */
	public void aplicarPeriodoPreDefinido() {
		if (filtroPeriodo != null && !filtroPeriodo.isEmpty()) {
			try {
				int meses = Integer.parseInt(filtroPeriodo);
				Calendar cal = Calendar.getInstance();
				filtroDataFim = cal.getTime();
				cal.add(Calendar.MONTH, -meses);
				filtroDataInicio = cal.getTime();
			} catch (NumberFormatException e) {
				LOG.warn("Valor de período predefinido inválido: {}", filtroPeriodo);
			}
		}
		aplicarFiltros();
	}

	public void aplicarFiltros() {
		try {
			maquinasFiltradas = solicitacoesMaquina.stream().filter(m -> filtrarPorData(m.getDataManutencao()))
					.filter(m -> filtrarPorTipoManutencaoEnum(m.getTipoManutencao()))
					.filter(m -> filtrarPorStatusMaquina(m))
					.filter(m -> filtroTipo.equals("TODOS") || filtroTipo.equals("MAQUINA"))
					.filter(m -> filtrarPorTipoMaquina(m)).collect(Collectors.toList());

			prediaisFiltradas = solicitacoesPredial.stream().filter(p -> filtrarPorData(p.getDataManutencao()))
					.filter(p -> filtrarPorTipoManutencaoEnum(p.getTipoManutencao()))
					.filter(p -> filtrarPorStatusPredial(p))
					.filter(p -> filtroTipo.equals("TODOS") || filtroTipo.equals("PREDIAL"))
					.collect(Collectors.toList());

			maquinasFiltradasSemCanceladas = maquinasFiltradas.stream().filter(m -> !isCanceladaMaquina(m.getId()))
					.collect(Collectors.toList());

			prediaisFiltradasSemCanceladas = prediaisFiltradas.stream().filter(p -> !isCanceladaPredial(p.getId()))
					.collect(Collectors.toList());

			calcularEstatisticas();
			calcularIndicadores();
			prepararDadosGraficos();
			prepararUltimasManutencoes();

		} catch (Exception e) {
			LOG.error("Erro ao aplicar filtros: ", e);
		}
	}

	public void limparFiltros() {
		filtroDataInicio = null;
		filtroDataFim = null;
		filtroTipoManutencao = null;
		filtroStatus = null;
		filtroTipo = "TODOS";
		filtroTipoMaquina = null;
		filtroPeriodo = "";
		aplicarFiltros();
	}

	// ==================== HELPERS CANCELAMENTO ====================

	private boolean isCanceladaMaquina(Long id) {
		ControleStatusManutencao controle = buscarControle(id, TipoManutencaoReferencia.MAQUINA);
		return controle != null && controle.getStatus() == ControleStatusManutencao.StatusManutencao.CANCELADA;
	}

	private boolean isCanceladaPredial(Long id) {
		ControleStatusManutencao controle = buscarControle(id, TipoManutencaoReferencia.PREDIAL);
		return controle != null && controle.getStatus() == ControleStatusManutencao.StatusManutencao.CANCELADA;
	}

	// ==================== MÉTODOS DE FILTRO ====================

	private boolean filtrarPorData(Date data) {
		if (data == null)
			return true;
		if (filtroDataInicio != null && data.before(filtroDataInicio))
			return false;
		if (filtroDataFim != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(filtroDataFim);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			if (data.after(cal.getTime()))
				return false;
		}
		return true;
	}

	private boolean filtrarPorTipoMaquina(SolicitacaoManutencaoMaquina m) {
		if (filtroTipoMaquina == null || filtroTipoMaquina.isEmpty())
			return true;
		return m.getMaquina() != null && filtroTipoMaquina.equals(m.getMaquina().getTipo());
	}

	private boolean filtrarPorTipoManutencaoEnum(TipoManutencao tipo) {
		if (filtroTipoManutencao == null || filtroTipoManutencao.isEmpty())
			return true;
		if (tipo == null)
			return false;
		return tipo.name().equals(filtroTipoManutencao);
	}

	private boolean filtrarPorStatusMaquina(SolicitacaoManutencaoMaquina m) {
		if (filtroStatus == null || filtroStatus.isEmpty())
			return true;
		ControleStatusManutencao controle = buscarControle(m.getId(), TipoManutencaoReferencia.MAQUINA);
		if (controle == null)
			return "ATIVA".equals(filtroStatus);
		return controle.getStatus().name().equals(filtroStatus);
	}

	private boolean filtrarPorStatusPredial(SolicitacaoManutencaoPredial p) {
		if (filtroStatus == null || filtroStatus.isEmpty())
			return true;
		ControleStatusManutencao controle = buscarControle(p.getId(), TipoManutencaoReferencia.PREDIAL);
		if (controle == null)
			return "ATIVA".equals(filtroStatus);
		return controle.getStatus().name().equals(filtroStatus);
	}

	private ControleStatusManutencao buscarControle(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		return todosControles.stream()
				.filter(c -> c.getSolicitacaoId().equals(solicitacaoId) && c.getTipoManutencao() == tipo).findFirst()
				.orElse(null);
	}

	// ==================== CÁLCULOS ====================

	private void calcularEstatisticas() {
		totalMaquinas = maquinasFiltradas.size();
		totalPredial = prediaisFiltradas.size();
		totalManutencoes = totalMaquinas + totalPredial;

		totalParaFazer = 0;
		totalEmAndamento = 0;
		totalConcluidas = 0;
		totalAguardandoCompra = 0;
		totalCanceladas = 0;
		totalAtivas = 0;

		for (SolicitacaoManutencaoMaquina m : maquinasFiltradas) {
			contarStatus(m.getId(), TipoManutencaoReferencia.MAQUINA);
		}
		for (SolicitacaoManutencaoPredial p : prediaisFiltradas) {
			contarStatus(p.getId(), TipoManutencaoReferencia.PREDIAL);
		}

		long riscoMaquinas = maquinasFiltradas.stream().filter(SolicitacaoManutencaoMaquina::isRisco).count();
		long riscoPrediais = prediaisFiltradas.stream().filter(SolicitacaoManutencaoPredial::isRiscoSeguranca).count();
		totalComRisco = riscoMaquinas + riscoPrediais;

		long urgenteSemRisco = prediaisFiltradas.stream().filter(p -> p.isUrgente() && !p.isRiscoSeguranca()).count();
		totalUrgente = urgenteSemRisco;

		totalNormal = totalManutencoes - totalComRisco - totalUrgente;
		if (totalNormal < 0)
			totalNormal = 0;
	}

	private void contarStatus(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		ControleStatusManutencao controle = buscarControle(solicitacaoId, tipo);
		if (controle == null) {
			totalAtivas++;
			return;
		}
		switch (controle.getStatus()) {
		case PARA_FAZER:
			totalParaFazer++;
			break;
		case EM_ANDAMENTO:
			totalEmAndamento++;
			break;
		case CONCLUIDA:
			totalConcluidas++;
			break;
		case AGUARDANDO_COMPRA:
			totalAguardandoCompra++;
			break;
		case CANCELADA:
			totalCanceladas++;
			break;
		}
	}

	private void calcularIndicadores() {
		calcularReparosPorMaquina();
		calcularTempoMedioPorReparo();
		calcularMTTR();
		calcularMTBF();
		calcularAvariasPorCategoria();
		calcularAvariasPorMaquinaPorMes();
		calcularCustoPorTipoManutencao();
		calcularCustoMensal();
		calcularCustoTotalPorMaquina();
		calcularCustoTotalGeral();
		calcularCustoMaoDeObra();
		calcularComprasPorTipo();
		calcularConfiabilidadePorMaquina();
		calcularPioresAtores();
		calcularTempoTotalHoras();
		calcularPioresAtoresPorCategoria();
		calcularConfiabilidadePorCategoriaLista();
		calcularBbsNearMiss();
	}

	private void calcularBbsNearMiss() {
		try {
			Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream()
					.map(SolicitacaoManutencaoMaquina::getId).collect(Collectors.toSet());
			Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream()
					.map(SolicitacaoManutencaoPredial::getId).collect(Collectors.toSet());

			totalBbsNearMiss = todosControles.stream()
					.filter(c -> c.isBbsNearMiss())
					.filter(c -> {
						if (c.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA)
							return idsMaquina.contains(c.getSolicitacaoId());
						else
							return idsPredial.contains(c.getSolicitacaoId());
					})
					.count();

			long totalGeral = idsMaquina.size() + idsPredial.size();
			totalSemBbsNearMiss = totalGeral - totalBbsNearMiss;
			percentualBbsNearMiss = totalGeral > 0 ? (totalBbsNearMiss * 100.0 / totalGeral) : 0.0;
		} catch (Exception e) {
			LOG.error("Erro ao calcular BBS/Near Miss: ", e);
			totalBbsNearMiss = 0;
			totalSemBbsNearMiss = 0;
			percentualBbsNearMiss = 0;
		}
	}

	private void calcularReparosPorMaquina() {
		reparosPorMaquina = maquinasFiltradasSemCanceladas.stream().filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo(),
						Collectors.counting()))
				.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private void calcularTempoMedioPorReparo() {
		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		double totalHoras = 0;
		int totalReparos = 0;

		for (PeriodoTrabalhoManutencao periodo : todosPeriodos) {
			if (!filtrarPorData(periodo.getDataHoraInicio())) {
				continue;
			}
			boolean pertence = false;
			if (periodo.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA
					&& idsMaquina.contains(periodo.getSolicitacaoId())) {
				pertence = true;
			} else if (periodo.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL
					&& idsPredial.contains(periodo.getSolicitacaoId())) {
				pertence = true;
			}
			if (pertence) {
				totalHoras += periodo.getDuracaoEmHoras();
				totalReparos++;
			}
		}

		tempoMedioPorReparo = totalReparos > 0 ? totalHoras / totalReparos : 0;
	}

	private void calcularMTTR() {
		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());

		List<Double> horasPorReparo = new ArrayList<>();
		for (Long id : idsMaquina) {
			ControleStatusManutencao controle = buscarControle(id, TipoManutencaoReferencia.MAQUINA);
			if (controle != null && controle.getStatus() == ControleStatusManutencao.StatusManutencao.CONCLUIDA) {
				horasPorReparo.add(todosPeriodos.stream()
						.filter(p -> p.getSolicitacaoId().equals(id)
								&& p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA
								&& filtrarPorData(p.getDataHoraInicio()))
						.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum());
			}
		}

		mttr = CalculoIndicadores.mttr(horasPorReparo);
	}

	private void calcularMTBF() {
		Map<Long, List<Date>> falhasPorMaquina = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null && m.getDataManutencao() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getId(),
						Collectors.mapping(SolicitacaoManutencaoMaquina::getDataManutencao, Collectors.toList())));

		mtbf = CalculoIndicadores.mtbf(falhasPorMaquina.values());
	}

	private void calcularAvariasPorCategoria() {
		avariasPorCategoria = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null && m.getMaquina().getTipo() != null
						&& !m.getMaquina().getTipo().isEmpty())
				.collect(Collectors.groupingBy(m -> m.getMaquina().getTipo(), Collectors.counting())).entrySet()
				.stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private void calcularAvariasPorMaquinaPorMes() {
		SimpleDateFormat sdfMes = new SimpleDateFormat("MM/yyyy");
		avariasPorMaquinaPorMes = new LinkedHashMap<>();

		Map<String, Long> topMaquinas = maquinasFiltradasSemCanceladas.stream().filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo(),
						Collectors.counting()))
				.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		for (String nomeMaquina : topMaquinas.keySet()) {
			Map<String, Long> porMes = maquinasFiltradasSemCanceladas.stream()
					.filter(m -> m.getMaquina() != null
							&& (m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo()).equals(nomeMaquina)
							&& m.getDataManutencao() != null)
					.collect(Collectors.groupingBy(m -> sdfMes.format(m.getDataManutencao()), Collectors.counting()));
			avariasPorMaquinaPorMes.put(nomeMaquina, porMes);
		}
	}

	private void calcularCustoPorTipoManutencao() {
		custoPorTipoManutencao = new LinkedHashMap<>();
		custoPorTipoManutencao.put("Corretiva", 0.0);
		custoPorTipoManutencao.put("Preventiva", 0.0);
		custoPorTipoManutencao.put("Preditiva", 0.0);

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getValorGasto() == null || compra.getValorGasto() <= 0
					|| compra.getStatusSolicitacao() != SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO
					|| !filtrarPorData(compra.getDataSolicitacao()))
				continue;
			Long idMnt = compra.getSolicitacaoManutencaoId();
			if (idMnt == null)
				continue;
			boolean pertence = false;
			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
				pertence = idsMaquina.contains(idMnt);
			} else if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL) {
				pertence = idsPredial.contains(idMnt);
			}
			if (!pertence)
				continue;
			TipoManutencao tipoMnt = obterTipoManutencaoDaCompra(compra);
			if (tipoMnt != null) {
				custoPorTipoManutencao.merge(tipoMnt.getDescricao(), compra.getValorGasto(), Double::sum);
			}
		}
	}

	private TipoManutencao obterTipoManutencaoDaCompra(SolicitacaoCompraMateriaisServicos compra) {
		try {
			Long idManutencao = compra.getSolicitacaoManutencaoId();
			if (idManutencao == null)
				return null;
			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
				SolicitacaoManutencaoMaquina maq = manterMaquinaService.buscarPorId(idManutencao);
				return maq != null ? maq.getTipoManutencao() : null;
			} else {
				SolicitacaoManutencaoPredial pred = manterPredialService.buscarPorId(idManutencao);
				return pred != null ? pred.getTipoManutencao() : null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	private void calcularCustoMensal() {
		SimpleDateFormat sdfMes = new SimpleDateFormat("MM/yyyy");

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		custoMensal = todasCompras.stream().filter(c -> c.getValorGasto() != null && c.getValorGasto() > 0)
				.filter(c -> c
						.getStatusSolicitacao() == SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO)
				.filter(c -> c.getDataSolicitacao() != null).filter(c -> filtrarPorData(c.getDataSolicitacao()))
				.filter(c -> {
					Long idMnt = c.getSolicitacaoManutencaoId();
					if (idMnt == null)
						return false;
					if (c.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
						return idsMaquina.contains(idMnt);
					} else {
						return idsPredial.contains(idMnt);
					}
				})
				.collect(Collectors.groupingBy(c -> sdfMes.format(c.getDataSolicitacao()),
						Collectors.summingDouble(SolicitacaoCompraMateriaisServicos::getValorGasto)))
				.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private void calcularCustoTotalPorMaquina() {
		Map<String, Double> custos = new HashMap<>();

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getValorGasto() == null || compra.getValorGasto() <= 0
					|| compra.getStatusSolicitacao() != SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO
					|| !filtrarPorData(compra.getDataSolicitacao()))
				continue;
			if (compra
					.getTipoManutencaoReferencia() != TipoManutencaoReferencia.MAQUINA)
				continue;
			if (!idsMaquina.contains(compra.getSolicitacaoManutencaoId()))
				continue;
			try {
				SolicitacaoManutencaoMaquina maq = manterMaquinaService
						.buscarPorId(compra.getSolicitacaoManutencaoId());
				if (maq != null && maq.getMaquina() != null) {
					String nome = maq.getMaquina().getFabricante() + " " + maq.getMaquina().getModelo();
					custos.merge(nome, compra.getValorGasto(), Double::sum);
				}
			} catch (Exception e) {
				LOG.error("Erro ao calcular custo por máquina: ", e);
			}
		}

		custoTotalPorMaquina = custos.entrySet().stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(10)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Calcula o valor total gasto (geral, somente máquinas/equipamentos e
	 * somente predial), respeitando todos os filtros atuais do dashboard
	 * (período, tipo de manutenção, categoria de máquina, status, tipo).
	 */
	private void calcularCustoTotalGeral() {
		custoTotalGeral = 0;
		custoTotalMaquinas = 0;
		custoTotalPredial = 0;

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getValorGasto() == null || compra.getValorGasto() <= 0
					|| compra.getStatusSolicitacao() != SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO
					|| !filtrarPorData(compra.getDataSolicitacao()))
				continue;
			Long idMnt = compra.getSolicitacaoManutencaoId();
			if (idMnt == null)
				continue;

			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA
					&& idsMaquina.contains(idMnt)) {
				custoTotalMaquinas += compra.getValorGasto();
				custoTotalGeral += compra.getValorGasto();
			} else if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL
					&& idsPredial.contains(idMnt)) {
				custoTotalPredial += compra.getValorGasto();
				custoTotalGeral += compra.getValorGasto();
			}
		}
	}

	/**
	 * Calcula o valor total de mão de obra técnica (horas trabalhadas x valor
	 * da hora cadastrado no técnico responsável de cada período), separado
	 * por Máquinas/Equipamentos e Predial, respeitando os filtros atuais do
	 * dashboard (período, tipo de manutenção, categoria de máquina, status,
	 * tipo). Além de pertencer a uma manutenção filtrada, o próprio período
	 * de trabalho também precisa ter sua data de início dentro do intervalo
	 * De/Até selecionado — mesmo critério já usado no cálculo de custo de
	 * compras.
	 */
	private void calcularCustoMaoDeObra() {
		custoMaoObraMaquina = 0;
		custoMaoObraPredial = 0;

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		for (PeriodoTrabalhoManutencao periodo : todosPeriodos) {
			if (periodo.getTecnicoResponsavel() == null)
				continue;

			if (!filtrarPorData(periodo.getDataHoraInicio()))
				continue;

			double horas = periodo.getDuracaoEmHoras();
			if (horas <= 0)
				continue;

			if (periodo.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA
					&& idsMaquina.contains(periodo.getSolicitacaoId())) {
				Double valorHora = periodo.getTecnicoResponsavel().getValorHoraMaquina();
				if (valorHora != null && valorHora > 0) {
					custoMaoObraMaquina += horas * valorHora;
				}
			} else if (periodo.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL
					&& idsPredial.contains(periodo.getSolicitacaoId())) {
				Double valorHora = periodo.getTecnicoResponsavel().getValorHoraPredial();
				if (valorHora != null && valorHora > 0) {
					custoMaoObraPredial += horas * valorHora;
				}
			}
		}
	}

	/**
	 * Calcula, para cada tipo de solicitação de compra (Material, Serviço,
	 * Material e Serviço), a quantidade de solicitações (pendentes +
	 * processadas, excluindo as canceladas) e o valor efetivamente gasto
	 * (somente compras com status Processada/COMPRADO), respeitando os
	 * filtros atuais do dashboard (período, tipo de manutenção, categoria de
	 * máquina, status, tipo).
	 */
	private void calcularComprasPorTipo() {
		quantidadeComprasMaterial = 0;
		quantidadeComprasServico = 0;
		quantidadeComprasAmbos = 0;
		custoComprasMaterial = 0;
		custoComprasServico = 0;
		custoComprasAmbos = 0;
		custoComprasAmbosMaterial = 0;
		custoComprasAmbosServico = 0;

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getTipoSolicitacao() == null || !filtrarPorData(compra.getDataSolicitacao())) {
				continue;
			}

			Long idMnt = compra.getSolicitacaoManutencaoId();
			if (idMnt == null) {
				continue;
			}

			boolean pertence = false;
			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
				pertence = idsMaquina.contains(idMnt);
			} else if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL) {
				pertence = idsPredial.contains(idMnt);
			}
			if (!pertence) {
				continue;
			}

			if (compra.getStatusSolicitacao() == SolicitacaoCompraMateriaisServicos.StatusSolicitacao.CANCELADA) {
				continue;
			}

			// Quantidade: conta pendentes e processadas (canceladas ficam de fora)
			switch (compra.getTipoSolicitacao()) {
			case MATERIAL:
				quantidadeComprasMaterial++;
				break;
			case SERVICO:
				quantidadeComprasServico++;
				break;
			case AMBOS:
				quantidadeComprasAmbos++;
				break;
			}

			// Valor gasto: somente compras já processadas
			if (compra.getValorGasto() != null && compra.getValorGasto() > 0
					&& compra.getStatusSolicitacao() == SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO) {
				switch (compra.getTipoSolicitacao()) {
				case MATERIAL:
					custoComprasMaterial += compra.getValorGasto();
					break;
				case SERVICO:
					custoComprasServico += compra.getValorGasto();
					break;
				case AMBOS:
					custoComprasAmbos += compra.getValorGasto();
					// Acumula parcelas separadas (material e serviço) do tipo AMBOS
					if (compra.getValorGastoMaterial() != null && compra.getValorGastoMaterial() > 0) {
						custoComprasAmbosMaterial += compra.getValorGastoMaterial();
					}
					if (compra.getValorGastoServico() != null && compra.getValorGastoServico() > 0) {
						custoComprasAmbosServico += compra.getValorGastoServico();
					}
					break;
				}
			}
		}
	}

	private void calcularConfiabilidadePorMaquina() {
		confiabilidadePorMaquina = new LinkedHashMap<>();

		// Agrupa manutenções filtradas por nome de máquina
		Map<String, List<SolicitacaoManutencaoMaquina>> manutencoesPorNome = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo()));

		// Determina a lista de máquinas a considerar:
		// Se houver filtro de tipo de máquina, respeita; caso contrário, usa todas
		// cadastradas.
		List<CadastroMaquinaEquipamento> maquinasParaExibir;
		if (filtroTipoMaquina != null && !filtroTipoMaquina.isEmpty()) {
			maquinasParaExibir = listaMaquinas.stream().filter(mq -> filtroTipoMaquina.equals(mq.getTipo()))
					.collect(Collectors.toList());
		} else if ("PREDIAL".equals(filtroTipo)) {
			// Filtro de tipo PREDIAL — não exibe máquinas no gráfico
			maquinasParaExibir = new ArrayList<>();
		} else {
			maquinasParaExibir = new ArrayList<>(listaMaquinas);
		}

		double horasTotalPeriodo = calcularHorasTotalPeriodo();

		for (CadastroMaquinaEquipamento mq : maquinasParaExibir) {
			if (mq.getFabricante() == null || mq.getModelo() == null)
				continue;
			String nomeMaquina = mq.getFabricante() + " " + mq.getModelo();

			List<SolicitacaoManutencaoMaquina> manutencoes = manutencoesPorNome.getOrDefault(nomeMaquina,
					new ArrayList<>());

			double totalHorasParada = 0;
			for (SolicitacaoManutencaoMaquina m : manutencoes) {
				ControleStatusManutencao controle = buscarControle(m.getId(),
						TipoManutencaoReferencia.MAQUINA);
				if (controle != null && m.getDataManutencao() != null) {
					Date dataFim = controle.getDataConclusao() != null ? controle.getDataConclusao() : new Date();
					totalHorasParada += CalculoIndicadores.horasUteisEntre(m.getDataManutencao(), dataFim);
				}
			}

			confiabilidadePorMaquina.put(nomeMaquina,
					CalculoIndicadores.confiabilidade(horasTotalPeriodo, totalHorasParada, !manutencoes.isEmpty()));
		}

		// Ordena do menor para o maior (piores primeiro)
		confiabilidadePorMaquina = CalculoIndicadores.ordenadoCrescente(confiabilidadePorMaquina);
		mediaGeralConfiabilidadeMaquina = CalculoIndicadores.media(confiabilidadePorMaquina.values());
	}

	private double calcularHorasTotalPeriodo() {
		Date inicio, fim;
		if (filtroDataInicio != null) {
			inicio = filtroDataInicio;
		} else {
			Optional<Date> maisAntiga = maquinasFiltradasSemCanceladas.stream()
					.map(SolicitacaoManutencaoMaquina::getDataManutencao).filter(Objects::nonNull)
					.min(Comparator.naturalOrder());
			inicio = maisAntiga.orElse(new Date());
		}
		fim = filtroDataFim != null ? filtroDataFim : new Date();
		long diff = fim.getTime() - inicio.getTime();
		return CalculoIndicadores.horasUteis(diff);
	}

	private void calcularPioresAtores() {
		pioresAtores = maquinasFiltradasSemCanceladas.stream().filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo(),
						Collectors.counting()))
				.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		pioresAtoresPorcentagem = new LinkedHashMap<>();
		long totalMaq = maquinasFiltradasSemCanceladas.size();
		if (totalMaq > 0) {
			for (Map.Entry<String, Long> entry : pioresAtores.entrySet()) {
				pioresAtoresPorcentagem.put(entry.getKey(), CalculoIndicadores.percentual(entry.getValue(), totalMaq));
			}
		}
	}

	private void calcularPioresAtoresPorCategoria() {
		pioresAtoresPorCategoria = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null && m.getMaquina().getTipo() != null
						&& !m.getMaquina().getTipo().trim().isEmpty())
				.collect(Collectors.groupingBy(m -> m.getMaquina().getTipo(), Collectors.counting())).entrySet()
				.stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		pioresAtoresCategoriaPorcentagem = new LinkedHashMap<>();
		long totalMaq = maquinasFiltradasSemCanceladas.size();
		if (totalMaq > 0) {
			for (Map.Entry<String, Long> entry : pioresAtoresPorCategoria.entrySet()) {
				pioresAtoresCategoriaPorcentagem.put(entry.getKey(),
						CalculoIndicadores.percentual(entry.getValue(), totalMaq));
			}
		}
	}

	private void calcularTempoTotalHoras() {
		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		tempoTotalHorasGastas = todosPeriodos.stream()
				.filter(p -> filtrarPorData(p.getDataHoraInicio()))
				.filter(p -> {
			if (p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA) {
				return idsMaquina.contains(p.getSolicitacaoId());
			} else {
				return idsPredial.contains(p.getSolicitacaoId());
			}
		}).mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum();
	}

	/**
	 * Considera TODAS as categorias cadastradas em listaMaquinas.
	 * Categorias sem manutenção têm confiabilidade 100%. Calcula média por
	 * categoria e média geral de categorias.
	 */
	private void calcularConfiabilidadePorCategoriaLista() {
		confiabilidadePorCategoriaLista = new ArrayList<>();

		// Agrupa manutenções filtradas por categoria
		Map<String, List<SolicitacaoManutencaoMaquina>> porCategoria = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null && m.getMaquina().getTipo() != null
						&& !m.getMaquina().getTipo().trim().isEmpty())
				.collect(Collectors.groupingBy(m -> m.getMaquina().getTipo()));

		// Obtém TODAS as categorias cadastradas (independente de filtro de tipo
		// máquina)
		Set<String> todasCategorias = listaMaquinas.stream().map(CadastroMaquinaEquipamento::getTipo)
				.filter(t -> t != null && !t.trim().isEmpty()).map(String::trim).collect(Collectors.toSet());

		// Se filtro de tipo for PREDIAL, não exibe categorias de máquina
		if ("PREDIAL".equals(filtroTipo)) {
			mediaGeralConfiabilidadeCategoria = 0;
			mediaGeralConfiabilidade = 0;
			return;
		}

		// Se houver filtro de categoria de máquina, restringe
		if (filtroTipoMaquina != null && !filtroTipoMaquina.isEmpty()) {
			todasCategorias = todasCategorias.stream().filter(cat -> cat.equals(filtroTipoMaquina))
					.collect(Collectors.toSet());
		}

		double horasTotalPeriodoBase = calcularHorasTotalPeriodo();

		for (String categoria : todasCategorias) {
			List<SolicitacaoManutencaoMaquina> manutencoes = porCategoria.getOrDefault(categoria, new ArrayList<>());

			long qtd = manutencoes.size();

			// Quantidade de máquinas únicas cadastradas nesta categoria
			long qtdMaquinasUnicasCadastradas = listaMaquinas.stream().filter(mq -> categoria.equals(mq.getTipo()))
					.count();
			if (qtdMaquinasUnicasCadastradas == 0)
				qtdMaquinasUnicasCadastradas = 1;

			// Horas totais de parada da categoria
			double totalHorasParada = 0;
			for (SolicitacaoManutencaoMaquina m : manutencoes) {
				ControleStatusManutencao controle = buscarControle(m.getId(),
						TipoManutencaoReferencia.MAQUINA);
				if (controle != null && m.getDataManutencao() != null) {
					Date dataFim = controle.getDataConclusao() != null ? controle.getDataConclusao() : new Date();
					totalHorasParada += CalculoIndicadores.horasUteisEntre(m.getDataManutencao(), dataFim);
				}
			}

			double confiabilidade = CalculoIndicadores.confiabilidade(
					horasTotalPeriodoBase * qtdMaquinasUnicasCadastradas, totalHorasParada, !manutencoes.isEmpty());

			ConfiabilidadeCategoria item = new ConfiabilidadeCategoria();
			item.setCategoria(categoria);
			item.setQuantidadeManutencoes(qtd);
			item.setConfiabilidade(confiabilidade);
			confiabilidadePorCategoriaLista.add(item);
		}

		// Ordena do menor para o maior
		confiabilidadePorCategoriaLista.sort(Comparator.comparingDouble(ConfiabilidadeCategoria::getConfiabilidade));

		mediaGeralConfiabilidadeCategoria = CalculoIndicadores.media(confiabilidadePorCategoriaLista.stream()
				.map(ConfiabilidadeCategoria::getConfiabilidade).collect(Collectors.toList()));

		// Mantém compatibilidade: mediaGeralConfiabilidade aponta para a média de
		// categorias
		mediaGeralConfiabilidade = mediaGeralConfiabilidadeCategoria;
	}

	// ==================== PREPARAR GRÁFICOS ====================

	private void prepararDadosGraficos() {
		maquinasPorQuantidade = maquinasFiltradasSemCanceladas.stream().filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo(),
						Collectors.counting()))
				.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		locaisPorQuantidade = prediaisFiltradasSemCanceladas.stream().filter(p -> p.getLocal() != null)
				.collect(Collectors.groupingBy(p -> p.getLocal().getNome(), Collectors.counting())).entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		calcularManutencoesComMaisGastos();
	}

	private void calcularManutencoesComMaisGastos() {
		Map<String, Double> gastoPorManutencao = new HashMap<>();

		Set<Long> idsMaquina = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());
		Set<Long> idsPredial = prediaisFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoPredial::getId)
				.collect(Collectors.toSet());

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getValorGasto() == null || compra.getValorGasto() <= 0)
				continue;
			if (compra.getStatusSolicitacao() != SolicitacaoCompraMateriaisServicos.StatusSolicitacao.COMPRADO)
				continue;
			if (!filtrarPorData(compra.getDataSolicitacao()))
				continue;
			Long idManutencao = compra.getSolicitacaoManutencaoId();
			if (idManutencao == null)
				continue;

			boolean pertence = false;
			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
				pertence = idsMaquina.contains(idManutencao);
			} else if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL) {
				pertence = idsPredial.contains(idManutencao);
			}
			if (!pertence)
				continue;

			String chave = gerarChaveManutencao(compra);
			if (chave != null) {
				gastoPorManutencao.merge(chave, compra.getValorGasto(), Double::sum);
			}
		}

		manutencoesComMaisGastos = gastoPorManutencao.entrySet().stream()
				.sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(5)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	private String gerarChaveManutencao(SolicitacaoCompraMateriaisServicos compra) {
		try {
			Long idManutencao = compra.getSolicitacaoManutencaoId();
			if (idManutencao == null)
				return null;
			String nomeEquipamento = "N/A";
			if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
				SolicitacaoManutencaoMaquina manutencao = manterMaquinaService.buscarPorId(idManutencao);
				if (manutencao != null && manutencao.getMaquina() != null) {
					nomeEquipamento = manutencao.getMaquina().getFabricante() + " "
							+ manutencao.getMaquina().getModelo();
				}
			} else if (compra
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL) {
				SolicitacaoManutencaoPredial manutencao = manterPredialService.buscarPorId(idManutencao);
				if (manutencao != null && manutencao.getLocal() != null) {
					nomeEquipamento = manutencao.getLocal().getNome();
				}
			}
			return "#" + idManutencao + " - " + nomeEquipamento;
		} catch (Exception e) {
			LOG.error("Erro ao gerar chave para manutenção: ", e);
			return null;
		}
	}

	private void prepararUltimasManutencoes() {
		ultimasManutencoes = new ArrayList<>();

		for (SolicitacaoManutencaoMaquina m : maquinasFiltradas) {
			ItemEstatistica item = new ItemEstatistica();
			item.setId(m.getId());
			item.setTipo("MAQUINA");
			item.setFalha(m.getFalha());
			item.setMaquina(
					m.getMaquina() != null ? m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo() : "N/A");
			item.setDataManutencao(m.getDataManutencao());
			item.setRisco(m.isRisco());
			item.setRiscoSeguranca(false);
			item.setUrgente(false);
			item.setCancelada(m.isCancelada());
			ControleStatusManutencao controle = buscarControle(m.getId(),
					TipoManutencaoReferencia.MAQUINA);
			if (controle != null)
				item.setStatus(controle.getStatus());
			ultimasManutencoes.add(item);
		}

		for (SolicitacaoManutencaoPredial p : prediaisFiltradas) {
			ItemEstatistica item = new ItemEstatistica();
			item.setId(p.getId());
			item.setTipo("PREDIAL");
			item.setProblema(p.getProblema());
			item.setLocal(p.getLocal() != null ? p.getLocal().getNome() : "N/A");
			item.setDataManutencao(p.getDataManutencao());
			item.setRisco(false);
			item.setRiscoSeguranca(p.isRiscoSeguranca());
			item.setUrgente(p.isUrgente());
			item.setCancelada(p.isCancelada());
			ControleStatusManutencao controle = buscarControle(p.getId(),
					TipoManutencaoReferencia.PREDIAL);
			if (controle != null)
				item.setStatus(controle.getStatus());
			ultimasManutencoes.add(item);
		}

		ultimasManutencoes.sort((a, b) -> {
			if (a.getDataManutencao() == null && b.getDataManutencao() == null)
				return 0;
			if (a.getDataManutencao() == null)
				return 1;
			if (b.getDataManutencao() == null)
				return -1;
			return b.getDataManutencao().compareTo(a.getDataManutencao());
		});

		if (ultimasManutencoes.size() > 20) {
			ultimasManutencoes = ultimasManutencoes.subList(0, 20);
		}
	}

	// ==================== MÉTODOS JSON ====================

	public String getTopMaquinasLabelsJSON() {
		return Json.rotulos(maquinasPorQuantidade);
	}

	public String getTopMaquinasDataJSON() {
		return Json.valoresInteiros(maquinasPorQuantidade);
	}

	public String getTopLocaisLabelsJSON() {
		return Json.rotulos(locaisPorQuantidade);
	}

	public String getTopLocaisDataJSON() {
		return Json.valoresInteiros(locaisPorQuantidade);
	}

	public String getTopGastosLabelsJSON() {
		return Json.rotulos(manutencoesComMaisGastos);
	}

	public String getTopGastosDataJSON() {
		return Json.valoresDecimais(manutencoesComMaisGastos);
	}

	public String getReparosPorMaquinaLabelsJSON() {
		return Json.rotulos(reparosPorMaquina);
	}

	public String getReparosPorMaquinaDataJSON() {
		return Json.valoresInteiros(reparosPorMaquina);
	}

	public String getAvariasPorCategoriaLabelsJSON() {
		return Json.rotulos(avariasPorCategoria);
	}

	public String getAvariasPorCategoriaDataJSON() {
		return Json.valoresInteiros(avariasPorCategoria);
	}

	public String getAvariasPorMaquinaPorMesJSON() {
		if (avariasPorMaquinaPorMes == null || avariasPorMaquinaPorMes.isEmpty())
			return "{}";
		try {
			Set<String> todosOsMeses = new TreeSet<>();
			for (Map<String, Long> porMes : avariasPorMaquinaPorMes.values()) {
				todosOsMeses.addAll(porMes.keySet());
			}
			StringBuilder sb = new StringBuilder("{");
			sb.append("\"meses\":[");
			sb.append(todosOsMeses.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));
			sb.append("],\"datasets\":[");
			boolean first = true;
			String[] cores = { "rgba(102,126,234,0.8)", "rgba(118,75,162,0.8)", "rgba(237,100,166,0.8)",
					"rgba(255,154,158,0.8)", "rgba(250,208,196,0.8)" };
			int corIndex = 0;
			for (Map.Entry<String, Map<String, Long>> entry : avariasPorMaquinaPorMes.entrySet()) {
				if (!first)
					sb.append(",");
				first = false;
				sb.append("{\"label\":\"").append(Json.escapar(entry.getKey())).append("\",");
				sb.append("\"cor\":\"").append(cores[corIndex % cores.length]).append("\",");
				sb.append("\"data\":[");
				boolean firstData = true;
				for (String mes : todosOsMeses) {
					if (!firstData)
						sb.append(",");
					firstData = false;
					sb.append(entry.getValue().getOrDefault(mes, 0L));
				}
				sb.append("]}");
				corIndex++;
			}
			sb.append("]}");
			return sb.toString();
		} catch (Exception e) {
			LOG.error("Erro ao gerar JSON avarias por mês: ", e);
			return "{}";
		}
	}

	public String getCustoPorTipoManutencaoLabelsJSON() {
		return Json.rotulos(custoPorTipoManutencao);
	}

	public String getCustoPorTipoManutencaoDataJSON() {
		return Json.valoresDecimais(custoPorTipoManutencao);
	}

	public String getCustoMensalLabelsJSON() {
		return Json.rotulos(custoMensal);
	}

	public String getCustoMensalDataJSON() {
		return Json.valoresDecimais(custoMensal);
	}

	public String getCustoTotalPorMaquinaLabelsJSON() {
		return Json.rotulos(custoTotalPorMaquina);
	}

	public String getCustoTotalPorMaquinaDataJSON() {
		return Json.valoresDecimais(custoTotalPorMaquina);
	}

	public double getCustoTotalGeral() {
		return custoTotalGeral;
	}

	public double getCustoTotalMaquinas() {
		return custoTotalMaquinas;
	}

	public double getCustoTotalPredial() {
		return custoTotalPredial;
	}

	public double getCustoMaoObraMaquina() {
		return custoMaoObraMaquina;
	}

	public double getCustoMaoObraPredial() {
		return custoMaoObraPredial;
	}

	public double getCustoTotalMaoObra() {
		return custoMaoObraMaquina + custoMaoObraPredial;
	}

	public String getConfiabilidadeLabelsJSON() {
		return Json.rotulos(confiabilidadePorMaquina);
	}

	public String getConfiabilidadeDataJSON() {
		return Json.valoresDecimais(confiabilidadePorMaquina);
	}

	/** Rótulos para o gráfico de confiabilidade por categoria */
	public String getConfiabilidadePorCategoriaLabelsJSON() {
		if (confiabilidadePorCategoriaLista == null || confiabilidadePorCategoriaLista.isEmpty())
			return "[]";
		return "[" + confiabilidadePorCategoriaLista.stream().map(i -> "\"" + Json.escapar(i.getCategoria()) + "\"")
				.collect(Collectors.joining(",")) + "]";
	}

	/** Valores para o gráfico de confiabilidade por categoria */
	public String getConfiabilidadePorCategoriaDataJSON() {
		if (confiabilidadePorCategoriaLista == null || confiabilidadePorCategoriaLista.isEmpty())
			return "[]";
		return "[" + confiabilidadePorCategoriaLista.stream()
				.map(i -> String.format(Locale.US, "%.2f", i.getConfiabilidade())).collect(Collectors.joining(","))
				+ "]";
	}

	public String getPioresAtoresLabelsJSON() {
		return Json.rotulos(pioresAtores);
	}

	public String getPioresAtoresDataJSON() {
		return Json.valoresInteiros(pioresAtores);
	}

	public String getPioresAtoresPorcentagemDataJSON() {
		return Json.valoresDecimais(pioresAtoresPorcentagem);
	}

	public String getPioresAtoresCategoriaLabelsJSON() {
		return Json.rotulos(pioresAtoresPorCategoria);
	}

	public String getPioresAtoresCategoriaDataJSON() {
		return Json.valoresInteiros(pioresAtoresPorCategoria);
	}

	public String getPioresAtoresCategoriaPorcentagemDataJSON() {
		return Json.valoresDecimais(pioresAtoresCategoriaPorcentagem);
	}

	// ==================== AUXILIARES JSON ====================

	// ==================== AUXILIARES ====================

	public String getStatusLabel(ControleStatusManutencao.StatusManutencao status) {
		if (status == null)
			return "Ativa";
		switch (status) {
		case PARA_FAZER:
			return "Para Fazer";
		case EM_ANDAMENTO:
			return "Em Andamento";
		case CONCLUIDA:
			return "Concluída";
		case AGUARDANDO_COMPRA:
			return "Aguardando Compra";
		case CANCELADA:
			return "Cancelada";
		default:
			return "Ativa";
		}
	}

	public String getTempoTotalFormatado() {
		if (tempoTotalHorasGastas == 0)
			return "0h";
		int horas = (int) tempoTotalHorasGastas;
		int minutos = (int) ((tempoTotalHorasGastas - horas) * 60);
		return horas + "h " + minutos + "m";
	}

	public String getMttrFormatado() {
		if (mttr == 0)
			return "0h";
		int horas = (int) mttr;
		int minutos = (int) ((mttr - horas) * 60);
		return horas + "h " + minutos + "m";
	}

	public String getMtbfFormatado() {
		if (mtbf == 0)
			return "0h";
		int horas = (int) mtbf;
		int minutos = (int) ((mtbf - horas) * 60);
		return horas + "h " + minutos + "m";
	}

	public String getTempoMedioPorReparoFormatado() {
		if (tempoMedioPorReparo == 0)
			return "0h";
		int horas = (int) tempoMedioPorReparo;
		int minutos = (int) ((tempoMedioPorReparo - horas) * 60);
		return horas + "h " + minutos + "m";
	}

	public String getMediaGeralConfiabilidadeFormatada() {
		return String.format("%.2f%%", mediaGeralConfiabilidade);
	}

	public String getMediaGeralConfiabilidadeClass() {
		if (mediaGeralConfiabilidade >= 80)
			return "confiabilidade-alta";
		if (mediaGeralConfiabilidade >= 50)
			return "confiabilidade-media";
		return "confiabilidade-baixa";
	}

	// Getters das médias separadas

	public double getMediaGeralConfiabilidadeMaquina() {
		return mediaGeralConfiabilidadeMaquina;
	}

	public String getMediaGeralConfiabilidadeMaquinaFormatada() {
		return String.format("%.2f%%", mediaGeralConfiabilidadeMaquina);
	}

	public String getMediaGeralConfiabilidadeMaquinaClass() {
		if (mediaGeralConfiabilidadeMaquina >= 80)
			return "confiabilidade-alta";
		if (mediaGeralConfiabilidadeMaquina >= 50)
			return "confiabilidade-media";
		return "confiabilidade-baixa";
	}

	public double getMediaGeralConfiabilidadeCategoria() {
		return mediaGeralConfiabilidadeCategoria;
	}

	public String getMediaGeralConfiabilidadeCategoriaFormatada() {
		return String.format("%.2f%%", mediaGeralConfiabilidadeCategoria);
	}

	public String getMediaGeralConfiabilidadeCategoriaClass() {
		if (mediaGeralConfiabilidadeCategoria >= 80)
			return "confiabilidade-alta";
		if (mediaGeralConfiabilidadeCategoria >= 50)
			return "confiabilidade-media";
		return "confiabilidade-baixa";
	}

	// ==================== BBS / NEAR MISS GETTERS ====================

	public long getTotalBbsNearMiss() {
		return totalBbsNearMiss;
	}

	public long getTotalSemBbsNearMiss() {
		return totalSemBbsNearMiss;
	}

	public double getPercentualBbsNearMiss() {
		return percentualBbsNearMiss;
	}

	public String getPercentualBbsNearMissFormatado() {
		return String.format(Locale.US, "%.1f", percentualBbsNearMiss);
	}

	// ==================== GETTERS E SETTERS ====================

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

	public String getFiltroTipoManutencao() {
		return filtroTipoManutencao;
	}

	public void setFiltroTipoManutencao(String filtroTipoManutencao) {
		this.filtroTipoManutencao = filtroTipoManutencao;
	}

	public String getFiltroStatus() {
		return filtroStatus;
	}

	public void setFiltroStatus(String filtroStatus) {
		this.filtroStatus = filtroStatus;
	}

	public String getFiltroTipo() {
		return filtroTipo;
	}

	public void setFiltroTipo(String filtroTipo) {
		this.filtroTipo = filtroTipo;
	}

	public String getFiltroTipoMaquina() {
		return filtroTipoMaquina;
	}

	public void setFiltroTipoMaquina(String filtroTipoMaquina) {
		this.filtroTipoMaquina = filtroTipoMaquina;
	}

	public String getFiltroPeriodo() {
		return filtroPeriodo;
	}

	public void setFiltroPeriodo(String filtroPeriodo) {
		this.filtroPeriodo = filtroPeriodo;
	}

	public List<String> getListaTiposMaquina() {
		return listaTiposMaquina;
	}

	public long getTotalManutencoes() {
		return totalManutencoes;
	}

	public long getTotalMaquinas() {
		return totalMaquinas;
	}

	public long getTotalPredial() {
		return totalPredial;
	}

	public long getTotalParaFazer() {
		return totalParaFazer;
	}

	public long getTotalEmAndamento() {
		return totalEmAndamento;
	}

	public long getTotalConcluidas() {
		return totalConcluidas;
	}

	public long getTotalAguardandoCompra() {
		return totalAguardandoCompra;
	}

	public long getTotalCanceladas() {
		return totalCanceladas;
	}

	public long getTotalAtivas() {
		return totalAtivas;
	}

	public long getTotalComRisco() {
		return totalComRisco;
	}

	public long getTotalUrgente() {
		return totalUrgente;
	}

	public long getTotalNormal() {
		return totalNormal;
	}

	public double getTempoMedioPorReparo() {
		return tempoMedioPorReparo;
	}

	public double getMttr() {
		return mttr;
	}

	public double getMtbf() {
		return mtbf;
	}

	public double getTempoTotalHorasGastas() {
		return tempoTotalHorasGastas;
	}

	public double getMediaGeralConfiabilidade() {
		return mediaGeralConfiabilidade;
	}

	public long getQuantidadeComprasMaterial() {
		return quantidadeComprasMaterial;
	}

	public long getQuantidadeComprasServico() {
		return quantidadeComprasServico;
	}

	public long getQuantidadeComprasAmbos() {
		return quantidadeComprasAmbos;
	}

	public double getCustoComprasMaterial() {
		return custoComprasMaterial;
	}

	public double getCustoComprasServico() {
		return custoComprasServico;
	}

	public double getCustoComprasAmbos() {
		return custoComprasAmbos;
	}

	public double getCustoComprasAmbosMaterial() {
		return custoComprasAmbosMaterial;
	}

	public double getCustoComprasAmbosServico() {
		return custoComprasAmbosServico;
	}

	public List<ItemEstatistica> getUltimasManutencoes() {
		return ultimasManutencoes;
	}

	public List<ConfiabilidadeCategoria> getConfiabilidadePorCategoriaLista() {
		return confiabilidadePorCategoriaLista;
	}

	// ==================== CLASSE INTERNA: ItemEstatistica ====================
	// ==================== CLASSE INTERNA: ConfiabilidadeCategoria
	// ====================
}