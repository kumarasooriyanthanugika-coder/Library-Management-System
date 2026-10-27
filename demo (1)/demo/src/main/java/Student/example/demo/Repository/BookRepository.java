package Student.example.demo.Repository;

import Student.example.demo.Entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book ,Long> {
}
