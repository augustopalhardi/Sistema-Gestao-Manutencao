package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos;
import com.augustopalhardi.manutencao.model.SolicitacaoCompraMateriaisServicos.StatusSolicitacao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class SolicitacaoCompraMateriaisServicosDao implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public SolicitacaoCompraMateriaisServicos salvar(SolicitacaoCompraMateriaisServicos solicitacao) {
		return manager.merge(solicitacao);
	}

	public SolicitacaoCompraMateriaisServicos buscarPeloCodigo(Long id) {
		return manager.find(SolicitacaoCompraMateriaisServicos.class, id);
	}

	public List<SolicitacaoCompraMateriaisServicos> buscarTodas() {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s ORDER BY s.dataSolicitacao DESC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		return q.getResultList();
	}

	/**
	 * Busca solicitações pendentes (não processadas)
	 */
	public List<SolicitacaoCompraMateriaisServicos> buscarPendentes() {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s " + "WHERE s.statusSolicitacao = :status "
				+ "ORDER BY s.dataSolicitacao ASC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		q.setParameter("status", StatusSolicitacao.PENDENTE);
		return q.getResultList();
	}

	/**
	 * Busca solicitações por status
	 */
	public List<SolicitacaoCompraMateriaisServicos> buscarPorStatus(StatusSolicitacao status) {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s " + "WHERE s.statusSolicitacao = :status "
				+ "ORDER BY s.dataSolicitacao DESC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		q.setParameter("status", status);
		return q.getResultList();
	}

	/**
	 * Busca solicitações por técnico solicitante
	 */
	public List<SolicitacaoCompraMateriaisServicos> buscarPorTecnico(CadastroUsuario tecnico) {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s " + "WHERE s.tecnicoSolicitante = :tecnico "
				+ "ORDER BY s.dataSolicitacao DESC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		q.setParameter("tecnico", tecnico);
		return q.getResultList();
	}

	/**
	 * Busca solicitações por solicitação de manutenção específica
	 */
	public List<SolicitacaoCompraMateriaisServicos> buscarPorSolicitacaoManutencao(Long solicitacaoManutencaoId,
			TipoManutencaoReferencia tipoManutencao) {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.solicitacaoManutencaoId = :solicitacaoId "
				+ "AND s.tipoManutencaoReferencia = :tipoManutencao " + "ORDER BY s.dataSolicitacao DESC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		q.setParameter("solicitacaoId", solicitacaoManutencaoId);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getResultList();
	}

	/**
	 * Conta solicitações pendentes
	 */
	public Long contarPendentes() {
		String query = "SELECT COUNT(s) FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.statusSolicitacao = :status";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		q.setParameter("status", StatusSolicitacao.PENDENTE);
		return q.getSingleResult();
	}

	/**
	 * Conta o total de solicitações, independente do status
	 */
	public Long contarTodas() {
		String query = "SELECT COUNT(s) FROM SolicitacaoCompraMateriaisServicos s";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		return q.getSingleResult();
	}

	/**
	 * Conta solicitações por status (usado nos cards de resumo da tela)
	 */
	public Long contarPorStatus(StatusSolicitacao status) {
		String query = "SELECT COUNT(s) FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.statusSolicitacao = :status";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		q.setParameter("status", status);
		return q.getSingleResult();
	}

	/**
	 * Verifica se existe solicitação pendente para uma manutenção específica
	 */
	public boolean existeSolicitacaoPendentePorManutencao(Long solicitacaoManutencaoId,
			TipoManutencaoReferencia tipoManutencao) {
		String query = "SELECT COUNT(s) FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.solicitacaoManutencaoId = :solicitacaoId "
				+ "AND s.tipoManutencaoReferencia = :tipoManutencao " + "AND s.statusSolicitacao = :status";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		q.setParameter("solicitacaoId", solicitacaoManutencaoId);
		q.setParameter("tipoManutencao", tipoManutencao);
		q.setParameter("status", StatusSolicitacao.PENDENTE);
		return q.getSingleResult() > 0;
	}

	/**
	 * Busca solicitações com valor gasto entre um intervalo
	 */
	public List<SolicitacaoCompraMateriaisServicos> buscarPorFaixaValor(Double valorMinimo, Double valorMaximo) {
		String query = "SELECT s FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.valorGasto >= :valorMin AND s.valorGasto <= :valorMax "
				+ "AND s.statusSolicitacao = :status " + "ORDER BY s.valorGasto DESC";
		TypedQuery<SolicitacaoCompraMateriaisServicos> q = manager.createQuery(query,
				SolicitacaoCompraMateriaisServicos.class);
		q.setParameter("valorMin", valorMinimo);
		q.setParameter("valorMax", valorMaximo);
		q.setParameter("status", StatusSolicitacao.COMPRADO);
		return q.getResultList();
	}

	/**
	 * Calcula soma total de valores gastos em um período
	 */
	public Double calcularTotalGastoPorPeriodo(java.util.Date dataInicio, java.util.Date dataFim) {
		String query = "SELECT COALESCE(SUM(s.valorGasto), 0.0) FROM SolicitacaoCompraMateriaisServicos s "
				+ "WHERE s.statusSolicitacao = :status " + "AND s.dataProcessamento >= :dataInicio "
				+ "AND s.dataProcessamento <= :dataFim";
		TypedQuery<Double> q = manager.createQuery(query, Double.class);
		q.setParameter("status", StatusSolicitacao.COMPRADO);
		q.setParameter("dataInicio", dataInicio);
		q.setParameter("dataFim", dataFim);
		return q.getSingleResult();
	}

	@Transactional
	public void excluir(SolicitacaoCompraMateriaisServicos solicitacao) {
		if (!manager.contains(solicitacao)) {
			solicitacao = manager.merge(solicitacao);
		}
		manager.remove(solicitacao);
	}
}