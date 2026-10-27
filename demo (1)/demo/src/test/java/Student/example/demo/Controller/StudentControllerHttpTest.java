package Student.example.demo.Controller;

import Student.example.demo.Dto.StudentDto;
import Student.example.demo.Service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentControllerHttpTest {

    private MockMvc mockMvc;
    private InMemoryStudentService fakeService;

    @BeforeEach
    void setUp() {
        fakeService = new InMemoryStudentService();
        StudentController controller = new StudentController(fakeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postStudentShouldReturnOkWithCreatedStudentJson() throws Exception {
        String jsonPayload = """
                {
                    "studentName": "Thanugika",
                    "age": "22",
                    "gender": "F",
                    "Course": "Software Engineering"
                }
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Studentid", notNullValue()))
                .andExpect(jsonPath("$.studentName", is("Thanugika")))
                .andExpect(jsonPath("$.age", is("22")))
                .andExpect(jsonPath("$.gender", is("F")))
                .andExpect(jsonPath("$.Course", is("Software Engineering")));
    }

    @Test
    void getAllStudentsShouldReturnOkWithStudentListJson() throws Exception {
        fakeService.create(student("Student 1", "21", "M", "IT"));
        fakeService.create(student("Student 2", "22", "F", "CS"));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].studentName", is("Student 1")))
                .andExpect(jsonPath("$[1].studentName", is("Student 2")));
    }

    @Test
    void getStudentByIdShouldReturnOkWithMatchingStudentJson() throws Exception {
        StudentDto created = fakeService.create(student("Thanugika", "22", "F", "SE"));

        mockMvc.perform(get("/api/students/{id}", created.getStudentid()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Studentid", is(created.getStudentid().intValue())))
                .andExpect(jsonPath("$.studentName", is("Thanugika")));
    }

    @Test
    void putStudentShouldReturnOkWithUpdatedStudentJson() throws Exception {
        StudentDto created = fakeService.create(student("Original", "20", "M", "Maths"));
        String updatePayload = """
                {
                    "studentName": "Updated Name",
                    "age": "21",
                    "gender": "M",
                    "Course": "Physics"
                }
                """;

        mockMvc.perform(put("/api/students/{id}", created.getStudentid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.Studentid", is(created.getStudentid().intValue())))
                .andExpect(jsonPath("$.studentName", is("Updated Name")))
                .andExpect(jsonPath("$.Course", is("Physics")));
    }

    @Test
    void deleteStudentShouldReturnOkWithMessageJson() throws Exception {
        StudentDto created = fakeService.create(student("To Delete", "20", "M", "Art"));

        mockMvc.perform(delete("/api/students/{id}", created.getStudentid()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("Student deleted successfully")));
    }

    private StudentDto student(String name, String age, String gender, String course) {
        StudentDto dto = new StudentDto();
        dto.setStudentName(name);
        dto.setAge(age);
        dto.setGender(gender);
        dto.setCourse(course);
        dto.setAmount(new BigDecimal("0.00"));
        return dto;
    }

    private static class InMemoryStudentService implements StudentService {
        private final Map<Long, StudentDto> students = new LinkedHashMap<>();
        private long nextId = 1L;

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
                throw new NoSuchElementException("Student not found");
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
