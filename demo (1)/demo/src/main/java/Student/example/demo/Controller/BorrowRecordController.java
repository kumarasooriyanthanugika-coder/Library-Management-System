package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowRecordDto;
import Student.example.demo.Service.BorrowRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
@CrossOrigin
public class BorrowRecordController {

    private final BorrowRecordService service;

    @PostMapping
    public BorrowRecordDto create(
            @RequestBody BorrowRecordDto dto) {
        return service.create(dto);
    }


    @GetMapping
    public List<BorrowRecordDto> getAll() {

        return service.getAll();
    }

    @GetMapping("/student/{studentId}/active")
    public List<BorrowRecordDto> getActiveForStudent(@PathVariable Long studentId) {
        return service.getActiveForStudent(studentId);
    }

}