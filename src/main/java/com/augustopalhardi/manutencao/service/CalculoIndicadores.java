package com.augustopalhardi.manutencao.service;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fórmulas dos indicadores de manutenção (MTTR, MTBF, confiabilidade e
 * auxiliares).
 *
 * As funções são puras: recebem números e datas já filtrados pela tela e
 * devolvem o resultado, sem acessar banco nem sessão. Assim as mesmas regras
 * valem para o dashboard e para o relatório de indicadores, e podem ser
 * testadas isoladamente.
 *
 * Convenção de tempo: o sistema mede disponibilidade em horas úteis, onde cada
 * 24h de calendário equivalem a {@value #HORAS_UTEIS_POR_DIA} horas de trabalho.
 */
public final class CalculoIndicadores {

	/** Horas de trabalho consideradas em cada dia de calendário. */
	public static final double HORAS_UTEIS_POR_DIA = 8.0;

	private static final double MS_POR_DIA = 1000.0 * 60 * 60 * 24;

	private CalculoIndicadores() {
	}

	/** Converte uma diferença em milissegundos para horas úteis. */
	public static double horasUteis(long diferencaMs) {
		return (diferencaMs / MS_POR_DIA) * HORAS_UTEIS_POR_DIA;
	}

	/** Horas úteis entre duas datas; zero se alguma for nula ou se o fim vier antes do início. */
	public static double horasUteisEntre(Date inicio, Date fim) {
		if (inicio == null || fim == null) {
			return 0;
		}
		return Math.max(0, horasUteis(fim.getTime() - inicio.getTime()));
	}

	/**
	 * MTTR (tempo médio para reparo): média das horas trabalhadas em cada reparo
	 * concluído. Reparos sem horas apontadas (zero ou negativo) são ignorados.
	 */
	public static double mttr(Collection<Double> horasPorReparo) {
		return mediaDosPositivos(horasPorReparo);
	}

	/**
	 * MTBF (tempo médio entre falhas): para cada máquina, ordena as datas de falha
	 * e mede o intervalo, em horas úteis, entre falhas consecutivas. O resultado é
	 * a média de todos os intervalos positivos. Máquinas com menos de duas falhas
	 * não geram intervalo.
	 */
	public static double mtbf(Collection<? extends Collection<Date>> datasDeFalhaPorMaquina) {
		if (datasDeFalhaPorMaquina == null) {
			return 0;
		}
		double soma = 0;
		int intervalos = 0;
		for (Collection<Date> datas : datasDeFalhaPorMaquina) {
			if (datas == null) {
				continue;
			}
			List<Date> ordenadas = datas.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
			for (int i = 1; i < ordenadas.size(); i++) {
				double horas = horasUteis(ordenadas.get(i).getTime() - ordenadas.get(i - 1).getTime());
				if (horas > 0) {
					soma += horas;
					intervalos++;
				}
			}
		}
		return intervalos > 0 ? soma / intervalos : 0;
	}

	/**
	 * Confiabilidade (%) = (horas disponíveis − horas paradas) / horas disponíveis,
	 * limitada entre 0 e 100 e arredondada em duas casas.
	 *
	 * Sem período mensurável (horas disponíveis ≤ 0), vale 100% quando não houve
	 * manutenção e 0% quando houve.
	 */
	public static double confiabilidade(double horasDisponiveis, double horasParadas, boolean houveManutencao) {
		double percentual;
		if (horasDisponiveis > 0) {
			percentual = ((horasDisponiveis - horasParadas) / horasDisponiveis) * 100;
			percentual = Math.max(0, Math.min(100, percentual));
		} else {
			percentual = houveManutencao ? 0.0 : 100.0;
		}
		return arredondar(percentual);
	}

	/** Percentual de {@code parte} sobre {@code total}, em duas casas; zero se o total for zero. */
	public static double percentual(long parte, long total) {
		return total > 0 ? arredondar(parte * 100.0 / total) : 0.0;
	}

	/** Média simples arredondada em duas casas; zero para coleção vazia. */
	public static double media(Collection<Double> valores) {
		if (valores == null || valores.isEmpty()) {
			return 0;
		}
		double soma = valores.stream().mapToDouble(Double::doubleValue).sum();
		return arredondar(soma / valores.size());
	}

	/** Arredonda em duas casas decimais. */
	public static double arredondar(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}

	/** Os {@code limite} maiores valores do mapa, do maior para o menor. */
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> maiores(Map<K, V> mapa, int limite) {
		return mapa.entrySet().stream()
				.sorted(Map.Entry.<K, V>comparingByValue().reversed())
				.limit(limite)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/** O mapa inteiro ordenado do menor para o maior valor. */
	public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> ordenadoCrescente(Map<K, V> mapa) {
		return mapa.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	private static double mediaDosPositivos(Collection<Double> valores) {
		if (valores == null) {
			return 0;
		}
		double soma = 0;
		int quantidade = 0;
		for (Double v : valores) {
			if (v != null && v > 0) {
				soma += v;
				quantidade++;
			}
		}
		return quantidade > 0 ? soma / quantidade : 0;
	}
}
