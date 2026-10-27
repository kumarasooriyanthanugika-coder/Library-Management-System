package Student.example.demo.Service;

import Student.example.demo.Dto.BorrowRecordDto;

import java.util.List;

public interface BorrowRecordService {

    BorrowRecordDto create(BorrowRecordDto dto);
    List<BorrowRecordDto> getAll();
    List<BorrowRecordDto> getActiveForStudent(Long studentId);

}