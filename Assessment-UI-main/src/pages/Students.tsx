import { useEffect, useRef, useState } from "react";
import { Eye, Pencil, Plus, Search, Trash2, DollarSign } from "lucide-react";
import Modal from "../components/common/Modal";
import PageHeader from "../components/common/PageHeader";
import { studentsApi } from "../api/management/students";
import type { BorrowFineRecord, Gender, Student, StudentInput } from "../types";
import { formatCurrency } from "../utils/constants";

const emptyForm: StudentInput = { studentName: "", age: 0, gender: "" as Gender, Course: "" };

export default function Students() {
  const [students, setStudents] = useState<Student[]>([]);
  const [search, setSearch] = useState("");
  const [gender, setGender] = useState<"ALL" | Gender>("ALL");
  const [modal, setModal] = useState<"add" | "edit" | "fine" | "view" | "delete" | null>(null);
  const [selected, setSelected] = useState<Student | null>(null);
  const [viewingStudent, setViewingStudent] = useState<Student | null>(null);
  const [isLoadingStudent, setIsLoadingStudent] = useState(false);
  const [form, setForm] = useState<StudentInput>(emptyForm);
  const [fineRecords, setFineRecords] = useState<BorrowFineRecord[]>([]);
  const [isLoadingFine, setIsLoadingFine] = useState(false);
  const [fineError, setFineError] = useState("");
  const [paymentRecord, setPaymentRecord] = useState<BorrowFineRecord | null>(null);
  const [paymentAmount, setPaymentAmount] = useState("");
  const [paymentError, setPaymentError] = useState("");
  const [isSubmittingPayment, setIsSubmittingPayment] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const paymentInFlight = useRef(false);
  const paymentIdempotencyKey = useRef("");
  const fineRequestId = useRef(0);

  const load = async () => setStudents(await studentsApi.getAll());

  useEffect(() => { load(); }, []);

  const filtered = students.filter((s) =>
    (gender === "ALL" || s.gender === gender) &&
    `${s.studentName} ${s.Course} ${s.Studentid}`.toLowerCase().includes(search.toLowerCase())
  );

  const openAdd = () => {
    setError("");
    setSuccess("");
    setForm(emptyForm);
    setModal("add");
  };

  const openEdit = (student: Student) => {
    setError("");
    setSuccess("");
    setSelected(student);
    setForm({
      studentName: student.studentName,
      age: student.age,
      gender: student.gender,
      Course: student.Course ?? "",
    });
    setModal("edit");
  };

  const openView = async (studentId: number) => {
    setError("");
    setViewingStudent(null);
    setIsLoadingStudent(true);
    setModal("view");

    try {
      setViewingStudent(await studentsApi.getById(studentId));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load student");
    } finally {
      setIsLoadingStudent(false);
    }
  };

  const saveStudent = async () => {
    try {
      setError("");
      if (!form.studentName.trim()) throw new Error("Student name is required");
      if (form.age <= 0) throw new Error("Age must be positive");
      if (!form.gender) throw new Error("Gender is required");
      if (!form.Course.trim()) throw new Error("Course is required");

      const action = modal === "add" ? "Added" : "Updated";
      if (modal === "add") await studentsApi.create(form);
      if (modal === "edit" && selected) await studentsApi.update(selected.Studentid, form);

      await load();
      setModal(null);
      setSuccess(`Successfully ${action} student record.`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Operation failed");
    }
  };

  const removeStudent = async () => {
    if (!selected) return;
    try {
      setError("");
      await studentsApi.remove(selected.Studentid);
      await load();
      setModal(null);
      setSuccess("Successfully deleted student record.");
      setSelected(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Delete failed");
    }
  };

  const filterGender = async (value: "ALL" | Gender) => {
    setGender(value);
    if (value === "ALL") {
      await load();
    } else {
      setStudents(await studentsApi.getByGender(value));
    }
  };

  const openFine = async (student: Student) => {
    const requestId = ++fineRequestId.current;
    setSelected(student);
    setFineRecords([]);
    setPaymentRecord(null);
    setFineError("");
    setIsLoadingFine(true);
    setModal("fine");

    try {
      const records = await studentsApi.getFineRecords(student.Studentid);
      if (requestId === fineRequestId.current) setFineRecords(records);
    } catch (e) {
      if (requestId === fineRequestId.current) {
        setFineError(e instanceof Error ? e.message : "Could not load borrowing records");
      }
    } finally {
      if (requestId === fineRequestId.current) setIsLoadingFine(false);
    }
  };

  const openPayment = (record: BorrowFineRecord) => {
    setPaymentRecord(record);
    setPaymentAmount("");
    setPaymentError("");
    paymentIdempotencyKey.current = "";
  };

  const submitPayment = async () => {
    if (paymentInFlight.current || !selected || !paymentRecord) return;
    try {
      setPaymentError("");
      const amountCents = parseAmountCents(paymentAmount);
      if (amountCents === null || amountCents <= 0) {
        throw new Error("Enter a payment greater than LKR 0.00 with up to two decimal places.");
      }
      if (amountCents > toCents(paymentRecord.unpaidAmount)) {
        throw new Error("Payment cannot exceed this borrow record's unpaid balance.");
      }

      paymentInFlight.current = true;
      setIsSubmittingPayment(true);
      if (!paymentIdempotencyKey.current) {
        paymentIdempotencyKey.current = crypto.randomUUID();
      }
      const updated = await studentsApi.payFine(
        selected.Studentid,
        paymentRecord.borrowRecordId,
        { amount: amountCents / 100, idempotencyKey: paymentIdempotencyKey.current },
      );
      setFineRecords((current) => current.map((record) =>
        record.borrowRecordId === updated.borrowRecordId ? updated : record
      ));
      setPaymentRecord(null);
      paymentIdempotencyKey.current = "";
      setSuccess(`Payment saved for ${updated.bookTitle} (borrow record #${updated.borrowRecordId}).`);
    } catch (e) {
      setPaymentError(e instanceof Error ? e.message : "Payment failed");
    } finally {
      paymentInFlight.current = false;
      setIsSubmittingPayment(false);
    }
  };

  const fineTotals = fineRecords.reduce(
    (totals, record) => ({
      fine: totals.fine + toCents(record.totalFine),
      paid: totals.paid + toCents(record.paidAmount),
      unpaid: totals.unpaid + toCents(record.unpaidAmount),
    }),
    { fine: 0, paid: 0, unpaid: 0 },
  );

  return (
    <>
      <PageHeader
        title="Student Management"
        description="Register, search, update, delete and manage student fines."
        action={<button className="primary-button" onClick={openAdd}><Plus size={18} /> Register Student</button>}
      />

      <div className="toolbar student-toolbar">
        <div className="search-box student-search">
          <Search size={18} />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search by name, ID or course..." />
        </div>
        <select className="student-gender-filter" value={gender} onChange={(e) => filterGender(e.target.value as "ALL" | Gender)}>
          <option value="ALL">All Genders</option>
          <option value="M">Male</option>
          <option value="F">Female</option>
        </select>
      </div>

      {success && <div className="success-message" role="status">{success}</div>}

      <div className="table-card">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Student Name</th><th>Age</th><th>Gender</th><th>Course</th><th>Fine Amount</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((student) => (
                <tr key={student.Studentid}>
                  <td>#{student.Studentid}</td>
                  <td className="strong">{student.studentName}</td>
                  <td>{student.age}</td>
                  <td><span className={`gender-badge ${student.gender === "M" ? "male" : "female"}`}>{student.gender}</span></td>
                  <td>{student.Course}</td>
                  <td>
  {student.amount == null
    ? "—"
    : formatCurrency(student.amount)}
</td>
                  <td>
                    <div className="actions">
                      <button className="action-button" title="View Student" aria-label={`View ${student.studentName}`} onClick={() => openView(student.Studentid)}><Eye size={16} /></button>
                      <button className="action-button" title="Edit" onClick={() => openEdit(student)}><Pencil size={16} /></button>
                      <button className="action-button fine" title="Calculate Fine" onClick={() => openFine(student)}><DollarSign size={16} /></button>
                      <button className="action-button danger" title="Delete" onClick={() => { setSelected(student); setError(""); setModal("delete"); }}><Trash2 size={16} /></button>
                    </div>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && <tr><td colSpan={7} className="empty">No students found.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={modal === "add" || modal === "edit"} title={modal === "add" ? "Register Student" : "Update Student"} onClose={() => setModal(null)}>
        <StudentForm form={form} setForm={setForm} error={error} onSave={saveStudent} onCancel={() => setModal(null)} />
      </Modal>

      <Modal open={modal === "view"} title="Student Details" onClose={() => setModal(null)}>
        {isLoadingStudent && <div className="loading">Loading student...</div>}
        {error && <div className="error-message">{error}</div>}
        {viewingStudent && !isLoadingStudent && (
          <div className="form-grid">
            <div className="form-field"><label>Student ID</label><input value={viewingStudent.Studentid} readOnly /></div>
            <div className="form-field"><label>Student Name</label><input value={viewingStudent.studentName} readOnly /></div>
            <div className="form-field"><label>Age</label><input value={viewingStudent.age} readOnly /></div>
            <div className="form-field"><label>Gender</label><input value={viewingStudent.gender} readOnly /></div>
            <div className="form-field full"><label>Course</label><input value={viewingStudent.Course} readOnly /></div>
            <div className="form-field"><label>Fine Amount</label><input value={formatCurrency(viewingStudent.amount)} readOnly /></div>
          </div>
        )}
        <div className="modal-footer">
          <button className="secondary-button" onClick={() => setModal(null)}>Close</button>
        </div>
      </Modal>

      <Modal open={modal === "delete"} title="Confirm Delete" width="small" onClose={() => setModal(null)}>
        <p>Delete {selected?.studentName}?</p>
        {error && <div className="error-message">{error}</div>}
        <div className="modal-footer">
          <button className="secondary-button" onClick={() => setModal(null)}>Cancel</button>
          <button className="primary-button" onClick={removeStudent}>Delete Student</button>
        </div>
      </Modal>

      <Modal open={modal === "fine" && paymentRecord === null} title="Calculate Fine" width="large" onClose={() => setModal(null)}>
        <h3 className="strong">{selected?.studentName ?? ""}</h3>
        {isLoadingFine ? <div className="loading" role="status">Loading borrowing records...</div> : (
          fineError ? (
            <>
              <div className="error-message" role="alert">{fineError}</div>
              <div className="modal-footer">
                <button className="secondary-button" onClick={() => selected && openFine(selected)}>Retry</button>
                <button className="secondary-button" onClick={() => setModal(null)}>Close</button>
              </div>
            </>
          ) : fineRecords.length === 0 ? (
            <>
              <p className="empty">This student has no borrow records.</p>
              <div className="modal-footer">
                <button className="secondary-button" onClick={() => setModal(null)}>Close</button>
              </div>
            </>
          ) : (
            <>
              <div className="table-card">
                <div className="table-scroll">
                  <table className="fine-records-table">
                    <thead>
                      <tr>
                        <th>Book Title</th><th>Borrowed Quantity</th><th>Borrow Date</th><th>Due Date</th>
                        <th>Return Date</th><th>Days Since Borrowing</th><th>Outstanding Quantity</th><th>Late Days</th><th>Total Fine</th>
                        <th>Paid Amount</th><th>Unpaid Amount</th><th>Payment Status</th><th>Payment</th>
                      </tr>
                    </thead>
                    <tbody>
                      {fineRecords.map((record) => (
                        <tr key={record.borrowRecordId} className={record.unpaidAmount > 0 ? "fine-unpaid-row" : ""}>
                          <td>{record.bookTitle}<br /><span className="empty">Record #{record.borrowRecordId}</span></td>
                          <td>{record.borrowedQuantity}</td>
                          <td>{record.borrowDate}</td>
                          <td>{record.dueDate}</td>
                          <td>{record.returnDate ?? "—"}</td>
                          <td>{record.daysSinceBorrowing}</td>
                          <td>{record.outstandingQuantity}</td>
                          <td>{record.lateDays}</td>
                          <td>{formatFineCurrency(record.totalFine)}</td>
                          <td>{formatFineCurrency(record.paidAmount)}</td>
                          <td>{formatFineCurrency(record.unpaidAmount)}</td>
                          <td className="payment-status">{record.paymentStatus}</td>
                          <td>
                            <button
                              className="primary-button"
                              onClick={() => openPayment(record)}
                              disabled={record.unpaidAmount <= 0}
                            >
                              Pay
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
              <div className="fine-totals" aria-live="polite">
                <span>Total Fine: <strong>{formatFineCurrency(fineTotals.fine / 100)}</strong></span>
                <span>Total Paid: <strong>{formatFineCurrency(fineTotals.paid / 100)}</strong></span>
                <span>Total Unpaid: <strong>{formatFineCurrency(fineTotals.unpaid / 100)}</strong></span>
              </div>
              <div className="modal-footer">
                <button className="secondary-button" onClick={() => setModal(null)}>Close</button>
              </div>
            </>
          )
        )}
      </Modal>
      <Modal
        open={modal === "fine" && paymentRecord !== null}
        title="Record Payment"
        width="small"
        onClose={() => {
          if (!isSubmittingPayment) setPaymentRecord(null);
        }}
      >
        {paymentRecord && (
          <>
            <p className="payment-dialog-summary">
              {paymentRecord.bookTitle} · Borrow record #{paymentRecord.borrowRecordId}<br />
              Remaining balance: <strong>{formatFineCurrency(paymentRecord.unpaidAmount)}</strong>
            </p>
            <div className="form-field">
              <label htmlFor="fine-payment-amount">Payment Amount (LKR)</label>
              <input
                id="fine-payment-amount"
                type="number"
                min="0.01"
                step="0.01"
                value={paymentAmount}
                onChange={(event) => {
                  setPaymentAmount(event.target.value);
                  setPaymentError("");
                  paymentIdempotencyKey.current = "";
                }}
              />
            </div>
            {paymentError && <div className="error-message" role="alert">{paymentError}</div>}
            <div className="modal-footer">
              <button className="secondary-button" onClick={() => setPaymentRecord(null)} disabled={isSubmittingPayment}>Cancel</button>
              <button className="primary-button" onClick={submitPayment} disabled={isSubmittingPayment}>
                {isSubmittingPayment ? "Saving..." : "Submit Payment"}
              </button>
            </div>
          </>
        )}
      </Modal>
    </>
  );
}

function parseAmountCents(value: string): number | null {
  if (!/^\d+(?:\.\d{1,2})?$/.test(value)) return null;
  const [whole, fraction = ""] = value.split(".");
  const cents = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  return Number.isSafeInteger(cents) ? cents : null;
}

function toCents(amount: number): number {
  return Math.round((amount + Number.EPSILON) * 100);
}

function formatFineCurrency(amount: number): string {
  return `LKR ${amount.toLocaleString("en-LK", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

function StudentForm({ form, setForm, error, onSave, onCancel }: {
  form: StudentInput;
  setForm: React.Dispatch<React.SetStateAction<StudentInput>>;
  error: string;
  onSave: () => void;
  onCancel: () => void;
}) {
  return (
    <>
      <div className="form-grid">
        <div className="form-field full">
          <label>Student Name</label>
          <input value={form.studentName} onChange={(e) => setForm({ ...form, studentName: e.target.value })} />
        </div>
        <div className="form-field">
          <label>Age</label>
          <input type="number" min="1" value={form.age} onChange={(e) => setForm({ ...form, age: Number(e.target.value) })} />
        </div>
        <div className="form-field">
          <label>Gender</label>
          <select value={form.gender} onChange={(e) => setForm({ ...form, gender: e.target.value as Gender })}>
            <option value="" disabled>Select gender</option>
            <option value="M">Male (M)</option>
            <option value="F">Female (F)</option>
          </select>
        </div>
        <div className="form-field full">
          <label>Course</label>
          <input value={form.Course} onChange={(e) => setForm({ ...form, Course: e.target.value })} />
        </div>
      </div>
      {error && <div className="error-message">{error}</div>}
      <div className="modal-footer">
        <button className="secondary-button" onClick={onCancel}>Cancel</button>
        <button className="primary-button" onClick={onSave}>Save Student</button>
      </div>
    </>
  );
}
