package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;

public class ItemPreventiva implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String categoria; // "MAQUINA" ou "PREDIAL"
	private String descricao;
	private Date dataPrevista;
	private String observacoes;
	private StatusManutencao status;
	private String localOuMaquina;
	private String solicitante;
	private boolean atrasada;
	private Date dataConclusao;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Date getDataPrevista() {
		return dataPrevista;
	}

	public void setDataPrevista(Date dataPrevista) {
		this.dataPrevista = dataPrevista;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public StatusManutencao getStatus() {
		return status;
	}

	public void setStatus(StatusManutencao status) {
		this.status = status;
	}

	public String getLocalOuMaquina() {
		return localOuMaquina;
	}

	public void setLocalOuMaquina(String localOuMaquina) {
		this.localOuMaquina = localOuMaquina;
	}

	public String getSolicitante() {
		return solicitante;
	}

	public void setSolicitante(String solicitante) {
		this.solicitante = solicitante;
	}

	public boolean isAtrasada() {
		return atrasada;
	}

	public void setAtrasada(boolean atrasada) {
		this.atrasada = atrasada;
	}

	public Date getDataConclusao() {
		return dataConclusao;
	}

	public void setDataConclusao(Date dataConclusao) {
		this.dataConclusao = dataConclusao;
	}

	/**
	 * Referência (id + tipo) usada para abrir o diálogo de detalhes,
	 * reaproveitando o método prepararDetalhes(Map) do TodasSolicitacoesBean
	 * — o mesmo usado na página "Todas as Solicitações".
	 */
	public Map<String, Object> getDetalhesRef() {
		Map<String, Object> ref = new HashMap<>();
		ref.put("id", id);
		ref.put("tipo", "MAQUINA".equals(categoria) ? "MÁQUINA" : "PREDIAL");
		return ref;
	}
}
