package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.FinePayment;
import Student.example.demo.Entity.Student;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.FinePaymentRepository;
import Student.example.demo.Repository.ReturnBookRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.FinePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FineServiceImplTest {
    @Test
    void keepsFineAndPaymentTotalsIndependentForEachBorrowRecordAfterReload() {
        Student student = new Student();
        student.setStudentid(7L);

        FinePolicy policy = new FinePolicy(03, new BigDecimal("10.00"), new BigDecimal("50.00"));
        BorrowRecord first = borrowRecord(101L, student, 201L, "First Book", 2, LocalDate.now(FinePolicy.LIBRARY_ZONE).minusDays(2), policy);
        BorrowRecord second = borrowRecord(102L, student, 202L, "Second Book", 1, LocalDate.now(FinePolicy.LIBRARY_ZONE).plusDays(3), policy);

        FinePayment firstPayment = new FinePayment();
        firstPayment.setPaymentId(301L);
        firstPayment.setAmount(new java.math.BigDecimal("10.00"));
        firstPayment.setBorrowRecord(first);
        firstPayment.setStudent(student);

        StudentRepository studentRepository = mock(StudentRepository.class);
        BorrowRecordRepository borrowRecordRepository = mock(BorrowRecordRepository.class);
        ReturnBookRepository returnBookRepository = mock(ReturnBookRepository.class);
        FinePaymentRepository finePaymentRepository = mock(FinePaymentRepository.class);

        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
        when(borrowRecordRepository.findAllByStudent_StudentidOrderByBorrowDateAscBorrowRecordIdAsc(7L))
                .thenReturn(List.of(first, second));
        when(returnBookRepository.findAllByBorrowRecord_BorrowRecordIdInOrderByReturnDateAscReturnBookIdAsc(
                List.of(101L, 102L))).thenReturn(List.of());
        when(finePaymentRepository.findAllByBorrowRecord_BorrowRecordIdInOrderByPaidAtAscPaymentIdAsc(
                List.of(101L, 102L))).thenReturn(List.of(firstPayment));

        FineServiceImpl service = new FineServiceImpl(
                borrowRecordRepository,
                returnBookRepository,
                finePaymentRepository,
                studentRepository,
                policy);

        var firstLoad = service.getFineRecords(7L);
        var refreshedLoad = service.getFineRecords(7L);

        assertEquals(new BigDecimal("140.00"), firstLoad.get(0).getTotalFine());
        assertEquals(new BigDecimal("100.00"), firstLoad.get(0).getBaseBorrowingCharge());
        assertEquals(new BigDecimal("40.00"), firstLoad.get(0).getLateFine());
        assertEquals(new BigDecimal("10.00"), firstLoad.get(0).getPaidAmount());
        assertEquals(new BigDecimal("130.00"), firstLoad.get(0).getUnpaidAmount());
        assertEquals("Partially Paid", firstLoad.get(0).getPaymentStatus());
        assertEquals(new BigDecimal("50.00"), firstLoad.get(1).getTotalFine());
        assertEquals(new BigDecimal("0.00"), firstLoad.get(1).getPaidAmount());
        assertEquals(new BigDecimal("50.00"), firstLoad.get(1).getUnpaidAmount());
        assertEquals(firstLoad, refreshedLoad);
    }

    private BorrowRecord borrowRecord(
            Long recordId,
            Student student,
            Long bookId,
            String title,
            int quantity,
            LocalDate dueDate,
            FinePolicy policy) {
        Book book = new Book();
        book.setBookId(bookId);
        book.setBook_title(title);

        BorrowRecord record = new BorrowRecord();
        record.setBorrowRecordId(recordId);
        record.setStudent(student);
        record.setBook(book);
        record.setBorrowedQuantity(quantity);
        record.setNumberofBooks(quantity);
        record.setDueDate(dueDate);
        record.setBorrowDate(dueDate.minusDays(policy.getLoanPeriodDays()));
        return record;
    }
}
