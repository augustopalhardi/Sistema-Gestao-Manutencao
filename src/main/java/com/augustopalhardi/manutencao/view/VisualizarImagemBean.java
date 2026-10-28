package com.augustopalhardi.manutencao.view;

import com.augustopalhardi.manutencao.util.Uploads;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean global do dialog de visualização de imagens.
 *
 * Centraliza TODA a exibição de fotos do sistema (locais, máquinas e
 * equipamentos, solicitações de manutenção etc.) em um único dialog,
 * declarado em /includes/visualizarImagemDialog.xhtml.
 *
 * Uso a partir de qualquer página ou bean:
 *
 * <pre>
 * visualizarImagemBean.abrir("Foto do Local", "locais/" + local.getUrlFoto());
 * visualizarImagemBean.addInfo("Nome", local.getNome());
 * </pre>
 *
 * Depois basta o botão fazer:
 * update=":dlgVisualizarImagemForm:dlgVisualizarImagemPanel"
 * oncomplete="PF('dlgVisualizarImagem').show();"
 *
 * Assim, qualquer ajuste de layout, tamanho ou tratamento de "sem foto"
 * é feito em um lugar só e vale para o sistema inteiro.
 */
@Named
@SessionScoped
public class VisualizarImagemBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(VisualizarImagemBean.class);

	/** Pasta raiz servida pelo ImageServlet (@WebServlet("/uploads/*")). */
	private static final String BASE_UPLOADS = Uploads.URL_BASE;

	/** Título exibido no cabeçalho do dialog. */
	private String titulo = "Imagem";

	/** Texto opcional exibido logo abaixo do título (ex.: nome do local). */
	private String subtitulo;

	/**
	 * Caminho da foto. Pode ser relativo à pasta de uploads
	 * ("locais/foto.jpg") ou já absoluto ("/uploads/locais/foto.jpg").
	 */
	private String caminhoFoto;

	/** Mensagem exibida quando não há foto para o registro. */
	private String mensagemSemFoto = "Nenhuma foto anexada a este registro.";

	/** Linhas do quadro de informações exibido abaixo da imagem. */
	private List<ItemInfo> informacoes = new ArrayList<>();

	// ==========================================
	// ABERTURA DO DIALOG
	// ==========================================

	/**
	 * Prepara o dialog para exibir uma imagem. Sempre limpa o estado
	 * anterior, para não vazar dados de um registro para outro.
	 *
	 * @param titulo      cabeçalho do dialog
	 * @param caminhoFoto caminho relativo ("locais/foto.jpg") ou absoluto
	 */
	public void abrir(String titulo, String caminhoFoto) {
		limpar();
		this.titulo = (titulo == null || titulo.trim().isEmpty()) ? "Imagem" : titulo;
		this.caminhoFoto = caminhoFoto;

		LOGGER.info("Dialog de imagem preparado. Titulo: '{}', foto: '{}'", this.titulo, this.caminhoFoto);
	}

	/**
	 * Igual ao abrir(titulo, caminhoFoto), com um subtítulo em destaque.
	 */
	public void abrir(String titulo, String subtitulo, String caminhoFoto) {
		abrir(titulo, caminhoFoto);
		this.subtitulo = subtitulo;
	}

	/** Adiciona uma linha ao quadro de informações. Valores nulos viram "-". */
	public void addInfo(String rotulo, Object valor) {
		String texto = (valor == null || String.valueOf(valor).trim().isEmpty()) ? "-" : String.valueOf(valor);
		informacoes.add(new ItemInfo(rotulo, texto));
	}

	/** Zera todo o estado do dialog. */
	public void limpar() {
		this.titulo = "Imagem";
		this.subtitulo = null;
		this.caminhoFoto = null;
		this.mensagemSemFoto = "Nenhuma foto anexada a este registro.";
		this.informacoes = new ArrayList<>();
	}

	// ==========================================
	// CONSULTAS USADAS PELO XHTML
	// ==========================================

	/** Indica se há foto para exibir. */
	public boolean isPossuiFoto() {
		return caminhoFoto != null && !caminhoFoto.trim().isEmpty();
	}

	/**
	 * Monta a URL final entregue ao ImageServlet. Aceita tanto caminho
	 * relativo quanto absoluto, mesma regra já usada em
	 * DetalhesManutencaoBean.getUrlFotoCorrigida().
	 */
	public String getUrlFotoCorrigida() {
		if (!isPossuiFoto()) {
			return null;
		}

		String foto = caminhoFoto.trim();

		if (foto.startsWith("/")) {
			return foto;
		}

		return BASE_UPLOADS + foto;
	}

	/** Indica se há linhas no quadro de informações. */
	public boolean isPossuiInformacoes() {
		return informacoes != null && !informacoes.isEmpty();
	}

	// ==========================================
	// GETTERS E SETTERS
	// ==========================================

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getSubtitulo() {
		return subtitulo;
	}

	public void setSubtitulo(String subtitulo) {
		this.subtitulo = subtitulo;
	}

	public String getCaminhoFoto() {
		return caminhoFoto;
	}

	public void setCaminhoFoto(String caminhoFoto) {
		this.caminhoFoto = caminhoFoto;
	}

	public String getMensagemSemFoto() {
		return mensagemSemFoto;
	}

	public void setMensagemSemFoto(String mensagemSemFoto) {
		this.mensagemSemFoto = mensagemSemFoto;
	}

	public List<ItemInfo> getInformacoes() {
		return informacoes;
	}

	public void setInformacoes(List<ItemInfo> informacoes) {
		this.informacoes = informacoes;
	}

	// ==========================================
	// LINHA DO QUADRO DE INFORMAÇÕES
	// ==========================================

	public static class ItemInfo implements Serializable {

		private static final long serialVersionUID = 1L;

		private final String rotulo;
		private final String valor;

		public ItemInfo(String rotulo, String valor) {
			this.rotulo = rotulo;
			this.valor = valor;
		}

		public String getRotulo() {
			return rotulo;
		}

		public String getValor() {
			return valor;
		}
	}
}