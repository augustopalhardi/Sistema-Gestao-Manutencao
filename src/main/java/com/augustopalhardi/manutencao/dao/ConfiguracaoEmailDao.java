package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.augustopalhardi.manutencao.model.ConfiguracaoEmail;
import com.augustopalhardi.manutencao.model.ConfiguracaoEmail.TipoNotificacao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class ConfiguracaoEmailDao implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private EntityManager manager;

	@Transactional
	public ConfiguracaoEmail salvar(ConfiguracaoEmail config) {
		return manager.merge(config);
	}

	public ConfiguracaoEmail buscarPorId(Long id) {
		return manager.find(ConfiguracaoEmail.class, id);
	}

	public List<ConfiguracaoEmail> buscarTodos() {
		String jpql = "SELECT c FROM ConfiguracaoEmail c ORDER BY c.emailDestinatario";
		TypedQuery<ConfiguracaoEmail> query = manager.createQuery(jpql, ConfiguracaoEmail.class);
		return query.getResultList();
	}

	public List<ConfiguracaoEmail> buscarAtivos() {
		String jpql = "SELECT c FROM ConfiguracaoEmail c WHERE c.ativo = true ORDER BY c.emailDestinatario";
		TypedQuery<ConfiguracaoEmail> query = manager.createQuery(jpql, ConfiguracaoEmail.class);
		return query.getResultList();
	}

	public List<ConfiguracaoEmail> buscarPorTipoNotificacao(TipoNotificacao tipo) {
		String jpql = "SELECT c FROM ConfiguracaoEmail c WHERE c.ativo = true AND "
				+ "(c.tipoNotificacao = :tipo OR c.tipoNotificacao = :ambos) " + "ORDER BY c.emailDestinatario";
		TypedQuery<ConfiguracaoEmail> query = manager.createQuery(jpql, ConfiguracaoEmail.class);
		query.setParameter("tipo", tipo);
		query.setParameter("ambos", TipoNotificacao.AMBOS);
		return query.getResultList();
	}

	@Transactional
	public void excluir(ConfiguracaoEmail config) {
		ConfiguracaoEmail gerenciado = manager.merge(config);
		manager.remove(gerenciado);
	}
}