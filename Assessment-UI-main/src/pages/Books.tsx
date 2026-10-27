import { useEffect, useState } from "react";
import { Plus } from "lucide-react";
import Modal from "../components/common/Modal";
import PageHeader from "../components/common/PageHeader";
import { booksApi } from "../api/management/books";
import type { Book, BookInput } from "../types";

const emptyBook: BookInput = {bookTitle: "", author: "", category: "", availableQuantity: 1 };

export default function Books() {
  const [books, setBooks] = useState<Book[]>([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<BookInput>(emptyBook);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = async () => setBooks(await booksApi.getAll());
  useEffect(() => { load(); }, []);

  const save = async () => {
    try {
      if (!form.bookTitle.trim()) throw new Error("Book title is required");
      if (!form.author.trim()) throw new Error("Author is required");
      if (!form.category.trim()) throw new Error("Category is required");
      if (form.availableQuantity < 0) throw new Error("Available quantity cannot be negative");
      await booksApi.create(form);
      await load();
      setOpen(false);
      setForm(emptyBook);
      setError("");
      setSuccess("Successfully added book record.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Operation failed");
    }
  };

  return (
    <>
      <PageHeader
        title="Book Management"
        description="Register books and view the current library inventory."
        action={<button className="primary-button" onClick={() => { setOpen(true); setError(""); }}><Plus size={18} /> Register Book</button>}
      />

      {success && <div className="success-message" role="status">{success}</div>}

      <div className="table-card">
        <div className="table-scroll">
          <table>
            <thead><tr><th>ID</th><th>Book Title</th><th>Author</th><th>Category</th><th>Available Quantity</th></tr></thead>
            <tbody>
              {books.map((book) => (
                <tr key={book.BookId}>
                  <td>#{book.BookId}</td>
                  <td className="strong">{book.bookTitle}</td>
                  <td>{book.author}</td>
                  <td><span className="category-badge">{book.category}</span></td>
                  <td><span className={book.availableQuantity === 0 ? "stock-zero" : "stock"}>{book.availableQuantity}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={open} title="Register Book" onClose={() => setOpen(false)}>
        <div className="form-grid">
          <div className="form-field full"><label>Book Title</label><input value={form.bookTitle} onChange={(e) => setForm({ ...form, bookTitle: e.target.value })} /></div>
          <div className="form-field"><label>Author</label><input value={form.author} onChange={(e) => setForm({ ...form, author: e.target.value })} /></div>
          <div className="form-field"><label>Category</label><input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} /></div>
          <div className="form-field"><label>Available Quantity</label><input type="number" min="0" value={form.availableQuantity} onChange={(e) => setForm({ ...form, availableQuantity: Number(e.target.value) })} /></div>
        </div>
        {error && <div className="error-message">{error}</div>}
        <div className="modal-footer">
          <button className="secondary-button" onClick={() => setOpen(false)}>Cancel</button>
          <button className="primary-button" onClick={save}>Save Book</button>
        </div>
      </Modal>
    </>
  );
}
