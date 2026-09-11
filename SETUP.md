# Hisab — Setup & Test Guide (Phase 1 / MVP)

This is a native Android app (Kotlin + Jetpack Compose). I wrote and self-reviewed every
file; the project is a complete, standard Gradle project — it includes the Gradle wrapper
(`gradlew`, `gradlew.bat`, `gradle/wrapper/`), so any standard Android/Gradle build
pipeline can build it directly with:
```bash
./gradlew assembleDebug
```
which produces `app/build/outputs/apk/debug/app-debug.apk`. No local Android Studio
install is required to build it — use whatever build service/app you have in mind.

If you do want to run it locally at some point, opening the folder in Android Studio and
clicking Run also works (it uses the same `gradlew`).

The APK needs an emulator or a real Android phone (min Android 8 / API 26) to install and
run on, since the core feature (reading SMS) only makes sense on-device. A real phone is
better for testing since it'll have your actual bKash/Nagad/bank SMS in its inbox; an
emulator can only be sent a synthetic test SMS via `adb`:
```bash
adb emu sms send bKash "Cash Out Tk 500.00 from 01700000000 successful. Fee Tk 0.00. Balance Tk 4,500.00. Ref 8XYZ12."
```

## 2. Connect it to the web backend (hisab.prothom.shop)

The app works fully offline on its own (local Room database) — sync is optional but wired
up. Once the `hisab-web` backend is live (see its own DEPLOY.md):

1. Open the app → **More → Settings**.
2. Under **Backend Sync**, enter:
   - **Backend URL**: `https://hisab.prothom.shop` (no trailing slash, no `/api`)
   - **API Key**: the value shown on the web dashboard's own Settings page
3. Tap **Save**, then **Sync Now**.
4. Optionally turn on **Auto Background Sync** — runs every ~30 minutes over wifi/data via
   WorkManager, no need to open the app.

What sync actually does:
- **Accounts/Categories**: matched by name in both directions, so the default seeded ones
  (bKash/Nagad/Bank/Cash, the standard category list) don't get duplicated — whichever
  side has one first, the other links to it.
- **Transactions**: pushes anything recorded on the phone up to the server, and pulls down
  anything added from the web dashboard, replaying it through the same balance-adjusting
  logic locally — so account balances stay consistent no matter which side a transaction
  was entered from.
- **SMS**: only pushed up from the phone (the server can't receive real SMS), but if you
  classify a pending one from the web dashboard's SMS Review page, that status comes back
  down too, so the phone won't show it as pending anymore.

This is a first version of sync, not a fully general one — see the note at the end of this
file for what's deliberately out of scope.

## 3. Manual test checklist

1. Launch → **Get Started** → **Allow SMS Access** → grant both permissions when Android
   asks. The app imports any existing bKash/Nagad/bank SMS already in your inbox.
2. **Dashboard** shows 4 seeded accounts (bKash, Nagad, Bank, Cash) all at ৳0, and a
   pending SMS count if anything was imported.
3. Open **SMS Review**, pick one item, choose **Expense**, pick an Account + Category,
   tap **Confirm & Next** — it should disappear from the pending list.
4. Go to **Transactions** — the confirmed item should now appear there, and back on
   **Dashboard** the account balance and This-Month Expense should have updated.
5. Tap **+ → Transfer**, move money between two accounts — confirm both account balances
   change and This-Month Income/Expense are unaffected.
6. Tap **+ → Neutral**, record one — confirm the account balance changes but Income/Expense
   totals don't.
7. **Accounts → + Add New Account**, **Categories → Add Category** — confirm new ones show
   up immediately in the Add Transaction pickers.
8. With the backend connected (step 2 above): **Settings → Sync Now** — confirm it reports
   success, and that the transactions/accounts you created above now show up on
   `https://hisab.prothom.shop/`. Then add a transaction *on the web dashboard* and Sync Now
   again on the phone — it should appear in the phone's Transactions list too, with the
   right account balance change.

If anything crashes, `adb logcat` (or Android Studio's Logcat panel, if you end up opening
it there) will show the stack trace — send that over and I'll fix it.

## What's not built yet (Phase 2, on purpose)

Reports/charts, AI-suggested SMS classification, custom auto-apply rules, Backup & Export
UI, PIN/biometric lock, theme switching, notifications, calendar view. The bottom-nav
"Reports" tab and the "More → Settings" screen say so explicitly rather than showing a
half-built feature.

## Sync limitations (by design, not bugs)

- **Edits and deletes don't sync** — only new rows. Renaming an account or deleting a
  transaction on one side won't currently propagate to the other. Everything so far is
  additive/create-only on both sides, since that covers the actual use case (record on the
  phone via SMS, view/occasionally add from the web) without the real complexity of
  distributed edit-conflict resolution.
- **No offline queue awareness beyond "retry next sync"** — if a sync run fails partway
  (e.g. wifi drops), whatever already went through stays synced (it's marked incrementally,
  not all-or-nothing), and the rest picks up cleanly on the next run.
- **Balances aren't synced directly** — each side computes its own account balance from its
  own transaction history. Since transactions themselves do sync both ways, the balances
  end up matching in practice once a sync completes, rather than one side copying the
  other's number.
