package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;
import Student.example.demo.Service.FineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FineControllerTest {

    private FineController controller;
    private TestFakeFineService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new TestFakeFineService();
        controller = new FineController(fakeService);
    }

    @Test
    void getFineRecordsShouldReturnRecordsForStudent() {
        BorrowFineRecordDto record = new BorrowFineRecordDto();
        record.setBorrowRecordId(101L);
        record.setStudentId(1L);
        record.setTotalFine(new BigDecimal("60.00"));
        record.setUnpaidAmount(new BigDecimal("60.00"));
        record.setPaymentStatus("Unpaid");
        fakeService.recordsToReturn = List.of(record);

        List<BorrowFineRecordDto> result = controller.getFineRecords(1L);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getBorrowRecordId());
        assertEquals(new BigDecimal("60.00"), result.get(0).getTotalFine());
    }

    @Test
    void getFineRecordsShouldForwardStudentId() {
        controller.getFineRecords(42L);

        assertEquals(42L, fakeService.lastRequestedStudentId);
    }

    @Test
    void getFineRecordsShouldPropagateNotFoundWhenStudentNotFound() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getFineRecords(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Student not found", ex.getReason());
    }

    @Test
    void previewReturnShouldReturnPreviewFromService() {
        ReturnFinePreviewRequestDto request = previewRequest(1L, 10L, 100L, 1, LocalDate.of(2026, 10, 5));

        ReturnFinePreviewDto previewDto = new ReturnFinePreviewDto();
        previewDto.setBorrowRecordId(100L);
        previewDto.setStudentId(1L);
        previewDto.setReturningQuantity(1);
        previewDto.setLateDays(1L);
        previewDto.setLateFine(new BigDecimal("10.00"));
        previewDto.setTotalAmountDue(new BigDecimal("60.00"));
        fakeService.previewToReturn = previewDto;

        ReturnFinePreviewDto result = controller.previewReturn(request);

        assertNotNull(result);
        assertEquals(100L, result.getBorrowRecordId());
        assertEquals(1L, result.getLateDays());
        assertEquals(new BigDecimal("10.00"), result.getLateFine());
        assertEquals(new BigDecimal("60.00"), result.getTotalAmountDue());
    }

    @Test
    void previewReturnShouldForwardRequestToService() {
        ReturnFinePreviewRequestDto request = previewRequest(2L, 20L, 200L, 2, LocalDate.of(2026, 10, 6));

        controller.previewReturn(request);

        assertSame(request, fakeService.lastPreviewRequest);
        assertEquals(2L, fakeService.lastPreviewRequest.getStudentid());
        assertEquals(200L, fakeService.lastPreviewRequest.getBorrowRecordId());
    }

    @Test
    void previewReturnShouldPropagateBadRequestWhenExceedingQuantity() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return quantity exceeds this record's outstanding quantity");
        ReturnFinePreviewRequestDto request = previewRequest(1L, 10L, 100L, 5, LocalDate.of(2026, 10, 5));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.previewReturn(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createPaymentShouldReturnPaymentRecord() {
        FinePaymentRequestDto request = new FinePaymentRequestDto();
        request.setAmount(new BigDecimal("25.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        BorrowFineRecordDto updated = new BorrowFineRecordDto();
        updated.setBorrowRecordId(101L);
        updated.setStudentId(1L);
        updated.setPaidAmount(new BigDecimal("25.00"));
        updated.setUnpaidAmount(new BigDecimal("35.00"));
        updated.setPaymentStatus("Partially Paid");
        fakeService.paymentResultToReturn = updated;

        BorrowFineRecordDto result = controller.createPayment(1L, 101L, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("25.00"), result.getPaidAmount());
        assertEquals(new BigDecimal("35.00"), result.getUnpaidAmount());
        assertEquals("Partially Paid", result.getPaymentStatus());
    }

    @Test
    void createPaymentShouldForwardStudentIdBorrowRecordIdAndRequest() {
        FinePaymentRequestDto request = new FinePaymentRequestDto();
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        controller.createPayment(7L, 777L, request);

        assertEquals(7L, fakeService.lastPaymentStudentId);
        assertEquals(777L, fakeService.lastPaymentBorrowRecordId);
        assertSame(request, fakeService.lastPaymentRequest);
    }

    @Test
    void createPaymentShouldPropagateBadRequestWhenExceedingBalance() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed unpaid balance");
        FinePaymentRequestDto request = new FinePaymentRequestDto();
        request.setAmount(new BigDecimal("999.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.createPayment(1L, 101L, request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Payment cannot exceed unpaid balance", ex.getReason());
    }

    @Test
    void createPaymentShouldPropagateConflictWhenIdempotencyConflict() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.CONFLICT, "This payment request key was already used for a different payment");
        FinePaymentRequestDto request = new FinePaymentRequestDto();
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey(UUID.randomUUID().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.createPayment(1L, 101L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    private ReturnFinePreviewRequestDto previewRequest(Long studentId, Long bookId, Long recordId, int qty, LocalDate returnDate) {
        ReturnFinePreviewRequestDto dto = new ReturnFinePreviewRequestDto();
        dto.setStudentid(studentId);
        dto.setBookId(bookId);
        dto.setBorrowRecordId(recordId);
        dto.setNumberOfBooks(qty);
        dto.setReturnDate(returnDate);
        return dto;
    }

    private static class TestFakeFineService implements FineService {
        List<BorrowFineRecordDto> recordsToReturn = List.of();
        ReturnFinePreviewDto previewToReturn = new ReturnFinePreviewDto();
        BorrowFineRecordDto paymentResultToReturn = new BorrowFineRecordDto();
        ResponseStatusException exceptionToThrow;

        Long lastRequestedStudentId;
        ReturnFinePreviewRequestDto lastPreviewRequest;
        Long lastPaymentStudentId;
        Long lastPaymentBorrowRecordId;
        FinePaymentRequestDto lastPaymentRequest;

        @Override
        public List<BorrowFineRecordDto> getFineRecords(Long studentId) {
            this.lastRequestedStudentId = studentId;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return recordsToReturn;
        }

        @Override
        public BigDecimal getStudentTotalUnpaidAmount(Long studentId) {
            return BigDecimal.ZERO;
        }

        @Override
        public ReturnFinePreviewDto previewReturn(ReturnFinePreviewRequestDto request) {
            this.lastPreviewRequest = request;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return previewToReturn;
        }

        @Override
        public BorrowFineRecordDto createPayment(Long studentId, Long borrowRecordId, FinePaymentRequestDto request) {
            this.lastPaymentStudentId = studentId;
            this.lastPaymentBorrowRecordId = borrowRecordId;
            this.lastPaymentRequest = request;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return paymentResultToReturn;
        }
    }
}
