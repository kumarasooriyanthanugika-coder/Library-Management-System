package Student.example.demo.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BorrowFineRecordDto {
    private Long borrowRecordId;
    private Long studentId;
    private Long bookId;
    private String bookTitle;
    private Integer borrowedQuantity;
    private Integer outstandingQuantity;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Long daysSinceBorrowing;
    private Long lateDays;
    private BigDecimal baseBorrowingCharge;
    private BigDecimal lateFine;
    private BigDecimal totalFine;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private String paymentStatus;
    private List<FinePaymentDto> paymentHistory;
}
