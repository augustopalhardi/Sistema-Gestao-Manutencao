package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos.StatusSolicitacao;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos.TipoSolicitacao;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoUsuario;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.service.ManterCadastroUsuarioService;
import com.augustopalhardi.manutencao.service.ManterControleStatusManutencaoService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoCompraMateriaisServicosService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoMaquinaService;
import com.augustopalhardi.manutencao.service.ManterSolicitacaoManutencaoPredialService;

@Named
@ViewScoped
public class SolicitacaoCompraMateriaisServicosBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(SolicitacaoCompraMateriaisServicosBean.class);

	@Inject
	private ManterCadastroUsuarioService manterCadastroUsuarioService;

	@Inject
	private ManterSolicitacaoCompraMateriaisServicosService manterSolicitacaoCompraService;

	@Inject
	private ManterControleStatusManutencaoService manterControleStatusManutencaoService;

	@Inject
	private ManterSolicitacaoManutencaoMaquinaService manterSolicitacaoManutencaoMaquinaService;

	@Inject
	private ManterSolicitacaoManutencaoPredialService manterSolicitacaoManutencaoPredialService;

	// Listas
	private List<SolicitacaoCompraMateriaisServicos> solicitacoesPendentes = new ArrayList<>();
	private List<SolicitacaoCompraMateriaisServicos> todasSolicitacoes = new ArrayList<>();
	private List<CadastroUsuario> administradoresDisponiveis = new ArrayList<>();

	// Objetos para processamento
	private SolicitacaoCompraMateriaisServicos solicitacaoSelecionada;
	private Double valorGasto;
	private Double valorGastoMaterial;
	private Double valorGastoServico;
	private String observacoesAdministrador;
	private Long administradorProcessouId;

	// Controles da interface
	private String filtroStatus = "PENDENTE";

	/**
	 * CACHE DA LISTA FILTRADA — FONTE ÚNICA DA TELA.
	 *
	 * Guarda o resultado dos filtros (exceto o de status) já aplicados sobre
	 * todasSolicitacoes. Tanto a lista de cards quanto os contadores das abas
	 * saem daqui, então é impossível a lista mostrar uma coisa e o número
	 * mostrar outra.
	 *
	 * Antes, os contadores vinham de COUNT direto no banco, sem filtro nenhum:
	 * filtrar por "Predial" deixava a lista com 3 itens e o badge com 47.
	 *
	 * O cache vale por REQUISIÇÃO. O FacesContext é criado um por requisição,
	 * então guardar a instância usada e comparar por identidade é uma forma
	 * barata de detectar que já estamos em uma requisição nova e que a lista
	 * precisa ser remontada. Sem isso, o getter era recalculado a cada chamada
	 * do EL — e a página chama várias vezes por render (o ui:repeat e o
	 * rendered do empty-state, no mínimo).
	 *
	 * Os dois campos são transient porque FacesContext não é serializável e a
	 * lista pode ser reconstruída a qualquer momento.
	 */
	private transient FacesContext contextoDoCache;
	private transient List<SolicitacaoCompraMateriaisServicos> cacheFiltradasSemStatus;

	// Filtros de busca (abas/barra de filtro no topo da tela)
	private String filtroBuscaId = "";
	private String filtroCategoria = "";
	private String filtroTipoSolicitacao = "";
	private Date filtroDataInicio;
	private Date filtroDataFim;

	// Campos para o dialog de cancelamento de solicitação PENDENTE
	private String motivoCancelamento;
	private Long administradorCancelamentoId;

	// Campos para o dialog de cancelamento de compra JÁ PROCESSADA
	private String motivoCancelamentoProcessada;
	private Long administradorCancelamentoProcessadaId;

	@PostConstruct
	public void inicializar() {
		try {
			carregarAdministradores();
			atualizarListas();
			LOGGER.info("Bean SolicitacaoCompraMateriaisServicos inicializado com sucesso");
		} catch (Exception e) {
			LOGGER.error("Erro ao inicializar bean: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao carregar dados", e.getMessage()));
		}
	}

	private void carregarAdministradores() {
		try {
			administradoresDisponiveis = manterCadastroUsuarioService.buscarTodos().stream()
					.filter(usuario -> TipoUsuario.ADMINISTRADOR.equals(usuario.getTipoUsuario()))
					.collect(Collectors.toList());
			LOGGER.info("Administradores carregados: {}", administradoresDisponiveis.size());
		} catch (Exception e) {
			LOGGER.warn("Erro ao carregar administradores: {}", e.getMessage());
			administradoresDisponiveis = new ArrayList<>();
		}
	}

	public void carregarSolicitacoesPendentes() {
		try {
			solicitacoesPendentes = manterSolicitacaoCompraService.buscarPendentes();
			LOGGER.info("Carregadas {} solicitações pendentes", solicitacoesPendentes.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar solicitações pendentes: ", e);
		}
	}

	public void carregarTodasSolicitacoes() {
		try {
			todasSolicitacoes = manterSolicitacaoCompraService.buscarTodas();
			LOGGER.info("Carregadas {} solicitações no total", todasSolicitacoes.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar todas as solicitações: ", e);
		}
	}

	public void atualizarListas() {
		try {
			carregarSolicitacoesPendentes();
			carregarTodasSolicitacoes();

			// Os dados do banco mudaram: o cache da lista filtrada não vale
			// mais e precisa ser remontado.
			invalidarCache();

			LOGGER.info("Listas atualizadas - Pendentes: {}, Total: {}", solicitacoesPendentes.size(),
					todasSolicitacoes.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao atualizar listas: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao atualizar dados", e.getMessage()));
		}
	}

	/** Descarta o cache da lista filtrada, forçando o recálculo. */
	private void invalidarCache() {
		this.contextoDoCache = null;
		this.cacheFiltradasSemStatus = null;
	}

	public String getMaquinaOuLocal(SolicitacaoCompraMateriaisServicos solicitacao) {
		if (solicitacao == null) {
			return "N/A";
		}

		try {
			if (solicitacao
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {

				SolicitacaoManutencaoMaquina manutencaoMaquina = manterSolicitacaoManutencaoMaquinaService
						.buscarPorId(solicitacao.getSolicitacaoManutencaoId());

				if (manutencaoMaquina != null && manutencaoMaquina.getMaquina() != null) {
					CadastroMaquinaEquipamento maquina = manutencaoMaquina.getMaquina();
					// PADRONIZADO: identificação da máquina agora vem de getIdentificacao()
					// O formato oficial ("tipo - fabricante modelo") é definido em um lugar só,
					// na entidade CadastroMaquinaEquipamento.
					return maquina.getIdentificacao();
				}

			} else if (solicitacao
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.PREDIAL) {

				SolicitacaoManutencaoPredial manutencaoPredial = manterSolicitacaoManutencaoPredialService
						.buscarPorId(solicitacao.getSolicitacaoManutencaoId());

				if (manutencaoPredial != null && manutencaoPredial.getLocal() != null) {
					return manutencaoPredial.getLocal().getNome();
				}
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao buscar máquina/local da manutenção: ", e);
		}

		return "N/A";
	}

	public String getMaquinaOuLocalFormatado(SolicitacaoCompraMateriaisServicos solicitacao) {
		if (solicitacao == null) {
			return "N/A";
		}

		String nomeEquipamento = getMaquinaOuLocal(solicitacao);

		if (solicitacao
				.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA) {
			// PADRONIZADO: prefixo escrito com acento ("Máquina: "), como no
			// restante do sistema — o rótulo é o mesmo em todas as telas.
			return "Máquina: " + nomeEquipamento;
		} else {
			return "Local: " + nomeEquipamento;
		}
	}

	/**
	 * Processa a compra (marca como COMPRADO) e LIBERA a manutenção Este é o método
	 * chamado pelo botão "Processar Compra" no XHTML
	 */
	public void processarCompra() {
		try {
			LOGGER.info("Processando compra - Solicitação ID: {}",
					solicitacaoSelecionada != null ? solicitacaoSelecionada.getId() : "null");

			if (solicitacaoSelecionada == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma solicitação selecionada"));
				return;
			}

			// Validar administrador
			if (administradorProcessouId == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Selecione o administrador responsável"));
				return;
			}

			CadastroUsuario adminProcessou = buscarAdministradorPorId(administradorProcessouId);
			if (adminProcessou == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Administrador selecionado não encontrado"));
				return;
			}

			// Validar valores conforme o tipo de solicitação
			if (solicitacaoSelecionada.getTipoSolicitacao() == TipoSolicitacao.AMBOS) {
				// Para tipo AMBOS, exige os dois valores separados
				if (valorGastoMaterial == null || valorGastoMaterial <= 0) {
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Informe o valor gasto com material"));
					return;
				}
				if (valorGastoServico == null || valorGastoServico <= 0) {
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Informe o valor gasto com serviço"));
					return;
				}
				// Calcula o valor total como soma dos dois
				valorGasto = valorGastoMaterial + valorGastoServico;
			} else {
				// Para tipo MATERIAL ou SERVICO, campo único
				if (valorGasto == null || valorGasto <= 0) {
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Informe o valor gasto na compra"));
					return;
				}
				// Limpa os campos separados pois não se aplicam
				valorGastoMaterial = null;
				valorGastoServico = null;
			}

			LOGGER.info("Processando compra: Solicitação={}, Valor={}, ValorMaterial={}, ValorServico={}, Admin={}",
					solicitacaoSelecionada.getId(), valorGasto, valorGastoMaterial, valorGastoServico,
					adminProcessou.getNome());

			// Processar a compra usando o service
			SolicitacaoCompraMateriaisServicos solicitacaoAtualizada = manterSolicitacaoCompraService.processarCompra(
					solicitacaoSelecionada.getId(), adminProcessou, valorGasto, valorGastoMaterial, valorGastoServico,
					observacoesAdministrador != null ? observacoesAdministrador.trim() : "");

			if (solicitacaoAtualizada != null) {
				LOGGER.info("Compra processada - Status: {}", solicitacaoAtualizada.getStatusSolicitacao());

				// Liberar a manutenção - Voltar para EM_ANDAMENTO
				liberarManutencaoParaAndamento(solicitacaoAtualizada);

				String mensagemValor;
				if (solicitacaoSelecionada.getTipoSolicitacao() == TipoSolicitacao.AMBOS) {
					mensagemValor = "Valor total: R$ " + String.format("%.2f", valorGasto)
							+ " (Material: R$ " + String.format("%.2f", valorGastoMaterial)
							+ " / Serviço: R$ " + String.format("%.2f", valorGastoServico) + ")";
				} else {
					mensagemValor = "Valor gasto: R$ " + String.format("%.2f", valorGasto);
				}

				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_INFO, "Compra Processada com Sucesso!",
								"A manutenção foi liberada e o técnico pode continuar o trabalho. " + mensagemValor));

				LOGGER.info("Compra processada com sucesso - Solicitação: {}, Valor: R$ {}, Admin: {}",
						solicitacaoAtualizada.getId(), valorGasto, adminProcessou.getNome());

				// Limpar campos
				limparCamposProcessamento();

				// Recarregar listas
				atualizarListas();

			} else {
				LOGGER.error("Falha ao processar compra - retorno null do service");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Falha ao processar compra"));
			}

		} catch (Exception e) {
			LOGGER.error("Erro ao processar compra: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao processar: " + e.getMessage()));
		}
	}

	/**
	 * Libera a manutenção de volta para EM_ANDAMENTO após compra processada ou
	 * cancelada
	 */
	private void liberarManutencaoParaAndamento(SolicitacaoCompraMateriaisServicos solicitacao) {
		try {
			if (solicitacao == null) {
				LOGGER.warn("Solicitação null, não é possível liberar manutenção");
				return;
			}

			// Determinar o tipo de manutenção
			TipoManutencaoReferencia tipoManutencao = solicitacao
					.getTipoManutencaoReferencia() == TipoManutencaoReferencia.MAQUINA
							? TipoManutencaoReferencia.MAQUINA
							: TipoManutencaoReferencia.PREDIAL;

			LOGGER.info("Liberando manutenção {} - Tipo: {}", solicitacao.getSolicitacaoManutencaoId(), tipoManutencao);

			// Atualizar status para EM_ANDAMENTO
			manterControleStatusManutencaoService.criarOuAtualizar(solicitacao.getSolicitacaoManutencaoId(),
					tipoManutencao, ControleStatusManutencao.StatusManutencao.EM_ANDAMENTO);

			LOGGER.info("Manutenção {} liberada - Status alterado para EM_ANDAMENTO",
					solicitacao.getSolicitacaoManutencaoId());

		} catch (Exception e) {
			LOGGER.error("Erro ao liberar manutenção para andamento: ", e);
			// Não interrompe o fluxo, mas loga o erro
		}
	}

	/**
	 * Cancela APENAS a solicitação de compra e volta a manutenção para PARA_FAZER.
	 * A manutenção NÃO é cancelada — só a compra é cancelada.
	 */
	public void cancelarSolicitacao() {
		try {
			LOGGER.info("Cancelando solicitação de compra ID: {}",
					solicitacaoSelecionada != null ? solicitacaoSelecionada.getId() : "null");

			if (solicitacaoSelecionada == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma solicitação selecionada"));
				return;
			}

			if (motivoCancelamento == null || motivoCancelamento.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Informe o motivo do cancelamento"));
				return;
			}

			if (administradorCancelamentoId == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Selecione o administrador responsável"));
				return;
			}

			CadastroUsuario adminProcessou = buscarAdministradorPorId(administradorCancelamentoId);
			if (adminProcessou == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro",
						"Administrador selecionado não encontrado"));
				return;
			}

			// Cancelar APENAS a solicitação de compra
			solicitacaoSelecionada.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
			solicitacaoSelecionada.setAdministradorProcessou(adminProcessou);
			solicitacaoSelecionada.setObservacoesAdministrador(motivoCancelamento.trim());
			solicitacaoSelecionada.setDataProcessamento(new Date());

			manterSolicitacaoCompraService.salvar(solicitacaoSelecionada);

			LOGGER.info("Solicitação de compra cancelada - Status: {}", solicitacaoSelecionada.getStatusSolicitacao());

			// Voltar a manutenção para EM_ANDAMENTO (NÃO cancela a manutenção)
			liberarManutencaoParaAndamento(solicitacaoSelecionada);

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Solicitação de Compra Cancelada",
							"Apenas a solicitação de compra foi cancelada. "
									+ "A manutenção voltou para 'Em Andamento' e o técnico pode "
									+ "fazer uma nova solicitação se necessário."));

			LOGGER.info("Solicitação de compra cancelada - ID: {}, Motivo: {}, Manutenção voltou para EM_ANDAMENTO",
					solicitacaoSelecionada.getId(), motivoCancelamento);

			limparCamposProcessamento();

			// Recarregar listas
			atualizarListas();

		} catch (Exception e) {
			LOGGER.error("Erro ao cancelar solicitação de compra: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao cancelar: " + e.getMessage()));
		}
	}

	/**
	 * Limpa campos do formulário de processamento
	 */
	private void limparCamposProcessamento() {
		this.solicitacaoSelecionada = null;
		this.valorGasto = null;
		this.valorGastoMaterial = null;
		this.valorGastoServico = null;
		this.observacoesAdministrador = null;
		this.administradorProcessouId = null;
		this.motivoCancelamento = null;
		this.administradorCancelamentoId = null;
		this.motivoCancelamentoProcessada = null;
		this.administradorCancelamentoProcessadaId = null;
		LOGGER.debug("Campos de processamento limpos");
	}

	/**
	 * Prepara o processamento de uma solicitação
	 */
	public void prepararProcessamento(SolicitacaoCompraMateriaisServicos solicitacao) {
		this.solicitacaoSelecionada = solicitacao;
		this.valorGasto = null;
		this.valorGastoMaterial = null;
		this.valorGastoServico = null;
		this.observacoesAdministrador = null;
		this.administradorProcessouId = null;
		LOGGER.debug("Preparando processamento da solicitação ID: {}", solicitacao.getId());
	}

	/**
	 * Prepara o cancelamento de uma solicitação
	 */
	public void prepararCancelamento(SolicitacaoCompraMateriaisServicos solicitacao) {
		this.solicitacaoSelecionada = solicitacao;
		this.motivoCancelamento = null;
		this.administradorCancelamentoId = null;
		LOGGER.debug("Preparando cancelamento da solicitação ID: {}", solicitacao.getId());
	}

	/**
	 * Prepara o cancelamento de uma compra JÁ PROCESSADA (status COMPRADO)
	 */
	public void prepararCancelamentoCompraProcessada(SolicitacaoCompraMateriaisServicos solicitacao) {
		this.solicitacaoSelecionada = solicitacao;
		this.motivoCancelamentoProcessada = null;
		this.administradorCancelamentoProcessadaId = null;
		LOGGER.debug("Preparando cancelamento de compra já processada - ID: {}", solicitacao.getId());
	}

	/**
	 * Confirma o cancelamento de uma compra JÁ PROCESSADA. O motivo é salvo em
	 * um histórico dedicado (HistoricoCancelamentoCompra), preservando os
	 * dados originais da compra (valor gasto, quem processou, quando).
	 */
	public void confirmarCancelamentoCompraProcessada() {
		try {
			LOGGER.info("Cancelando compra já processada - Solicitação ID: {}",
					solicitacaoSelecionada != null ? solicitacaoSelecionada.getId() : "null");

			if (solicitacaoSelecionada == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhuma solicitação selecionada"));
				return;
			}

			if (motivoCancelamentoProcessada == null || motivoCancelamentoProcessada.trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Erro", "Informe o motivo do cancelamento da compra"));
				return;
			}

			if (administradorCancelamentoProcessadaId == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Selecione o administrador responsável"));
				return;
			}

			CadastroUsuario adminCancelamento = buscarAdministradorPorId(administradorCancelamentoProcessadaId);
			if (adminCancelamento == null) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Erro", "Administrador selecionado não encontrado"));
				return;
			}

			manterSolicitacaoCompraService.cancelarCompraProcessada(solicitacaoSelecionada.getId(), adminCancelamento,
					motivoCancelamentoProcessada.trim());

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Compra Cancelada",
							"A compra já processada foi cancelada. O motivo e os dados da compra original "
									+ "foram registrados no histórico para consulta futura."));

			LOGGER.info("Compra já processada cancelada - Solicitação ID: {}, Motivo: {}",
					solicitacaoSelecionada.getId(), motivoCancelamentoProcessada);

			limparCamposProcessamento();
			atualizarListas();

		} catch (IllegalStateException e) {
			LOGGER.warn("Não foi possível cancelar a compra processada: {}", e.getMessage());
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
		} catch (Exception e) {
			LOGGER.error("Erro ao cancelar compra já processada: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao cancelar: " + e.getMessage()));
		}
	}

	/**
	 * Retorna classe CSS baseada no status
	 */
	public String getClasseStatus(SolicitacaoCompraMateriaisServicos.StatusSolicitacao status) {
		if (status == null) {
			return "status-unknown";
		}

		switch (status) {
		case PENDENTE:
			return "status-pendente";
		case COMPRADO:
			return "status-comprado";
		case CANCELADA:
			return "status-cancelada";
		default:
			return "status-unknown";
		}
	}

	/**
	 * Retorna ícone baseado no tipo de solicitação
	 */
	public String getIconeTipo(SolicitacaoCompraMateriaisServicos.TipoSolicitacao tipo) {
		if (tipo == null) {
			return "pi pi-question";
		}

		switch (tipo) {
		case MATERIAL:
			return "pi pi-box";
		case SERVICO:
			return "pi pi-wrench";
		case AMBOS:
			return "pi pi-shopping-cart";
		default:
			return "pi pi-question";
		}
	}

	/**
	 * FONTE ÚNICA DA TELA: aplica todos os filtros da barra superior, EXCETO
	 * o de status (que é a aba escolhida).
	 *
	 * Fica de fora o status de propósito: é a partir desta lista que os badges
	 * das abas são contados. Assim, filtrando por "Predial", o badge de
	 * Pendentes mostra quantas prediais estão pendentes — e não o total geral
	 * do banco, como acontecia antes.
	 *
	 * Trabalha SEMPRE sobre todasSolicitacoes, a lista carregada em
	 * atualizarListas(). Antes, cada aba consultava o banco por conta própria e
	 * a aba "Todas" usava esta lista em memória: três fontes diferentes, que
	 * podiam discordar entre si depois de processar ou cancelar uma compra.
	 */
	private List<SolicitacaoCompraMateriaisServicos> obterFiltradasSemStatus() {
		FacesContext contextoAtual = FacesContext.getCurrentInstance();

		// Cache válido apenas dentro da mesma requisição
		if (cacheFiltradasSemStatus != null && contextoDoCache == contextoAtual) {
			return cacheFiltradasSemStatus;
		}

		List<SolicitacaoCompraMateriaisServicos> filtradas;

		try {
			filtradas = new ArrayList<>(todasSolicitacoes != null ? todasSolicitacoes
					: new ArrayList<SolicitacaoCompraMateriaisServicos>());

			// Busca pelo ID da manutenção (máquina ou predial) relacionada à
			// compra. Continua sendo "contém": digitar 4 traz 4, 14, 40, 41.
			if (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty()) {
				String idBusca = filtroBuscaId.trim().replaceAll("[^0-9]", "");
				if (!idBusca.isEmpty()) {
					filtradas = filtradas.stream()
							.filter(s -> s.getSolicitacaoManutencaoId() != null
									&& String.valueOf(s.getSolicitacaoManutencaoId()).contains(idBusca))
							.collect(Collectors.toList());
				}
			}

			// Filtro por categoria (tipo de manutenção referência: máquina/predial)
			if (filtroCategoria != null && !filtroCategoria.isEmpty()) {
				filtradas = filtradas.stream().filter(s -> s.getTipoManutencaoReferencia() != null
						&& s.getTipoManutencaoReferencia().name().equals(filtroCategoria))
						.collect(Collectors.toList());
			}

			// Filtro por tipo de solicitação de compra (Material/Serviço/Material e Serviço)
			if (filtroTipoSolicitacao != null && !filtroTipoSolicitacao.isEmpty()) {
				filtradas = filtradas.stream().filter(
						s -> s.getTipoSolicitacao() != null && s.getTipoSolicitacao().name().equals(filtroTipoSolicitacao))
						.collect(Collectors.toList());
			}

			// Filtro por período (data da solicitação) - a partir de
			if (filtroDataInicio != null) {
				Calendar calInicio = Calendar.getInstance();
				calInicio.setTime(filtroDataInicio);
				calInicio.set(Calendar.HOUR_OF_DAY, 0);
				calInicio.set(Calendar.MINUTE, 0);
				calInicio.set(Calendar.SECOND, 0);
				calInicio.set(Calendar.MILLISECOND, 0);
				Date dataInicioAjustada = calInicio.getTime();

				filtradas = filtradas.stream()
						.filter(s -> s.getDataSolicitacao() != null && !s.getDataSolicitacao().before(dataInicioAjustada))
						.collect(Collectors.toList());
			}

			// Filtro por período (data da solicitação) - até
			if (filtroDataFim != null) {
				Calendar calFim = Calendar.getInstance();
				calFim.setTime(filtroDataFim);
				calFim.set(Calendar.HOUR_OF_DAY, 23);
				calFim.set(Calendar.MINUTE, 59);
				calFim.set(Calendar.SECOND, 59);
				calFim.set(Calendar.MILLISECOND, 999);
				Date dataFimAjustada = calFim.getTime();

				filtradas = filtradas.stream()
						.filter(s -> s.getDataSolicitacao() != null && !s.getDataSolicitacao().after(dataFimAjustada))
						.collect(Collectors.toList());
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao filtrar solicitações: ", e);
			filtradas = new ArrayList<>();
		}

		this.cacheFiltradasSemStatus = filtradas;
		this.contextoDoCache = contextoAtual;

		return filtradas;
	}

	/** Conta, dentro da lista já filtrada, quantas estão no status informado. */
	private long contarPorStatusFiltrado(StatusSolicitacao status) {
		return obterFiltradasSemStatus().stream().filter(s -> s.getStatusSolicitacao() == status).count();
	}

	/**
	 * Lista exibida na tela: os filtros da barra superior mais a aba escolhida.
	 */
	public List<SolicitacaoCompraMateriaisServicos> getSolicitacoesFiltradas() {
		List<SolicitacaoCompraMateriaisServicos> base = obterFiltradasSemStatus();

		if (filtroStatus == null || "TODAS".equals(filtroStatus)) {
			return base;
		}

		StatusSolicitacao status;

		if ("PENDENTE".equals(filtroStatus)) {
			status = StatusSolicitacao.PENDENTE;
		} else if ("COMPRADO".equals(filtroStatus)) {
			status = StatusSolicitacao.COMPRADO;
		} else if ("CANCELADA".equals(filtroStatus)) {
			status = StatusSolicitacao.CANCELADA;
		} else {
			return base;
		}

		return base.stream().filter(s -> s.getStatusSolicitacao() == status).collect(Collectors.toList());
	}

	// ==========================================================
	// INDICAÇÃO DE FILTROS ATIVOS
	// ==========================================================

	/** Há algum filtro da barra superior preenchido? */
	public boolean isFiltrosAtivos() {
		return (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty())
				|| (filtroCategoria != null && !filtroCategoria.isEmpty())
				|| (filtroTipoSolicitacao != null && !filtroTipoSolicitacao.isEmpty()) || filtroDataInicio != null
				|| filtroDataFim != null;
	}

	/**
	 * Texto legível dos filtros ativos, exibido em uma faixa acima da lista.
	 *
	 * Existe porque a queixa mais comum era "sumiu tudo": com um filtro de data
	 * esquecido lá em cima, a lista fica vazia sem explicação aparente.
	 */
	public String getResumoFiltros() {
		List<String> partes = new ArrayList<>();

		if (filtroBuscaId != null && !filtroBuscaId.trim().isEmpty()) {
			partes.add("manutenção contendo \"" + filtroBuscaId.trim() + "\"");
		}

		if ("MAQUINA".equals(filtroCategoria)) {
			partes.add("categoria: Máquinas e Equipamentos");
		} else if ("PREDIAL".equals(filtroCategoria)) {
			partes.add("categoria: Manutenção Predial");
		}

		if (filtroTipoSolicitacao != null && !filtroTipoSolicitacao.isEmpty()) {
			try {
				partes.add("tipo: " + TipoSolicitacao.valueOf(filtroTipoSolicitacao).getDescricao());
			} catch (IllegalArgumentException e) {
				partes.add("tipo: " + filtroTipoSolicitacao);
			}
		}

		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");

		if (filtroDataInicio != null && filtroDataFim != null) {
			partes.add("de " + sdf.format(filtroDataInicio) + " até " + sdf.format(filtroDataFim));
		} else if (filtroDataInicio != null) {
			partes.add("a partir de " + sdf.format(filtroDataInicio));
		} else if (filtroDataFim != null) {
			partes.add("até " + sdf.format(filtroDataFim));
		}

		return String.join(" · ", partes);
	}

	/**
	 * Troca a aba/status ativo (Todas, Pendentes, Processadas, Canceladas)
	 */
	public void mudarFiltroStatus(String status) {
		this.filtroStatus = status;
		invalidarCache();
	}

	/**
	 * Limpa os filtros de busca (mantém a aba selecionada)
	 */
	public void limparFiltros() {
		this.filtroBuscaId = "";
		this.filtroCategoria = "";
		this.filtroTipoSolicitacao = "";
		this.filtroDataInicio = null;
		this.filtroDataFim = null;
		invalidarCache();
	}

	public String getDescricaoTipoSolicitacao(TipoSolicitacao tipo) {
		return tipo != null ? tipo.getDescricao() : "";
	}

	public String getDescricaoStatusSolicitacao(StatusSolicitacao status) {
		return status != null ? status.getDescricao() : "";
	}

	public String getCorStatus(StatusSolicitacao status) {
		if (status == null) {
			return "text-muted";
		}

		switch (status) {
		case PENDENTE:
			return "text-warning";
		case COMPRADO:
			return "text-success";
		case CANCELADA:
			return "text-danger";
		default:
			return "text-muted";
		}
	}

	private CadastroUsuario buscarAdministradorPorId(Long adminId) {
		if (adminId == null) {
			return null;
		}
		return administradoresDisponiveis.stream().filter(a -> a.getId().equals(adminId)).findFirst().orElse(null);
	}

	/**
	 * Monta o texto exibido nas listas de seleção de administrador, no
	 * formato "Nome (nuRegistro)" (ou só "Nome" quando não há registro).
	 */
	public String getLabelAdministrador(CadastroUsuario admin) {
		if (admin == null) {
			return "";
		}
		if (admin.getNuRegistro() != null && !admin.getNuRegistro().trim().isEmpty()) {
			return admin.getNome() + " (" + admin.getNuRegistro() + ")";
		}
		return admin.getNome();
	}

	// Getters e Setters
	public List<SolicitacaoCompraMateriaisServicos> getSolicitacoesPendentes() {
		return solicitacoesPendentes;
	}

	public void setSolicitacoesPendentes(List<SolicitacaoCompraMateriaisServicos> solicitacoesPendentes) {
		this.solicitacoesPendentes = solicitacoesPendentes;
	}

	public List<SolicitacaoCompraMateriaisServicos> getTodasSolicitacoes() {
		return todasSolicitacoes;
	}

	public void setTodasSolicitacoes(List<SolicitacaoCompraMateriaisServicos> todasSolicitacoes) {
		this.todasSolicitacoes = todasSolicitacoes;
	}

	public SolicitacaoCompraMateriaisServicos getSolicitacaoSelecionada() {
		return solicitacaoSelecionada;
	}

	public void setSolicitacaoSelecionada(SolicitacaoCompraMateriaisServicos solicitacaoSelecionada) {
		this.solicitacaoSelecionada = solicitacaoSelecionada;
	}

	public Double getValorGasto() {
		return valorGasto;
	}

	public void setValorGasto(Double valorGasto) {
		this.valorGasto = valorGasto;
	}

	public Double getValorGastoMaterial() {
		return valorGastoMaterial;
	}

	public void setValorGastoMaterial(Double valorGastoMaterial) {
		this.valorGastoMaterial = valorGastoMaterial;
	}

	public Double getValorGastoServico() {
		return valorGastoServico;
	}

	public void setValorGastoServico(Double valorGastoServico) {
		this.valorGastoServico = valorGastoServico;
	}

	/**
	 * Verifica se a solicitação selecionada é do tipo AMBOS (Material e Serviço)
	 */
	public boolean isSolicitacaoTipoAmbos() {
		return solicitacaoSelecionada != null
				&& solicitacaoSelecionada.getTipoSolicitacao() == TipoSolicitacao.AMBOS;
	}

	public String getObservacoesAdministrador() {
		return observacoesAdministrador;
	}

	public void setObservacoesAdministrador(String observacoesAdministrador) {
		this.observacoesAdministrador = observacoesAdministrador;
	}

	public String getFiltroStatus() {
		return filtroStatus;
	}

	public void setFiltroStatus(String filtroStatus) {
		this.filtroStatus = filtroStatus;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	// ==========================================================
	// CONTADORES DAS ABAS
	//
	// Derivados da MESMA lista que alimenta os cards, e não mais de COUNT no
	// banco. Por isso respeitam os filtros da barra superior: o badge da aba
	// ativa é sempre igual ao número de cards exibidos abaixo dela.
	// ==========================================================

	public long getContadorPendentes() {
		return contarPorStatusFiltrado(StatusSolicitacao.PENDENTE);
	}

	public Long getAdministradorProcessouId() {
		return administradorProcessouId;
	}

	public void setAdministradorProcessouId(Long administradorProcessouId) {
		this.administradorProcessouId = administradorProcessouId;
	}

	public List<CadastroUsuario> getAdministradoresDisponiveis() {
		return administradoresDisponiveis;
	}

	public void setAdministradoresDisponiveis(List<CadastroUsuario> administradoresDisponiveis) {
		this.administradoresDisponiveis = administradoresDisponiveis;
	}

	public String getMotivoCancelamento() {
		return motivoCancelamento;
	}

	public void setMotivoCancelamento(String motivoCancelamento) {
		this.motivoCancelamento = motivoCancelamento;
	}

	public Long getAdministradorCancelamentoId() {
		return administradorCancelamentoId;
	}

	public void setAdministradorCancelamentoId(Long administradorCancelamentoId) {
		this.administradorCancelamentoId = administradorCancelamentoId;
	}

	public int getContadorTotal() {
		return obterFiltradasSemStatus().size();
	}

	public long getContadorProcessadas() {
		return contarPorStatusFiltrado(StatusSolicitacao.COMPRADO);
	}

	public long getContadorCanceladas() {
		return contarPorStatusFiltrado(StatusSolicitacao.CANCELADA);
	}

	public String getFiltroBuscaId() {
		return filtroBuscaId;
	}

	public void setFiltroBuscaId(String filtroBuscaId) {
		this.filtroBuscaId = filtroBuscaId;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	public String getFiltroCategoria() {
		return filtroCategoria;
	}

	public void setFiltroCategoria(String filtroCategoria) {
		this.filtroCategoria = filtroCategoria;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	public String getFiltroTipoSolicitacao() {
		return filtroTipoSolicitacao;
	}

	public void setFiltroTipoSolicitacao(String filtroTipoSolicitacao) {
		this.filtroTipoSolicitacao = filtroTipoSolicitacao;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	public Date getFiltroDataInicio() {
		return filtroDataInicio;
	}

	public void setFiltroDataInicio(Date filtroDataInicio) {
		this.filtroDataInicio = filtroDataInicio;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	public Date getFiltroDataFim() {
		return filtroDataFim;
	}

	public void setFiltroDataFim(Date filtroDataFim) {
		this.filtroDataFim = filtroDataFim;
		// Filtro mudou: o cache da lista filtrada precisa ser refeito.
		invalidarCache();
	}

	public String getMotivoCancelamentoProcessada() {
		return motivoCancelamentoProcessada;
	}

	public void setMotivoCancelamentoProcessada(String motivoCancelamentoProcessada) {
		this.motivoCancelamentoProcessada = motivoCancelamentoProcessada;
	}

	public Long getAdministradorCancelamentoProcessadaId() {
		return administradorCancelamentoProcessadaId;
	}

	public void setAdministradorCancelamentoProcessadaId(Long administradorCancelamentoProcessadaId) {
		this.administradorCancelamentoProcessadaId = administradorCancelamentoProcessadaId;
	}
}