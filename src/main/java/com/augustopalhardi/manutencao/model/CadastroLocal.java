package com.augustopalhardi.manutencao.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class CadastroLocal implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nome;

	// Área em m². Opcional: local sem área informada é gravado como 0 e as
	// telas exibem "—" nesse caso.
	private int area;

	private String observacoes;

	// Nome do arquivo da foto dentro de <uploads.dir>/locais/, não a URL.
	private String urlFoto;

	// Nulo em registros anteriores à criação da coluna; ver isAtivo().
	@Column(name = "ativo")
	private Boolean ativo = true;

	public CadastroLocal() {
	}

	// ========== GETTERS E SETTERS ==========

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public int getArea() {
		return area;
	}

	public void setArea(int area) {
		this.area = area;
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

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
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
		CadastroLocal other = (CadastroLocal) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	// ========== MÉTODOS AUXILIARES ==========

	/**
	 * Trata o campo nulo como INATIVO.
	 *
	 * ATENÇÃO: o critério aqui é o oposto do adotado nas telas de local, onde
	 * CadastroLocalBean.isRegistroAtivo() trata nulo como ATIVO — que é o
	 * default da entidade para registros novos. A divergência é conhecida e já
	 * causou a coluna Status mostrar "Inativo" para um registro que o filtro
	 * considerava ativo.
	 *
	 * Em EL, "#{local.ativo}" resolve para este método, não para getAtivo().
	 * Para decidir se um local deve aparecer como ativo na interface, use
	 * isRegistroAtivo() do bean.
	 */
	public boolean isAtivo() {
		return ativo != null && ativo;
	}
}
