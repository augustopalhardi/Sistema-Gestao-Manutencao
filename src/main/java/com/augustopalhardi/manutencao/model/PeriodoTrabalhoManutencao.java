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
import javax.persistence.Transient;

@Entity
public class PeriodoTrabalhoManutencao implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "solicitacao_id", nullable = false)
	private Long solicitacaoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_manutencao", nullable = false)
	private TipoManutencaoReferencia tipoManutencao;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_hora_inicio", nullable = false)
	private Date dataHoraInicio;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_hora_fim", nullable = false)
	private Date dataHoraFim;

	@Column(name = "descricao_atividade", length = 500)
	private String descricaoAtividade;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_criacao")
	private Date dataCriacao;

	// Técnico responsável pelo período
	@ManyToOne
	@JoinColumn(name = "tecnico_responsavel_id")
	private CadastroUsuario tecnicoResponsavel;

	// Indica se o período foi utilizado para diagnóstico
	@Column(name = "utilizado_para_diagnostico")
	private Boolean utilizadoParaDiagnostico = false;

	@Transient // Importante: para não criar a coluna no banco
	private double duracaoEmHoras;

	public PeriodoTrabalhoManutencao() {
		this.dataCriacao = new Date();
	}

	public PeriodoTrabalhoManutencao(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao, Date dataHoraInicio,
			Date dataHoraFim) {
		this();
		this.solicitacaoId = solicitacaoId;
		this.tipoManutencao = tipoManutencao;
		this.dataHoraInicio = dataHoraInicio;
		this.dataHoraFim = dataHoraFim;
	}

	// Construtor com técnico responsável
	public PeriodoTrabalhoManutencao(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao, Date dataHoraInicio,
			Date dataHoraFim, CadastroUsuario tecnicoResponsavel) {
		this(solicitacaoId, tipoManutencao, dataHoraInicio, dataHoraFim);
		this.tecnicoResponsavel = tecnicoResponsavel;
	}

	// Getters e Setters existentes
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

	public Date getDataHoraInicio() {
		return dataHoraInicio;
	}

	public void setDataHoraInicio(Date dataHoraInicio) {
		this.dataHoraInicio = dataHoraInicio;
	}

	public Date getDataHoraFim() {
		return dataHoraFim;
	}

	public void setDataHoraFim(Date dataHoraFim) {
		this.dataHoraFim = dataHoraFim;
	}

	public String getDescricaoAtividade() {
		return descricaoAtividade;
	}

	public void setDescricaoAtividade(String descricaoAtividade) {
		this.descricaoAtividade = descricaoAtividade;
	}

	public Date getDataCriacao() {
		return dataCriacao;
	}

	public void setDataCriacao(Date dataCriacao) {
		this.dataCriacao = dataCriacao;
	}

	// Getters e Setters para técnico responsável
	public CadastroUsuario getTecnicoResponsavel() {
		return tecnicoResponsavel;
	}

	public void setTecnicoResponsavel(CadastroUsuario tecnicoResponsavel) {
		this.tecnicoResponsavel = tecnicoResponsavel;
	}

	// Getters e Setters para utilizado para diagnóstico
	public Boolean getUtilizadoParaDiagnostico() {
		return utilizadoParaDiagnostico;
	}

	public void setUtilizadoParaDiagnostico(Boolean utilizadoParaDiagnostico) {
		this.utilizadoParaDiagnostico = utilizadoParaDiagnostico;
	}

	public boolean isUtilizadoParaDiagnostico() {
		return Boolean.TRUE.equals(utilizadoParaDiagnostico);
	}

	// Método para calcular duração em horas
	public double getDuracaoEmHoras() {
		if (dataHoraInicio == null || dataHoraFim == null || dataHoraFim.before(dataHoraInicio)) {
			return 0.0;
		}
		long diferencaMs = dataHoraFim.getTime() - dataHoraInicio.getTime();
		return diferencaMs / (1000.0 * 60.0 * 60.0); // Converte para horas
	}

	// Setter para o campo transient
	public void setDuracaoEmHoras(double duracaoEmHoras) {
		this.duracaoEmHoras = duracaoEmHoras;
	}

	// Método utilitário para obter nome do técnico
	public String getNomeTecnicoResponsavel() {
		return tecnicoResponsavel != null ? tecnicoResponsavel.getNome() : "Não informado";
	}

	// Método utilitário para obter registro do técnico
	public String getRegistroTecnicoResponsavel() {
		return tecnicoResponsavel != null ? tecnicoResponsavel.getNuRegistro() : "N/A";
	}

	// Método para validar se o período é válido
	public boolean isValido() {
		return solicitacaoId != null && tipoManutencao != null && dataHoraInicio != null && dataHoraFim != null
				&& !dataHoraFim.before(dataHoraInicio);
	}

	// Método para formatar duração como string
	public String getDuracaoFormatada() {
		double horas = getDuracaoEmHoras();
		if (horas == 0.0) {
			return "0h";
		}

		int horasInteiras = (int) horas;
		int minutos = (int) ((horas - horasInteiras) * 60);

		if (minutos == 0) {
			return horasInteiras + "h";
		} else {
			return horasInteiras + "h" + minutos + "m";
		}
	}

	// Método para verificar se há sobreposição com outro período
	public boolean temSobreposicaoCom(PeriodoTrabalhoManutencao outro) {
		if (outro == null || outro.dataHoraInicio == null || outro.dataHoraFim == null) {
			return false;
		}

		return !(this.dataHoraFim.compareTo(outro.dataHoraInicio) <= 0
				|| this.dataHoraInicio.compareTo(outro.dataHoraFim) >= 0);
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
		PeriodoTrabalhoManutencao other = (PeriodoTrabalhoManutencao) obj;
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
		return "PeriodoTrabalhoManutencao{" + "id=" + id + ", solicitacaoId=" + solicitacaoId + ", tipoManutencao="
				+ tipoManutencao + ", dataHoraInicio=" + dataHoraInicio + ", dataHoraFim=" + dataHoraFim
				+ ", tecnicoResponsavel=" + (tecnicoResponsavel != null ? tecnicoResponsavel.getNome() : "null")
				+ ", utilizadoParaDiagnostico=" + utilizadoParaDiagnostico + ", duracaoEmHoras=" + getDuracaoEmHoras()
				+ '}';
	}
}