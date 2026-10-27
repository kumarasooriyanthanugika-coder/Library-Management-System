package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.LibrarySummaryResponse;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.Student;
import Student.example.demo.Repository.BookRepository;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.LibrarySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LibrarySummaryServiceImpl
        implements LibrarySummaryService {

    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public LibrarySummaryResponse getSummary() {

        List<Student> students = studentRepository.findAll();
        List<Book> books = bookRepository.findAll();
        List<BorrowRecord> records =
                borrowRecordRepository.findAll();

        LibrarySummaryResponse summary =
                new LibrarySummaryResponse();

        summary.setTotalStudents(students.size());

        summary.setTotalMaleStudents(
                students.stream()
                        .filter(s -> "M".equalsIgnoreCase(s.getGender()))
                        .count()
        );

        summary.setTotalFemaleStudents(
                students.stream()
                        .filter(s -> "F".equalsIgnoreCase(s.getGender()))
                        .count()
        );

        long availableBooks = books.stream()
                .mapToLong(book ->
                        book.getAvailableQuantity() == null
                                ? 0
                                : book.getAvailableQuantity())
                .sum();

        Map<Long, Long> borrowedByStudent = new HashMap<>();
        long borrowedBooks = 0;

        for (BorrowRecord record : records) {

            Integer quantity = record.getNumberofBooks();

            if (record.getReturnDate() != null
                    || quantity == null
                    || quantity <= 0) {
                continue;
            }

            borrowedBooks += quantity;

            if (record.getStudent() != null) {
                Long studentId =
                        record.getStudent().getStudentid();

                borrowedByStudent.merge(
                        studentId,
                        quantity.longValue(),
                        Long::sum
                );
            }
        }

        summary.setTotalAvailableBooks(availableBooks);
        summary.setTotalBorrowedBooks(borrowedBooks);
        summary.setTotalBooksCurrentlyBorrowed(borrowedBooks);

        summary.setTotalRegisteredBooks(
                availableBooks + borrowedBooks
        );

        long[] studentCounts = new long[6];

        for (Student student : students) {
            long borrowed = borrowedByStudent.getOrDefault(
                    student.getStudentid(), 0L
            );

            if (borrowed >= 0 && borrowed <= 5) {
                studentCounts[(int) borrowed]++;
            }
        }

        summary.setStudentsBorrowing0Books(studentCounts[0]);
        summary.setStudentsBorrowing1Book(studentCounts[1]);
        summary.setStudentsBorrowing2Books(studentCounts[2]);
        summary.setStudentsBorrowing3Books(studentCounts[3]);
        summary.setStudentsBorrowing4Books(studentCounts[4]);
        summary.setStudentsBorrowing5Books(studentCounts[5]);

        return summary;
    }
}