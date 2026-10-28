package com.augustopalhardi.manutencao.view;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.dao.CadastroUsuarioDao;
import com.augustopalhardi.manutencao.service.LoginService;

@Named("loginUsuarioBean")
@SessionScoped
public class LoginUsuarioBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(LoginUsuarioBean.class);

	@Inject
	private LoginService loginService;

	@Inject
	private CadastroUsuarioDao cadastroUsuarioDao;

	private String nuRegistroInput = "";
	private String senhaInput = "";

	private CadastroUsuario usuarioLogado;
	private boolean logado = false;

	// Flag para controlar se o bean foi inicializado corretamente
	private boolean inicializado = false;

	@PostConstruct
	public void inicializar() {
		try {
			LOGGER.info("Iniciando LoginUsuarioBean...");
			this.nuRegistroInput = "";
			this.senhaInput = "";
			this.usuarioLogado = null;
			this.logado = false;
			this.inicializado = true;
			LOGGER.info("LoginUsuarioBean inicializado com sucesso!");
		} catch (Exception e) {
			LOGGER.error("Erro ao inicializar LoginUsuarioBean: ", e);
			this.inicializado = false;
		}
	}

	public void autenticar() {
		LOGGER.info("=== MÉTODO AUTENTICAR CHAMADO ===");
		LOGGER.info("nuRegistroInput: [{}]", nuRegistroInput);
		LOGGER.info("senhaInput está vazia? {}", (senhaInput == null || senhaInput.isEmpty()));

		try {
			// Validação de campos vazios
			if (nuRegistroInput == null || nuRegistroInput.trim().isEmpty()) {
				LOGGER.warn("Número de registro vazio");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Número de Registro é obrigatório"));
				return;
			}

			if (senhaInput == null || senhaInput.trim().isEmpty()) {
				LOGGER.warn("Senha vazia");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Senha é obrigatória"));
				return;
			}

			// Autenticação
			LOGGER.info("Chamando loginService.autenticar...");
			CadastroUsuario usuario = loginService.autenticar(nuRegistroInput, senhaInput);

			if (usuario != null) {
				// Salva no bean
				this.usuarioLogado = usuario;
				this.logado = true;

				// Salva na sessão HTTP
				FacesContext context = FacesContext.getCurrentInstance();
				HttpSession session = (HttpSession) context.getExternalContext().getSession(true);
				session.setAttribute("usuarioLogado", usuario);

				LOGGER.info("Login realizado com sucesso para: {}", usuario.getNome());

				// Redirecionamento baseado no tipo de usuário
				String url;
				if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
					url = context.getExternalContext().getRequestContextPath() + "/home/homeAdm.xhtml";
				} else if (usuario.getTipoUsuario() == TipoUsuario.TECNICO) {
					url = context.getExternalContext().getRequestContextPath() + "/home/homeTecnico.xhtml";
				} else {
					url = context.getExternalContext().getRequestContextPath() + "/home/homeUser.xhtml";
				}

				LOGGER.info("Redirecionando para: {}", url);
				context.getExternalContext().redirect(url);

			} else {
				LOGGER.warn("Autenticação falhou - usuário retornou null");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Acesso Negado",
								"Número de Registro ou Senha incorretos. Verifique seus dados e tente novamente."));

				// Limpa apenas a senha por segurança
				this.senhaInput = "";
			}

		} catch (Exception e) {
			LOGGER.error("Erro durante o login: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro no Sistema", "Ocorreu um erro interno. Tente novamente em alguns instantes."));
		}
	}

	public String logout() {
		try {
			LOGGER.info("Logout realizado para usuário: {}",
					(usuarioLogado != null ? usuarioLogado.getNome() : "desconhecido"));

			// Limpa os dados do bean
			this.usuarioLogado = null;
			this.logado = false;
			this.nuRegistroInput = "";
			this.senhaInput = "";

			// Invalida a sessão
			FacesContext.getCurrentInstance().getExternalContext().invalidateSession();

			return "/index.xhtml?faces-redirect=true";

		} catch (Exception e) {
			LOGGER.error("Erro durante o logout: ", e);
			return "/index.xhtml?faces-redirect=true";
		}
	}

	public boolean existemUsuarios() {
		try {
			if (cadastroUsuarioDao == null) {
				LOGGER.warn("cadastroUsuarioDao é null em existemUsuarios()");
				return true;
			}
			long count = cadastroUsuarioDao.contarUsuarios();
			LOGGER.debug("Contagem de usuários: {}", count);
			return count > 0;
		} catch (Exception e) {
			LOGGER.error("Erro ao verificar se existem usuários: ", e);
			return true;
		}
	}

	public boolean naoExistemUsuarios() {
		try {
			if (cadastroUsuarioDao == null) {
				LOGGER.warn("cadastroUsuarioDao é null em naoExistemUsuarios()");
				return false;
			}
			long count = cadastroUsuarioDao.contarUsuarios();
			LOGGER.debug("Contagem de usuários (naoExistem): {}", count);
			return count == 0;
		} catch (Exception e) {
			LOGGER.error("Erro ao verificar se não existem usuários: ", e);
			return false;
		}
	}

	public void limpar() {
		this.nuRegistroInput = "";
		this.senhaInput = "";
	}

	// ========== GETTERS E SETTERS ==========

	public String getNuRegistroInput() {
		return nuRegistroInput;
	}

	public void setNuRegistroInput(String nuRegistroInput) {
		this.nuRegistroInput = nuRegistroInput;
	}

	public String getSenhaInput() {
		return senhaInput;
	}

	public void setSenhaInput(String senhaInput) {
		this.senhaInput = senhaInput;
	}

	public CadastroUsuario getUsuarioLogado() {
		return usuarioLogado;
	}

	public void setUsuarioLogado(CadastroUsuario usuarioLogado) {
		this.usuarioLogado = usuarioLogado;
	}

	public boolean isLogado() {
		return logado;
	}

	public void setLogado(boolean logado) {
		this.logado = logado;
	}

	public boolean isInicializado() {
		return inicializado;
	}
}