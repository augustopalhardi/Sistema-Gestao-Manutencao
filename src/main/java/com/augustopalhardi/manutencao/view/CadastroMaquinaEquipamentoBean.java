package com.augustopalhardi.manutencao.view;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.model.file.UploadedFile;
import org.primefaces.shaded.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.ManterCadastroMaquinaEquipamentoService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.util.Uploads;

@Named
@ViewScoped
public class CadastroMaquinaEquipamentoBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CadastroMaquinaEquipamentoBean.class);

	private static final String UPLOAD_DIRECTORY = Uploads.diretorio(Uploads.MAQUINAS);

	@Inject
	private ManterCadastroMaquinaEquipamentoService manterCadastroMaquinaEquipamentoService;

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterSolicitacaoManutencaoMaquinaService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusManutencaoService;

	// Dialog GLOBAL de imagem
	@Inject
	private VisualizarImagemBean visualizarImagemBean;

	private List<CadastroMaquinaEquipamento> cadastroMaquinasEquipamentos = new ArrayList<>();
	private List<CadastroMaquinaEquipamento> cadastroMaquinasEquipamentosFiltrados = new ArrayList<>();
	private UploadedFile uploadedPhoto;

	private String numeroPatrimonio;
	private String fabricante;
	private String modelo;
	private String tipo;
	private Date anoFabricacao;
	private String potencia;
	private String lubrificacao;
	private String localizacao;
	private String observacoes;

	// Filtro de status: "TODOS", "ATIVO", "INATIVO"
	private String filtroStatus = "TODOS";

	// Filtro de status da TELA DE CONSULTA (maquinaEquipamentoTecnico.xhtml).
	// Independente do filtro da tela de cadastro. Inicia em "ATIVO" para
	// que a consulta abra já mostrando somente os equipamentos ativos.
	private String filtroStatusTecnico = "ATIVO";

	private CadastroMaquinaEquipamento equipamentoSelecionado;

	// Lista de manutenções do equipamento selecionado
	private List<SolicitacaoManutencaoMaquina> manutencoesDoEquipamento = new ArrayList<>();

	// Manutenção selecionada para ver detalhes
	private SolicitacaoManutencaoMaquina manutencaoSelecionada;

	// Cache de status das manutenções do equipamento selecionado, para
	// evitar uma consulta ao banco por linha (e por comparação de
	// ordenação) toda vez que o histórico é exibido.
	private Map<Long, ControleStatusManutencao> cacheControlesEquipamento = new HashMap<>();

	@PostConstruct
	public void inicializar() {
		prepararNovo();
		cadastroMaquinasEquipamentos = manterCadastroMaquinaEquipamentoService.buscarTodos();
		aplicarFiltroStatus();
		LOGGER.info("Bean de CadastroMaquinaEquipamento inicializado com sucesso");
	}

	public String prepararNovo() {
		numeroPatrimonio = null;
		fabricante = null;
		modelo = null;
		tipo = null;
		anoFabricacao = null;
		potencia = null;
		lubrificacao = null;
		localizacao = null;
		observacoes = null;
		uploadedPhoto = null;
		equipamentoSelecionado = null;
		manutencoesDoEquipamento = new ArrayList<>();
		LOGGER.info("Preparando novo cadastro de equipamento");
		return null;
	}

	public String prepararEdicao(CadastroMaquinaEquipamento equipamento) {
		this.equipamentoSelecionado = equipamento;
		this.numeroPatrimonio = equipamento.getNumeroPatrimonio();
		this.fabricante = equipamento.getFabricante();
		this.modelo = equipamento.getModelo();
		this.tipo = equipamento.getTipo();
		this.anoFabricacao = equipamento.getAnoFabricacao();
		this.potencia = equipamento.getPotencia();
		this.lubrificacao = equipamento.getLubrificacao();
		this.localizacao = equipamento.getLocalizacao();
		this.observacoes = equipamento.getObservacoes();
		this.uploadedPhoto = null;
		LOGGER.info("Preparando edição do equipamento ID: {}, Patrimônio: {}", equipamento.getId(),
				equipamento.getNumeroPatrimonio());
		return null;
	}

	// ==========================================================
	// SUGESTÃO DE TIPOS JÁ CADASTRADOS
	// ==========================================================

	/**
	 * Normaliza um texto para comparação: remove acentos, espaços das pontas e
	 * diferenças de maiúsculas/minúsculas.
	 *
	 * Necessário porque "tipo" é texto livre: sem isso, a lista de sugestões
	 * mostraria "Torno", "torno" e "TORNO" como três opções diferentes.
	 */
	private static String chaveComparacao(String texto) {
		if (texto == null) {
			return "";
		}
		String semAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcentos.toUpperCase(Locale.ROOT);
	}

	/**
	 * Alimenta o autocomplete do campo "Tipo de Equipamento" com os tipos já
	 * existentes no cadastro.
	 *
	 * O campo continua sendo texto livre: o autocomplete é apenas uma ajuda
	 * para reaproveitar um tipo existente em vez de digitar uma variação nova.
	 * Quando o usuário clica no botão da lista, a consulta chega vazia e todos
	 * os tipos são devolvidos.
	 *
	 * A lista sai em ordem alfabética e sem repetir variações de escrita — o
	 * rótulo exibido é a primeira grafia encontrada no cadastro.
	 */
	public List<String> completarTipo(String query) {
		List<String> sugestoes = new ArrayList<>();

		if (cadastroMaquinasEquipamentos == null) {
			return sugestoes;
		}

		String termo = chaveComparacao(query);

		// TreeMap para já sair em ordem alfabética pela chave normalizada
		Map<String, String> tiposPorChave = new TreeMap<>();

		for (CadastroMaquinaEquipamento equipamento : cadastroMaquinasEquipamentos) {
			String tipoCadastrado = equipamento.getTipo();

			if (tipoCadastrado == null || tipoCadastrado.trim().isEmpty()) {
				continue;
			}

			String chave = chaveComparacao(tipoCadastrado);

			if (!termo.isEmpty() && !chave.contains(termo)) {
				continue;
			}

			tiposPorChave.putIfAbsent(chave, tipoCadastrado.trim());
		}

		sugestoes.addAll(tiposPorChave.values());
		return sugestoes;
	}

	/**
	 * Alinha o tipo digitado com a grafia que já existe no cadastro.
	 *
	 * Se o usuário digitar "TORNO" e o banco já tiver "Torno", grava "Torno".
	 * Sem isso, cada variação de escrita vira uma categoria a mais na tela de
	 * solicitação de manutenção — e as duas parecem idênticas na tabela, o que
	 * torna o problema difícil de perceber.
	 *
	 * O próprio registro em edição é ignorado na comparação: quem estiver
	 * editando o ÚNICO equipamento de um tipo continua conseguindo corrigir a
	 * grafia dele, em vez de ver o valor voltar sozinho.
	 *
	 * @param tipoInformado o que veio da tela
	 * @param idEmEdicao    id do equipamento sendo editado, ou null se for novo
	 * @return a grafia já existente, ou o texto informado quando o tipo é novo
	 */
	private String canonizarTipo(String tipoInformado, Long idEmEdicao) {
		if (tipoInformado == null) {
			return null;
		}

		String limpo = tipoInformado.trim();

		if (limpo.isEmpty() || cadastroMaquinasEquipamentos == null) {
			return limpo;
		}

		String chave = chaveComparacao(limpo);

		for (CadastroMaquinaEquipamento equipamento : cadastroMaquinasEquipamentos) {
			if (idEmEdicao != null && idEmEdicao.equals(equipamento.getId())) {
				continue;
			}

			String tipoCadastrado = equipamento.getTipo();

			if (tipoCadastrado == null || tipoCadastrado.trim().isEmpty()) {
				continue;
			}

			if (chaveComparacao(tipoCadastrado).equals(chave)) {
				String canonico = tipoCadastrado.trim();

				if (!canonico.equals(limpo)) {
					LOGGER.info("Tipo '{}' alinhado com o já cadastrado '{}'", limpo, canonico);

					// Avisa em vez de trocar em silêncio: a pessoa digitou uma
					// coisa e vai ver outra gravada na tabela.
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_INFO, "Tipo ajustado",
									"O tipo \"" + limpo + "\" foi gravado como \"" + canonico
											+ "\", que já existe no cadastro."));
				}

				return canonico;
			}
		}

		return limpo;
	}

	/**
	 * Carrega todas as manutenções do equipamento selecionado
	 */
	public void carregarManutencoes(CadastroMaquinaEquipamento equipamento) {
		try {
			this.equipamentoSelecionado = equipamento;
			this.manutencoesDoEquipamento = manterSolicitacaoManutencaoMaquinaService.buscarPorMaquina(equipamento);
			carregarCacheControlesEquipamento();
			this.manutencoesDoEquipamento
					.sort(Comparator.comparingInt(this::ordemStatusManutencao));
			LOGGER.info("Carregadas {} manutenções para o equipamento ID: {}", manutencoesDoEquipamento.size(),
					equipamento.getId());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar manutenções do equipamento: ", e);
			this.manutencoesDoEquipamento = new ArrayList<>();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar histórico de manutenções"));
		}
	}

	/**
	 * Carrega, em uma única consulta, os status de todas as manutenções de
	 * máquina — evitando uma consulta por linha (e novamente durante a
	 * ordenação) toda vez que o histórico de um equipamento é exibido.
	 */
	private void carregarCacheControlesEquipamento() {
		cacheControlesEquipamento = new HashMap<>();
		try {
			for (ControleStatusManutencao c : manterControleStatusManutencaoService.buscarTodos()) {
				if (c.getTipoManutencao() == TipoManutencaoReferencia.MAQUINA) {
					cacheControlesEquipamento.put(c.getSolicitacaoId(), c);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar cache de status de manutenção de máquina: {}", e.getMessage());
		}
	}

	/**
	 * Determina o status real da manutenção (Em Andamento, Em Aberto, Concluída
	 * ou Cancelada), consultando o cache de status em memória.
	 */
	private ControleStatusManutencao.StatusManutencao obterStatusManutencao(SolicitacaoManutencaoMaquina m) {
		if (m.isCancelada()) {
			return ControleStatusManutencao.StatusManutencao.CANCELADA;
		}
		try {
			ControleStatusManutencao controle = cacheControlesEquipamento.get(m.getId());
			if (controle != null && controle.getStatus() != null) {
				return controle.getStatus();
			}
		} catch (Exception e) {
			LOGGER.warn("Erro ao consultar status da manutencao MAQUINA ID {}: {}", m.getId(), e.getMessage());
		}
		return ControleStatusManutencao.StatusManutencao.PARA_FAZER;
	}

	/**
	 * Define a ordem de exibição no histórico: Em Andamento > Em Aberto >
	 * Concluída > Cancelada.
	 */
	private int ordemStatusManutencao(SolicitacaoManutencaoMaquina m) {
		switch (obterStatusManutencao(m)) {
		case EM_ANDAMENTO:
		case AGUARDANDO_COMPRA:
			return 0;
		case PARA_FAZER:
			return 1;
		case CONCLUIDA:
			return 2;
		case CANCELADA:
		default:
			return 3;
		}
	}

	/**
	 * Rótulo de status exibido no histórico de manutenções do equipamento.
	 */
	public String getStatusLabelManutencao(SolicitacaoManutencaoMaquina m) {
		switch (obterStatusManutencao(m)) {
		case EM_ANDAMENTO:
		case AGUARDANDO_COMPRA:
			return "Em Andamento";
		case PARA_FAZER:
			return "Em Aberto";
		case CONCLUIDA:
			return "Concluída";
		case CANCELADA:
		default:
			return "Cancelada";
		}
	}

	/**
	 * Classe CSS do badge de status exibido no histórico de manutenções do
	 * equipamento.
	 */
	public String getStatusClasseManutencao(SolicitacaoManutencaoMaquina m) {
		switch (obterStatusManutencao(m)) {
		case EM_ANDAMENTO:
		case AGUARDANDO_COMPRA:
			return "status-andamento";
		case PARA_FAZER:
			return "status-aberto";
		case CONCLUIDA:
			return "status-concluida";
		case CANCELADA:
		default:
			return "status-cancelada";
		}
	}

	/**
	 * Referência (id + tipo) usada para abrir o diálogo de detalhes,
	 * reaproveitando o método prepararDetalhes(Map) do TodasSolicitacoesBean
	 * — o mesmo diálogo usado nas páginas "Gerenciar Manutenção",
	 * "Cronograma de Preventivas" e "Todas as Solicitações".
	 */
	public Map<String, Object> getDetalhesRef(SolicitacaoManutencaoMaquina manutencao) {
		Map<String, Object> ref = new HashMap<>();
		ref.put("id", manutencao.getId());
		ref.put("tipo", "MÁQUINA");
		return ref;
	}

	/**
	 * Retorna o total de manutenções do equipamento selecionado
	 */
	public int getTotalManutencoes() {
		return manutencoesDoEquipamento != null ? manutencoesDoEquipamento.size() : 0;
	}

	/**
	 * Retorna o total de manutenções ativas (não canceladas)
	 */
	public long getManutencoesAtivas() {
		if (manutencoesDoEquipamento == null) {
			return 0;
		}
		return manutencoesDoEquipamento.stream().filter(m -> !m.isCancelada()).count();
	}

	/**
	 * Retorna o total de manutenções canceladas
	 */
	public long getManutencoesCanceladas() {
		if (manutencoesDoEquipamento == null) {
			return 0;
		}
		return manutencoesDoEquipamento.stream().filter(SolicitacaoManutencaoMaquina::isCancelada).count();
	}

	// Alternar status ativo/inativo do equipamento
	public void alternarStatus(CadastroMaquinaEquipamento equipamento) {
		try {
			String statusAnterior = equipamento.isAtivo() ? "Ativo" : "Inativo";
			manterCadastroMaquinaEquipamentoService.alternarStatus(equipamento);
			String statusNovo = equipamento.isAtivo() ? "Ativo" : "Inativo";

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
							"Equipamento " + equipamento.getNumeroPatrimonio() + " alterado de " + statusAnterior
									+ " para " + statusNovo));

			LOGGER.info("Status do equipamento ID: {} alterado de {} para {}", equipamento.getId(), statusAnterior,
					statusNovo);

			// Recarrega a lista e reaplica filtro
			cadastroMaquinasEquipamentos = manterCadastroMaquinaEquipamentoService.buscarTodos();
			aplicarFiltroStatus();

		} catch (Exception e) {
			LOGGER.error("Erro ao alternar status do equipamento: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao alterar status do equipamento"));
		}
	}

	// Aplicar filtro de status na lista
	public void aplicarFiltroStatus() {
		if ("ATIVO".equals(filtroStatus)) {
			cadastroMaquinasEquipamentosFiltrados = new ArrayList<>();
			for (CadastroMaquinaEquipamento eq : cadastroMaquinasEquipamentos) {
				if (eq.getAtivo() == null || eq.getAtivo()) {
					cadastroMaquinasEquipamentosFiltrados.add(eq);
				}
			}
		} else if ("INATIVO".equals(filtroStatus)) {
			cadastroMaquinasEquipamentosFiltrados = new ArrayList<>();
			for (CadastroMaquinaEquipamento eq : cadastroMaquinasEquipamentos) {
				if (eq.getAtivo() != null && !eq.getAtivo()) {
					cadastroMaquinasEquipamentosFiltrados.add(eq);
				}
			}
		} else {
			cadastroMaquinasEquipamentosFiltrados = new ArrayList<>(cadastroMaquinasEquipamentos);
		}
		LOGGER.info("Filtro '{}' aplicado. Exibindo {} de {} equipamentos", filtroStatus,
				cadastroMaquinasEquipamentosFiltrados.size(), cadastroMaquinasEquipamentos.size());
	}

	// Chamado pelo selectOneMenu ao trocar o filtro
	public void filtrarPorStatus() {
		aplicarFiltroStatus();
	}

	public void salvar() {
		try {
			if (numeroPatrimonio == null || numeroPatrimonio.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Número de Patrimônio é obrigatório!"));
				return;
			}

			// Normaliza para maiúsculas: o campo é unique no banco e o
			// text-transform da tela é apenas visual, então "a0001" e "A0001"
			// seriam gravados como registros distintos.
			String patrimonioNormalizado = numeroPatrimonio.trim().toUpperCase();

			Long idExcluir = (equipamentoSelecionado != null) ? equipamentoSelecionado.getId() : null;

			if (manterCadastroMaquinaEquipamentoService.existeNumeroPatrimonio(patrimonioNormalizado, idExcluir)) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Número de Patrimônio já cadastrado!"));
				return;
			}

			CadastroMaquinaEquipamento equipamento;
			if (equipamentoSelecionado != null) {
				equipamento = equipamentoSelecionado;
				LOGGER.info("Atualizando equipamento existente ID: {}", equipamento.getId());
			} else {
				equipamento = new CadastroMaquinaEquipamento();
				LOGGER.info("Criando novo equipamento");
			}

			equipamento.setNumeroPatrimonio(patrimonioNormalizado);
			equipamento.setFabricante(fabricante);
			equipamento.setModelo(modelo);

			// Limpa os espaços das pontas e alinha com a grafia já
			// cadastrada, para não criar variações do mesmo tipo — cada uma
			// delas vira uma categoria a mais na tela de solicitação.
			equipamento.setTipo(canonizarTipo(tipo, idExcluir));

			equipamento.setAnoFabricacao(anoFabricacao);
			equipamento.setPotencia(potencia);
			equipamento.setLubrificacao(lubrificacao);
			equipamento.setLocalizacao(localizacao);
			equipamento.setObservacoes(observacoes);

			if (uploadedPhoto != null && uploadedPhoto.getFileName() != null
					&& !uploadedPhoto.getFileName().trim().isEmpty() && uploadedPhoto.getSize() > 0) {
				try {
					String nomeArquivo = salvarFoto(uploadedPhoto);
					equipamento.setUrlFoto(nomeArquivo);
					LOGGER.info("Foto salva: {}", nomeArquivo);
				} catch (Exception e) {
					LOGGER.error("Erro ao fazer upload da foto: ", e);
				}
			}

			equipamento = manterCadastroMaquinaEquipamentoService.salvar(equipamento);

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso!", "Equipamento salvo com sucesso!"));

			LOGGER.info("Equipamento salvo: {} - PAT: {}", equipamento.getId(), equipamento.getNumeroPatrimonio());

			prepararNovo();
			cadastroMaquinasEquipamentos = manterCadastroMaquinaEquipamentoService.buscarTodos();
			aplicarFiltroStatus();

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar equipamento: ", e);
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
					"Erro ao salvar equipamento: " + e.getMessage()));
		}
	}

	private String salvarFoto(UploadedFile foto) throws IOException {
		try {
			File uploadDir = new File(UPLOAD_DIRECTORY);
			if (!uploadDir.exists()) {
				boolean created = uploadDir.mkdirs();
				LOGGER.info("Diretório criado: {} - Sucesso: {}", UPLOAD_DIRECTORY, created);
			}

			String nomeOriginal = foto.getFileName();
			String extensao = FilenameUtils.getExtension(nomeOriginal);

			String nomeArquivo = System.currentTimeMillis() + "." + extensao;

			String caminhoCompleto = UPLOAD_DIRECTORY + nomeArquivo;
			File destino = new File(caminhoCompleto);

			try (InputStream input = foto.getInputStream(); OutputStream output = new FileOutputStream(destino)) {

				byte[] buffer = new byte[1024];
				int length;
				while ((length = input.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
			}

			LOGGER.info("Foto salva com sucesso em: {}", caminhoCompleto);

			return nomeArquivo;

		} catch (IOException e) {
			LOGGER.error("Erro ao salvar foto: ", e);
			throw new IOException("Não foi possível salvar a foto.", e);
		}
	}

	public void excluir(CadastroMaquinaEquipamento equipamento) {
		try {
			manterCadastroMaquinaEquipamentoService.excluir(equipamento);
			cadastroMaquinasEquipamentos.remove(equipamento);
			aplicarFiltroStatus();

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Equipamento excluído com sucesso!"));

			LOGGER.info("Equipamento excluído: {}", equipamento.getId());

		} catch (Exception e) {
			LOGGER.error("Erro ao excluir equipamento: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao excluir equipamento"));
		}
	}

	public void limpar() {
		prepararNovo();
		LOGGER.info("Formulário limpo");
	}

	// ========== GETTERS E SETTERS ==========

	public List<CadastroMaquinaEquipamento> getCadastroMaquinasEquipamentos() {
		return cadastroMaquinasEquipamentos;
	}

	public void setCadastroMaquinasEquipamentos(List<CadastroMaquinaEquipamento> cadastroMaquinasEquipamentos) {
		this.cadastroMaquinasEquipamentos = cadastroMaquinasEquipamentos;
	}

	public List<CadastroMaquinaEquipamento> getCadastroMaquinasEquipamentosFiltrados() {
		return cadastroMaquinasEquipamentosFiltrados;
	}

	public void setCadastroMaquinasEquipamentosFiltrados(
			List<CadastroMaquinaEquipamento> cadastroMaquinasEquipamentosFiltrados) {
		this.cadastroMaquinasEquipamentosFiltrados = cadastroMaquinasEquipamentosFiltrados;
	}

	public String getFiltroStatus() {
		return filtroStatus;
	}

	public void setFiltroStatus(String filtroStatus) {
		this.filtroStatus = filtroStatus;
	}

	// ==========================================
	// CONSULTA (maquinaEquipamentoTecnico.xhtml) - FILTRO ATIVO/INATIVO
	// ==========================================

	public String getFiltroStatusTecnico() {
		return filtroStatusTecnico;
	}

	public void setFiltroStatusTecnico(String filtroStatusTecnico) {
		this.filtroStatusTecnico = filtroStatusTecnico;
	}

	/**
	 * Prepara o dialog GLOBAL de imagem com a foto do equipamento.
	 * A marcação do dialog fica em /includes/visualizarImagemDialog.xhtml.
	 */
	public void verImagem(CadastroMaquinaEquipamento equipamento) {
		this.equipamentoSelecionado = equipamento;

		if (equipamento == null) {
			visualizarImagemBean.abrir("Foto do Equipamento", null);
			return;
		}

		// PADRONIZADO: identificação da máquina vem de getIdentificacao(),
		// o formato oficial do sistema ("tipo - fabricante modelo"). Antes
		// aqui havia mais uma concatenação própria, com formato diferente do
		// resto das telas.
		String descricao = equipamento.getIdentificacao();

		visualizarImagemBean.abrir("Foto do Equipamento", descricao,
				"maquinas_equipamentos/" + equipamento.getUrlFoto());

		visualizarImagemBean.setMensagemSemFoto("Nenhuma foto anexada a este equipamento.");
		visualizarImagemBean.addInfo("ID", equipamento.getId());
		visualizarImagemBean.addInfo("Nº Patrimônio", equipamento.getNumeroPatrimonio());
		visualizarImagemBean.addInfo("Fabricante", equipamento.getFabricante());
		visualizarImagemBean.addInfo("Modelo", equipamento.getModelo());
		visualizarImagemBean.addInfo("Tipo", equipamento.getTipo());
		visualizarImagemBean.addInfo("Status", isRegistroAtivo(equipamento) ? "Ativo" : "Inativo");
	}

	/**
	 * Critério ÚNICO de "ativo" usado tanto pelo filtro quanto pela coluna
	 * Status da tela de consulta.
	 *
	 * Existe porque a entidade expõe getAtivo() (Boolean) e isAtivo()
	 * (boolean). O EL resolve "#{equipamento.ativo}" para isAtivo(), que
	 * devolve FALSE quando o campo é null, enquanto getAtivo() devolve
	 * null. Isso fazia a coluna mostrar "Inativo" e o filtro considerar
	 * "Ativo" o mesmo registro. Aqui null é tratado como ATIVO, mesmo
	 * default da entidade (private Boolean ativo = true).
	 */
	public boolean isRegistroAtivo(CadastroMaquinaEquipamento equipamento) {
		return equipamento == null || equipamento.getAtivo() == null || equipamento.getAtivo();
	}

	/**
	 * Lista exibida na tela de consulta de máquinas e equipamentos
	 * (maquinaEquipamentoTecnico.xhtml), já aplicando o filtro de status
	 * escolhido na coluna "Status".
	 */
	public List<CadastroMaquinaEquipamento> getEquipamentosTecnico() {
		List<CadastroMaquinaEquipamento> resultado = new ArrayList<>();

		if (cadastroMaquinasEquipamentos == null) {
			return resultado;
		}

		for (CadastroMaquinaEquipamento eq : cadastroMaquinasEquipamentos) {
			boolean ativo = isRegistroAtivo(eq);

			if ("ATIVO".equals(filtroStatusTecnico) && !ativo) {
				continue;
			}
			if ("INATIVO".equals(filtroStatusTecnico) && ativo) {
				continue;
			}
			resultado.add(eq);
		}

		return resultado;
	}

	public UploadedFile getUploadedPhoto() {
		return uploadedPhoto;
	}

	public void setUploadedPhoto(UploadedFile uploadedPhoto) {
		this.uploadedPhoto = uploadedPhoto;
	}

	public String getNumeroPatrimonio() {
		return numeroPatrimonio;
	}

	public void setNumeroPatrimonio(String numeroPatrimonio) {
		this.numeroPatrimonio = numeroPatrimonio;
	}

	public String getFabricante() {
		return fabricante;
	}

	public void setFabricante(String fabricante) {
		this.fabricante = fabricante;
	}

	public String getModelo() {
		return modelo;
	}

	public void setModelo(String modelo) {
		this.modelo = modelo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Date getAnoFabricacao() {
		return anoFabricacao;
	}

	public void setAnoFabricacao(Date anoFabricacao) {
		this.anoFabricacao = anoFabricacao;
	}

	public String getPotencia() {
		return potencia;
	}

	public void setPotencia(String potencia) {
		this.potencia = potencia;
	}

	public String getLubrificacao() {
		return lubrificacao;
	}

	public void setLubrificacao(String lubrificacao) {
		this.lubrificacao = lubrificacao;
	}

	public String getLocalizacao() {
		return localizacao;
	}

	public void setLocalizacao(String localizacao) {
		this.localizacao = localizacao;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public CadastroMaquinaEquipamento getEquipamentoSelecionado() {
		return equipamentoSelecionado;
	}

	public void setEquipamentoSelecionado(CadastroMaquinaEquipamento equipamentoSelecionado) {
		this.equipamentoSelecionado = equipamentoSelecionado;
	}

	public List<SolicitacaoManutencaoMaquina> getManutencoesDoEquipamento() {
		return manutencoesDoEquipamento;
	}

	public void setManutencoesDoEquipamento(List<SolicitacaoManutencaoMaquina> manutencoesDoEquipamento) {
		this.manutencoesDoEquipamento = manutencoesDoEquipamento;
	}

	public SolicitacaoManutencaoMaquina getManutencaoSelecionada() {
		return manutencaoSelecionada;
	}

	public void setManutencaoSelecionada(SolicitacaoManutencaoMaquina manutencaoSelecionada) {
		this.manutencaoSelecionada = manutencaoSelecionada;
	}
}