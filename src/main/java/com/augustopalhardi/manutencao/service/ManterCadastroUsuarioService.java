package com.augustopalhardi.manutencao.service;

import java.io.Serializable;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.mindrot.jbcrypt.BCrypt;

import com.augustopalhardi.manutencao.model.CadastroUsuario;
import com.augustopalhardi.manutencao.dao.CadastroUsuarioDao;
import com.augustopalhardi.manutencao.util.jpa.Transactional;

@ApplicationScoped
public class ManterCadastroUsuarioService implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private CadastroUsuarioDao cadastroUsuarioDao;

	@Transactional
	public CadastroUsuario salvar(CadastroUsuario cadastroUsuario) {

		// --- 2. LÓGICA DE HASH ANTES DE SALVAR ---
		// Verifica se a senha foi preenchida (não é nula nem vazia)
		if (cadastroUsuario.getSenha() != null && !cadastroUsuario.getSenha().trim().isEmpty()) {

			// IMPORTANTE: Só faz o hash se a senha NÃO for um hash.
			// Isso impede o "re-hashing" de um hash existente se o usuário
			// estiver apenas salvando outras informações do perfil sem
			// alterar a senha. (Hashes BCrypt começam com $2a$, $2b$, ou $2y$)
			String senhaAtual = cadastroUsuario.getSenha();

			if (!senhaAtual.startsWith("$2a$") && !senhaAtual.startsWith("$2b$") && !senhaAtual.startsWith("$2y$")) {

				// A senha é texto plano. Vamos gerar o hash.
				String hashDaSenha = BCrypt.hashpw(senhaAtual, BCrypt.gensalt());

				// Atualiza a senha no objeto para o HASH antes de salvar
				cadastroUsuario.setSenha(hashDaSenha);
			}
		}
		// --- FIM DA LÓGICA DE HASH ---

		// 3. Salva o usuário no banco (agora com a senha hasheada)
		return cadastroUsuarioDao.salvar(cadastroUsuario);
	}

	public List<CadastroUsuario> buscarTodos() {
		return cadastroUsuarioDao.buscarTodos();
	}

	@Transactional
	public void excluir(CadastroUsuario cadastroUsuario) {
		cadastroUsuarioDao.excluir(cadastroUsuario);
	}

	public CadastroUsuario buscarPorId(Long usuarioId) {
		return cadastroUsuarioDao.buscarPeloCodigo(usuarioId);
	}

	public CadastroUsuario buscarPorNuRegistro(String nuRegistro) {
		return cadastroUsuarioDao.buscarPorNuRegistro(nuRegistro);
	}

	public CadastroUsuario buscarPorEmail(String email) {
		return cadastroUsuarioDao.buscarPorEmail(email);
	}

	public CadastroUsuario buscarPorTelefone(String telefone) {
		return cadastroUsuarioDao.buscarPorTelefone(telefone);
	}

	@Transactional
	public void alternarStatus(CadastroUsuario usuario) {
		usuario.setAtivo(!usuario.isAtivo());
		cadastroUsuarioDao.salvar(usuario);
	}
}