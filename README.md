# Khata (Android, Java)

Built in the same style as the Movie_Database project: XML layouts, Fragments behind a custom bottom
navigation bar, RecyclerView adapters, Intent-based detail screens, Gradle Kotlin DSL + version catalog.
Uses the SAME Firebase project and Firestore data as the Khata website.

## Setup
1. Open this folder in Android Studio and let Gradle sync.
2. Firebase console -> project `business-ledger-f7720` -> Project settings -> Add app -> Android,
   package name `com.khata.ledger`.
3. Terminal: `./gradlew signingReport`, copy the debug SHA1, add it under the Android app's fingerprints.
4. Download `google-services.json` (after adding the SHA1) and put it in `app/`.
5. Authentication -> enable Email/Password and Google.
6. Run. For a release build, add the release keystore SHA1 too and re-download google-services.json.

## Structure (com.khata.ledger)
- LoginActivity, MainActivity (loads data once, bottom tabs)
- Tabs: HomeFragment, LedgerFragment, StockFragment, PersonalFragment, ProfileFragment
- Screens: EntryActivity, StockEntryActivity, ExpenseEntryActivity, PredictionActivity, ReportActivity, AdminActivity
- Adapters: LedgerAdapter, StockAdapter, ExpenseAdapter
- Data: DataStore (Firestore), Ledger/Stock/Expense/PricePoint models, Predictor, Util
- Languages: res/values (English) and res/values-bn (Bangla); switch in Profile tab.
