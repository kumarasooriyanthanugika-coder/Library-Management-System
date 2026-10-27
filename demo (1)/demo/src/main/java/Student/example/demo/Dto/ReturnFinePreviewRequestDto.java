package Student.example.demo.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReturnFinePreviewRequestDto {
    @NotNull(message = "Student ID is required")
    private Long studentid;

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Borrow record ID is required")
    private Long borrowRecordId;

    @NotNull(message = "Return quantity is required")
    @Positive(message = "Return quantity must be a positive whole number")
    private Integer numberOfBooks;

    @NotNull(message = "Return date is required")
    private LocalDate returnDate;
}
