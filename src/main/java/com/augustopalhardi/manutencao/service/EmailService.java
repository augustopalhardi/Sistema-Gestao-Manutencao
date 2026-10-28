package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.util.Config;

/**
 * Serviço responsável por enviar emails de notificação do sistema
 */
@ApplicationScoped
public class EmailService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

	// Configurações do servidor de email.
	// Os valores vêm do arquivo externo config.properties (ver classe Config).
	// Em produção: /etc/manutencao/config.properties
	private static final String SMTP_HOST = Config.get("smtp.host");
	private static final String SMTP_PORT = Config.get("smtp.port");
	private static final String EMAIL_FROM = Config.get("smtp.from");
	private static final String EMAIL_PASSWORD = Config.get("smtp.password");
	private static final boolean USE_TLS = true;

	/**
	 * Envia email de notificação de nova manutenção
	 */
	public boolean enviarEmailNovaManutencao(String emailDestino, String tipoManutencao, String descricao,
			String solicitante) {
		try {
			String assunto = "Nova Solicitação de Manutenção - " + tipoManutencao;

			StringBuilder corpo = new StringBuilder();
			corpo.append("<html><body style='font-family: Arial, sans-serif;'>");
			corpo.append("<h2 style='color: #2c3e50;'>Nova Solicitação de Manutenção</h2>");
			corpo.append("<div style='background-color: #f8f9fa; padding: 15px; border-left: 4px solid #007bff;'>");
			corpo.append("<p><strong>Tipo:</strong> ").append(tipoManutencao).append("</p>");
			corpo.append("<p><strong>Solicitante:</strong> ").append(solicitante).append("</p>");
			corpo.append("<p><strong>Descrição:</strong></p>");
			corpo.append("<p style='white-space: pre-wrap;'>").append(descricao).append("</p>");
			corpo.append("</div>");
			corpo.append("<p style='color: #6c757d; margin-top: 20px;'>Acesse o sistema para mais detalhes.</p>");
			corpo.append("</body></html>");

			return enviarEmail(emailDestino, assunto, corpo.toString());

		} catch (Exception e) {
			LOGGER.error("Erro ao enviar email de nova manutenção: ", e);
			return false;
		}
	}

	/**
	 * Envia email de notificação de nova solicitação de compra
	 */
	public boolean enviarEmailSolicitacaoCompra(String emailDestino, String tipoSolicitacao, String descricao,
			String tecnicoSolicitante, Long manutencaoId, String tipoManutencao) {
		try {
			String assunto = "Nova Solicitação de Compra - " + tipoSolicitacao;

			StringBuilder corpo = new StringBuilder();
			corpo.append("<html><body style='font-family: Arial, sans-serif;'>");
			corpo.append("<h2 style='color: #f39c12;'>Nova Solicitação de Compra</h2>");
			corpo.append("<div style='background-color: #fff3cd; padding: 15px; border-left: 4px solid #f39c12;'>");
			corpo.append("<p><strong>Tipo de Solicitação:</strong> ").append(tipoSolicitacao).append("</p>");
			corpo.append("<p><strong>Técnico Solicitante:</strong> ").append(tecnicoSolicitante).append("</p>");
			corpo.append("<p><strong>Manutenção ID:</strong> ").append(manutencaoId).append(" (").append(tipoManutencao)
					.append(")</p>");
			corpo.append("<p><strong>Descrição Detalhada:</strong></p>");
			corpo.append("<p style='white-space: pre-wrap;'>").append(descricao).append("</p>");
			corpo.append("</div>");
			corpo.append(
					"<div style='background-color: #fff3e0; padding: 12px; margin-top: 15px; border-left: 3px solid #ff9800;'>");
			corpo.append("<p style='margin: 0; color: #e65100;'><strong>⚠️ Atenção:</strong> ");
			corpo.append(
					"A manutenção está aguardando a compra dos materiais/serviços e foi temporariamente pausada.</p>");
			corpo.append("</div>");
			corpo.append(
					"<p style='color: #6c757d; margin-top: 20px;'>Acesse o sistema para processar a solicitação.</p>");
			corpo.append("</body></html>");

			return enviarEmail(emailDestino, assunto, corpo.toString());

		} catch (Exception e) {
			LOGGER.error("Erro ao enviar email de solicitação de compra: ", e);
			return false;
		}
	}

	/**
	 * Método genérico para enviar emails
	 */
	private boolean enviarEmail(String destinatario, String assunto, String corpoHtml) {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.host", SMTP_HOST);
			props.put("mail.smtp.port", SMTP_PORT);
			props.put("mail.smtp.auth", "true");

			if (USE_TLS) {
				props.put("mail.smtp.starttls.enable", "true");
			}

			Session session = Session.getInstance(props, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
				}
			});

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EMAIL_FROM));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
			message.setSubject(assunto);
			message.setContent(corpoHtml, "text/html; charset=utf-8");

			Transport.send(message);

			LOGGER.info("Email enviado com sucesso para: " + destinatario);
			return true;

		} catch (MessagingException e) {
			LOGGER.error("Erro ao enviar email para " + destinatario + ": ", e);
			return false;
		}
	}

	/**
	 * Valida se um endereço de email é válido
	 */
	public boolean validarEmail(String email) {
		if (email == null || email.trim().isEmpty()) {
			return false;
		}

		String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
		return email.matches(emailRegex);
	}
}