package Student.example.demo.Controller;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookControllerHttpTest {

    private MockMvc mockMvc;
    private InMemoryBookService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryBookService();
        BookController controller = new BookController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postBookShouldReturnOkWithCreatedBookJson() throws Exception {
        String payload = """
                {
                    "bookTitle": "Effective Java",
                    "author": "Joshua Bloch",
                    "category": "Software",
                    "availableQuantity": 8
                }
                """;

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.BookId", notNullValue()))
                .andExpect(jsonPath("$.bookTitle", is("Effective Java")))
                .andExpect(jsonPath("$.author", is("Joshua Bloch")))
                .andExpect(jsonPath("$.category", is("Software")))
                .andExpect(jsonPath("$.availableQuantity", is(8)));
    }

    @Test
    void getBooksShouldReturnOkWithBookListJson() throws Exception {
        fakeService.create(book("Book A", "Author A", "Cat A", 3));
        fakeService.create(book("Book B", "Author B", "Cat B", 5));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].bookTitle", is("Book A")))
                .andExpect(jsonPath("$[1].bookTitle", is("Book B")));
    }

    @Test
    void getBooksWhenEmptyShouldReturnOkWithEmptyJsonArray() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
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

        @Override
        public BookDto create(BookDto dto) {
            dto.setBookId(nextId++);
            books.put(dto.getBookId(), dto);
            return dto;
        }

        @Override
        public List<BookDto> getAll() {
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
