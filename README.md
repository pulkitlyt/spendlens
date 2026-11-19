# SpendLens 💸

An Android app that reads your bank, credit card and UPI alert SMS, then sorts every expense into a category: Food, Groceries, Shopping, Bills, Travel, Fuel, EMI and more. It shows where your money went each month.

**Private by design:** the app has **no internet permission**. Your SMS never leave the phone.

> Android only. iPhones don't let any app read SMS.

## Install on your phone

1. On your Android phone, open the download link:
   **`https://github.com/pulkitlyt/spendlens/releases/latest/download/SpendLens.apk`**
   (or open the repo's **Releases** page and tap `SpendLens.apk`)
2. Open the downloaded file. If asked, allow your browser to **install unknown apps**.
3. Open SpendLens and tap **Allow SMS access**.
   - If Android says **"Restricted setting"**, go to *Settings → Apps → SpendLens → ⋮ (top-right) → Allow restricted settings*, then try again. Android 13+ shows this for any app installed outside the Play Store.

Updates: download from the same link again and install over the old version. Your category corrections are kept.

## What it does

- Reads the last 12 months of SMS and picks out real transactions. It skips OTPs, promos, "payment due" reminders, failed transactions, UPI collect requests and SMS from personal numbers.
- Pulls out the amount, debit or credit, merchant (from UPI VPA, "at X", "to X", "Info:" and similar) and the last digits of the account or card.
- Puts each transaction into one of 16 expense categories or 3 income categories, using a large list of Indian merchants and keywords (Swiggy, Zomato, Blinkit, Amazon, Uber, IRCTC, Airtel, Zerodha, LIC…).
- **Overview tab:** total spent and received per month, a donut chart, and a breakdown by category.
- **Transactions tab:** day-by-day list you can filter by category.
- **Tap any transaction** to change its category. Tick *"Always use this for <merchant>"* and the app learns that merchant.
- Credit card bill payments are not counted, so a card spend isn't counted twice (once when you spend, again when you pay the bill).

## How the APK gets built

Every push to `main` runs [`.github/workflows/build.yml`](.github/workflows/build.yml). It:

1. runs the SMS parser unit tests,
2. builds a signed release APK,
3. publishes it as a new GitHub Release, so the `latest/download/SpendLens.apk` link always gives the newest version.

Signing uses these repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`. Keep them the same, or phones will refuse to install updates over the old version.

## Improving categorisation

Rules live in [`Categorizer.kt`](app/src/main/java/com/spendlens/app/parse/Categorizer.kt), and SMS parsing lives in [`SmsParser.kt`](app/src/main/java/com/spendlens/app/parse/SmsParser.kt). If one of your bank's SMS is read wrong, add it as a case in [`SmsParserTest.kt`](app/src/test/java/com/spendlens/app/SmsParserTest.kt) and fix the parser until the test passes.
