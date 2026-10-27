package Student.example.demo.Dto;

import lombok.Data;

@Data
public class LibrarySummaryResponse {
    private long totalStudents;
    private long totalMaleStudents;
    private long totalFemaleStudents;
    private long totalBooksCurrentlyBorrowed;

    private long studentsBorrowing0Books;
    private long studentsBorrowing1Book;
    private long studentsBorrowing2Books;
    private long studentsBorrowing3Books;
    private long studentsBorrowing4Books;
    private long studentsBorrowing5Books;

    private long totalRegisteredBooks;
    private long totalAvailableBooks;
    private long totalBorrowedBooks;
}