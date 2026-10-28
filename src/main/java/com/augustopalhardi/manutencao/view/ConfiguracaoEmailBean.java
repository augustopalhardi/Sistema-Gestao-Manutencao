package com.augustopalhardi.manutencao.view;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

import com.augustopalhardi.manutencao.model.ConfiguracaoEmail.TipoNotificacao;
import com.augustopalhardi.manutencao.model.ConfiguracaoEmail;
import com.augustopalhardi.manutencao.service.ManterConfiguracaoEmailService;

@Named
@ViewScoped
public class ConfiguracaoEmailBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(ConfiguracaoEmailBean.class);

	@Inject
	private ManterConfiguracaoEmailService service;

	private List<ConfiguracaoEmail> configuracoes;
	private ConfiguracaoEmail configuracaoSelecionada;

	// Campos do formulário
	private String emailDestinatario;
	private TipoNotificacao tipoNotificacao;
	private String descricao;

	public void init() {
		carregarConfiguracoes();
	}

	public void carregarConfiguracoes() {
		try {
			configuracoes = service.buscarTodos();
			LOGGER.debug("Configurações carregadas: " + configuracoes.size());
		} catch (Exception e) {
			LOGGER.error("Erro ao carregar configurações: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao carregar configurações de email"));
		}
	}

	public void prepararNovo() {
		limparCampos();
		LOGGER.debug("Formulário preparado para nova configuração");
	}

	public void salvar() {
		try {
			ConfiguracaoEmail config = new ConfiguracaoEmail();
			config.setEmailDestinatario(emailDestinatario.trim());
			config.setTipoNotificacao(tipoNotificacao);
			config.setDescricao(descricao != null ? descricao.trim() : null);
			config.setAtivo(true);

			service.salvar(config);

			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Configuração de email cadastrada com sucesso!"));

			limparCampos();
			carregarConfiguracoes();

		} catch (IllegalArgumentException e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", e.getMessage()));
		} catch (Exception e) {
			LOGGER.error("Erro ao salvar configuração: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao salvar configuração de email"));
		}
	}

	public void prepararEdicao(ConfiguracaoEmail config) {
		this.configuracaoSelecionada = config;
		this.emailDestinatario = config.getEmailDestinatario();
		this.tipoNotificacao = config.getTipoNotificacao();
		this.descricao = config.getDescricao();
		LOGGER.debug("Configuração preparada para edição: " + config.getId());
	}

	public void atualizar() {
		try {
			configuracaoSelecionada.setEmailDestinatario(emailDestinatario.trim());
			configuracaoSelecionada.setTipoNotificacao(tipoNotificacao);
			configuracaoSelecionada.setDescricao(descricao != null ? descricao.trim() : null);

			service.salvar(configuracaoSelecionada);

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração atualizada com sucesso!"));

			limparCampos();
			carregarConfiguracoes();

		} catch (Exception e) {
			LOGGER.error("Erro ao atualizar configuração: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao atualizar configuração"));
		}
	}

	public void alternarStatus(ConfiguracaoEmail config) {
		try {
			service.alternarStatus(config.getId());

			String status = config.getAtivo() ? "ativada" : "desativada";
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso",
					"Configuração " + status + " com sucesso!"));

			carregarConfiguracoes();

		} catch (Exception e) {
			LOGGER.error("Erro ao alternar status: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao alterar status da configuração"));
		}
	}

	public void excluir(ConfiguracaoEmail config) {
		try {
			service.excluir(config.getId());

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", "Configuração excluída com sucesso!"));

			carregarConfiguracoes();

		} catch (Exception e) {
			LOGGER.error("Erro ao excluir configuração: ", e);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Erro ao excluir configuração"));
		}
	}

	private void limparCampos() {
		this.emailDestinatario = null;
		this.tipoNotificacao = null;
		this.descricao = null;
		this.configuracaoSelecionada = null;
	}

	// Getters e Setters
	public List<ConfiguracaoEmail> getConfiguracoes() {
		return configuracoes;
	}

	public String getEmailDestinatario() {
		return emailDestinatario;
	}

	public void setEmailDestinatario(String emailDestinatario) {
		this.emailDestinatario = emailDestinatario;
	}

	public TipoNotificacao getTipoNotificacao() {
		return tipoNotificacao;
	}

	public void setTipoNotificacao(TipoNotificacao tipoNotificacao) {
		this.tipoNotificacao = tipoNotificacao;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public TipoNotificacao[] getTiposNotificacao() {
		return TipoNotificacao.values();
	}

	public ConfiguracaoEmail getConfiguracaoSelecionada() {
		return configuracaoSelecionada;
	}
}