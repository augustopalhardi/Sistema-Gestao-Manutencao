package com.augustopalhardi.manutencao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculoIndicadoresTest {

	private static final long UM_DIA = 24L * 60 * 60 * 1000;
	private static final double DELTA = 0.0001;

	private static Date dia(int n) {
		return new Date(n * UM_DIA);
	}

	// ---------- horas úteis ----------

	@Test
	@DisplayName("Um dia de calendário vale 8 horas úteis")
	void umDiaValeOitoHorasUteis() {
		assertEquals(8.0, CalculoIndicadores.horasUteis(UM_DIA), DELTA);
		assertEquals(4.0, CalculoIndicadores.horasUteis(UM_DIA / 2), DELTA);
	}

	@Test
	@DisplayName("Horas entre datas: nulo ou fim antes do início dá zero")
	void horasEntreDatasInvalidasDaZero() {
		assertEquals(0, CalculoIndicadores.horasUteisEntre(null, dia(1)), DELTA);
		assertEquals(0, CalculoIndicadores.horasUteisEntre(dia(1), null), DELTA);
		assertEquals(0, CalculoIndicadores.horasUteisEntre(dia(3), dia(1)), DELTA);
		assertEquals(16, CalculoIndicadores.horasUteisEntre(dia(1), dia(3)), DELTA);
	}

	// ---------- MTTR ----------

	@Test
	@DisplayName("MTTR é a média das horas dos reparos, ignorando reparos sem horas")
	void mttrIgnoraReparosSemHoras() {
		assertEquals(3.0, CalculoIndicadores.mttr(Arrays.asList(2.0, 4.0, 0.0)), DELTA);
	}

	@Test
	@DisplayName("MTTR sem reparos é zero")
	void mttrSemReparos() {
		assertEquals(0, CalculoIndicadores.mttr(Collections.emptyList()), DELTA);
		assertEquals(0, CalculoIndicadores.mttr(null), DELTA);
	}

	// ---------- MTBF ----------

	@Test
	@DisplayName("MTBF mede o intervalo entre falhas consecutivas, mesmo fora de ordem")
	void mtbfComDatasForaDeOrdem() {
		// falhas nos dias 3, 0 e 1 -> intervalos de 1 dia (8h) e 2 dias (16h) -> média 12h
		List<Date> maquinaA = Arrays.asList(dia(3), dia(0), dia(1));
		assertEquals(12.0, CalculoIndicadores.mtbf(Collections.singletonList(maquinaA)), DELTA);
	}

	@Test
	@DisplayName("MTBF ignora máquinas com uma única falha")
	void mtbfIgnoraMaquinaComUmaFalha() {
		List<Date> maquinaA = Arrays.asList(dia(0), dia(2)); // 16h
		List<Date> maquinaB = Collections.singletonList(dia(5));
		assertEquals(16.0, CalculoIndicadores.mtbf(Arrays.asList(maquinaA, maquinaB)), DELTA);
	}

	@Test
	@DisplayName("MTBF sem intervalos é zero")
	void mtbfSemIntervalos() {
		assertEquals(0, CalculoIndicadores.mtbf(Collections.emptyList()), DELTA);
	}

	// ---------- confiabilidade ----------

	@Test
	@DisplayName("Confiabilidade = horas disponíveis menos paradas, em percentual")
	void confiabilidadeBasica() {
		assertEquals(75.0, CalculoIndicadores.confiabilidade(100, 25, true), DELTA);
	}

	@Test
	@DisplayName("Confiabilidade fica entre 0 e 100")
	void confiabilidadeLimitada() {
		assertEquals(0.0, CalculoIndicadores.confiabilidade(100, 250, true), DELTA);
		assertEquals(100.0, CalculoIndicadores.confiabilidade(100, -10, false), DELTA);
	}

	@Test
	@DisplayName("Sem período mensurável: 100% sem manutenção, 0% com manutenção")
	void confiabilidadeSemPeriodo() {
		assertEquals(100.0, CalculoIndicadores.confiabilidade(0, 0, false), DELTA);
		assertEquals(0.0, CalculoIndicadores.confiabilidade(0, 0, true), DELTA);
	}

	@Test
	@DisplayName("Confiabilidade é arredondada em duas casas")
	void confiabilidadeArredondada() {
		assertEquals(66.67, CalculoIndicadores.confiabilidade(3, 1, true), DELTA);
	}

	// ---------- auxiliares ----------

	@Test
	@DisplayName("Percentual com total zero não divide por zero")
	void percentual() {
		assertEquals(33.33, CalculoIndicadores.percentual(1, 3), DELTA);
		assertEquals(0.0, CalculoIndicadores.percentual(5, 0), DELTA);
	}

	@Test
	@DisplayName("Média arredondada e média de lista vazia")
	void media() {
		assertEquals(2.33, CalculoIndicadores.media(Arrays.asList(1.0, 2.0, 4.0)), DELTA);
		assertEquals(0.0, CalculoIndicadores.media(Collections.emptyList()), DELTA);
	}

	@Test
	@DisplayName("Maiores valores vêm em ordem decrescente e respeitam o limite")
	void maiores() {
		Map<String, Long> contagem = new HashMap<>();
		contagem.put("Torno", 3L);
		contagem.put("Prensa", 7L);
		contagem.put("Serra", 1L);

		Map<String, Long> top2 = CalculoIndicadores.maiores(contagem, 2);

		assertEquals(Arrays.asList("Prensa", "Torno"), Arrays.asList(top2.keySet().toArray()));
	}

	@Test
	@DisplayName("Ordenação crescente coloca o pior primeiro")
	void ordenadoCrescente() {
		Map<String, Double> conf = new HashMap<>();
		conf.put("A", 90.0);
		conf.put("B", 40.0);
		conf.put("C", 75.0);

		assertEquals(Arrays.asList("B", "C", "A"),
				Arrays.asList(CalculoIndicadores.ordenadoCrescente(conf).keySet().toArray()));
	}
}
