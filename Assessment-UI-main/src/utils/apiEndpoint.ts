/** Base URL and API endpoint paths used by the application. */
export const API_BASE_URL = "http://localhost:8080";

export const API_ENDPOINTS = {
  students: "/api/students",
  studentById: (id: number) => `/api/students/${id}`,
  studentsByGender: (gender: string) => `/api/students/gender/${gender}`,
  studentFines: (id: number) => `/api/students/${id}/fine-records`,
  borrowRecordPayment: (studentId: number, borrowRecordId: number) =>
    `/api/students/${studentId}/borrow-records/${borrowRecordId}/payments`,
  books: "/api/books",
  booksById:(id:number)=>`/api/books/${id}`,
  borrow: "/api/borrow",
  activeStudentBorrowRecords: (studentId: number) => `/api/borrow/student/${studentId}/active`,
  borrowById: (id: number) => `/api/borrow/${id}`,
  return: "/api/return",
  returnPreview: "/api/return/fine-preview",
  returnById: (id: number) => `/api/return/${id}`,
  librarySummary: "/api/library/summary",
} as const;
