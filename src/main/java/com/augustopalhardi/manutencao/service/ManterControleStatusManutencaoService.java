package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.dao.ControleStatusManutencaoDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterControleStatusManutencaoService implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private ControleStatusManutencaoDao controleStatusDao;

	@Transactional
	public ControleStatusManutencao salvar(ControleStatusManutencao controleStatus) {
		return controleStatusDao.salvar(controleStatus);
	}

	public ControleStatusManutencao buscarPorId(Long controleId) {
		return controleStatusDao.buscarPeloCodigo(controleId);
	}

	public List<ControleStatusManutencao> buscarTodos() {
		return controleStatusDao.buscarTodos();
	}

	public ControleStatusManutencao buscarPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		return controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);
	}

	public List<ControleStatusManutencao> buscarPorStatus(StatusManutencao status) {
		return controleStatusDao.buscarPorStatus(status);
	}

	public List<ControleStatusManutencao> buscarPorTipo(TipoManutencaoReferencia tipo) {
		return controleStatusDao.buscarPorTipo(tipo);
	}

	public List<ControleStatusManutencao> buscarPorStatusETipo(StatusManutencao status, TipoManutencaoReferencia tipo) {
		return controleStatusDao.buscarPorStatusETipo(status, tipo);
	}

	@Transactional
	public ControleStatusManutencao iniciarManutencao(Long solicitacaoId, TipoManutencaoReferencia tipo,
			java.util.Date dataInicio) {
		ControleStatusManutencao controle = controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			controle = new ControleStatusManutencao(solicitacaoId, tipo);
		}

		controle.setStatus(StatusManutencao.EM_ANDAMENTO);
		controle.setDataInicio(dataInicio);

		return controleStatusDao.salvar(controle);
	}

	@Transactional
	public ControleStatusManutencao finalizarManutencao(Long solicitacaoId, TipoManutencaoReferencia tipo,
			java.util.Date dataConclusao, String solucao, String observacoes, boolean bbsNearMiss) {
		ControleStatusManutencao controle = controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			throw new IllegalStateException("Não é possível finalizar uma manutenção que não foi iniciada.");
		}

		controle.setStatus(StatusManutencao.CONCLUIDA);
		controle.setDataConclusao(dataConclusao);
		controle.setSolucao(solucao);
		controle.setObservacoesFinais(observacoes);
		controle.setBbsNearMiss(bbsNearMiss);

		return controleStatusDao.salvar(controle);
	}

	@Transactional
	public ControleStatusManutencao salvarBbsNearMiss(Long solicitacaoId, TipoManutencaoReferencia tipo, boolean bbsNearMiss) {
		ControleStatusManutencao controle = controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			controle = new ControleStatusManutencao(solicitacaoId, tipo);
		}

		controle.setBbsNearMiss(bbsNearMiss);

		return controleStatusDao.salvar(controle);
	}

	@Transactional
	public ControleStatusManutencao salvarSolucaoObservacoes(Long solicitacaoId, TipoManutencaoReferencia tipo, String solucao,
			String observacoes) {
		ControleStatusManutencao controle = controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			controle = new ControleStatusManutencao(solicitacaoId, tipo);
		}

		controle.setSolucao(solucao);
		controle.setObservacoesFinais(observacoes);

		return controleStatusDao.salvar(controle);
	}

	@Transactional
	public ControleStatusManutencao cancelarManutencao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		ControleStatusManutencao controle = controleStatusDao.buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			controle = new ControleStatusManutencao(solicitacaoId, tipo);
		}

		controle.setStatus(StatusManutencao.PARA_FAZER);
		controle.setDataInicio(null);
		controle.setDataConclusao(null);
		controle.setSolucao(null);
		controle.setObservacoesFinais(null);
		controle.setBbsNearMiss(false);

		return controleStatusDao.salvar(controle);
	}

	@Transactional
	public ControleStatusManutencao criarOuAtualizar(Long solicitacaoId, TipoManutencaoReferencia tipo,
			StatusManutencao novoStatus) {
		return controleStatusDao.criarOuAtualizar(solicitacaoId, tipo, novoStatus);
	}

	@Transactional
	public void excluir(ControleStatusManutencao controleStatus) {
		controleStatusDao.excluir(controleStatus);
	}
}