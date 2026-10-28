package com.augustopalhardi.manutencao.util;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 * Atalhos para adicionar mensagens ao contexto JSF.
 *
 * Substitui a repetição de
 * {@code FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(...))}
 * espalhada pelos beans. O comportamento é idêntico: mensagem global (clientId
 * nulo), exibida pelo {@code p:growl}/{@code p:messages} da página.
 */
public final class Mensagens {

	private static final String RESUMO_SUCESSO = "Sucesso";
	private static final String RESUMO_ATENCAO = "Atenção";
	private static final String RESUMO_ERRO = "Erro";

	private Mensagens() {
	}

	// ========== INFO ==========

	/**
	 * Mensagem de sucesso com o resumo padrão "Sucesso".
	 */
	public static void info(String detalhe) {
		adicionar(FacesMessage.SEVERITY_INFO, RESUMO_SUCESSO, detalhe);
	}

	/**
	 * Mensagem de sucesso com resumo próprio.
	 */
	public static void info(String resumo, String detalhe) {
		adicionar(FacesMessage.SEVERITY_INFO, resumo, detalhe);
	}

	// ========== AVISO ==========

	/**
	 * Mensagem de alerta com o resumo padrão "Atenção".
	 */
	public static void aviso(String detalhe) {
		adicionar(FacesMessage.SEVERITY_WARN, RESUMO_ATENCAO, detalhe);
	}

	/**
	 * Mensagem de alerta com resumo próprio.
	 */
	public static void aviso(String resumo, String detalhe) {
		adicionar(FacesMessage.SEVERITY_WARN, resumo, detalhe);
	}

	// ========== ERRO ==========

	/**
	 * Mensagem de erro com o resumo padrão "Erro".
	 */
	public static void erro(String detalhe) {
		adicionar(FacesMessage.SEVERITY_ERROR, RESUMO_ERRO, detalhe);
	}

	/**
	 * Mensagem de erro com resumo próprio.
	 */
	public static void erro(String resumo, String detalhe) {
		adicionar(FacesMessage.SEVERITY_ERROR, resumo, detalhe);
	}

	// ========== MÉTODO AUXILIAR ==========

	private static void adicionar(FacesMessage.Severity severidade, String resumo, String detalhe) {
		FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, resumo, detalhe));
	}
}
