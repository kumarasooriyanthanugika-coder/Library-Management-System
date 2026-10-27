package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.ReturnBookDto;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.ReturnBook;
import Student.example.demo.Repository.BookRepository;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.ReturnBookRepository;
import Student.example.demo.Service.FineService;
import Student.example.demo.Service.ReturnBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReturnBookServiceimple implements ReturnBookService {

    private final ReturnBookRepository repository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FineService fineService;

    @Override
    @Transactional
    public ReturnBookDto create(ReturnBookDto dto) {
        if (dto.getStudentid() == null
                || dto.getBookId() == null
                || dto.getBorrowRecordId() == null
                || dto.getReturnDate() == null
                || dto.getNumberOfBooks() == null
                || dto.getNumberOfBooks() <= 0
                || dto.getIdempotencyKey() == null
                || !isUuid(dto.getIdempotencyKey())) {
            throw badRequest("IDs, return date and valid quantity are required");
        }

        ReturnBook previous = repository.findByIdempotencyKey(dto.getIdempotencyKey())
                .orElse(null);
        if (previous != null) {
            if (!sameRequest(previous, dto)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This return request key was already used for a different return");
            }
            return toDto(previous);
        }

        BorrowRecord borrow = borrowRecordRepository
                .findByIdForUpdate(dto.getBorrowRecordId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Borrow record not found"));

        previous = repository.findByIdempotencyKey(dto.getIdempotencyKey())
                .orElse(null);
        if (previous != null) {
            if (!sameRequest(previous, dto)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This return request key was already used for a different return");
            }
            return toDto(previous);
        }

        if (borrow.getStudent() == null
                || borrow.getBook() == null
                || !dto.getStudentid().equals(
                borrow.getStudent().getStudentid())
                || !dto.getBookId().equals(
                borrow.getBook().getBookId())) {
            throw badRequest("Student or book does not match borrow record");
        }

        Integer remaining = borrow.getNumberofBooks();
        int returned = dto.getNumberOfBooks();

        if (remaining == null
                || remaining <= 0) {
            throw badRequest("This borrow record is already returned");
        }

        if (returned > remaining) {
            throw badRequest("Return quantity exceeds borrowed quantity");
        }

        Student.example.demo.Dto.ReturnFinePreviewRequestDto previewRequest =
                new Student.example.demo.Dto.ReturnFinePreviewRequestDto();
        previewRequest.setStudentid(dto.getStudentid());
        previewRequest.setBookId(dto.getBookId());
        previewRequest.setBorrowRecordId(dto.getBorrowRecordId());
        previewRequest.setNumberOfBooks(returned);
        previewRequest.setReturnDate(dto.getReturnDate());
        fineService.previewReturn(previewRequest);

        Book book = borrow.getBook();

        if (book.getAvailableQuantity() == null) {
            throw badRequest("Book available quantity is missing");
        }

        try {
            book.setAvailableQuantity(Math.addExact(book.getAvailableQuantity(), returned));
        } catch (ArithmeticException exception) {
            throw badRequest("Book stock quantity exceeds the supported limit");
        }

        borrow.setNumberofBooks(remaining - returned);

        if (remaining == returned) {
            borrow.setReturnDate(dto.getReturnDate());
        }

        ReturnBook returnBook = new ReturnBook();
        returnBook.setStudent(borrow.getStudent());
        returnBook.setBook(book);
        returnBook.setBorrowRecord(borrow);
        returnBook.setNumberOfBooks(returned);
        returnBook.setReturnDate(dto.getReturnDate());
        returnBook.setIdempotencyKey(dto.getIdempotencyKey());

        bookRepository.save(book);
        borrowRecordRepository.save(borrow);
        ReturnBook saved = repository.saveAndFlush(returnBook);

        return toDto(saved);
    }

    private ReturnBookDto toDto(ReturnBook saved) {
        ReturnBookDto response = new ReturnBookDto();
        response.setStudentid(saved.getStudent().getStudentid());
        response.setBookId(saved.getBook().getBookId());
        response.setBorrowRecordId(saved.getBorrowRecord().getBorrowRecordId());
        response.setNumberOfBooks(saved.getNumberOfBooks());
        response.setReturnDate(saved.getReturnDate());
        response.setIdempotencyKey(saved.getIdempotencyKey());
        return response;
    }

    private boolean sameRequest(ReturnBook previous, ReturnBookDto request) {
        return Objects.equals(previous.getStudent().getStudentid(), request.getStudentid())
                && Objects.equals(previous.getBook().getBookId(), request.getBookId())
                && Objects.equals(previous.getBorrowRecord().getBorrowRecordId(), request.getBorrowRecordId())
                && Objects.equals(previous.getNumberOfBooks(), request.getNumberOfBooks())
                && Objects.equals(previous.getReturnDate(), request.getReturnDate());
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }
}