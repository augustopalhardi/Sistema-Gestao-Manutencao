package com.augustopalhardi.manutencao.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class SolicitacaoManutencaoPredial implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "local_id")
	private CadastroLocal local;

	private String problema;
	private boolean riscoSeguranca;
	private String descrRisco;
	private boolean urgente;
	private String descrUrgente;
	private String urlFoto;

	@Temporal(TemporalType.DATE)
	private Date dataManutencao;

	private String observacoes;

	@Column(name = "solicitante", length = 100)
	private String solicitante;

	// Novo campo para tipo de manutenção
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_manutencao")
	private TipoManutencao tipoManutencao;

	@Column(name = "usuario_registro", length = 100)
	private String usuarioRegistro;

	// Campos para cancelamento
	private boolean cancelada = false;

	@Column(length = 500)
	private String motivoCancelamento;

	@Temporal(TemporalType.TIMESTAMP)
	private Date dataCancelamento;

	// Construtores
	public SolicitacaoManutencaoPredial() {
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CadastroLocal getLocal() {
		return local;
	}

	public void setLocal(CadastroLocal local) {
		this.local = local;
	}

	public String getProblema() {
		return problema;
	}

	public void setProblema(String problema) {
		this.problema = problema;
	}

	public boolean isRiscoSeguranca() {
		return riscoSeguranca;
	}

	public void setRiscoSeguranca(boolean riscoSeguranca) {
		this.riscoSeguranca = riscoSeguranca;
	}

	public String getDescrRisco() {
		return descrRisco;
	}

	public void setDescrRisco(String descrRisco) {
		this.descrRisco = descrRisco;
	}

	public boolean isUrgente() {
		return urgente;
	}

	public void setUrgente(boolean urgente) {
		this.urgente = urgente;
	}

	public String getDescrUrgente() {
		return descrUrgente;
	}

	public void setDescrUrgente(String descrUrgente) {
		this.descrUrgente = descrUrgente;
	}

	public String getUrlFoto() {
		return urlFoto;
	}

	public void setUrlFoto(String urlFoto) {
		this.urlFoto = urlFoto;
	}

	public Date getDataManutencao() {
		return dataManutencao;
	}

	public void setDataManutencao(Date dataManutencao) {
		this.dataManutencao = dataManutencao;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	// Getter e Setter para o novo campo tipo de manutenção
	public TipoManutencao getTipoManutencao() {
		return tipoManutencao;
	}

	public void setTipoManutencao(TipoManutencao tipoManutencao) {
		this.tipoManutencao = tipoManutencao;
	}

	// Getters e Setters para os campos de cancelamento
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

	public Date getDataCancelamento() {
		return dataCancelamento;
	}

	public void setDataCancelamento(Date dataCancelamento) {
		this.dataCancelamento = dataCancelamento;
	}

	public String getSolicitante() {
		return solicitante;
	}

	public void setSolicitante(String solicitante) {
		this.solicitante = solicitante;
	}

	public String getUsuarioRegistro() {
		return usuarioRegistro;
	}

	public void setUsuarioRegistro(String usuarioRegistro) {
		this.usuarioRegistro = usuarioRegistro;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		SolicitacaoManutencaoPredial other = (SolicitacaoManutencaoPredial) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}