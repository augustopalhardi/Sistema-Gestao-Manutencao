package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.event.AjaxBehaviorEvent;
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
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.CalculoIndicadores;
import com.augustopalhardi.manutencao.service.ManterCadastroMaquinaEquipamentoService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.util.Json;
import com.augustopalhardi.manutencao.view.dto.ConfiabilidadeCategoria;

@Named
@ViewScoped
public class RelatorioIndicadoresBean implements Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(RelatorioIndicadoresBean.class);
	private static final long serialVersionUID = 1L;

	// Categorias fixas — tudo que não estiver aqui vira "Especial"
	private static final List<String> CATEGORIAS_FIXAS = Arrays.asList("Centro de Usinagem", "Furadeira", "Torno",
			"Serra", "Fresadora", "Prensa");
	private static final String CATEGORIA_ESPECIAL = "Especial";

	/**
	 * Retorna a categoria normalizada: se pertence às fixas devolve ela mesma,
	 * senão "Especial".
	 */
	private String normalizarCategoria(String tipo) {
		if (tipo == null || tipo.trim().isEmpty()) {
			return CATEGORIA_ESPECIAL;
		}
		String t = tipo.trim();
		for (String fixa : CATEGORIAS_FIXAS) {
			if (fixa.equalsIgnoreCase(t)) {
				return fixa;
			}
		}
		return CATEGORIA_ESPECIAL;
	}

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterMaquinaService;

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
	private String filtroTipoManutencao; // padrão: CORRETIVA
	private String filtroTipoMaquina; // categoria da máquina
	private String filtroPeriodo; // 1, 3, 6, 12, 24 meses

	private List<CadastroMaquinaEquipamento> listaMaquinas;
	private List<String> listaTiposMaquina;

	// ==================== DADOS BRUTOS ====================
	private List<SolicitacaoManutencaoMaquina> todasMaquinas;
	private List<SolicitacaoCompraMateriaisServicos> todasCompras;
	private List<PeriodoTrabalhoManutencao> todosPeriodos;
	private List<ControleStatusManutencao> todosControles;

	// Filtradas (sem canceladas já aplicado)
	private List<SolicitacaoManutencaoMaquina> maquinasFiltradas; // com canceladas (para contagem total)
	private List<SolicitacaoManutencaoMaquina> maquinasFiltradasSemCanceladas; // para cálculos

	// ==================== TOTALIZADORES ====================
	private long totalManutencoesCategoria; // total por categoria (sum de todas)
	private long totalConcluidas;
	private Map<String, Long> manutencoesTotaisPorCategoria; // para os cards

	// ==================== INDICADORES ====================
	private double tempoTotalHorasGastas;
	private double tempoMedioPorReparo;
	private double mttr;
	private double mtbf;

	// Piores atores
	private Map<String, Long> pioresAtores;
	private Map<String, Double> pioresAtoresPorcentagem;

	// Custo por categoria e total
	private Map<String, Double> custoPorCategoria;
	private Map<String, Double> custoPorMaquina;
	private double custoTotal;

	// Confiabilidade
	private Map<String, Double> confiabilidadePorMaquina;
	private double mediaGeralConfiabilidadeMaquina;
	private List<ConfiabilidadeCategoria> confiabilidadePorCategoriaLista;
	private double mediaGeralConfiabilidadeCategoria;

	@PostConstruct
	public void init() {
		try {
			LOG.info("Inicializando RelatorioIndicadoresBean...");
			filtroTipoManutencao = "CORRETIVA"; // padrão
			filtroPeriodo = "12"; // padrão: últimos 12 meses

			listaMaquinas = manterMaquinaEquipService.buscarTodos();
			carregarTiposMaquina();
			carregarDadosBrutos();
			aplicarPeriodoPreDefinido(); // já aplica o período padrão (12 meses)

			LOG.info("RelatorioIndicadoresBean inicializado com sucesso");
		} catch (Exception e) {
			LOG.error("Erro ao inicializar RelatorioIndicadoresBean: ", e);
		}
	}

	private void carregarTiposMaquina() {
		// Monta lista de categorias agrupadas (fixas + Especial se houver)
		Set<String> categoriasUsadas = listaMaquinas.stream().map(CadastroMaquinaEquipamento::getTipo)
				.filter(t -> t != null && !t.trim().isEmpty()).map(this::normalizarCategoria)
				.collect(Collectors.toSet());

		listaTiposMaquina = new ArrayList<>(CATEGORIAS_FIXAS);
		if (categoriasUsadas.contains(CATEGORIA_ESPECIAL)) {
			listaTiposMaquina.add(CATEGORIA_ESPECIAL);
		}
	}

	private void carregarDadosBrutos() {
		todasMaquinas = manterMaquinaService.buscarTodos();
		todasCompras = manterCompraService.buscarTodas();
		todosPeriodos = manterPeriodoService.buscarTodos();
		todosControles = manterControleService.buscarTodos();
	}

	// ==================== PERÍODO PRÉ-DEFINIDO ====================

	public void aplicarPeriodoPreDefinido() {
		if (filtroPeriodo != null && !filtroPeriodo.isEmpty()) {
			try {
				int meses = Integer.parseInt(filtroPeriodo);
				Calendar cal = Calendar.getInstance();
				filtroDataFim = cal.getTime();
				cal.add(Calendar.MONTH, -meses);
				filtroDataInicio = cal.getTime();
			} catch (NumberFormatException e) {
				filtroDataInicio = null;
				filtroDataFim = null;
			}
		} else {
			filtroDataInicio = null;
			filtroDataFim = null;
		}
		aplicarFiltros();
	}

	/** Overload para f:ajax listener */
	public void aplicarPeriodoPreDefinido(AjaxBehaviorEvent event) {
		aplicarPeriodoPreDefinido();
	}

	// ==================== FILTROS ====================

	public void aplicarFiltros() {
		try {
			// Filtra apenas máquinas (sem predial)
			maquinasFiltradas = todasMaquinas.stream().filter(m -> filtrarPorData(m.getDataManutencao()))
					.filter(m -> filtrarPorTipoManutencao(m.getTipoManutencao())).filter(m -> filtrarPorCategoria(m))
					.collect(Collectors.toList());

			maquinasFiltradasSemCanceladas = maquinasFiltradas.stream().filter(m -> !isCancelada(m.getId()))
					.collect(Collectors.toList());

			calcularTotais();
			calcularIndicadores();
			calcularCustos();
			calcularConfiabilidade();

		} catch (Exception e) {
			LOG.error("Erro ao aplicar filtros do relatório: ", e);
		}
	}

	/** Overload para f:ajax listener */
	public void aplicarFiltros(AjaxBehaviorEvent event) {
		aplicarFiltros();
	}

	public void limparFiltros() {
		filtroDataInicio = null;
		filtroDataFim = null;
		filtroTipoManutencao = "CORRETIVA";
		filtroTipoMaquina = null;
		filtroPeriodo = "";
		aplicarFiltros();
	}

	/** Overload para f:ajax listener */
	public void limparFiltros(AjaxBehaviorEvent event) {
		limparFiltros();
	}

	// ==================== HELPERS ====================

	private boolean isCancelada(Long id) {
		ControleStatusManutencao c = buscarControle(id);
		return c != null && c.getStatus() == ControleStatusManutencao.StatusManutencao.CANCELADA;
	}

	private ControleStatusManutencao buscarControle(Long id) {
		return todosControles.stream()
				.filter(c -> c.getSolicitacaoId().equals(id)
						&& c.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA)
				.findFirst().orElse(null);
	}

	private boolean filtrarPorData(Date data) {
		if (data == null) {
			return true;
		}
		if (filtroDataInicio != null && data.before(filtroDataInicio)) {
			return false;
		}
		if (filtroDataFim != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(filtroDataFim);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			if (data.after(cal.getTime())) {
				return false;
			}
		}
		return true;
	}

	private boolean filtrarPorTipoManutencao(TipoManutencao tipo) {
		if (filtroTipoManutencao == null || filtroTipoManutencao.isEmpty()) {
			return true;
		}
		if (tipo == null) {
			return false;
		}
		return tipo.name().equals(filtroTipoManutencao);
	}

	private boolean filtrarPorCategoria(SolicitacaoManutencaoMaquina m) {
		if (filtroTipoMaquina == null || filtroTipoMaquina.isEmpty()) {
			return true;
		}
		if (m.getMaquina() == null) {
			return false;
		}
		String catNormalizada = normalizarCategoria(m.getMaquina().getTipo());
		return filtroTipoMaquina.equals(catNormalizada);
	}

	// ==================== CÁLCULOS ====================

	private void calcularTotais() {
		// Total concluídas
		totalConcluidas = maquinasFiltradas.stream().filter(m -> {
			ControleStatusManutencao c = buscarControle(m.getId());
			return c != null && c.getStatus() == ControleStatusManutencao.StatusManutencao.CONCLUIDA;
		}).count();

		// Agrupa por categoria normalizada
		Map<String, Long> contagem = maquinasFiltradasSemCanceladas.stream().filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> normalizarCategoria(m.getMaquina().getTipo()),
						Collectors.counting()));

		// Monta mapa com TODAS as categorias (fixas + Especial), mesmo que zero
		manutencoesTotaisPorCategoria = new LinkedHashMap<>();
		for (String cat : CATEGORIAS_FIXAS) {
			manutencoesTotaisPorCategoria.put(cat, contagem.getOrDefault(cat, 0L));
		}
		// Adiciona "Especial" sempre
		manutencoesTotaisPorCategoria.put(CATEGORIA_ESPECIAL, contagem.getOrDefault(CATEGORIA_ESPECIAL, 0L));

		// Ordena por valor decrescente
		manutencoesTotaisPorCategoria = manutencoesTotaisPorCategoria.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

		totalManutencoesCategoria = maquinasFiltradasSemCanceladas.size();
	}

	private void calcularIndicadores() {
		calcularTempoTotal();
		calcularTempoMedio();
		calcularMTTR();
		calcularMTBF();
		calcularPioresAtores();
	}

	private void calcularTempoTotal() {
		Set<Long> ids = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());

		tempoTotalHorasGastas = todosPeriodos.stream()
				.filter(p -> p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA
						&& ids.contains(p.getSolicitacaoId()))
				.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras).sum();
	}

	private void calcularTempoMedio() {
		Set<Long> ids = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());

		double totalH = 0;
		int count = 0;
		for (PeriodoTrabalhoManutencao p : todosPeriodos) {
			if (p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA
					&& ids.contains(p.getSolicitacaoId())) {
				totalH += p.getDuracaoEmHoras();
				count++;
			}
		}
		tempoMedioPorReparo = count > 0 ? totalH / count : 0;
	}

	private void calcularMTTR() {
		Set<Long> ids = maquinasFiltradasSemCanceladas.stream().map(SolicitacaoManutencaoMaquina::getId)
				.collect(Collectors.toSet());

		List<Double> horasPorReparo = new ArrayList<>();
		for (Long id : ids) {
			ControleStatusManutencao c = buscarControle(id);
			if (c != null && c.getStatus() == ControleStatusManutencao.StatusManutencao.CONCLUIDA) {
				horasPorReparo.add(todosPeriodos.stream()
						.filter(p -> p.getSolicitacaoId().equals(id)
								&& p.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA)
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

	private void calcularPioresAtores() {
		pioresAtores = CalculoIndicadores.maiores(maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo(),
						Collectors.counting())), 5);

		// Aqui o gráfico mostra o quanto cada máquina NÃO representa do total
		pioresAtoresPorcentagem = new LinkedHashMap<>();
		long total = maquinasFiltradasSemCanceladas.size();
		if (total > 0) {
			for (Map.Entry<String, Long> e : pioresAtores.entrySet()) {
				pioresAtoresPorcentagem.put(e.getKey(),
						CalculoIndicadores.arredondar(100.0 - e.getValue() * 100.0 / total));
			}
		}
	}

	// ==================== CUSTOS ====================

	private void calcularCustos() {
		// Monta mapa de id -> manutenção para evitar N+1 queries e usar apenas
		// as manutenções já filtradas (período, tipo, categoria, sem canceladas)
		Map<Long, SolicitacaoManutencaoMaquina> mapaManutencoes = maquinasFiltradasSemCanceladas.stream()
				.collect(Collectors.toMap(SolicitacaoManutencaoMaquina::getId, m -> m, (a, b) -> a));

		Map<String, Double> tempCat = new HashMap<>();
		Map<String, Double> tempMaq = new HashMap<>();
		custoTotal = 0;

		for (SolicitacaoCompraMateriaisServicos compra : todasCompras) {
			if (compra.getValorGasto() == null || compra.getValorGasto() <= 0 || (compra
					.getTipoManutencaoReferencia() != TipoManutencaoReferencia.MAQUINA)) {
				continue;
			}

			Long idMnt = compra.getSolicitacaoManutencaoId();
			if (idMnt == null) {
				continue;
			}

			// Só considera compras vinculadas a manutenções que passaram nos filtros
			SolicitacaoManutencaoMaquina mnt = mapaManutencoes.get(idMnt);
			if (mnt == null || mnt.getMaquina() == null) {
				continue;
			}

			double valor = compra.getValorGasto();

			// Custo por categoria
			String cat = normalizarCategoria(mnt.getMaquina().getTipo());
			tempCat.merge(cat, valor, Double::sum);

			// Custo por máquina
			String nome = mnt.getMaquina().getFabricante() + " " + mnt.getMaquina().getModelo();
			tempMaq.merge(nome, valor, Double::sum);

			custoTotal += valor;
		}

		custoPorCategoria = CalculoIndicadores.maiores(tempCat, Integer.MAX_VALUE);
		custoPorMaquina = CalculoIndicadores.maiores(tempMaq, 10);
	}

	// ==================== CONFIABILIDADE ====================

	private void calcularConfiabilidade() {
		calcularConfiabilidadePorMaquina();
		calcularConfiabilidadePorCategoria();
	}

	private void calcularConfiabilidadePorMaquina() {
		confiabilidadePorMaquina = new LinkedHashMap<>();

		Map<String, List<SolicitacaoManutencaoMaquina>> manutencoesPorNome = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> m.getMaquina().getFabricante() + " " + m.getMaquina().getModelo()));

		List<CadastroMaquinaEquipamento> maquinasParaExibir;
		if (filtroTipoMaquina != null && !filtroTipoMaquina.isEmpty()) {
			maquinasParaExibir = listaMaquinas.stream()
					.filter(mq -> filtroTipoMaquina.equals(normalizarCategoria(mq.getTipo())))
					.collect(Collectors.toList());
		} else {
			maquinasParaExibir = new ArrayList<>(listaMaquinas);
		}

		double horasTotalPeriodo = calcularHorasTotalPeriodo();

		for (CadastroMaquinaEquipamento mq : maquinasParaExibir) {
			if (mq.getFabricante() == null || mq.getModelo() == null) {
				continue;
			}
			String nome = mq.getFabricante() + " " + mq.getModelo();
			List<SolicitacaoManutencaoMaquina> lista = manutencoesPorNome.getOrDefault(nome, new ArrayList<>());

			double horasParada = 0;
			for (SolicitacaoManutencaoMaquina m : lista) {
				ControleStatusManutencao c = buscarControle(m.getId());
				if (c != null && m.getDataManutencao() != null) {
					Date fim = c.getDataConclusao() != null ? c.getDataConclusao() : new Date();
					horasParada += CalculoIndicadores.horasUteisEntre(m.getDataManutencao(), fim);
				}
			}

			confiabilidadePorMaquina.put(nome,
					CalculoIndicadores.confiabilidade(horasTotalPeriodo, horasParada, !lista.isEmpty()));
		}

		confiabilidadePorMaquina = CalculoIndicadores.ordenadoCrescente(confiabilidadePorMaquina);
		mediaGeralConfiabilidadeMaquina = CalculoIndicadores.media(confiabilidadePorMaquina.values());
	}

	private void calcularConfiabilidadePorCategoria() {
		confiabilidadePorCategoriaLista = new ArrayList<>();

		// Agrupa manutenções por categoria normalizada
		Map<String, List<SolicitacaoManutencaoMaquina>> porCategoria = maquinasFiltradasSemCanceladas.stream()
				.filter(m -> m.getMaquina() != null)
				.collect(Collectors.groupingBy(m -> normalizarCategoria(m.getMaquina().getTipo())));

		// Monta lista de categorias a exibir
		List<String> categoriasParaExibir = new ArrayList<>(CATEGORIAS_FIXAS);
		categoriasParaExibir.add(CATEGORIA_ESPECIAL);

		if (filtroTipoMaquina != null && !filtroTipoMaquina.isEmpty()) {
			categoriasParaExibir = categoriasParaExibir.stream().filter(c -> c.equals(filtroTipoMaquina))
					.collect(Collectors.toList());
		}

		double horasBase = calcularHorasTotalPeriodo();

		for (String cat : categoriasParaExibir) {
			List<SolicitacaoManutencaoMaquina> lista = porCategoria.getOrDefault(cat, new ArrayList<>());

			long qtdMaquinas = listaMaquinas.stream().filter(mq -> cat.equals(normalizarCategoria(mq.getTipo())))
					.count();
			if (qtdMaquinas == 0) {
				qtdMaquinas = 1;
			}

			double horasParada = 0;
			for (SolicitacaoManutencaoMaquina m : lista) {
				ControleStatusManutencao c = buscarControle(m.getId());
				if (c != null && m.getDataManutencao() != null) {
					Date fim = c.getDataConclusao() != null ? c.getDataConclusao() : new Date();
					horasParada += CalculoIndicadores.horasUteisEntre(m.getDataManutencao(), fim);
				}
			}

			double conf = CalculoIndicadores.confiabilidade(horasBase * qtdMaquinas, horasParada, !lista.isEmpty());

			ConfiabilidadeCategoria item = new ConfiabilidadeCategoria();
			item.setCategoria(cat);
			item.setQuantidadeManutencoes(lista.size());
			item.setConfiabilidade(conf);
			confiabilidadePorCategoriaLista.add(item);
		}

		confiabilidadePorCategoriaLista.sort(
				Comparator.comparingDouble(ConfiabilidadeCategoria::getConfiabilidade));

		mediaGeralConfiabilidadeCategoria = CalculoIndicadores.media(confiabilidadePorCategoriaLista.stream()
				.map(ConfiabilidadeCategoria::getConfiabilidade).collect(Collectors.toList()));
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
		return CalculoIndicadores.horasUteis(fim.getTime() - inicio.getTime());
	}

	// ==================== GETTERS JSON ====================

	// Manutenções por categoria
	public String getManutencoesTotaisPorCategoriaLabelsJSON() {
		return Json.rotulos(manutencoesTotaisPorCategoria);
	}

	public String getManutencoesTotaisPorCategoriaDataJSON() {
		return Json.valoresInteiros(manutencoesTotaisPorCategoria);
	}

	// Piores atores por máquina
	public String getPioresAtoresLabelsJSON() {
		return Json.rotulos(pioresAtores);
	}

	public String getPioresAtoresDataJSON() {
		return Json.valoresInteiros(pioresAtores);
	}

	public String getPioresAtoresPctDataJSON() {
		return Json.valoresDecimais(pioresAtoresPorcentagem);
	}

	// Custo por categoria
	public String getCustoPorCategoriaLabelsJSON() {
		return Json.rotulos(custoPorCategoria);
	}

	public String getCustoPorCategoriaDataJSON() {
		return Json.valoresDecimais(custoPorCategoria);
	}

	// Custo por máquina
	public String getCustoPorMaquinaLabelsJSON() {
		return Json.rotulos(custoPorMaquina);
	}

	public String getCustoPorMaquinaDataJSON() {
		return Json.valoresDecimais(custoPorMaquina);
	}

	// Confiabilidade por máquina
	public String getConfMaquinaLabelsJSON() {
		return Json.rotulos(confiabilidadePorMaquina);
	}

	public String getConfMaquinaDataJSON() {
		return Json.valoresDecimais(confiabilidadePorMaquina);
	}

	// Confiabilidade por categoria
	public String getConfCatLabelsJSON() {
		if (confiabilidadePorCategoriaLista == null || confiabilidadePorCategoriaLista.isEmpty()) {
			return "[]";
		}
		return "[" + confiabilidadePorCategoriaLista.stream().map(i -> "\"" + Json.escapar(i.getCategoria()) + "\"")
				.collect(Collectors.joining(",")) + "]";
	}

	public String getConfCatDataJSON() {
		if (confiabilidadePorCategoriaLista == null || confiabilidadePorCategoriaLista.isEmpty()) {
			return "[]";
		}
		return "[" + confiabilidadePorCategoriaLista.stream()
				.map(i -> String.format(Locale.US, "%.2f", i.getConfiabilidade())).collect(Collectors.joining(","))
				+ "]";
	}

	// ==================== FORMATADORES ====================

	private String formatarHoras(double h) {
		if (h == 0) {
			return "0h";
		}
		int horas = (int) h;
		int min = (int) ((h - horas) * 60);
		return horas + "h " + min + "m";
	}

	public String getTempoTotalFormatado() {
		return formatarHoras(tempoTotalHorasGastas);
	}

	public String getTempoMedioPorReparoFormatado() {
		return formatarHoras(tempoMedioPorReparo);
	}

	public String getMttrFormatado() {
		return formatarHoras(mttr);
	}

	public String getMtbfFormatado() {
		return formatarHoras(mtbf);
	}

	public String getCustoTotalFormatado() {
		return String.format(new Locale.Builder().setLanguage("pt").setRegion("BR").build(), "R$ %,.2f", custoTotal);
	}

	public String getMediaGeralConfMaquinaFormatada() {
		return String.format("%.2f%%", mediaGeralConfiabilidadeMaquina);
	}

	public String getMediaGeralConfCategoriaFormatada() {
		return String.format("%.2f%%", mediaGeralConfiabilidadeCategoria);
	}

	public String getMediaGeralConfMaquinaClass() {
		if (mediaGeralConfiabilidadeMaquina >= 80) {
			return "confiabilidade-alta";
		}
		if (mediaGeralConfiabilidadeMaquina >= 50) {
			return "confiabilidade-media";
		}
		return "confiabilidade-baixa";
	}

	public String getMediaGeralConfCategoriaClass() {
		if (mediaGeralConfiabilidadeCategoria >= 80) {
			return "confiabilidade-alta";
		}
		if (mediaGeralConfiabilidadeCategoria >= 50) {
			return "confiabilidade-media";
		}
		return "confiabilidade-baixa";
	}

	// ==================== GETTERS SIMPLES ====================

	public long getTotalManutencoesCategoria() {
		return totalManutencoesCategoria;
	}

	public long getTotalConcluidas() {
		return totalConcluidas;
	}

	public double getCustoTotal() {
		return custoTotal;
	}

	public double getMttr() {
		return mttr;
	}

	public double getMtbf() {
		return mtbf;
	}

	public double getMediaGeralConfiabilidadeMaquina() {
		return mediaGeralConfiabilidadeMaquina;
	}

	public double getMediaGeralConfiabilidadeCategoria() {
		return mediaGeralConfiabilidadeCategoria;
	}

	public Map<String, Long> getManutencoesTotaisPorCategoria() {
		return manutencoesTotaisPorCategoria;
	}

	public Map<String, Double> getCustoPorCategoria() {
		return custoPorCategoria;
	}

	public List<ConfiabilidadeCategoria> getConfiabilidadePorCategoriaLista() {
		return confiabilidadePorCategoriaLista;
	}

	// Filtros getters/setters
	public Date getFiltroDataInicio() {
		return filtroDataInicio;
	}

	public void setFiltroDataInicio(Date v) {
		this.filtroDataInicio = v;
	}

	public Date getFiltroDataFim() {
		return filtroDataFim;
	}

	public void setFiltroDataFim(Date v) {
		this.filtroDataFim = v;
	}

	public String getFiltroTipoManutencao() {
		return filtroTipoManutencao;
	}

	public void setFiltroTipoManutencao(String v) {
		this.filtroTipoManutencao = v;
	}

	public String getFiltroTipoMaquina() {
		return filtroTipoMaquina;
	}

	public void setFiltroTipoMaquina(String v) {
		this.filtroTipoMaquina = v;
	}

	public String getFiltroPeriodo() {
		return filtroPeriodo;
	}

	public void setFiltroPeriodo(String v) {
		this.filtroPeriodo = v;
	}

	public List<String> getListaTiposMaquina() {
		return listaTiposMaquina;
	}
}