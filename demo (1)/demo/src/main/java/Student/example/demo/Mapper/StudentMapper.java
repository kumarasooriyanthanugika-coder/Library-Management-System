package Student.example.demo.Mapper;



import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Entity.Student;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;


@Component
public class StudentMapper {

    public Student toEntity(StudentDto dto) {
        Student student = new Student();

        student.setStudentName(dto.getStudentName());
        student.setAge(dto.getAge());
        student.setGender(dto.getGender());
        student.setAmount(dto.getAmount());
        student.setCourse(dto.getCourse());

        return student;
    }

    public StudentDto toDto(Student student) {

        StudentDto dto = new StudentDto();

        dto.setStudentid(student.getStudentid());
        dto.setStudentName(student.getStudentName());
        dto.setAge(student.getAge());
        dto.setGender(student.getGender());
        dto.setCourse(student.getCourse());
        dto.setAmount(student.getAmount());

        return dto;
    }

    public void updateEntity(StudentDto dto,Student student){
        student.setStudentName(dto.getStudentName());
        student.setAge(dto.getAge());
        student.setGender(dto.getGender());
        student.setAmount(dto.getAmount());
        student.setCourse(dto.getCourse());

    }
}
