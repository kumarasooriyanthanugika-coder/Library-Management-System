package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.FinePayment;
import Student.example.demo.Entity.ReturnBook;
import Student.example.demo.Entity.Student;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.FinePaymentRepository;
import Student.example.demo.Repository.ReturnBookRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.FinePolicy;
import Student.example.demo.Service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final BorrowRecordRepository borrowRecordRepository;
    private final ReturnBookRepository returnBookRepository;
    private final FinePaymentRepository finePaymentRepository;
    private final StudentRepository studentRepository;
    private final FinePolicy finePolicy;

    @Override
    @Transactional(readOnly = true)
    public List<BorrowFineRecordDto> getFineRecords(Long studentId) {
        Student student = findStudent(studentId);
        List<BorrowRecord> records =
                borrowRecordRepository.findAllByStudent_StudentidOrderByBorrowDateAscBorrowRecordIdAsc(student.getStudentid());
        return mapRecords(student, records);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getStudentTotalUnpaidAmount(Long studentId) {
        return getFineRecords(studentId).stream()
                .map(BorrowFineRecordDto::getUnpaidAmount)
                .reduce(ZERO_MONEY, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnFinePreviewDto previewReturn(ReturnFinePreviewRequestDto request) {
        Student student = findStudent(request.getStudentid());
        BorrowRecord record = borrowRecordRepository.findById(request.getBorrowRecordId())
                .orElseThrow(() -> notFound("Borrow record not found"));
        validateReturnRequest(student, record, request);

        List<ReturnBook> returnEvents = returnBookRepository
                .findAllByBorrowRecord_BorrowRecordIdOrderByReturnDateAscReturnBookIdAsc(
                        record.getBorrowRecordId());
        List<FinePayment> payments = finePaymentRepository
                .findAllByBorrowRecord_BorrowRecordIdOrderByPaidAtAscPaymentIdAsc(
                        record.getBorrowRecordId());
        return toReturnPreview(student, record, returnEvents, payments, request,
                LocalDate.now(FinePolicy.LIBRARY_ZONE));
    }

    @Override
    @Transactional
    public BorrowFineRecordDto createPayment(
            Long studentId,
            Long borrowRecordId,
            FinePaymentRequestDto request) {
        Student student = findStudent(studentId);
        BorrowRecord borrowRecord = borrowRecordRepository.findByIdForUpdate(borrowRecordId)
                .orElseThrow(() -> notFound("Borrow record not found"));

        if (!Objects.equals(borrowRecord.getStudent().getStudentid(), student.getStudentid())) {
            throw notFound("Borrow record not found for this student");
        }
        validatePayment(request);

        FinePayment priorPayment = finePaymentRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);
        if (priorPayment != null) {
            if (!Objects.equals(priorPayment.getBorrowRecord().getBorrowRecordId(), borrowRecordId)
                    || !Objects.equals(priorPayment.getStudent().getStudentid(), studentId)
                    || priorPayment.getAmount().compareTo(request.getAmount()) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This payment request key was already used for a different payment");
            }
            return mapRecords(student, List.of(borrowRecord)).getFirst();
        }

        BorrowFineRecordDto current = mapRecords(student, List.of(borrowRecord)).getFirst();
        if (request.getAmount().compareTo(current.getUnpaidAmount()) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment cannot exceed this borrow record's unpaid balance of LKR "
                            + current.getUnpaidAmount().toPlainString());
        }

        FinePayment payment = new FinePayment();
        payment.setAmount(request.getAmount().setScale(2, RoundingMode.UNNECESSARY));
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setPaidAt(java.time.Instant.now());
        payment.setStudent(student);
        payment.setBorrowRecord(borrowRecord);
        finePaymentRepository.saveAndFlush(payment);

        return mapRecords(student, List.of(borrowRecord)).getFirst();
    }

    private List<BorrowFineRecordDto> mapRecords(Student student, List<BorrowRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<Long> recordIds = records.stream()
                .map(BorrowRecord::getBorrowRecordId)
                .toList();
        Map<Long, List<ReturnBook>> returnsByRecord = returnBookRepository
                .findAllByBorrowRecord_BorrowRecordIdInOrderByReturnDateAscReturnBookIdAsc(recordIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getBorrowRecord().getBorrowRecordId()));
        Map<Long, List<FinePayment>> paymentsByRecord = finePaymentRepository
                .findAllByBorrowRecord_BorrowRecordIdInOrderByPaidAtAscPaymentIdAsc(recordIds)
                .stream()
                .collect(Collectors.groupingBy(
                        payment -> payment.getBorrowRecord().getBorrowRecordId()));

        LocalDate today = LocalDate.now(FinePolicy.LIBRARY_ZONE);
        return records.stream()
                .map(record -> toDto(
                        student,
                        record,
                        returnsByRecord.getOrDefault(record.getBorrowRecordId(), List.of()),
                        paymentsByRecord.getOrDefault(record.getBorrowRecordId(), List.of()),
                        today))
                .toList();
    }

    private BorrowFineRecordDto toDto(
            Student student,
            BorrowRecord record,
            List<ReturnBook> returnEvents,
            List<FinePayment> payments,
            LocalDate today) {
        if (record.getBorrowDate() == null || record.getBook() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Borrow record is missing its borrow date or book");
        }

        LocalDate dueDate = record.getBorrowDate()
                .plusDays(finePolicy.getLoanPeriodDays());
        int returnedQuantity = returnEvents.stream()
                .map(ReturnBook::getNumberOfBooks)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        int outstandingQuantity = Math.max(0, Objects.requireNonNullElse(record.getNumberofBooks(), 0));
        int borrowedQuantity = record.getBorrowedQuantity() != null
                ? record.getBorrowedQuantity()
                : outstandingQuantity + returnedQuantity;

        BigDecimal lateFine = ZERO_MONEY;
        long maxLateDays = 0;
        for (ReturnBook returned : returnEvents) {
            if (returned.getReturnDate() == null || returned.getNumberOfBooks() == null) {
                continue;
            }
            long lateDays = lateDays(dueDate, returned.getReturnDate());
            maxLateDays = Math.max(maxLateDays, lateDays);
            lateFine = lateFine.add(calculateFine(returned.getNumberOfBooks(), lateDays));
        }

        long activeLateDays = lateDays(dueDate, today);
        if (outstandingQuantity > 0) {
            maxLateDays = Math.max(maxLateDays, activeLateDays);
            lateFine = lateFine.add(calculateFine(outstandingQuantity, activeLateDays));
        }

        BigDecimal baseBorrowingCharge = finePolicy.getBorrowingCharge()
                .multiply(BigDecimal.valueOf(borrowedQuantity))
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal totalFine = baseBorrowingCharge.add(lateFine)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal paidAmount = payments.stream()
                .map(FinePayment::getAmount)
                .reduce(ZERO_MONEY, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal unpaidAmount = totalFine.subtract(paidAmount).max(ZERO_MONEY);
        LocalDate lastReturnDate = returnEvents.stream()
                .map(ReturnBook::getReturnDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(record.getReturnDate());
        LocalDate elapsedThrough = outstandingQuantity == 0 && lastReturnDate != null
                ? lastReturnDate
                : today;
        long daysSinceBorrowing = Math.max(
                0,
                ChronoUnit.DAYS.between(record.getBorrowDate(), elapsedThrough));

        BorrowFineRecordDto dto = new BorrowFineRecordDto();
        dto.setBorrowRecordId(record.getBorrowRecordId());
        dto.setStudentId(student.getStudentid());
        dto.setBookId(record.getBook().getBookId());
        dto.setBookTitle(record.getBook().getBook_title());
        dto.setBorrowedQuantity(borrowedQuantity);
        dto.setOutstandingQuantity(outstandingQuantity);
        dto.setBorrowDate(record.getBorrowDate());
        dto.setDueDate(dueDate);
        dto.setReturnDate(lastReturnDate);
        dto.setDaysSinceBorrowing(daysSinceBorrowing);
        dto.setLateDays(maxLateDays);
        dto.setBaseBorrowingCharge(baseBorrowingCharge);
        dto.setLateFine(lateFine.setScale(2, RoundingMode.UNNECESSARY));
        dto.setTotalFine(totalFine);
        dto.setPaidAmount(paidAmount);
        dto.setUnpaidAmount(unpaidAmount.setScale(2, RoundingMode.UNNECESSARY));
        dto.setPaymentStatus(unpaidAmount.signum() == 0
                ? "Paid"
                : paidAmount.signum() > 0 ? "Partially Paid" : "Unpaid");
        dto.setPaymentHistory(payments.stream().map(this::toPaymentDto).toList());
        return dto;
    }

    private FinePaymentDto toPaymentDto(FinePayment payment) {
        FinePaymentDto dto = new FinePaymentDto();
        dto.setPaymentId(payment.getPaymentId());
        dto.setAmount(payment.getAmount());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }

    private BigDecimal calculateFine(int quantity, long lateDays) {
        return finePolicy.getDailyRate()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(lateDays))
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    private ReturnFinePreviewDto toReturnPreview(
            Student student,
            BorrowRecord record,
            List<ReturnBook> returnEvents,
            List<FinePayment> payments,
            ReturnFinePreviewRequestDto request,
            LocalDate today) {
        LocalDate dueDate = record.getBorrowDate()
                .plusDays(finePolicy.getLoanPeriodDays());
        int outstandingQuantity = Objects.requireNonNullElse(record.getNumberofBooks(), 0);
        int borrowedQuantity = record.getBorrowedQuantity() != null
                ? record.getBorrowedQuantity()
                : outstandingQuantity + returnEvents.stream()
                        .map(ReturnBook::getNumberOfBooks)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();
        long lateDays = lateDays(dueDate, request.getReturnDate());
        int remainingAfterReturn = outstandingQuantity - request.getNumberOfBooks();

        BigDecimal previousLateFine = returnEvents.stream()
                .filter(item -> item.getReturnDate() != null && item.getNumberOfBooks() != null)
                .map(item -> calculateFine(
                        item.getNumberOfBooks(),
                        lateDays(dueDate, item.getReturnDate())))
                .reduce(ZERO_MONEY, BigDecimal::add);
        BigDecimal fineForOneBook = calculateFine(1, lateDays);
        BigDecimal fineForReturningQuantity =
                calculateFine(request.getNumberOfBooks(), lateDays);
        BigDecimal remainingCopiesLateFine =
                calculateFine(remainingAfterReturn, lateDays(dueDate, today));
        BigDecimal lateFine = previousLateFine
                .add(fineForReturningQuantity)
                .add(remainingCopiesLateFine)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal baseBorrowingCharge = finePolicy.getBorrowingCharge()
                .multiply(BigDecimal.valueOf(borrowedQuantity))
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal baseChargeForReturningQuantity = finePolicy.getBorrowingCharge()
                .multiply(BigDecimal.valueOf(request.getNumberOfBooks()))
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal totalAmountDue = baseBorrowingCharge.add(lateFine)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal alreadyPaidAmount = payments.stream()
                .map(FinePayment::getAmount)
                .reduce(ZERO_MONEY, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal remainingUnpaidAmount = totalAmountDue.subtract(alreadyPaidAmount)
                .max(ZERO_MONEY)
                .setScale(2, RoundingMode.UNNECESSARY);

        ReturnFinePreviewDto dto = new ReturnFinePreviewDto();
        dto.setBorrowRecordId(record.getBorrowRecordId());
        dto.setStudentId(student.getStudentid());
        dto.setStudentName(student.getStudentName());
        dto.setBookId(record.getBook().getBookId());
        dto.setBookTitle(record.getBook().getBook_title());
        dto.setReturningQuantity(request.getNumberOfBooks());
        dto.setOutstandingQuantity(outstandingQuantity);
        dto.setBorrowDate(record.getBorrowDate());
        dto.setDueDate(dueDate);
        dto.setReturnDate(request.getReturnDate());
        dto.setDaysSinceBorrowing(Math.max(
                0, ChronoUnit.DAYS.between(record.getBorrowDate(), request.getReturnDate())));
        dto.setLateDays(lateDays);
        dto.setBaseBorrowingCharge(baseBorrowingCharge);
        dto.setBaseChargeForReturningQuantity(baseChargeForReturningQuantity);
        dto.setFineForOneBook(fineForOneBook);
        dto.setFineForReturningQuantity(fineForReturningQuantity);
        dto.setLateFine(lateFine);
        dto.setTotalAmountDue(totalAmountDue);
        dto.setAlreadyPaidAmount(alreadyPaidAmount);
        dto.setRemainingUnpaidAmount(remainingUnpaidAmount);
        dto.setPaymentStatus(remainingUnpaidAmount.signum() == 0
                ? "Paid"
                : alreadyPaidAmount.signum() > 0 ? "Partially Paid" : "Unpaid");
        return dto;
    }

    private void validateReturnRequest(
            Student student,
            BorrowRecord record,
            ReturnFinePreviewRequestDto request) {
        if (!Objects.equals(record.getStudent().getStudentid(), student.getStudentid())
                || !Objects.equals(record.getBook().getBookId(), request.getBookId())) {
            throw notFound("Borrow record not found for this student and book");
        }
        if (request.getNumberOfBooks() == null || request.getNumberOfBooks() <= 0) {
            throw badRequest("Return quantity must be a positive whole number");
        }
        if (request.getNumberOfBooks() > Objects.requireNonNullElse(record.getNumberofBooks(), 0)) {
            throw badRequest("Return quantity exceeds this record's outstanding quantity");
        }
        if (request.getReturnDate() == null
                || request.getReturnDate().isBefore(record.getBorrowDate())) {
            throw badRequest("Return date cannot be before the borrow date");
        }
        Optional<LocalDate> latestReturn = returnBookRepository
                .findAllByBorrowRecord_BorrowRecordIdOrderByReturnDateAscReturnBookIdAsc(
                        record.getBorrowRecordId())
                .stream()
                .map(ReturnBook::getReturnDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
        if (latestReturn.isPresent() && request.getReturnDate().isBefore(latestReturn.get())) {
            throw badRequest("Return date cannot be before a previous partial return");
        }
    }

    private long lateDays(LocalDate dueDate, LocalDate endDate) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, endDate));
    }

    private void validatePayment(FinePaymentRequestDto request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment must be greater than LKR 0.00");
        }
        if (request.getAmount().scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment must have no more than two decimal places");
        }
        if (request.getIdempotencyKey() == null
                || !request.getIdempotencyKey().matches(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid payment request key is required");
        }
    }

    private Student findStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("Student not found"));
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
