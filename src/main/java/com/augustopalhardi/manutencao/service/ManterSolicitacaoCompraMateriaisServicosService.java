package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos.StatusSolicitacao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.dao.SolicitacaoCompraMateriaisServicosDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterSolicitacaoCompraMateriaisServicosService implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private SolicitacaoCompraMateriaisServicosDao solicitacaoCompraDao;

	@Inject
	private ManterHistoricoCancelamentoCompraService historicoCancelamentoCompraService;

	@Transactional
	public SolicitacaoCompraMateriaisServicos salvar(SolicitacaoCompraMateriaisServicos solicitacao) {
		return solicitacaoCompraDao.salvar(solicitacao);
	}

	public SolicitacaoCompraMateriaisServicos buscarPorId(Long id) {
		return solicitacaoCompraDao.buscarPeloCodigo(id);
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarTodas() {
		return solicitacaoCompraDao.buscarTodas();
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarPendentes() {
		return solicitacaoCompraDao.buscarPendentes();
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarPorStatus(StatusSolicitacao status) {
		return solicitacaoCompraDao.buscarPorStatus(status);
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarPorTecnico(CadastroUsuario tecnico) {
		return solicitacaoCompraDao.buscarPorTecnico(tecnico);
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarPorSolicitacaoManutencao(Long solicitacaoManutencaoId,
			TipoManutencaoReferencia tipoManutencao) {
		return solicitacaoCompraDao.buscarPorSolicitacaoManutencao(solicitacaoManutencaoId, tipoManutencao);
	}

	public Long contarPendentes() {
		return solicitacaoCompraDao.contarPendentes();
	}

	public Long contarTodas() {
		return solicitacaoCompraDao.contarTodas();
	}

	public Long contarPorStatus(StatusSolicitacao status) {
		return solicitacaoCompraDao.contarPorStatus(status);
	}

	public boolean existeSolicitacaoPendentePorManutencao(Long solicitacaoManutencaoId,
			TipoManutencaoReferencia tipoManutencao) {
		return solicitacaoCompraDao.existeSolicitacaoPendentePorManutencao(solicitacaoManutencaoId, tipoManutencao);
	}

	/**
	 * Cria uma nova solicitação de compra
	 */
	@Transactional
	public SolicitacaoCompraMateriaisServicos criarSolicitacao(Long solicitacaoManutencaoId,
			TipoManutencaoReferencia tipoManutencao, CadastroUsuario tecnico,
			SolicitacaoCompraMateriaisServicos.TipoSolicitacao tipoSolicitacao, String descricaoDetalhada) {
		SolicitacaoCompraMateriaisServicos solicitacao = new SolicitacaoCompraMateriaisServicos();
		solicitacao.setSolicitacaoManutencaoId(solicitacaoManutencaoId);
		solicitacao.setTipoManutencaoReferencia(tipoManutencao);
		solicitacao.setTecnicoSolicitante(tecnico);
		solicitacao.setTipoSolicitacao(tipoSolicitacao);
		solicitacao.setDescricaoDetalhada(descricaoDetalhada);
		solicitacao.setStatusSolicitacao(StatusSolicitacao.PENDENTE);
		solicitacao.setDataSolicitacao(new Date());

		return salvar(solicitacao);
	}

	/**
	 * Processa uma solicitação (marca como comprada).
	 * Para solicitações do tipo AMBOS, recebe valores separados de material e serviço.
	 */
	@Transactional
	public SolicitacaoCompraMateriaisServicos processarCompra(Long solicitacaoId, CadastroUsuario administrador,
			Double valorGasto, Double valorGastoMaterial, Double valorGastoServico, String observacoesAdministrador) {
		SolicitacaoCompraMateriaisServicos solicitacao = buscarPorId(solicitacaoId);
		if (solicitacao == null) {
			throw new IllegalArgumentException("Solicitação não encontrada");
		}

		if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
			throw new IllegalStateException("Solicitação não está pendente");
		}

		solicitacao.setStatusSolicitacao(StatusSolicitacao.COMPRADO);
		solicitacao.setAdministradorProcessou(administrador);
		solicitacao.setValorGasto(valorGasto);
		solicitacao.setValorGastoMaterial(valorGastoMaterial);
		solicitacao.setValorGastoServico(valorGastoServico);
		solicitacao.setObservacoesAdministrador(observacoesAdministrador);
		solicitacao.setDataProcessamento(new Date());

		return salvar(solicitacao);
	}

	/**
	 * Cancela uma solicitação
	 */
	@Transactional
	public SolicitacaoCompraMateriaisServicos cancelarSolicitacao(Long solicitacaoId, CadastroUsuario administrador,
			String motivoCancelamento) {
		SolicitacaoCompraMateriaisServicos solicitacao = buscarPorId(solicitacaoId);
		if (solicitacao == null) {
			throw new IllegalArgumentException("Solicitação não encontrada");
		}

		if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.PENDENTE) {
			throw new IllegalStateException("Solicitação não está pendente");
		}

		solicitacao.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
		solicitacao.setAdministradorProcessou(administrador);
		solicitacao.setObservacoesAdministrador(motivoCancelamento);
		solicitacao.setDataProcessamento(new Date());

		return salvar(solicitacao);
	}

	/**
	 * Cancela uma compra que JÁ HAVIA SIDO PROCESSADA (status COMPRADO).
	 *
	 * Diferente de {@link #cancelarSolicitacao}, que só cancela solicitações
	 * ainda pendentes, este método trata do caso em que a compra já havia sido
	 * concluída e precisa ser desfeita (ex: fornecedor cancelou, item foi
	 * devolvido, erro no lançamento). Antes de alterar o status, os dados da
	 * compra (valor gasto, quem processou, quando processou) são preservados
	 * em um registro de histórico, para fins de auditoria.
	 */
	@Transactional
	public SolicitacaoCompraMateriaisServicos cancelarCompraProcessada(Long solicitacaoId,
			CadastroUsuario administradorCancelamento, String motivoCancelamento) {
		SolicitacaoCompraMateriaisServicos solicitacao = buscarPorId(solicitacaoId);
		if (solicitacao == null) {
			throw new IllegalArgumentException("Solicitação não encontrada");
		}

		if (solicitacao.getStatusSolicitacao() != StatusSolicitacao.COMPRADO) {
			throw new IllegalStateException("Somente compras já processadas podem ser canceladas por aqui");
		}

		// Registra no histórico os dados da compra antes de sobrescrever
		historicoCancelamentoCompraService.registrarCancelamento(solicitacao.getId(), solicitacao.getValorGasto(),
				solicitacao.getValorGastoMaterial(), solicitacao.getValorGastoServico(),
				solicitacao.getAdministradorProcessou() != null ? solicitacao.getAdministradorProcessou().getNome()
						: null,
				solicitacao.getDataProcessamento(), motivoCancelamento, administradorCancelamento.getNome());

		solicitacao.setStatusSolicitacao(StatusSolicitacao.CANCELADA);
		solicitacao.setMotivoCancelamento(motivoCancelamento);
		solicitacao.setAdministradorCancelamento(administradorCancelamento);
		solicitacao.setDataCancelamento(new Date());

		return salvar(solicitacao);
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarPorFaixaValor(Double valorMinimo, Double valorMaximo) {
		return solicitacaoCompraDao.buscarPorFaixaValor(valorMinimo, valorMaximo);
	}

	public Double calcularTotalGastoPorPeriodo(Date dataInicio, Date dataFim) {
		return solicitacaoCompraDao.calcularTotalGastoPorPeriodo(dataInicio, dataFim);
	}

	@Transactional
	public void excluir(SolicitacaoCompraMateriaisServicos solicitacao) {
		solicitacaoCompraDao.excluir(solicitacao);
	}
}