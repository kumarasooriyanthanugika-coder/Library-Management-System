package Student.example.demo.Service.Serviceimplementation;

import Student.example.demo.Dto.BookDto;
import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Mapper.BookMapper;
import Student.example.demo.Repository.BookRepository;
import Student.example.demo.Service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceimple implements BookService {
    private  final BookRepository repository;
    private  final BookMapper mapper;


    @Override
    public BookDto create(BookDto dto){


//        if (dto.getNumberOfBooks() == null || dto.getNumberOfBooks() <= 0) {
//            throw new RuntimeException("Number of books must be greater than 0");
//        }
//
//        if (dto.getNumberOfBooks() > 5) {
//            throw new RuntimeException("Maximum 5 books can be borrowed");
//        }
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }


    @Override
    public List<BookDto>getAll(){
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public BookDto getById(Long id){
        return repository.findById(id).map(mapper::toDto).orElseThrow();
    }


}

