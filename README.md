# 💸 ExpenseTracker — Android App

Automatically tracks and categorizes daily expenses by reading bank debit SMS messages.
Built with **Kotlin + Jetpack Compose**, fully offline, MVVM architecture.

---

## 📁 Project Structure

```
ExpenseTracker/
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/com/expensetracker/
│       ├── ExpenseTrackerApp.kt          # Hilt Application class
│       ├── AppModule.kt                  # Hilt DI module (DB, DAOs)
│       ├── MainActivity.kt               # Nav host + permission flow
│       ├── data/
│       │   ├── db/
│       │   │   ├── Entities.kt           # Room @Entity classes
│       │   │   ├── Daos.kt               # Room @Dao interfaces
│       │   │   ├── ExpenseDatabase.kt    # Room database
│       │   │   └── Mappers.kt            # Entity ↔ Domain converters
│       │   └── repository/
│       │       └── TransactionRepository.kt
│       ├── domain/model/
│       │   └── Models.kt                 # Transaction, Budget, CategorySummary
│       ├── service/
│       │   ├── SmsParser.kt              # ★ Core SMS parsing engine
│       │   └── SmsReceiver.kt            # BroadcastReceiver for incoming SMS
│       ├── presentation/
│       │   ├── MainActivity.kt
│       │   ├── SmsPermissionScreen.kt
│       │   ├── dashboard/
│       │   │   ├── DashboardViewModel.kt
│       │   │   └── DashboardScreen.kt    # Donut chart, stats, recent txns
│       │   ├── transactions/
│       │   │   ├── TransactionViewModel.kt
│       │   │   └── TransactionsScreen.kt # List, search, filter, add/edit/delete
│       │   ├── budget/
│       │   │   ├── BudgetViewModel.kt
│       │   │   └── BudgetScreen.kt       # Per-category budget + alerts
│       │   └── ui/theme/
│       │       └── Theme.kt              # Dark/light Material3 theme
│       └── utils/
│           └── CsvExporter.kt            # CSV export + share
└── gradle/libs.versions.toml             # Version catalog
```

---

## 🧠 SMS Parsing Logic (SmsParser.kt)

### How It Works — Step by Step

```
Incoming SMS
     │
     ▼
1. isDebitTransaction() ──► Reject OTPs, promos, credit-only messages
     │
     ▼
2. extractAmount()       ──► Multi-pattern regex matching (₹/Rs/INR/USD)
     │
     ▼
3. extractMerchant()     ──► "at/to/POS/UPI" pattern matching + cleanup
     │
     ▼
4. extractBalance()      ──► "Avl Bal / Balance / Bal:" patterns
     │
     ▼
5. categorize()          ──► Keyword lookup table (merchant + full SMS)
     │
     ▼
6. computeHash()         ──► SHA-256 first 16 chars for dedup
     │
     ▼
ParsedTransaction ──► Repository ──► Room DB
```

### Sample Regex Patterns

```kotlin
// AMOUNT — matches: Rs.1,234.56 | INR 500 | ₹ 2,000.00 | $12.50
Regex("""(?:Rs\.?|INR|₹|USD|\$|EUR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")

// MERCHANT — "at DMART MUMBAI on" / "POS AMAZON INDIA"
Regex("""(?:at|to)\s+([A-Za-z0-9\s\-&'.]+?)(?:\s+on\s+|\s+ref|\.|,)""")

// BALANCE — "Avl Bal:Rs.12,345.67" / "Balance: INR 5,678"
Regex("""(?:avl|avail|available|bal|balance)\s*(?:bal|balance)?\s*(?:is|:)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""")
```

### Supported Bank SMS Formats

| Bank       | Sample Format |
|------------|--------------|
| **SBI**    | `Your A/c XXXX1234 is debited for Rs.500.00. Info: UPI/Zomato. Avl Bal:Rs.12345.67` |
| **HDFC**   | `Rs 1,250.00 debited from HDFC Bank a/c XXXX1234. Info: POS at DMART. Avl bal: Rs 45,678` |
| **ICICI**  | `ICICI Bank: Rs 350.00 debited from ac XX1234 at SWIGGY. Avail Bal: Rs 23,456` |
| **Axis**   | `Your Axis Bank a/c debited with INR 2,500.00. UPI ref: Uber India` |
| **Kotak**  | `INR 899.00 debited from Kotak a/c at NETFLIX. Bal: INR 15,678` |
| **Generic**| `Transaction of Rs.1,200.00 using card 1234 at AMAZON INDIA` |

---

## 🧪 Testing with Dummy SMS

### Method 1 — ADB (No real SIM needed)
```bash
# Install app on emulator, then send fake SMS:
adb emu sms send 1234 "Your A/c XXXX9876 is debited for Rs.450.00 on 21/04/24. Info: UPI/Swiggy. Avl Bal:Rs.8,234.56"

adb emu sms send 1234 "Rs 1,250.00 debited from HDFC Bank a/c XX1234. Info: POS at DMART MUMBAI. Avl bal: Rs 45,678.90"

adb emu sms send 1234 "INR 2,500.00 debited from Axis Bank a/c XXXX1234. UPI ref no 123456789. Info: Uber India. Avl Bal INR 12,345.00"

# Test that OTP is IGNORED:
adb emu sms send 1234 "Your OTP is 123456. Do not share with anyone."

# Test that credit is IGNORED:
adb emu sms send 1234 "Rs 5,000.00 credited to your account XXXX1234. Available balance: Rs 15,000.00"
```

### Method 2 — In-App Test (use SmsParser.SAMPLE_SMS_FOR_TESTING)
```kotlin
// In a test or debug screen:
SmsParser.SAMPLE_SMS_FOR_TESTING.forEach { sms ->
    val result = SmsParser.parse(sms)
    println("Amount: ${result?.amount}, Merchant: ${result?.merchant}, Category: ${result?.category}")
}
```

### Method 3 — Unit Test
```kotlin
@Test
fun `test zomato sms parsed correctly`() {
    val sms = "Your A/c XXXX1234 is debited for Rs.350.00. Info: UPI/Zomato. Avl Bal:Rs.5,000.00"
    val result = SmsParser.parse(sms)
    assertNotNull(result)
    assertEquals(350.0, result!!.amount)
    assertEquals(ExpenseCategory.FOOD, result.category)
}
```

---

## 🏗️ Architecture

```
UI Layer (Compose Screens)
        │  observes StateFlow
        ▼
ViewModel Layer (Hilt @HiltViewModel)
        │  calls suspend functions / collects Flow
        ▼
Repository Layer (TransactionRepository)
        │  maps entities ↔ domain models
        ▼
Data Layer (Room DAOs + SmsParser)
        │
        ├── Room Database (SQLite, offline)
        └── SmsParser (pure Kotlin, no network)
```

**Key design decisions:**
- `Flow<List<T>>` from Room — screens auto-update when DB changes
- Dedup via SHA-256 hash of raw SMS body — prevents duplicate inserts
- `SmsParser` is a pure `object` (no dependencies) — easily unit testable
- Hilt for all DI — constructor injection throughout

---

## 🚀 Build & Run

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 35 (target), SDK 26+ (min = Android 8.0)

### Steps
```bash
git clone <repo>
cd ExpenseTracker
# Open in Android Studio → sync Gradle → Run on device/emulator
```

### First Launch
1. App asks for **READ_SMS + RECEIVE_SMS** permissions
2. Tap "Grant SMS Permission"
3. App begins listening for incoming bank SMS automatically
4. You can also tap **+** on the Transactions tab to add manually

---

## 🔒 Privacy Guarantee

- ✅ 100% offline — no network calls, no analytics, no ads
- ✅ All data stored in app-private SQLite database
- ✅ SMS is read locally, raw text stored only for reference
- ✅ User can delete any transaction at any time
- ✅ Export is user-initiated only (CSV via share sheet)

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose BOM 2024.06 | UI framework |
| Room 2.6.1 | Local SQLite database |
| Hilt 2.51.1 | Dependency injection |
| Navigation Compose | Screen routing |
| Accompanist Permissions | Runtime permission handling |
| DataStore Preferences | Theme preference storage |
| OpenCSV 5.9 | CSV export |
| Material3 | Design system |

---

## 🗺️ Roadmap / Enhancements

- [ ] ML Kit on-device text classification for smarter categorization
- [ ] Notification alerts when budget is 80% / 100% used
- [ ] Widget showing today's spend
- [ ] Historical SMS import on first launch
- [ ] Recurring expense detection
- [ ] Multi-currency support
