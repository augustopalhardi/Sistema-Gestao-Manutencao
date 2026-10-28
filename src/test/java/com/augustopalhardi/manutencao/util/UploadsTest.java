package com.augustopalhardi.manutencao.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UploadsTest {

	static {
		// Config lê o arquivo na primeira vez que é usada: aponta para o
		// config fictício de teste antes disso, inclusive ao rodar pela IDE.
		System.setProperty("config.file", "src/test/resources/config-teste.properties");
	}

	@Test
	@DisplayName("Remove o caminho enviado pelo navegador (Windows e Linux)")
	void removeCaminho() {
		assertEquals("foto.jpg", Uploads.nomeSeguro("C:\\Users\\joao\\Desktop\\foto.jpg"));
		assertEquals("foto.jpg", Uploads.nomeSeguro("/home/joao/foto.jpg"));
	}

	@Test
	@DisplayName("Impede path traversal no nome do arquivo")
	void impedePathTraversal() {
		String nome = Uploads.nomeSeguro("../../etc/passwd");
		assertEquals("passwd", nome);
	}

	@Test
	@DisplayName("Troca espaços e acentos por _")
	void trocaCaracteresEspeciais() {
		assertEquals("foto_da_m_quina.png", Uploads.nomeSeguro("foto da máquina.png"));
	}

	@Test
	@DisplayName("Nome vazio ou nulo vira 'arquivo'")
	void nomeVazio() {
		assertEquals("arquivo", Uploads.nomeSeguro(null));
		assertEquals("arquivo", Uploads.nomeSeguro("C:\\pasta\\"));
	}

	@Test
	@DisplayName("Diretório vem do config e termina sempre em /")
	void diretorio() {
		assertTrue(Uploads.diretorioBase().endsWith("/"));
		assertEquals(Uploads.diretorioBase() + "locais/", Uploads.diretorio(Uploads.LOCAIS));
		assertEquals("/uploads/locais/", Uploads.url(Uploads.LOCAIS));
	}
}
