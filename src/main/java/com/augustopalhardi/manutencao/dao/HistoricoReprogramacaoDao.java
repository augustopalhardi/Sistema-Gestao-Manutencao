package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;

/**
 * DAO para gerenciar o histórico de reprogramações de manutenções Usa injeção
 * automática de EntityManager via CDI
 */
public class HistoricoReprogramacaoDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager em;

	/**
	 * Salva um novo registro de reprogramação
	 */
	public HistoricoReprogramacao salvar(HistoricoReprogramacao historico) {
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
	public HistoricoReprogramacao buscarPorId(Long id) {
		return em.find(HistoricoReprogramacao.class, id);
	}

	/**
	 * Busca todos os históricos de uma solicitação específica
	 */
	public List<HistoricoReprogramacao> buscarPorSolicitacao(Long solicitacaoId,
			TipoManutencaoReferencia tipoManutencao) {
		try {
			TypedQuery<HistoricoReprogramacao> query = em.createQuery(
					"SELECT h FROM HistoricoReprogramacao h " + "WHERE h.solicitacaoId = :solicitacaoId "
							+ "AND h.tipoManutencao = :tipoManutencao " + "ORDER BY h.dataHoraReprogramacao DESC",
					HistoricoReprogramacao.class);
			query.setParameter("solicitacaoId", solicitacaoId);
			query.setParameter("tipoManutencao", tipoManutencao);
			return query.getResultList();
		} catch (NoResultException e) {
			return new ArrayList<>();
		}
	}

	/**
	 * Busca todos os históricos
	 */
	public List<HistoricoReprogramacao> buscarTodos() {
		TypedQuery<HistoricoReprogramacao> query = em.createQuery(
				"SELECT h FROM HistoricoReprogramacao h ORDER BY h.dataHoraReprogramacao DESC",
				HistoricoReprogramacao.class);
		return query.getResultList();
	}

	/**
	 * Conta quantas vezes uma solicitação foi reprogramada
	 */
	public Long contarReprogramacoes(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao) {
		try {
			TypedQuery<Long> query = em.createQuery("SELECT COUNT(h) FROM HistoricoReprogramacao h "
					+ "WHERE h.solicitacaoId = :solicitacaoId " + "AND h.tipoManutencao = :tipoManutencao", Long.class);
			query.setParameter("solicitacaoId", solicitacaoId);
			query.setParameter("tipoManutencao", tipoManutencao);
			return query.getSingleResult();
		} catch (NoResultException e) {
			return 0L;
		}
	}

	/**
	 * Busca a última reprogramação de uma solicitação
	 */
	public HistoricoReprogramacao buscarUltimaReprogramacao(Long solicitacaoId,
			TipoManutencaoReferencia tipoManutencao) {
		try {
			TypedQuery<HistoricoReprogramacao> query = em.createQuery(
					"SELECT h FROM HistoricoReprogramacao h " + "WHERE h.solicitacaoId = :solicitacaoId "
							+ "AND h.tipoManutencao = :tipoManutencao " + "ORDER BY h.dataHoraReprogramacao DESC",
					HistoricoReprogramacao.class);
			query.setParameter("solicitacaoId", solicitacaoId);
			query.setParameter("tipoManutencao", tipoManutencao);
			query.setMaxResults(1);
			return query.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	/**
	 * Deleta um histórico
	 */
	public void deletar(Long id) {
		HistoricoReprogramacao historico = buscarPorId(id);
		if (historico != null) {
			em.remove(historico);
		}
	}
}