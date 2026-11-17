package pe.edu.vallegrande.farmagro.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DelegatingTransactionDefinition;
import reactor.core.publisher.Mono;

@Configuration
public class R2dbcConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory) {
            @Override
            protected Mono<Void> doBegin(TransactionSynchronizationManager synchronizationManager,
                                         Object transaction,
                                         TransactionDefinition definition) {
                TransactionDefinition unnamed = new DelegatingTransactionDefinition(definition) {
                    @Override
                    public String getName() {
                        return null;
                    }
                };
                return super.doBegin(synchronizationManager, transaction, unnamed);
            }
        };
    }

    /** Programmatic transactions, for operations that must retry the whole transaction. */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }
}
