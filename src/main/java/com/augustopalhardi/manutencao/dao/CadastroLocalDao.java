package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class CadastroLocalDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public CadastroLocal salvar(CadastroLocal cadastroLocal) {
		return manager.merge(cadastroLocal);
	}

	public CadastroLocal buscarPeloCodigo(Long id) {
		return manager.find(CadastroLocal.class, id);
	}

	public List<CadastroLocal> buscarTodos() {
		String query = "SELECT a FROM CadastroLocal a";
		TypedQuery<CadastroLocal> q = manager.createQuery(query, CadastroLocal.class);
		return q.getResultList();
	}

	/**
	 * Locais ativos, ordenados por nome. Registros com "ativo" nulo (anteriores
	 * à criação da coluna) contam como ativos.
	 */
	public List<CadastroLocal> buscarAtivos() {
		String query = "SELECT a FROM CadastroLocal a WHERE a.ativo = true OR a.ativo IS NULL ORDER BY a.nome";
		TypedQuery<CadastroLocal> q = manager.createQuery(query, CadastroLocal.class);
		return q.getResultList();
	}

	/**
	 * Retorna null quando não há local com esse nome.
	 */
	public CadastroLocal buscarPorNome(String nome) {
		try {
			String query = "SELECT l FROM CadastroLocal l WHERE l.nome = :nome";
			TypedQuery<CadastroLocal> q = manager.createQuery(query, CadastroLocal.class);
			q.setParameter("nome", nome);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public List<CadastroLocal> buscarPorArea(int area) {
		String query = "SELECT l FROM CadastroLocal l WHERE l.area = :area";
		TypedQuery<CadastroLocal> q = manager.createQuery(query, CadastroLocal.class);
		q.setParameter("area", area);
		return q.getResultList();
	}

	@Transactional
	public void excluir(CadastroLocal cadastroLocal) {
		CadastroLocal cadastroLocalGerenciado = manager.merge(cadastroLocal);
		manager.remove(cadastroLocalGerenciado);
	}
}
