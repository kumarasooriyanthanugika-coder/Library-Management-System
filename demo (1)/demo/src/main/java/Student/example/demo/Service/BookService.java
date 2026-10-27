package Student.example.demo.Service;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Entity.Book;

import java.util.List;

public interface BookService {
    BookDto create(BookDto dto);
    List<BookDto>getAll();
    BookDto getById(Long id);
}
