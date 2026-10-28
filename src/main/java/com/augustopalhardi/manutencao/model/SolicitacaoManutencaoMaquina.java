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
public class SolicitacaoManutencaoMaquina implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "maquina_id")
	private CadastroMaquinaEquipamento maquina;

	@Column(name = "solicitante", length = 100)
	private String solicitante;

	private String falha;
	private boolean risco;
	private String descrRisco;
	private boolean operando;
	private String descrOperando;
	private String urlFoto;

	@Temporal(TemporalType.DATE)
	private Date dataManutencao;

	private String observacoes;

	// Horímetro da máquina no momento da falha
	@Column(name = "horimetro_falha", precision = 10, scale = 2)
	private Double horimetroFalha;

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

	@Column(name = "manutencao_eletrica")
	private Boolean manutencaoEletrica;

	// Construtores
	public SolicitacaoManutencaoMaquina() {
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CadastroMaquinaEquipamento getMaquina() {
		return maquina;
	}

	public void setMaquina(CadastroMaquinaEquipamento maquina) {
		this.maquina = maquina;
	}

	public String getFalha() {
		return falha;
	}

	public void setFalha(String falha) {
		this.falha = falha;
	}

	public boolean isRisco() {
		return risco;
	}

	public void setRisco(boolean risco) {
		this.risco = risco;
	}

	public String getDescrRisco() {
		return descrRisco;
	}

	public void setDescrRisco(String descrRisco) {
		this.descrRisco = descrRisco;
	}

	public boolean isOperando() {
		return operando;
	}

	public void setOperando(boolean operando) {
		this.operando = operando;
	}

	public String getDescrOperando() {
		return descrOperando;
	}

	public void setDescrOperando(String descrOperando) {
		this.descrOperando = descrOperando;
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

	// Getter e Setter para horímetro
	public Double getHorimetroFalha() {
		return horimetroFalha;
	}

	public void setHorimetroFalha(Double horimetroFalha) {
		this.horimetroFalha = horimetroFalha;
	}

	// Getter e Setter para o tipo de manutenção
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

	public Boolean getManutencaoEletrica() {
		return manutencaoEletrica;
	}

	public void setManutencaoEletrica(Boolean manutencaoEletrica) {
		this.manutencaoEletrica = manutencaoEletrica;
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
		SolicitacaoManutencaoMaquina other = (SolicitacaoManutencaoMaquina) obj;
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