package Student.example.demo.Service;

import Student.example.demo.Dto.StudentDto;

import java.util.List;

public interface StudentService {
    StudentDto create(StudentDto dto);
    List<StudentDto>getAll();
    StudentDto getById(Long id);
    StudentDto Update(Long id,StudentDto dto);
    void  delete(Long id);

}

