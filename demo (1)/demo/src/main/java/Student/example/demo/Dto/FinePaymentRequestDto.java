package Student.example.demo.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinePaymentRequestDto {
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Payment must be greater than LKR 0.00")
    @Digits(integer = 10, fraction = 2, message = "Payment must have no more than two decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Payment idempotency key is required")
    private String idempotencyKey;
}
