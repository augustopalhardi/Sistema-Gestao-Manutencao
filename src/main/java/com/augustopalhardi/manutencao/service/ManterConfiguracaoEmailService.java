package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.ConfiguracaoEmail;
import com.augustopalhardi.manutencao.model.ConfiguracaoEmail.TipoNotificacao;
import com.augustopalhardi.manutencao.dao.ConfiguracaoEmailDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterConfiguracaoEmailService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManterConfiguracaoEmailService.class);

	@Inject
	private ConfiguracaoEmailDao dao;

	@Inject
	private EmailService emailService;

	@Transactional
	public ConfiguracaoEmail salvar(ConfiguracaoEmail config) {
		try {
			// Validar email
			if (!emailService.validarEmail(config.getEmailDestinatario())) {
				throw new IllegalArgumentException("Email inválido");
			}

			config.setDataAtualizacao(new Date());

			ConfiguracaoEmail salvo = dao.salvar(config);
			LOGGER.info("Configuração de email salva: " + salvo.getEmailDestinatario());
			return salvo;

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar configuração de email: ", e);
			throw e;
		}
	}

	public ConfiguracaoEmail buscarPorId(Long id) {
		return dao.buscarPorId(id);
	}

	public List<ConfiguracaoEmail> buscarTodos() {
		return dao.buscarTodos();
	}

	public List<ConfiguracaoEmail> buscarAtivos() {
		return dao.buscarAtivos();
	}

	public List<ConfiguracaoEmail> buscarPorTipoNotificacao(TipoNotificacao tipo) {
		return dao.buscarPorTipoNotificacao(tipo);
	}

	@Transactional
	public void excluir(Long id) {
		try {
			ConfiguracaoEmail config = dao.buscarPorId(id);
			if (config != null) {
				dao.excluir(config);
				LOGGER.info("Configuração de email excluída: " + config.getEmailDestinatario());
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao excluir configuração de email: ", e);
			throw e;
		}
	}

	@Transactional
	public void alternarStatus(Long id) {
		try {
			ConfiguracaoEmail config = dao.buscarPorId(id);
			if (config != null) {
				config.setAtivo(!config.getAtivo());
				config.setDataAtualizacao(new Date());
				dao.salvar(config);
				LOGGER.info("Status alterado para " + config.getAtivo() + " - Email: " + config.getEmailDestinatario());
			}
		} catch (Exception e) {
			LOGGER.error("Erro ao alternar status: ", e);
			throw e;
		}
	}
}