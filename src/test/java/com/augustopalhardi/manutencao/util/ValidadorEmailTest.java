package com.augustopalhardi.manutencao.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Usa src/test/resources/config-teste.properties, que define
 * email.dominios.empresa=minhaempresa.com.br.
 */
class ValidadorEmailTest {

	static {
		// Config lê o arquivo na primeira vez que é usada: aponta para o
		// config fictício de teste antes disso, inclusive ao rodar pela IDE.
		System.setProperty("config.file", "src/test/resources/config-teste.properties");
	}

	@Test
	@DisplayName("Formato: aceita e-mail comum e recusa endereços malformados")
	void formato() {
		assertTrue(ValidadorEmail.formatoValido("joao@gmail.com"));
		assertTrue(ValidadorEmail.formatoValido("  joao@gmail.com  "));
		assertFalse(ValidadorEmail.formatoValido("joao@"));
		assertFalse(ValidadorEmail.formatoValido("joao.com"));
		assertFalse(ValidadorEmail.formatoValido("jo ao@gmail.com"));
		assertFalse(ValidadorEmail.formatoValido(null));
	}

	@Test
	@DisplayName("Sugere correção para erros de digitação comuns")
	void sugereCorrecao() {
		assertEquals("joao@gmail.com", ValidadorEmail.sugerirCorrecao("joao@gmial.com"));
		assertEquals("joao@hotmail.com", ValidadorEmail.sugerirCorrecao("joao@hotmial.com"));
		assertEquals("joao@gmail.com", ValidadorEmail.sugerirCorrecao("joao@gmail.con"));
	}

	@Test
	@DisplayName("Não sugere nada para domínio já conhecido")
	void naoSugereParaDominioConhecido() {
		assertNull(ValidadorEmail.sugerirCorrecao("joao@gmail.com"));
		assertNull(ValidadorEmail.sugerirCorrecao("joao@minhaempresa.com.br"));
	}

	@Test
	@DisplayName("Não 'corrige' domínio curto e legítimo para outro parecido")
	void naoConfundeDominioCurto() {
		assertNull(ValidadorEmail.sugerirCorrecao("ana@ibm.com.br"));
	}

	@Test
	@DisplayName("Domínio corporativo vem do config.properties")
	void dominioDaEmpresa() {
		assertTrue(ValidadorEmail.dominioDaEmpresa("ana@minhaempresa.com.br"));
		assertFalse(ValidadorEmail.dominioDaEmpresa("ana@gmail.com"));
	}
}
