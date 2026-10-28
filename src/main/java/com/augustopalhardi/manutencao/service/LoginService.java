package com.augustopalhardi.manutencao.service;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.dao.CadastroUsuarioDao;

@RequestScoped
public class LoginService implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(LoginService.class);

	@Inject
	private CadastroUsuarioDao cadastroUsuarioDao;

	public CadastroUsuario autenticar(String nuRegistro, String senha) {
		try {
			LOGGER.info("Iniciando autenticação para nuRegistro: {}", nuRegistro);

			if (nuRegistro == null || nuRegistro.trim().isEmpty() || senha == null || senha.trim().isEmpty()) {
				LOGGER.warn("NuRegistro ou senha inválidos fornecidos.");
				return null;
			}

			// 1. Busca o usuário pelo nuRegistro
			CadastroUsuario usuario = cadastroUsuarioDao.buscarPorNuRegistro(nuRegistro);

			if (usuario == null) {
				LOGGER.warn("Tentativa de login com nuRegistro inexistente.");
				return null;
			}

			// 2. Verificação se usuário está ativo
			if (!usuario.isAtivo()) {
				LOGGER.warn("Tentativa de login de usuário inativo: {}", nuRegistro);
				return null;
			}

			// 3. Verificação de senha segura (BCrypt)
			if (BCrypt.checkpw(senha, usuario.getSenha())) {
				LOGGER.info("Autenticação bem-sucedida para usuário: {}", usuario.getNome());
				return usuario;
			} else {
				LOGGER.warn("Senha incorreta para nuRegistro: {}", nuRegistro);
				return null;
			}

		} catch (Exception e) {
			LOGGER.error("Erro durante autenticação: ", e);
			return null;
		}
	}

	public boolean usuarioExiste(String nuRegistro) {
		try {
			CadastroUsuario usuario = cadastroUsuarioDao.buscarPorNuRegistro(nuRegistro);
			return usuario != null;
		} catch (Exception e) {
			LOGGER.error("Erro ao verificar existência do usuário: ", e);
			return false;
		}
	}
}