package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class CadastroUsuarioDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public CadastroUsuario salvar(CadastroUsuario cadastroUsuario) {
		return manager.merge(cadastroUsuario);
	}

	public CadastroUsuario buscarPeloCodigo(Long id) {
		return manager.find(CadastroUsuario.class, id);
	}

	public List<CadastroUsuario> buscarTodos() {
		String query = "SELECT a FROM CadastroUsuario a";
		TypedQuery<CadastroUsuario> q = manager.createQuery(query, CadastroUsuario.class);
		return q.getResultList();
	}

	public CadastroUsuario buscarPorNuRegistro(String nuRegistro) { // Alterado o parâmetro para String
		try {
			String query = "SELECT u FROM CadastroUsuario u WHERE u.nuRegistro = :nuRegistro";
			TypedQuery<CadastroUsuario> q = manager.createQuery(query, CadastroUsuario.class);
			q.setParameter("nuRegistro", nuRegistro);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public CadastroUsuario buscarPorEmail(String email) {
		try {
			String query = "SELECT u FROM CadastroUsuario u WHERE u.email = :email";
			TypedQuery<CadastroUsuario> q = manager.createQuery(query, CadastroUsuario.class);
			q.setParameter("email", email);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public CadastroUsuario buscarPorTelefone(String telefone) { // Alterado o parâmetro para String
		try {
			String query = "SELECT u FROM CadastroUsuario u WHERE u.telefone = :telefone";
			TypedQuery<CadastroUsuario> q = manager.createQuery(query, CadastroUsuario.class);
			q.setParameter("telefone", telefone);
			return q.getSingleResult();
		} catch (NoResultException e) {
			return null;
		}
	}

	public long contarUsuarios() {
		try {
			String query = "SELECT COUNT(u) FROM CadastroUsuario u";
			return manager.createQuery(query, Long.class).getSingleResult();
		} catch (Exception e) {
			return 0;
		}
	}

	@Transactional
	public void excluir(CadastroUsuario cadastroUsuario) {
		CadastroUsuario cadastroUsuarioGerenciado = manager.merge(cadastroUsuario);
		manager.remove(cadastroUsuarioGerenciado);
	}
}