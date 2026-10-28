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
public class SolicitacaoCompraMateriaisServicos implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum TipoSolicitacao {
		MATERIAL("Material"), SERVICO("Serviço"), AMBOS("Material e Serviço");

		private String descricao;

		TipoSolicitacao(String descricao) {
			this.descricao = descricao;
		}

		public String getDescricao() {
			return descricao;
		}
	}

	public enum StatusSolicitacao {
		PENDENTE("Pendente"), COMPRADO("Comprado"), CANCELADA("Cancelada");

		private String descricao;

		StatusSolicitacao(String descricao) {
			this.descricao = descricao;
		}

		public String getDescricao() {
			return descricao;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Referência à solicitação de manutenção
	@Column(name = "solicitacao_manutencao_id", nullable = false)
	private Long solicitacaoManutencaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_manutencao_referencia", nullable = false)
	private TipoManutencaoReferencia tipoManutencaoReferencia;

	// Técnico que fez a solicitação
	@ManyToOne
	@JoinColumn(name = "tecnico_solicitante_id", nullable = false)
	private CadastroUsuario tecnicoSolicitante;

	// Tipo da solicitação
	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_solicitacao", nullable = false)
	private TipoSolicitacao tipoSolicitacao;

	// Descrição detalhada
	@Column(name = "descricao_detalhada", length = 1000, nullable = false)
	private String descricaoDetalhada;

	// Status da solicitação
	@Enumerated(EnumType.STRING)
	@Column(name = "status_solicitacao", nullable = false)
	private StatusSolicitacao statusSolicitacao = StatusSolicitacao.PENDENTE;

	// Valor gasto total (preenchido pelo administrador)
	@Column(name = "valor_gasto", precision = 10, scale = 2)
	private Double valorGasto;

	// Valor gasto com material (usado quando tipoSolicitacao == AMBOS)
	@Column(name = "valor_gasto_material", precision = 10, scale = 2)
	private Double valorGastoMaterial;

	// Valor gasto com serviço (usado quando tipoSolicitacao == AMBOS)
	@Column(name = "valor_gasto_servico", precision = 10, scale = 2)
	private Double valorGastoServico;

	// Administrador que processou a compra
	@ManyToOne
	@JoinColumn(name = "administrador_processou_id")
	private CadastroUsuario administradorProcessou;

	// Datas
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_solicitacao", nullable = false)
	private Date dataSolicitacao;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_processamento")
	private Date dataProcessamento;

	// Observações do administrador
	@Column(name = "observacoes_administrador", length = 500)
	private String observacoesAdministrador;

	// ========== Cancelamento de compra já processada ==========
	// Motivo do cancelamento (usado quando uma compra já processada é cancelada)
	@Column(name = "motivo_cancelamento", length = 500)
	private String motivoCancelamento;

	// Administrador que cancelou a compra já processada
	@ManyToOne
	@JoinColumn(name = "administrador_cancelamento_id")
	private CadastroUsuario administradorCancelamento;

	// Data/hora em que a compra já processada foi cancelada
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_cancelamento")
	private Date dataCancelamento;

	// Construtores
	public SolicitacaoCompraMateriaisServicos() {
		this.dataSolicitacao = new Date();
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSolicitacaoManutencaoId() {
		return solicitacaoManutencaoId;
	}

	public void setSolicitacaoManutencaoId(Long solicitacaoManutencaoId) {
		this.solicitacaoManutencaoId = solicitacaoManutencaoId;
	}

	public TipoManutencaoReferencia getTipoManutencaoReferencia() {
		return tipoManutencaoReferencia;
	}

	public void setTipoManutencaoReferencia(TipoManutencaoReferencia tipoManutencaoReferencia) {
		this.tipoManutencaoReferencia = tipoManutencaoReferencia;
	}

	public CadastroUsuario getTecnicoSolicitante() {
		return tecnicoSolicitante;
	}

	public void setTecnicoSolicitante(CadastroUsuario tecnicoSolicitante) {
		this.tecnicoSolicitante = tecnicoSolicitante;
	}

	public TipoSolicitacao getTipoSolicitacao() {
		return tipoSolicitacao;
	}

	public void setTipoSolicitacao(TipoSolicitacao tipoSolicitacao) {
		this.tipoSolicitacao = tipoSolicitacao;
	}

	public String getDescricaoDetalhada() {
		return descricaoDetalhada;
	}

	public void setDescricaoDetalhada(String descricaoDetalhada) {
		this.descricaoDetalhada = descricaoDetalhada;
	}

	public StatusSolicitacao getStatusSolicitacao() {
		return statusSolicitacao;
	}

	public void setStatusSolicitacao(StatusSolicitacao statusSolicitacao) {
		this.statusSolicitacao = statusSolicitacao;
	}

	public Double getValorGasto() {
		return valorGasto;
	}

	public void setValorGasto(Double valorGasto) {
		this.valorGasto = valorGasto;
	}

	public Double getValorGastoMaterial() {
		return valorGastoMaterial;
	}

	public void setValorGastoMaterial(Double valorGastoMaterial) {
		this.valorGastoMaterial = valorGastoMaterial;
	}

	public Double getValorGastoServico() {
		return valorGastoServico;
	}

	public void setValorGastoServico(Double valorGastoServico) {
		this.valorGastoServico = valorGastoServico;
	}

	public CadastroUsuario getAdministradorProcessou() {
		return administradorProcessou;
	}

	public void setAdministradorProcessou(CadastroUsuario administradorProcessou) {
		this.administradorProcessou = administradorProcessou;
	}

	public Date getDataSolicitacao() {
		return dataSolicitacao;
	}

	public void setDataSolicitacao(Date dataSolicitacao) {
		this.dataSolicitacao = dataSolicitacao;
	}

	public Date getDataProcessamento() {
		return dataProcessamento;
	}

	public void setDataProcessamento(Date dataProcessamento) {
		this.dataProcessamento = dataProcessamento;
	}

	public String getObservacoesAdministrador() {
		return observacoesAdministrador;
	}

	public void setObservacoesAdministrador(String observacoesAdministrador) {
		this.observacoesAdministrador = observacoesAdministrador;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public CadastroUsuario getAdministradorCancelamento() {
		return administradorCancelamento;
	}

	public void setAdministradorCancelamento(CadastroUsuario administradorCancelamento) {
		this.administradorCancelamento = administradorCancelamento;
	}

	public Date getDataCancelamento() {
		return dataCancelamento;
	}

	public void setDataCancelamento(Date dataCancelamento) {
		this.dataCancelamento = dataCancelamento;
	}

	// Métodos auxiliares
	public String getNomeManutencaoReferencia() {
		// Este método deve ser implementado para retornar o nome da manutenção
		// baseado no tipo e ID da solicitação
		return tipoManutencaoReferencia.getDescricao() + " #" + solicitacaoManutencaoId;
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
		SolicitacaoCompraMateriaisServicos other = (SolicitacaoCompraMateriaisServicos) obj;
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