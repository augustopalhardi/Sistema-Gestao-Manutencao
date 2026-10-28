package com.augustopalhardi.manutencao.util.jpa;

import java.io.Serializable;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
@Transactional
public class TransactionInterceptor implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionInterceptor.class);

	@Inject
	private EntityManager manager;

	@AroundInvoke
	public Object invoke(InvocationContext context) throws Exception {
		EntityTransaction transaction = manager.getTransaction();
		boolean owner = false;

		try {
			if (!transaction.isActive()) {
				transaction.begin();
				owner = true;
				LOGGER.debug("Transação iniciada para método: {}", context.getMethod().getName());
			}

			Object result = context.proceed();

			if (owner && transaction.isActive()) {
				transaction.commit();
				LOGGER.debug("Transação commitada para método: {}", context.getMethod().getName());
			}

			return result;

		} catch (Exception e) {
			if (owner && transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
					LOGGER.warn("Transação feita rollback para método: {} devido ao erro: {}",
							context.getMethod().getName(), e.getMessage());
				} catch (Exception rollbackException) {
					LOGGER.error("Erro ao fazer rollback da transação: ", rollbackException);
				}
			}
			throw e;
		}
	}
}