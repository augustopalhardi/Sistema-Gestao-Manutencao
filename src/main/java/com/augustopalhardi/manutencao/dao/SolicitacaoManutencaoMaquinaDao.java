package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class SolicitacaoManutencaoMaquinaDao implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public SolicitacaoManutencaoMaquina salvar(SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina) {
		return manager.merge(solicitacaoManutencaoMaquina);
	}

	public SolicitacaoManutencaoMaquina buscarPeloCodigo(Long id) {
		return manager.find(SolicitacaoManutencaoMaquina.class, id);
	}

	public List<SolicitacaoManutencaoMaquina> buscarTodos() {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s ORDER BY s.cancelada ASC, s.id DESC";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		return q.getResultList();
	}

	// Novo método para buscar apenas solicitações ativas (não canceladas)
	public List<SolicitacaoManutencaoMaquina> buscarAtivas() {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.cancelada = false ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		return q.getResultList();
	}

	// Novo método para buscar solicitações canceladas
	public List<SolicitacaoManutencaoMaquina> buscarCanceladas() {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.cancelada = true ORDER BY s.dataCancelamento DESC";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorFalha(String falha) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.falha LIKE :falha";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("falha", "%" + falha + "%");
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorMaquina(CadastroMaquinaEquipamento maquina) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.maquina = :maquina";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("maquina", maquina);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorRisco(boolean risco) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.risco = :risco";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("risco", risco);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorOperando(boolean operando) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.operando = :operando";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("operando", operando);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorTipoManutencao(TipoManutencao tipoManutencao) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.tipoManutencao = :tipoManutencao ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getResultList();
	}

	// Método para buscar solicitações ativas por tipo
	public List<SolicitacaoManutencaoMaquina> buscarAtivasPorTipo(TipoManutencao tipoManutencao) {
		String query = "SELECT s FROM SolicitacaoManutencaoMaquina s WHERE s.cancelada = false AND s.tipoManutencao = :tipoManutencao ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoMaquina> q = manager.createQuery(query, SolicitacaoManutencaoMaquina.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getResultList();
	}

	// Método para contar solicitações por tipo
	public Long contarPorTipo(TipoManutencao tipoManutencao) {
		String query = "SELECT COUNT(s) FROM SolicitacaoManutencaoMaquina s WHERE s.tipoManutencao = :tipoManutencao";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getSingleResult();
	}

	@Transactional
	public void excluir(SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina) {
		SolicitacaoManutencaoMaquina solicitacaoGerenciada = manager.merge(solicitacaoManutencaoMaquina);
		manager.remove(solicitacaoGerenciada);
	}
}