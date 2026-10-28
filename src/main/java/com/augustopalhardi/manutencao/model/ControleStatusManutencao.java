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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class ControleStatusManutencao implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Status de uma manutenção.
	 *
	 * Cada constante carrega o rótulo, a classe CSS do badge e a ordem de
	 * exibição usados nas listagens, para que as telas não precisem repetir o
	 * mesmo {@code switch}.
	 *
	 * AGUARDANDO_COMPRA compartilha rótulo, classe e ordem com EM_ANDAMENTO:
	 * do ponto de vista de quem acompanha a lista, a manutenção está em
	 * andamento — a espera pela compra é um detalhe interno. Telas que
	 * precisem distinguir os dois devem tratar a constante diretamente, sem
	 * usar estes métodos.
	 *
	 * A persistência é por {@code EnumType.STRING}, ou seja, grava o
	 * {@code name()} da constante — os atributos abaixo não afetam o banco.
	 */
	public enum StatusManutencao {

		PARA_FAZER("Em Aberto", "status-aberto", 1),
		EM_ANDAMENTO("Em Andamento", "status-andamento", 0),
		CONCLUIDA("Concluída", "status-concluida", 2),
		CANCELADA("Cancelada", "status-cancelada", 3),
		AGUARDANDO_COMPRA("Em Andamento", "status-andamento", 0);

		private final String label;
		private final String classeCss;
		private final int ordem;

		StatusManutencao(String label, String classeCss, int ordem) {
			this.label = label;
			this.classeCss = classeCss;
			this.ordem = ordem;
		}

		/**
		 * Texto exibido ao usuário.
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Classe CSS do badge de status nas listagens.
		 */
		public String getClasseCss() {
			return classeCss;
		}

		/**
		 * Peso de ordenação: menor aparece primeiro. Em andamento, depois em
		 * aberto, depois concluída e por último cancelada.
		 */
		public int getOrdem() {
			return ordem;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "solicitacao_id", nullable = false)
	private Long solicitacaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_manutencao", nullable = false)
	private TipoManutencaoReferencia tipoManutencao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private StatusManutencao status;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_inicio")
	private Date dataInicio;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_conclusao")
	private Date dataConclusao;

	@Column(name = "solucao", length = 1000)
	private String solucao;

	@Column(name = "observacoes_finais", length = 500)
	private String observacoesFinais;

	@Column(name = "bbs_near_miss", columnDefinition = "TINYINT(1) DEFAULT 0")
	private Boolean bbsNearMiss = false;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_criacao")
	private Date dataCriacao;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_atualizacao")
	private Date dataAtualizacao;

	public ControleStatusManutencao() {
		this.dataCriacao = new Date();
		this.dataAtualizacao = new Date();
		this.status = StatusManutencao.PARA_FAZER;
	}

	public ControleStatusManutencao(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao) {
		this();
		this.solicitacaoId = solicitacaoId;
		this.tipoManutencao = tipoManutencao;
	}

	// ========== GETTERS E SETTERS ==========

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

	public StatusManutencao getStatus() {
		return status;
	}

	public void setStatus(StatusManutencao status) {
		this.status = status;
		this.dataAtualizacao = new Date();
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
		this.dataAtualizacao = new Date();
	}

	public Date getDataConclusao() {
		return dataConclusao;
	}

	public void setDataConclusao(Date dataConclusao) {
		this.dataConclusao = dataConclusao;
		this.dataAtualizacao = new Date();
	}

	public String getSolucao() {
		return solucao;
	}

	public void setSolucao(String solucao) {
		this.solucao = solucao;
		this.dataAtualizacao = new Date();
	}

	public String getObservacoesFinais() {
		return observacoesFinais;
	}

	public void setObservacoesFinais(String observacoesFinais) {
		this.observacoesFinais = observacoesFinais;
		this.dataAtualizacao = new Date();
	}

	public boolean isBbsNearMiss() {
		return Boolean.TRUE.equals(bbsNearMiss);
	}

	public void setBbsNearMiss(Boolean bbsNearMiss) {
		this.bbsNearMiss = bbsNearMiss;
		this.dataAtualizacao = new Date();
	}

	public Date getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(Date dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	public Date getDataAtualizacao() {
		return dataAtualizacao;
	}

	public void setDataAtualizacao(Date dataAtualizacao) {
		this.dataAtualizacao = dataAtualizacao;
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
		ControleStatusManutencao other = (ControleStatusManutencao) obj;
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
