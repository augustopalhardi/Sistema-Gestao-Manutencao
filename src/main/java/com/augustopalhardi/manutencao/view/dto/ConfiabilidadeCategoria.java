package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;

public class ConfiabilidadeCategoria implements Serializable {
	private static final long serialVersionUID = 1L;

	private String categoria;
	private long quantidadeManutencoes;
	private double confiabilidade;

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public long getQuantidadeManutencoes() {
		return quantidadeManutencoes;
	}

	public void setQuantidadeManutencoes(long quantidadeManutencoes) {
		this.quantidadeManutencoes = quantidadeManutencoes;
	}

	public double getConfiabilidade() {
		return confiabilidade;
	}

	public void setConfiabilidade(double confiabilidade) {
		this.confiabilidade = confiabilidade;
	}

	public String getConfiabilidadeFormatada() {
		return String.format("%.2f%%", confiabilidade);
	}

	public String getConfiabilidadeClass() {
		if (confiabilidade >= 80)
			return "confiabilidade-alta";
		if (confiabilidade >= 50)
			return "confiabilidade-media";
		return "confiabilidade-baixa";
	}
}
