package com.augustopalhardi.manutencao.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Conferência de e-mail digitado pelo usuário.
 *
 * POR QUE ISSO EXISTE: a validação por expressão regular só pega endereço
 * MALFORMADO ("joao@", "joao.com", com espaço no meio). O erro que realmente
 * acontece no dia a dia é outro: "joao@gmial.com", "joao@hotmail.con",
 * "joao@yaho.com". Esses endereços são perfeitamente válidos na forma — eles
 * apenas não existem, e o e-mail sai do sistema para nunca chegar em ninguém.
 *
 * A defesa contra esse caso é comparar o domínio digitado com uma lista de
 * domínios conhecidos e sugerir a correção quando a diferença for de um ou dois
 * caracteres.
 *
 * IMPORTANTE: nada aqui BLOQUEIA o cadastro por domínio desconhecido. Seria
 * arriscado — a lista nunca vai cobrir todo domínio legítimo que possa aparecer.
 * O papel desta classe é avisar e sugerir; a decisão continua com o usuário.
 */
public final class ValidadorEmail {

	/** Conferência de forma. Proposital não ser a RFC completa: pega o que erra na prática. */
	private static final Pattern PADRAO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");


	/**
	 * Domínios públicos mais usados no Brasil. Serve de referência tanto para
	 * "este domínio é conhecido" quanto para sugerir correção de digitação.
	 */
	private static final List<String> DOMINIOS_PUBLICOS = Arrays.asList(
			"gmail.com",
			"hotmail.com",
			"outlook.com",
			"outlook.com.br",
			"live.com",
			"msn.com",
			"yahoo.com",
			"yahoo.com.br",
			"icloud.com",
			"me.com",
			"bol.com.br",
			"uol.com.br",
			"terra.com.br",
			"ig.com.br",
			"globo.com",
			"globomail.com",
			"protonmail.com",
			"zipmail.com.br");

	/**
	 * Distância máxima de digitação para considerar que houve erro e sugerir
	 * correção. Dois cobre os casos comuns ("gmial", "hotmial", ".con") sem
	 * começar a sugerir besteira para domínios legítimos e curtos.
	 */
	private static final int DISTANCIA_MAXIMA = 2;

	private ValidadorEmail() {
	}

	/**
	 * Domínios corporativos da empresa que usa o sistema, lidos da chave
	 * OPCIONAL "email.dominios.empresa" do config.properties (vários separados
	 * por vírgula). Assim o domínio de cada instalação fica fora do código e
	 * pode mudar sem novo deploy.
	 *
	 * Exemplo no config.properties:
	 *   email.dominios.empresa=suaempresa.com.br,suaempresa.ind.br
	 */
	private static List<String> dominiosDaEmpresa() {
		List<String> dominios = new ArrayList<>();
		String configurado = Config.get("email.dominios.empresa", "");

		for (String parte : configurado.split(",")) {
			String limpo = parte.trim().toLowerCase(Locale.ROOT);
			if (!limpo.isEmpty()) {
				dominios.add(limpo);
			}
		}

		return dominios;
	}

	/** Todos os domínios tidos como válidos: os da empresa mais os públicos. */
	private static Set<String> dominiosConhecidos() {
		Set<String> todos = new LinkedHashSet<>(dominiosDaEmpresa());
		todos.addAll(DOMINIOS_PUBLICOS);
		return todos;
	}

	/** O endereço tem forma de e-mail? */
	public static boolean formatoValido(String email) {
		return email != null && PADRAO_EMAIL.matcher(email.trim()).matches();
	}

	/** Parte depois do @, em minúsculas. Devolve null se não der para extrair. */
	private static String extrairDominio(String email) {
		if (email == null) {
			return null;
		}

		String limpo = email.trim().toLowerCase(Locale.ROOT);
		int arroba = limpo.lastIndexOf('@');

		if (arroba < 0 || arroba == limpo.length() - 1) {
			return null;
		}

		return limpo.substring(arroba + 1);
	}

	/** O domínio digitado está entre os conhecidos? */
	public static boolean dominioConhecido(String email) {
		String dominio = extrairDominio(email);
		return dominio != null && dominiosConhecidos().contains(dominio);
	}

	/** O domínio digitado é um dos domínios da empresa? */
	public static boolean dominioDaEmpresa(String email) {
		String dominio = extrairDominio(email);
		return dominio != null && dominiosDaEmpresa().contains(dominio);
	}

	/**
	 * Procura um domínio conhecido parecido com o digitado e devolve o e-mail
	 * corrigido, ou null quando não há sugestão a fazer.
	 *
	 * Devolve null tanto quando o domínio já está certo quanto quando ele é
	 * desconhecido mas não se parece com nenhum da lista — nesse segundo caso
	 * pode ser um domínio corporativo legítimo de fora, e sugerir "gmail.com"
	 * só atrapalharia.
	 */
	public static String sugerirCorrecao(String email) {
		if (!formatoValido(email)) {
			return null;
		}

		String limpo = email.trim();
		String dominio = extrairDominio(limpo);

		if (dominio == null || dominiosConhecidos().contains(dominio)) {
			return null;
		}

		String usuario = limpo.substring(0, limpo.lastIndexOf('@'));
		String melhorDominio = null;
		int menorDistancia = Integer.MAX_VALUE;

		for (String candidato : dominiosConhecidos()) {
			if (!pareceErroDeDigitacao(dominio, candidato)) {
				continue;
			}

			int distancia = distanciaEdicao(dominio, candidato);

			if (distancia < menorDistancia) {
				menorDistancia = distancia;
				melhorDominio = candidato;
			}
		}

		return melhorDominio != null ? (usuario + "@" + melhorDominio) : null;
	}

	/**
	 * Decide se o domínio digitado é provavelmente um erro de digitação do
	 * candidato — e não simplesmente outro domínio.
	 *
	 * Comparar os domínios inteiros de uma vez não funciona. "ibm.com.br" e
	 * "ig.com.br" ficam a apenas 2 de distância, porque compartilham o
	 * ".com.br" inteiro; sugerir "ig.com.br" para quem digitou "ibm.com.br"
	 * seria absurdo. A diferença toda está no NOME, não no final.
	 *
	 * Por isso as duas partes são avaliadas separadamente:
	 *
	 * - NOME (antes do primeiro ponto): tolera diferença de até metade do
	 *   tamanho. "gmial"→"gmail" passa (2 de 5); "ibm"→"ig" não passa (2 de 3).
	 * - FINAL (o resto): tolera no máximo 1, que cobre ".con"→".com" e
	 *   ".com.bt"→".com.br" sem confundir ".com" com ".com.br".
	 * - E a soma das duas continua limitada a DISTANCIA_MAXIMA.
	 */
	private static boolean pareceErroDeDigitacao(String digitado, String candidato) {
		int pontoDigitado = digitado.indexOf('.');
		int pontoCandidato = candidato.indexOf('.');

		if (pontoDigitado < 0 || pontoCandidato < 0) {
			return false;
		}

		String nomeDigitado = digitado.substring(0, pontoDigitado);
		String finalDigitado = digitado.substring(pontoDigitado + 1);

		String nomeCandidato = candidato.substring(0, pontoCandidato);
		String finalCandidato = candidato.substring(pontoCandidato + 1);

		int distanciaNome = distanciaEdicao(nomeDigitado, nomeCandidato);
		int distanciaFinal = distanciaEdicao(finalDigitado, finalCandidato);

		if (distanciaFinal > 1) {
			return false;
		}

		if (distanciaNome > nomeCandidato.length() / 2) {
			return false;
		}

		return (distanciaNome + distanciaFinal) <= DISTANCIA_MAXIMA;
	}

	/**
	 * Distância de Levenshtein: quantas inserções, remoções ou trocas de letra
	 * separam um texto do outro. "gmial.com" está a 2 de "gmail.com".
	 */
	private static int distanciaEdicao(String a, String b) {
		if (a == null || b == null) {
			return Integer.MAX_VALUE;
		}

		int[] anterior = new int[b.length() + 1];
		int[] atual = new int[b.length() + 1];

		for (int j = 0; j <= b.length(); j++) {
			anterior[j] = j;
		}

		for (int i = 1; i <= a.length(); i++) {
			atual[0] = i;

			for (int j = 1; j <= b.length(); j++) {
				int custo = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
				atual[j] = Math.min(Math.min(atual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + custo);
			}

			int[] troca = anterior;
			anterior = atual;
			atual = troca;
		}

		return anterior[b.length()];
	}
}