package Student.example.demo.Controller;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;
import Student.example.demo.Service.FineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FineController {
    private final FineService fineService;

    @GetMapping("/students/{studentId}/fine-records")
    public List<BorrowFineRecordDto> getFineRecords(@PathVariable Long studentId) {
        return fineService.getFineRecords(studentId);
    }

    @PostMapping("/return/fine-preview")
    public ReturnFinePreviewDto previewReturn(
            @Valid @RequestBody ReturnFinePreviewRequestDto request) {
        return fineService.previewReturn(request);
    }

    @PostMapping("/students/{studentId}/borrow-records/{borrowRecordId}/payments")
    public BorrowFineRecordDto createPayment(
            @PathVariable Long studentId,
            @PathVariable Long borrowRecordId,
            @Valid @RequestBody FinePaymentRequestDto request) {
        return fineService.createPayment(studentId, borrowRecordId, request);
    }
}
