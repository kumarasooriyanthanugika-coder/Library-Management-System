import { useEffect, useRef, useState } from "react";
import { ArrowDownToLine, ArrowUpFromLine } from "lucide-react";
import Modal from "../components/common/Modal";
import PageHeader from "../components/common/PageHeader";
import { borrowReturnApi } from "../api/management/borrowReturn";
import { studentsApi } from "../api/management/students";
import type { Book, BorrowRecord, ReturnFinePreview, Student } from "../types";
import { formatCurrency } from "../utils/constants";

const getToday = () => new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Colombo",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
}).format(new Date());

export default function BorrowReturn() {
  const [students, setStudents] = useState<Student[]>([]);
  const [books, setBooks] = useState<Book[]>([]);
  const [records, setRecords] = useState<BorrowRecord[]>([]);
  const [studentBorrowRecords, setStudentBorrowRecords] = useState<BorrowRecord[]>([]);
  const [modal, setModal] = useState<"borrow" | "return" | null>(null);
  const [studentId, setStudentId] = useState("");
  const [borrowRecordId, setBorrowRecordId] = useState("");
  const [bookId, setBookId] = useState("");
  const [quantity, setQuantity] = useState(1);
  const [borrowDate, setBorrowDate] = useState(getToday);
  const [returnDate, setReturnDate] = useState(getToday);
  const [error, setError] = useState("");
  const [isLoadingRecords, setIsLoadingRecords] = useState(false);
  const [recordLoadError, setRecordLoadError] = useState("");
  const [preview, setPreview] = useState<ReturnFinePreview | null>(null);
  const [previewError, setPreviewError] = useState("");
  const [isLoadingPreview, setIsLoadingPreview] = useState(false);
  const [isSubmittingReturn, setIsSubmittingReturn] = useState(false);
  const [isPaymentDialogOpen, setIsPaymentDialogOpen] = useState(false);
  const [paymentAmount, setPaymentAmount] = useState("");
  const [paymentError, setPaymentError] = useState("");
  const [isSavingPayment, setIsSavingPayment] = useState(false);
  const paymentIdempotencyKey = useRef("");
  const returnIdempotencyKey = useRef("");
  const previewRequestId = useRef(0);
  const recordsRequestId = useRef(0);
  const paymentInFlight = useRef(false);
  const returnInFlight = useRef(false);

  const load = async () => {
    const [studentRows, bookRows, borrowRows] = await Promise.all([
      borrowReturnApi.getStudents(),
      borrowReturnApi.getBooks(),
      borrowReturnApi.getRecords(),
    ]);
    setStudents(studentRows);
    setBooks(bookRows);
    setRecords(borrowRows);
  };

  useEffect(() => {
    load().catch((reason: unknown) => {
      setError(reason instanceof Error ? reason.message : "Could not load borrowing data.");
    });
  }, []);

  const activeRecords = records.filter((record) => record.numberOfBooks > 0);
  const studentsWithActiveLoans = students.filter((student) =>
    activeRecords.some((record) => record.Studentid === student.Studentid),
  );
  const selectedStudent = students.find((student) => student.Studentid === Number(studentId));
  const selectedBorrow = studentBorrowRecords.find(
    (record) => record.borrowRecordId === Number(borrowRecordId),
  );

  useEffect(() => {
    if (modal !== "return" || !selectedStudent || !selectedBorrow || quantity < 1 || !returnDate) {
      setPreview(null);
      setPreviewError("");
      setIsLoadingPreview(false);
      return;
    }

    const requestId = ++previewRequestId.current;
    setIsLoadingPreview(true);
    setPreviewError("");
    const request = {
      studentid: selectedStudent.Studentid,
      bookId: selectedBorrow.BookId,
      borrowRecordId: selectedBorrow.borrowRecordId,
      numberOfBooks: quantity,
      returnDate,
    };
    const timeoutId = window.setTimeout(() => {
      borrowReturnApi.previewReturn(request)
        .then((result) => {
          if (requestId === previewRequestId.current) setPreview(result);
        })
        .catch((reason: unknown) => {
          if (requestId === previewRequestId.current) {
            setPreviewError(reason instanceof Error ? reason.message : "Could not calculate return charges.");
          }
        })
        .finally(() => {
          if (requestId === previewRequestId.current) setIsLoadingPreview(false);
        });
    }, 200);
    return () => window.clearTimeout(timeoutId);
  }, [modal, selectedStudent, selectedBorrow, quantity, returnDate]);

  const loadActiveRecords = async (id: number) => {
    const requestId = ++recordsRequestId.current;
    setIsLoadingRecords(true);
    setRecordLoadError("");
    try {
      const next = await borrowReturnApi.getActiveRecordsForStudent(id);
      if (requestId === recordsRequestId.current) setStudentBorrowRecords(next);
    } catch (reason) {
      if (requestId === recordsRequestId.current) {
        setRecordLoadError(reason instanceof Error ? reason.message : "Could not load this student's active borrow records.");
      }
    } finally {
      if (requestId === recordsRequestId.current) setIsLoadingRecords(false);
    }
  };

  const selectStudent = (value: string) => {
    setStudentId(value);
    setBorrowRecordId("");
    setBookId("");
    setQuantity(1);
    setStudentBorrowRecords([]);
    setPreview(null);
    setPreviewError("");
    returnIdempotencyKey.current = "";
    if (modal === "return" && value) void loadActiveRecords(Number(value));
    else {
      recordsRequestId.current += 1;
      setIsLoadingRecords(false);
    }
  };

  const openModal = (mode: "borrow" | "return") => {
    recordsRequestId.current += 1;
    setStudentId("");
    setBorrowRecordId("");
    setBookId("");
    setStudentBorrowRecords([]);
    setQuantity(1);
    setBorrowDate(getToday());
    setReturnDate(getToday());
    setError("");
    setRecordLoadError("");
    setPreview(null);
    setPreviewError("");
    setIsLoadingRecords(false);
    setIsPaymentDialogOpen(false);
    returnIdempotencyKey.current = "";
    setModal(mode);
  };

  const openReturnForRecord = async (record: BorrowRecord) => {
    openModal("return");
    setStudentId(String(record.Studentid));
    setBorrowRecordId(String(record.borrowRecordId));
    setBookId(String(record.BookId));
    setQuantity(record.numberOfBooks);
    await loadActiveRecords(record.Studentid);
  };

  const refreshStudentFine = async (id: number) => {
    const updated = await studentsApi.getById(id);
    setStudents((current) => current.map((student) =>
      student.Studentid === updated.Studentid ? updated : student
    ));
  };

  const submitReturn = async () => {
    if (returnInFlight.current || !selectedStudent || !selectedBorrow || !preview) return;
    try {
      setError("");
      if (!Number.isInteger(quantity) || quantity <= 0) {
        throw new Error("Enter a positive whole-number return quantity.");
      }
      if (quantity > selectedBorrow.numberOfBooks) {
        throw new Error("Return quantity exceeds this borrow record's outstanding quantity.");
      }
      if (!returnIdempotencyKey.current) returnIdempotencyKey.current = crypto.randomUUID();
      returnInFlight.current = true;
      setIsSubmittingReturn(true);
      await borrowReturnApi.return({
        studentid: selectedStudent.Studentid,
        bookId: selectedBorrow.BookId,
        borrowRecordId: selectedBorrow.borrowRecordId,
        numberOfBooks: quantity,
        returnDate,
        idempotencyKey: returnIdempotencyKey.current,
      });
      await load();
      setStudentBorrowRecords(await borrowReturnApi.getActiveRecordsForStudent(selectedStudent.Studentid));
      setModal(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Book return failed.");
    } finally {
      returnInFlight.current = false;
      setIsSubmittingReturn(false);
    }
  };

  const openPaymentDialog = () => {
    setPaymentAmount(preview?.remainingUnpaidAmount.toFixed(2) ?? "");
    setPaymentError("");
    paymentIdempotencyKey.current = "";
    setIsPaymentDialogOpen(true);
  };

  const submitPayment = async () => {
    if (paymentInFlight.current || !selectedStudent || !selectedBorrow || !preview) return;
    try {
      setPaymentError("");
      const amountCents = parseAmountCents(paymentAmount);
      if (amountCents === null || amountCents <= 0) {
        throw new Error("Payment must be greater than LKR 0.00 and have no more than two decimal places.");
      }
      if (amountCents > toCents(preview.remainingUnpaidAmount)) {
        throw new Error("Payment cannot exceed this borrow record's remaining unpaid amount.");
      }
      if (!paymentIdempotencyKey.current) paymentIdempotencyKey.current = crypto.randomUUID();
      paymentInFlight.current = true;
      setIsSavingPayment(true);
      await studentsApi.payFine(
        selectedStudent.Studentid,
        selectedBorrow.borrowRecordId,
        { amount: amountCents / 100, idempotencyKey: paymentIdempotencyKey.current },
      );
      await refreshStudentFine(selectedStudent.Studentid);
      paymentIdempotencyKey.current = "";
      setIsPaymentDialogOpen(false);
      setPreview(null);
      setPreviewError("");
      previewRequestId.current += 1;
      const refreshedPreview = await borrowReturnApi.previewReturn({
        studentid: selectedStudent.Studentid,
        bookId: selectedBorrow.BookId,
        borrowRecordId: selectedBorrow.borrowRecordId,
        numberOfBooks: quantity,
        returnDate,
      });
      setPreview(refreshedPreview);
    } catch (reason) {
      setPaymentError(reason instanceof Error ? reason.message : "Payment failed.");
    } finally {
      paymentInFlight.current = false;
      setIsSavingPayment(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Borrow / Return"
        description="Manage active book borrowing and returning records."
        action={
          <div className="header-actions">
            <button className="primary-button" onClick={() => openModal("borrow")}><ArrowUpFromLine size={18} /> Borrow Book</button>
            <button className="secondary-button" onClick={() => openModal("return")}><ArrowDownToLine size={18} /> Return Book</button>
          </div>
        }
      />

      <div className="info-strip">
        <span>Maximum books per student: <strong>5</strong></span>
        <span>Active borrow records: <strong>{activeRecords.length}</strong></span>
      </div>

      <div className="table-card">
        <div className="table-scroll">
          <table>
            <thead><tr><th>Record ID</th><th>Student</th><th>Book</th><th>Borrow Date</th><th>Quantity</th><th>Action</th></tr></thead>
            <tbody>
              {activeRecords.map((record) => {
                const student = students.find((item) => item.Studentid === record.Studentid);
                const book = books.find((item) => item.BookId === record.BookId);
                return (
                  <tr key={record.borrowRecordId}>
                    <td>#{record.borrowRecordId}</td>
                    <td className="strong">{student?.studentName ?? `Student #${record.Studentid}`}</td>
                    <td>{book?.bookTitle ?? `Book #${record.BookId}`}</td>
                    <td>{record.borrowDate}</td>
                    <td>{record.numberOfBooks}</td>
                    <td><button className="secondary-button" onClick={() => void openReturnForRecord(record)}>Return</button></td>
                  </tr>
                );
              })}
              {activeRecords.length === 0 && <tr><td colSpan={6} className="empty">No active borrow records.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={modal !== null && !isPaymentDialogOpen}
        title={modal === "borrow" ? "Borrow Book" : "Return Book"}
        width={modal === "return" ? "large" : "small"}
        onClose={() => setModal(null)}
      >
        <div className="form-grid">
          <div className="form-field full">
            <label>Student</label>
            <select value={studentId} onChange={(event) => selectStudent(event.target.value)}>
              <option value="">Select student</option>
              {(modal === "return" ? studentsWithActiveLoans : students).map((student) =>
                <option key={student.Studentid} value={student.Studentid}>#{student.Studentid} - {student.studentName}</option>
              )}
            </select>
          </div>
          <div className="form-field full">
            <label>{modal === "return" ? "Borrow Record" : "Book"}</label>
            {modal === "return" ? (
              <>
                <select
                  value={borrowRecordId}
                  onChange={(event) => {
                    setBorrowRecordId(event.target.value);
                    setBookId(studentBorrowRecords.find((record) => record.borrowRecordId === Number(event.target.value))?.BookId.toString() ?? "");
                    setQuantity(1);
                    setPreview(null);
                    setPreviewError("");
                    returnIdempotencyKey.current = "";
                  }}
                  disabled={!studentId || isLoadingRecords || Boolean(recordLoadError)}
                >
                  <option value="">{isLoadingRecords ? "Loading active borrow records..." : "Select borrow record"}</option>
                  {studentBorrowRecords.map((record) => (
                    <option key={record.borrowRecordId} value={record.borrowRecordId}>
                      {record.bookTitle ?? books.find((book) => book.BookId === record.BookId)?.bookTitle ?? `Book #${record.BookId}`}
                      {" — "}{record.numberOfBooks} outstanding · borrowed {record.borrowDate} · record #{record.borrowRecordId}
                    </option>
                  ))}
                </select>
                {recordLoadError && <div className="error-message" role="alert">{recordLoadError}</div>}
                {!recordLoadError && !isLoadingRecords && studentId && studentBorrowRecords.length === 0 && (
                  <span className="empty">This student has no books to return.</span>
                )}
              </>
            ) : (
              <select value={bookId} onChange={(event) => setBookId(event.target.value)}>
                <option value="">Select book</option>
                {books.map((book) => (
                  <option key={book.BookId} value={book.BookId}>
                    #{book.BookId} - {book.bookTitle} ({book.availableQuantity} available)
                  </option>
                ))}
              </select>
            )}
          </div>
          <div className="form-field">
            <label>Number of Books</label>
            <input
              type="number"
              min="1"
              max={modal === "return" ? selectedBorrow?.numberOfBooks ?? 1 : 5}
              value={quantity}
              onChange={(event) => {
                setQuantity(event.target.value === "" ? 0 : Number(event.target.value));
                returnIdempotencyKey.current = "";
              }}
            />
          </div>
          {modal === "borrow" && (
            <div className="form-field">
              <label htmlFor="borrow-date">Borrow Date</label>
              <input id="borrow-date" type="date" value={borrowDate} onChange={(event) => setBorrowDate(event.target.value)} />
            </div>
          )}
          {modal === "return" && (
            <>
              <div className="form-field">
                <label htmlFor="return-date">Return Date</label>
                <input id="return-date" type="date" value={returnDate} onChange={(event) => {
                  setReturnDate(event.target.value);
                  returnIdempotencyKey.current = "";
                }} />
              </div>
              <div className="form-field full">
                <label>Fine Details</label>
                {!selectedBorrow ? <span className="empty">Select a borrow record to preview return charges.</span> : (
                  <div className="table-card">
                    {isLoadingPreview ? <div className="loading" role="status">Calculating return charges...</div> : previewError ? (
                      <div className="error-message" role="alert">{previewError}</div>
                    ) : preview && (
                      <div className="table-scroll">
                        <table className="fine-records-table">
                          <thead><tr><th>Student Name</th><th>Book Title</th><th>Number Returning</th><th>Borrow Date</th><th>Due Date</th><th>Return Date</th><th>Days Since Borrowing</th><th>Late Days</th><th>Fine for One Book</th><th>Fine for Returning Quantity</th><th>Base Borrowing Charge</th><th>Total Amount Due</th><th>Already Paid</th><th>Remaining Unpaid</th></tr></thead>
                          <tbody><tr>
                            <td>{preview.studentName}</td>
                            <td>{preview.bookTitle}<br /><span className="empty">Record #{preview.borrowRecordId}</span></td>
                            <td>{preview.returningQuantity} ({preview.outstandingQuantity} currently outstanding)</td>
                            <td>{preview.borrowDate}</td>
                            <td>{preview.dueDate}</td>
                            <td>{preview.returnDate}</td>
                            <td>{preview.daysSinceBorrowing}</td>
                            <td>{preview.lateDays}</td>
                            <td>{formatCurrency(preview.fineForOneBook)}</td>
                            <td>{formatCurrency(preview.fineForReturningQuantity)}</td>
                            <td>{formatCurrency(preview.baseBorrowingCharge)}<br /><span className="empty">Returning copies: {formatCurrency(preview.baseChargeForReturningQuantity)}</span></td>
                            <td>{formatCurrency(preview.totalAmountDue)}</td>
                            <td>{formatCurrency(preview.alreadyPaidAmount)}</td>
                            <td>{formatCurrency(preview.remainingUnpaidAmount)}</td>
                          </tr></tbody>
                        </table>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
        {error && <div className="error-message" role="alert">{error}</div>}
        {modal === "return" && preview && !isLoadingPreview && !previewError && (
          <div className="modal-footer">
            <button className="secondary-button" onClick={openPaymentDialog} disabled={preview.remainingUnpaidAmount <= 0}>
              Paid
            </button>
            <button className="secondary-button" onClick={() => void submitReturn()} disabled={isSubmittingReturn || isLoadingRecords}>
              {isSubmittingReturn ? "Returning..." : "Unpaid"}
            </button>
            <button className="primary-button" onClick={() => void submitReturn()} disabled={isSubmittingReturn || isLoadingRecords}>
              {isSubmittingReturn ? "Returning..." : "Return Book"}
            </button>
          </div>
        )}
        {modal !== "return" && (
          <div className="modal-footer">
            <button className="secondary-button" onClick={() => setModal(null)}>Cancel</button>
            <button
              className="primary-button"
              onClick={() => void submitBorrow()}
              disabled={!studentId || !bookId || !Number.isInteger(quantity) || quantity <= 0}
            >
              Borrow Book
            </button>
          </div>
        )}
        {modal === "return" && !selectedBorrow && (
          <div className="modal-footer">
            <button className="secondary-button" onClick={() => setModal(null)}>Cancel</button>
          </div>
        )}
      </Modal>

      <Modal
        open={isPaymentDialogOpen}
        title="Record Payment"
        width="small"
        onClose={() => {
          if (!isSavingPayment) setIsPaymentDialogOpen(false);
        }}
      >
        <p className="payment-dialog-summary">
          {preview?.bookTitle} · Borrow record #{preview?.borrowRecordId}<br />
          Remaining balance: <strong>{preview ? formatCurrency(preview.remainingUnpaidAmount) : "—"}</strong>
        </p>
        <div className="form-field">
          <label htmlFor="return-payment-amount">Payment Amount (LKR)</label>
          <input
            id="return-payment-amount"
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
          <button className="secondary-button" onClick={() => setIsPaymentDialogOpen(false)} disabled={isSavingPayment}>Cancel</button>
          <button className="primary-button" onClick={() => void submitPayment()} disabled={isSavingPayment}>
            {isSavingPayment ? "Saving..." : "Submit Payment"}
          </button>
        </div>
      </Modal>
    </>
  );

  async function submitBorrow() {
    try {
      setError("");
      if (!studentId || !bookId) throw new Error("Student and book are required.");
      if (!Number.isInteger(quantity) || quantity <= 0) throw new Error("Enter a valid whole number of books.");
      await borrowReturnApi.borrow({
        Studentid: Number(studentId),
        BookId: Number(bookId),
        numberOfBooks: quantity,
        borrowDate,
      });
      await load();
      setModal(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Book borrowing failed.");
    }
  }
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
