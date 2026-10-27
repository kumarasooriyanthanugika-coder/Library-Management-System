package Student.example.demo.Service;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.FinePayment;
import Student.example.demo.Entity.ReturnBook;
import Student.example.demo.Entity.Student;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.FinePaymentRepository;
import Student.example.demo.Repository.ReturnBookRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.Serviceimplementation.FineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FineServiceBusinessRuleTest {

    private FineServiceImpl fineService;
    private FinePolicy policy;

    private Map<Long, Student> studentStore;
    private Map<Long, BorrowRecord> borrowRecordStore;
    private List<ReturnBook> returnBookStore;
    private List<FinePayment> finePaymentStore;

    private Student student;
    private Book book;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        policy = new FinePolicy(3, new BigDecimal("10.00"), new BigDecimal("50.00"));

        studentStore = new LinkedHashMap<>();
        borrowRecordStore = new LinkedHashMap<>();
        returnBookStore = new ArrayList<>();
        finePaymentStore = new ArrayList<>();

        StudentRepository studentRepo = createStudentRepositoryFake();
        BorrowRecordRepository borrowRepo = createBorrowRecordRepositoryFake();
        ReturnBookRepository returnRepo = createReturnBookRepositoryFake();
        FinePaymentRepository paymentRepo = createFinePaymentRepositoryFake();

        fineService = new FineServiceImpl(borrowRepo, returnRepo, paymentRepo, studentRepo, policy);

        student = new Student();
        student.setStudentid(1L);
        student.setStudentName("Thanugika");
        studentStore.put(student.getStudentid(), student);

        book = new Book();
        book.setBookId(101L);
        book.setBook_title("Database Systems");
        book.setAvailableQuantity(10);

        today = LocalDate.now(FinePolicy.LIBRARY_ZONE);
    }

    @Test
    void baseBorrowingChargeAppliesPerBook() {
        // Borrow 1 book returned on time
        BorrowRecord singleBookRecord = createBorrowRecord(101L, 1, today.minusDays(3), today.minusDays(3));
        addReturn(1L, singleBookRecord, 1, today.minusDays(0)); // returned on due date

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        BorrowFineRecordDto dto = records.get(0);
        assertEquals(new BigDecimal("50.00"), dto.getBaseBorrowingCharge());
        assertEquals(new BigDecimal("0.00"), dto.getLateFine());
        assertEquals(new BigDecimal("50.00"), dto.getTotalFine());
        assertEquals("Unpaid", dto.getPaymentStatus());
    }

    @Test
    void baseBorrowingChargeMultipliesForMultipleBooks() {
        BorrowRecord threeBooksRecord = createBorrowRecord(102L, 3, today.minusDays(3), today.minusDays(3));
        addReturn(1L, threeBooksRecord, 3, today.minusDays(0)); // returned on due date

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        assertEquals(new BigDecimal("150.00"), records.get(0).getBaseBorrowingCharge());
        assertEquals(new BigDecimal("0.00"), records.get(0).getLateFine());
        assertEquals(new BigDecimal("150.00"), records.get(0).getTotalFine());
    }

    @Test
    void dueDateBoundaryReturnOnExactDueDateIncursZeroLateFine() {
        LocalDate borrowDate = today.minusDays(6);
        LocalDate dueDate = borrowDate.plusDays(policy.getLoanPeriodDays()); // minusDays(3)

        BorrowRecord record = createBorrowRecord(103L, 1, borrowDate, dueDate);
        addReturn(1L, record, 1, dueDate); // returned exactly on due date

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        BorrowFineRecordDto dto = records.get(0);
        assertEquals(0L, dto.getLateDays());
        assertEquals(new BigDecimal("0.00"), dto.getLateFine());
        assertEquals(new BigDecimal("50.00"), dto.getTotalFine());
    }

    @Test
    void dueDateBoundaryReturnOneDayAfterDueDateIncursOneDayFine() {
        LocalDate borrowDate = today.minusDays(6);
        LocalDate dueDate = borrowDate.plusDays(policy.getLoanPeriodDays()); // minusDays(3)
        LocalDate returnDate = dueDate.plusDays(1); // 1 day overdue

        BorrowRecord record = createBorrowRecord(104L, 1, borrowDate, dueDate);
        addReturn(1L, record, 1, returnDate);

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        BorrowFineRecordDto dto = records.get(0);
        assertEquals(1L, dto.getLateDays());
        // 1 day * LKR 10 * 1 book = 10.00
        assertEquals(new BigDecimal("10.00"), dto.getLateFine());
        assertEquals(new BigDecimal("60.00"), dto.getTotalFine());
    }

    @Test
    void dueDateBoundaryReturnMultipleDaysLateWithMultipleBooks() {
        LocalDate borrowDate = today.minusDays(8);
        LocalDate dueDate = borrowDate.plusDays(policy.getLoanPeriodDays()); // minusDays(5)
        LocalDate returnDate = dueDate.plusDays(4); // 4 days overdue

        BorrowRecord record = createBorrowRecord(105L, 2, borrowDate, dueDate);
        addReturn(1L, record, 2, returnDate);

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        BorrowFineRecordDto dto = records.get(0);
        assertEquals(4L, dto.getLateDays());
        // 4 days * 10.00 * 2 books = 80.00
        assertEquals(new BigDecimal("80.00"), dto.getLateFine());
        // 2 books * 50.00 = 100.00 base
        assertEquals(new BigDecimal("100.00"), dto.getBaseBorrowingCharge());
        // total = 180.00
        assertEquals(new BigDecimal("180.00"), dto.getTotalFine());
    }

    @Test
    void partialReturnsCalculateFinesPerReturnBatchAccurately() {
        LocalDate borrowDate = today.minusDays(9);
        LocalDate dueDate = borrowDate.plusDays(policy.getLoanPeriodDays()); // minusDays(6)

        BorrowRecord record = createBorrowRecord(106L, 2, borrowDate, dueDate);
        // Return 1st book 2 days late: minusDays(4) -> 2 * 10 * 1 = 20.00
        addReturn(1L, record, 1, dueDate.plusDays(2));
        // Return 2nd book 5 days late: minusDays(1) -> 5 * 10 * 1 = 50.00
        addReturn(2L, record, 1, dueDate.plusDays(5));

        List<BorrowFineRecordDto> records = fineService.getFineRecords(student.getStudentid());

        assertEquals(1, records.size());
        BorrowFineRecordDto dto = records.get(0);
        // Late fine = 20.00 + 50.00 = 70.00
        assertEquals(new BigDecimal("70.00"), dto.getLateFine());
        // Base charge = 2 books * 50.00 = 100.00
        assertEquals(new BigDecimal("100.00"), dto.getBaseBorrowingCharge());
        assertEquals(new BigDecimal("170.00"), dto.getTotalFine());
        assertEquals(new BigDecimal("170.00"), dto.getUnpaidAmount());
    }

    @Test
    void returnFinePreviewCalculatesCorrectAmountsBeforeSubmission() {
        LocalDate borrowDate = today.minusDays(5);
        LocalDate dueDate = borrowDate.plusDays(policy.getLoanPeriodDays()); // minusDays(2)
        BorrowRecord record = createBorrowRecord(107L, 2, borrowDate, dueDate);
        record.setNumberofBooks(2);

        ReturnFinePreviewRequestDto previewRequest = new ReturnFinePreviewRequestDto();
        previewRequest.setStudentid(student.getStudentid());
        previewRequest.setBookId(book.getBookId());
        previewRequest.setBorrowRecordId(record.getBorrowRecordId());
        previewRequest.setNumberOfBooks(1);
        previewRequest.setReturnDate(dueDate.plusDays(2)); // 2 days late

        ReturnFinePreviewDto preview = fineService.previewReturn(previewRequest);

        assertNotNull(preview);
        assertEquals(2L, preview.getLateDays());
        // 1 book returning, 2 days late = 20.00
        assertEquals(new BigDecimal("20.00"), preview.getFineForReturningQuantity());
        assertEquals(new BigDecimal("20.00"), preview.getFineForOneBook());
        // base charge for 1 returning book = 50.00
        assertEquals(new BigDecimal("50.00"), preview.getBaseChargeForReturningQuantity());
        // base charge for all 2 borrowed books = 100.00
        assertEquals(new BigDecimal("100.00"), preview.getBaseBorrowingCharge());
    }

    @Test
    void paymentBalancesAndStatusProgression() {
        LocalDate borrowDate = today.minusDays(3);
        BorrowRecord record = createBorrowRecord(108L, 2, borrowDate, today);
        addReturn(1L, record, 2, today); // returned on due date: total = 100.00, late = 0.00

        // 1. Initial status: Unpaid
        List<BorrowFineRecordDto> initial = fineService.getFineRecords(student.getStudentid());
        assertEquals(new BigDecimal("100.00"), initial.get(0).getTotalFine());
        assertEquals(new BigDecimal("0.00"), initial.get(0).getPaidAmount());
        assertEquals(new BigDecimal("100.00"), initial.get(0).getUnpaidAmount());
        assertEquals("Unpaid", initial.get(0).getPaymentStatus());

        // 2. Partial payment of 40.00
        FinePaymentRequestDto payment1 = new FinePaymentRequestDto();
        payment1.setAmount(new BigDecimal("40.00"));
        payment1.setIdempotencyKey(UUID.randomUUID().toString());

        BorrowFineRecordDto afterPayment1 = fineService.createPayment(
                student.getStudentid(), record.getBorrowRecordId(), payment1);

        assertEquals(new BigDecimal("40.00"), afterPayment1.getPaidAmount());
        assertEquals(new BigDecimal("60.00"), afterPayment1.getUnpaidAmount());
        assertEquals("Partially Paid", afterPayment1.getPaymentStatus());

        // 3. Final payment of remaining 60.00
        FinePaymentRequestDto payment2 = new FinePaymentRequestDto();
        payment2.setAmount(new BigDecimal("60.00"));
        payment2.setIdempotencyKey(UUID.randomUUID().toString());

        BorrowFineRecordDto afterPayment2 = fineService.createPayment(
                student.getStudentid(), record.getBorrowRecordId(), payment2);

        assertEquals(new BigDecimal("100.00"), afterPayment2.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), afterPayment2.getUnpaidAmount());
        assertEquals("Paid", afterPayment2.getPaymentStatus());
    }

    @Test
    void rejectionOfOverpaymentExceedingUnpaidBalance() {
        LocalDate borrowDate = today.minusDays(3);
        BorrowRecord record = createBorrowRecord(109L, 1, borrowDate, today);
        addReturn(1L, record, 1, today); // total fine = 50.00

        FinePaymentRequestDto overpayment = new FinePaymentRequestDto();
        overpayment.setAmount(new BigDecimal("50.01")); // exceeds 50.00
        overpayment.setIdempotencyKey(UUID.randomUUID().toString());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                fineService.createPayment(student.getStudentid(), record.getBorrowRecordId(), overpayment));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Payment cannot exceed this borrow record's unpaid balance"));
    }

    @Test
    void paymentIdempotencyPreventsDuplicateCharge() {
        LocalDate borrowDate = today.minusDays(3);
        BorrowRecord record = createBorrowRecord(110L, 1, borrowDate, today);
        addReturn(1L, record, 1, today); // total = 50.00

        String sharedKey = UUID.randomUUID().toString();

        FinePaymentRequestDto firstRequest = new FinePaymentRequestDto();
        firstRequest.setAmount(new BigDecimal("20.00"));
        firstRequest.setIdempotencyKey(sharedKey);

        BorrowFineRecordDto firstResult = fineService.createPayment(
                student.getStudentid(), record.getBorrowRecordId(), firstRequest);
        assertEquals(new BigDecimal("20.00"), firstResult.getPaidAmount());
        assertEquals(new BigDecimal("30.00"), firstResult.getUnpaidAmount());

        // Duplicate request with the identical key
        FinePaymentRequestDto repeatRequest = new FinePaymentRequestDto();
        repeatRequest.setAmount(new BigDecimal("20.00"));
        repeatRequest.setIdempotencyKey(sharedKey);

        BorrowFineRecordDto secondResult = fineService.createPayment(
                student.getStudentid(), record.getBorrowRecordId(), repeatRequest);
        assertEquals(new BigDecimal("20.00"), secondResult.getPaidAmount());
        assertEquals(1, finePaymentStore.size()); // no second payment entity created

        // Conflicting request with the same key but different amount
        FinePaymentRequestDto conflictingRequest = new FinePaymentRequestDto();
        conflictingRequest.setAmount(new BigDecimal("30.00"));
        conflictingRequest.setIdempotencyKey(sharedKey);

        ResponseStatusException conflictEx = assertThrows(ResponseStatusException.class, () ->
                fineService.createPayment(student.getStudentid(), record.getBorrowRecordId(), conflictingRequest));
        assertEquals(HttpStatus.CONFLICT, conflictEx.getStatusCode());
    }

    // --- Helper setup methods ---

    private BorrowRecord createBorrowRecord(Long recordId, int quantity, LocalDate borrowDate, LocalDate dueDate) {
        BorrowRecord record = new BorrowRecord();
        record.setBorrowRecordId(recordId);
        record.setStudent(student);
        record.setBook(book);
        record.setBorrowedQuantity(quantity);
        record.setNumberofBooks(0); // 0 outstanding once all returned
        record.setBorrowDate(borrowDate);
        record.setDueDate(dueDate);
        borrowRecordStore.put(recordId, record);
        return record;
    }

    private void addReturn(Long returnId, BorrowRecord record, int returnedQty, LocalDate returnDate) {
        ReturnBook returnBook = new ReturnBook();
        returnBook.setReturnBookId(returnId);
        returnBook.setBorrowRecord(record);
        returnBook.setStudent(student);
        returnBook.setBook(book);
        returnBook.setNumberOfBooks(returnedQty);
        returnBook.setReturnDate(returnDate);
        returnBookStore.add(returnBook);
    }

    // --- Dynamic Proxy Fake Repositories (Standard Java, No Mockito) ---

    @SuppressWarnings("unchecked")
    private StudentRepository createStudentRepositoryFake() {
        return (StudentRepository) Proxy.newProxyInstance(
                StudentRepository.class.getClassLoader(),
                new Class<?>[]{StudentRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(studentStore.get(id));
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private BorrowRecordRepository createBorrowRecordRepositoryFake() {
        return (BorrowRecordRepository) Proxy.newProxyInstance(
                BorrowRecordRepository.class.getClassLoader(),
                new Class<?>[]{BorrowRecordRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name) || "findByIdForUpdate".equals(name)) {
                        Long id = (Long) args[0];
                        return Optional.ofNullable(borrowRecordStore.get(id));
                    }
                    if ("findAllByStudent_StudentidOrderByBorrowDateAscBorrowRecordIdAsc".equals(name)) {
                        Long studentId = (Long) args[0];
                        return borrowRecordStore.values().stream()
                                .filter(r -> studentId.equals(r.getStudent().getStudentid()))
                                .sorted(Comparator.comparing(BorrowRecord::getBorrowDate)
                                        .thenComparing(BorrowRecord::getBorrowRecordId))
                                .toList();
                    }
                    if ("save".equals(name)) {
                        BorrowRecord r = (BorrowRecord) args[0];
                        borrowRecordStore.put(r.getBorrowRecordId(), r);
                        return r;
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private ReturnBookRepository createReturnBookRepositoryFake() {
        return (ReturnBookRepository) Proxy.newProxyInstance(
                ReturnBookRepository.class.getClassLoader(),
                new Class<?>[]{ReturnBookRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findAllByBorrowRecord_BorrowRecordIdInOrderByReturnDateAscReturnBookIdAsc".equals(name)) {
                        List<Long> ids = (List<Long>) args[0];
                        return returnBookStore.stream()
                                .filter(rb -> ids.contains(rb.getBorrowRecord().getBorrowRecordId()))
                                .sorted(Comparator.comparing(ReturnBook::getReturnDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(ReturnBook::getReturnBookId, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
                    }
                    if ("findAllByBorrowRecord_BorrowRecordIdOrderByReturnDateAscReturnBookIdAsc".equals(name)) {
                        Long id = (Long) args[0];
                        return returnBookStore.stream()
                                .filter(rb -> id.equals(rb.getBorrowRecord().getBorrowRecordId()))
                                .sorted(Comparator.comparing(ReturnBook::getReturnDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(ReturnBook::getReturnBookId, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
                    }
                    return null;
                });
    }

    @SuppressWarnings("unchecked")
    private FinePaymentRepository createFinePaymentRepositoryFake() {
        return (FinePaymentRepository) Proxy.newProxyInstance(
                FinePaymentRepository.class.getClassLoader(),
                new Class<?>[]{FinePaymentRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findAllByBorrowRecord_BorrowRecordIdInOrderByPaidAtAscPaymentIdAsc".equals(name)) {
                        List<Long> ids = (List<Long>) args[0];
                        return finePaymentStore.stream()
                                .filter(p -> ids.contains(p.getBorrowRecord().getBorrowRecordId()))
                                .sorted(Comparator.comparing(FinePayment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(FinePayment::getPaymentId, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
                    }
                    if ("findAllByBorrowRecord_BorrowRecordIdOrderByPaidAtAscPaymentIdAsc".equals(name)) {
                        Long id = (Long) args[0];
                        return finePaymentStore.stream()
                                .filter(p -> id.equals(p.getBorrowRecord().getBorrowRecordId()))
                                .sorted(Comparator.comparing(FinePayment::getPaidAt, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(FinePayment::getPaymentId, Comparator.nullsLast(Comparator.naturalOrder())))
                                .toList();
                    }
                    if ("findByIdempotencyKey".equals(name)) {
                        String key = (String) args[0];
                        return finePaymentStore.stream()
                                .filter(p -> key.equals(p.getIdempotencyKey()))
                                .findFirst();
                    }
                    if ("saveAndFlush".equals(name) || "save".equals(name)) {
                        FinePayment p = (FinePayment) args[0];
                        if (p.getPaymentId() == null) {
                            p.setPaymentId((long) (finePaymentStore.size() + 1));
                        }
                        finePaymentStore.add(p);
                        return p;
                    }
                    return null;
                });
    }
}
