//package Student.example.demo.Mapper;
//
//import Student.example.demo.Dto.BorrowRecordDto;
//import Student.example.demo.Entity.Book;
//import Student.example.demo.Entity.BorrowRecord;
//import Student.example.demo.Entity.Student;
//import org.springframework.stereotype.Component;
//
//@Component
//public class BorrowRecordMapper {
//
//    public BorrowRecord toEntity(
//            BorrowRecordDto dto,
//            Student student,
//            Book book) {
//
//        BorrowRecord record = new BorrowRecord();
//
//        record.setBorrowRecordId(dto.getBorrowRecordId());
//        record.setBorrowDate(dto.getBorrowDate());
//        record.setReturnDate(dto.getReturnDate());
//        record.setNumberofBooks(dto.getNumberOfBooks());
//
//        record.setStudent(student);
//        record.setBook(book);
//
//        return record;
//    }
//
//    public BorrowRecordDto toDto(BorrowRecord record) {
//
//        BorrowRecordDto dto = new BorrowRecordDto();
//
//        dto.setBorrowRecordId(record.getBorrowRecordId());
//        dto.setBorrowDate(record.getBorrowDate());
//        dto.setReturnDate(record.getReturnDate());
//        dto.setNumberOfBooks(record.getNumberofBooks());
//
//        dto.setStudentid(record.getBorrowRecordId());
//        dto.setBookId(record.getBook().getBookId());
//
//        return dto;
//    }
//}

package Student.example.demo.Mapper;

import Student.example.demo.Dto.BorrowRecordDto;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.Student;
import Student.example.demo.Service.FinePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BorrowRecordMapper {
    private final FinePolicy finePolicy;

    public BorrowRecord toEntity(
            BorrowRecordDto dto,
            Student student,
            Book book) {

        BorrowRecord record = new BorrowRecord();

        record.setBorrowDate(dto.getBorrowDate());
        record.setDueDate(dto.getBorrowDate().plusDays(finePolicy.getLoanPeriodDays()));
        record.setReturnDate(dto.getReturnDate());
        record.setNumberofBooks(dto.getNumberOfBooks());
        record.setBorrowedQuantity(dto.getNumberOfBooks());

        record.setStudent(student);
        record.setBook(book);

        return record;
    }

    public BorrowRecordDto toDto(BorrowRecord record) {

        BorrowRecordDto dto = new BorrowRecordDto();

        dto.setBorrowRecordId(record.getBorrowRecordId());
        dto.setBorrowDate(record.getBorrowDate());
        dto.setDueDate(record.getDueDate() != null
                ? record.getDueDate()
                : record.getBorrowDate().plusDays(finePolicy.getLoanPeriodDays()));
        dto.setReturnDate(record.getReturnDate());
        dto.setNumberOfBooks(record.getNumberofBooks());
        dto.setBookTitle(record.getBook().getBook_title());
        dto.setStudentid(record.getStudent().getStudentid());
        dto.setBookId(record.getBook().getBookId());

        return dto;
    }
}