package Student.example.demo.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class BorrowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long BorrowRecordId;
    private LocalDate BorrowDate;
    private  LocalDate ReturnDate;
    private Integer numberofBooks;
    private LocalDate dueDate;

    @Column(name = "borrowed_quantity")
    private Integer borrowedQuantity;



    @ManyToOne
    @JoinColumn(name = "StudentId")
    private  Student student;

    @ManyToOne
    @JoinColumn(name="BookId")
    private Book book;


}
