package Student.example.demo.Controller;

import Student.example.demo.Dto.LibrarySummaryResponse;
import Student.example.demo.Service.LibrarySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/library/summary")
@CrossOrigin
public class LibrarySummaryController {

    private final LibrarySummaryService librarySummaryService;

    @GetMapping
    public LibrarySummaryResponse getSummary() {
        return librarySummaryService.getSummary();
    }
}