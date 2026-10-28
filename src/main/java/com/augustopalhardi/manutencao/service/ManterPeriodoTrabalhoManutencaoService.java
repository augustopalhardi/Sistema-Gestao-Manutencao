package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.dao.PeriodoTrabalhoManutencaoDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterPeriodoTrabalhoManutencaoService implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ManterPeriodoTrabalhoManutencaoService.class);

	@Inject
	private PeriodoTrabalhoManutencaoDao periodoTrabalhoDao;

	@Transactional
	public PeriodoTrabalhoManutencao salvar(PeriodoTrabalhoManutencao periodo) {
		try {
			// Validação básica
			if (periodo.getDataHoraInicio() == null || periodo.getDataHoraFim() == null) {
				throw new IllegalArgumentException("Data de início e fim são obrigatórias.");
			}

			if (periodo.getDataHoraFim().before(periodo.getDataHoraInicio())) {
				throw new IllegalArgumentException("Data de fim deve ser posterior à data de início.");
			}

			PeriodoTrabalhoManutencao salvo = periodoTrabalhoDao.salvar(periodo);
			LOGGER.info("Período de trabalho salvo: ID {}, Solicitação {}", salvo.getId(), salvo.getSolicitacaoId());
			return salvo;

		} catch (Exception e) {
			LOGGER.error("Erro ao salvar período de trabalho: ", e);
			throw new RuntimeException("Erro ao salvar período de trabalho: " + e.getMessage(), e);
		}
	}

	public PeriodoTrabalhoManutencao buscarPorId(Long id) {
		try {
			return periodoTrabalhoDao.buscarPeloCodigo(id);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar período por ID {}: ", id, e);
			return null;
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarTodos() {
		try {
			return periodoTrabalhoDao.buscarTodos();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar todos os períodos: ", e);
			return List.of(); // Retorna lista vazia em caso de erro
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			return periodoTrabalhoDao.buscarPorSolicitacao(solicitacaoId, tipo);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por solicitação {} e tipo {}: ", solicitacaoId, tipo, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorTipo(TipoManutencaoReferencia tipo) {
		try {
			return periodoTrabalhoDao.buscarPorTipo(tipo);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por tipo {}: ", tipo, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorPeriodo(Date dataInicio, Date dataFim) {
		try {
			return periodoTrabalhoDao.buscarPorPeriodo(dataInicio, dataFim);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos entre {} e {}: ", dataInicio, dataFim, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorData(Date data) {
		try {
			return periodoTrabalhoDao.buscarPorData(data);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por data {}: ", data, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorTecnico(CadastroUsuario tecnico) {
		try {
			return periodoTrabalhoDao.buscarPorTecnico(tecnico);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por técnico {}: ", tecnico != null ? tecnico.getNome() : "null", e);
			return List.of();
		}
	}

	public Double calcularTotalHoras(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			Double total = periodoTrabalhoDao.calcularTotalHoras(solicitacaoId, tipo);
			return total != null ? total : 0.0;
		} catch (Exception e) {
			LOGGER.error("Erro ao calcular total de horas para solicitação {} e tipo {}: ", solicitacaoId, tipo, e);
			return 0.0;
		}
	}

	@Transactional
	public PeriodoTrabalhoManutencao adicionarPeriodo(Long solicitacaoId, TipoManutencaoReferencia tipo, Date dataHoraInicio,
			Date dataHoraFim, String descricaoAtividade) {
		try {
			PeriodoTrabalhoManutencao periodo = new PeriodoTrabalhoManutencao(solicitacaoId, tipo, dataHoraInicio,
					dataHoraFim);
			periodo.setDescricaoAtividade(descricaoAtividade);

			return salvar(periodo);
		} catch (Exception e) {
			LOGGER.error("Erro ao adicionar período: ", e);
			throw new RuntimeException("Erro ao adicionar período de trabalho: " + e.getMessage(), e);
		}
	}

	/**
	 * Adiciona um período de trabalho com técnico responsável
	 */
	@Transactional
	public PeriodoTrabalhoManutencao adicionarPeriodoComTecnico(Long solicitacaoId, TipoManutencaoReferencia tipoManutencao,
			Date dataHoraInicio, Date dataHoraFim, String descricaoAtividade, CadastroUsuario tecnicoResponsavel) {
		try {
			PeriodoTrabalhoManutencao periodo = new PeriodoTrabalhoManutencao();
			periodo.setSolicitacaoId(solicitacaoId);
			periodo.setTipoManutencao(tipoManutencao);
			periodo.setDataHoraInicio(dataHoraInicio);
			periodo.setDataHoraFim(dataHoraFim);
			periodo.setDescricaoAtividade(descricaoAtividade);
			periodo.setTecnicoResponsavel(tecnicoResponsavel);

			PeriodoTrabalhoManutencao salvo = salvar(periodo);

			LOGGER.info("Período de trabalho adicionado com técnico. Solicitação: {}, Técnico: {}", solicitacaoId,
					tecnicoResponsavel != null ? tecnicoResponsavel.getNome() : "N/A");

			return salvo;

		} catch (Exception e) {
			LOGGER.error("Erro ao adicionar período de trabalho com técnico: ", e);
			throw new RuntimeException("Erro ao salvar período de trabalho: " + e.getMessage(), e);
		}
	}

	/**
	 * Adiciona um período de trabalho com técnico responsável e flag de
	 * diagnóstico
	 */
	@Transactional
	public PeriodoTrabalhoManutencao adicionarPeriodoComTecnicoComDiagnostico(Long solicitacaoId,
			TipoManutencaoReferencia tipoManutencao, Date dataHoraInicio, Date dataHoraFim, String descricaoAtividade,
			CadastroUsuario tecnicoResponsavel, Boolean utilizadoParaDiagnostico) {
		try {
			PeriodoTrabalhoManutencao periodo = new PeriodoTrabalhoManutencao();
			periodo.setSolicitacaoId(solicitacaoId);
			periodo.setTipoManutencao(tipoManutencao);
			periodo.setDataHoraInicio(dataHoraInicio);
			periodo.setDataHoraFim(dataHoraFim);
			periodo.setDescricaoAtividade(descricaoAtividade);
			periodo.setTecnicoResponsavel(tecnicoResponsavel);
			periodo.setUtilizadoParaDiagnostico(utilizadoParaDiagnostico);

			PeriodoTrabalhoManutencao salvo = salvar(periodo);

			LOGGER.info("Período de trabalho adicionado com técnico. Solicitação: {}, Técnico: {}, Diagnóstico: {}",
					solicitacaoId, tecnicoResponsavel != null ? tecnicoResponsavel.getNome() : "N/A",
					utilizadoParaDiagnostico);

			return salvo;

		} catch (Exception e) {
			LOGGER.error("Erro ao adicionar período de trabalho com técnico: ", e);
			throw new RuntimeException("Erro ao salvar período de trabalho: " + e.getMessage(), e);
		}
	}

	@Transactional
	public List<PeriodoTrabalhoManutencao> salvarMultiplosPeriodos(List<PeriodoTrabalhoManutencao> periodos) {
		try {
			for (PeriodoTrabalhoManutencao periodo : periodos) {
				salvar(periodo);
			}
			LOGGER.info("Salvos {} períodos de trabalho", periodos.size());
			return periodos;
		} catch (Exception e) {
			LOGGER.error("Erro ao salvar múltiplos períodos: ", e);
			throw new RuntimeException("Erro ao salvar períodos: " + e.getMessage(), e);
		}
	}

	@Transactional
	public void excluir(PeriodoTrabalhoManutencao periodo) {
		try {
			periodoTrabalhoDao.excluir(periodo);
			LOGGER.info("Período de trabalho excluído: ID {}", periodo.getId());
		} catch (Exception e) {
			LOGGER.error("Erro ao excluir período de trabalho: ", e);
			throw new RuntimeException("Erro ao excluir período: " + e.getMessage(), e);
		}
	}

	@Transactional
	public void excluirTodosPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			int quantidadeExcluida = periodoTrabalhoDao.excluirTodosPorSolicitacao(solicitacaoId, tipo);
			LOGGER.info("Excluídos {} períodos para solicitação {} do tipo {}", quantidadeExcluida, solicitacaoId,
					tipo);
		} catch (Exception e) {
			LOGGER.error("Erro ao excluir períodos por solicitação {} e tipo {}: ", solicitacaoId, tipo, e);
			throw new RuntimeException("Erro ao excluir períodos: " + e.getMessage(), e);
		}
	}

	/**
	 * Calcula total de horas por técnico em um período
	 */
	public Double calcularHorasPorTecnico(CadastroUsuario tecnico, Date dataInicio, Date dataFim) {
		try {
			return periodoTrabalhoDao.calcularHorasPorTecnico(tecnico, dataInicio, dataFim);
		} catch (Exception e) {
			LOGGER.error("Erro ao calcular horas por técnico: ", e);
			return 0.0;
		}
	}

	/**
	 * Busca períodos de trabalho por solicitação e técnico
	 */
	public List<PeriodoTrabalhoManutencao> buscarPorSolicitacaoETecnico(Long solicitacaoId, TipoManutencaoReferencia tipo,
			CadastroUsuario tecnico) {
		try {
			return periodoTrabalhoDao.buscarPorSolicitacaoETecnico(solicitacaoId, tipo, tecnico);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por solicitação e técnico: ", e);
			return List.of();
		}
	}

	// Método de conveniência para obter resumo de horas
	public String obterResumoHoras(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			List<PeriodoTrabalhoManutencao> periodos = buscarPorSolicitacao(solicitacaoId, tipo);
			if (periodos.isEmpty()) {
				return "Nenhum período registrado";
			}

			double totalHoras = calcularTotalHoras(solicitacaoId, tipo);
			int quantidadePeriodos = periodos.size();

			return String.format("%d período(s) - Total: %.2f horas", quantidadePeriodos, totalHoras);
		} catch (Exception e) {
			LOGGER.error("Erro ao obter resumo de horas: ", e);
			return "Erro ao calcular horas";
		}
	}

	/**
	 * Obter resumo detalhado com técnicos
	 */
	public String obterResumoComTecnicos(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			List<PeriodoTrabalhoManutencao> periodos = buscarPorSolicitacao(solicitacaoId, tipo);
			if (periodos.isEmpty()) {
				return "Nenhum período registrado";
			}

			double totalHoras = calcularTotalHoras(solicitacaoId, tipo);
			int quantidadePeriodos = periodos.size();

			long tecnicosDistintos = periodos.stream().filter(p -> p.getTecnicoResponsavel() != null)
					.map(p -> p.getTecnicoResponsavel().getId()).distinct().count();

			return String.format("%d período(s) - %.2f horas - %d técnico(s)", quantidadePeriodos, totalHoras,
					tecnicosDistintos);
		} catch (Exception e) {
			LOGGER.error("Erro ao obter resumo com técnicos: ", e);
			return "Erro ao calcular resumo";
		}
	}
}