package com.augustopalhardi.manutencao.view;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.util.Navegacao;

/**
 * Expõe para as telas a página inicial do usuário logado. Usado nos botões
 * "Voltar": {@code <h:link outcome="#{navegacaoBean.paginaInicial}" />}.
 */
@Named
@RequestScoped
public class NavegacaoBean {

	public String getPaginaInicial() {
		Object usuario = FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
				.get("usuarioLogado");
		return usuario instanceof CadastroUsuario
				? Navegacao.paginaInicial(((CadastroUsuario) usuario).getTipoUsuario())
				: Navegacao.LOGIN;
	}
}
