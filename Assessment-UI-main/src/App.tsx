import { useState } from "react";
import Layout from "./components/layout/Layout";
import Students from "./pages/Students";
import Books from "./pages/Books";
import BorrowReturn from "./pages/BorrowReturn";
import LibrarySummary from "./pages/LibrarySummary";

export default function App() {
  const [page, setPage] = useState("students");

  const renderPage = () => {
    switch (page) {
      case "books": return <Books />;
      case "borrow-return": return <BorrowReturn />;
      case "summary": return <LibrarySummary />;
      default: return <Students />;
    }
  };

  return (
    <Layout activePage={page} onNavigate={setPage}>
      {renderPage()}
    </Layout>
  );
}