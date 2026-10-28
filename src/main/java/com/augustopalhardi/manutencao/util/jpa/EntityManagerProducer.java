package com.augustopalhardi.manutencao.util.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.util.Config;

@ApplicationScoped
public class EntityManagerProducer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerProducer.class);
	private static final String PERSISTENCE_UNIT = "gestaoManutencaoPU";
	private static EntityManagerFactory factory;

	static {
		try {
			LOGGER.info("Iniciando criação do EntityManagerFactory...");
			factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, montarPropriedades());
			LOGGER.info("EntityManagerFactory criado com sucesso!");
		} catch (Exception e) {
			LOGGER.error("Erro ao criar EntityManagerFactory: ", e);
			throw new RuntimeException("Falha ao inicializar EntityManagerFactory", e);
		}
	}

	/**
	 * Monta as propriedades de conexão a partir do arquivo externo de
	 * configuração (ver Config).
	 *
	 * Estes valores não ficam mais no persistence.xml para que credenciais
	 * não sejam versionadas nem empacotadas dentro do .war.
	 */
	private static Map<String, String> montarPropriedades() {
		Map<String, String> props = new HashMap<>();
		props.put("javax.persistence.jdbc.url", Config.get("db.url"));
		props.put("javax.persistence.jdbc.user", Config.get("db.user"));
		props.put("javax.persistence.jdbc.password", Config.get("db.password"));
		return props;
	}

	@Produces
	@RequestScoped
	public EntityManager createEntityManager() {
		if (factory == null) {
			LOGGER.error("EntityManagerFactory está null - tentando recriar...");
			try {
				factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, montarPropriedades());
			} catch (Exception e) {
				LOGGER.error("Erro ao recriar EntityManagerFactory: ", e);
				throw new RuntimeException("EntityManagerFactory não disponível", e);
			}
		}

		EntityManager em = factory.createEntityManager();
		LOGGER.debug("EntityManager criado");
		return em;
	}

	public void closeEntityManager(@Disposes EntityManager manager) {
		if (manager != null && manager.isOpen()) {
			LOGGER.debug("Fechando EntityManager");
			manager.close();
		}
	}

	public static void closeFactory() {
		if (factory != null && factory.isOpen()) {
			LOGGER.info("Fechando EntityManagerFactory");
			factory.close();
		}
	}
}