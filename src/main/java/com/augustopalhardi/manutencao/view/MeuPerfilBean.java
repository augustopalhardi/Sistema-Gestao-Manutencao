package com.augustopalhardi.manutencao.view;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroUsuarioService;
import com.augustopalhardi.manutencao.util.Uploads;

@Named
@ViewScoped
public class MeuPerfilBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MeuPerfilBean.class);

	@Inject
	private ManterCadastroUsuarioService manterCadastroUsuarioService;

	private CadastroUsuario usuarioLogado;

	// Campos para edição
	private String nome;
	private String email;
	private String telefone;
	private String funcao;
	private String senhaAtual;
	private String novaSenha;
	private String confirmarSenha;
	private UploadedFile uploadedPhoto;

	// Controle de edição
	private boolean modoEdicao = false;

	@PostConstruct
	public void inicializar() {
		try {
			LOGGER.info("Inicializando MeuPerfilBean...");
			carregarUsuarioLogado();
		} catch (Exception e) {
			LOGGER.error("Erro ao inicializar MeuPerfilBean: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao carregar perfil", e.getMessage()));
		}
	}

	private void carregarUsuarioLogado() {
		try {
			FacesContext context = FacesContext.getCurrentInstance();
			HttpSession session = (HttpSession) context.getExternalContext().getSession(false);

			if (session != null) {
				this.usuarioLogado = (CadastroUsuario) session.getAttribute("usuarioLogado");

				if (usuarioLogado != null) {
					LOGGER.info("Usuário logado carregado: {}", usuarioLogado.getNome());
					carregarDadosParaEdicao();
				} else {
					LOGGER.warn("Usuário logado não encontrado na sessão");
					context.getExternalContext()
							.redirect(context.getExternalContext().getRequestContextPath() + "/index.xhtml");
				}
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar usuário logado: ", e);
		}
	}

	private void carregarDadosParaEdicao() {
		this.nome = usuarioLogado.getNome();
		this.email = usuarioLogado.getEmail();
		this.telefone = usuarioLogado.getTelefone();
		this.funcao = usuarioLogado.getFuncao();
		this.senhaAtual = "";
		this.novaSenha = "";
		this.confirmarSenha = "";
	}

	public void ativarEdicao() {
		this.modoEdicao = true;
		carregarDadosParaEdicao();
		LOGGER.info("Modo de edição ativado");
	}

	public void cancelarEdicao() {
		this.modoEdicao = false;
		carregarDadosParaEdicao();
		this.uploadedPhoto = null;
		LOGGER.info("Edição cancelada");

		FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_INFO, "Edição cancelada", "As alterações foram descartadas"));
	}

	public String salvarPerfil() {
		try {
			LOGGER.info("=== SALVANDO PERFIL DO USUÁRIO ===");

			// Validações básicas
			if (nome == null || nome.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório", null));
				return null;
			}

			if (email == null || email.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "E-mail é obrigatório", null));
				return null;
			}

			// Validação de senha (se estiver tentando alterar)
			if (novaSenha != null && !novaSenha.trim().isEmpty()) {
				// Verificar senha atual
				if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Senha atual é obrigatória", "Para alterar a senha, informe a senha atual"));
					return null;
				}

				// Verificar se a senha atual está correta (a senha no banco é um hash BCrypt)
				if (!senhaConfere(senhaAtual, usuarioLogado.getSenha())) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Senha atual incorreta", "A senha atual informada não está correta"));
					return null;
				}

				// Verificar confirmação de senha
				if (confirmarSenha == null || !novaSenha.equals(confirmarSenha)) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Senhas não conferem", "A nova senha e a confirmação devem ser iguais"));
					return null;
				}

				// Validar tamanho mínimo da senha
				if (novaSenha.trim().length() < 4) {
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
							"Senha muito curta", "A senha deve ter no mínimo 4 caracteres"));
					return null;
				}
			}

			// Atualizar dados do usuário
			usuarioLogado.setNome(nome.trim());
			usuarioLogado.setEmail(email.trim());
			usuarioLogado.setTelefone(telefone != null ? telefone.trim() : null);
			usuarioLogado.setFuncao(funcao != null ? funcao.trim() : null);

			// Atualizar senha se foi fornecida uma nova
			if (novaSenha != null && !novaSenha.trim().isEmpty()) {
				usuarioLogado.setSenha(novaSenha.trim());
				LOGGER.info("Senha será atualizada");
			}

			// Processar upload da foto se existir
			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().isEmpty()) {
				try {
					String urlFoto = processarUploadFoto();
					usuarioLogado.setUrlFoto(urlFoto);
					LOGGER.info("Foto atualizada: {}", urlFoto);
				} catch (IOException e) {
					LOGGER.error("Erro ao fazer upload da foto: ", e);
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
							"Erro ao salvar foto", "O perfil será atualizado sem a foto"));
				}
			}

			// Salvar no banco de dados
			CadastroUsuario usuarioAtualizado = manterCadastroUsuarioService.salvar(usuarioLogado);

			if (usuarioAtualizado != null && usuarioAtualizado.getId() != null) {
				// Atualizar na sessão
				FacesContext context = FacesContext.getCurrentInstance();
				HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
				if (session != null) {
					session.setAttribute("usuarioLogado", usuarioAtualizado);
					this.usuarioLogado = usuarioAtualizado;
				}

				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Perfil atualizado com sucesso!", "Suas informações foram atualizadas"));

				LOGGER.info("Perfil atualizado com sucesso: {}", usuarioAtualizado.getNome());

				// Desativar modo de edição
				this.modoEdicao = false;
				carregarDadosParaEdicao();
				this.uploadedPhoto = null;

			} else {
				throw new RuntimeException("Erro ao atualizar perfil");
			}

			return null;

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar perfil: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao salvar perfil", "Detalhes: " + e.getMessage()));
			return null;
		}
	}

	/**
	 * Compara a senha digitada com o hash BCrypt salvo. Um valor salvo que não
	 * seja hash válido (dado legado) é tratado como senha incorreta.
	 */
	private static boolean senhaConfere(String senhaDigitada, String hashSalvo) {
		if (senhaDigitada == null || hashSalvo == null) {
			return false;
		}
		try {
			return BCrypt.checkpw(senhaDigitada, hashSalvo);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("Senha armazenada não está em formato BCrypt");
			return false;
		}
	}

	private String processarUploadFoto() throws IOException {
		return Uploads.url(Uploads.USUARIOS) + Uploads.salvar(uploadedPhoto, Uploads.USUARIOS);
	}

	public void removerFoto() {
		try {
			if (usuarioLogado.getUrlFoto() != null && !usuarioLogado.getUrlFoto().isEmpty()) {
				// Remover arquivo físico
				try {
					// Extrai o nome do arquivo da URL
					String nomeArquivo = usuarioLogado.getUrlFoto();
					if (nomeArquivo.contains("/")) {
						nomeArquivo = nomeArquivo.substring(nomeArquivo.lastIndexOf("/") + 1);
					}

					// Caminho físico completo
					String uploadDirectory = Uploads.diretorio(Uploads.USUARIOS);
					File fileToDelete = new File(uploadDirectory + nomeArquivo);

					if (fileToDelete.exists()) {
						boolean deleted = fileToDelete.delete();
						if (deleted) {
							LOGGER.info("Arquivo de foto excluído: {}", fileToDelete.getAbsolutePath());
						} else {
							LOGGER.warn("Não foi possível excluir o arquivo: {}", fileToDelete.getAbsolutePath());
						}
					} else {
						LOGGER.warn("Arquivo não encontrado para exclusão: {}", fileToDelete.getAbsolutePath());
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao excluir arquivo de foto: {}", e.getMessage());
				}

				// Atualizar no banco
				usuarioLogado.setUrlFoto(null);
				CadastroUsuario usuarioAtualizado = manterCadastroUsuarioService.salvar(usuarioLogado);

				// Atualizar na sessão
				FacesContext context = FacesContext.getCurrentInstance();
				HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
				if (session != null) {
					session.setAttribute("usuarioLogado", usuarioAtualizado);
					this.usuarioLogado = usuarioAtualizado;
				}

				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Foto removida com sucesso!", null));

				LOGGER.info("Foto removida com sucesso");
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao remover foto: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao remover foto", e.getMessage()));
		}
	}

	// Getters e Setters
	public CadastroUsuario getUsuarioLogado() {
		return usuarioLogado;
	}

	public void setUsuarioLogado(CadastroUsuario usuarioLogado) {
		this.usuarioLogado = usuarioLogado;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getFuncao() {
		return funcao;
	}

	public void setFuncao(String funcao) {
		this.funcao = funcao;
	}

	public String getSenhaAtual() {
		return senhaAtual;
	}

	public void setSenhaAtual(String senhaAtual) {
		this.senhaAtual = senhaAtual;
	}

	public String getNovaSenha() {
		return novaSenha;
	}

	public void setNovaSenha(String novaSenha) {
		this.novaSenha = novaSenha;
	}

	public String getConfirmarSenha() {
		return confirmarSenha;
	}

	public void setConfirmarSenha(String confirmarSenha) {
		this.confirmarSenha = confirmarSenha;
	}

	public UploadedFile getUploadedPhoto() {
		return uploadedPhoto;
	}

	public void setUploadedPhoto(UploadedFile uploadedPhoto) {
		this.uploadedPhoto = uploadedPhoto;
	}

	public boolean isModoEdicao() {
		return modoEdicao;
	}

	public void setModoEdicao(boolean modoEdicao) {
		this.modoEdicao = modoEdicao;
	}
}