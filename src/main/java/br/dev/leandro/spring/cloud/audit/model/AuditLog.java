package br.dev.leandro.spring.cloud.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Map;

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

    @Field(type = FieldType.Keyword)
    private String method;

    @Field(type = FieldType.Text)
    private String endpoint;

    @Field(type = FieldType.Text)
    private String requestBody;

    @Field(type = FieldType.Integer)
    private int responseStatus;

    @Field(type = FieldType.Text)
    private String responseBody;

    @Field(type = FieldType.Object)
    private Map<String, String> requestHeaders;

    @Field(type = FieldType.Object)
    private Map<String, String> responseHeaders;

    @Field(type = FieldType.Object)
    private Map<String, String> requestParams;

    private String tipo;

}
