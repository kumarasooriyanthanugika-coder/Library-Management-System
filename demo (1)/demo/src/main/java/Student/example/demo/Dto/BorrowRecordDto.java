package Student.example.demo.Dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowRecordDto {

    private Long borrowRecordId;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private Integer numberOfBooks;
    private String bookTitle;
    private Long Studentid;
    private Long BookId;
}