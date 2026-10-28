package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import com.augustopalhardi.manutencao.model.CadastroUsuario;

public class PeriodoTrabalho implements Serializable {
	private static final long serialVersionUID = 1L;

	private Date dataHoraInicio;
	private Date dataHoraFim;
	private String descricaoAtividade;
	private Long tecnicoResponsavelId;
	private transient CadastroUsuario tecnicoResponsavel;
	private Boolean utilizadoParaDiagnostico = false;

	private String diaInicio;
	private String mesInicio;
	private String anoInicio;
	private String horaInicio;
	private String minutoInicio;

	private String diaFim;
	private String mesFim;
	private String anoFim;
	private String horaFim;
	private String minutoFim;

	private String mensagemValidacao;

	public PeriodoTrabalho() {
		Calendar cal = Calendar.getInstance();
		this.diaInicio = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
		this.mesInicio = String.format("%02d", cal.get(Calendar.MONTH) + 1);
		this.anoInicio = String.valueOf(cal.get(Calendar.YEAR));
		this.horaInicio = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
		this.minutoInicio = String.format("%02d", cal.get(Calendar.MINUTE));

		cal.add(Calendar.HOUR_OF_DAY, 1);
		this.diaFim = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
		this.mesFim = String.format("%02d", cal.get(Calendar.MONTH) + 1);
		this.anoFim = String.valueOf(cal.get(Calendar.YEAR));
		this.horaFim = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
		this.minutoFim = String.format("%02d", cal.get(Calendar.MINUTE));
	}

	public PeriodoTrabalho(Date dataHoraInicio, Date dataHoraFim, String descricaoAtividade,
			CadastroUsuario tecnicoResponsavel) {
		this();
		this.dataHoraInicio = dataHoraInicio;
		this.dataHoraFim = dataHoraFim;
		this.descricaoAtividade = descricaoAtividade;
		this.tecnicoResponsavel = tecnicoResponsavel;
		this.tecnicoResponsavelId = tecnicoResponsavel != null ? tecnicoResponsavel.getId() : null;
		atualizarCamposInicio();
		atualizarCamposFim();
	}

	public void validarDataInicio() {
		this.mensagemValidacao = null;
		try {
			if (isDataInicioPreenchida()) {
				Date dataInicio = construirDataInicio();
				Date hoje = new Date();
				if (isDataPosteriorAoHoje(dataInicio, hoje)) {
					this.mensagemValidacao = "Data de início não pode ser posterior ao dia atual";
					this.dataHoraInicio = null;
				} else {
					this.dataHoraInicio = dataInicio;
					if (isDataFimPreenchida()) {
						validarDataFim();
					}
				}
			}
		} catch (Exception e) {
			this.mensagemValidacao = "Data de inicio invalida. Verifique o formato.";
			this.dataHoraInicio = null;
		}
	}

	public void validarDataFim() {
		if (this.mensagemValidacao != null && this.mensagemValidacao.contains("inicio")) {
			return;
		}
		try {
			if (isDataFimPreenchida()) {
				Date dataFim = construirDataFim();
				Date hoje = new Date();
				if (isDataPosteriorAoHoje(dataFim, hoje)) {
					this.mensagemValidacao = "Data de fim não pode ser posterior ao dia atual";
					this.dataHoraFim = null;
				} else if (this.dataHoraInicio != null && dataFim.before(this.dataHoraInicio)) {
					this.mensagemValidacao = "Data de fim deve ser posterior a data de inicio";
					this.dataHoraFim = null;
				} else if (this.dataHoraInicio != null && dataFim.equals(this.dataHoraInicio)) {
					this.mensagemValidacao = "Data e hora de fim devem ser posteriores ao início (mínimo 1 minuto)";
					this.dataHoraFim = null;
				} else {
					this.dataHoraFim = dataFim;
					this.mensagemValidacao = null;
				}
			}
		} catch (Exception e) {
			this.mensagemValidacao = "Data de fim invalida. Verifique o formato.";
			this.dataHoraFim = null;
		}
	}

	private boolean isDataPosteriorAoHoje(Date dataParaVerificar, Date hoje) {
		Calendar calDataVerificar = Calendar.getInstance();
		calDataVerificar.setTime(dataParaVerificar);
		Calendar calHoje = Calendar.getInstance();
		calHoje.setTime(hoje);

		int anoData = calDataVerificar.get(Calendar.YEAR);
		int mesData = calDataVerificar.get(Calendar.MONTH);
		int diaData = calDataVerificar.get(Calendar.DAY_OF_MONTH);

		int anoHoje = calHoje.get(Calendar.YEAR);
		int mesHoje = calHoje.get(Calendar.MONTH);
		int diaHoje = calHoje.get(Calendar.DAY_OF_MONTH);

		if ((anoData > anoHoje) || (anoData == anoHoje && mesData > mesHoje)) {
			return true;
		}
		if (anoData == anoHoje && mesData == mesHoje && diaData > diaHoje) {
			return true;
		}
		return false;
	}

	private boolean isDataInicioPreenchida() {
		return !isStringVaziaOuNula(diaInicio) && !isStringVaziaOuNula(mesInicio) && !isStringVaziaOuNula(anoInicio)
				&& !isStringVaziaOuNula(horaInicio) && !isStringVaziaOuNula(minutoInicio);
	}

	private boolean isDataFimPreenchida() {
		return !isStringVaziaOuNula(diaFim) && !isStringVaziaOuNula(mesFim) && !isStringVaziaOuNula(anoFim)
				&& !isStringVaziaOuNula(horaFim) && !isStringVaziaOuNula(minutoFim);
	}

	private Date construirDataInicio() throws Exception {
		try {
			int dia = Integer.parseInt(diaInicio.trim());
			int mes = Integer.parseInt(mesInicio.trim());
			int ano = Integer.parseInt(anoInicio.trim());
			int hora = Integer.parseInt(horaInicio.trim());
			int minuto = Integer.parseInt(minutoInicio.trim());

			if (dia < 1 || dia > 31) {
				throw new Exception("Dia inválido");
			}
			if (mes < 1 || mes > 12) {
				throw new Exception("Mês inválido");
			}
			if (ano < 2020 || ano > 2030) {
				throw new Exception("Ano inválido");
			}
			if (hora < 0 || hora > 23) {
				throw new Exception("Hora invalida");
			}
			if (minuto < 0 || minuto > 59) {
				throw new Exception("Minuto inválido");
			}

			Calendar cal = Calendar.getInstance();
			cal.set(ano, mes - 1, dia, hora, minuto, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} catch (NumberFormatException e) {
			throw new Exception("Formato numérico inválido");
		}
	}

	private Date construirDataFim() throws Exception {
		try {
			int dia = Integer.parseInt(diaFim.trim());
			int mes = Integer.parseInt(mesFim.trim());
			int ano = Integer.parseInt(anoFim.trim());
			int hora = Integer.parseInt(horaFim.trim());
			int minuto = Integer.parseInt(minutoFim.trim());

			if (dia < 1 || dia > 31) {
				throw new Exception("Dia inválido");
			}
			if (mes < 1 || mes > 12) {
				throw new Exception("Mês inválido");
			}
			if (ano < 2020 || ano > 2030) {
				throw new Exception("Ano inválido");
			}
			if (hora < 0 || hora > 23) {
				throw new Exception("Hora invalida");
			}
			if (minuto < 0 || minuto > 59) {
				throw new Exception("Minuto inválido");
			}

			Calendar cal = Calendar.getInstance();
			cal.set(ano, mes - 1, dia, hora, minuto, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} catch (NumberFormatException e) {
			throw new Exception("Formato numérico inválido");
		}
	}

	private boolean isStringVaziaOuNula(String str) {
		return str == null || str.trim().isEmpty();
	}

	public boolean isDuracaoValida() {
		return dataHoraInicio != null && dataHoraFim != null && dataHoraFim.after(dataHoraInicio)
				&& !dataHoraFim.equals(dataHoraInicio) && mensagemValidacao == null;
	}

	public double getDuracaoEmHoras() {
		if (!isDuracaoValida()) {
			return 0.0;
		}
		long diferencaMs = dataHoraFim.getTime() - dataHoraInicio.getTime();
		return diferencaMs / (1000.0 * 60.0 * 60.0);
	}

	public String getDuracaoFormatada() {
		if (!isDuracaoValida()) {
			return "";
		}
		long diferencaMs = dataHoraFim.getTime() - dataHoraInicio.getTime();
		long totalMinutos = diferencaMs / (1000 * 60);
		long horas = totalMinutos / 60;
		long minutos = totalMinutos % 60;

		if (horas > 0 && minutos > 0) {
			return String.format("%dh %dmin", horas, minutos);
		} else if (horas > 0) {
			return String.format("%dh", horas);
		} else {
			return String.format("%dmin", minutos);
		}
	}

	public void atualizarCamposInicio() {
		if (dataHoraInicio != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dataHoraInicio);
			this.diaInicio = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
			this.mesInicio = String.format("%02d", cal.get(Calendar.MONTH) + 1);
			this.anoInicio = String.valueOf(cal.get(Calendar.YEAR));
			this.horaInicio = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
			this.minutoInicio = String.format("%02d", cal.get(Calendar.MINUTE));
		}
	}

	public void atualizarCamposFim() {
		if (dataHoraFim != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(dataHoraFim);
			this.diaFim = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));
			this.mesFim = String.format("%02d", cal.get(Calendar.MONTH) + 1);
			this.anoFim = String.valueOf(cal.get(Calendar.YEAR));
			this.horaFim = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY));
			this.minutoFim = String.format("%02d", cal.get(Calendar.MINUTE));
		}
	}

	// Getters e Setters (propriedades sem acento/cedilha)
	public Date getDataHoraInicio() {
		return dataHoraInicio;
	}

	public void setDataHoraInicio(Date dataHoraInicio) {
		this.dataHoraInicio = dataHoraInicio;
		atualizarCamposInicio();
	}

	public Date getDataHoraFim() {
		return dataHoraFim;
	}

	public void setDataHoraFim(Date dataHoraFim) {
		this.dataHoraFim = dataHoraFim;
		atualizarCamposFim();
	}

	public String getDescricaoAtividade() {
		return descricaoAtividade;
	}

	public void setDescricaoAtividade(String descricaoAtividade) {
		this.descricaoAtividade = descricaoAtividade;
	}

	public CadastroUsuario getTecnicoResponsavel() {
		return tecnicoResponsavel;
	}

	public void setTecnicoResponsavel(CadastroUsuario tecnicoResponsavel) {
		this.tecnicoResponsavel = tecnicoResponsavel;
		this.tecnicoResponsavelId = tecnicoResponsavel != null ? tecnicoResponsavel.getId() : null;
	}

	public Long getTecnicoResponsavelId() {
		return tecnicoResponsavelId;
	}

	public void setTecnicoResponsavelId(Long tecnicoResponsavelId) {
		this.tecnicoResponsavelId = tecnicoResponsavelId;
	}

	public String getDiaInicio() {
		return diaInicio;
	}

	public void setDiaInicio(String diaInicio) {
		this.diaInicio = diaInicio;
	}

	public String getMesInicio() {
		return mesInicio;
	}

	public void setMesInicio(String mesInicio) {
		this.mesInicio = mesInicio;
	}

	public String getAnoInicio() {
		return anoInicio;
	}

	public void setAnoInicio(String anoInicio) {
		this.anoInicio = anoInicio;
	}

	public String getHoraInicio() {
		return horaInicio;
	}

	public void setHoraInicio(String horaInicio) {
		this.horaInicio = horaInicio;
	}

	public String getMinutoInicio() {
		return minutoInicio;
	}

	public void setMinutoInicio(String minutoInicio) {
		this.minutoInicio = minutoInicio;
	}

	public String getDiaFim() {
		return diaFim;
	}

	public void setDiaFim(String diaFim) {
		this.diaFim = diaFim;
	}

	public String getMesFim() {
		return mesFim;
	}

	public void setMesFim(String mesFim) {
		this.mesFim = mesFim;
	}

	public String getAnoFim() {
		return anoFim;
	}

	public void setAnoFim(String anoFim) {
		this.anoFim = anoFim;
	}

	public String getHoraFim() {
		return horaFim;
	}

	public void setHoraFim(String horaFim) {
		this.horaFim = horaFim;
	}

	public String getMinutoFim() {
		return minutoFim;
	}

	public void setMinutoFim(String minutoFim) {
		this.minutoFim = minutoFim;
	}

	public String getMensagemValidacao() {
		return mensagemValidacao;
	}

	public void setMensagemValidacao(String mensagemValidacao) {
		this.mensagemValidacao = mensagemValidacao;
	}

	public Boolean getUtilizadoParaDiagnostico() {
		return utilizadoParaDiagnostico;
	}

	public void setUtilizadoParaDiagnostico(Boolean utilizadoParaDiagnostico) {
		this.utilizadoParaDiagnostico = utilizadoParaDiagnostico;
	}
}
