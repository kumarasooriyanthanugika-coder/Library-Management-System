package Student.example.demo.Service;

import Student.example.demo.Dto.LibrarySummaryResponse;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.Student;
import Student.example.demo.Repository.BookRepository;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.Serviceimplementation.LibrarySummaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibrarySummaryServiceTest {

    private LibrarySummaryServiceImpl summaryService;
    private List<Student> students;
    private List<Book> books;
    private List<BorrowRecord> borrowRecords;

    @BeforeEach
    void setUp() {
        students = new ArrayList<>();
        books = new ArrayList<>();
        borrowRecords = new ArrayList<>();

        StudentRepository studentRepo = createStudentRepositoryFake();
        BookRepository bookRepo = createBookRepositoryFake();
        BorrowRecordRepository borrowRepo = createBorrowRecordRepositoryFake();

        summaryService = new LibrarySummaryServiceImpl(studentRepo, bookRepo, borrowRepo);
    }

    @Test
    void getSummaryCalculatesStudentGenderCountsAndBookTotalsCorrectly() {
        // Students: 2 Male, 1 Female
        Student s1 = createStudent(1L, "M");
        Student s2 = createStudent(2L, "M");
        Student s3 = createStudent(3L, "F");
        students.addAll(List.of(s1, s2, s3));

        // Books: 2 books with available 10 and 5 -> total available = 15
        Book b1 = createBook(101L, 10);
        Book b2 = createBook(102L, 5);
        books.addAll(List.of(b1, b2));

        // Borrow Records:
        // s1 borrows 2 books (active, returnDate is null)
        BorrowRecord r1 = createBorrowRecord(1001L, s1, b1, 2, null);
        // s2 borrows 1 book (active)
        BorrowRecord r2 = createBorrowRecord(1002L, s2, b2, 1, null);
        // s3 borrowed 1 book but already returned it (returnDate != null)
        BorrowRecord r3 = createBorrowRecord(1003L, s3, b1, 1, LocalDate.of(2026, 10, 10));
        borrowRecords.addAll(List.of(r1, r2, r3));

        LibrarySummaryResponse summary = summaryService.getSummary();

        assertNotNull(summary);
        assertEquals(3, summary.getTotalStudents());
        assertEquals(2, summary.getTotalMaleStudents());
        assertEquals(1, summary.getTotalFemaleStudents());
        assertEquals(15, summary.getTotalAvailableBooks());
        assertEquals(3, summary.getTotalBorrowedBooks()); // 2 + 1 active
        assertEquals(3, summary.getTotalBooksCurrentlyBorrowed());
        assertEquals(18, summary.getTotalRegisteredBooks()); // 15 available + 3 borrowed

        // Distribution:
        // s1 has 2 books borrowed -> bucket 2
        // s2 has 1 book borrowed -> bucket 1
        // s3 has 0 active books borrowed -> bucket 0
        assertEquals(1, summary.getStudentsBorrowing0Books());
        assertEquals(1, summary.getStudentsBorrowing1Book());
        assertEquals(1, summary.getStudentsBorrowing2Books());
        assertEquals(0, summary.getStudentsBorrowing3Books());
        assertEquals(0, summary.getStudentsBorrowing4Books());
        assertEquals(0, summary.getStudentsBorrowing5Books());
    }

    private Student createStudent(Long id, String gender) {
        Student s = new Student();
        s.setStudentid(id);
        s.setGender(gender);
        return s;
    }

    private Book createBook(Long id, int availableQuantity) {
        Book b = new Book();
        b.setBookId(id);
        b.setAvailableQuantity(availableQuantity);
        return b;
    }

    private BorrowRecord createBorrowRecord(Long id, Student s, Book b, int qty, LocalDate returnDate) {
        BorrowRecord r = new BorrowRecord();
        r.setBorrowRecordId(id);
        r.setStudent(s);
        r.setBook(b);
        r.setNumberofBooks(qty);
        r.setReturnDate(returnDate);
        return r;
    }

    @SuppressWarnings("unchecked")
    private StudentRepository createStudentRepositoryFake() {
        return (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> "findAll".equals(method.getName()) ? students : null);
    }

    @SuppressWarnings("unchecked")
    private BookRepository createBookRepositoryFake() {
        return (BookRepository) Proxy.newProxyInstance(
                BookRepository.class.getClassLoader(),
                new Class<?>[]{BookRepository.class},
                (proxy, method, args) -> "findAll".equals(method.getName()) ? books : null);
    }

    @SuppressWarnings("unchecked")
    private BorrowRecordRepository createBorrowRecordRepositoryFake() {
        return (BorrowRecordRepository) Proxy.newProxyInstance(
                BorrowRecordRepository.class.getClassLoader(),
                new Class<?>[]{BorrowRecordRepository.class},
                (proxy, method, args) -> "findAll".equals(method.getName()) ? borrowRecords : null);
    }
}
