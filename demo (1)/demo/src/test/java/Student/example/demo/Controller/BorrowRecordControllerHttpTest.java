package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowRecordDto;
import Student.example.demo.Service.BorrowRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BorrowRecordControllerHttpTest {

    private MockMvc mockMvc;
    private InMemoryBorrowRecordService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryBorrowRecordService();
        BorrowRecordController controller = new BorrowRecordController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postBorrowRecordShouldReturnOkWithJson() throws Exception {
        String payload = """
                {
                    "Studentid": 1,
                    "BookId": 101,
                    "numberOfBooks": 2,
                    "borrowDate": "2026-10-01",
                    "dueDate": "2026-10-04"
                }
                """;

        mockMvc.perform(post("/api/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.borrowRecordId", notNullValue()))
                .andExpect(jsonPath("$.Studentid", is(1)))
                .andExpect(jsonPath("$.BookId", is(101)))
                .andExpect(jsonPath("$.numberOfBooks", is(2)))
                .andExpect(jsonPath("$.borrowDate", is("2026-10-01")))
                .andExpect(jsonPath("$.dueDate", is("2026-10-04")));
    }

    @Test
    void postBorrowRecordWhenServiceRejectsShouldReturnBadRequest() throws Exception {
        fakeService.shouldThrowBadRequest = true;
        String payload = """
                {
                    "Studentid": 1,
                    "BookId": 101,
                    "numberOfBooks": 6,
                    "borrowDate": "2026-10-01"
                }
                """;

        mockMvc.perform(post("/api/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllBorrowRecordsShouldReturnOkWithListJson() throws Exception {
        fakeService.create(record(1L, 101L, 1, LocalDate.of(2026, 10, 1)));
        fakeService.create(record(2L, 102L, 2, LocalDate.of(2026, 10, 2)));

        mockMvc.perform(get("/api/borrow"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].Studentid", is(1)))
                .andExpect(jsonPath("$[1].Studentid", is(2)));
    }

    @Test
    void getActiveForStudentShouldReturnOkWithActiveListJson() throws Exception {
        fakeService.create(record(1L, 101L, 1, LocalDate.of(2026, 10, 1)));
        fakeService.create(record(2L, 102L, 2, LocalDate.of(2026, 10, 2)));

        mockMvc.perform(get("/api/borrow/student/{studentId}/active", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].Studentid", is(1)))
                .andExpect(jsonPath("$[0].BookId", is(101)));
    }

    @Test
    void getActiveForStudentWhenNotFoundShouldReturnNotFound() throws Exception {
        fakeService.shouldThrowNotFound = true;

        mockMvc.perform(get("/api/borrow/student/{studentId}/active", 999L))
                .andExpect(status().isNotFound());
    }

    private BorrowRecordDto record(Long studentId, Long bookId, int qty, LocalDate borrowDate) {
        BorrowRecordDto dto = new BorrowRecordDto();
        dto.setStudentid(studentId);
        dto.setBookId(bookId);
        dto.setNumberOfBooks(qty);
        dto.setBorrowDate(borrowDate);
        dto.setDueDate(borrowDate.plusDays(3));
        return dto;
    }

    private static class InMemoryBorrowRecordService implements BorrowRecordService {
        private final Map<Long, BorrowRecordDto> records = new LinkedHashMap<>();
        private long nextId = 1L;
        boolean shouldThrowBadRequest = false;
        boolean shouldThrowNotFound = false;

        @Override
        public BorrowRecordDto create(BorrowRecordDto dto) {
            if (shouldThrowBadRequest) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeds max allowed books");
            }
            dto.setBorrowRecordId(nextId++);
            records.put(dto.getBorrowRecordId(), dto);
            return dto;
        }

        @Override
        public List<BorrowRecordDto> getAll() {
            return new ArrayList<>(records.values());
        }

        @Override
        public List<BorrowRecordDto> getActiveForStudent(Long studentId) {
            if (shouldThrowNotFound) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
            }
            return records.values().stream()
                    .filter(r -> studentId.equals(r.getStudentid()))
                    .toList();
        }
    }
}
