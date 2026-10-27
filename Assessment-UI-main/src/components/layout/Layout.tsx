import { BookOpen, BookMarked, Users, ArrowLeftRight, ClipboardList } from "lucide-react";
import type { ReactNode } from "react";

interface LayoutProps {
  activePage: string;
  onNavigate: (page: string) => void;
  children: ReactNode;
}

const navItems = [
  { id: "students", label: "Students", icon: Users },
  { id: "books", label: "Books", icon: BookOpen },
  { id: "borrow-return", label: "Borrow / Return", icon: ArrowLeftRight },
  { id: "summary", label: "Library Summary", icon: ClipboardList },
];

export default function Layout({ activePage, onNavigate, children }: LayoutProps) {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon"><BookMarked size={23} /></div>
          <div>
            <strong>Library</strong>
            <span>Management System</span>
          </div>
        </div>

        <nav>
          {navItems.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={`nav-item ${activePage === id ? "active" : ""}`}
              onClick={() => onNavigate(id)}
            >
              <Icon size={19} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <span>LIBRARY MANAGEMENT SYSTEM</span>
          <span>LIBRARY APP</span>
        </header>
        <div className="content">{children}</div>
      </main>
    </div>
  );
}
