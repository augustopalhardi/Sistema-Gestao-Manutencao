package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.Date;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;

public class ItemManutencaoTecnico implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long solicitacaoId;
	private TipoManutencaoReferencia tipoManutencaoRef;
	private String tipoManutencao;
	private String descricao;
	private String equipamentoLocal;
	private Date dataManutencao;
	private StatusManutencao status;
	private double horasTecnico;
	private boolean cancelada;
	private String urlFoto;
	private String solicitante;
	private String observacoes;

	// Campos específicos de máquina
	private boolean risco;
	private String descrRisco;
	private boolean operando;
	private String descrOperando;

	public Long getSolicitacaoId() { return solicitacaoId; }
	public void setSolicitacaoId(Long solicitacaoId) { this.solicitacaoId = solicitacaoId; }
	public TipoManutencaoReferencia getTipoManutencaoRef() { return tipoManutencaoRef; }
	public void setTipoManutencaoRef(TipoManutencaoReferencia tipoManutencaoRef) { this.tipoManutencaoRef = tipoManutencaoRef; }
	public String getTipoManutencao() { return tipoManutencao; }
	public void setTipoManutencao(String tipoManutencao) { this.tipoManutencao = tipoManutencao; }
	public String getDescricao() { return descricao; }
	public void setDescricao(String descricao) { this.descricao = descricao; }
	public String getEquipamentoLocal() { return equipamentoLocal; }
	public void setEquipamentoLocal(String equipamentoLocal) { this.equipamentoLocal = equipamentoLocal; }
	public Date getDataManutencao() { return dataManutencao; }
	public void setDataManutencao(Date dataManutencao) { this.dataManutencao = dataManutencao; }
	public StatusManutencao getStatus() { return status; }
	public void setStatus(StatusManutencao status) { this.status = status; }
	public double getHorasTecnico() { return horasTecnico; }
	public void setHorasTecnico(double horasTecnico) { this.horasTecnico = horasTecnico; }
	public boolean isCancelada() { return cancelada; }
	public void setCancelada(boolean cancelada) { this.cancelada = cancelada; }
	public String getUrlFoto() { return urlFoto; }
	public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
	public String getSolicitante() { return solicitante; }
	public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
	public String getObservacoes() { return observacoes; }
	public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
	public boolean isRisco() { return risco; }
	public void setRisco(boolean risco) { this.risco = risco; }
	public String getDescrRisco() { return descrRisco; }
	public void setDescrRisco(String descrRisco) { this.descrRisco = descrRisco; }
	public boolean isOperando() { return operando; }
	public void setOperando(boolean operando) { this.operando = operando; }
	public String getDescrOperando() { return descrOperando; }
	public void setDescrOperando(String descrOperando) { this.descrOperando = descrOperando; }
}
