package Student.example.demo.Controller;

import Student.example.demo.Dto.ReturnBookDto;
import Student.example.demo.Service.ReturnBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReturnBookControllerHttpTest {

    private MockMvc mockMvc;
    private TestFakeReturnBookService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new TestFakeReturnBookService();
        ReturnBookController controller = new ReturnBookController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postReturnShouldReturnOkWithProcessedReturnJson() throws Exception {
        String key = UUID.randomUUID().toString();
        String payload = """
                {
                    "studentid": 1,
                    "bookId": 101,
                    "borrowRecordId": 1001,
                    "numberOfBooks": 2,
                    "returnDate": "2026-10-04",
                    "idempotencyKey": "%s"
                }
                """.formatted(key);

        mockMvc.perform(post("/api/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.studentid", is(1)))
                .andExpect(jsonPath("$.bookId", is(101)))
                .andExpect(jsonPath("$.borrowRecordId", is(1001)))
                .andExpect(jsonPath("$.numberOfBooks", is(2)))
                .andExpect(jsonPath("$.returnDate", is("2026-10-04")))
                .andExpect(jsonPath("$.idempotencyKey", is(key)));
    }

    @Test
    void postReturnWithMalformedJsonShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json-content}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReturnWhenServiceThrowsBadRequestShouldReturnBadRequest() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDs, return date and valid quantity are required");

        String payload = """
                {
                    "studentid": 1
                }
                """;

        mockMvc.perform(post("/api/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReturnWhenServiceThrowsNotFoundShouldReturnNotFound() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.NOT_FOUND, "Borrow record not found");

        String payload = """
                {
                    "studentid": 1,
                    "borrowRecordId": 9999
                }
                """;

        mockMvc.perform(post("/api/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void postReturnWhenServiceThrowsConflictShouldReturnConflict() throws Exception {
        fakeService.exceptionToThrow = new ResponseStatusException(HttpStatus.CONFLICT, "This return request key was already used for a different return");

        String payload = """
                {
                    "idempotencyKey": "%s"
                }
                """.formatted(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    private static class TestFakeReturnBookService implements ReturnBookService {
        ResponseStatusException exceptionToThrow;

        @Override
        public ReturnBookDto create(ReturnBookDto dto) {
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return dto;
        }
    }
}
