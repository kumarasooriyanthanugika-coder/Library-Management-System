package Student.example.demo.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class FinePaymentDto {
    private Long paymentId;
    private BigDecimal amount;
    private Instant paidAt;
}
