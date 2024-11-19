package br.dev.leandro.spring.cloud.audit.mapper;

import br.dev.leandro.spring.cloud.audit.dto.AuditLogDTO;
import br.dev.leandro.spring.cloud.audit.model.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "id", ignore = true)
    AuditLog toModel(AuditLogDTO dto);

    AuditLogDTO toDTO(AuditLog model);


}
