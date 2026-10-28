package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.Date;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;

public class ItemManutencao implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String tipo;
	private String falha;
	private String problema;
	private String local;
	private String maquina;
	private Date dataManutencao;
	private Date dataInicio;
	private Date dataConclusao;
	private Date dataCancelamento;
	private boolean risco;
	private boolean riscoSeguranca;
	private boolean urgente;
	private boolean operando;
	private boolean cancelada;
	private String motivoCancelamento;
	private String solucao;
	private StatusManutencao status;
	private double totalHorasTrabalhadas;
	private SolicitacaoManutencaoMaquina solicitacaoMaquina;
	private SolicitacaoManutencaoPredial solicitacaoPredial;

	// ADICIONADO: Propriedades para dados de solicitação de compra
	private Date dataSolicitacaoCompra;
	private String descricaoSolicitacaoCompra;

	public ItemManutencao(SolicitacaoManutencaoMaquina maquina, ControleStatusManutencao controle,
			Double totalHoras) {
		this.solicitacaoMaquina = maquina;
		this.id = maquina.getId();
		this.tipo = "MAQUINA";
		this.falha = maquina.getFalha();
		this.dataManutencao = maquina.getDataManutencao();
		this.risco = maquina.isRisco();
		this.operando = maquina.isOperando();
		this.cancelada = maquina.isCancelada();
		this.motivoCancelamento = maquina.getMotivoCancelamento();
		this.dataCancelamento = maquina.getDataCancelamento();
		this.totalHorasTrabalhadas = totalHoras != null ? totalHoras : 0.0;
		// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
		// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
		// na entidade CadastroMaquinaEquipamento.
		this.maquina = maquina.getMaquina() != null
				? maquina.getMaquina().getIdentificacao()
				: "N/A";

		if (controle != null) {
			this.status = controle.getStatus();
			this.dataInicio = controle.getDataInicio();
			this.dataConclusao = controle.getDataConclusao();
			this.solucao = controle.getSolucao();
		} else {
			this.status = StatusManutencao.PARA_FAZER;
		}
	}

	public ItemManutencao(SolicitacaoManutencaoPredial predial, ControleStatusManutencao controle,
			Double totalHoras) {
		this.solicitacaoPredial = predial;
		this.id = predial.getId();
		this.tipo = "PREDIAL";
		this.problema = predial.getProblema();
		this.dataManutencao = predial.getDataManutencao();
		this.riscoSeguranca = predial.isRiscoSeguranca();
		this.urgente = predial.isUrgente();
		this.cancelada = predial.isCancelada();
		this.motivoCancelamento = predial.getMotivoCancelamento();
		this.dataCancelamento = predial.getDataCancelamento();
		this.totalHorasTrabalhadas = totalHoras != null ? totalHoras : 0.0;
		this.local = predial.getLocal() != null ? predial.getLocal().getNome() : "N/A";

		if (controle != null) {
			this.status = controle.getStatus();
			this.dataInicio = controle.getDataInicio();
			this.dataConclusao = controle.getDataConclusao();
			this.solucao = controle.getSolucao();
		} else {
			this.status = StatusManutencao.PARA_FAZER;
		}
	}

	// Getters e Setters (nomes das propriedades sem acento/cedilha)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getFalha() {
		return falha;
	}

	public void setFalha(String falha) {
		this.falha = falha;
	}

	public String getProblema() {
		return problema;
	}

	public void setProblema(String problema) {
		this.problema = problema;
	}

	public String getLocal() {
		return local;
	}

	public void setLocal(String local) {
		this.local = local;
	}

	public String getMaquina() {
		return maquina;
	}

	public void setMaquina(String maquina) {
		this.maquina = maquina;
	}

	public Date getDataManutencao() {
		return dataManutencao;
	}

	public void setDataManutencao(Date dataManutencao) {
		this.dataManutencao = dataManutencao;
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataConclusao() {
		return dataConclusao;
	}

	public void setDataConclusao(Date dataConclusao) {
		this.dataConclusao = dataConclusao;
	}

	public Date getDataCancelamento() {
		return dataCancelamento;
	}

	public void setDataCancelamento(Date dataCancelamento) {
		this.dataCancelamento = dataCancelamento;
	}

	public boolean isRisco() {
		return risco;
	}

	public void setRisco(boolean risco) {
		this.risco = risco;
	}

	public boolean isRiscoSeguranca() {
		return riscoSeguranca;
	}

	public void setRiscoSeguranca(boolean riscoSeguranca) {
		this.riscoSeguranca = riscoSeguranca;
	}

	public boolean isUrgente() {
		return urgente;
	}

	public void setUrgente(boolean urgente) {
		this.urgente = urgente;
	}

	public boolean isOperando() {
		return operando;
	}

	public void setOperando(boolean operando) {
		this.operando = operando;
	}

	public boolean isCancelada() {
		return cancelada;
	}

	public void setCancelada(boolean cancelada) {
		this.cancelada = cancelada;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public String getSolucao() {
		return solucao;
	}

	public void setSolucao(String solucao) {
		this.solucao = solucao;
	}

	public StatusManutencao getStatus() {
		return status;
	}

	public void setStatus(StatusManutencao status) {
		this.status = status;
	}

	public double getTotalHorasTrabalhadas() {
		return totalHorasTrabalhadas;
	}

	public void setTotalHorasTrabalhadas(double totalHorasTrabalhadas) {
		this.totalHorasTrabalhadas = totalHorasTrabalhadas;
	}

	public SolicitacaoManutencaoMaquina getSolicitacaoMaquina() {
		return solicitacaoMaquina;
	}

	public void setSolicitacaoMaquina(SolicitacaoManutencaoMaquina solicitacaoMaquina) {
		this.solicitacaoMaquina = solicitacaoMaquina;
	}

	public SolicitacaoManutencaoPredial getSolicitacaoPredial() {
		return solicitacaoPredial;
	}

	public void setSolicitacaoPredial(SolicitacaoManutencaoPredial solicitacaoPredial) {
		this.solicitacaoPredial = solicitacaoPredial;
	}

	// GETTERS E SETTERS ADICIONADOS
	public Date getDataSolicitacaoCompra() {
		return dataSolicitacaoCompra;
	}

	public void setDataSolicitacaoCompra(Date dataSolicitacaoCompra) {
		this.dataSolicitacaoCompra = dataSolicitacaoCompra;
	}

	public String getDescricaoSolicitacaoCompra() {
		return descricaoSolicitacaoCompra;
	}

	public void setDescricaoSolicitacaoCompra(String descricaoSolicitacaoCompra) {
		this.descricaoSolicitacaoCompra = descricaoSolicitacaoCompra;
	}
}
