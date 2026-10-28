package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.HistoricoCancelamentoCompra;
import com.augustopalhardi.manutencao.dao.HistoricoCancelamentoCompraDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

/**
 * Service para gerenciar o histórico de cancelamentos de compras já
 * processadas.
 */
@ApplicationScoped
public class ManterHistoricoCancelamentoCompraService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManterHistoricoCancelamentoCompraService.class);

	@Inject
	private HistoricoCancelamentoCompraDao dao;

	/**
	 * Registra o cancelamento de uma compra já processada, guardando os dados
	 * anteriores da compra para fins de auditoria.
	 */
	@Transactional
	public HistoricoCancelamentoCompra registrarCancelamento(Long solicitacaoCompraId, Double valorGastoAnterior,
			Double valorGastoMaterialAnterior, Double valorGastoServicoAnterior,
			String administradorProcessouAnterior, Date dataProcessamentoAnterior, String motivoCancelamento,
			String administradorCancelamento) {

		LOGGER.info("Registrando cancelamento de compra já processada - Solicitação ID: {}", solicitacaoCompraId);

		HistoricoCancelamentoCompra historico = new HistoricoCancelamentoCompra(solicitacaoCompraId,
				valorGastoAnterior, valorGastoMaterialAnterior, valorGastoServicoAnterior,
				administradorProcessouAnterior, dataProcessamentoAnterior, motivoCancelamento,
				administradorCancelamento);

		historico = dao.salvar(historico);

		LOGGER.info("Cancelamento de compra registrado no histórico com sucesso. ID: {}", historico.getId());

		return historico;
	}

	/**
	 * Busca o histórico de cancelamentos de uma solicitação de compra
	 */
	public List<HistoricoCancelamentoCompra> buscarHistoricoPorSolicitacaoCompra(Long solicitacaoCompraId) {
		return dao.buscarPorSolicitacaoCompra(solicitacaoCompraId);
	}

	/**
	 * Busca todos os históricos de cancelamento
	 */
	public List<HistoricoCancelamentoCompra> buscarTodos() {
		return dao.buscarTodos();
	}

	/**
	 * Busca histórico por ID
	 */
	public HistoricoCancelamentoCompra buscarPorId(Long id) {
		return dao.buscarPorId(id);
	}
}