package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.dao.CadastroMaquinaEquipamentoDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterCadastroMaquinaEquipamentoService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private CadastroMaquinaEquipamentoDao cadastroMaquinaEquipamentoDao;

	@Transactional
	public CadastroMaquinaEquipamento salvar(CadastroMaquinaEquipamento cadastroMaquinaEquipamento) {
		return cadastroMaquinaEquipamentoDao.salvar(cadastroMaquinaEquipamento);
	}

	public CadastroMaquinaEquipamento buscarPorId(Long maquinaEquipamentoId) {
		return cadastroMaquinaEquipamentoDao.buscarPeloCodigo(maquinaEquipamentoId);
	}

	public List<CadastroMaquinaEquipamento> buscarTodos() {
		return cadastroMaquinaEquipamentoDao.buscarTodos();
	}

	// Buscar apenas máquinas/equipamentos ativos
	public List<CadastroMaquinaEquipamento> buscarAtivos() {
		return cadastroMaquinaEquipamentoDao.buscarAtivos();
	}

	// Alternar status ativo/inativo
	@Transactional
	public CadastroMaquinaEquipamento alternarStatus(CadastroMaquinaEquipamento maquina) {
		maquina.setAtivo(!maquina.isAtivo());
		return cadastroMaquinaEquipamentoDao.salvar(maquina);
	}

	public CadastroMaquinaEquipamento buscarPorNumeroPatrimonio(String numeroPatrimonio) {
		return cadastroMaquinaEquipamentoDao.buscarPorNumeroPatrimonio(numeroPatrimonio);
	}

	public boolean existeNumeroPatrimonio(String numeroPatrimonio, Long idExcluir) {
		return cadastroMaquinaEquipamentoDao.existeNumeroPatrimonio(numeroPatrimonio, idExcluir);
	}

	public List<CadastroMaquinaEquipamento> buscarPorFabricante(String fabricante) {
		return cadastroMaquinaEquipamentoDao.buscarPorFabricante(fabricante);
	}

	public List<CadastroMaquinaEquipamento> buscarPorModelo(String modelo) {
		return cadastroMaquinaEquipamentoDao.buscarPorModelo(modelo);
	}

	public List<CadastroMaquinaEquipamento> buscarPorTipo(String tipo) {
		return cadastroMaquinaEquipamentoDao.buscarPorTipo(tipo);
	}

	public CadastroMaquinaEquipamento buscarPorAnoFabricacao(Date anoFabricacao) {
		return cadastroMaquinaEquipamentoDao.buscarPorAnoFabricacao(anoFabricacao);
	}

	@Transactional
	public void excluir(CadastroMaquinaEquipamento cadastroMaquinaEquipamento) {
		cadastroMaquinaEquipamentoDao.excluir(cadastroMaquinaEquipamento);
	}
}