package com.augustopalhardi.manutencao.view;

import com.augustopalhardi.manutencao.util.Uploads;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.shaded.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.ManterCadastroLocalService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;
import com.augustopalhardi.manutencao.util.Mensagens;

@Named
@ViewScoped
public class CadastroLocalBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CadastroLocalBean.class);

	private static final String UPLOAD_DIRECTORY = Uploads.diretorio(Uploads.LOCAIS);

	@Inject
	private ManterCadastroLocalService manterCadastroLocalService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterSolicitacaoManutencaoPredialService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusManutencaoService;

	// Dialog global de imagem, cuja marcação fica em
	// /includes/visualizarImagemDialog.xhtml.
	@Inject
	private VisualizarImagemBean visualizarImagemBean;

	private List<CadastroLocal> cadastroLocais = new ArrayList<>();
	private List<CadastroLocal> cadastroLocaisFiltrados = new ArrayList<>();
	private UploadedFile uploadedPhoto;

	private String nome;
	private Integer area;
	private String observacoes;

	// Filtro da tela de cadastro (local.xhtml): "TODOS", "ATIVO" ou "INATIVO".
	private String filtroStatus = "TODOS";

	// Filtro da tela de consulta (localTecnico.xhtml), independente do filtro
	// de cadastro. Inicia em "ATIVO" para que a consulta abra já mostrando
	// somente os locais ativos.
	private String filtroStatusTecnico = "ATIVO";

	private CadastroLocal localSelecionado;

	private List<SolicitacaoManutencaoPredial> manutencoesDoLocal = new ArrayList<>();

	private SolicitacaoManutencaoPredial manutencaoSelecionada;

	// Status das manutenções do local selecionado, carregados de uma vez para
	// evitar uma consulta ao banco por linha (e de novo a cada comparação da
	// ordenação) toda vez que o histórico é exibido.
	private Map<Long, ControleStatusManutencao> cacheControlesLocal = new HashMap<>();

	@PostConstruct
	public void inicializar() {
		prepararNovo();
		cadastroLocais = manterCadastroLocalService.buscarTodos();
		aplicarFiltroStatus();
	}

	// ========== AÇÕES ==========

	public String prepararNovo() {
		nome = null;
		area = null;
		observacoes = null;
		uploadedPhoto = null;
		localSelecionado = null;
		manutencoesDoLocal = new ArrayList<>();
		return null;
	}

	public String prepararEdicao(CadastroLocal local) {
		try {
			this.localSelecionado = local;
			this.nome = local.getNome();

			// Local gravado sem área vem como 0. Trazer 0 para o campo faria o
			// usuário ver "0" onde na verdade não há informação, então o campo
			// abre em branco nesse caso.
			this.area = local.getArea() > 0 ? local.getArea() : null;

			this.observacoes = local.getObservacoes();
			this.uploadedPhoto = null;

			LOGGER.info("Preparando edição do local ID: {}, Nome: {}", local.getId(), local.getNome());

			return null;
		} catch (Exception e) {
			LOGGER.error("Erro ao preparar edição: ", e);
			Mensagens.erro("Erro ao preparar edição");
			return null;
		}
	}

	public void salvar() {
		try {
			if (FacesContext.getCurrentInstance().isValidationFailed()) {
				LOGGER.warn("Validação JSF falhou, interrompendo salvamento");
				return;
			}

			if (nome == null || nome.trim().isEmpty()) {
				Mensagens.erro("Nome do local é obrigatório");
				return;
			}

			// Área é opcional. Valor negativo não é aceito — a tela já impede
			// pelo minValue="0", então só chegaria aqui por manipulação da
			// requisição.
			if (area != null && area < 0) {
				Mensagens.erro("Área não pode ser negativa");
				return;
			}

			CadastroLocal local;
			String acao;

			if (localSelecionado != null) {
				local = localSelecionado;
				acao = "atualizado";
				LOGGER.info("Editando local existente: ID={}", local.getId());
			} else {
				local = new CadastroLocal();
				acao = "cadastrado";
				LOGGER.info("Criando novo local");
			}

			local.setNome(nome);

			// O bean guarda Integer e a entidade guarda int primitivo, então
			// passar null direto estouraria NullPointerException no
			// auto-unboxing. Local sem área informada é gravado como 0, e as
			// telas exibem "—" nesse caso.
			local.setArea(area != null ? area : 0);

			local.setObservacoes(observacoes);

			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().trim().isEmpty() && uploadedPhoto.getSize() > 0) {
				try {
					String urlFoto = processarUploadFoto(uploadedPhoto);
					local.setUrlFoto(urlFoto);
					LOGGER.info("Foto processada: {}", urlFoto);
				} catch (IOException e) {
					LOGGER.error("Erro ao fazer upload da foto: ", e);
					Mensagens.aviso("Erro ao salvar foto, mas local será cadastrado");
				}
			}

			CadastroLocal localSalvo = manterCadastroLocalService.salvar(local);

			if (localSalvo == null) {
				throw new RuntimeException("Local não foi salvo corretamente");
			}

			Mensagens.info("Sucesso!", String.format("Local %s com sucesso!", acao));

			LOGGER.info("Local {} com sucesso: ID={}, Nome={}", acao, localSalvo.getId(), localSalvo.getNome());

			prepararNovo();
			cadastroLocais = manterCadastroLocalService.buscarTodos();
			aplicarFiltroStatus();

			PrimeFaces.current().executeScript("PF('editarDialog').hide();");

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar local: ", e);
			Mensagens.erro("Erro ao salvar local");
		}
	}

	public void excluir(CadastroLocal local) {
		try {
			if (local == null) {
				Mensagens.erro("Local inválido para exclusão");
				return;
			}

			if (local.getUrlFoto() != null && !local.getUrlFoto().isEmpty()) {
				try {
					File file = new File(UPLOAD_DIRECTORY + local.getUrlFoto());

					if (file.exists() && file.delete()) {
						LOGGER.info("Arquivo de foto excluído: {}", file.getAbsolutePath());
					}
				} catch (Exception e) {
					LOGGER.error("Erro ao excluir arquivo de foto: ", e);
				}
			}

			manterCadastroLocalService.excluir(local);
			cadastroLocais.remove(local);
			aplicarFiltroStatus();

			Mensagens.info("Local excluído com sucesso!");

			LOGGER.info("Local excluído: {}", local.getId());

		} catch (Exception e) {
			LOGGER.error("Erro ao excluir local: ", e);
			Mensagens.erro("Erro ao excluir local");
		}
	}

	/**
	 * Inverte o status ativo/inativo do local e recarrega a listagem.
	 */
	public void alternarStatus(CadastroLocal local) {
		try {
			String statusAnterior = local.isAtivo() ? "Ativo" : "Inativo";
			manterCadastroLocalService.alternarStatus(local);
			String statusNovo = local.isAtivo() ? "Ativo" : "Inativo";

			Mensagens.info("Local " + local.getNome() + " alterado de " + statusAnterior + " para " + statusNovo);

			LOGGER.info("Status do local ID: {} alterado de {} para {}", local.getId(), statusAnterior, statusNovo);

			cadastroLocais = manterCadastroLocalService.buscarTodos();
			aplicarFiltroStatus();

		} catch (Exception e) {
			LOGGER.error("Erro ao alternar status do local: ", e);
			Mensagens.erro("Erro ao alterar status do local");
		}
	}

	/**
	 * Monta cadastroLocaisFiltrados a partir do filtro de status da tela de
	 * cadastro. Registros com "ativo" nulo contam como ativos.
	 */
	public void aplicarFiltroStatus() {
		if ("ATIVO".equals(filtroStatus)) {
			cadastroLocaisFiltrados = new ArrayList<>();
			for (CadastroLocal l : cadastroLocais) {
				if (l.getAtivo() == null || l.getAtivo()) {
					cadastroLocaisFiltrados.add(l);
				}
			}
		} else if ("INATIVO".equals(filtroStatus)) {
			cadastroLocaisFiltrados = new ArrayList<>();
			for (CadastroLocal l : cadastroLocais) {
				if (l.getAtivo() != null && !l.getAtivo()) {
					cadastroLocaisFiltrados.add(l);
				}
			}
		} else {
			cadastroLocaisFiltrados = new ArrayList<>(cadastroLocais);
		}
		LOGGER.info("Filtro '{}' aplicado. Exibindo {} de {} locais", filtroStatus, cadastroLocaisFiltrados.size(),
				cadastroLocais.size());
	}

	/**
	 * Chamado pelo selectOneMenu ao trocar o filtro da tela de cadastro.
	 */
	public void filtrarPorStatus() {
		aplicarFiltroStatus();
	}

	/**
	 * Carrega o histórico de manutenções do local selecionado, já ordenado por
	 * status.
	 */
	public void carregarManutencoes(CadastroLocal local) {
		try {
			this.localSelecionado = local;
			this.manutencoesDoLocal = manterSolicitacaoManutencaoPredialService.buscarPorLocal(local);
			carregarCacheControlesLocal();
			this.manutencoesDoLocal.sort(Comparator.comparingInt(this::ordemStatusManutencao));
			LOGGER.info("Carregadas {} manutenções para o local ID: {}", manutencoesDoLocal.size(), local.getId());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar manutenções do local: ", e);
			this.manutencoesDoLocal = new ArrayList<>();
			Mensagens.erro("Erro ao carregar histórico de manutenções");
		}
	}

	/**
	 * Prepara o dialog global de imagem com a foto do local.
	 */
	public void verImagem(CadastroLocal local) {
		this.localSelecionado = local;

		if (local == null) {
			visualizarImagemBean.abrir("Foto do Local", null);
			return;
		}

		visualizarImagemBean.abrir("Foto do Local", local.getNome(), "locais/" + local.getUrlFoto());

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a este local.");
		visualizarImagemBean.addInfo("ID", local.getId());
		visualizarImagemBean.addInfo("Nome", local.getNome());
		// Área é opcional: sem valor cadastrado mostra "—".
		visualizarImagemBean.addInfo("Área (m²)", local.getArea() > 0 ? local.getArea() : "—");
		visualizarImagemBean.addInfo("Observações", local.getObservacoes());
		visualizarImagemBean.addInfo("Status", isRegistroAtivo(local) ? "Ativo" : "Inativo");
	}

	public void limpar() {
		prepararNovo();
	}

	// ========== MÉTODOS AUXILIARES ==========

	/**
	 * Grava a foto em UPLOAD_DIRECTORY com um nome baseado no timestamp e
	 * devolve apenas esse nome de arquivo, que é o que fica gravado na
	 * entidade.
	 */
	private String processarUploadFoto(UploadedFile foto) throws IOException {
		try {
			String extension = FilenameUtils.getExtension(foto.getFileName());

			String uniqueFileName = System.currentTimeMillis() + "." + extension;

			File uploadDirectory = new File(UPLOAD_DIRECTORY);
			if (!uploadDirectory.exists()) {
				boolean created = uploadDirectory.mkdirs();
				LOGGER.info("Diretório criado: {} - Sucesso: {}", UPLOAD_DIRECTORY, created);
			}

			String caminhoCompleto = UPLOAD_DIRECTORY + uniqueFileName;
			File file = new File(caminhoCompleto);

			try (InputStream input = foto.getInputStream(); OutputStream output = new FileOutputStream(file)) {

				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = input.read(buffer)) != -1) {
					output.write(buffer, 0, bytesRead);
				}
			}

			LOGGER.info("Foto salva com sucesso em: {}", caminhoCompleto);

			return uniqueFileName;

		} catch (IOException e) {
			LOGGER.error("Erro ao processar upload: ", e);
			throw e;
		}
	}

	/**
	 * Carrega, em uma única consulta, os status de todas as manutenções
	 * prediais, evitando uma consulta por linha a cada exibição do histórico.
	 */
	private void carregarCacheControlesLocal() {
		cacheControlesLocal = new HashMap<>();
		try {
			for (ControleStatusManutencao c : manterControleStatusManutencaoService.buscarTodos()) {
				if (c.getTipoManutencao() == TipoManutencaoReferencia.PREDIAL) {
					cacheControlesLocal.put(c.getSolicitacaoId(), c);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar cache de status de manutenção predial: {}", e.getMessage());
		}
	}

	/**
	 * Status real da manutenção, lido do cache em memória. Manutenção sem
	 * registro de controle é tratada como PARA_FAZER.
	 */
	private ControleStatusManutencao.StatusManutencao obterStatusManutencao(SolicitacaoManutencaoPredial m) {
		if (m.isCancelada()) {
			return ControleStatusManutencao.StatusManutencao.CANCELADA;
		}
		try {
			ControleStatusManutencao controle = cacheControlesLocal.get(m.getId());
			if (controle != null && controle.getStatus() != null) {
				return controle.getStatus();
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao consultar status da manutencao PREDIAL ID {}: {}", m.getId(), e.getMessage());
		}
		return ControleStatusManutencao.StatusManutencao.PARA_FAZER;
	}

	private int ordemStatusManutencao(SolicitacaoManutencaoPredial m) {
		return obterStatusManutencao(m).getOrdem();
	}

	/**
	 * Critério único de "ativo" usado tanto pelo filtro quanto pela coluna
	 * Status da tela de consulta.
	 *
	 * Existe porque a entidade expõe getAtivo() (Boolean) e isAtivo()
	 * (boolean). O EL resolve "#{local.ativo}" para isAtivo(), que devolve
	 * false quando o campo é nulo, enquanto getAtivo() devolve nulo. Isso fazia
	 * a coluna mostrar "Inativo" e o filtro considerar "Ativo" o mesmo
	 * registro. Aqui nulo é tratado como ATIVO, mesmo default da entidade
	 * (private Boolean ativo = true).
	 */
	public boolean isRegistroAtivo(CadastroLocal local) {
		return local == null || local.getAtivo() == null || local.getAtivo();
	}

	// ========== GETTERS E SETTERS ==========

	public List<CadastroLocal> getCadastroLocais() {
		return cadastroLocais;
	}

	public void setCadastroLocais(List<CadastroLocal> cadastroLocais) {
		this.cadastroLocais = cadastroLocais;
	}

	public List<CadastroLocal> getCadastroLocaisFiltrados() {
		return cadastroLocaisFiltrados;
	}

	public void setCadastroLocaisFiltrados(List<CadastroLocal> cadastroLocaisFiltrados) {
		this.cadastroLocaisFiltrados = cadastroLocaisFiltrados;
	}

	/**
	 * Lista exibida na tela de consulta de locais (localTecnico.xhtml), já
	 * aplicando o filtro de status escolhido na coluna "Status".
	 */
	public List<CadastroLocal> getLocaisTecnico() {
		List<CadastroLocal> resultado = new ArrayList<>();

		if (cadastroLocais == null) {
			return resultado;
		}

		for (CadastroLocal l : cadastroLocais) {
			boolean ativo = isRegistroAtivo(l);

			if ("ATIVO".equals(filtroStatusTecnico) && !ativo) {
				continue;
			}
			if ("INATIVO".equals(filtroStatusTecnico) && ativo) {
				continue;
			}
			resultado.add(l);
		}

		return resultado;
	}

	public String getFiltroStatus() {
		return filtroStatus;
	}

	public void setFiltroStatus(String filtroStatus) {
		this.filtroStatus = filtroStatus;
	}

	public String getFiltroStatusTecnico() {
		return filtroStatusTecnico;
	}

	public void setFiltroStatusTecnico(String filtroStatusTecnico) {
		this.filtroStatusTecnico = filtroStatusTecnico;
	}

	public UploadedFile getUploadedPhoto() {
		return uploadedPhoto;
	}

	public void setUploadedPhoto(UploadedFile uploadedPhoto) {
		this.uploadedPhoto = uploadedPhoto;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Integer getArea() {
		return area;
	}

	public void setArea(Integer area) {
		this.area = area;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public CadastroLocal getLocalSelecionado() {
		return localSelecionado;
	}

	public void setLocalSelecionado(CadastroLocal localSelecionado) {
		this.localSelecionado = localSelecionado;
	}

	public List<SolicitacaoManutencaoPredial> getManutencoesDoLocal() {
		return manutencoesDoLocal;
	}

	public void setManutencoesDoLocal(List<SolicitacaoManutencaoPredial> manutencoesDoLocal) {
		this.manutencoesDoLocal = manutencoesDoLocal;
	}

	public SolicitacaoManutencaoPredial getManutencaoSelecionada() {
		return manutencaoSelecionada;
	}

	public void setManutencaoSelecionada(SolicitacaoManutencaoPredial manutencaoSelecionada) {
		this.manutencaoSelecionada = manutencaoSelecionada;
	}

	/**
	 * Rótulo de status exibido no histórico de manutenções do local.
	 */
	public String getStatusLabelManutencao(SolicitacaoManutencaoPredial m) {
		return obterStatusManutencao(m).getLabel();
	}

	/**
	 * Classe CSS do badge de status exibido no histórico de manutenções do
	 * local.
	 */
	public String getStatusClasseManutencao(SolicitacaoManutencaoPredial m) {
		return obterStatusManutencao(m).getClasseCss();
	}

	public int getTotalManutencoes() {
		return manutencoesDoLocal != null ? manutencoesDoLocal.size() : 0;
	}

	/**
	 * Total de manutenções não canceladas do local selecionado.
	 */
	public long getManutencoesAtivas() {
		if (manutencoesDoLocal == null) {
			return 0;
		}
		return manutencoesDoLocal.stream().filter(m -> !m.isCancelada()).count();
	}

	public long getManutencoesCanceladas() {
		if (manutencoesDoLocal == null) {
			return 0;
		}
		return manutencoesDoLocal.stream().filter(SolicitacaoManutencaoPredial::isCancelada).count();
	}

	/**
	 * Referência (id + tipo) usada para abrir o diálogo de detalhes,
	 * reaproveitando prepararDetalhes(Map) do TodasSolicitacoesBean — o mesmo
	 * diálogo usado em "Gerenciar Manutenção", "Cronograma de Preventivas" e
	 * "Todas as Solicitações".
	 */
	public Map<String, Object> getDetalhesRef(SolicitacaoManutencaoPredial manutencao) {
		Map<String, Object> ref = new HashMap<>();
		ref.put("id", manutencao.getId());
		ref.put("tipo", "PREDIAL");
		return ref;
	}
}
