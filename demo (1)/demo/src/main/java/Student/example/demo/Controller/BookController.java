package Student.example.demo.Controller;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@CrossOrigin
public class BookController {
    private  final BookService service;

    @PostMapping
    public BookDto create(@RequestBody BookDto dto){
        return service.create(dto);
    }
    @GetMapping
    public List<BookDto> getAll(){
        return service.getAll();
    }

}
