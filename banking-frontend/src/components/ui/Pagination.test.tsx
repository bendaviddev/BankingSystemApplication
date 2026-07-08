import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Pagination } from "./Pagination";

describe("Pagination", () => {
  it("renders nothing when there is one page or fewer", () => {
    const { container } = render(<Pagination page={0} totalPages={1} onChange={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("disables Prev on the first page and Next on the last page", () => {
    render(<Pagination page={0} totalPages={3} onChange={vi.fn()} />);
    expect(screen.getByRole("button", { name: /previous page/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /next page/i })).not.toBeDisabled();
  });

  it("shows the current page and total", () => {
    render(<Pagination page={1} totalPages={4} onChange={vi.fn()} />);
    expect(screen.getByText("Page 2 of 4")).toBeInTheDocument();
  });

  it("calls onChange with page - 1 when Prev is clicked", async () => {
    const onChange = vi.fn();
    render(<Pagination page={2} totalPages={4} onChange={onChange} />);
    await userEvent.click(screen.getByRole("button", { name: /previous page/i }));
    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("calls onChange with page + 1 when Next is clicked", async () => {
    const onChange = vi.fn();
    render(<Pagination page={0} totalPages={4} onChange={onChange} />);
    await userEvent.click(screen.getByRole("button", { name: /next page/i }));
    expect(onChange).toHaveBeenCalledWith(1);
  });
});
