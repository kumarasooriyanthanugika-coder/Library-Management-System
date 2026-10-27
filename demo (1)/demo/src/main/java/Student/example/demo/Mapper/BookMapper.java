package Student.example.demo.Mapper;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Entity.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book toEntity(BookDto dto) {

        Book book = new Book();

        book.setBook_title(dto.getBookTitle());
        book.setAuthor(dto.getAuthor());
        book.setCategory(dto.getCategory());
        book.setAvailableQuantity(dto.getAvailableQuantity());

        return book;
    }

    public BookDto toDto(Book book) {

        BookDto dto = new BookDto();

        dto.setBookId(book.getBookId());
        dto.setBookTitle(book.getBook_title());
        dto.setAuthor(book.getAuthor());
        dto.setCategory(book.getCategory());
        dto.setAvailableQuantity(book.getAvailableQuantity());

        return dto;
    }

//    public void updateEntity(BookDto dto, Book book) {
//
//        book.setBook_title(dto.getBookTitle());
//        book.setAuthor(dto.getAuthor());
//        book.setCategory(dto.getCategory());
//        book.setAvailableQuantity(dto.getAvailableQuantity());
//    }
}