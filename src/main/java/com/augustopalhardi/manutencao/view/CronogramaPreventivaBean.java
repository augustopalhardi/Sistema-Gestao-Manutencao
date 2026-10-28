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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.view.dto.GrupoMes;
import com.augustopalhardi.manutencao.view.dto.ItemPreventiva;

@Named
@ViewScoped
public class CronogramaPreventivaBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CronogramaPreventivaBean.class);

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService maquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService predialService;

	@Inject
	private ManterControleStatusManutencaoService controleStatusService;

	// Lista unificada de itens preventivos
	private List<ItemPreventiva> todosItens = new ArrayList<>();

	// Listas agrupadas por mês/ano para exibição no cronograma, separadas por
	// aba (usa List em vez de Map para funcionar com ui:repeat)
	private List<GrupoMes> gruposPorMesAbertas = new ArrayList<>();
	private List<GrupoMes> gruposPorMesConcluidas = new ArrayList<>();

	// Aba selecionada: "ABERTAS" ou "CONCLUIDAS"
	private String abaAtiva = "ABERTAS";

	// Contadores para os cards de resumo
	private int totalPreventivas;
	private int totalParaFazer;
	private int totalEmAndamento;
	private int totalAguardandoCompra;
	private int totalAtrasadas;
	private int totalConcluidas;

	// Mês/ano atual para navegação
	private int mesAtual;
	private int anoAtual;
	private String labelMesAno;

	// Filtros
	private String filtroCategoria = "";
	private String filtroId = "";
	private Date filtroDataInicio;
	private Date filtroDataFim;

	@PostConstruct
	public void init() {
		Calendar cal = Calendar.getInstance();
		mesAtual = cal.get(Calendar.MONTH);
		anoAtual = cal.get(Calendar.YEAR);
		carregarDados();
	}

	/**
	 * Carrega, em UMA consulta, o status de todas as manutenções, em vez
	 * de consultar o banco uma vez por solicitação dentro do laço.
	 *
	 * A chave do mapa combina o tipo (MAQUINA/PREDIAL) com o id da solicitação,
	 * porque os ids se repetem entre os dois domínios: existe a manutenção de
	 * máquina #7 e a predial #7, e elas têm status independentes.
	 *
	 * Mesmo padrão já usado em CadastroLocalBean e CadastroMaquinaEquipamentoBean.
	 */
	private Map<String, ControleStatusManutencao> carregarCacheControles() {
		Map<String, ControleStatusManutencao> cache = new HashMap<>();

		try {
			for (ControleStatusManutencao c : controleStatusService.buscarTodos()) {
				if (c.getTipoManutencao() != null && c.getSolicitacaoId() != null) {
					cache.put(c.getTipoManutencao().name() + "-" + c.getSolicitacaoId(), c);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar cache de status das preventivas: {}", e.getMessage());
		}

		return cache;
	}

	public void carregarDados() {
		todosItens.clear();
		gruposPorMesAbertas.clear();
		gruposPorMesConcluidas.clear();

		try {
			// Uma consulta só, em vez de uma por solicitação
			Map<String, ControleStatusManutencao> cacheControles = carregarCacheControles();

			// Buscar manutenções preventivas de máquinas (ativas, não canceladas)
			List<SolicitacaoManutencaoMaquina> preventivasMaquina = maquinaService
					.buscarAtivasPorTipo(TipoManutencao.PREVENTIVA);

			for (SolicitacaoManutencaoMaquina sol : preventivasMaquina) {
				ControleStatusManutencao controle = cacheControles
						.get(TipoManutencaoReferencia.MAQUINA.name() + "-" + sol.getId());

				StatusManutencao status = (controle != null) ? controle.getStatus() : StatusManutencao.PARA_FAZER;

				if (status == StatusManutencao.CANCELADA) {
					continue;
				}

				ItemPreventiva item = new ItemPreventiva();
				item.setId(sol.getId());
				item.setCategoria("MAQUINA");
				item.setDescricao(sol.getFalha() != null ? sol.getFalha() : "Sem descrição");
				item.setDataPrevista(sol.getDataManutencao());
				item.setObservacoes(sol.getObservacoes());
				item.setStatus(status);
				item.setSolicitante(sol.getSolicitante());

				if (controle != null && controle.getDataConclusao() != null) {
					item.setDataConclusao(controle.getDataConclusao());
				}

				if (sol.getMaquina() != null) {
					item.setLocalOuMaquina(sol.getMaquina().getIdentificacao());
				} else {
					item.setLocalOuMaquina("Máquina não informada");
				}

				if (sol.getDataManutencao() != null && sol.getDataManutencao().before(new Date())
						&& status != StatusManutencao.EM_ANDAMENTO && status != StatusManutencao.CONCLUIDA) {
					item.setAtrasada(true);
				}

				todosItens.add(item);
			}

			// Buscar manutenções preventivas prediais (ativas, não canceladas)
			List<SolicitacaoManutencaoPredial> preventivasPredial = predialService
					.buscarAtivasPorTipo(TipoManutencao.PREVENTIVA);

			for (SolicitacaoManutencaoPredial sol : preventivasPredial) {
				ControleStatusManutencao controle = cacheControles
						.get(TipoManutencaoReferencia.PREDIAL.name() + "-" + sol.getId());

				StatusManutencao status = (controle != null) ? controle.getStatus() : StatusManutencao.PARA_FAZER;

				if (status == StatusManutencao.CANCELADA) {
					continue;
				}

				ItemPreventiva item = new ItemPreventiva();
				item.setId(sol.getId());
				item.setCategoria("PREDIAL");
				item.setDescricao(sol.getProblema() != null ? sol.getProblema() : "Sem descrição");
				item.setDataPrevista(sol.getDataManutencao());
				item.setObservacoes(sol.getObservacoes());
				item.setStatus(status);
				item.setSolicitante(sol.getSolicitante());

				if (controle != null && controle.getDataConclusao() != null) {
					item.setDataConclusao(controle.getDataConclusao());
				}

				if (sol.getLocal() != null) {
					item.setLocalOuMaquina(sol.getLocal().getNome());
				} else {
					item.setLocalOuMaquina("Local não informado");
				}

				if (sol.getDataManutencao() != null && sol.getDataManutencao().before(new Date())
						&& status != StatusManutencao.EM_ANDAMENTO && status != StatusManutencao.CONCLUIDA) {
					item.setAtrasada(true);
				}

				todosItens.add(item);
			}

			// Aplicar filtros
			List<ItemPreventiva> itensFiltrados = new ArrayList<>(todosItens);

			// Aplicar filtro de ID (busca parcial - contém o número digitado)
			if (filtroId != null && !filtroId.trim().isEmpty()) {
				String idBusca = filtroId.trim().replaceAll("[^0-9]", "");
				if (!idBusca.isEmpty()) {
					itensFiltrados = itensFiltrados.stream()
							.filter(i -> i.getId() != null && String.valueOf(i.getId()).contains(idBusca))
							.collect(Collectors.toList());
				}
			}

			// Aplicar filtro de categoria
			if (filtroCategoria != null && !filtroCategoria.isEmpty()) {
				itensFiltrados = itensFiltrados.stream().filter(i -> i.getCategoria().equals(filtroCategoria))
						.collect(Collectors.toList());
			}

			// A data inicial agora é normalizada para 00:00.
			//
			// Antes, a hora que viesse junto do campo era comparada como estava,
			// e uma preventiva marcada para o próprio dia do filtro podia ficar
			// de fora. O filtro final já fazia esse ajuste para 23:59; faltava o
			// par no começo do período.
			if (filtroDataInicio != null) {
				Calendar calInicio = Calendar.getInstance();
				calInicio.setTime(filtroDataInicio);
				calInicio.set(Calendar.HOUR_OF_DAY, 0);
				calInicio.set(Calendar.MINUTE, 0);
				calInicio.set(Calendar.SECOND, 0);
				calInicio.set(Calendar.MILLISECOND, 0);
				Date dataInicioAjustada = calInicio.getTime();

				itensFiltrados = itensFiltrados.stream()
						.filter(i -> i.getDataPrevista() != null && !i.getDataPrevista().before(dataInicioAjustada))
						.collect(Collectors.toList());
			}

			// Aplicar filtro de data fim
			if (filtroDataFim != null) {
				Calendar calFim = Calendar.getInstance();
				calFim.setTime(filtroDataFim);
				calFim.set(Calendar.HOUR_OF_DAY, 23);
				calFim.set(Calendar.MINUTE, 59);
				calFim.set(Calendar.SECOND, 59);
				calFim.set(Calendar.MILLISECOND, 999);
				Date dataFimAjustada = calFim.getTime();

				itensFiltrados = itensFiltrados.stream()
						.filter(i -> i.getDataPrevista() != null && !i.getDataPrevista().after(dataFimAjustada))
						.collect(Collectors.toList());
			}

			// Separar entre "Em Aberto/Ativas" e "Concluídas"
			List<ItemPreventiva> itensAbertas = itensFiltrados.stream()
					.filter(i -> i.getStatus() != StatusManutencao.CONCLUIDA).collect(Collectors.toList());
			List<ItemPreventiva> itensConcluidas = itensFiltrados.stream()
					.filter(i -> i.getStatus() == StatusManutencao.CONCLUIDA).collect(Collectors.toList());

			// Ativas: ordenar por data crescente (mais próxima do dia atual primeiro, depois futuras)
			itensAbertas.sort(Comparator.comparing(ItemPreventiva::getDataPrevista,
					Comparator.nullsLast(Comparator.naturalOrder())));

			// Concluídas: ordenar por data de conclusão decrescente (mais recentes primeiro, depois mais antigas)
			itensConcluidas.sort(Comparator.comparing(ItemPreventiva::getDataConclusao,
					Comparator.nullsLast(Comparator.reverseOrder())));

			// Agrupar por mês/ano usando LinkedHashMap temporário para manter ordem
			SimpleDateFormat sdfLabel = new SimpleDateFormat("MMMM 'de' yyyy", new Locale.Builder().setLanguage("pt").setRegion("BR").build());

			gruposPorMesAbertas.addAll(agruparPorMes(itensAbertas, sdfLabel, false));
			gruposPorMesConcluidas.addAll(agruparPorMes(itensConcluidas, sdfLabel, true));

			// Calcular contadores (baseados nas abertas/ativas)
			calcularContadores(itensAbertas);
			totalConcluidas = itensConcluidas.size();
			totalPreventivas = itensFiltrados.size();

			// Label do mês atual
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, mesAtual);
			cal.set(Calendar.YEAR, anoAtual);
			labelMesAno = sdfLabel.format(cal.getTime());
			labelMesAno = labelMesAno.substring(0, 1).toUpperCase() + labelMesAno.substring(1);

		} catch (Exception e) {
			LOGGER.error("Erro ao carregar dados do cronograma preventiva: ", e);
		}
	}

	private List<GrupoMes> agruparPorMes(List<ItemPreventiva> itens, SimpleDateFormat sdfLabel, boolean usarDataConclusao) {
		Map<String, List<ItemPreventiva>> mapaTemp = new LinkedHashMap<>();

		for (ItemPreventiva item : itens) {
			String chave;
			Date dataParaAgrupar = usarDataConclusao && item.getDataConclusao() != null
					? item.getDataConclusao()
					: item.getDataPrevista();

			if (dataParaAgrupar != null) {
				chave = sdfLabel.format(dataParaAgrupar);
				chave = chave.substring(0, 1).toUpperCase() + chave.substring(1);
			} else {
				chave = "Sem data definida";
			}
			mapaTemp.computeIfAbsent(chave, k -> new ArrayList<>()).add(item);
		}

		List<GrupoMes> grupos = new ArrayList<>();
		for (Map.Entry<String, List<ItemPreventiva>> entry : mapaTemp.entrySet()) {
			grupos.add(new GrupoMes(entry.getKey(), entry.getValue()));
		}
		return grupos;
	}

	private void calcularContadores(List<ItemPreventiva> itens) {
		totalPreventivas = itens.size();
		totalParaFazer = 0;
		totalEmAndamento = 0;
		totalAguardandoCompra = 0;
		totalAtrasadas = 0;

		for (ItemPreventiva item : itens) {
			switch (item.getStatus()) {
			case PARA_FAZER:
				totalParaFazer++;
				break;
			case EM_ANDAMENTO:
				totalEmAndamento++;
				break;
			case AGUARDANDO_COMPRA:
				totalAguardandoCompra++;
				break;
			default:
				break;
			}
			if (item.isAtrasada()) {
				totalAtrasadas++;
			}
		}
	}

	public void filtrarPorCategoria() {
		carregarDados();
	}

	public void limparFiltros() {
		filtroCategoria = "";
		filtroId = "";
		filtroDataInicio = null;
		filtroDataFim = null;
		carregarDados();
	}

	/**
	 * Trocar de aba agora reaplica os filtros.
	 *
	 * Antes só o botão "Filtrar" chamava carregarDados(). Quem digitasse um
	 * filtro e clicasse direto em uma aba via o campo preenchido na tela, mas a
	 * lista continuava mostrando o resultado da filtragem anterior — parecia que
	 * o filtro não funcionava.
	 */
	public void mudarAba(String aba) {
		this.abaAtiva = aba;
		carregarDados();
	}

	// ==========================================================
	// INDICAÇÃO DE FILTROS ATIVOS
	//
	// Mesmo comportamento da tela de compras: sem isso, um filtro esquecido lá
	// em cima deixa o cronograma vazio sem nenhuma explicação na tela.
	// ==========================================================

	/** Há algum filtro da barra superior preenchido? */
	public boolean isFiltrosAtivos() {
		return (filtroId != null && !filtroId.trim().isEmpty())
				|| (filtroCategoria != null && !filtroCategoria.isEmpty()) || filtroDataInicio != null
				|| filtroDataFim != null;
	}

	/** Texto legível dos filtros ativos, exibido em uma faixa acima da lista. */
	public String getResumoFiltros() {
		List<String> partes = new ArrayList<>();

		if (filtroId != null && !filtroId.trim().isEmpty()) {
			partes.add("OS contendo \"" + filtroId.trim() + "\"");
		}

		if ("MAQUINA".equals(filtroCategoria)) {
			partes.add("categoria: Máquinas e Equipamentos");
		} else if ("PREDIAL".equals(filtroCategoria)) {
			partes.add("categoria: Manutenção Predial");
		}

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

		if (filtroDataInicio != null && filtroDataFim != null) {
			partes.add("de " + sdf.format(filtroDataInicio) + " até " + sdf.format(filtroDataFim));
		} else if (filtroDataInicio != null) {
			partes.add("a partir de " + sdf.format(filtroDataInicio));
		} else if (filtroDataFim != null) {
			partes.add("até " + sdf.format(filtroDataFim));
		}

		return String.join(" · ", partes);
	}

	public String getStatusLabel(StatusManutencao status) {
		if (status == null) {
			return "Não definido";
		}
		switch (status) {
		case PARA_FAZER:
			return "Para Fazer";
		case EM_ANDAMENTO:
			return "Em Andamento";
		case AGUARDANDO_COMPRA:
			return "Aguardando Compra";
		case CONCLUIDA:
			return "Concluída";
		default:
			return status.name();
		}
	}

	public String getStatusClass(ItemPreventiva item) {
		if (item.isAtrasada()) {
			return "status-atrasada";
		}
		if (item.getStatus() == null) {
			return "status-para-fazer";
		}
		switch (item.getStatus()) {
		case PARA_FAZER:
			return "status-para-fazer";
		case EM_ANDAMENTO:
			return "status-em-andamento";
		case AGUARDANDO_COMPRA:
			return "status-aguardando";
		case CONCLUIDA:
			return "status-concluida";
		default:
			return "status-para-fazer";
		}
	}

	public String getCategoriaLabel(String categoria) {
		if ("MAQUINA".equals(categoria)) {
			return "Máquina/Equipamento";
		} else if ("PREDIAL".equals(categoria)) {
			return "Predial";
		}
		return categoria;
	}

	public String getCategoriaClass(String categoria) {
		if ("MAQUINA".equals(categoria)) {
			return "cat-maquina";
		} else if ("PREDIAL".equals(categoria)) {
			return "cat-predial";
		}
		return "";
	}

	public boolean isHoje(Date data) {
		if (data == null) {
			return false;
		}
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(data);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
	}

	public boolean isSemanaAtual(Date data) {
		if (data == null) {
			return false;
		}
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(data);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
				&& cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
	}

	public String getDiaSemana(Date data) {
		if (data == null) {
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("EEE", new Locale.Builder().setLanguage("pt").setRegion("BR").build());
		return sdf.format(data).toUpperCase();
	}

	public String getDiaNumero(Date data) {
		if (data == null) {
			return "--";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd");
		return sdf.format(data);
	}

	public String getMesAbreviado(Date data) {
		if (data == null) {
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MMM", new Locale.Builder().setLanguage("pt").setRegion("BR").build());
		return sdf.format(data).toUpperCase();
	}

	public boolean isListaVazia() {
		List<GrupoMes> lista = getGruposPorMes();
		return lista == null || lista.isEmpty();
	}

	// Getters e Setters
	public List<ItemPreventiva> getTodosItens() {
		return todosItens;
	}

	public List<GrupoMes> getGruposPorMes() {
		return "CONCLUIDAS".equals(abaAtiva) ? gruposPorMesConcluidas : gruposPorMesAbertas;
	}

	public String getAbaAtiva() {
		return abaAtiva;
	}

	public void setAbaAtiva(String abaAtiva) {
		this.abaAtiva = abaAtiva;
	}

	public int getTotalConcluidas() {
		return totalConcluidas;
	}

	public int getTotalPreventivas() {
		return totalPreventivas;
	}

	/**
	 * Total de preventivas em aberto (total menos concluídas).
	 *
	 * Existe para o badge da aba e o card não precisarem fazer a subtração no
	 * EL, onde ela ficava repetida e sem nome.
	 */
	public int getTotalEmAberto() {
		return totalPreventivas - totalConcluidas;
	}

	public int getTotalParaFazer() {
		return totalParaFazer;
	}

	public int getTotalEmAndamento() {
		return totalEmAndamento;
	}

	public int getTotalAguardandoCompra() {
		return totalAguardandoCompra;
	}

	public int getTotalAtrasadas() {
		return totalAtrasadas;
	}

	public String getLabelMesAno() {
		return labelMesAno;
	}

	public String getFiltroCategoria() {
		return filtroCategoria;
	}

	public void setFiltroCategoria(String filtroCategoria) {
		this.filtroCategoria = filtroCategoria;
	}

	public String getFiltroId() {
		return filtroId;
	}

	public void setFiltroId(String filtroId) {
		this.filtroId = filtroId;
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

	// ========== Classe interna para representar um grupo de mês no cronograma
	// ==========
	// ========== Classe interna para representar um item do cronograma ==========
	// ========== MÉTODOS PARA REDIRECIONAR PARA INICIAR MANUTENÇÃO ==========

	private ItemPreventiva itemParaIniciar;

	public void selecionarParaIniciar(ItemPreventiva item) {
		this.itemParaIniciar = item;
	}

	public String redirecionarParaIniciar() {
		if (itemParaIniciar == null) {
			return null;
		}
		String pagina = "/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoPreventiva.xhtml";
		return pagina + "?faces-redirect=true&abrirIniciarId=" + itemParaIniciar.getId()
				+ "&abrirIniciarTipo=" + itemParaIniciar.getCategoria();
	}

	public ItemPreventiva getItemParaIniciar() {
		return itemParaIniciar;
	}
}