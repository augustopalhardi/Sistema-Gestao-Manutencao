package com.augustopalhardi.manutencao.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.util.Navegacao;

/**
 * Filtro de segurança que implementa AUTENTICAÇÃO (login) e AUTORIZAÇÃO
 * (permissão). Esta versão usa uma lógica de "Allow-List" baseada nas regras de
 * negócio.
 */

@WebFilter(urlPatterns = { "*.xhtml" })
public class AuthenticationFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		HttpSession session = req.getSession(false);

		String requestURI = req.getRequestURI();
		String contextPath = req.getContextPath();

		// Tenta pegar o usuário da sessão
		CadastroUsuario usuarioLogado = null;
		if (session != null) {
			usuarioLogado = (CadastroUsuario) session.getAttribute("usuarioLogado");
		}
		boolean loggedIn = (usuarioLogado != null);

		// --- 1. Páginas Públicas ---
		boolean isPaginaPublica = requestURI.endsWith("/index.xhtml") || requestURI.endsWith("/login.xhtml")
				|| requestURI.contains("/javax.faces.resource/") || requestURI.contains("/error/");

		if (isPaginaPublica) {
			if (loggedIn && (requestURI.endsWith("/index.xhtml") || requestURI.endsWith("/login.xhtml"))) {
				// Usuário logado tentando acessar login? Manda para a home.
				redirecionar(req, res, getHomePageParaUsuario(usuarioLogado.getTipoUsuario(), contextPath));
			} else {
				// Página pública, deixa passar (CSS, JS, etc.)
				chain.doFilter(request, response);
			}
			return;
		}

		// --- 2. Páginas Privadas ---

		if (loggedIn) {
			// Usuário está logado. Vamos verificar a AUTORIZAÇÃO.
			TipoUsuario tipoUsuario = usuarioLogado.getTipoUsuario();
			boolean temPermissao = false;

			// Verifica se a URI solicitada é permitida para o perfil do usuário
			switch (tipoUsuario) {
			case ADMINISTRADOR:
				temPermissao = isAcessoPermitidoParaAdmin(requestURI);
				break;
			case TECNICO:
				temPermissao = isAcessoPermitidoParaTecnico(requestURI);
				break;
			case FUNCIONARIO:
				temPermissao = isAcessoPermitidoParaFuncionario(requestURI);
				break;
			}

			if (temPermissao) {
				// --- PERMISSÃO CONCEDIDA ---
				// Adiciona headers de segurança para não guardar páginas privadas em cache
				res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
				res.setHeader("Pragma", "no-cache");
				res.setHeader("Expires", "0");

				chain.doFilter(request, response);
			} else {
				// --- ACESSO NEGADO ---
				// O usuário está logado, mas tentou acessar uma página que não pode.
				// Redireciona para a home page DELE, avisando o motivo.
				redirecionar(req, res, getHomePageParaUsuario(tipoUsuario, contextPath) + "?acesso=negado");
			}

		} else {
			// Não é página pública E não está logado. Redireciona para o login,
			// avisando quando o motivo é uma sessão que expirou.
			boolean sessaoExpirou = req.getRequestedSessionId() != null && !req.isRequestedSessionIdValid();
			redirecionar(req, res, contextPath + "/index.xhtml" + (sessaoExpirou ? "?sessao=expirada" : ""));
		}
	}

	/**
	 * Redireciona respeitando o tipo de requisição. Num clique ajax do JSF, um
	 * 302 comum é seguido pelo navegador "por baixo" e a tela simplesmente não
	 * reage; por isso a resposta vira um {@code <redirect>} do protocolo ajax
	 * do JSF, que o PrimeFaces executa.
	 */
	private void redirecionar(HttpServletRequest req, HttpServletResponse res, String url) throws IOException {
		if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
			res.setContentType("text/xml");
			res.setCharacterEncoding("UTF-8");
			res.getWriter().printf(
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\"%s\"></redirect></partial-response>",
					url);
		} else {
			res.sendRedirect(url);
		}
	}

	// --- MÉTODOS DE AUTORIZAÇÃO (ALLOW-LIST) ---

	/**
	 * Verifica se a URI é permitida para o perfil ADMINISTRADOR.
	 */
	private boolean isAcessoPermitidoParaAdmin(String uri) {
		return uri.endsWith("/home/homeAdm.xhtml") || uri.endsWith("/compras/comprarMateriaisServicos.xhtml")
				|| uri.endsWith("/email/configuracaoEmail.xhtml") || uri.endsWith("/usuario/usuario.xhtml")
				|| uri.endsWith("/maquinaEquipamento/maquinaEquipamento.xhtml") || uri.endsWith("/local/local.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoMaquina.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoPredial.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/todasSolicitacoes.xhtml")
				|| uri.endsWith("/relatorios/dashboard.xhtml") || uri.endsWith("/local/localTecnico.xhtml")
				|| uri.endsWith("/maquinaEquipamento/maquinaEquipamentoTecnico.xhtml")
				|| uri.endsWith("/conta/minhaConta.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoCorretiva.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoPreditiva.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoPreventiva.xhtml")
				|| uri.endsWith("/relatorios/relatorioIndicadores.xhtml")
				|| uri.endsWith("/cronograma/cronogramaPreventiva.xhtml")
				|| uri.endsWith("/tecnico/consultaTecnico.xhtml");
	}

	/**
	 * Verifica se a URI é permitida para o perfil TECNICO.
	 */
	private boolean isAcessoPermitidoParaTecnico(String uri) {
		return uri.endsWith("/home/homeTecnico.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoMaquina.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoPredial.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/todasSolicitacoes.xhtml")
				|| uri.endsWith("/relatorios/dashboard.xhtml") 
				|| uri.endsWith("/local/localTecnico.xhtml")
				|| uri.endsWith("/maquinaEquipamento/maquinaEquipamentoTecnico.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoCorretiva.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoPreditiva.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/manutencaoTecnico/manutencaoTecnicoPreventiva.xhtml")
				|| uri.endsWith("/conta/minhaConta.xhtml") 
				|| uri.endsWith("/cronograma/cronogramaPreventiva.xhtml");
	}

	/**
	 * Verifica se a URI é permitida para o perfil FUNCIONARIO.
	 */
	private boolean isAcessoPermitidoParaFuncionario(String uri) {
		return uri.endsWith("/home/homeUser.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoMaquina.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/solicitacaoManutencaoPredial.xhtml")
				|| uri.endsWith("/solicitacaoManutencao/todasSolicitacoes.xhtml");
	}

	/**
	 * Retorna a URL da home page correta para o tipo de usuário.
	 */
	private String getHomePageParaUsuario(TipoUsuario tipo, String contextPath) {
		return contextPath + Navegacao.paginaInicial(tipo);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}