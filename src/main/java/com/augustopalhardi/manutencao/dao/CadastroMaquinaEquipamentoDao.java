package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class CadastroMaquinaEquipamentoDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public CadastroMaquinaEquipamento salvar(CadastroMaquinaEquipamento cadastroMaquinaEquipamento) {
		return manager.merge(cadastroMaquinaEquipamento);
	}

	public CadastroMaquinaEquipamento buscarPeloCodigo(Long id) {
		return manager.find(CadastroMaquinaEquipamento.class, id);
	}

	public List<CadastroMaquinaEquipamento> buscarTodos() {
		String query = "SELECT a FROM CadastroMaquinaEquipamento a ORDER BY a.numeroPatrimonio";
		TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
		return q.getResultList();
	}

	// Buscar apenas máquinas/equipamentos ativos (inclui NULL como
	// ativo)
	public List<CadastroMaquinaEquipamento> buscarAtivos() {
		String query = "SELECT a FROM CadastroMaquinaEquipamento a WHERE a.ativo = true OR a.ativo IS NULL ORDER BY a.numeroPatrimonio";
		TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
		return q.getResultList();
	}

	public CadastroMaquinaEquipamento buscarPorNumeroPatrimonio(String numeroPatrimonio) {
		try {
			String query = "SELECT m FROM CadastroMaquinaEquipamento m WHERE m.numeroPatrimonio = :numeroPatrimonio";
			TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
			q.setParameter("numeroPatrimonio", numeroPatrimonio);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public boolean existeNumeroPatrimonio(String numeroPatrimonio, Long idExcluir) {
		try {
			String query = "SELECT COUNT(m) FROM CadastroMaquinaEquipamento m WHERE m.numeroPatrimonio = :numeroPatrimonio";

			if (idExcluir != null) {
				query += " AND m.id != :idExcluir";
			}

			TypedQuery<Long> q = manager.createQuery(query, Long.class);
			q.setParameter("numeroPatrimonio", numeroPatrimonio);

			if (idExcluir != null) {
				q.setParameter("idExcluir", idExcluir);
			}

			return q.getSingleResult() > 0;
		} catch (Exception e) {
			return false;
		}
	}

	public List<CadastroMaquinaEquipamento> buscarPorFabricante(String fabricante) {
		String query = "SELECT m FROM CadastroMaquinaEquipamento m WHERE m.fabricante = :fabricante ORDER BY m.numeroPatrimonio";
		TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
		q.setParameter("fabricante", fabricante);
		return q.getResultList();
	}

	public List<CadastroMaquinaEquipamento> buscarPorModelo(String modelo) {
		String query = "SELECT m FROM CadastroMaquinaEquipamento m WHERE m.modelo = :modelo ORDER BY m.numeroPatrimonio";
		TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
		q.setParameter("modelo", modelo);
		return q.getResultList();
	}

	public List<CadastroMaquinaEquipamento> buscarPorTipo(String tipo) {
		String query = "SELECT m FROM CadastroMaquinaEquipamento m WHERE m.tipo = :tipo ORDER BY m.numeroPatrimonio";
		TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
		q.setParameter("tipo", tipo);
		return q.getResultList();
	}

	public CadastroMaquinaEquipamento buscarPorAnoFabricacao(Date anoFabricacao) {
		try {
			String query = "SELECT m FROM CadastroMaquinaEquipamento m WHERE m.anoFabricacao = :anoFabricacao";
			TypedQuery<CadastroMaquinaEquipamento> q = manager.createQuery(query, CadastroMaquinaEquipamento.class);
			q.setParameter("anoFabricacao", anoFabricacao);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	@Transactional
	public void excluir(CadastroMaquinaEquipamento cadastroMaquinaEquipamento) {
		CadastroMaquinaEquipamento maquinaEquipamentoGerenciado = manager.merge(cadastroMaquinaEquipamento);
		manager.remove(maquinaEquipamentoGerenciado);
	}
}