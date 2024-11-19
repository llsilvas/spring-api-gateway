package br.dev.leandro.spring.cloud.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Map;

@Data
@AllArgsConstructor
public class ResponseLog {
    @Field(type = FieldType.Object)
    private Map<String, String> responseHeaders;
    @Field(type = FieldType.Text)
    private String responseBody;
    @Field(type = FieldType.Integer)
    private Integer responseStatus;

}
