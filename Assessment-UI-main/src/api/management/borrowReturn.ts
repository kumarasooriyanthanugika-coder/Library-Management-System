import type { BorrowInput, BorrowRecord, ReturnFinePreview, ReturnInput } from "../../types";
import { apiClient } from "../../lib/api";
import { API_ENDPOINTS } from "../../utils/apiEndpoint";
import { booksApi } from "./books";
import { studentsApi } from "./students";

/** Borrowing and returning API methods. */
export const borrowReturnApi = {
  getStudents: studentsApi.getAll,
  getBooks: booksApi.getAll,

  getRecords(): Promise<BorrowRecord[]> {
    return apiClient.request<BorrowRecord[]>(API_ENDPOINTS.borrow);
  },

  getActiveRecordsForStudent(studentId: number): Promise<BorrowRecord[]> {
    return apiClient.request<BorrowRecord[]>(API_ENDPOINTS.activeStudentBorrowRecords(studentId));
  },

  previewReturn(input: Omit<ReturnInput, "idempotencyKey">): Promise<ReturnFinePreview> {
    return apiClient.request<ReturnFinePreview>(API_ENDPOINTS.returnPreview, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  borrow(input: BorrowInput): Promise<BorrowRecord> {
    return apiClient.request<BorrowRecord>(API_ENDPOINTS.borrow, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  return(input: ReturnInput): Promise<ReturnInput> {
    return apiClient.request<ReturnInput>(API_ENDPOINTS.return, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
};
