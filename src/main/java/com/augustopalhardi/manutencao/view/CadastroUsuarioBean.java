package com.augustopalhardi.manutencao.view;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.service.ManterCadastroUsuarioService;
import com.augustopalhardi.manutencao.util.Uploads;

@Named
@ViewScoped
public class CadastroUsuarioBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CadastroUsuarioBean.class);

	@Inject
	private ManterCadastroUsuarioService manterCadastroUsuarioService;

	private List<CadastroUsuario> cadastroUsuarios = new ArrayList<>();
	private List<CadastroUsuario> cadastroUsuariosFiltrados = new ArrayList<>();
	private UploadedFile uploadedPhoto;

	// Campos individuais
	private String nome;
	private String nuRegistro;
	private String email;
	private String senha;
	private String funcao;
	private String telefone;
	private String tipoUsuario;
	private Double valorHoraMaquina;
	private Double valorHoraPredial;

	// Para edição
	private CadastroUsuario usuarioSelecionado;

	// Filtro de status
	private String filtroStatus = "TODOS";
	private List<CadastroUsuario> cadastroUsuariosFiltradosPorStatus;

	@PostConstruct
	public void inicializar() {
		try {
			LOGGER.info("Inicializando CadastroUsuarioBean...");
			limpar();
			carregarUsuarios();
		} catch (Exception e) {
			LOGGER.error("Erro em inicializar(): ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao inicializar: " + e.getMessage(), null));
		}
	}

	private void carregarUsuarios() {
		try {
			this.cadastroUsuarios = manterCadastroUsuarioService.buscarTodos();
			LOGGER.info("Usuários carregados: {}", cadastroUsuarios.size());
			aplicarFiltroStatus();
			// Sincronizar a lista filtrada do PrimeFaces com a lista atual
			this.cadastroUsuariosFiltrados = new ArrayList<>(this.cadastroUsuariosFiltradosPorStatus);
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar usuários: {}", e.getMessage());
			this.cadastroUsuarios = new ArrayList<>();
			this.cadastroUsuariosFiltradosPorStatus = new ArrayList<>();
			this.cadastroUsuariosFiltrados = new ArrayList<>();
		}
	}

	public void aplicarFiltroStatus() {
		if ("ATIVO".equals(filtroStatus)) {
			cadastroUsuariosFiltradosPorStatus = new ArrayList<>();
			for (CadastroUsuario u : cadastroUsuarios) {
				if (u.getAtivo() == null || u.getAtivo()) {
					cadastroUsuariosFiltradosPorStatus.add(u);
				}
			}
		} else if ("INATIVO".equals(filtroStatus)) {
			cadastroUsuariosFiltradosPorStatus = new ArrayList<>();
			for (CadastroUsuario u : cadastroUsuarios) {
				if (u.getAtivo() != null && !u.getAtivo()) {
					cadastroUsuariosFiltradosPorStatus.add(u);
				}
			}
		} else {
			cadastroUsuariosFiltradosPorStatus = new ArrayList<>(cadastroUsuarios);
		}
		// Sincronizar a lista filtrada do PrimeFaces
		this.cadastroUsuariosFiltrados = new ArrayList<>(cadastroUsuariosFiltradosPorStatus);
	}

	public void filtrarPorStatus() {
		aplicarFiltroStatus();
	}

	public String salvar() {
		try {
			LOGGER.info("=== SALVAMENTO USUÁRIO ===");
			LOGGER.info("Nome: [{}]", nome);
			LOGGER.info("NuRegistro: [{}]", nuRegistro);
			LOGGER.info("Email: [{}]", email);
			LOGGER.info("TipoUsuario: [{}]", tipoUsuario);
			LOGGER.info("Funcao: [{}]", funcao);
			LOGGER.info("Telefone: [{}]", telefone);
			LOGGER.info("ValorHoraMaquina: [{}]", valorHoraMaquina);
			LOGGER.info("ValorHoraPredial: [{}]", valorHoraPredial);

			// DEBUG: Verificar se usuarioSelecionado está sendo mantido
			if (usuarioSelecionado != null) {
				LOGGER.info("UsuarioSelecionado: ID={}, Nome={}", usuarioSelecionado.getId(),
						usuarioSelecionado.getNome());
			} else {
				LOGGER.warn("UsuarioSelecionado é NULL - será criado novo usuário");
			}

			// Verificar se há falha de validação JSF
			if (FacesContext.getCurrentInstance().isValidationFailed()) {
				LOGGER.warn("Validação JSF falhou, interrompendo salvamento");
				return null;
			}

			// Validações básicas
			if (nome == null || nome.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório", null));
				return null;
			}

			if (email == null || email.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório", null));
				return null;
			}

			// VALIDAÇÃO DE NÚMERO DE REGISTRO DUPLICADO
			if (nuRegistro != null && !nuRegistro.trim().isEmpty()) {
				String nuRegistroTrimmed = nuRegistro.trim();

				for (CadastroUsuario usuarioExistente : cadastroUsuarios) {
					boolean isEdicaoDoMesmoUsuario = usuarioSelecionado != null && usuarioSelecionado.getId() != null
							&& usuarioSelecionado.getId().equals(usuarioExistente.getId());

					if (!isEdicaoDoMesmoUsuario && usuarioExistente.getNuRegistro() != null
							&& usuarioExistente.getNuRegistro().trim().equalsIgnoreCase(nuRegistroTrimmed)) {

						LOGGER.warn("Tentativa de cadastrar número de registro duplicado: {}", nuRegistroTrimmed);
						FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
								"Número de Registro já cadastrado",
								"O número de registro '" + nuRegistroTrimmed + "' já está em uso por outro usuário."));
						return null;
					}
				}
			}

			// VALIDAÇÃO DE EMAIL DUPLICADO
			{
				String emailTrimmed = email.trim();
				for (CadastroUsuario usuarioExistente : cadastroUsuarios) {
					boolean isEdicaoDoMesmoUsuario = usuarioSelecionado != null && usuarioSelecionado.getId() != null
							&& usuarioSelecionado.getId().equals(usuarioExistente.getId());
					if (!isEdicaoDoMesmoUsuario && usuarioExistente.getEmail() != null
							&& usuarioExistente.getEmail().trim().equalsIgnoreCase(emailTrimmed)) {
						LOGGER.warn("Tentativa de cadastrar e-mail duplicado: {}", emailTrimmed);
						FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
								"E-mail já cadastrado",
								"O e-mail '" + emailTrimmed + "' já está em uso por outro usuário."));
						return null;
					}
				}
			}

			// Criar/atualizar objeto CadastroUsuario
			CadastroUsuario usuario;
			if (usuarioSelecionado != null && usuarioSelecionado.getId() != null) {
				// Editando
				usuario = usuarioSelecionado;
				LOGGER.info("Editando usuário existente: ID={}", usuario.getId());
			} else {
				// Novo
				usuario = new CadastroUsuario();
				LOGGER.info("Criando novo usuário");
			}

			// Definir valores
			usuario.setNome(nome.trim());
			usuario.setNuRegistro(nuRegistro != null ? nuRegistro.trim() : null);
			usuario.setEmail(email.trim());

			// Para edição, só atualizar senha se foi fornecida uma nova
			if (usuarioSelecionado == null || (senha != null && !senha.trim().isEmpty())) {
				usuario.setSenha(senha != null ? senha.trim() : null);
			}

			usuario.setFuncao(funcao != null ? funcao.trim() : null);
			usuario.setTelefone(telefone != null ? telefone.trim() : null);

			// Converter String para Enum e processar valor hora
			if (tipoUsuario != null && !tipoUsuario.trim().isEmpty()) {
				try {
					TipoUsuario tipo = TipoUsuario.valueOf(tipoUsuario.trim().toUpperCase());
					usuario.setTipoUsuario(tipo);
					LOGGER.info("Tipo de usuário definido: {}", tipoUsuario.trim().toUpperCase());

					// Processar valor hora apenas se for TÉCNICO
					if (tipo == TipoUsuario.TECNICO) {
						if (valorHoraMaquina != null && valorHoraMaquina > 0 && valorHoraPredial != null
								&& valorHoraPredial > 0) {
							usuario.setValorHoraMaquina(valorHoraMaquina);
							usuario.setValorHoraPredial(valorHoraPredial);
							LOGGER.info("Valor hora técnico definido: Máquina R$ {} / Predial R$ {}", valorHoraMaquina,
									valorHoraPredial);
						} else {
							FacesContext.getCurrentInstance().addMessage(null,
									new FacesMessage(FacesMessage.SEVERITY_ERROR,
											"Valor da hora (Máquina e Predial) é obrigatório para técnicos", null));
							return null;
						}
					} else {
						// Se não for técnico, limpar os valores
						usuario.setValorHoraMaquina(null);
						usuario.setValorHoraPredial(null);
						LOGGER.info("Tipo não é técnico, valor hora limpo");
					}
				} catch (IllegalArgumentException e) {
					LOGGER.warn("Tipo de usuário inválido: {}, usando FUNCIONARIO como padrão", tipoUsuario);
					usuario.setTipoUsuario(TipoUsuario.FUNCIONARIO);
					usuario.setValorHoraMaquina(null);
					usuario.setValorHoraPredial(null);
				}
			} else {
				usuario.setTipoUsuario(TipoUsuario.FUNCIONARIO);
				usuario.setValorHoraMaquina(null);
				usuario.setValorHoraPredial(null);
				LOGGER.info("Tipo de usuário padrão: FUNCIONARIO");
			}

			// Processar upload da foto se existir
			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().isEmpty()) {
				try {
					String urlFoto = processarUploadFoto();
					usuario.setUrlFoto(urlFoto);
					LOGGER.info("Foto processada: {}", urlFoto);
				} catch (IOException e) {
					LOGGER.error("Erro ao fazer upload da foto: ", e);
					FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
							"Erro ao salvar foto, mas usuário será cadastrado", null));
				}
			}

			// Salvar usuário
			CadastroUsuario usuarioSalvo = manterCadastroUsuarioService.salvar(usuario);

			if (usuarioSalvo != null && usuarioSalvo.getId() != null) {
				String acao = usuarioSelecionado != null ? "atualizado" : "cadastrado";
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Sucesso!", "Usuário " + usuarioSalvo.getNome() + " " + acao + " com sucesso!"));

				LOGGER.info("Usuário {} com sucesso: ID={}, Nome={}", acao, usuarioSalvo.getId(),
						usuarioSalvo.getNome());

				// Limpar formulário e recarregar lista
				limpar();
				carregarUsuarios();

				// Fechar o dialog se estiver aberto
				PrimeFaces.current().executeScript("PF('editarDialog').hide();");
			} else {
				throw new RuntimeException("Usuário não foi salvo corretamente");
			}

			return null; // Permanece na mesma página

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar usuário: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao salvar usuário", "Detalhes: " + e.getMessage()));
			return null;
		}
	}

	public String prepararEdicao(CadastroUsuario usuario) {
		try {
			LOGGER.info("Preparando edição do usuário: {}", usuario.getNome());
			this.usuarioSelecionado = usuario;
			this.nome = usuario.getNome();
			this.nuRegistro = usuario.getNuRegistro();
			this.email = usuario.getEmail();
			this.senha = ""; // Não mostrar senha atual por segurança
			this.funcao = usuario.getFuncao();
			this.telefone = usuario.getTelefone();
			this.tipoUsuario = usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().toString() : "";
			this.valorHoraMaquina = usuario.getValorHoraMaquina();
			this.valorHoraPredial = usuario.getValorHoraPredial();
			this.uploadedPhoto = null; // Limpar upload anterior

			LOGGER.info("Dados carregados para edição - ID: {}, Nome: {}, ValorHoraMaquina: {}, ValorHoraPredial: {}",
					usuario.getId(), usuario.getNome(), usuario.getValorHoraMaquina(), usuario.getValorHoraPredial());

			return null;
		} catch (Exception e) {
			LOGGER.error("Erro ao preparar edição: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao preparar edição", e.getMessage()));
			return null;
		}
	}

	public void abrirDialogEdicao(CadastroUsuario usuario) {
		prepararEdicao(usuario);
		PrimeFaces.current().executeScript("PF('editarDialog').show();");
	}

	public String prepararNovo() {
		LOGGER.info("Preparando novo usuário");
		limpar();
		return null;
	}

	private String processarUploadFoto() throws IOException {
		return Uploads.url(Uploads.USUARIOS) + Uploads.salvar(uploadedPhoto, Uploads.USUARIOS);
	}

	public String excluir(CadastroUsuario usuario) {
		try {
			if (usuario == null || usuario.getId() == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Usuário inválido para exclusão"));
				return null;
			}

			// Excluir arquivo de foto se existir
			if (usuario.getUrlFoto() != null && !usuario.getUrlFoto().isEmpty()) {
				try {
					File fileToDelete = new File(Uploads.diretorio(Uploads.USUARIOS)
							+ usuario.getUrlFoto().substring(usuario.getUrlFoto().lastIndexOf("/") + 1));
					if (fileToDelete.exists()) {
						fileToDelete.delete();
						LOGGER.info("Arquivo de foto excluído: {}", fileToDelete.getAbsolutePath());
					}
				} catch (Exception e) {
					LOGGER.warn("Erro ao excluir arquivo de foto: {}", e.getMessage());
				}
			}

			manterCadastroUsuarioService.excluir(usuario);
			carregarUsuarios();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso!", "Usuário excluído com sucesso!"));
			LOGGER.info("Usuário excluído: {}", usuario.getNome());
			return null;
		} catch (Exception e) {
			LOGGER.error("Erro ao excluir usuário: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao excluir usuário", "Erro: " + e.getMessage()));
			return null;
		}
	}

	public String alternarStatus(CadastroUsuario usuario) {
		try {
			String statusAnterior = usuario.isAtivo() ? "Ativo" : "Inativo";
			manterCadastroUsuarioService.alternarStatus(usuario);
			String statusNovo = usuario.isAtivo() ? "Ativo" : "Inativo";
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Usuário " + usuario.getNome() + " alterado de " + statusAnterior + " para " + statusNovo));
			LOGGER.info("Status do usuário ID: {} alterado de {} para {}", usuario.getId(), statusAnterior, statusNovo);
			cadastroUsuarios = manterCadastroUsuarioService.buscarTodos();
			aplicarFiltroStatus();
		} catch (Exception e) {
			LOGGER.error("Erro ao alternar status do usuário: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao alterar status do usuário"));
		}
		return null;
	}

	public String limpar() {
		nome = "";
		nuRegistro = "";
		email = "";
		senha = "";
		funcao = "";
		telefone = "";
		tipoUsuario = "";
		valorHoraMaquina = null;
		valorHoraPredial = null;
		uploadedPhoto = null;
		usuarioSelecionado = null;
		LOGGER.info("Formulário limpo");
		return null;
	}

	// Getters e Setters

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getNuRegistro() {
		return nuRegistro;
	}

	public void setNuRegistro(String nuRegistro) {
		this.nuRegistro = nuRegistro;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getFuncao() {
		return funcao;
	}

	public void setFuncao(String funcao) {
		this.funcao = funcao;
	}

	public String getTelefone() {
		return telefone;
	}

	public void setTelefone(String telefone) {
		this.telefone = telefone;
	}

	public String getTipoUsuario() {
		return tipoUsuario;
	}

	public void setTipoUsuario(String tipoUsuario) {
		this.tipoUsuario = tipoUsuario;
	}

	public Double getValorHoraMaquina() {
		return valorHoraMaquina;
	}

	public void setValorHoraMaquina(Double valorHoraMaquina) {
		this.valorHoraMaquina = valorHoraMaquina;
	}

	public Double getValorHoraPredial() {
		return valorHoraPredial;
	}

	public void setValorHoraPredial(Double valorHoraPredial) {
		this.valorHoraPredial = valorHoraPredial;
	}

	public List<CadastroUsuario> getCadastroUsuarios() {
		return cadastroUsuarios;
	}

	public void setCadastroUsuarios(List<CadastroUsuario> cadastroUsuarios) {
		this.cadastroUsuarios = cadastroUsuarios;
	}

	public List<CadastroUsuario> getCadastroUsuariosFiltrados() {
		return cadastroUsuariosFiltrados;
	}

	public void setCadastroUsuariosFiltrados(List<CadastroUsuario> cadastroUsuariosFiltrados) {
		this.cadastroUsuariosFiltrados = cadastroUsuariosFiltrados;
	}

	public UploadedFile getUploadedPhoto() {
		return uploadedPhoto;
	}

	public void setUploadedPhoto(UploadedFile uploadedPhoto) {
		this.uploadedPhoto = uploadedPhoto;
	}

	public CadastroUsuario getUsuarioSelecionado() {
		return usuarioSelecionado;
	}

	public void setUsuarioSelecionado(CadastroUsuario usuarioSelecionado) {
		this.usuarioSelecionado = usuarioSelecionado;
	}

	public String getFiltroStatus() {
		return filtroStatus;
	}

	public void setFiltroStatus(String filtroStatus) {
		this.filtroStatus = filtroStatus;
	}

	public List<CadastroUsuario> getCadastroUsuariosFiltradosPorStatus() {
		return cadastroUsuariosFiltradosPorStatus;
	}

	public String salvarPrimeiraConta() {
		try {
			LOGGER.info("=== CRIANDO PRIMEIRA CONTA (ADMINISTRADOR) ===");

			if (manterCadastroUsuarioService.buscarTodos() != null
					&& !manterCadastroUsuarioService.buscarTodos().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Já existem usuários cadastrados no sistema."));
				return null;
			}

			if (nome == null || nome.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório", null));
				return null;
			}

			if (email == null || email.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório", null));
				return null;
			}

			if (senha == null || senha.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Senha é obrigatória", null));
				return null;
			}

			if (nuRegistro == null || nuRegistro.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Número de Registro é obrigatório", null));
				return null;
			}

			CadastroUsuario usuario = new CadastroUsuario();
			usuario.setNome(nome.trim());
			usuario.setNuRegistro(nuRegistro.trim());
			usuario.setEmail(email.trim());
			usuario.setSenha(senha.trim());
			usuario.setFuncao(funcao != null ? funcao.trim() : "Administrador");
			usuario.setTelefone(telefone != null ? telefone.trim() : null);
			usuario.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
			usuario.setValorHoraMaquina(null);
			usuario.setValorHoraPredial(null);

			CadastroUsuario usuarioSalvo = manterCadastroUsuarioService.salvar(usuario);

			if (usuarioSalvo != null && usuarioSalvo.getId() != null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
						"Sucesso!", "Primeira conta criada com sucesso! Faça login para continuar."));
				LOGGER.info("Primeira conta criada com sucesso: ID={}, Nome={}", usuarioSalvo.getId(),
						usuarioSalvo.getNome());
				return "/index.xhtml?faces-redirect=true";
			} else {
				throw new RuntimeException("Erro ao criar primeira conta");
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao criar primeira conta: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao criar conta", "Detalhes: " + e.getMessage()));
			return null;
		}
	}
}