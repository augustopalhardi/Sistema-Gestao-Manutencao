package com.augustopalhardi.manutencao.servlet;

import com.augustopalhardi.manutencao.util.Uploads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/uploads/*")
public class ImageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageServlet.class);

	private static final Set<String> INLINE_CONTENT_TYPES = new HashSet<>(
			Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"));

	private String BASE_PATH;

	@Override
	public void init() throws ServletException {
		// Diretório externo à aplicação (uploads.dir no config.properties)
		BASE_PATH = Uploads.diretorioBase();

		LOGGER.info("ImageServlet inicializado com BASE_PATH: {}", BASE_PATH);

		File baseDir = new File(BASE_PATH);
		if (!baseDir.exists()) {
			LOGGER.warn("Diretório base não existe: {}. Tentando criar...", BASE_PATH);
			if (baseDir.mkdirs()) {
				LOGGER.info("Diretório base criado com sucesso: {}", BASE_PATH);
			} else {
				LOGGER.error("Não foi possível criar o diretório base: {}", BASE_PATH);
			}
		} else {
			LOGGER.info("Diretório base existe: {}", BASE_PATH);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// Verifica se o usuário está logado
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("usuarioLogado") == null) {
			LOGGER.warn("Tentativa de acesso não autenticado ao /uploads/ por: {}", request.getRemoteAddr());
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado. Você precisa estar logado.");
			return;
		}

		String pathInfo = request.getPathInfo();
		LOGGER.info("Requisição recebida para: {}", pathInfo);

		if (pathInfo == null || pathInfo.equals("/")) {
			LOGGER.warn("Caminho inválido: {}", pathInfo);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caminho inválido");
			return;
		}

		// Remove a barra inicial
		if (pathInfo.startsWith("/")) {
			pathInfo = pathInfo.substring(1);
		}

		// Constrói o caminho completo do arquivo
		File imageFile = new File(BASE_PATH, pathInfo);
		LOGGER.info("Tentando acessar arquivo: {}", imageFile.getAbsolutePath());

		// Verifica se o arquivo existe
		if (!imageFile.exists()) {
			LOGGER.error("Arquivo não encontrado: {}", imageFile.getAbsolutePath());
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Arquivo não encontrado");
			return;
		}

		// Verifica se é um arquivo (não diretório)
		if (!imageFile.isFile()) {
			LOGGER.error("Caminho não é um arquivo: {}", imageFile.getAbsolutePath());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Caminho inválido");
			return;
		}

		// Proteção contra Path Traversal
		try {
			String canonicalBasePath = new File(BASE_PATH).getCanonicalPath();
			String canonicalFilePath = imageFile.getCanonicalPath();

			if (!canonicalFilePath.startsWith(canonicalBasePath)) {
				LOGGER.error("Tentativa de Path Traversal detectada! Base: {}, Arquivo: {}", canonicalBasePath,
						canonicalFilePath);
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado");
				return;
			}
		} catch (IOException e) {
			LOGGER.error("Erro ao verificar path traversal", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao processar requisição");
			return;
		}

		// Define o tipo de conteúdo
		String contentType = getContentType(imageFile);
		response.setContentType(contentType);
		response.setContentLengthLong(imageFile.length());

		// Headers de cache
		response.setHeader("Cache-Control", "public, max-age=3600");

		// Se o tipo for seguro (PNG, JPG), mostra "inline"
		// Se não for, força o download
		if (INLINE_CONTENT_TYPES.contains(contentType)) {
			response.setHeader("Content-Disposition", "inline; filename=\"" + imageFile.getName() + "\"");
		} else {
			response.setHeader("Content-Disposition", "attachment; filename=\"" + imageFile.getName() + "\"");
		}

		LOGGER.info("Servindo arquivo: {} - Tipo: {} - Tamanho: {} bytes", imageFile.getName(), contentType,
				imageFile.length());

		// Envia o arquivo
		try (FileInputStream fileInputStream = new FileInputStream(imageFile);
				OutputStream outputStream = response.getOutputStream()) {

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();

			LOGGER.info("Arquivo servido com sucesso: {}", imageFile.getName());

		} catch (IOException e) {
			LOGGER.error("Erro ao enviar arquivo: {}", imageFile.getName(), e);
			throw e;
		}
	}

	/**
	 * Detecta o tipo MIME do arquivo
	 */
	private String getContentType(File file) {
		// Tentar usar o probe do NIO
		try {
			String mimeType = Files.probeContentType(file.toPath());
			if (mimeType != null) {
				return mimeType;
			}
		} catch (IOException e) {
			LOGGER.debug("Não foi possível determinar MIME type via NIO para: {}", file.getName());
		}

		// Fallback baseado em extensão
		String fileName = file.getName().toLowerCase();
		int lastDotIndex = fileName.lastIndexOf('.');

		if (lastDotIndex > 0) {
			String extension = fileName.substring(lastDotIndex + 1);

			switch (extension) {
			case "jpg":
			case "jpeg":
				return "image/jpeg";
			case "png":
				return "image/png";
			case "gif":
				return "image/gif";
			case "bmp":
				return "image/bmp";
			case "webp":
				return "image/webp";
			case "svg":
				return "image/svg+xml";
			case "ico":
				return "image/x-icon";
			case "pdf":
				return "application/pdf";
			case "doc":
				return "application/msword";
			case "docx":
				return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
			case "xls":
				return "application/vnd.ms-excel";
			case "xlsx":
				return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
			default:
				return "application/octet-stream";
			}
		}

		return "application/octet-stream";
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	public void destroy() {
		super.destroy();
		LOGGER.info("ImageServlet sendo destruído");
	}
}