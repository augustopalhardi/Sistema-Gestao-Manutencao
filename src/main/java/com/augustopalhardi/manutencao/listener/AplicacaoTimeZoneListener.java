package com.augustopalhardi.manutencao.listener;

import java.util.TimeZone;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Garante que o timezone padrão da JVM seja sempre America/Sao_Paulo
 * (horário de Brasília), independente do timezone configurado no
 * servidor/máquina onde a aplicação está hospedada (ex: servidores AWS
 * costumam rodar em UTC por padrão).
 *
 * Isso é essencial porque diversos pontos do sistema usam new Date(),
 * Calendar.getInstance() e SimpleDateFormat sem especificar o timezone
 * explicitamente — esses métodos usam o timezone padrão da JVM. Sem esse
 * listener, em um servidor configurado em UTC, todo horário registrado ou
 * calculado no sistema (datas de criação, início/fim de período de
 * trabalho, comparações de "hoje", etc.) sairia deslocado em relação ao
 * horário real de Brasília.
 *
 * Este listener é carregado automaticamente pelo container (Servlet 3.0+)
 * graças à anotação @WebListener, antes de qualquer outro componente da
 * aplicação (filtros, beans, etc.) ser inicializado.
 */
@WebListener
public class AplicacaoTimeZoneListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AplicacaoTimeZoneListener.class);

	private static final String TIMEZONE_PADRAO = "America/Sao_Paulo";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_PADRAO));
		LOGGER.info("Timezone padrão da aplicação definido para {}. Timezone atual da JVM: {}", TIMEZONE_PADRAO,
				TimeZone.getDefault().getID());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Nada a fazer
	}
}