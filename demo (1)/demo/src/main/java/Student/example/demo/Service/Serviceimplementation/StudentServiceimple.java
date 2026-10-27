package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Entity.Student;
import Student.example.demo.Mapper.StudentMapper;
import Student.example.demo.Repository.StudentRepository;
import Student.example.demo.Service.StudentService;
import Student.example.demo.Service.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class StudentServiceimple implements StudentService{
    private  final StudentRepository repository;
    private  final StudentMapper mapper;
    private final FineService fineService;


    @Override
    public StudentDto create(StudentDto dto){
        Student student = repository.save(mapper.toEntity(dto));
        return withCurrentBalance(mapper.toDto(student));

    }

    @Override
    public List<StudentDto> getAll() {

        return repository.findAll().stream()
                .map(mapper::toDto)
                .map(this::withCurrentBalance)
                .toList();
    }

    @Override
    public StudentDto getById(Long id) {
        return repository.findById(id).map(mapper::toDto)
                .map(this::withCurrentBalance)
                .orElseThrow();

    }

    @Override
    public StudentDto Update(Long id, StudentDto dto) {
        Student student=repository.findById(id).orElseThrow();
        mapper.updateEntity(dto,student);
        return withCurrentBalance(mapper.toDto(repository.save(student)));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private StudentDto withCurrentBalance(StudentDto dto) {
        dto.setAmount(fineService.getStudentTotalUnpaidAmount(dto.getStudentid()));
        return dto;
    }

}



