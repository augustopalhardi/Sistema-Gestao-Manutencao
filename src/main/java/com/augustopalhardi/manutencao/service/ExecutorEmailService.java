package com.augustopalhardi.manutencao.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executa envios de e-mail FORA da thread da requisição.
 *
 * Por que existe: o envio de e-mail era feito no meio do salvar() das telas de
 * solicitação. O registro já estava gravado no banco, mas a página só voltava
 * para o usuário depois que o servidor SMTP respondesse — e cada destinatário
 * abria uma conexão SMTP nova, com handshake TLS e autenticação. Com os
 * timeouts de 10 segundos configurados no NotificacaoService, uma falha de rede
 * podia deixar a tela travada por bem mais de meio minuto.
 *
 * Agora o salvamento devolve a tela na hora e o e-mail sai em segundo plano.
 *
 * ATENÇÃO ao usar: a tarefa enviada para cá roda em uma thread comum, FORA do
 * contexto de requisição do CDI. Ela NÃO pode tocar em EntityManager, em beans
 * @RequestScoped, no FacesContext nem em entidades com carregamento preguiçoso.
 * Tudo o que a tarefa precisa (lista de destinatários, assunto, corpo) tem que
 * ser resolvido ANTES, na thread da requisição, e passado como String.
 *
 * As threads são daemon: se o Tomcat for derrubado, elas não impedem o desligamento.
 * Em contrapartida, e-mails ainda na fila no momento de um redeploy são perdidos —
 * o que é aceitável aqui, já que uma falha de envio nunca impediu o cadastro.
 */
@ApplicationScoped
public class ExecutorEmailService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorEmailService.class);

	/**
	 * Duas threads: suficiente para não segurar fila em uso normal e baixo o
	 * bastante para não abrir muitas conexões SMTP simultâneas (vários provedores
	 * limitam conexões por origem).
	 */
	private static final int TAMANHO_POOL = 2;

	/** Tempo esperado no shutdown para os envios em andamento terminarem. */
	private static final int SEGUNDOS_ESPERA_SHUTDOWN = 15;

	private ExecutorService executor;

	@PostConstruct
	public void iniciar() {
		final AtomicInteger contador = new AtomicInteger(1);

		ThreadFactory fabrica = new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "envio-email-" + contador.getAndIncrement());
				thread.setDaemon(true);
				return thread;
			}
		};

		executor = Executors.newFixedThreadPool(TAMANHO_POOL, fabrica);
		LOGGER.info("Executor de e-mails iniciado com {} thread(s)", TAMANHO_POOL);
	}

	/**
	 * Agenda a tarefa para rodar em segundo plano.
	 *
	 * Nunca lança exceção para quem chamou: um problema no envio de e-mail não
	 * pode derrubar o cadastro que o usuário acabou de fazer.
	 */
	public void executar(Runnable tarefa) {
		if (tarefa == null) {
			return;
		}

		if (executor == null || executor.isShutdown()) {
			LOGGER.warn("Executor de e-mails indisponível. Enviando na própria thread como alternativa.");
			executarComSeguranca(tarefa);
			return;
		}

		try {
			executor.submit(() -> executarComSeguranca(tarefa));
		} catch (Exception e) {
			LOGGER.error("Não foi possível agendar o envio de e-mail: ", e);
		}
	}

	private void executarComSeguranca(Runnable tarefa) {
		try {
			tarefa.run();
		} catch (Exception e) {
			LOGGER.error("Erro durante envio de e-mail em segundo plano: ", e);
		}
	}

	@PreDestroy
	public void encerrar() {
		if (executor == null) {
			return;
		}

		LOGGER.info("Encerrando executor de e-mails...");
		executor.shutdown();

		try {
			if (!executor.awaitTermination(SEGUNDOS_ESPERA_SHUTDOWN, TimeUnit.SECONDS)) {
				LOGGER.warn("Envios de e-mail ainda em andamento foram interrompidos no desligamento");
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}