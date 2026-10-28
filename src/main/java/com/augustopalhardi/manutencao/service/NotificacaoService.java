package com.augustopalhardi.manutencao.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.ConfiguracaoEmail;
import com.augustopalhardi.manutencao.dao.ConfiguracaoEmailDao;
import com.augustopalhardi.manutencao.util.Config;

/**
 * Serviço responsável por enviar notificações por email
 *
 * v3.0 - ENVIO EM SEGUNDO PLANO E EM LOTE
 *
 * Antes, o envio acontecia dentro da thread da requisição, no meio do salvar()
 * das telas de solicitação, e abria uma conexão SMTP NOVA para cada
 * destinatário. Com três administradores cadastrados mais o solicitante, eram
 * quatro handshakes TLS completos com autenticação, um atrás do outro, enquanto
 * o usuário olhava o botão travado — e os timeouts configurados aqui são de 10
 * segundos cada.
 *
 * Duas mudanças resolveram isso:
 *
 * 1. Os destinatários e o texto dos e-mails são resolvidos na thread da
 *    requisição (é uma consulta rápida), e só o envio propriamente dito vai para
 *    o ExecutorEmailService. O usuário recebe a tela de volta imediatamente.
 *
 * 2. O envio passou a ser em LOTE: uma única conexão SMTP é aberta e
 *    reaproveitada para todos os destinatários daquela notificação.
 *
 * REGRA IMPORTANTE ao mexer aqui: o que roda em segundo plano não pode tocar em
 * EntityManager, DAO, bean @RequestScoped ou FacesContext — nada disso existe
 * fora da requisição. Consulte o banco ANTES e passe Strings prontas adiante.
 *
 * @author Sistema de Manutenção
 * @version 3.0 - Envio assíncrono e em lote
 */
@ApplicationScoped
public class NotificacaoService {

	private static final Logger logger = LoggerFactory.getLogger(NotificacaoService.class);

	@Inject
	private ConfiguracaoEmailDao configuracaoEmailDao;

	// Envia os e-mails fora da thread da requisição
	@Inject
	private ExecutorEmailService executorEmailService;

	// ========================================
	// CONFIGURAÇÕES SMTP
	// Valores vêm do arquivo externo config.properties (ver classe Config).
	// Em produção: /etc/manutencao/config.properties
	// ========================================
	private static final String SMTP_HOST = Config.get("smtp.host");
	private static final String SMTP_PORT = Config.get("smtp.port");
	private static final String SMTP_USERNAME = Config.get("smtp.username");
	private static final String SMTP_PASSWORD = Config.get("smtp.password");
	private static final String SMTP_FROM = Config.get("smtp.from");

	/**
	 * Método existente - mantém compatibilidade com código antigo Chama o novo
	 * método passando null para emailSolicitante
	 */
	public void notificarNovaManutencao(String tipoManutencao, String descricao, String local, String solicitante) {
		notificarNovaManutencao(tipoManutencao, descricao, local, solicitante, null);
	}

	/**
	 * Notifica administradores E o solicitante.
	 *
	 * Este método agora RETORNA IMEDIATAMENTE. Ele apenas consulta os
	 * destinatários e monta os textos; o envio vai para segundo plano.
	 *
	 * @param tipoManutencao   Tipo da manutenção ("Manutenção de Máquina" ou
	 *                         "Manutenção Predial")
	 * @param descricao        Descrição do problema/falha reportada
	 * @param local            Nome do local ou máquina onde ocorreu o problema
	 * @param solicitante      Nome do usuário que reportou o problema
	 * @param emailSolicitante Email de quem criou a solicitação (pode ser null)
	 */
	public void notificarNovaManutencao(String tipoManutencao, String descricao, String local, String solicitante,
			String emailSolicitante) {
		try {
			logger.info("=== AGENDANDO NOTIFICAÇÕES ===");
			logger.info("Tipo: {}", tipoManutencao);
			logger.info("Local/Máquina: {}", local);
			logger.info("Solicitante: {}", solicitante);
			logger.info("Email Solicitante: {}", emailSolicitante != null ? emailSolicitante : "não fornecido");

			// ----------------------------------------------------------
			// 1. ADMINISTRADORES
			//
			// A consulta ao banco fica AQUI, na thread da requisição: o DAO
			// depende do EntityManager @RequestScoped, que não existe na thread
			// de segundo plano.
			// ----------------------------------------------------------
			List<String> destinatariosAdm = buscarDestinatarios(ConfiguracaoEmail.TipoNotificacao.NOVA_MANUTENCAO);

			if (destinatariosAdm.isEmpty()) {
				logger.warn("⚠️ Nenhum administrador configurado para receber notificações de: {}", tipoManutencao);
			} else {
				final String assuntoAdm = "🔧 Nova Solicitação de " + tipoManutencao;
				final String corpoAdm = construirEmailAdministrador(tipoManutencao, descricao, local, solicitante);

				logger.info("Agendando envio para {} administrador(es)", destinatariosAdm.size());
				executorEmailService.executar(() -> enviarLote(destinatariosAdm, assuntoAdm, corpoAdm, "ADM"));
			}

			// ----------------------------------------------------------
			// 2. SOLICITANTE
			// ----------------------------------------------------------
			if (emailSolicitante != null && !emailSolicitante.trim().isEmpty()) {
				final List<String> destinatarioSolicitante = new ArrayList<>();
				destinatarioSolicitante.add(emailSolicitante.trim());

				final String assuntoSol = "Solicitação de " + tipoManutencao + " Recebida";
				final String corpoSol = construirEmailSolicitante(tipoManutencao, descricao, local, solicitante);

				logger.info("Agendando confirmação para o solicitante");
				executorEmailService
						.executar(() -> enviarLote(destinatarioSolicitante, assuntoSol, corpoSol, "SOLICITANTE"));
			} else {
				logger.info("Email do solicitante não fornecido. Email de confirmação não será enviado.");
			}

			logger.info("=== NOTIFICAÇÕES AGENDADAS (envio segue em segundo plano) ===");

		} catch (Exception e) {
			logger.error("Erro ao agendar notificações de manutenção", e);
			// Não propaga a exceção - erro no envio de email não deve impactar cadastro
		}
	}

	/**
	 * Consulta no banco os e-mails cadastrados para um tipo de notificação.
	 *
	 * Roda SEMPRE na thread da requisição, porque usa o DAO.
	 */
	private List<String> buscarDestinatarios(ConfiguracaoEmail.TipoNotificacao tipo) {
		List<String> emails = new ArrayList<>();

		try {
			List<ConfiguracaoEmail> configuracoes = configuracaoEmailDao.buscarPorTipoNotificacao(tipo);

			if (configuracoes != null) {
				for (ConfiguracaoEmail config : configuracoes) {
					if (config.getEmailDestinatario() != null && !config.getEmailDestinatario().trim().isEmpty()) {
						emails.add(config.getEmailDestinatario().trim());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Erro ao buscar destinatários da notificação {}: ", tipo, e);
		}

		return emails;
	}

	/**
	 * Constrói o HTML do email para os administradores Template profissional com
	 * destaque vermelho
	 */
	private String construirEmailAdministrador(String tipoManutencao, String descricao, String local,
			String solicitante) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html>");
		html.append("<html><head>");
		html.append("<meta charset='UTF-8'>");
		html.append("<style>");
		html.append(
				"  body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
		html.append(
				"  .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
		html.append(
				"  .header { background: linear-gradient(135deg, #f44336 0%, #d32f2f 100%); color: white; padding: 30px; text-align: center; }");
		html.append("  .header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
		html.append("  .content { padding: 30px; }");
		html.append(
				"  .details { background-color: #fafafa; padding: 20px; margin: 20px 0; border-left: 4px solid #f44336; border-radius: 4px; }");
		html.append("  .detail-item { padding: 10px 0; border-bottom: 1px solid #e0e0e0; }");
		html.append("  .detail-item:last-child { border-bottom: none; }");
		html.append("  .label { font-weight: 600; color: #333; display: inline-block; min-width: 120px; }");
		html.append("  .value { color: #555; }");
		html.append(
				"  .footer { background-color: #f5f5f5; text-align: center; padding: 20px; color: #777; font-size: 12px; border-top: 1px solid #e0e0e0; }");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class='container'>");

		// Cabeçalho
		html.append("<div class='header'>");
		html.append("<h1>🔧 Nova Solicitação de Manutenção</h1>");
		html.append("</div>");

		// Conteúdo
		html.append("<div class='content'>");
		html.append("<p style='font-size: 16px; color: #333;'>");
		html.append("Uma nova solicitação de manutenção foi registrada no sistema e requer sua atenção.");
		html.append("</p>");

		// Detalhes
		html.append("<div class='details'>");
		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Tipo:</span>");
		html.append("<span class='value'>").append(tipoManutencao).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Local/Máquina:</span>");
		html.append("<span class='value'>").append(local).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Solicitante:</span>");
		html.append("<span class='value'>").append(solicitante).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Descrição:</span>");
		html.append("<span class='value'>").append(descricao).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Data/Hora:</span>");
		html.append("<span class='value'>").append(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss").format(new Date()))
				.append("</span>");
		html.append("</div>");
		html.append("</div>");

		html.append("<p style='margin-top: 25px; color: #555;'>");
		html.append("⚠️ Acesse o sistema para visualizar detalhes completos e atribuir um técnico.");
		html.append("</p>");
		html.append("</div>");

		// Rodapé
		html.append("<div class='footer'>");
		html.append("<p style='margin: 0;'>Sistema de Gerenciamento de Manutenção</p>");
		html.append("<p style='margin: 5px 0 0 0;'>Este é um email automático. Por favor, não responda.</p>");
		html.append("</div>");

		html.append("</div>");
		html.append("</body></html>");

		return html.toString();
	}

	/**
	 * Constrói o HTML do email para o solicitante Template profissional com
	 * destaque verde (confirmação)
	 */
	private String construirEmailSolicitante(String tipoManutencao, String descricao, String local,
			String solicitante) {
		StringBuilder html = new StringBuilder();

		html.append("<!DOCTYPE html>");
		html.append("<html><head>");
		html.append("<meta charset='UTF-8'>");
		html.append("<style>");
		html.append(
				"  body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }");
		html.append(
				"  .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
		html.append(
				"  .header { background: linear-gradient(135deg, #4CAF50 0%, #388E3C 100%); color: white; padding: 30px; text-align: center; }");
		html.append("  .header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
		html.append("  .content { padding: 30px; }");
		html.append("  .greeting { font-size: 18px; color: #333; margin-bottom: 15px; }");
		html.append("  .message { font-size: 16px; color: #555; line-height: 1.6; margin-bottom: 25px; }");
		html.append(
				"  .details { background-color: #f9f9f9; padding: 20px; margin: 20px 0; border-left: 4px solid #4CAF50; border-radius: 4px; }");
		html.append("  .detail-item { padding: 10px 0; border-bottom: 1px solid #e0e0e0; }");
		html.append("  .detail-item:last-child { border-bottom: none; }");
		html.append("  .label { font-weight: 600; color: #333; display: inline-block; min-width: 100px; }");
		html.append("  .value { color: #555; }");
		html.append(
				"  .info-box { background-color: #E3F2FD; padding: 20px; border-radius: 6px; margin: 25px 0; border-left: 4px solid #2196F3; }");
		html.append(
				"  .footer { background-color: #f5f5f5; text-align: center; padding: 20px; color: #777; font-size: 12px; border-top: 1px solid #e0e0e0; }");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class='container'>");

		// Cabeçalho
		html.append("<div class='header'>");
		html.append("<h1>Solicitação Recebida!</h1>");
		html.append("</div>");

		// Conteúdo
		html.append("<div class='content'>");
		html.append("<p class='greeting'>Olá, <strong>").append(solicitante).append("</strong>!</p>");

		html.append("<p class='message'>");
		html.append("Sua solicitação de manutenção foi <strong style='color: #4CAF50;'>recebida com sucesso</strong> ");
		html.append("e está sendo processada por nossa equipe técnica.");
		html.append("</p>");

		// Detalhes da solicitação
		html.append("<div class='details'>");
		html.append("<h3 style='margin: 0 0 15px 0; color: #333; font-size: 18px;'>📋 Detalhes da Solicitação</h3>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Tipo:</span>");
		html.append("<span class='value'>").append(tipoManutencao).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Local/Máquina:</span>");
		html.append("<span class='value'>").append(local).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Descrição:</span>");
		html.append("<span class='value'>").append(descricao).append("</span>");
		html.append("</div>");

		html.append("<div class='detail-item'>");
		html.append("<span class='label'>Registrado em:</span>");
		html.append("<span class='value'>").append(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(new Date()))
				.append("</span>");
		html.append("</div>");
		html.append("</div>");

		// Informações adicionais
		html.append("<div class='info-box'>");
		html.append("<p style='margin: 0; color: #1976D2; font-size: 15px; line-height: 1.6;'>");
		html.append("<strong>ℹ️ Próximos passos:</strong><br><br>");
		html.append("Nossa equipe técnica irá avaliar sua solicitação e retornará em breve com mais informações. ");
		html.append("Você pode acompanhar o status da manutenção através do sistema. ");
		html.append("</p>");
		html.append("</div>");

		html.append("<p style='color: #777; margin-top: 25px; font-size: 14px;'>");
		html.append("Obrigado por utilizar nosso sistema de manutenção!");
		html.append("</p>");
		html.append("</div>");

		// Rodapé
		html.append("<div class='footer'>");
		html.append("<p style='margin: 0;'><strong>Sistema de Gerenciamento de Manutenção</strong></p>");
		html.append("<p style='margin: 5px 0 0 0;'>Este é um email automático. Por favor, não responda.</p>");
		html.append("</div>");

		html.append("</div>");
		html.append("</body></html>");

		return html.toString();
	}

	// ========================================
	// ENVIO PROPRIAMENTE DITO
	//
	// Tudo daqui para baixo roda em SEGUNDO PLANO. Só pode depender de String,
	// das constantes SMTP e do JavaMail — nada de DAO, EntityManager ou CDI
	// com escopo de requisição.
	// ========================================

	/** Monta a sessão SMTP. Barato: o custo real está em conectar. */
	private Session criarSessao() {
		Properties props = new Properties();
		props.put("mail.smtp.host", SMTP_HOST);
		props.put("mail.smtp.port", SMTP_PORT);
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.ssl.trust", SMTP_HOST);
		props.put("mail.smtp.ssl.protocols", "TLSv1.2");
		props.put("mail.smtp.connectiontimeout", "10000"); // 10 segundos
		props.put("mail.smtp.timeout", "10000");
		props.put("mail.smtp.writetimeout", "10000");

		return Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
			}
		});
	}

	/**
	 * Envia a mesma mensagem para vários destinatários reaproveitando UMA
	 * conexão SMTP.
	 *
	 * O código antigo chamava Transport.send() por destinatário, e cada chamada
	 * abria conexão, negociava TLS, autenticava, enviava e fechava. Com quatro
	 * destinatários eram quatro handshakes completos.
	 *
	 * Cada destinatário recebe a própria mensagem (e não uma cópia com todos no
	 * campo Para), preservando a privacidade dos endereços; o que se reaproveita
	 * é apenas a conexão.
	 *
	 * A falha em um destinatário não interrompe os demais.
	 */
	private void enviarLote(List<String> destinatarios, String assunto, String corpoHtml, String rotuloLog) {
		if (destinatarios == null || destinatarios.isEmpty()) {
			return;
		}

		long inicio = System.currentTimeMillis();
		Session session = criarSessao();
		Transport transport = null;
		int sucessos = 0;
		int falhas = 0;

		try {
			transport = session.getTransport("smtp");
			transport.connect(SMTP_HOST, SMTP_USERNAME, SMTP_PASSWORD);

			for (String destinatario : destinatarios) {
				try {
					MimeMessage message = new MimeMessage(session);
					message.setFrom(new InternetAddress(SMTP_FROM));
					message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
					// O assunto tem emoji e acento: sem informar o charset, alguns
					// clientes exibem caracteres quebrados.
					message.setSubject(assunto, "UTF-8");
					message.setContent(corpoHtml, "text/html; charset=utf-8");
					message.setSentDate(new Date());

					transport.sendMessage(message, message.getAllRecipients());

					logger.info("  [{}] Email enviado para: {}", rotuloLog, destinatario);
					sucessos++;
				} catch (Exception e) {
					logger.error("  ❌ [{}] Erro ao enviar para: {}", rotuloLog, destinatario, e);
					falhas++;
				}
			}

		} catch (Exception e) {
			logger.error("[{}] Erro ao conectar no servidor SMTP. Nenhum email deste lote foi enviado.", rotuloLog, e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (Exception e) {
					logger.warn("[{}] Erro ao fechar conexão SMTP: {}", rotuloLog, e.getMessage());
				}
			}
		}

		logger.info("[{}] Lote concluído em {} ms - {} sucesso(s), {} falha(s)", rotuloLog,
				System.currentTimeMillis() - inicio, sucessos, falhas);
	}

	// ========================================
	// MÉTODOS ADICIONAIS (mantidos para compatibilidade)
	// ========================================

	/**
	 * Notifica sobre solicitação de compra de materiais/serviços.
	 *
	 * Mesma estratégia — consulta os destinatários aqui e manda o
	 * envio para segundo plano.
	 */
	public void notificarSolicitacaoCompra(String tipoSolicitacao, String descricaoSolicitacao, String nomeReferencia,
			Long idManutencao, String tipoManutencao, String tecnicoSolicitante) {
		try {
			logger.info("Agendando notificação de solicitação de compra...");

			List<String> destinatarios = buscarDestinatarios(ConfiguracaoEmail.TipoNotificacao.SOLICITACAO_COMPRA);

			if (destinatarios.isEmpty()) {
				logger.warn("Nenhum email configurado para receber notificações de solicitação de compra");
				return;
			}

			final String assunto = "🛒 Nova Solicitação de Compra - " + tipoSolicitacao;
			final String corpo = construirEmailSolicitacaoCompra(tipoSolicitacao, descricaoSolicitacao, nomeReferencia,
					idManutencao, tipoManutencao, tecnicoSolicitante);

			executorEmailService.executar(() -> enviarLote(destinatarios, assunto, corpo, "COMPRA"));

		} catch (Exception e) {
			logger.error("Erro ao agendar notificação de solicitação de compra", e);
		}
	}

	/**
	 * Constrói email para solicitação de compra
	 */
	private String construirEmailSolicitacaoCompra(String tipoSolicitacao, String descricaoSolicitacao,
			String nomeReferencia, Long idManutencao, String tipoManutencao, String tecnicoSolicitante) {
		StringBuilder html = new StringBuilder();
		html.append("<html><body style='font-family: Arial, sans-serif;'>");
		html.append("<div style='max-width: 600px; margin: 0 auto;'>");
		html.append("<h2 style='color: #FF9800;'>🛒 Nova Solicitação de Compra</h2>");
		html.append("<div style='background-color: #fff3e0; padding: 15px; border-left: 4px solid #FF9800;'>");
		html.append("<p><strong>Tipo:</strong> ").append(tipoSolicitacao).append("</p>");
		html.append("<p><strong>Descrição:</strong> ").append(descricaoSolicitacao).append("</p>");
		html.append("<p><strong>Referência:</strong> ").append(nomeReferencia).append("</p>");
		html.append("<p><strong>ID Manutenção:</strong> ").append(idManutencao).append("</p>");
		html.append("<p><strong>Tipo Manutenção:</strong> ").append(tipoManutencao).append("</p>");
		html.append("<p><strong>Solicitado por:</strong> ").append(tecnicoSolicitante).append("</p>");
		html.append("<p><strong>Data:</strong> ")
				.append(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(new Date())).append("</p>");
		html.append("</div>");
		html.append("<p style='margin-top: 20px;'>Acesse o sistema para processar esta solicitação.</p>");
		html.append("<hr><p style='color: #666; font-size: 12px; text-align: center;'>");
		html.append("Email automático - Não responda</p>");
		html.append("</div></body></html>");

		return html.toString();
	}
}