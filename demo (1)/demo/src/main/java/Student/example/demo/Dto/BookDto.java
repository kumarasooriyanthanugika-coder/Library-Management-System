package Student.example.demo.Dto;

import lombok.Data;

@Data
public class BookDto {


    private Long BookId;
    private String bookTitle;
    private String author;
    private String category;
    private Integer availableQuantity;
}
