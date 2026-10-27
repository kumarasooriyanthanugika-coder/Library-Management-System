package Student.example.demo.Controller;

import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class StudentControllerTest {

    private StudentController controller;

    @BeforeEach
    void setUp() {
        controller = new StudentController(
                new InMemoryStudentService()
        );
    }

    @Test
    void createShouldReturnCreatedStudent() {
        StudentDto input = student("Thanugika");

        StudentDto result = controller.Create(input);

        assertNotNull(result.getStudentid());
        assertEquals("Thanugika", result.getStudentName());
        assertEquals("22", result.getAge());
        assertEquals("F", result.getGender());
        assertEquals("Software Engineering", result.getCourse());
        assertEquals(new BigDecimal("0.00"), result.getAmount());
    }

    @Test
    void getAllShouldReturnAllStudents() {
        controller.Create(student("Thanugika"));
        controller.Create(student("Abi"));

        List<StudentDto> result = controller.getAll();

        assertEquals(2, result.size());
        assertEquals("Thanugika", result.get(0).getStudentName());
        assertEquals("Abi", result.get(1).getStudentName());
    }

    @Test
    void getAllShouldReturnEmptyListWhenNoStudentsExist() {
        assertTrue(controller.getAll().isEmpty());
    }

    @Test
    void getByIdShouldReturnMatchingStudent() {
        StudentDto first = controller.Create(student("Thanugika"));
        controller.Create(student("Abi"));

        StudentDto result = controller.getById(
                first.getStudentid()
        );

        assertEquals(first.getStudentid(), result.getStudentid());
        assertEquals("Thanugika", result.getStudentName());
    }

    @Test
    void updateShouldReturnUpdatedStudent() {
        StudentDto created = controller.Create(student("Old Name"));
        StudentDto changes = student("Updated Name");
        changes.setCourse("Computing");

        StudentDto result = controller.update(
                created.getStudentid(), changes
        );

        assertEquals(created.getStudentid(), result.getStudentid());
        assertEquals("Updated Name", result.getStudentName());
        assertEquals("Computing", result.getCourse());
        assertEquals(
                "Updated Name",
                controller.getById(created.getStudentid())
                        .getStudentName()
        );
    }

    @Test
    void deleteShouldRemoveOnlySelectedStudent() {
        StudentDto first = controller.Create(student("Thanugika"));
        StudentDto second = controller.Create(student("Abi"));

        Map<String, String> result = controller.delete(
                first.getStudentid()
        );

        assertEquals(
                "Student deleted successfully",
                result.get("message")
        );
        assertEquals(1, controller.getAll().size());
        assertEquals(
                second.getStudentid(),
                controller.getAll().get(0).getStudentid()
        );
    }

    @Test
    void getByIdShouldPropagateNoSuchElementExceptionWhenStudentNotFound() {
        assertThrows(NoSuchElementException.class, () -> controller.getById(999L));
    }

    @Test
    void updateShouldPropagateNoSuchElementExceptionWhenStudentNotFound() {
        StudentDto changes = student("Non-existent");
        assertThrows(NoSuchElementException.class, () -> controller.update(999L, changes));
    }

    @Test
    void deleteShouldPropagateNoSuchElementExceptionWhenStudentNotFound() {
        assertThrows(NoSuchElementException.class, () -> controller.delete(999L));
    }

    private StudentDto student(String name) {
        StudentDto dto = new StudentDto();
        dto.setStudentName(name);
        dto.setAge("22");
        dto.setGender("F");
        dto.setCourse("Software Engineering");
        dto.setAmount(new BigDecimal("0.00"));
        return dto;
    }

    // Test-only service. It does not connect to the database.
    private static class InMemoryStudentService
            implements StudentService {

        private final Map<Long, StudentDto> students =
                new LinkedHashMap<>();

        private long nextId = 1;

        @Override
        public StudentDto create(StudentDto dto) {
            dto.setStudentid(nextId++);
            students.put(dto.getStudentid(), dto);
            return dto;
        }

        @Override
        public List<StudentDto> getAll() {
            return new ArrayList<>(students.values());
        }

        @Override
        public StudentDto getById(Long id) {
            StudentDto dto = students.get(id);

            if (dto == null) {
                throw new NoSuchElementException(
                        "Student not found"
                );
            }

            return dto;
        }

        @Override
        public StudentDto Update(Long id, StudentDto dto) {
            getById(id);
            dto.setStudentid(id);
            students.put(id, dto);
            return dto;
        }

        @Override
        public void delete(Long id) {
            getById(id);
            students.remove(id);
        }
    }
}