package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowRecordDto;
import Student.example.demo.Service.BorrowRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BorrowRecordControllerTest {

    private BorrowRecordController controller;
    private InMemoryBorrowRecordService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryBorrowRecordService();
        controller = new BorrowRecordController(fakeService);
    }

    @Test
    void createShouldReturnCreatedBorrowRecord() {
        BorrowRecordDto input = record(1L, 101L, 2, LocalDate.of(2026, 10, 1));

        BorrowRecordDto result = controller.create(input);

        assertNotNull(result.getBorrowRecordId());
        assertEquals(1L, result.getStudentid());
        assertEquals(101L, result.getBookId());
        assertEquals(2, result.getNumberOfBooks());
        assertEquals(LocalDate.of(2026, 10, 1), result.getBorrowDate());
        assertEquals(LocalDate.of(2026, 10, 4), result.getDueDate());
    }

    @Test
    void createShouldForwardInputToService() {
        BorrowRecordDto input = record(2L, 102L, 1, LocalDate.of(2026, 10, 5));

        BorrowRecordDto result = controller.create(input);

        assertEquals(input.getStudentid(), fakeService.lastCreatedInput.getStudentid());
        assertEquals(input.getBookId(), fakeService.lastCreatedInput.getBookId());
        assertEquals(input.getNumberOfBooks(), fakeService.lastCreatedInput.getNumberOfBooks());
        assertEquals(result.getBorrowRecordId(), fakeService.records.get(result.getBorrowRecordId()).getBorrowRecordId());
    }

    @Test
    void createShouldPropagateBadRequestWhenExceedingLimit() {
        fakeService.shouldThrowBadRequest = true;
        BorrowRecordDto input = record(1L, 101L, 6, LocalDate.of(2026, 10, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.create(input));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Student can borrow maximum 5 books only", ex.getReason());
    }

    @Test
    void getAllShouldReturnAllBorrowRecords() {
        controller.create(record(1L, 101L, 1, LocalDate.of(2026, 10, 1)));
        controller.create(record(2L, 102L, 2, LocalDate.of(2026, 10, 2)));

        List<BorrowRecordDto> result = controller.getAll();

        assertEquals(2, result.size());
    }

    @Test
    void getAllShouldReturnEmptyListWhenNoRecordsExist() {
        List<BorrowRecordDto> result = controller.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllShouldPropagateServiceError() {
        fakeService.shouldThrowOnGetAll = true;

        assertThrows(RuntimeException.class, () -> controller.getAll());
    }

    @Test
    void getActiveForStudentShouldReturnMatchingActiveRecords() {
        controller.create(record(1L, 101L, 1, LocalDate.of(2026, 10, 1)));
        controller.create(record(1L, 102L, 2, LocalDate.of(2026, 10, 2)));
        controller.create(record(2L, 103L, 1, LocalDate.of(2026, 10, 3)));

        List<BorrowRecordDto> student1Active = controller.getActiveForStudent(1L);

        assertEquals(2, student1Active.size());
        assertEquals(1L, student1Active.get(0).getStudentid());
        assertEquals(1L, student1Active.get(1).getStudentid());
    }

    @Test
    void getActiveForStudentShouldForwardStudentId() {
        controller.getActiveForStudent(42L);

        assertEquals(42L, fakeService.lastRequestedStudentId);
    }

    @Test
    void getActiveForStudentShouldPropagateNotFoundWhenStudentDoesNotExist() {
        fakeService.shouldThrowNotFoundOnActive = true;

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller.getActiveForStudent(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Student not found", ex.getReason());
    }

    private BorrowRecordDto record(Long studentId, Long bookId, int quantity, LocalDate borrowDate) {
        BorrowRecordDto dto = new BorrowRecordDto();
        dto.setStudentid(studentId);
        dto.setBookId(bookId);
        dto.setNumberOfBooks(quantity);
        dto.setBorrowDate(borrowDate);
        dto.setDueDate(borrowDate.plusDays(3));
        return dto;
    }

    private static class InMemoryBorrowRecordService implements BorrowRecordService {
        private final Map<Long, BorrowRecordDto> records = new LinkedHashMap<>();
        private long nextId = 1L;
        BorrowRecordDto lastCreatedInput;
        Long lastRequestedStudentId;
        boolean shouldThrowBadRequest = false;
        boolean shouldThrowOnGetAll = false;
        boolean shouldThrowNotFoundOnActive = false;

        @Override
        public BorrowRecordDto create(BorrowRecordDto dto) {
            if (shouldThrowBadRequest) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student can borrow maximum 5 books only");
            }
            this.lastCreatedInput = dto;
            dto.setBorrowRecordId(nextId++);
            records.put(dto.getBorrowRecordId(), dto);
            return dto;
        }

        @Override
        public List<BorrowRecordDto> getAll() {
            if (shouldThrowOnGetAll) {
                throw new RuntimeException("Service failure");
            }
            return new ArrayList<>(records.values());
        }

        @Override
        public List<BorrowRecordDto> getActiveForStudent(Long studentId) {
            this.lastRequestedStudentId = studentId;
            if (shouldThrowNotFoundOnActive) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
            }
            return records.values().stream()
                    .filter(r -> studentId.equals(r.getStudentid()) && r.getReturnDate() == null)
                    .toList();
        }
    }
}
