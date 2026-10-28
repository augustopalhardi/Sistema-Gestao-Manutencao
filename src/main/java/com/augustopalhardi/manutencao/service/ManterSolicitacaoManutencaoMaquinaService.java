package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.CadastroMaquinaEquipamento;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoMaquina;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.dao.SolicitacaoManutencaoMaquinaDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterSolicitacaoManutencaoMaquinaService implements Serializable {
	private static final long serialVersionUID = 1L;

	@Inject
	private SolicitacaoManutencaoMaquinaDao solicitacaoManutencaoMaquinaDao;

	@Transactional
	public SolicitacaoManutencaoMaquina salvar(SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina) {
		return solicitacaoManutencaoMaquinaDao.salvar(solicitacaoManutencaoMaquina);
	}

	public SolicitacaoManutencaoMaquina buscarPorId(Long solicitacaoId) {
		return solicitacaoManutencaoMaquinaDao.buscarPeloCodigo(solicitacaoId);
	}

	public List<SolicitacaoManutencaoMaquina> buscarTodos() {
		return solicitacaoManutencaoMaquinaDao.buscarTodos();
	}

	// Novos métodos para buscar solicitações ativas e canceladas
	public List<SolicitacaoManutencaoMaquina> buscarAtivas() {
		return solicitacaoManutencaoMaquinaDao.buscarAtivas();
	}

	public List<SolicitacaoManutencaoMaquina> buscarCanceladas() {
		return solicitacaoManutencaoMaquinaDao.buscarCanceladas();
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorFalha(String falha) {
		return solicitacaoManutencaoMaquinaDao.buscarPorFalha(falha);
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorMaquina(CadastroMaquinaEquipamento maquina) {
		return solicitacaoManutencaoMaquinaDao.buscarPorMaquina(maquina);
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorRisco(boolean risco) {
		return solicitacaoManutencaoMaquinaDao.buscarPorRisco(risco);
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorOperando(boolean operando) {
		return solicitacaoManutencaoMaquinaDao.buscarPorOperando(operando);
	}

	@Transactional
	public void excluir(SolicitacaoManutencaoMaquina solicitacaoManutencaoMaquina) {
		solicitacaoManutencaoMaquinaDao.excluir(solicitacaoManutencaoMaquina);
	}

	public List<SolicitacaoManutencaoMaquina> buscarPorTipoManutencao(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoMaquinaDao.buscarPorTipoManutencao(tipoManutencao);
	}

	public List<SolicitacaoManutencaoMaquina> buscarAtivasPorTipo(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoMaquinaDao.buscarAtivasPorTipo(tipoManutencao);
	}

	public Long contarPorTipo(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoMaquinaDao.contarPorTipo(tipoManutencao);
	}
}