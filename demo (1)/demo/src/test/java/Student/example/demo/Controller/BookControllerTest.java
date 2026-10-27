package Student.example.demo.Controller;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class BookControllerTest {

    private BookController controller;
    private InMemoryBookService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryBookService();
        controller = new BookController(fakeService);
    }

    @Test
    void createShouldReturnCreatedBook() {
        BookDto input = book("Clean Code", "Robert Martin", "Programming", 10);

        BookDto result = controller.create(input);

        assertNotNull(result.getBookId());
        assertEquals("Clean Code", result.getBookTitle());
        assertEquals("Robert Martin", result.getAuthor());
        assertEquals("Programming", result.getCategory());
        assertEquals(10, result.getAvailableQuantity());
    }

    @Test
    void createShouldForwardInputToService() {
        BookDto input = book("Design Patterns", "GoF", "Architecture", 5);

        BookDto result = controller.create(input);

        assertEquals(result, fakeService.getById(result.getBookId()));
        assertEquals(input.getBookTitle(), fakeService.lastCreatedInput.getBookTitle());
    }

    @Test
    void createShouldPropagateServiceError() {
        fakeService.shouldFailOnCreate = true;
        BookDto input = book("Failing Book", "Author", "Cat", 1);

        assertThrows(RuntimeException.class, () -> controller.create(input));
    }

    @Test
    void getAllShouldReturnAllBooks() {
        controller.create(book("Book 1", "Author 1", "Cat 1", 3));
        controller.create(book("Book 2", "Author 2", "Cat 2", 7));

        List<BookDto> result = controller.getAll();

        assertEquals(2, result.size());
        assertEquals("Book 1", result.get(0).getBookTitle());
        assertEquals("Book 2", result.get(1).getBookTitle());
    }

    @Test
    void getAllShouldReturnEmptyListWhenNoBooksExist() {
        List<BookDto> result = controller.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllShouldPropagateServiceError() {
        fakeService.shouldFailOnGetAll = true;

        assertThrows(RuntimeException.class, () -> controller.getAll());
    }

    private BookDto book(String title, String author, String category, int quantity) {
        BookDto dto = new BookDto();
        dto.setBookTitle(title);
        dto.setAuthor(author);
        dto.setCategory(category);
        dto.setAvailableQuantity(quantity);
        return dto;
    }

    private static class InMemoryBookService implements BookService {
        private final Map<Long, BookDto> books = new LinkedHashMap<>();
        private long nextId = 1L;
        BookDto lastCreatedInput;
        boolean shouldFailOnCreate = false;
        boolean shouldFailOnGetAll = false;

        @Override
        public BookDto create(BookDto dto) {
            if (shouldFailOnCreate) {
                throw new RuntimeException("Service failed to create book");
            }
            this.lastCreatedInput = dto;
            dto.setBookId(nextId++);
            books.put(dto.getBookId(), dto);
            return dto;
        }

        @Override
        public List<BookDto> getAll() {
            if (shouldFailOnGetAll) {
                throw new RuntimeException("Service failed to retrieve books");
            }
            return new ArrayList<>(books.values());
        }

        @Override
        public BookDto getById(Long id) {
            BookDto dto = books.get(id);
            if (dto == null) {
                throw new NoSuchElementException("Book not found: " + id);
            }
            return dto;
        }
    }
}
