import { describe, expect, it } from "vitest";
import { checkPasswordStrength, formatMoney, formatSignedMoney, maskAccountNumber } from "./format";

describe("formatMoney", () => {
  it("formats a positive number as USD currency", () => {
    expect(formatMoney(1234.5)).toBe("$1,234.50");
  });

  it("formats zero", () => {
    expect(formatMoney(0)).toBe("$0.00");
  });

  it("rounds to two decimal places", () => {
    expect(formatMoney(9.999)).toBe("$10.00");
  });
});

describe("formatSignedMoney", () => {
  it("prefixes negative amounts with a minus sign", () => {
    expect(formatSignedMoney(50, true)).toBe("-$50.00");
  });

  it("prefixes positive amounts with a plus sign", () => {
    expect(formatSignedMoney(50, false)).toBe("+$50.00");
  });
});

describe("maskAccountNumber", () => {
  it("masks all but the last 4 digits", () => {
    expect(maskAccountNumber("1234567890")).toBe("•••• 7890");
  });

  it("returns short numbers unchanged", () => {
    expect(maskAccountNumber("123")).toBe("123");
  });

  it("handles empty/undefined input safely", () => {
    expect(maskAccountNumber("")).toBe("");
  });
});

describe("checkPasswordStrength", () => {
  it("flags a strong password as valid", () => {
    const result = checkPasswordStrength("Str0ng!Pass");
    expect(result.valid).toBe(true);
    expect(result.minLength).toBe(true);
    expect(result.hasUppercase).toBe(true);
    expect(result.hasNumber).toBe(true);
    expect(result.hasSpecial).toBe(true);
  });

  it("rejects a password missing an uppercase letter", () => {
    const result = checkPasswordStrength("weak1234!");
    expect(result.valid).toBe(false);
    expect(result.hasUppercase).toBe(false);
  });

  it("rejects a password missing a special character", () => {
    const result = checkPasswordStrength("Weak1234");
    expect(result.valid).toBe(false);
    expect(result.hasSpecial).toBe(false);
  });

  it("rejects a password shorter than 8 characters", () => {
    const result = checkPasswordStrength("Sh0rt!");
    expect(result.valid).toBe(false);
    expect(result.minLength).toBe(false);
  });

  it("rejects a password missing a number", () => {
    const result = checkPasswordStrength("NoNumber!");
    expect(result.valid).toBe(false);
    expect(result.hasNumber).toBe(false);
  });
});
