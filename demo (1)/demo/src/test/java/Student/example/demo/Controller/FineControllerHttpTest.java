package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;
import Student.example.demo.Service.FineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FineControllerHttpTest {

    private MockMvc mockMvc;
    private TestFakeFineService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new TestFakeFineService();
        FineController controller = new FineController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getFineRecordsShouldReturnOkWithListJson() throws Exception {
        BorrowFineRecordDto record = new BorrowFineRecordDto();
        record.setBorrowRecordId(101L);
        record.setStudentId(1L);
        record.setTotalFine(new BigDecimal("140.00"));
        record.setUnpaidAmount(new BigDecimal("130.00"));
        record.setPaidAmount(new BigDecimal("10.00"));
        record.setPaymentStatus("Partially Paid");
        fakeService.recordsToReturn = List.of(record);

        mockMvc.perform(get("/api/students/{studentId}/fine-records", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].borrowRecordId", is(101)))
                .andExpect(jsonPath("$[0].totalFine", is(140.00)))
                .andExpect(jsonPath("$[0].paymentStatus", is("Partially Paid")));
    }

    @Test
    void getFineRecordsWhenStudentNotFoundShouldReturnNotFound() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");

        mockMvc.perform(get("/api/students/{studentId}/fine-records", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void postFinePreviewWithValidBodyShouldReturnOkWithPreviewJson() throws Exception {
        ReturnFinePreviewDto previewDto = new ReturnFinePreviewDto();
        previewDto.setBorrowRecordId(1001L);
        previewDto.setStudentId(1L);
        previewDto.setReturningQuantity(1);
        previewDto.setLateDays(1L);
        previewDto.setLateFine(new BigDecimal("10.00"));
        previewDto.setTotalAmountDue(new BigDecimal("60.00"));
        previewDto.setRemainingUnpaidAmount(new BigDecimal("60.00"));
        previewDto.setPaymentStatus("Unpaid");
        fakeService.previewToReturn = previewDto;

        String payload = """
                {
                    "studentid": 1,
                    "bookId": 101,
                    "borrowRecordId": 1001,
                    "numberOfBooks": 1,
                    "returnDate": "2026-10-05"
                }
                """;

        mockMvc.perform(post("/api/return/fine-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.borrowRecordId", is(1001)))
                .andExpect(jsonPath("$.lateDays", is(1)))
                .andExpect(jsonPath("$.lateFine", is(10.00)))
                .andExpect(jsonPath("$.totalAmountDue", is(60.00)))
                .andExpect(jsonPath("$.paymentStatus", is("Unpaid")));
    }

    @Test
    void postFinePreviewWithInvalidBodyShouldReturnBadRequestDueToValidation() throws Exception {
        String invalidPayload = """
                {
                    "bookId": 101,
                    "borrowRecordId": 1001,
                    "numberOfBooks": -1
                }
                """;

        mockMvc.perform(post("/api/return/fine-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPaymentWithValidBodyShouldReturnOkWithUpdatedRecordJson() throws Exception {
        String key = UUID.randomUUID().toString();
        BorrowFineRecordDto recordDto = new BorrowFineRecordDto();
        recordDto.setBorrowRecordId(1001L);
        recordDto.setStudentId(1L);
        recordDto.setTotalFine(new BigDecimal("50.00"));
        recordDto.setPaidAmount(new BigDecimal("50.00"));
        recordDto.setUnpaidAmount(new BigDecimal("0.00"));
        recordDto.setPaymentStatus("Paid");
        fakeService.paymentResultToReturn = recordDto;

        String payload = """
                {
                    "amount": 50.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(key);

        mockMvc.perform(post("/api/students/{studentId}/borrow-records/{borrowRecordId}/payments", 1L, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.borrowRecordId", is(1001)))
                .andExpect(jsonPath("$.paidAmount", is(50.00)))
                .andExpect(jsonPath("$.unpaidAmount", is(0.00)))
                .andExpect(jsonPath("$.paymentStatus", is("Paid")));
    }

    @Test
    void postPaymentWithInvalidValidationShouldReturnBadRequest() throws Exception {
        String invalidPayload = """
                {
                    "amount": -10.00,
                    "idempotencyKey": ""
                }
                """;

        mockMvc.perform(post("/api/students/{studentId}/borrow-records/{borrowRecordId}/payments", 1L, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPaymentWhenOverpayingShouldReturnBadRequest() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed unpaid balance");

        String payload = """
                {
                    "amount": 500.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/students/{studentId}/borrow-records/{borrowRecordId}/payments", 1L, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPaymentWhenRecordNotFoundShouldReturnNotFound() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found");

        String payload = """
                {
                    "amount": 20.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/students/{studentId}/borrow-records/{borrowRecordId}/payments", 1L, 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void postPaymentWhenIdempotencyKeyConflictShouldReturnConflict() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.CONFLICT, "This payment request key was already used for a different payment");

        String payload = """
                {
                    "amount": 20.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/students/{studentId}/borrow-records/{borrowRecordId}/payments", 1L, 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    private static class TestFakeFineService implements FineService {
        List<BorrowFineRecordDto> recordsToReturn = List.of();
        ReturnFinePreviewDto previewToReturn = new ReturnFinePreviewDto();
        BorrowFineRecordDto paymentResultToReturn = new BorrowFineRecordDto();
        ResponseStatusException exceptionToThrow;

        @Override
        public List<BorrowFineRecordDto> getFineRecords(Long studentId) {
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
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return previewToReturn;
        }

        @Override
        public BorrowFineRecordDto createPayment(Long studentId, Long borrowRecordId, FinePaymentRequestDto request) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return paymentResultToReturn;
        }
    }
}
