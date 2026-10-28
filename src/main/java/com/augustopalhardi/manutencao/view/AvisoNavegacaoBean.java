package com.augustopalhardi.manutencao.view;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import com.augustopalhardi.manutencao.util.Mensagens;

/**
 * Explica ao usuário por que ele foi levado para a tela atual.
 *
 * O filtro de autenticação e o logout redirecionam com um parâmetro na URL
 * ({@code ?sessao=expirada}, {@code ?acesso=negado}, {@code ?sessao=encerrada}).
 * As telas de login e as homes chamam {@link #exibir()} num
 * {@code f:viewAction}, que transforma esse parâmetro em mensagem.
 */
@Named
@RequestScoped
public class AvisoNavegacaoBean {

	public void exibir() {
		Map<String, String> parametros = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterMap();

		String sessao = parametros.get("sessao");
		String acesso = parametros.get("acesso");

		if ("expirada".equals(sessao)) {
			Mensagens.aviso("Sessão expirada", "Sua sessão expirou por inatividade. Entre novamente para continuar.");
		} else if ("encerrada".equals(sessao)) {
			Mensagens.info("Até logo", "Você saiu do sistema com segurança.");
		}

		if ("negado".equals(acesso)) {
			Mensagens.aviso("Acesso negado", "Seu perfil não tem permissão para acessar aquela página.");
		}
	}
}
