package Student.example.demo.Service;

import Student.example.demo.Dto.BorrowFineRecordDto;
import Student.example.demo.Dto.FinePaymentRequestDto;
import Student.example.demo.Dto.ReturnFinePreviewDto;
import Student.example.demo.Dto.ReturnFinePreviewRequestDto;

import java.util.List;
import java.math.BigDecimal;

public interface FineService {
    List<BorrowFineRecordDto> getFineRecords(Long studentId);

    BigDecimal getStudentTotalUnpaidAmount(Long studentId);

    ReturnFinePreviewDto previewReturn(ReturnFinePreviewRequestDto request);

    BorrowFineRecordDto createPayment(
            Long studentId,
            Long borrowRecordId,
            FinePaymentRequestDto request);
}
