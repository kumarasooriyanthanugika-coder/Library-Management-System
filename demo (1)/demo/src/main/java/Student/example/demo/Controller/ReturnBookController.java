package Student.example.demo.Controller;


import Student.example.demo.Dto.ReturnBookDto;
import Student.example.demo.Service.ReturnBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/return")
@RequiredArgsConstructor
@CrossOrigin
public class ReturnBookController {
    private  final ReturnBookService service;
    @PostMapping
    public ReturnBookDto create(@Valid @RequestBody ReturnBookDto dto){
        return  service.create(dto);
    }

}
