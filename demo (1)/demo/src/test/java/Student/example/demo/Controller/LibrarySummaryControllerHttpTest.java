package Student.example.demo.Controller;

import Student.example.demo.Dto.LibrarySummaryResponse;
import Student.example.demo.Service.LibrarySummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LibrarySummaryControllerHttpTest {

    private MockMvc mockMvc;
    private InMemoryLibrarySummaryService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryLibrarySummaryService();
        LibrarySummaryController controller = new LibrarySummaryController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSummaryShouldReturnOkWithSummaryJson() throws Exception {
        LibrarySummaryResponse response = new LibrarySummaryResponse();
        response.setTotalStudents(12);
        response.setTotalMaleStudents(7);
        response.setTotalFemaleStudents(5);
        response.setTotalBooksCurrentlyBorrowed(6);
        response.setStudentsBorrowing0Books(8);
        response.setStudentsBorrowing1Book(2);
        response.setStudentsBorrowing2Books(2);
        response.setStudentsBorrowing3Books(0);
        response.setStudentsBorrowing4Books(0);
        response.setStudentsBorrowing5Books(0);
        response.setTotalRegisteredBooks(50);
        response.setTotalAvailableBooks(44);
        response.setTotalBorrowedBooks(6);
        fakeService.responseToReturn = response;

        mockMvc.perform(get("/api/library/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalStudents", is(12)))
                .andExpect(jsonPath("$.totalMaleStudents", is(7)))
                .andExpect(jsonPath("$.totalFemaleStudents", is(5)))
                .andExpect(jsonPath("$.totalBooksCurrentlyBorrowed", is(6)))
                .andExpect(jsonPath("$.studentsBorrowing0Books", is(8)))
                .andExpect(jsonPath("$.studentsBorrowing1Book", is(2)))
                .andExpect(jsonPath("$.studentsBorrowing2Books", is(2)))
                .andExpect(jsonPath("$.totalRegisteredBooks", is(50)))
                .andExpect(jsonPath("$.totalAvailableBooks", is(44)))
                .andExpect(jsonPath("$.totalBorrowedBooks", is(6)));
    }

    private static class InMemoryLibrarySummaryService implements LibrarySummaryService {
        LibrarySummaryResponse responseToReturn = new LibrarySummaryResponse();

        @Override
        public LibrarySummaryResponse getSummary() {
            return responseToReturn;
        }
    }
}
