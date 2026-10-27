package Student.example.demo.Entity;

import jakarta.persistence.*;

import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class ReturnBook {


    @Id
    @GeneratedValue(strategy =GenerationType.IDENTITY)
    private Long returnBookId;

    private  Integer numberOfBooks;
    private  LocalDate returnDate;

    @Column(name = "idempotency_key", length = 36)
    private String idempotencyKey;

    @ManyToOne
    @JoinColumn(name = "Studentid")
    private  Student student;

    @ManyToOne
    @JoinColumn(name="BookId")
    private Book book;

    @ManyToOne
    @JoinColumn(name = "BorrowRecordId")
    private  BorrowRecord borrowRecord;
}
