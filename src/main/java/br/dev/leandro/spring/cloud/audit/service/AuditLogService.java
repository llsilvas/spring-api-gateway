package br.dev.leandro.spring.cloud.audit.service;

import br.dev.leandro.spring.cloud.audit.dto.AuditLogDTO;
import br.dev.leandro.spring.cloud.audit.mapper.AuditLogMapper;
import br.dev.leandro.spring.cloud.audit.model.AuditLog;
import br.dev.leandro.spring.cloud.audit.repository.AuditLogRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Log4j2
@Service
public class AuditLogService {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    public Mono<Void> salvarLog(AuditLogDTO auditLogDTO) {
        try {
            log.info("Iniciando salvamento do log para correlationId={}", auditLogDTO.correlationId());

            AuditLog auditLog = auditLogMapper.toModel(auditLogDTO);

            auditLog.setId(UUID.randomUUID().toString());
            auditLog.setCriado(LocalDateTime.now().format(formatter));

            // Salvar no Elasticsearch
            return auditLogRepository.save(auditLog)
                    .doOnSuccess(savedLog -> log.info("Log salvo com sucesso para correlationId={}", auditLogDTO.correlationId()))
                    .doOnError(e -> log.error("Erro ao salvar log para correlationId={}: {}", auditLogDTO.correlationId(), e.getMessage(), e))
                    .then();

        } catch (Exception e) {
            log.error("Erro ao mapear ou salvar log: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
}
