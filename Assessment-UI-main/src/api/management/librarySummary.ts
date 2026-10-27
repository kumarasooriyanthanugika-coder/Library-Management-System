import type { LibrarySummary } from "../../types";
import { apiClient } from "../../lib/api";
import { API_ENDPOINTS } from "../../utils/apiEndpoint";

/** Library summary API method. */
export const librarySummaryApi = {
  get(): Promise<LibrarySummary> {
    return apiClient.request<LibrarySummary>(API_ENDPOINTS.librarySummary);
  },
};
