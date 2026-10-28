package com.augustopalhardi.manutencao.util;

import com.augustopalhardi.manutencao.model.TipoUsuario;

/**
 * Página inicial de cada perfil, num lugar só.
 *
 * Usada pelo login, pelo filtro de autenticação e pelos botões "Voltar" das
 * telas compartilhadas entre perfis (via NavegacaoBean), para que ninguém seja
 * mandado para a home de outro perfil.
 */
public final class Navegacao {

	public static final String LOGIN = "/index.xhtml";

	private Navegacao() {
	}

	/** Caminho (sem o context path) da home do perfil; o login se o perfil for nulo. */
	public static String paginaInicial(TipoUsuario tipo) {
		if (tipo == null) {
			return LOGIN;
		}
		switch (tipo) {
		case ADMINISTRADOR:
			return "/home/homeAdm.xhtml";
		case TECNICO:
			return "/home/homeTecnico.xhtml";
		case FUNCIONARIO:
			return "/home/homeUser.xhtml";
		default:
			return LOGIN;
		}
	}
}
