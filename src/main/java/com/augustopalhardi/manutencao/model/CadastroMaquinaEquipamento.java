package com.augustopalhardi.manutencao.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
public class CadastroMaquinaEquipamento implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "numero_patrimonio", unique = true, length = 50)
	private String numeroPatrimonio;

	private String fabricante;
	private String modelo;
	private String tipo;

	@Temporal(TemporalType.DATE)
	private Date anoFabricacao;

	private String potencia;
	private String lubrificacao;

	// onde a máquina está fisicamente (texto livre)
	@Column(name = "localizacao", length = 255)
	private String localizacao;

	@Column(length = 1000)
	private String observacoes;

	private String urlFoto;

	// Ativo/Inativo
	@Column(name = "ativo")
	private Boolean ativo = true;

	// Construtores
	public CadastroMaquinaEquipamento() {
	}

	// Getters e Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNumeroPatrimonio() {
		return numeroPatrimonio;
	}

	public void setNumeroPatrimonio(String numeroPatrimonio) {
		this.numeroPatrimonio = numeroPatrimonio;
	}

	public String getFabricante() {
		return fabricante;
	}

	public void setFabricante(String fabricante) {
		this.fabricante = fabricante;
	}

	public String getModelo() {
		return modelo;
	}

	public void setModelo(String modelo) {
		this.modelo = modelo;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Date getAnoFabricacao() {
		return anoFabricacao;
	}

	public void setAnoFabricacao(Date anoFabricacao) {
		this.anoFabricacao = anoFabricacao;
	}

	public String getPotencia() {
		return potencia;
	}

	public void setPotencia(String potencia) {
		this.potencia = potencia;
	}

	public String getLocalizacao() {
		return localizacao;
	}

	public void setLocalizacao(String localizacao) {
		this.localizacao = localizacao;
	}

	public String getLubrificacao() {
		return lubrificacao;
	}

	public void setLubrificacao(String lubrificacao) {
		this.lubrificacao = lubrificacao;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public String getUrlFoto() {
		return urlFoto;
	}

	public void setUrlFoto(String urlFoto) {
		this.urlFoto = urlFoto;
	}

	// GETTER e SETTER do campo Ativo
	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	// Método auxiliar para verificar se está ativo
	public boolean isAtivo() {
		return ativo != null && ativo;
	}

	/**
	 * FORMATO PADRÃO DE IDENTIFICAÇÃO DA MÁQUINA NO SISTEMA INTEIRO.
	 *
	 * Produz "tipo - fabricante modelo", por exemplo:
	 *
	 * <pre>
	 *   Torno CNC - Romi GL240
	 * </pre>
	 *
	 * Antes desta padronização havia oito formatos diferentes espalhados pelo
	 * sistema (só fabricante+modelo, só tipo, campos separados, etc.), cada um
	 * montado por concatenação em um bean diferente. Agora TODOS os pontos que
	 * exibem uma máquina chamam este método — telas do técnico, consulta,
	 * todas as solicitações, cronograma, dashboard, relatórios e e-mails.
	 *
	 * Para mudar o formato do sistema inteiro, mude AQUI e em nenhum outro
	 * lugar. Se precisar de uma variação pontual (por exemplo incluir a
	 * localização em uma tela específica), crie um método novo em vez de
	 * alterar este.
	 *
	 * Partes vazias são omitidas sem deixar separador solto, então uma máquina
	 * sem tipo cadastrado sai como "Romi GL240" em vez de " - Romi GL240".
	 * Nunca devolve null nem imprime a palavra "null" na tela — esse era um
	 * defeito das concatenações antigas, que não checavam campo vazio.
	 */
	public String getIdentificacao() {
		String tipoLimpo = limpar(tipo);
		String fabricanteLimpo = limpar(fabricante);
		String modeloLimpo = limpar(modelo);

		// Fabricante e modelo andam juntos, separados por espaço
		StringBuilder equipamento = new StringBuilder();

		if (fabricanteLimpo != null) {
			equipamento.append(fabricanteLimpo);
		}

		if (modeloLimpo != null) {
			if (equipamento.length() > 0) {
				equipamento.append(" ");
			}
			equipamento.append(modeloLimpo);
		}

		// O tipo vem na frente, separado por hífen
		StringBuilder identificacao = new StringBuilder();

		if (tipoLimpo != null) {
			identificacao.append(tipoLimpo);
		}

		if (equipamento.length() > 0) {
			if (identificacao.length() > 0) {
				identificacao.append(" - ");
			}
			identificacao.append(equipamento);
		}

		if (identificacao.length() > 0) {
			return identificacao.toString();
		}

		// Nada preenchido: cai para o patrimônio e, em último caso, para o id.
		// Sem isso a máquina apareceria como uma linha em branco na tela.
		String patrimonioLimpo = limpar(numeroPatrimonio);

		if (patrimonioLimpo != null) {
			return patrimonioLimpo;
		}

		return id != null ? ("Equipamento #" + id) : "Sem identificação";
	}

	/** Devolve o texto sem espaços nas pontas, ou null se não houver conteúdo. */
	private static String limpar(String texto) {
		if (texto == null) {
			return null;
		}
		String limpo = texto.trim();
		return limpo.isEmpty() ? null : limpo;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		CadastroMaquinaEquipamento other = (CadastroMaquinaEquipamento) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}