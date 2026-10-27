package Student.example.demo.Repository;

import Student.example.demo.Entity.FinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FinePaymentRepository
        extends JpaRepository<FinePayment, Long> {

    @Query("""
        SELECT f FROM FinePayment f
        WHERE f.borrowRecord.BorrowRecordId IN :borrowRecordIds
        ORDER BY f.paidAt ASC, f.paymentId ASC
        """)
    List<FinePayment>
    findAllByBorrowRecord_BorrowRecordIdInOrderByPaidAtAscPaymentIdAsc(
            @Param("borrowRecordIds")
            Collection<Long> borrowRecordIds
    );

    @Query("""
        SELECT f FROM FinePayment f
        WHERE f.borrowRecord.BorrowRecordId = :borrowRecordId
        ORDER BY f.paidAt ASC, f.paymentId ASC
        """)
    List<FinePayment>
    findAllByBorrowRecord_BorrowRecordIdOrderByPaidAtAscPaymentIdAsc(
            @Param("borrowRecordId")
            Long borrowRecordId
    );

    Optional<FinePayment> findByIdempotencyKey(
            String idempotencyKey
    );
}