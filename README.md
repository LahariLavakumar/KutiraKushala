# Kutira-Kushala — Android App

> Digital Self-Employment Platform for cottage industries and bulk buyers.  
> This is the **Android Studio** port of the original React/Firebase web app.

---

## Architecture

```
KutiraKushala/
├── app/
│   ├── google-services.json          ← Firebase config (replace with yours)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/kutira/kushala/
│           ├── MainActivity.kt
│           ├── KutiraApp.kt           ← Application class / manual DI
│           ├── data/
│           │   ├── model/
│           │   │   └── Models.kt      ← All data classes (Business, Product, Quote, etc.)
│           │   └── repository/
│           │       ├── AuthRepository.kt
│           │       ├── FirestoreRepository.kt  ← Core data + mirrors security rules
│           │       ├── StorageRepository.kt
│           │       └── GeminiRepository.kt
│           └── ui/
│               ├── KutiraNavGraph.kt  ← Navigation + role-aware bottom tabs
│               ├── theme/Theme.kt
│               ├── auth/              ← AuthViewModel, AuthScreen
│               ├── business/          ← BusinessViewModel, ListScreen, DetailScreen
│               ├── wishlist/          ← WishlistViewModel, WishlistScreen
│               └── quotes/            ← QuoteViewModel, OrdersScreen
```

---

## Setup

### 1. Firebase
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Add an **Android app** with package name `com.kutira.kushala`
3. Download **`google-services.json`** and replace `app/google-services.json`
4. The same **Firestore database** and **`firestore.rules`** are used — no changes needed on the backend

### 2. Gemini API Key
In `local.properties` (create from `local.properties.example`):
```
GEMINI_API_KEY=your_key_here
```

### 3. Open in Android Studio
- Open the `KutiraKushala/` folder in Android Studio Hedgehog or later
- Let Gradle sync
- Run on emulator or device (API 26+)

---

## Feature Map (Web → Android)

| Web Feature | Android Equivalent |
|---|---|
| React Router | Jetpack Navigation Compose |
| Firebase Auth (email/password) | `AuthRepository` + `FirebaseAuth` |
| Firestore queries | `FirestoreRepository` with `callbackFlow` |
| Firebase Storage uploads | `StorageRepository` |
| Gemini AI description gen | `GeminiRepository` (streaming) |
| Tailwind CSS | Material 3 + Compose |
| `isAcceptingOrders` toggle | `isAcceptingOrders: Boolean` on `Business` |
| Role: Maker / Collector | `UserProfile.role` — immutable post sign-up |
| Quote status lifecycle | `QuoteStatus` enum — seller advances, buyer cancels |
| `firestore.rules` | Same rules file — enforced server-side; client mirrors for fast feedback |

---

## Security Rules
The `firestore.rules` file is **unchanged** — the same backend rules apply to Android as to the web app. The `FirestoreRepository` mirrors the validation logic locally to give users instant feedback before the Firestore write is attempted.

---

## Dependencies
| Library | Purpose |
|---|---|
| Firebase Auth KTX | Authentication |
| Firebase Firestore KTX | Database |
| Firebase Storage KTX | Image uploads |
| Jetpack Compose + Material 3 | UI |
| Navigation Compose | Screen routing |
| Coil | Image loading from Storage URLs |
| Generative AI (Gemini) | AI-powered descriptions |
| Coroutines + Play Services | Async Firestore/Auth |
