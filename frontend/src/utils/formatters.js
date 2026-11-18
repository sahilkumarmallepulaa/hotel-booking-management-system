/**
 * Formats a numeric price into Indian Rupees (INR) using standard Indian numbering system.
 *
 * @param {number} amount - Numeric monetary value
 * @returns {string} Formatted INR currency string (e.g. ₹150 or ₹150.50)
 */
export const formatINR = (amount) => {
  if (typeof amount !== 'number' || isNaN(amount)) return '';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: amount % 1 === 0 ? 0 : 2,
  }).format(amount);
};
