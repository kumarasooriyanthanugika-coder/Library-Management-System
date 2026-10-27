export type Gender = "M" | "F";

export interface Student {
  Studentid: number;
  studentName: string;
  age: number;
  gender: Gender;
  Course: string;
  amount: number;
}

export interface Book {
  BookId: number;
  bookTitle: string;
  author: string;
  category: string;
  availableQuantity: number;
}

export interface BorrowRecord {
  borrowRecordId: number;
  borrowDate: string;
  dueDate: string;
  returnDate: string | null;
  numberOfBooks: number;
  Studentid: number;
  BookId: number;
  bookTitle?: string;
}

export interface StudentInput {
  studentName: string;
  age: number;
  gender: Gender;
  Course: string;
}

export interface BookInput {
  bookTitle: string;
  author: string;
  category: string;
  availableQuantity: number;
}

export interface BorrowInput {
  Studentid: number;
  BookId: number;
  numberOfBooks: number;
  borrowDate?: string;
  returnDate?: string;
}

export interface ReturnInput {
  studentid: number;
  bookId: number;
  borrowRecordId: number;
  numberOfBooks: number;
  returnDate: string;
  idempotencyKey: string;
}

export interface ReturnFinePreview {
  borrowRecordId: number;
  studentId: number;
  studentName: string;
  bookId: number;
  bookTitle: string;
  returningQuantity: number;
  outstandingQuantity: number;
  borrowDate: string;
  dueDate: string;
  returnDate: string;
  daysSinceBorrowing: number;
  lateDays: number;
  baseBorrowingCharge: number;
  baseChargeForReturningQuantity: number;
  fineForOneBook: number;
  fineForReturningQuantity: number;
  lateFine: number;
  totalAmountDue: number;
  alreadyPaidAmount: number;
  remainingUnpaidAmount: number;
  paymentStatus: "Paid" | "Partially Paid" | "Unpaid";
}

export interface BorrowFineRecord {
  borrowRecordId: number;
  studentId: number;
  bookId: number;
  bookTitle: string;
  borrowedQuantity: number;
  outstandingQuantity: number;
  borrowDate: string;
  dueDate: string;
  returnDate: string | null;
  daysSinceBorrowing: number;
  lateDays: number;
  totalFine: number;
  paidAmount: number;
  unpaidAmount: number;
  paymentStatus: "Paid" | "Partially Paid" | "Unpaid";
  paymentHistory: FinePaymentRecord[];
}

export interface FinePaymentInput {
  amount: number;
  idempotencyKey: string;
}

export interface FinePaymentRecord {
  paymentId: number;
  amount: number;
  paidAt: string;
}

export interface LibrarySummary {
  totalStudents: number;
  totalMaleStudents: number;
  totalFemaleStudents: number;
  totalBooksCurrentlyBorrowed: number;
  studentsBorrowing0Books: number;
  studentsBorrowing1Book: number;
  studentsBorrowing2Books: number;
  studentsBorrowing3Books: number;
  studentsBorrowing4Books: number;
  studentsBorrowing5Books: number;
  totalRegisteredBooks: number;
  totalAvailableBooks: number;
  totalBorrowedBooks: number;
}
