package Student.example.demo.Repository;

import Student.example.demo.Entity.BorrowRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository
        extends JpaRepository<BorrowRecord, Long> {

    @Query("""
        SELECT COALESCE(SUM(b.numberofBooks), 0)
        FROM BorrowRecord b
        WHERE b.student.Studentid = :Studentid
          AND b.ReturnDate IS NULL
          AND b.numberofBooks > 0
        """)
    Integer getCurrentBorrowedBooks(
            @Param("Studentid") Long Studentid
    );

    @Query("""
        SELECT b FROM BorrowRecord b
        WHERE b.student.Studentid = :studentId
        ORDER BY b.BorrowDate ASC, b.BorrowRecordId ASC
        """)
    List<BorrowRecord>
    findAllByStudent_StudentidOrderByBorrowDateAscBorrowRecordIdAsc(
            @Param("studentId") Long studentId
    );

    @Query("""
        SELECT br FROM BorrowRecord br
        WHERE br.student.Studentid = :studentId
          AND br.numberofBooks > 0
        ORDER BY br.BorrowDate ASC, br.BorrowRecordId ASC
        """)
    List<BorrowRecord> findActiveByStudentId(@Param("studentId") Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT br FROM BorrowRecord br
        WHERE br.BorrowRecordId = :borrowRecordId
        """)
    Optional<BorrowRecord> findByIdForUpdate(
            @Param("borrowRecordId") Long borrowRecordId
    );
}