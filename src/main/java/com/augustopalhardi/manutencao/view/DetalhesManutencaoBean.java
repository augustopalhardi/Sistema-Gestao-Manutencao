package com.augustopalhardi.manutencao.view;

import com.augustopalhardi.manutencao.util.Uploads;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterHistoricoReprogramacaoService;
import com.augustopalhardi.manutencao.service.ManterPeriodoTrabalhoManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;

/**
 * Bean central e reutilizável para o dialog de detalhes de manutenção.
 * Usado por todas as páginas que exibem o dialog "Detalhes da Solicitação".
 *
 * Uso: qualquer página chama {@code detalhesManutencaoBean.abrirDetalhes(id, tipo)}
 * e inclui o fragment {@code detalhesManutencaoDialog.xhtml} via ui:include.
 */
@Named
@SessionScoped
public class DetalhesManutencaoBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(DetalhesManutencaoBean.class);

	// ===== SERVICES =====

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusService;

	@Inject
	private ManterPeriodoTrabalhoManutencaoService manterPeriodoTrabalhoService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterCompraService;

	@Inject
	private ManterHistoricoReprogramacaoService manterHistoricoReprogramacaoService;

	// ===== DIALOG GLOBAL DE IMAGEM =====

	@Inject
	private VisualizarImagemBean visualizarImagemBean;

	// ===== DADOS DO DIALOG =====

	// Identificação
	private Long solicitacaoId;
	private TipoManutencaoReferencia tipoRef;
	private boolean aberto;

	// Dados da solicitação (máquina OU predial)
	private SolicitacaoManutencaoMaquina solicitacaoMaquina;
	private SolicitacaoManutencaoPredial solicitacaoPredial;

	// Dados técnicos
	private ControleStatusManutencao controleStatus;
	private List<PeriodoTrabalhoManutencao> periodos = new ArrayList<>();
	private List<SolicitacaoCompraMateriaisServicos> compras = new ArrayList<>();
	private List<HistoricoReprogramacao> historicoReprogramacoes = new ArrayList<>();

	// Calculados
	private double totalHorasTrabalhadas;
	private String tecnicosResponsaveis;

	// ===== MÉTODO PRINCIPAL =====

	/**
	 * Abre o dialog carregando todos os dados da manutenção.
	 * Chamado por qualquer página antes de exibir o dialog.
	 *
	 * @param id   ID da solicitação de manutenção
	 * @param tipo TipoManutencaoReferencia (MAQUINA ou PREDIAL)
	 */
	public void abrirDetalhes(Long id, TipoManutencaoReferencia tipo) {
		limpar();
		this.solicitacaoId = id;
		this.tipoRef = tipo;
		this.aberto = true;

		try {
			carregarSolicitacao();
			carregarControleStatus();
			carregarPeriodosTrabalho();
			carregarCompras();
			carregarHistoricoReprogramacoes();
			calcularDadosDerivados();
		} catch (Exception e) {
			LOG.error("Erro ao abrir detalhes da manutenção {} ({}): ", id, tipo, e);
		}
	}

	/**
	 * Sobrecarga para chamadas com String (útil em EL expressions).
	 * Aceita tanto o enum ("MAQUINA"/"PREDIAL") quanto o label com acento ("MÁQUINA").
	 */
	public void abrirDetalhes(Long id, String tipo) {
		TipoManutencaoReferencia tipoRef;
		try {
			// Normaliza: "MÁQUINA" → "MAQUINA"
			String tipoNormalizado = tipo.replace("Á", "A");
			tipoRef = TipoManutencaoReferencia.valueOf(tipoNormalizado);
		} catch (Exception e) {
			LOG.error("Tipo de manutenção inválido: {}", tipo);
			return;
		}
		abrirDetalhes(id, tipoRef);
	}

	private void limpar() {
		solicitacaoId = null;
		tipoRef = null;
		aberto = false;
		solicitacaoMaquina = null;
		solicitacaoPredial = null;
		controleStatus = null;
		periodos = new ArrayList<>();
		compras = new ArrayList<>();
		historicoReprogramacoes = new ArrayList<>();
		totalHorasTrabalhadas = 0;
		tecnicosResponsaveis = null;
	}

	// ===== CARREGAMENTO DE DADOS =====

	private void carregarSolicitacao() {
		try {
			if (tipoRef == TipoManutencaoReferencia.MAQUINA) {
				solicitacaoMaquina = manterMaquinaService.buscarPorId(solicitacaoId);
			} else if (tipoRef == TipoManutencaoReferencia.PREDIAL) {
				solicitacaoPredial = manterPredialService.buscarPorId(solicitacaoId);
			}
		} catch (Exception e) {
			LOG.error("Erro ao carregar solicitação {} ({}): ", solicitacaoId, tipoRef, e);
		}
	}

	private void carregarControleStatus() {
		try {
			controleStatus = manterControleStatusService.buscarPorSolicitacao(solicitacaoId, tipoRef);
		} catch (Exception e) {
			LOG.error("Erro ao carregar controle de status: ", e);
		}
	}

	private void carregarPeriodosTrabalho() {
		try {
			periodos = manterPeriodoTrabalhoService.buscarPorSolicitacao(solicitacaoId, tipoRef);
		} catch (Exception e) {
			LOG.error("Erro ao carregar períodos de trabalho: ", e);
			periodos = new ArrayList<>();
		}
	}

	private void carregarCompras() {
		try {
			List<SolicitacaoCompraMateriaisServicos> todasCompras = manterCompraService.buscarTodas();
			compras = todasCompras.stream()
					.filter(c -> c.getSolicitacaoManutencaoId() != null
							&& c.getSolicitacaoManutencaoId().equals(solicitacaoId)
							&& c.getTipoManutencaoReferencia() == tipoRef)
					.collect(Collectors.toList());
		} catch (Exception e) {
			LOG.error("Erro ao carregar compras: ", e);
			compras = new ArrayList<>();
		}
	}

	private void carregarHistoricoReprogramacoes() {
		try {
			historicoReprogramacoes = manterHistoricoReprogramacaoService
					.buscarHistoricoPorSolicitacao(solicitacaoId, tipoRef);
		} catch (Exception e) {
			LOG.error("Erro ao carregar histórico de reprogramações: ", e);
			historicoReprogramacoes = new ArrayList<>();
		}
	}

	private void calcularDadosDerivados() {
		// Total de horas trabalhadas
		totalHorasTrabalhadas = periodos.stream()
				.mapToDouble(PeriodoTrabalhoManutencao::getDuracaoEmHoras)
				.sum();

		// Lista de técnicos responsáveis (nomes distintos)
		List<String> nomes = periodos.stream()
				.filter(p -> p.getTecnicoResponsavel() != null)
				.map(p -> p.getTecnicoResponsavel().getNome())
				.distinct()
				.collect(Collectors.toList());
		tecnicosResponsaveis = nomes.isEmpty() ? null : String.join(", ", nomes);
	}

	// ===== GETTERS UNIVERSAIS (usados pelo dialog XHTML) =====

	/** ID da solicitação */
	public Long getSolicitacaoId() {
		return solicitacaoId;
	}

	/** Se é MAQUINA ou PREDIAL (para uso em EL: #{bean.tipoMaquina}) */
	public boolean isTipoMaquina() {
		return tipoRef == TipoManutencaoReferencia.MAQUINA;
	}

	public boolean isTipoPredial() {
		return tipoRef == TipoManutencaoReferencia.PREDIAL;
	}

	/** Label do tipo: "MÁQUINA" ou "PREDIAL" */
	public String getTipoLabel() {
		if (tipoRef == null) return "N/A";
		return tipoRef == TipoManutencaoReferencia.MAQUINA ? "MÁQUINA" : "PREDIAL";
	}

	/** Solicitante */
	public String getSolicitante() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getSolicitante();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getSolicitante();
		return null;
	}

	/** Tipo de manutenção (CORRETIVA, PREVENTIVA, PREDITIVA) */
	public String getTipoManutencao() {
		if (isTipoMaquina() && solicitacaoMaquina != null && solicitacaoMaquina.getTipoManutencao() != null) {
			return solicitacaoMaquina.getTipoManutencao().name();
		}
		if (isTipoPredial() && solicitacaoPredial != null && solicitacaoPredial.getTipoManutencao() != null) {
			return solicitacaoPredial.getTipoManutencao().name();
		}
		return "N/A";
	}

	/** Data da manutenção */
	public Date getDataManutencao() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getDataManutencao();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getDataManutencao();
		return null;
	}

	/** Observações */
	public String getObservacoes() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getObservacoes();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getObservacoes();
		return null;
	}

	/** URL da foto */
	public String getUrlFoto() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getUrlFoto();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getUrlFoto();
		return null;
	}

	/** URL da foto corrigida (para o ImageServlet) */
	public String getUrlFotoCorrigida() {
		String foto = getUrlFoto();
		if (foto == null) return null;
		if (foto.startsWith("/")) return foto;
		return Uploads.URL_BASE + foto;
	}

	/**
	 * Prepara o dialog GLOBAL de imagem com a foto da MÁQUINA relacionada
	 * a esta solicitação (não a foto da solicitação em si).
	 */
	public void verFotoMaquina() {
		CadastroMaquinaEquipamento maquina = getMaquina();

		if (maquina == null) {
			visualizarImagemBean.abrir("Foto da Máquina", null);
			visualizarImagemBean.setMensagemSemFoto("Nenhuma máquina vinculada a esta solicitação.");
			return;
		}

		// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
		// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
		// na entidade CadastroMaquinaEquipamento.
		visualizarImagemBean.abrir("Foto da Máquina",
				maquina.getIdentificacao(),
				"maquinas_equipamentos/" + maquina.getUrlFoto());

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a esta máquina.");
		visualizarImagemBean.addInfo("Nº Patrimônio", maquina.getNumeroPatrimonio());
		visualizarImagemBean.addInfo("Fabricante", maquina.getFabricante());
		visualizarImagemBean.addInfo("Modelo", maquina.getModelo());
		visualizarImagemBean.addInfo("Tipo", maquina.getTipo());
		visualizarImagemBean.addInfo("Localização", maquina.getLocalizacao());
	}

	/**
	 * Prepara o dialog GLOBAL de imagem com a foto do LOCAL relacionado
	 * a esta solicitação predial.
	 */
	public void verFotoLocal() {
		CadastroLocal local = getLocal();

		if (local == null) {
			visualizarImagemBean.abrir("Foto do Local", null);
			visualizarImagemBean.setMensagemSemFoto("Nenhum local vinculado a esta solicitação.");
			return;
		}

		visualizarImagemBean.abrir("Foto do Local", local.getNome(),
				"locais/" + local.getUrlFoto());

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a este local.");
		visualizarImagemBean.addInfo("Nome", local.getNome());
		visualizarImagemBean.addInfo("Área (m²)", local.getArea());
		visualizarImagemBean.addInfo("Observações", local.getObservacoes());
	}

	/**
	 * Prepara o dialog GLOBAL de imagem com a foto desta solicitação.
	 * Toda a marcação do dialog fica em
	 * /includes/visualizarImagemDialog.xhtml.
	 */
	public void verImagem() {
		visualizarImagemBean.abrir("Imagem da Solicitação",
				"Solicitação #" + getSolicitacaoId() + " - " + getTipoLabel(),
				getUrlFoto());

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a esta solicitação.");
		visualizarImagemBean.addInfo("Solicitante", getSolicitante());
		visualizarImagemBean.addInfo("Tipo de Manutenção", getTipoManutencao());
	}

	/** Cancelada? */
	public boolean isCancelada() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.isCancelada();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.isCancelada();
		return false;
	}

	/** Motivo do cancelamento */
	public String getMotivoCancelamento() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getMotivoCancelamento();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getMotivoCancelamento();
		return null;
	}

	/** Data do cancelamento */
	public Date getDataCancelamento() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getDataCancelamento();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getDataCancelamento();
		return null;
	}

	// ===== GETTERS ESPECÍFICOS DE MÁQUINA =====

	public SolicitacaoManutencaoMaquina getSolicitacaoMaquina() {
		return solicitacaoMaquina;
	}

	public CadastroMaquinaEquipamento getMaquina() {
		return solicitacaoMaquina != null ? solicitacaoMaquina.getMaquina() : null;
	}

	public String getFalha() {
		return solicitacaoMaquina != null ? solicitacaoMaquina.getFalha() : null;
	}

	public boolean isRisco() {
		return solicitacaoMaquina != null && solicitacaoMaquina.isRisco();
	}

	public String getDescrRisco() {
		if (isTipoMaquina() && solicitacaoMaquina != null) return solicitacaoMaquina.getDescrRisco();
		if (isTipoPredial() && solicitacaoPredial != null) return solicitacaoPredial.getDescrRisco();
		return null;
	}

	public boolean isOperando() {
		return solicitacaoMaquina != null && solicitacaoMaquina.isOperando();
	}

	public String getDescrOperando() {
		return solicitacaoMaquina != null ? solicitacaoMaquina.getDescrOperando() : null;
	}

	public Double getHorimetroFalha() {
		return solicitacaoMaquina != null ? solicitacaoMaquina.getHorimetroFalha() : null;
	}

	public Boolean getManutencaoEletrica() {
		return solicitacaoMaquina != null ? solicitacaoMaquina.getManutencaoEletrica() : null;
	}

	// ===== GETTERS ESPECÍFICOS DE PREDIAL =====

	public SolicitacaoManutencaoPredial getSolicitacaoPredial() {
		return solicitacaoPredial;
	}

	public CadastroLocal getLocal() {
		return solicitacaoPredial != null ? solicitacaoPredial.getLocal() : null;
	}

	public String getProblema() {
		return solicitacaoPredial != null ? solicitacaoPredial.getProblema() : null;
	}

	public boolean isRiscoSeguranca() {
		return solicitacaoPredial != null && solicitacaoPredial.isRiscoSeguranca();
	}

	public boolean isUrgente() {
		return solicitacaoPredial != null && solicitacaoPredial.isUrgente();
	}

	public String getDescrUrgente() {
		return solicitacaoPredial != null ? solicitacaoPredial.getDescrUrgente() : null;
	}

	// ===== GETTERS DE DADOS TÉCNICOS =====

	public ControleStatusManutencao getControleStatus() {
		return controleStatus;
	}

	public List<PeriodoTrabalhoManutencao> getPeriodos() {
		return periodos;
	}

	public List<SolicitacaoCompraMateriaisServicos> getCompras() {
		return compras;
	}

	public List<HistoricoReprogramacao> getHistoricoReprogramacoes() {
		return historicoReprogramacoes;
	}

	public double getTotalHorasTrabalhadas() {
		return totalHorasTrabalhadas;
	}

	public String getTecnicosResponsaveis() {
		return tecnicosResponsaveis;
	}

	public boolean isAberto() {
		return aberto;
	}

	// ===== MÉTODOS DE VERIFICAÇÃO PARA ABAS =====

	/** Verifica se tem dados técnicos para exibir a aba "Manutenção Técnica" */
	public boolean isTemDadosTecnicos() {
		if (controleStatus == null) return false;
		return controleStatus.getDataInicio() != null
				|| controleStatus.getSolucao() != null
				|| controleStatus.getObservacoesFinais() != null
				|| controleStatus.isBbsNearMiss()
				|| !periodos.isEmpty()
				|| !compras.isEmpty();
	}

	/** Verifica se é BBS/Near Miss */
	public boolean isBbsNearMiss() {
		return controleStatus != null
				&& controleStatus.isBbsNearMiss();
	}

	// ===== MÉTODOS UTILITÁRIOS =====

	/** Retorna o label do status da manutenção */
	public String getStatusLabel() {
		if (controleStatus == null) {
			return isCancelada() ? "Cancelada" : "Para Fazer";
		}
		StatusManutencao status = controleStatus.getStatus();
		if (status == null) return "Para Fazer";
		switch (status) {
			case PARA_FAZER: return "PARA FAZER";
			case EM_ANDAMENTO: return "EM ANDAMENTO";
			case CONCLUIDA: return "CONCLUÍDA";
			case CANCELADA: return "CANCELADA";
			case AGUARDANDO_COMPRA: return "AGUARDANDO COMPRA";
			default: return status.name();
		}
	}

	/** Retorna a cor de fundo do badge de status */
	public String getStatusColor() {
		String label = getStatusLabel();
		switch (label) {
			case "PARA FAZER": return "#3498db";
			case "EM ANDAMENTO": return "#f39c12";
			case "CONCLUÍDA": return "#27ae60";
			case "AGUARDANDO COMPRA": return "#9b59b6";
			case "CANCELADA": return "#e74c3c";
			default: return "#95a5a6";
		}
	}

	/** Formata horas decimais como "Xh Ym" */
	public String formatarHoras(double horas) {
		if (horas == 0) return "0h";
		int horasInteiras = (int) horas;
		int minutos = (int) ((horas - horasInteiras) * 60);
		if (minutos == 0) return horasInteiras + "h";
		return horasInteiras + "h " + String.format("%02d", minutos) + "m";
	}

	/** Total de horas formatado */
	public String getTotalHorasFormatado() {
		return formatarHoras(totalHorasTrabalhadas);
	}
}