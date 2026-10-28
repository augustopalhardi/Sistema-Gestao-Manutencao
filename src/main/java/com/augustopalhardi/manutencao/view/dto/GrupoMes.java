package com.augustopalhardi.manutencao.view.dto;

import java.io.Serializable;
import java.util.List;

public class GrupoMes implements Serializable {
	private static final long serialVersionUID = 1L;

	private String mesAno;
	private List<ItemPreventiva> itens;

	public GrupoMes(String mesAno, List<ItemPreventiva> itens) {
		this.mesAno = mesAno;
		this.itens = itens;
	}

	public String getMesAno() {
		return mesAno;
	}

	public void setMesAno(String mesAno) {
		this.mesAno = mesAno;
	}

	public List<ItemPreventiva> getItens() {
		return itens;
	}

	public void setItens(List<ItemPreventiva> itens) {
		this.itens = itens;
	}

	public int getQuantidade() {
		return itens != null ? itens.size() : 0;
	}
}
