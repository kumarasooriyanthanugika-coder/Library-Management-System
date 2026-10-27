package Student.example.demo.Controller;


import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Entity.Student;
import Student.example.demo.Service.StudentService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import  java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin

public class StudentController {
    private  final StudentService service;

    @PostMapping
    public StudentDto Create(@RequestBody StudentDto dto){
        return service.create(dto);
    }

    @GetMapping
    public List<StudentDto>getAll(){
        return service.getAll();
    }

    @GetMapping("/{id}")
    public StudentDto getById(@PathVariable Long id){
        return  service.getById(id);
    }



    @PutMapping("/{id}")
    public StudentDto update(
            @PathVariable Long id,
            @RequestBody StudentDto dto) {

        return service.Update(id, dto);
    }



    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {

        service.delete(id);

        return Map.of("message", "Student deleted successfully");
    }

}



