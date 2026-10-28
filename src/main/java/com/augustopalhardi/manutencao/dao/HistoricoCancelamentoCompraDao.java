package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.HistoricoCancelamentoCompra;

/**
 * DAO para gerenciar o histórico de cancelamentos de compras já processadas.
 * Usa injeção automática de EntityManager via CDI.
 */
public class HistoricoCancelamentoCompraDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager em;

	/**
	 * Salva um novo registro de cancelamento de compra
	 */
	public HistoricoCancelamentoCompra salvar(HistoricoCancelamentoCompra historico) {
		if (historico.getId() == null) {
			em.persist(historico);
			return historico;
		} else {
			return em.merge(historico);
		}
	}

	/**
	 * Busca histórico por ID
	 */
	public HistoricoCancelamentoCompra buscarPorId(Long id) {
		return em.find(HistoricoCancelamentoCompra.class, id);
	}

	/**
	 * Busca todos os registros de cancelamento de uma solicitação de compra
	 * específica
	 */
	public List<HistoricoCancelamentoCompra> buscarPorSolicitacaoCompra(Long solicitacaoCompraId) {
		try {
			TypedQuery<HistoricoCancelamentoCompra> query = em.createQuery(
					"SELECT h FROM HistoricoCancelamentoCompra h " + "WHERE h.solicitacaoCompraId = :solicitacaoCompraId "
							+ "ORDER BY h.dataCancelamento DESC",
					HistoricoCancelamentoCompra.class);
			query.setParameter("solicitacaoCompraId", solicitacaoCompraId);
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		}
	}

	/**
	 * Busca todos os históricos de cancelamento
	 */
	public List<HistoricoCancelamentoCompra> buscarTodos() {
		TypedQuery<HistoricoCancelamentoCompra> query = em.createQuery(
				"SELECT h FROM HistoricoCancelamentoCompra h ORDER BY h.dataCancelamento DESC",
				HistoricoCancelamentoCompra.class);
		return query.getResultList();
	}

	/**
	 * Deleta um histórico
	 */
	public void deletar(Long id) {
		HistoricoCancelamentoCompra historico = buscarPorId(id);
		if (historico != null) {
			em.remove(historico);
		}
	}
}