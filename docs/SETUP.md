# Setup Guide

This guide takes you from a fresh clone to a running WordVoyage app. It takes about 20 minutes.

## What you need

- **Android Studio** (latest stable version). It includes the JDK.
- An **Android phone** (recommended, because speech recognition needs a real microphone) or an emulator with microphone support.
- A free **Google account** for Firebase.
- Internet access. In this version it is also needed to load the stories.

---

## Step 1: Get the code

```bash
git clone https://github.com/Lourdusmel-16/WordVoyage-Firestore.git
cd WordVoyage-Firestore
```

Open the folder in Android Studio and let Gradle sync. It may fail until you finish steps 2 to 5, and that is expected.

---

## Step 2: Create a Firebase project

1. Go to the [Firebase console](https://console.firebase.google.com) and click **Add project**.
2. Name it (for example `WordVoyage`). Google Analytics is optional and can be turned off.
3. In **Project settings → Your apps**, click the **Android** icon.
4. For **Android package name**, enter the `applicationId` from `app/build.gradle(.kts)`. By default this is `com.sample.readingapp`.
5. Click **Register app**, then **Download `google-services.json`**.
6. Put the file here:

   ```
   WordVoyage-Firestore/app/google-services.json
   ```

> ⚠️ This file is listed in `.gitignore`. Never commit it to a public repository.

---

## Step 3: Turn on sign-in and the database

**Authentication**
1. Firebase console → **Build → Authentication → Get started**.
2. **Sign-in method → Email/Password → Enable → Save**.

**Firestore**
1. **Build → Firestore Database → Create database**.
2. Choose a location close to your users and start in **production mode**. You will add rules in step 5.

---

## Step 4: Set the admin passcode

The admin passcode is **not** stored in the app. You set it yourself in Firestore.

1. Firestore → **Start collection** → Collection ID: `config`.
2. Document ID: `admin`.
3. Add a field: name `code`, type `string`, value = **your chosen passcode**.
4. Save.

To change the passcode later, just edit this field. No app update is needed.

---

## Step 5: Publish the security rules

Firestore → **Rules** tab → replace the content with the following, then click **Publish**.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // The passcode document is never readable from the app.
    match /config/{docId} {
      allow read, write: if false;
    }

    // A user can create a claim only by supplying the correct passcode.
    match /adminClaims/{uid} {
      allow read: if false;
      allow create, update: if request.auth != null
        && request.auth.uid == uid
        && request.resource.data.code == get(/databases/$(database)/documents/config/admin).data.code;
    }

    // Any signed-in user can read stories. Only verified admins can add or change them.
    match /stories/{storyId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null
        && exists(/databases/$(database)/documents/adminClaims/$(request.auth.uid));
    }

    // Each user can read and write only their own profile and progress.
    // Becoming an admin requires a valid claim from the step above.
    match /users/{uid} {
      allow read: if request.auth != null && request.auth.uid == uid;
      allow create, update: if request.auth != null
        && request.auth.uid == uid
        && (request.resource.data.get('isAdmin', false) == false
            || exists(/databases/$(database)/documents/adminClaims/$(uid)));
    }
  }
}
```

If the admin option says "Incorrect passcode" even with the right code, these rules or the `config/admin` document are missing or have a typo.

---

## Step 6: Add the speech-recognition model

WordVoyage listens using the on-device **Sherpa-ONNX** speech recogniser. The model files are large, so they are **not** in the repository. You must add them.

1. Download a **streaming English zipformer transducer** model from the [sherpa-onnx pretrained models](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html) list.
2. Create this folder: `app/src/main/assets/sherpa-model/`
3. Copy these four files into it, and use exactly these names:

   ```
   encoder-epoch-99-avg-1.onnx
   decoder-epoch-99-avg-1.onnx
   joiner-epoch-99-avg-1.onnx
   tokens.txt
   ```

   If your downloaded files have different names (for example a `-chunk-16-left-128` suffix), either rename them to match, or change the file names in `speech/SherpaSpeechManager.kt`.

> The model files are large. Do **not** commit them to a normal Git repo. Keep `app/src/main/assets/sherpa-model/` out of Git by adding it to `.gitignore`, or use [Git LFS](https://git-lfs.com).

---

## Step 7: Run the app

1. Connect your phone with USB debugging on (or start an emulator).
2. In Android Studio, press **Run ▶**.
3. When asked, **allow the microphone**.
4. Tap **Sign up**, create an account, choose your **grade level**, and start reading.

### Becoming an admin

On the sign-in or sign-up screen, tick **Sign in as Admin** and enter the passcode from step 4. Admins then see an **Upload New Story (Admin)** button on the home screen.

---

## Step 8: Add stories to Firestore

The app shows nothing until the `stories` collection has documents. Choose either way:

**A. From the app (as admin)**
1. Sign in with **Sign in as Admin** ticked and your passcode.
2. On the home screen tap **Upload New Story (Admin)**.
3. Choose the **grade level**, paste the story JSON, and tap **Upload to Firestore**.

**B. From the Firebase console**
1. Firestore → **stories** collection → **Add document**.
2. Use any unique Document ID (this becomes the permanent story ID).
3. Add the fields `title`, `gradeLevel`, `ageGroup`, `difficulty`, `order`, `screens` (array of strings) and `quiz` (map with `question`, `options`, `correctIndex`).

Story JSON format:

```json
{
  "title": "The Little Fox",
  "gradeLevel": "Grades 1 to 3",
  "ageGroup": "GRADES_1_3",
  "difficulty": "Easy",
  "order": 1,
  "screens": ["Page one text.", "Page two text."],
  "quiz": {
    "question": "What did the fox find?",
    "options": ["A key", "A lantern", "A map"],
    "correctIndex": 1
  }
}
```

Rules to remember:
- Never rename a story's document ID, because saved progress is stored under it.
- `ageGroup` must be `GRADES_1_3`, `GRADES_4_5`, `GRADES_6_8` or `GRADES_9_12`.

---

## Step 9: Turn on GitHub Actions (optional)

1. GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**.
2. Name: `GOOGLE_SERVICES_JSON`
3. Value: paste the **entire contents** of your `google-services.json`.
4. Push a commit and open the **Actions** tab. A green tick means the app builds and the tests pass.

---

## Troubleshooting

| Problem | Likely cause and fix |
|---|---|
| Gradle error about `google-services.json` | The file is missing from `app/` or has the wrong package name. Repeat step 2. |
| Sign-up or sign-in fails | Email/Password is not enabled (step 3), or the phone has no internet. |
| "Incorrect Admin Passcode" | `config/admin` is missing, the field is not named `code`, or the rules are not published (steps 4 and 5). |
| Words are not highlighted when reading | The model files are missing or misnamed (step 6), or microphone permission was denied. Check **Settings → Apps → WordVoyage → Permissions**. |
| App crashes when you start reading | Same as above. Check Logcat for a message about `sherpa-model`. |
| Word meaning box says it cannot find a word | The dictionary service needs internet, and some words are not listed. |
| Home screen shows no stories | The `stories` collection is empty, the `ageGroup` value is misspelt, the phone is offline, or the rules in step 5 are missing the `stories` block. |
| Admin upload fails | You are not signed in as admin, or the `adminClaims` and `stories` rules are not published. |
| Progress is not saved | Check the Firestore rules (step 5) and that you are signed in. |
