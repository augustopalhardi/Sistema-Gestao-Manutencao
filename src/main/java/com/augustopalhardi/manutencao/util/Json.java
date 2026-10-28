package com.augustopalhardi.manutencao.util;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta os arrays JSON consumidos pelos gráficos (Chart.js) das telas de
 * relatório: rótulos a partir das chaves de um mapa e dados a partir dos
 * valores.
 */
public final class Json {

	private Json() {
	}

	/** Chaves do mapa como array de strings: {@code ["A","B"]}. */
	public static String rotulos(Map<String, ?> mapa) {
		if (mapa == null || mapa.isEmpty()) {
			return "[]";
		}
		return "[" + mapa.keySet().stream().map(k -> "\"" + escapar(k) + "\"").collect(Collectors.joining(",")) + "]";
	}

	/** Valores inteiros do mapa: {@code [3,1]}. */
	public static String valoresInteiros(Map<String, Long> mapa) {
		if (mapa == null || mapa.isEmpty()) {
			return "[]";
		}
		return "[" + mapa.values().stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
	}

	/** Valores decimais do mapa com duas casas e ponto decimal: {@code [1.50,2.00]}. */
	public static String valoresDecimais(Map<String, Double> mapa) {
		if (mapa == null || mapa.isEmpty()) {
			return "[]";
		}
		return "[" + mapa.values().stream().map(v -> String.format(Locale.US, "%.2f", v))
				.collect(Collectors.joining(",")) + "]";
	}

	/** Escapa barra invertida e aspas para uso dentro de uma string JSON. */
	public static String escapar(String texto) {
		if (texto == null) {
			return "";
		}
		return texto.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
