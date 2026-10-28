package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.HistoricoReprogramacao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.dao.HistoricoReprogramacaoDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

/**
 * Service para gerenciar reprogramações de manutenções
 */
public class ManterHistoricoReprogramacaoService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManterHistoricoReprogramacaoService.class);

	@Inject
	private HistoricoReprogramacaoDao dao;

	/**
	 * Registra uma nova reprogramação
	 */
	@Transactional
	public HistoricoReprogramacao registrarReprogramacao(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao,
			Date dataAnterior, Date dataNova, String motivoReprogramacao, String usuarioReprogramacao) {

		LOGGER.info("Registrando reprogramação para solicitação {} do tipo {}", solicitacaoId, tipoManutencao);

		HistoricoReprogramacao historico = new HistoricoReprogramacao(solicitacaoId, tipoManutencao, dataAnterior,
				dataNova, motivoReprogramacao, usuarioReprogramacao);

		historico = dao.salvar(historico);

		LOGGER.info("Reprogramação registrada com sucesso. ID: {}", historico.getId());

		return historico;
	}

	/**
	 * Busca histórico de uma solicitação
	 */
	public List<HistoricoReprogramacao> buscarHistoricoPorSolicitacao(Long solicitacaoId,
			TipoManutencaoReferencia tipoManutencao) {

		return dao.buscarPorSolicitacao(solicitacaoId, tipoManutencao);
	}

	/**
	 * Busca a última reprogramação
	 */
	public HistoricoReprogramacao buscarUltimaReprogramacao(Long solicitacaoId,
			TipoManutencaoReferencia tipoManutencao) {

		return dao.buscarUltimaReprogramacao(solicitacaoId, tipoManutencao);
	}

	/**
	 * Conta quantas reprogramações uma solicitação teve
	 */
	public Long contarReprogramacoes(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao) {
		return dao.contarReprogramacoes(solicitacaoId, tipoManutencao);
	}

	/**
	 * Busca todos os históricos
	 */
	public List<HistoricoReprogramacao> buscarTodos() {
		return dao.buscarTodos();
	}

	/**
	 * Busca histórico por ID
	 */
	public HistoricoReprogramacao buscarPorId(Long id) {
		return dao.buscarPorId(id);
	}
}