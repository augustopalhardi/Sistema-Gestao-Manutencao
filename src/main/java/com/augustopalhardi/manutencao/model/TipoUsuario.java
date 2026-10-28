package com.augustopalhardi.manutencao.model;

public enum TipoUsuario {
	ADMINISTRADOR("Administrador"), FUNCIONARIO("Funcionário"), TECNICO("Tecnico");

	private String descricao;

	TipoUsuario(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}
}