package br.dev.leandro.spring.cloud.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@AllArgsConstructor
@Data
public class RequestLog {
    @Field(type = FieldType.Keyword)
    private String method;
    @Field(type = FieldType.Text)
    private String clientIp;
    @Field(type = FieldType.Text)
    private String userAgent;
    @Field(type = FieldType.Object)
    private Map<String, String> requestHeaders;
    @Field(type = FieldType.Object)
    private Map<String, String> requestParams;
    @Field(type = FieldType.Text)
    private String requestBody;

}
