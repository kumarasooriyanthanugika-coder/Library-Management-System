/** Shared API client configuration. */
import { API_BASE_URL } from "../utils/apiEndpoint";

export const apiClient = {
  baseUrl: "http://localhost:8080",

  async request<T>(path: string, options?: RequestInit): Promise<T> {
    let response: Response;
    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...options?.headers,
        },
      });
    } catch (error) {
      if (error instanceof TypeError) {
        throw new Error(
          `Cannot reach the library API at ${API_BASE_URL}. Check that the Spring Boot backend is running and allows requests from this frontend (CORS).`,
        );
      }
      throw error;
    }

    if (!response.ok) {
      const fallbackMessage = `API request failed: ${response.status} ${response.statusText}`;
      const body = await response.text();
      let message = body.trim() || fallbackMessage;

      if (body.trim() && response.headers.get("content-type")?.includes("application/json")) {
        try {
          const parsed: unknown = JSON.parse(body);
          if (parsed && typeof parsed === "object") {
            const errorBody = parsed as Record<string, unknown>;
            const apiMessage = errorBody.message ?? errorBody.error ?? errorBody.detail;
            if (typeof apiMessage === "string" && apiMessage.trim()) {
              message = apiMessage;
            }
          }
        } catch {
          message = body;
        }
      }

      throw new Error(message);
    }

    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  },
};
