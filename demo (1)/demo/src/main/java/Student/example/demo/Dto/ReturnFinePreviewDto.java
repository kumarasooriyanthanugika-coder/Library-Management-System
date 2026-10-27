package Student.example.demo.Dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReturnFinePreviewDto {
    private Long borrowRecordId;
    private Long studentId;
    private String studentName;
    private Long bookId;
    private String bookTitle;
    private Integer returningQuantity;
    private Integer outstandingQuantity;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Long daysSinceBorrowing;
    private Long lateDays;
    private BigDecimal baseBorrowingCharge;
    private BigDecimal baseChargeForReturningQuantity;
    private BigDecimal fineForOneBook;
    private BigDecimal fineForReturningQuantity;
    private BigDecimal lateFine;
    private BigDecimal totalAmountDue;
    private BigDecimal alreadyPaidAmount;
    private BigDecimal remainingUnpaidAmount;
    private String paymentStatus;
}
