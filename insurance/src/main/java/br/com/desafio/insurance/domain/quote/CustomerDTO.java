package br.com.desafio.insurance.domain.quote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO para dados do cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {

    @NotBlank(message = "Número de documento é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "Documento deve conter 11 dígitos")
    private String documentNumber;

    @NotBlank(message = "Nome do cliente é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    private String name;

    @NotNull(message = "Tipo de cliente é obrigatório")
    private CustomerType type;

    @NotNull(message = "Gênero é obrigatório")
    private Gender gender;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotNull(message = "Número de telefone é obrigatório")
    @Min(value = 10, message = "Número de telefone inválido")
    private Long phoneNumber;
}