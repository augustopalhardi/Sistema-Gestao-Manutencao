package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.augustopalhardi.manutencao.model.CadastroLocal;
import com.augustopalhardi.manutencao.model.SolicitacaoManutencaoPredial;
import com.augustopalhardi.manutencao.model.TipoManutencao;
import com.augustopalhardi.manutencao.dao.SolicitacaoManutencaoPredialDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterSolicitacaoManutencaoPredialService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private SolicitacaoManutencaoPredialDao solicitacaoManutencaoPredialDao;

	@Transactional
	public SolicitacaoManutencaoPredial salvar(SolicitacaoManutencaoPredial solicitacaoManutencaoPredial) {
		return solicitacaoManutencaoPredialDao.salvar(solicitacaoManutencaoPredial);
	}

	public SolicitacaoManutencaoPredial buscarPorId(Long solicitacaoId) {
		return solicitacaoManutencaoPredialDao.buscarPeloCodigo(solicitacaoId);
	}

	public List<SolicitacaoManutencaoPredial> buscarTodos() {
		return solicitacaoManutencaoPredialDao.buscarTodos();
	}

	// Novos métodos para buscar solicitações ativas e canceladas
	public List<SolicitacaoManutencaoPredial> buscarAtivas() {
		return solicitacaoManutencaoPredialDao.buscarAtivas();
	}

	public List<SolicitacaoManutencaoPredial> buscarCanceladas() {
		return solicitacaoManutencaoPredialDao.buscarCanceladas();
	}

	public List<SolicitacaoManutencaoPredial> buscarPorProblema(String problema) {
		return solicitacaoManutencaoPredialDao.buscarPorProblema(problema);
	}

	public List<SolicitacaoManutencaoPredial> buscarPorLocal(CadastroLocal local) {
		return solicitacaoManutencaoPredialDao.buscarPorLocal(local);
	}

	public List<SolicitacaoManutencaoPredial> buscarPorRiscoSeguranca(boolean riscoSeguranca) {
		return solicitacaoManutencaoPredialDao.buscarPorRiscoSeguranca(riscoSeguranca);
	}

	public List<SolicitacaoManutencaoPredial> buscarPorUrgencia(boolean urgente) {
		return solicitacaoManutencaoPredialDao.buscarPorUrgencia(urgente);
	}

	@Transactional
	public void excluir(SolicitacaoManutencaoPredial solicitacaoManutencaoPredial) {
		solicitacaoManutencaoPredialDao.excluir(solicitacaoManutencaoPredial);
	}

	public List<SolicitacaoManutencaoPredial> buscarPorTipoManutencao(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoPredialDao.buscarPorTipoManutencao(tipoManutencao);
	}

	public List<SolicitacaoManutencaoPredial> buscarAtivasPorTipo(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoPredialDao.buscarAtivasPorTipo(tipoManutencao);
	}

	public Long contarPorTipo(TipoManutencao tipoManutencao) {
		return solicitacaoManutencaoPredialDao.contarPorTipo(tipoManutencao);
	}
}