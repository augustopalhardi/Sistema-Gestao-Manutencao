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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entidade para armazenar o histórico de reprogramações de manutenções
 */
@Entity
@Table(name = "historico_reprogramacao")
public class HistoricoReprogramacao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Referência à solicitação
	@Column(name = "solicitacao_id", nullable = false)
	private Long solicitacaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_manutencao", nullable = false)
	private TipoManutencaoReferencia tipoManutencao;

	// Data anterior (antes da reprogramação)
	@Temporal(TemporalType.DATE)
	@Column(name = "data_anterior", nullable = false)
	private Date dataAnterior;

	// Nova data (após reprogramação)
	@Temporal(TemporalType.DATE)
	@Column(name = "data_nova", nullable = false)
	private Date dataNova;

	// Motivo da reprogramação
	@Column(name = "motivo_reprogramacao", length = 500, nullable = false)
	private String motivoReprogramacao;

	// Usuário que fez a reprogramação
	@Column(name = "usuario_reprogramacao", length = 100, nullable = false)
	private String usuarioReprogramacao;

	// Data/hora da reprogramação
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_hora_reprogramacao", nullable = false)
	private Date dataHoraReprogramacao;

	// Construtor padrão
	public HistoricoReprogramacao() {
		this.dataHoraReprogramacao = new Date();
	}

	// Construtor com parâmetros
	public HistoricoReprogramacao(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao, Date dataAnterior,
			Date dataNova, String motivoReprogramacao, String usuarioReprogramacao) {
		this.solicitacaoId = solicitacaoId;
		this.tipoManutencao = tipoManutencao;
		this.dataAnterior = dataAnterior;
		this.dataNova = dataNova;
		this.motivoReprogramacao = motivoReprogramacao;
		this.usuarioReprogramacao = usuarioReprogramacao;
		this.dataHoraReprogramacao = new Date();
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSolicitacaoId() {
		return solicitacaoId;
	}

	public void setSolicitacaoId(Long solicitacaoId) {
		this.solicitacaoId = solicitacaoId;
	}

	public TipoManutencaoReferencia getTipoManutencao() {
		return tipoManutencao;
	}

	public void setTipoManutencao(TipoManutencaoReferencia tipoManutencao) {
		this.tipoManutencao = tipoManutencao;
	}

	public Date getDataAnterior() {
		return dataAnterior;
	}

	public void setDataAnterior(Date dataAnterior) {
		this.dataAnterior = dataAnterior;
	}

	public Date getDataNova() {
		return dataNova;
	}

	public void setDataNova(Date dataNova) {
		this.dataNova = dataNova;
	}

	public String getMotivoReprogramacao() {
		return motivoReprogramacao;
	}

	public void setMotivoReprogramacao(String motivoReprogramacao) {
		this.motivoReprogramacao = motivoReprogramacao;
	}

	public String getUsuarioReprogramacao() {
		return usuarioReprogramacao;
	}

	public void setUsuarioReprogramacao(String usuarioReprogramacao) {
		this.usuarioReprogramacao = usuarioReprogramacao;
	}

	public Date getDataHoraReprogramacao() {
		return dataHoraReprogramacao;
	}

	public void setDataHoraReprogramacao(Date dataHoraReprogramacao) {
		this.dataHoraReprogramacao = dataHoraReprogramacao;
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
		HistoricoReprogramacao other = (HistoricoReprogramacao) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "HistoricoReprogramacao [id=" + id + ", solicitacaoId=" + solicitacaoId + ", tipoManutencao="
				+ tipoManutencao + ", dataAnterior=" + dataAnterior + ", dataNova=" + dataNova
				+ ", motivoReprogramacao=" + motivoReprogramacao + ", usuarioReprogramacao=" + usuarioReprogramacao
				+ ", dataHoraReprogramacao=" + dataHoraReprogramacao + "]";
	}
}