package com.augustopalhardi.manutencao.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lê as configurações do arquivo externo {@code config.properties}.
 *
 * O caminho do arquivo vem do parâmetro da JVM {@code -Dconfig.file}; sem ele,
 * usa {@code /etc/manutencao/config.properties}. Assim nenhuma credencial fica
 * versionada nem empacotada dentro do .war.
 */
public final class Config {

	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
	private static final String CAMINHO_PADRAO = "/etc/manutencao/config.properties";
	private static final Properties props = new Properties();

	static {
		String caminho = System.getProperty("config.file", CAMINHO_PADRAO);
		try (InputStream in = new FileInputStream(caminho)) {
			props.load(in);
			LOGGER.info("Configuração carregada de: {}", caminho);
		} catch (Exception e) {
			LOGGER.error("Falha ao carregar configuração de: {}", caminho, e);
			throw new IllegalStateException("Arquivo de configuração não encontrado: " + caminho, e);
		}
	}

	private Config() {
	}

	/** Retorna uma chave obrigatória; lança exceção se estiver ausente ou vazia. */
	public static String get(String chave) {
		String valor = props.getProperty(chave);
		if (valor == null || valor.trim().isEmpty()) {
			throw new IllegalStateException("Configuração ausente: " + chave);
		}
		return valor.trim();
	}

	/** Retorna uma chave opcional, ou {@code padrao} se estiver ausente ou vazia. */
	public static String get(String chave, String padrao) {
		String valor = props.getProperty(chave);
		return (valor == null || valor.trim().isEmpty()) ? padrao : valor.trim();
	}
}
