package com.augustopalhardi.manutencao.model;

public enum TipoManutencao {
	CORRETIVA("Corretiva"), PREVENTIVA("Preventiva"), PREDITIVA("Preditiva");

	private final String descricao;

	TipoManutencao(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

	@Override
	public String toString() {
		return descricao;
	}
}