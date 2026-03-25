package br.com.desafio.insurance.domain.entity;

import br.com.desafio.insurance.domain.quote.CustomerType;
import br.com.desafio.insurance.domain.quote.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;


@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    private String documentNumber;
    private String name;
    private CustomerType type;
    private Gender gender;
    private String dateOfBirth;
    private String email;
    private Long phoneNumber;
}



