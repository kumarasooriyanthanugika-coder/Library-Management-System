package Student.example.demo.Controller;

import Student.example.demo.Dto.ReturnBookDto;
import Student.example.demo.Service.ReturnBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReturnBookControllerTest {

    private ReturnBookController controller;
    private InMemoryReturnBookService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryReturnBookService();
        controller = new ReturnBookController(fakeService);
    }

    @Test
    void createShouldReturnProcessedReturn() {
        ReturnBookDto input = returnDto(1L, 101L, 1001L, 2, LocalDate.of(2026, 10, 4));

        ReturnBookDto result = controller.create(input);

        assertNotNull(result);
        assertEquals(1L, result.getStudentid());
        assertEquals(101L, result.getBookId());
        assertEquals(1001L, result.getBorrowRecordId());
        assertEquals(2, result.getNumberOfBooks());
        assertEquals(LocalDate.of(2026, 10, 4), result.getReturnDate());
        assertEquals(input.getIdempotencyKey(), result.getIdempotencyKey());
    }

    @Test
    void createShouldForwardInputToService() {
        ReturnBookDto input = returnDto(2L, 102L, 1002L, 1, LocalDate.of(2026, 10, 6));

        controller.create(input);

        assertEquals(input.getStudentid(), fakeService.lastInput.getStudentid());
        assertEquals(input.getBookId(), fakeService.lastInput.getBookId());
        assertEquals(input.getBorrowRecordId(), fakeService.lastInput.getBorrowRecordId());
        assertEquals(input.getNumberOfBooks(), fakeService.lastInput.getNumberOfBooks());
        assertEquals(input.getReturnDate(), fakeService.lastInput.getReturnDate());
        assertEquals(input.getIdempotencyKey(), fakeService.lastInput.getIdempotencyKey());
    }

    @Test
    void createShouldPropagateBadRequestWhenValidationFails() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDs, return date and valid quantity are required");
        ReturnBookDto input = returnDto(null, 101L, 1001L, -1, null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.create(input));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("IDs, return date and valid quantity are required", ex.getReason());
    }

    @Test
    void createShouldPropagateNotFoundWhenBorrowRecordNotFound() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found");
        ReturnBookDto input = returnDto(1L, 101L, 9999L, 1, LocalDate.of(2026, 10, 4));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.create(input));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createShouldPropagateConflictWhenIdempotencyKeyReused() {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.CONFLICT, "This return request key was already used for a different return");
        ReturnBookDto input = returnDto(1L, 101L, 1001L, 1, LocalDate.of(2026, 10, 4));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.create(input));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    private ReturnBookDto returnDto(Long studentId, Long bookId, Long recordId, int quantity, LocalDate returnDate) {
        ReturnBookDto dto = new ReturnBookDto();
        dto.setStudentid(studentId);
        dto.setBookId(bookId);
        dto.setBorrowRecordId(recordId);
        dto.setNumberOfBooks(quantity);
        dto.setReturnDate(returnDate);
        dto.setIdempotencyKey(UUID.randomUUID().toString());
        return dto;
    }

    private static class InMemoryReturnBookService implements ReturnBookService {
        ReturnBookDto lastInput;
        ResponseStatusException exceptionToThrow;

        @Override
        public ReturnBookDto create(ReturnBookDto dto) {
            this.lastInput = dto;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return dto;
        }
    }
}
