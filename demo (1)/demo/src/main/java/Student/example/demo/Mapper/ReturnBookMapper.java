package Student.example.demo.Mapper;

import Student.example.demo.Dto.ReturnBookDto;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.ReturnBook;
import Student.example.demo.Entity.Student;
import org.springframework.stereotype.Component;

@Component
public class ReturnBookMapper {
    public ReturnBook toEntity(ReturnBookDto dto, Student student, Book book, BorrowRecord borrowRecord){
        ReturnBook returnBook=new ReturnBook();
        returnBook.setStudent(student);
        returnBook.setBook(book);
        returnBook.setBorrowRecord(borrowRecord);

      return returnBook;
    }

}
