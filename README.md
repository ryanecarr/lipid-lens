# Lipid Lens

**Lipid Lens** is an Android companion for tracking cholesterol-lowering therapies, daily adherence, side effects, longitudinal lipid panels, and clinician visits.

> **Disclaimer:** Lipid Lens is a personal health-tracking and educational tool. It does not provide medical advice, diagnosis, or treatment. Always consult your physician or cardiologist before starting, stopping, or modifying any medication or clinical target.

## Key Features

- **Combination & Non-Statin Therapy Tracking**
  - Supports primary and adjunct therapies across statins, ezetimibe, bempedoic acid, PCSK9 inhibitors (evolocumab, alirocumab), inclisiran, and icosapent ethyl.
  - Tracks active regimens, therapy switches, stop/end dates, and dosing cadences (daily, every other day, every 2 weeks, monthly, or custom).
- **Schedule-Aware Adherence & Reminders**
  - Daily dose and comfort check-ins (how you feel, side effects, and notes) evaluated against each medication's true dosing cadence.
  - Cadence-aware local notification reminders anchored to each regimen's start date and schedule, restored automatically across device reboots.
- **Lipid Labs, Personalized Targets & Baseline Anchors**
  - Log full lipid panels (Total Cholesterol, LDL-C, HDL-C, Triglycerides, Non-HDL-C, ApoB, and Lp(a)) with fasting status and pre-therapy baseline tagging.
  - Configure physician-prescribed **LDL-C**, **Non-HDL-C**, and **ApoB** targets (presets for General `<100`, High Risk `<70`, Very High Risk `<55`, or Custom), plotted on multi-range trend charts (`3M`, `6M`, `1Y`, `All`).
- **Post-Switch Lab Recheck Window**
  - Tracks weeks elapsed since starting or changing therapy and surfaces a calm 4–12 week follow-up lab window indicator until a post-therapy panel is logged.
- **Cardiology AI Insights & Physician PDF Reports**
  - Generates personalized trend commentary and a structured 20-second physician visit brief powered by Firebase AI Logic.
  - Exports multi-page PDF clinical reports combining active/prior therapies, personalized targets, baseline deltas, adherence/tolerance metrics, and lab history for appointments.

## Tech Stack

- **Language & UI:** Kotlin, Jetpack Compose, Material 3 (adaptive compact/expanded layouts and custom themes)
- **Architecture:** MVVM with `ViewModel` and Kotlin Coroutines / `StateFlow`
- **Backend & Security:** Firebase Authentication (Google Sign-In via Android Credential Manager), Cloud Firestore with per-user zero-trust security rules, and Firebase App Check
- **AI:** Firebase AI Logic
- **Testing:** JUnit 4, Robolectric, Coroutines Test, and Firestore Emulator Rules tests

## Local Setup

Sensitive Firebase configuration files and signing keystores are excluded via `.gitignore`. To build locally on a fresh clone:

1. **Prerequisites**
   - Android Studio / Android SDK (compileSdk `36`, minSdk `24`, targetSdk `36`, JDK `11+`)
   - A Firebase project with **Google Sign-In**, **Cloud Firestore**, **Firebase App Check**, and **Firebase AI Logic** enabled

2. **Add Firebase Configuration Files**
   - Download `google-services.json` from your Firebase project and place it at:
     `app/google-services.json`
   - Create `app/src/main/res/values/firebase_applet_config.xml`:
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <resources>
         <string name="firestore_database_id" translatable="false">YOUR_FIRESTORE_DATABASE_ID</string>
         <string name="recaptcha_site_key" translatable="false">YOUR_RECAPTCHA_SITE_KEY</string>
     </resources>
     ```

3. **Build & Run Tests**
   ```bash
   # Run local unit & Robolectric tests
   ./gradlew :app:testDebugUnitTest

   # Build debug APK
   ./gradlew assembleDebug
   ```

4. **Release Signing (Optional)**
   - Release builds read signing credentials from environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) so no upload keys or passwords are stored in the repository.

5. **Firestore Security Rules Tests (Optional)**
   - With the Firebase CLI installed, validate `firestore.rules` against the local emulator:
     ```bash
     firebase emulators:exec --only firestore --project demo-no-project "node --test firestore.test.js"
     ```
