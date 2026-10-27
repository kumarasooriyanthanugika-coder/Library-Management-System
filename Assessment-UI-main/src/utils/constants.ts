export const MAX_BORROWED_BOOKS = 5;

export const FINE_RULES = [
  { min: 0, max: 3, amount: 0 },
  { min: 4, max: 7, amount: 100 },
  { min: 8, max: 14, amount: 250 },
  { min: 15, max: 30, amount: 500 },
  { min: 31, max: Infinity, amount: 1000 },
];

export const formatCurrency = (amount: number) =>
  `Rs. ${amount?.toLocaleString("en-LK")}`;
