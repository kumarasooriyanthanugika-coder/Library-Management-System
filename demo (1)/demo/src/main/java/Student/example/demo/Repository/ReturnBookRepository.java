package Student.example.demo.Repository;

import Student.example.demo.Entity.ReturnBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReturnBookRepository
        extends JpaRepository<ReturnBook, Long> {

    @Query("""
        SELECT r FROM ReturnBook r
        WHERE r.borrowRecord.BorrowRecordId IN :borrowRecordIds
        ORDER BY r.returnDate ASC, r.returnBookId ASC
        """)
    List<ReturnBook>
    findAllByBorrowRecord_BorrowRecordIdInOrderByReturnDateAscReturnBookIdAsc(
            @Param("borrowRecordIds")
            Collection<Long> borrowRecordIds
    );

    @Query("""
        SELECT r FROM ReturnBook r
        WHERE r.borrowRecord.BorrowRecordId = :borrowRecordId
        ORDER BY r.returnDate ASC, r.returnBookId ASC
        """)
    List<ReturnBook>
    findAllByBorrowRecord_BorrowRecordIdOrderByReturnDateAscReturnBookIdAsc(
            @Param("borrowRecordId")
            Long borrowRecordId
    );

    java.util.Optional<ReturnBook> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}