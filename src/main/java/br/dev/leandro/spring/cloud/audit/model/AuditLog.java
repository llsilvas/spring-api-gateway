package br.dev.leandro.spring.cloud.audit.model;

import br.dev.leandro.spring.cloud.jwt.JwtToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "#{@indexNameProvider.getIndexName()}")
public class AuditLog {

    @Id
    private String id;

    private String correlationId;

    @Field(type = FieldType.Text)
    private String criado;

    @Field(type = FieldType.Text)
    private String endpoint;

    @Field(type = FieldType.Nested, includeInParent = true)
    private RequestLog request;

    @Field(type = FieldType.Nested, includeInParent = true)
    private ResponseLog response;

    private JwtToken jwtToken;

}
