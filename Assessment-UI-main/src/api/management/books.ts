import type { Book, BookInput } from "../../types";
import { apiClient } from "../../lib/api";
import { API_ENDPOINTS } from "../../utils/apiEndpoint";

/** Book management API methods. */
export const booksApi = {
  getAll(): Promise<Book[]> {
    return apiClient.request<Book[]>(API_ENDPOINTS.books);
  },

  create(input: BookInput): Promise<Book> {
    return apiClient.request<Book>(API_ENDPOINTS.books, {
      method: "POST",
      body: JSON.stringify(input),
    });
  },
};
