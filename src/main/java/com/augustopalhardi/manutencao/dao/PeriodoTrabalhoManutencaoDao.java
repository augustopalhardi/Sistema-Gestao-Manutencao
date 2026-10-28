package com.augustopalhardi.manutencao.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.model.PeriodoTrabalhoManutencao;
import com.augustopalhardi.manutencao.model.TipoManutencaoReferencia;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@RequestScoped
public class PeriodoTrabalhoManutencaoDao implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PeriodoTrabalhoManutencaoDao.class);

	@Inject
	private EntityManager manager;

	@Transactional
	public PeriodoTrabalhoManutencao salvar(PeriodoTrabalhoManutencao periodo) {
		try {
			if (periodo.getId() == null) {
				manager.persist(periodo);
				LOGGER.debug("Período de trabalho persistido: {}", periodo.getId());
			} else {
				periodo = manager.merge(periodo);
				LOGGER.debug("Período de trabalho atualizado: {}", periodo.getId());
			}
			return periodo;
		} catch (Exception e) {
			LOGGER.error("Erro ao salvar período de trabalho: ", e);
			throw e;
		}
	}

	public PeriodoTrabalhoManutencao buscarPeloCodigo(Long id) {
		try {
			return manager.find(PeriodoTrabalhoManutencao.class, id);
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar período por ID {}: ", id, e);
			return null;
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarTodos() {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p ORDER BY p.dataCriacao DESC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar todos os períodos: ", e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p WHERE p.solicitacaoId = :solicitacaoId AND p.tipoManutencao = :tipo ORDER BY p.dataHoraInicio ASC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("solicitacaoId", solicitacaoId);
			q.setParameter("tipo", tipo);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por solicitação {} e tipo {}: ", solicitacaoId, tipo, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorTipo(TipoManutencaoReferencia tipo) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p WHERE p.tipoManutencao = :tipo ORDER BY p.dataHoraInicio DESC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("tipo", tipo);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por tipo {}: ", tipo, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorPeriodo(Date dataInicio, Date dataFim) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p WHERE p.dataHoraInicio >= :dataInicio AND p.dataHoraFim <= :dataFim ORDER BY p.dataHoraInicio ASC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("dataInicio", dataInicio);
			q.setParameter("dataFim", dataFim);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos entre {} e {}: ", dataInicio, dataFim, e);
			return List.of();
		}
	}

	public List<PeriodoTrabalhoManutencao> buscarPorData(Date data) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p WHERE DATE(p.dataHoraInicio) = DATE(:data) OR DATE(p.dataHoraFim) = DATE(:data) ORDER BY p.dataHoraInicio ASC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("data", data);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por data {}: ", data, e);
			return List.of();
		}
	}

	// Buscar períodos por técnico responsável
	public List<PeriodoTrabalhoManutencao> buscarPorTecnico(CadastroUsuario tecnico) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p WHERE p.tecnicoResponsavel = :tecnico ORDER BY p.dataHoraInicio DESC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("tecnico", tecnico);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por técnico {}: ", tecnico != null ? tecnico.getNome() : "null", e);
			return List.of();
		}
	}

	// Buscar por solicitação e técnico
	public List<PeriodoTrabalhoManutencao> buscarPorSolicitacaoETecnico(Long solicitacaoId, TipoManutencaoReferencia tipo,
			CadastroUsuario tecnico) {
		try {
			String query = "SELECT p FROM PeriodoTrabalhoManutencao p " + "WHERE p.solicitacaoId = :solicitacaoId "
					+ "AND p.tipoManutencao = :tipo " + "AND p.tecnicoResponsavel = :tecnico "
					+ "ORDER BY p.dataHoraInicio ASC";
			TypedQuery<PeriodoTrabalhoManutencao> q = manager.createQuery(query, PeriodoTrabalhoManutencao.class);
			q.setParameter("solicitacaoId", solicitacaoId);
			q.setParameter("tipo", tipo);
			q.setParameter("tecnico", tecnico);
			return q.getResultList();
		} catch (Exception e) {
			LOGGER.error("Erro ao buscar períodos por solicitação, tipo e técnico: ", e);
			return List.of();
		}
	}

	// Método para calcular total de horas trabalhadas em uma solicitação
	public Double calcularTotalHoras(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			String query = "SELECT COALESCE(SUM("
					+ "(FUNCTION('TIMESTAMPDIFF', 'SECOND', p.dataHoraInicio, p.dataHoraFim)) / 3600.0" + "), 0.0) "
					+ "FROM PeriodoTrabalhoManutencao p "
					+ "WHERE p.solicitacaoId = :solicitacaoId AND p.tipoManutencao = :tipo";

			TypedQuery<Double> q = manager.createQuery(query, Double.class);
			q.setParameter("solicitacaoId", solicitacaoId);
			q.setParameter("tipo", tipo);

			Double resultado = q.getSingleResult();
			return resultado != null ? resultado : 0.0;
		} catch (Exception e) {
			// Se a função TIMESTAMPDIFF não funcionar, usa cálculo Java
			LOGGER.warn("Erro com TIMESTAMPDIFF, usando cálculo Java: {}", e.getMessage());
			try {
				List<PeriodoTrabalhoManutencao> periodos = buscarPorSolicitacao(solicitacaoId, tipo);
				double totalHoras = 0.0;
				for (PeriodoTrabalhoManutencao periodo : periodos) {
					if (periodo.getDataHoraInicio() != null && periodo.getDataHoraFim() != null) {
						long diferencaMs = periodo.getDataHoraFim().getTime() - periodo.getDataHoraInicio().getTime();
						totalHoras += diferencaMs / (1000.0 * 60.0 * 60.0);
					}
				}
				return totalHoras;
			} catch (Exception e2) {
				LOGGER.error("Erro ao calcular total de horas: ", e2);
				return 0.0;
			}
		}
	}

	// Calcular horas trabalhadas por técnico em um período
	public Double calcularHorasPorTecnico(CadastroUsuario tecnico, Date dataInicio, Date dataFim) {
		try {
			String query = "SELECT COALESCE(SUM("
					+ "(FUNCTION('TIMESTAMPDIFF', 'SECOND', p.dataHoraInicio, p.dataHoraFim)) / 3600.0" + "), 0.0) "
					+ "FROM PeriodoTrabalhoManutencao p " + "WHERE p.tecnicoResponsavel = :tecnico "
					+ "AND p.dataHoraInicio >= :dataInicio " + "AND p.dataHoraFim <= :dataFim";

			TypedQuery<Double> q = manager.createQuery(query, Double.class);
			q.setParameter("tecnico", tecnico);
			q.setParameter("dataInicio", dataInicio);
			q.setParameter("dataFim", dataFim);

			Double resultado = q.getSingleResult();
			return resultado != null ? resultado : 0.0;
		} catch (NoResultException e) {
			return 0.0;
		} catch (Exception e) {
			LOGGER.error("Erro ao calcular horas por técnico: ", e);
			return 0.0;
		}
	}

	@Transactional
	public void excluir(PeriodoTrabalhoManutencao periodo) {
		try {
			PeriodoTrabalhoManutencao periodoGerenciado = manager.merge(periodo);
			manager.remove(periodoGerenciado);
			LOGGER.debug("Período de trabalho excluído: {}", periodo.getId());
		} catch (Exception e) {
			LOGGER.error("Erro ao excluir período de trabalho: ", e);
			throw e;
		}
	}

	@Transactional
	public int excluirTodosPorSolicitacao(Long solicitacaoId, TipoManutencaoReferencia tipo) {
		try {
			// Primeiro buscar os períodos que serão excluídos
			List<PeriodoTrabalhoManutencao> periodosParaExcluir = buscarPorSolicitacao(solicitacaoId, tipo);

			// Depois excluir um por um para garantir que o EntityManager gerencia
			// corretamente
			for (PeriodoTrabalhoManutencao periodo : periodosParaExcluir) {
				excluir(periodo);
			}

			int quantidadeExcluida = periodosParaExcluir.size();
			LOGGER.debug("Excluídos {} períodos para solicitação {} do tipo {}", quantidadeExcluida, solicitacaoId,
					tipo);
			return quantidadeExcluida;

		} catch (Exception e) {
			LOGGER.error("Erro ao excluir períodos por solicitação {} e tipo {}: ", solicitacaoId, tipo, e);
			throw e;
		}
	}
}