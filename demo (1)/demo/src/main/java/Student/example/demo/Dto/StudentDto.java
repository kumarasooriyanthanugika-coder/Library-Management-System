package Student.example.demo.Dto;

import lombok.Data;

import java.math.BigDecimal;


@Data
public class StudentDto {
    private Long Studentid;
    private String studentName;
    private String age;
    private String gender;
    private BigDecimal amount;
    private String Course;
}

