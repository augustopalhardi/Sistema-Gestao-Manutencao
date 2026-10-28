package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class SolicitacaoManutencaoPredialDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public SolicitacaoManutencaoPredial salvar(SolicitacaoManutencaoPredial solicitacaoManutencaoPredial) {
		return manager.merge(solicitacaoManutencaoPredial);
	}

	public SolicitacaoManutencaoPredial buscarPeloCodigo(Long id) {
		return manager.find(SolicitacaoManutencaoPredial.class, id);
	}

	public List<SolicitacaoManutencaoPredial> buscarTodos() {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s ORDER BY s.cancelada ASC, s.id DESC";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarAtivas() {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.cancelada = false ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarCanceladas() {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.cancelada = true ORDER BY s.dataCancelamento DESC";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarPorProblema(String problema) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.problema LIKE :problema";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("problema", "%" + problema + "%");
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarPorLocal(CadastroLocal local) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.local = :local";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("local", local);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarPorRiscoSeguranca(boolean riscoSeguranca) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.riscoSeguranca = :riscoSeguranca";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("riscoSeguranca", riscoSeguranca);
		return q.getResultList();
	}

	public List<SolicitacaoManutencaoPredial> buscarPorUrgencia(boolean urgente) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.urgente = :urgente";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("urgente", urgente);
		return q.getResultList();
	}

	// Método para buscar por tipo de manutenção
	public List<SolicitacaoManutencaoPredial> buscarPorTipoManutencao(TipoManutencao tipoManutencao) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.tipoManutencao = :tipoManutencao ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getResultList();
	}

	// Método para buscar solicitações ativas por tipo
	public List<SolicitacaoManutencaoPredial> buscarAtivasPorTipo(TipoManutencao tipoManutencao) {
		String query = "SELECT s FROM SolicitacaoManutencaoPredial s WHERE s.cancelada = false AND s.tipoManutencao = :tipoManutencao ORDER BY s.id DESC";
		TypedQuery<SolicitacaoManutencaoPredial> q = manager.createQuery(query, SolicitacaoManutencaoPredial.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getResultList();
	}

	// Método para contar solicitações por tipo
	public Long contarPorTipo(TipoManutencao tipoManutencao) {
		String query = "SELECT COUNT(s) FROM SolicitacaoManutencaoPredial s WHERE s.tipoManutencao = :tipoManutencao";
		TypedQuery<Long> q = manager.createQuery(query, Long.class);
		q.setParameter("tipoManutencao", tipoManutencao);
		return q.getSingleResult();
	}

	@Transactional
	public void excluir(SolicitacaoManutencaoPredial solicitacaoManutencaoPredial) {
		SolicitacaoManutencaoPredial solicitacaoGerenciada = manager.merge(solicitacaoManutencaoPredial);
		manager.remove(solicitacaoGerenciada);
	}
}