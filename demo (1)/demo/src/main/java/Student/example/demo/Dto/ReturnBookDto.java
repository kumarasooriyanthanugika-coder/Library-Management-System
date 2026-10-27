package Student.example.demo.Dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReturnBookDto {
    private Long studentid;
    private Long bookId;
    private Long borrowRecordId;
    private Integer numberOfBooks;
    private LocalDate returnDate;
    private String idempotencyKey;
}