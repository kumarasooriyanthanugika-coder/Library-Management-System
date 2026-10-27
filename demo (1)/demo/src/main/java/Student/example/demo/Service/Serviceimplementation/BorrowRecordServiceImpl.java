package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.BorrowRecordDto;
import Student.example.demo.Entity.Book;
import Student.example.demo.Entity.BorrowRecord;
import Student.example.demo.Entity.Student;
import Student.example.demo.Mapper.BorrowRecordMapper;
import Student.example.demo.Repository.BookRepository;
import Student.example.demo.Repository.BorrowRecordRepository;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowRecordServiceImpl implements BorrowRecordService {

    private final BorrowRecordRepository repository;
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordMapper mapper;

    @Override
    public BorrowRecordDto create(BorrowRecordDto dto) {
        if (dto.getStudentid() == null
                || dto.getBookId() == null
                || dto.getNumberOfBooks() == null
                || dto.getNumberOfBooks() <= 0
                || dto.getBorrowDate() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Student, book, borrow date and a positive whole-book quantity are required");
        }

        Integer currentBooks =
                repository.getCurrentBorrowedBooks(dto.getStudentid());

        if (currentBooks + dto.getNumberOfBooks() > 5) {
            throw new RuntimeException(
                    "Student can borrow maximum 5 books only"
            );
        }


        Student student = studentRepository.findById(dto.getStudentid()).orElseThrow(() ->
                        new RuntimeException("Student not found"));

        Book book = bookRepository.findById(dto.getBookId()).orElseThrow(() ->
                        new RuntimeException("Book not found"));

        if (book.getAvailableQuantity() == null
                || book.getAvailableQuantity() < dto.getNumberOfBooks()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Borrow quantity exceeds available book stock");
        }

        book.setAvailableQuantity(
                book.getAvailableQuantity() - dto.getNumberOfBooks()
        );


        bookRepository.save(book);

        BorrowRecord record = mapper.toEntity(dto, student, book);

        BorrowRecord saved = repository.save(record);
        return mapper.toDto(saved);
    }

    @Override
    public List<BorrowRecordDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public List<BorrowRecordDto> getActiveForStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Student not found");
        }
        return repository.findActiveByStudentId(studentId).stream()
                .map(mapper::toDto)
                .toList();
    }
}