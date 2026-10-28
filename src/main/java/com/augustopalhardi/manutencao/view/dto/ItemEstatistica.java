package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.Date;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao;

public class ItemEstatistica implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long id;
	private String tipo;
	private String falha;
	private String problema;
	private String local;
	private String maquina;
	private Date dataManutencao;
	private boolean risco;
	private boolean riscoSeguranca;
	private boolean urgente;
	private boolean cancelada;
	private ControleStatusManutencao.StatusManutencao status;

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

	public boolean isCancelada() {
		return cancelada;
	}

	public void setCancelada(boolean cancelada) {
		this.cancelada = cancelada;
	}

	public ControleStatusManutencao.StatusManutencao getStatus() {
		return status;
	}

	public void setStatus(ControleStatusManutencao.StatusManutencao status) {
		this.status = status;
	}
}
