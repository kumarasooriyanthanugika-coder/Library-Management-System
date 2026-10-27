package Student.example.demo.Controller;

import Student.example.demo.Dto.LibrarySummaryResponse;
import Student.example.demo.Service.LibrarySummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LibrarySummaryControllerTest {

    private LibrarySummaryController controller;
    private InMemoryLibrarySummaryService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryLibrarySummaryService();
        controller = new LibrarySummaryController(fakeService);
    }

    @Test
    void getSummaryShouldReturnLibrarySummaryResponse() {
        LibrarySummaryResponse response = new LibrarySummaryResponse();
        response.setTotalStudents(10);
        response.setTotalMaleStudents(6);
        response.setTotalFemaleStudents(4);
        response.setTotalAvailableBooks(25);
        response.setTotalBorrowedBooks(5);
        response.setTotalRegisteredBooks(30);
        response.setTotalBooksCurrentlyBorrowed(5);
        response.setStudentsBorrowing0Books(7);
        response.setStudentsBorrowing1Book(1);
        response.setStudentsBorrowing2Books(2);
        fakeService.responseToReturn = response;

        LibrarySummaryResponse result = controller.getSummary();

        assertNotNull(result);
        assertEquals(10, result.getTotalStudents());
        assertEquals(6, result.getTotalMaleStudents());
        assertEquals(4, result.getTotalFemaleStudents());
        assertEquals(25, result.getTotalAvailableBooks());
        assertEquals(5, result.getTotalBorrowedBooks());
        assertEquals(30, result.getTotalRegisteredBooks());
        assertEquals(5, result.getTotalBooksCurrentlyBorrowed());
        assertEquals(7, result.getStudentsBorrowing0Books());
        assertEquals(1, result.getStudentsBorrowing1Book());
        assertEquals(2, result.getStudentsBorrowing2Books());
    }

    @Test
    void getSummaryShouldPropagateServiceError() {
        fakeService.shouldThrow = true;

        assertThrows(RuntimeException.class, () -> controller.getSummary());
    }

    private static class InMemoryLibrarySummaryService implements LibrarySummaryService {
        LibrarySummaryResponse responseToReturn = new LibrarySummaryResponse();
        boolean shouldThrow = false;

        @Override
        public LibrarySummaryResponse getSummary() {
            if (shouldThrow) {
                throw new RuntimeException("Summary service failure");
            }
            return responseToReturn;
        }
    }
}
