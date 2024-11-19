package br.dev.leandro.spring.cloud.audit.repository;

import br.dev.leandro.spring.cloud.audit.model.AuditLog;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AuditLogRepository extends ReactiveElasticsearchRepository<AuditLog, String> {

}
