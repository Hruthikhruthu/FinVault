export function money(value: number | string | undefined) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0
  }).format(amount);
}

export function percent(value: number | string | undefined) {
  return `${Number(value || 0).toFixed(1)}%`;
}

export function todayMonthKey() {
  return new Date().toISOString().slice(0, 7);
}

export function todayDateTimeLocal() {
  return new Date().toISOString().slice(0, 16);
}

export function futureDate(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}
