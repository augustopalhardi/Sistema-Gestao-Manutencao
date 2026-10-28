package com.augustopalhardi.manutencao.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entidade para armazenar o histórico de cancelamentos de compras de
 * materiais/serviços que já haviam sido processadas (status COMPRADO).
 *
 * Guarda uma "fotografia" dos dados da compra no momento do cancelamento
 * (valor gasto, quem processou, quando processou), além do motivo e de quem
 * realizou o cancelamento, para fins de auditoria.
 */
@Entity
@Table(name = "historico_cancelamento_compra")
public class HistoricoCancelamentoCompra implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Referência à solicitação de compra que foi cancelada
	@Column(name = "solicitacao_compra_id", nullable = false)
	private Long solicitacaoCompraId;

	// Valor que havia sido gasto na compra antes do cancelamento
	@Column(name = "valor_gasto_anterior", precision = 10, scale = 2)
	private Double valorGastoAnterior;

	// Valor gasto com material antes do cancelamento (quando tipo AMBOS)
	@Column(name = "valor_gasto_material_anterior", precision = 10, scale = 2)
	private Double valorGastoMaterialAnterior;

	// Valor gasto com serviço antes do cancelamento (quando tipo AMBOS)
	@Column(name = "valor_gasto_servico_anterior", precision = 10, scale = 2)
	private Double valorGastoServicoAnterior;

	// Nome do administrador que havia processado a compra originalmente
	@Column(name = "administrador_processou_anterior", length = 100)
	private String administradorProcessouAnterior;

	// Data em que a compra havia sido processada originalmente
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_processamento_anterior")
	private Date dataProcessamentoAnterior;

	// Motivo do cancelamento
	@Column(name = "motivo_cancelamento", length = 500, nullable = false)
	private String motivoCancelamento;

	// Nome do administrador que realizou o cancelamento
	@Column(name = "administrador_cancelamento", length = 100, nullable = false)
	private String administradorCancelamento;

	// Data/hora em que o cancelamento foi realizado
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_cancelamento", nullable = false)
	private Date dataCancelamento;

	// Construtor padrão
	public HistoricoCancelamentoCompra() {
		this.dataCancelamento = new Date();
	}

	// Construtor com parâmetros
	public HistoricoCancelamentoCompra(Long solicitacaoCompraId, Double valorGastoAnterior,
			Double valorGastoMaterialAnterior, Double valorGastoServicoAnterior,
			String administradorProcessouAnterior, Date dataProcessamentoAnterior, String motivoCancelamento,
			String administradorCancelamento) {
		this.solicitacaoCompraId = solicitacaoCompraId;
		this.valorGastoAnterior = valorGastoAnterior;
		this.valorGastoMaterialAnterior = valorGastoMaterialAnterior;
		this.valorGastoServicoAnterior = valorGastoServicoAnterior;
		this.administradorProcessouAnterior = administradorProcessouAnterior;
		this.dataProcessamentoAnterior = dataProcessamentoAnterior;
		this.motivoCancelamento = motivoCancelamento;
		this.administradorCancelamento = administradorCancelamento;
		this.dataCancelamento = new Date();
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getSolicitacaoCompraId() {
		return solicitacaoCompraId;
	}

	public void setSolicitacaoCompraId(Long solicitacaoCompraId) {
		this.solicitacaoCompraId = solicitacaoCompraId;
	}

	public Double getValorGastoAnterior() {
		return valorGastoAnterior;
	}

	public void setValorGastoAnterior(Double valorGastoAnterior) {
		this.valorGastoAnterior = valorGastoAnterior;
	}

	public Double getValorGastoMaterialAnterior() {
		return valorGastoMaterialAnterior;
	}

	public void setValorGastoMaterialAnterior(Double valorGastoMaterialAnterior) {
		this.valorGastoMaterialAnterior = valorGastoMaterialAnterior;
	}

	public Double getValorGastoServicoAnterior() {
		return valorGastoServicoAnterior;
	}

	public void setValorGastoServicoAnterior(Double valorGastoServicoAnterior) {
		this.valorGastoServicoAnterior = valorGastoServicoAnterior;
	}

	public String getAdministradorProcessouAnterior() {
		return administradorProcessouAnterior;
	}

	public void setAdministradorProcessouAnterior(String administradorProcessouAnterior) {
		this.administradorProcessouAnterior = administradorProcessouAnterior;
	}

	public Date getDataProcessamentoAnterior() {
		return dataProcessamentoAnterior;
	}

	public void setDataProcessamentoAnterior(Date dataProcessamentoAnterior) {
		this.dataProcessamentoAnterior = dataProcessamentoAnterior;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public String getAdministradorCancelamento() {
		return administradorCancelamento;
	}

	public void setAdministradorCancelamento(String administradorCancelamento) {
		this.administradorCancelamento = administradorCancelamento;
	}

	public Date getDataCancelamento() {
		return dataCancelamento;
	}

	public void setDataCancelamento(Date dataCancelamento) {
		this.dataCancelamento = dataCancelamento;
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
		HistoricoCancelamentoCompra other = (HistoricoCancelamentoCompra) obj;
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
		return "HistoricoCancelamentoCompra [id=" + id + ", solicitacaoCompraId=" + solicitacaoCompraId
				+ ", valorGastoAnterior=" + valorGastoAnterior + ", administradorProcessouAnterior="
				+ administradorProcessouAnterior + ", dataProcessamentoAnterior=" + dataProcessamentoAnterior
				+ ", motivoCancelamento=" + motivoCancelamento + ", administradorCancelamento="
				+ administradorCancelamento + ", dataCancelamento=" + dataCancelamento + "]";
	}
}