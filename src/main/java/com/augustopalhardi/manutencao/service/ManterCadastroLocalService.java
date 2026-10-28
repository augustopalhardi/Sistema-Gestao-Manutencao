package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.dao.CadastroLocalDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterCadastroLocalService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private CadastroLocalDao cadastroLocalDao;

	@Transactional
	public CadastroLocal salvar(CadastroLocal cadastroLocal) {
		return cadastroLocalDao.salvar(cadastroLocal);
	}

	public CadastroLocal buscarPorId(Long localId) {
		return cadastroLocalDao.buscarPeloCodigo(localId);
	}

	public List<CadastroLocal> buscarTodos() {
		return cadastroLocalDao.buscarTodos();
	}

	/**
	 * Locais ativos, ordenados por nome.
	 */
	public List<CadastroLocal> buscarAtivos() {
		return cadastroLocalDao.buscarAtivos();
	}

	public CadastroLocal buscarPorNome(String nome) {
		return cadastroLocalDao.buscarPorNome(nome);
	}

	public List<CadastroLocal> buscarPorArea(int area) {
		return cadastroLocalDao.buscarPorArea(area);
	}

	/**
	 * Inverte o status do local e persiste a mudança.
	 *
	 * O critério de inversão é o isAtivo() da entidade, que trata nulo como
	 * inativo: um registro antigo com "ativo" nulo é exibido como ativo nas
	 * telas, mas ao ser alternado pela primeira vez vira ativo, não inativo.
	 */
	@Transactional
	public CadastroLocal alternarStatus(CadastroLocal local) {
		local.setAtivo(!local.isAtivo());
		return cadastroLocalDao.salvar(local);
	}

	@Transactional
	public void excluir(CadastroLocal cadastroLocal) {
		cadastroLocalDao.excluir(cadastroLocal);
	}
}
