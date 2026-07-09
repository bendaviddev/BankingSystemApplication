# E2E Test Plan — Banking System Application

**Manual and automated test scenarios for the full-stack banking application.**

This plan covers user registration, authentication, account operations, transfers, analytics, admin functions, and edge cases. Tests are marked **[MANUAL]** or **[AUTOMATED]** (with test file reference).

---

## Test Environment Setup

**Prerequisite:** Application running locally.

```bash
# Terminal 1: Backend
./run.sh
# Wait for: "Started BankingSystemApiApplication in X seconds"

# Terminal 2: Frontend
./run-frontend.sh
# Open http://localhost:5173 in browser
```

**Demo Accounts (pre-seeded):**
- `demo` / `Demo123!` — User role, $50 CHECKING + $736.46 SAVINGS
- `sofia` / `Sofia123!` — User role, $1000 CHECKING
- `admin` / `Admin123!` — Admin role

---

## Test Scenarios

### 1. Registration & Login Flow [MANUAL + AUTOMATED]

#### 1.1 Register New User — Valid Input
- **Steps:**
  1. Click "Register" link on landing page
  2. Fill form: username=`testuser1`, password=`Test1234!`, firstName=`John`, lastName=`Doe`
  3. Click "Sign Up"
- **Expected:** User created, redirected to login page with success message
- **Test:** `AuthFlowTest::testRegisterSuccess`

#### 1.2 Register — Username Already Taken
- **Steps:**
  1. Try to register with username=`demo` (already exists)
  2. Fill rest of form normally
- **Expected:** Error: "Username already taken" (specific message, not generic error)
- **Test:** `AuthFlowTest::testRegisterDuplicateUsername`

#### 1.3 Register — Weak Password
- **Steps:**
  1. Try to register with password=`weak` (too short, no uppercase/number/special)
- **Expected:** Validation error before submission (client-side); if submitted, server rejects with specific rules
- **Test:** `PasswordUtilTest::testValidation*`

#### 1.4 Login — Valid Credentials
- **Steps:**
  1. Navigate to login page
  2. Enter username=`demo`, password=`Demo123!`
  3. Click "Log In"
- **Expected:** Token received; redirected to dashboard; token visible in sessionStorage (DevTools)
- **Test:** `AuthFlowTest::testLoginSuccess`

#### 1.5 Login — Invalid Password
- **Steps:**
  1. Enter username=`demo`, password=`wrong`
- **Expected:** Error: "Username or password is incorrect"
- **Test:** `AuthFlowTest::testLoginInvalid`

#### 1.6 Login — Non-Existent User
- **Steps:**
  1. Enter username=`nonexistent`, password=`Pass123!`
- **Expected:** Same error as wrong password (no user enumeration)

#### 1.7 Logout & Session Revocation
- **Steps:**
  1. Login as `demo`
  2. Click user menu → "Logout"
  3. Confirm token removed from sessionStorage
  4. Try to access `/dashboard` directly (via URL bar)
- **Expected:** Redirected to login page; GET /api/accounts returns 401 (invalid token)
- **Test:** `AuthFlowTest::testLogoutRevokesSession`

---

### 2. Account Lifecycle [MANUAL + AUTOMATED]

#### 2.1 View Accounts List
- **Steps:**
  1. Login as `demo`
  2. Navigate to "Accounts" page
- **Expected:** Two accounts shown (CHECKING $50.00, SAVINGS $736.46) with status, type, account number, balance
- **Test:** `AccountServiceTest::testOpenAccount*`

#### 2.2 Open New Account
- **Steps:**
  1. On Accounts page, click "Open Account"
  2. Select type=`CHECKING`, leave balance empty (defaults to $0)
  3. Click "Create"
- **Expected:** New account created; appears in list with status=ACTIVE, balance=$0.00
- **Test:** Backend: `AccountServiceTest::testOpenAccount*`; Frontend: routing guard verified

#### 2.3 View Account Detail
- **Steps:**
  1. Click on one of the accounts
  2. View account detail page (balance, type, status, account number, statement)
- **Expected:** All fields populated; account number is unique (e.g., ACCT-XXXXXXXXXXXX)
- **Test:** `AccountDetailPage` routing verified

#### 2.4 Freeze Account (Admin Only)
- **Steps:**
  1. Login as `admin`
  2. Navigate to "Admin" → "Accounts"
  3. Find demo's account, click "Freeze"
  4. Confirm status changed to FROZEN
  5. Logout, login as `demo`, try to deposit
- **Expected:** Admin action succeeds; demo user sees status=FROZEN; transactions fail with "Account is frozen"
- **Test:** Manual (admin UI not fully automated); backend: `BankingApiSecurityTest::testFrozenAccountRejectsTransactions`

#### 2.5 Unfreeze Account (Admin Only)
- **Steps:**
  1. Login as admin, go to Admin → Accounts
  2. Find frozen account, click "Unfreeze"
- **Expected:** Status changes back to ACTIVE; user can deposit/withdraw again

---

### 3. Transactions — Happy Path [MANUAL + AUTOMATED]

#### 3.1 Deposit to Own Account
- **Steps:**
  1. Login as `demo`
  2. Go to account detail or dashboard
  3. Click "Deposit", enter amount=$50.00, category=INCOME
  4. Click "Submit"
- **Expected:** Transaction created with status=COMPLETED, running balance updated, alert triggered
- **Verify in API:** GET /api/accounts/1 shows new balance
- **Test:** `AccountServiceTest::testDepositAndWithdrawHappyPathTracksRunningBalance`

#### 3.2 Withdraw from Own Account
- **Steps:**
  1. From account with balance ≥ $50
  2. Click "Withdraw", enter amount=$30.00, category=GROCERIES
  3. Click "Submit"
- **Expected:** Transaction created with status=COMPLETED, balance decremented
- **Test:** `AccountServiceTest::testDepositAndWithdrawHappyPathTracksRunningBalance`

#### 3.3 Withdraw — Insufficient Funds
- **Steps:**
  1. Try to withdraw $10,000 from an account with $50 balance
- **Expected:** 
  - Frontend: validation warning
  - Server: Transaction created with status=FAILED, balance unchanged, alert (large/failed) triggered
  - User sees error: "Insufficient funds"
- **Test:** `AccountServiceTest::testWithdrawInsufficientFundsCreatesFailedTransaction`

#### 3.4 Internal Transfer (Same User, Different Accounts)
- **Steps:**
  1. Login as `demo` (has CHECKING $50, SAVINGS $736.46)
  2. Go to "Transfer" page
  3. From=CHECKING, To=SAVINGS, Amount=$25
  4. Click "Review" → "Confirm"
- **Expected:** Two transactions created (WITHDRAWAL on CHECKING, DEPOSIT on SAVINGS), both COMPLETED, running balances updated
- **Test:** `AccountServiceTest::testInternalTransfer*` (backend); manual frontend flow

#### 3.5 External Transfer — Happy Path
- **Steps:**
  1. Login as `demo`
  2. Click "Transfer" → "External"
  3. Enter recipient account number (e.g., ACCT-... from `sofia`'s account)
  4. Amount=$50
  5. Review and confirm
- **Expected:** Two transactions created (WITHDRAWAL from demo, DEPOSIT to sofia), both COMPLETED; running balances updated
- **Test:** `ExternalTransferTest::testExternalTransferHappyPath`

#### 3.6 External Transfer — Invalid Recipient
- **Steps:**
  1. Try to enter a non-existent account number
- **Expected:** Lookup fails; error "Account not found"
- **Test:** `ExternalTransferTest::testExternalTransferRecipientNotFound`

#### 3.7 External Transfer — Same Account (Reject)
- **Steps:**
  1. Try to transfer to your own account number
- **Expected:** Validation error: "Cannot transfer to own account"
- **Test:** `ExternalTransferTest::testExternalTransferToOwnAccountRejected`

#### 3.8 External Transfer — Insufficient Funds
- **Steps:**
  1. Try to transfer $10,000 from an account with $100
- **Expected:** WITHDRAWAL transaction created with status=FAILED; DEPOSIT not created; alert fired
- **Test:** `ExternalTransferTest::testExternalTransferInsufficientFunds`

---

### 4. Transaction Querying & Export [MANUAL + AUTOMATED]

#### 4.1 Transaction List — Default (Paginated)
- **Steps:**
  1. Login as `demo`
  2. Go to "Transactions" page
- **Expected:** 
  - First 10 transactions shown (page=0, size=10 default)
  - Each row shows: reference, type, amount, status, balance, date, category
  - Pagination controls at bottom (page X of Y)
- **Test:** `TransactionQueryTest::testTransactionPaginationDefaultSize`

#### 4.2 Filter by Date Range
- **Steps:**
  1. Set "From" date = 2026-06-01, "To" date = 2026-06-30
  2. Click "Filter"
- **Expected:** List shows only transactions in June; page count updates
- **Test:** `TransactionQueryTest::testTransactionFilterByDateRange`

#### 4.3 Filter by Category
- **Steps:**
  1. Select category=GROCERIES
  2. Click "Filter"
- **Expected:** Only transactions with category=GROCERIES shown
- **Test:** `TransactionQueryTest::testTransactionFilterByCategory`

#### 4.4 Filter by Status
- **Steps:**
  1. Select status=FAILED
- **Expected:** Only FAILED transactions shown (if any)
- **Test:** Manual (status filter in UI)

#### 4.5 Combine Filters
- **Steps:**
  1. Filter by date range + category + status
- **Expected:** Intersection of all filters applied; results accurate

#### 4.6 CSV Export — All Transactions
- **Steps:**
  1. No filters applied (or empty page)
  2. Click "Export to CSV"
- **Expected:** 
  - Browser downloads file: `transactions_yyyymmdd.csv`
  - CSV has headers: transactionId, reference, accountId, type, status, amount, category, date
  - BOM present (Excel compatibility)
  - All rows present
- **Test:** `TransactionQueryTest::testTransactionExportCsv`

#### 4.7 CSV Export — Filtered Results
- **Steps:**
  1. Apply filters (date range, category, status)
  2. Click "Export to CSV"
- **Expected:** 
  - CSV contains only filtered transactions
  - Counts match UI pagination summary
- **Test:** Manual (backend tested; frontend UI tested)

#### 4.8 Pagination — Next/Previous
- **Steps:**
  1. On page 0 (first page), click "Next"
  2. Verify page changes to 1
  3. Click "Previous"
- **Expected:** Navigation works; data updates correctly
- **Test:** `Pagination.test.tsx`

---

### 5. Analytics & Dashboard [MANUAL]

#### 5.1 Dashboard Overview
- **Steps:**
  1. Login as `demo`
  2. Go to "Dashboard" page
- **Expected:** 
  - Total balance displayed (sum of all accounts)
  - Cards showing: total in (30d), total out (30d), largest transaction
  - Charts: monthly flow (inflow/outflow), spending by category, balance history

#### 5.2 Monthly Flow Chart
- **Steps:**
  1. View "Monthly Flow" chart on dashboard
- **Expected:** 
  - X-axis: months (e.g., Feb 2026, Mar 2026, ...)
  - Y-axis: amount
  - Blue bars (inflow), red bars (outflow)
  - Correct values (sum of deposits/withdrawals per month)

#### 5.3 Spending by Category Chart
- **Steps:**
  1. View "Spending by Category" pie/bar chart
- **Expected:** 
  - Categories listed with amounts
  - Demo user shows: RENT, SHOPPING, GROCERIES, etc.
  - Totals match transaction list filtered by category

#### 5.4 Balance History Chart
- **Steps:**
  1. View "Balance History" line chart on analytics page
- **Expected:** 
  - Line shows daily balance
  - X-axis: dates (last 30 days)
  - Y-axis: balance
  - Matches running balance in transaction detail

#### 5.5 Largest Transactions Table
- **Steps:**
  1. View "Largest Transactions" on dashboard
- **Expected:** 
  - Shows 5-10 largest by amount
  - Sorted descending
  - Types include DEPOSIT, WITHDRAWAL, TRANSFER

---

### 6. Alerts & Notifications [MANUAL + AUTOMATED]

#### 6.1 Large Transaction Alert
- **Steps:**
  1. Login as `demo`
  2. Deposit $3000 (exceeds threshold)
  3. Go to "Alerts" page
- **Expected:** 
  - Alert created: "Large transaction of $3,000.00 recorded."
  - Severity: INFO
  - Marked as unread
- **Test:** Backend: alerts created for large txns; Frontend: unread count increments

#### 6.2 Alert List & Read Status
- **Steps:**
  1. On Alerts page
  2. Verify unread count (badge on menu icon)
  3. Click alert row to view detail
  4. Click "Mark as Read" or use "Read All" button
- **Expected:** 
  - Alert detail shown
  - After marking read, unread badge decreases
  - Read alerts appear with normal styling (not bold)

#### 6.3 Insufficient Funds Alert
- **Steps:**
  1. Try to withdraw $5000 from account with $50
  2. Transaction fails (status=FAILED)
  3. Check "Alerts" page
- **Expected:** Alert triggered (e.g., "Failed transaction: Insufficient funds")

#### 6.4 Failed Transfer Alert
- **Steps:**
  1. Attempt external transfer with insufficient funds
  2. Check alerts
- **Expected:** Alert shows transfer failed and reason

---

### 7. User Profile & Settings [MANUAL]

#### 7.1 View Profile
- **Steps:**
  1. Login as `demo`
  2. Click user menu → "Profile"
- **Expected:** 
  - Username: demo
  - Name: Demo (first + last)
  - Role: USER
  - Email: (if present) demo@example.com
  - Member since: (creation date)

#### 7.2 Change Password
- **Steps:**
  1. On Profile page, click "Change Password"
  2. Enter old password: `Demo123!`
  3. New password: `NewPass123!`
  4. Confirm new password: `NewPass123!`
  5. Click "Update"
- **Expected:** 
  - Success message: "Password updated"
  - All existing sessions revoked (user must re-login)
  - Cannot login with old password
  - Can login with new password

#### 7.3 Change Password — Wrong Old Password
- **Steps:**
  1. Enter old password: `wrong`
- **Expected:** Error: "Current password is incorrect"

#### 7.4 Change Password — Weak New Password
- **Steps:**
  1. Try to set new password to `weak`
- **Expected:** Validation error: "Password must be 8+ characters with uppercase, number, special char"

#### 7.5 Logout (Revisited)
- **Steps:**
  1. On Profile page, click "Logout"
- **Expected:** 
  - Redirected to landing page
  - Token removed from sessionStorage
  - Attempt to navigate to /dashboard redirected to /login

---

### 8. Admin Console [MANUAL]

#### 8.1 Admin Login
- **Steps:**
  1. Navigate to login
  2. Enter username=`admin`, password=`Admin123!`
  3. Click "Log In"
- **Expected:** 
  - Redirected to dashboard
  - Admin menu appears (link to "Admin" console)

#### 8.2 System Statistics
- **Steps:**
  1. Click "Admin" menu → "System Stats"
- **Expected:** 
  - Total Users: 3 (demo, sofia, admin)
  - Total Accounts: 3+ (demo's + sofia's + new ones)
  - Total Transactions: 85+ (seeded data + any new ones)
  - Total Volume: (sum of all transaction amounts)
  - Failed Transactions (24h): (count of FAILED txns in last 24h)

#### 8.3 User Management
- **Steps:**
  1. Go to Admin → "Users"
- **Expected:** 
  - List of all users (demo, sofia, admin, any new registrants)
  - Columns: username, name, role, joined date
  - Can click user for detail

#### 8.4 Account Management
- **Steps:**
  1. Go to Admin → "Accounts"
- **Expected:** 
  - List of all accounts across all users
  - Status control: can freeze/unfreeze
  - Shows owner, balance, type, status

#### 8.5 Activity/Audit Log
- **Steps:**
  1. Go to Admin → "Audit Log"
- **Expected:** 
  - Chronological list of events (logins, account opens, transfers, etc.)
  - Columns: timestamp, user, action, details, result
  - Pagination if many entries
  - Demo user's actions visible

#### 8.6 Non-Admin Cannot Access Admin
- **Steps:**
  1. Login as `demo`
  2. Try to navigate to `/admin` directly
- **Expected:** 
  - 403 Forbidden error or redirect to home
  - No admin menu appears
- **Test:** `AdminRoute` guard in frontend; backend `/api/admin/*` rejects non-admin

---

### 9. Security & Edge Cases [MANUAL + AUTOMATED]

#### 9.1 Invalid Token Rejected
- **Steps:**
  1. Open DevTools (F12) → Application → sessionStorage
  2. Clear token or modify it (e.g., replace one character)
  3. Refresh page or make API call
- **Expected:** 
  - 401 Unauthorized
  - Redirected to login
  - Clear error message: "Authentication required" or "Invalid session"
- **Test:** Backend: `BankingApiSecurityTest::testInvalidTokenRejected`

#### 9.2 Token Expiry (24h)
- **Steps:**
  1. (Manual verification via code inspection or mock clock)
  2. Verify token TTL is 24 hours in SessionService
- **Expected:** 
  - Token created with expiry = now + 24h
  - After expiry, requests return 401
- **Test:** Backend: `AuthFlowTest::testTokenExpiry` (or inspection of SessionService.EXPIRY constant)

#### 9.3 Cannot Access Other User's Account
- **Steps:**
  1. Login as `demo`
  2. Open DevTools → Network tab
  3. Make manual request: `GET /api/accounts/999` (sofia's account ID)
- **Expected:** 
  - 403 Forbidden or 404 Not Found (no info leakage)
  - Message: "Account not found" or "Access denied"
- **Test:** `BankingApiSecurityTest::testOwnershipCheckEnforced`

#### 9.4 Cannot View Other User's Transactions
- **Steps:**
  1. Login as `demo`
  2. Query: `GET /api/transactions?page=0&size=100` should return only demo's txns
  3. Logout, login as sofia
  4. Query same endpoint; should return only sofia's txns
- **Expected:** 
  - Results filtered to current user only
  - No cross-user data leakage
- **Test:** `TransactionQueryTest::testTransactionQueryFilters`

#### 9.5 Rate Limiting on Login
- **Steps:**
  1. Make 11+ rapid login attempts with wrong password
- **Expected:** 
  - 10th request succeeds (or fails with wrong credentials)
  - 11th request returns 429 Too Many Requests
  - Wait 60 seconds or restart server; can try again
- **Test:** Manual (tested during dev; in-memory rate limiter)

#### 9.6 Rate Limiting on Register
- **Steps:**
  1. Register 11+ new users rapidly
- **Expected:** 11th request returns 429

#### 9.7 CORS Preflight
- **Steps:**
  1. (DevTools → Network)
  2. Observe OPTIONS request before POST /api/auth/login
- **Expected:** 
  - OPTIONS request returns 200 with Access-Control-Allow-* headers
  - Subsequent POST succeeds (CORS allowed)

#### 9.8 No Stack Trace in Error Responses
- **Steps:**
  1. Cause an error (e.g., POST invalid JSON body)
  2. Check response body
- **Expected:** 
  - Error message only (e.g., "Invalid request")
  - No stack trace, no internal exception type leaks
- **Test:** Backend: `server.error.include-stacktrace=never` verified

#### 9.9 Password Hashing Verified
- **Steps:**
  1. (Code inspection) Check BCrypt usage in PasswordUtil and AuthService
  2. Verify plain passwords never logged or returned
- **Expected:** 
  - Passwords hashed with BCrypt strength ≥ 11
  - Never sent in API responses
- **Test:** `PasswordUtilTest` checks BCrypt hashing

---

### 10. Responsive Design & Dark Mode [MANUAL]

#### 10.1 Mobile Viewport (375x812)
- **Steps:**
  1. Open app in browser
  2. Press F12 (DevTools)
  3. Click device toolbar icon
  4. Select "iPhone 12" or set width=375
- **Expected:** 
  - Layout reflows to single column
  - Navigation menu collapses to hamburger
  - Buttons, inputs, tables remain readable and tappable
  - No horizontal scroll
  - No content cutoff

#### 10.2 Tablet Viewport (768x1024)
- **Steps:**
  1. Set width=768 in DevTools
- **Expected:** 
  - 2-column layout where applicable
  - Sidebar navigation visible (not collapsed)
  - Forms and tables properly formatted

#### 10.3 Desktop Viewport (1280x800+)
- **Steps:**
  1. Set width ≥ 1280
- **Expected:** 
  - Full layout: sidebar + main content
  - Charts display properly (recharts responsive)
  - All features accessible

#### 10.4 Dark Mode Toggle
- **Steps:**
  1. On any page, find theme toggle (usually top-right corner or in settings)
  2. Click to toggle light ↔ dark
- **Expected:** 
  - All colors invert (background dark, text light)
  - Charts remain readable
  - Buttons and links contrast well
  - Preference persisted (reload page, still dark)
  - Respects system preference on first load

#### 10.5 Dark Mode at Chart
- **Steps:**
  1. Go to Dashboard or Analytics
  2. Toggle dark mode
- **Expected:** 
  - Recharts bars/lines have sufficient contrast
  - Legend readable
  - Colors match overall theme

---

### 11. Browser Console (No Errors) [MANUAL]

#### 11.1 No Console Errors on Landing Page
- **Steps:**
  1. Open landing page
  2. Press F12 → Console tab
- **Expected:** 
  - No error messages (red)
  - No warnings from React or libraries (orange)
  - (Yellow warnings OK if from third-party libs like Recharts)

#### 11.2 No Errors After Login & Navigation
- **Steps:**
  1. Login as demo
  2. Navigate through all pages (Accounts, Transfers, Transactions, Analytics, Alerts, Profile, Admin)
  3. Refresh each page
  4. Check Console throughout
- **Expected:** No error messages

#### 11.3 No Errors During Transactions
- **Steps:**
  1. Perform deposit, withdrawal, transfer
  2. Check Console
- **Expected:** No errors; API calls logged if debugging enabled

---

## Test Execution Checklist

| Test ID | Scenario | Status | Notes |
|---------|----------|--------|-------|
| 1.1 | Register New User — Valid Input | [ ] | |
| 1.2 | Register — Username Already Taken | [ ] | |
| 1.3 | Register — Weak Password | [ ] | |
| 1.4 | Login — Valid Credentials | [ ] | |
| 1.5 | Login — Invalid Password | [ ] | |
| 1.6 | Login — Non-Existent User | [ ] | |
| 1.7 | Logout & Session Revocation | [ ] | |
| 2.1 | View Accounts List | [ ] | |
| 2.2 | Open New Account | [ ] | |
| 2.3 | View Account Detail | [ ] | |
| 2.4 | Freeze Account (Admin Only) | [ ] | |
| 2.5 | Unfreeze Account (Admin Only) | [ ] | |
| 3.1 | Deposit to Own Account | [ ] | |
| 3.2 | Withdraw from Own Account | [ ] | |
| 3.3 | Withdraw — Insufficient Funds | [ ] | |
| 3.4 | Internal Transfer | [ ] | |
| 3.5 | External Transfer — Happy Path | [ ] | |
| 3.6 | External Transfer — Invalid Recipient | [ ] | |
| 3.7 | External Transfer — Same Account | [ ] | |
| 3.8 | External Transfer — Insufficient Funds | [ ] | |
| 4.1 | Transaction List — Default | [ ] | |
| 4.2 | Filter by Date Range | [ ] | |
| 4.3 | Filter by Category | [ ] | |
| 4.4 | Filter by Status | [ ] | |
| 4.5 | Combine Filters | [ ] | |
| 4.6 | CSV Export — All Transactions | [ ] | |
| 4.7 | CSV Export — Filtered Results | [ ] | |
| 4.8 | Pagination — Next/Previous | [ ] | |
| 5.1 | Dashboard Overview | [ ] | |
| 5.2 | Monthly Flow Chart | [ ] | |
| 5.3 | Spending by Category Chart | [ ] | |
| 5.4 | Balance History Chart | [ ] | |
| 5.5 | Largest Transactions Table | [ ] | |
| 6.1 | Large Transaction Alert | [ ] | |
| 6.2 | Alert List & Read Status | [ ] | |
| 6.3 | Insufficient Funds Alert | [ ] | |
| 6.4 | Failed Transfer Alert | [ ] | |
| 7.1 | View Profile | [ ] | |
| 7.2 | Change Password | [ ] | |
| 7.3 | Change Password — Wrong Old | [ ] | |
| 7.4 | Change Password — Weak New | [ ] | |
| 7.5 | Logout (Revisited) | [ ] | |
| 8.1 | Admin Login | [ ] | |
| 8.2 | System Statistics | [ ] | |
| 8.3 | User Management | [ ] | |
| 8.4 | Account Management | [ ] | |
| 8.5 | Activity/Audit Log | [ ] | |
| 8.6 | Non-Admin Cannot Access Admin | [ ] | |
| 9.1 | Invalid Token Rejected | [ ] | |
| 9.2 | Token Expiry (24h) | [ ] | Code inspection |
| 9.3 | Cannot Access Other User's Account | [ ] | |
| 9.4 | Cannot View Other User's Transactions | [ ] | |
| 9.5 | Rate Limiting on Login | [ ] | Manual |
| 9.6 | Rate Limiting on Register | [ ] | Manual |
| 9.7 | CORS Preflight | [ ] | DevTools |
| 9.8 | No Stack Trace in Errors | [ ] | |
| 9.9 | Password Hashing Verified | [ ] | Code inspection |
| 10.1 | Mobile Viewport (375x812) | [ ] | DevTools device emulation |
| 10.2 | Tablet Viewport (768x1024) | [ ] | DevTools device emulation |
| 10.3 | Desktop Viewport (1280x800+) | [ ] | |
| 10.4 | Dark Mode Toggle | [ ] | |
| 10.5 | Dark Mode on Charts | [ ] | |
| 11.1 | No Console Errors — Landing | [ ] | DevTools Console |
| 11.2 | No Errors — Navigation | [ ] | DevTools Console |
| 11.3 | No Errors — Transactions | [ ] | DevTools Console |

---

## Test Summary

- **Total Scenarios:** 51
- **Manual:** 40+
- **Automated (Backend):** 27 tests (AccountService, Transaction, Auth, Security)
- **Automated (Frontend):** 26 tests (Components, Routing, Formatting)
- **Coverage:** Authentication, authorization, transactions (happy & edge), analytics, alerts, admin, security, responsive design, accessibility

**To run automated tests:**
```bash
cd BankingSystemAPI && mvn test
cd banking-frontend && npm test
```

---

**Last Updated:** 2026-07-08
