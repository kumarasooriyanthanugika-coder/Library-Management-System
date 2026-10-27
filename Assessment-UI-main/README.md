# Library Management System

React + TypeScript frontend for a library management system. The app uses the API base URL and endpoint paths in `src/utils/apiEndpoint.ts`; configure those values for your backend before running it.

## Run the app

```bash
npm install
npm run dev
```

## Project structure

```text
src/
  api/management/  Feature-specific API methods
  components/       Reusable visual components
  lib/api.ts        Shared fetch client
  pages/            Page-level React components
  types/            API data types
  utils/            Endpoint URLs and shared helpers
```

Each page uses its corresponding service: `studentsApi`, `booksApi`, `borrowReturnApi`, or `librarySummaryApi`. These services call the backend through `src/lib/api.ts`. Success and error messages are handled by the pages.
