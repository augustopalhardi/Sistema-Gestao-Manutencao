package com.augustopalhardi.manutencao.model;

/**
 * Enum centralizado que identifica se uma manutenção é de MÁQUINA ou PREDIAL.
 *
 * Utilizado como discriminador nas entidades que referenciam solicitações de
 * manutenção de forma polimórfica (via Long solicitacaoId), como:
 * ControleStatusManutencao, PeriodoTrabalhoManutencao,
 * SolicitacaoCompraMateriaisServicos e HistoricoReprogramacao.
 */
public enum TipoManutencaoReferencia {
	MAQUINA("Máquina"), PREDIAL("Predial");

	private final String descricao;

	TipoManutencaoReferencia(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}
}