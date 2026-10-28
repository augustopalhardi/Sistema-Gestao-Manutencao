package com.augustopalhardi.manutencao.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto único para gravar, localizar e excluir arquivos enviados pelos usuários.
 *
 * Os arquivos ficam em um diretório externo à aplicação (chave
 * {@code uploads.dir} do config.properties; padrão {@code /opt/uploads}), para
 * que um novo deploy não apague as fotos. O {@code ImageServlet} os entrega na
 * URL {@code /uploads/<pasta>/<arquivo>}.
 */
public final class Uploads {

	private static final Logger LOGGER = LoggerFactory.getLogger(Uploads.class);

	/** Prefixo das URLs atendidas pelo ImageServlet. */
	public static final String URL_BASE = "/uploads/";

	public static final String USUARIOS = "usuarios/";
	public static final String LOCAIS = "locais/";
	public static final String MAQUINAS = "maquinas_equipamentos/";
	public static final String SOLICITACOES_MAQUINAS = "solicitacoes_maquinas/";
	public static final String SOLICITACOES_PREDIAL = "solicitacoes_predial/";

	private Uploads() {
	}

	/** Diretório raiz no disco, sempre terminado em "/". */
	public static String diretorioBase() {
		String base = Config.get("uploads.dir", "/opt/uploads").replace('\\', '/');
		return base.endsWith("/") ? base : base + "/";
	}

	/** Caminho no disco de uma subpasta (ex.: {@code diretorio(LOCAIS)}). */
	public static String diretorio(String pasta) {
		return diretorioBase() + pasta;
	}

	/** URL pública de uma subpasta (ex.: {@code /uploads/locais/}). */
	public static String url(String pasta) {
		return URL_BASE + pasta;
	}

	/**
	 * Grava o arquivo na subpasta com um nome único e seguro.
	 *
	 * @return apenas o nome do arquivo gerado (sem diretório)
	 */
	public static String salvar(UploadedFile arquivo, String pasta) throws IOException {
		File dir = new File(diretorio(pasta));
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Não foi possível criar o diretório de upload: " + dir.getAbsolutePath());
		}

		String nomeUnico = System.currentTimeMillis() + "_" + nomeSeguro(arquivo.getFileName());
		File destino = new File(dir, nomeUnico);
		try (InputStream in = arquivo.getInputStream()) {
			Files.copy(in, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		LOGGER.info("Arquivo salvo em: {}", destino.getAbsolutePath());
		return nomeUnico;
	}

	/**
	 * Exclui do disco o arquivo referenciado por uma URL ou nome salvo no banco.
	 * Só o nome final é considerado, o que impede apagar arquivos fora da pasta.
	 */
	public static void excluir(String urlOuNome, String pasta) {
		if (urlOuNome == null || urlOuNome.trim().isEmpty()) {
			return;
		}
		String nome = urlOuNome.substring(urlOuNome.lastIndexOf('/') + 1);
		File arquivo = new File(diretorio(pasta), nome);
		if (arquivo.exists() && !arquivo.delete()) {
			LOGGER.warn("Não foi possível excluir o arquivo: {}", arquivo.getAbsolutePath());
		}
	}

	/** Remove caminho do cliente e troca caracteres especiais por "_". */
	static String nomeSeguro(String nomeOriginal) {
		String nome = nomeOriginal == null ? "arquivo" : nomeOriginal;
		nome = nome.substring(Math.max(nome.lastIndexOf('/'), nome.lastIndexOf('\\')) + 1);
		nome = nome.replaceAll("[^a-zA-Z0-9._-]", "_");
		return nome.isEmpty() ? "arquivo" : nome;
	}
}
