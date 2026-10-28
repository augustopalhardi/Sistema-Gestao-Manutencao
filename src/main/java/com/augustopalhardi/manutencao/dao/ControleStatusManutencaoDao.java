package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.ControleStatusManutencao;
import com.augustopalhardi.manutencao.model.ControleStatusManutencao.StatusManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class ControleStatusManutencaoDao implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public ControleStatusManutencao salvar(ControleStatusManutencao controleStatus) {
		return manager.merge(controleStatus);
	}

	public ControleStatusManutencao buscarPeloCodigo(Long id) {
		return manager.find(ControleStatusManutencao.class, id);
	}

	public List<ControleStatusManutencao> buscarTodos() {
		String query = "SELECT c FROM ControleStatusManutencao c ORDER BY c.dataAtualizacao DESC";
		TypedQuery<ControleStatusManutencao> q = manager.createQuery(query, ControleStatusManutencao.class);
		return q.getResultList();
	}

	public ControleStatusManutencao buscarPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			String query = "SELECT c FROM ControleStatusManutencao c WHERE c.solicitacaoId = :solicitacaoId AND c.tipoManutencao = :tipo";
			TypedQuery<ControleStatusManutencao> q = manager.createQuery(query, ControleStatusManutencao.class);
			q.setParameter("solicitacaoId", solicitacaoId);
			q.setParameter("tipo", tipo);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<ControleStatusManutencao> buscarPorStatus(StatusManutencao status) {
		String query = "SELECT c FROM ControleStatusManutencao c WHERE c.status = :status ORDER BY c.dataAtualizacao DESC";
		TypedQuery<ControleStatusManutencao> q = manager.createQuery(query, ControleStatusManutencao.class);
		q.setParameter("status", status);
		return q.getResultList();
	}

	public List<ControleStatusManutencao> buscarPorTipo(TipoManutencaoReferencia tipo) {
		String query = "SELECT c FROM ControleStatusManutencao c WHERE c.tipoManutencao = :tipo ORDER BY c.dataAtualizacao DESC";
		TypedQuery<ControleStatusManutencao> q = manager.createQuery(query, ControleStatusManutencao.class);
		q.setParameter("tipo", tipo);
		return q.getResultList();
	}

	public List<ControleStatusManutencao> buscarPorStatusETipo(StatusManutencao status, TipoManutencaoReferencia tipo) {
		String query = "SELECT c FROM ControleStatusManutencao c WHERE c.status = :status AND c.tipoManutencao = :tipo ORDER BY c.dataAtualizacao DESC";
		TypedQuery<ControleStatusManutencao> q = manager.createQuery(query, ControleStatusManutencao.class);
		q.setParameter("status", status);
		q.setParameter("tipo", tipo);
		return q.getResultList();
	}

	@Transactional
	public void excluir(ControleStatusManutencao controleStatus) {
		ControleStatusManutencao controleGerenciado = manager.merge(controleStatus);
		manager.remove(controleGerenciado);
	}

	@Transactional
	public ControleStatusManutencao criarOuAtualizar(Long solicitacaoId, TipoManutencaoReferencia tipo,
			StatusManutencao novoStatus) {
		ControleStatusManutencao controle = buscarPorSolicitacao(solicitacaoId, tipo);

		if (controle == null) {
			// Criar novo registro
			controle = new ControleStatusManutencao(solicitacaoId, tipo);
		}

		controle.setStatus(novoStatus);
		return salvar(controle);
	}
}