# LiveUpdateNotification 🚀

An Android sample project demonstrating **Live Update (Progress-Centric) Notifications** on Android 16 (API 36) — written in Kotlin.  

This repo shows how to build dynamic, ongoing notifications (e.g. ride tracking, download progress, workout timers) that update in real time.

---

![Manual Dependency Injection (7)](https://github.com/user-attachments/assets/58dfacc9-14f2-4d73-b036-4d8d738c92df)

<img width="516" height="274" alt="Your paragraph text" src="https://github.com/user-attachments/assets/52940af9-91fb-46c4-9538-f3561675bdf7" />

# Read Medium blog:
https://medium.com/@KaushalVasava/live-update-notification-in-android-16-15c0a810849e

## ✨ What are Live Update Notifications?

Unlike regular notifications, Live Update (promoted ongoing) notifications are:

- Prominently shown at the top of the notification shade. :contentReference[oaicite:1]{index=1}  
- Shown on lock screen and as a status-bar chip. :contentReference[oaicite:2]{index=2}  
- Intended for **ongoing, user-initiated, time-sensitive tasks** — e.g. rides, timers, downloads, workouts. :contentReference[oaicite:3]{index=3}

This project demonstrates how to create and update such notifications using Kotlin + Android APIs.

---

## ✅ Features

- Sample implementation of Live Update / Progress-style notification for Android 16+.  
- Example of creating, updating and dismissing a live notification.  
- Clean Kotlin-based code (no custom RemoteViews) implementing best practices.  
- Ready-to-run Android Studio project.

---

## 📋 Requirements

- Android API level: **Target SDK = 36 (Android 16)**  
- Minimum SDK: (as in project)  
- Kotlin (code is 100% Kotlin)  
- AndroidX + androidx.core for NotificationCompat  

---

## 🛠️ Setup & Usage

1. Clone the repository:

    ```bash
    git clone https://github.com/KaushalVasava/LiveUpdateNotification.git
    ```

2. Open in Android Studio. Let Gradle sync.  

3. Add required permission in `AndroidManifest.xml`:

    ```xml
    <uses-permission android:name="android.permission.POST_PROMOTED_NOTIFICATIONS"/>
    ```

4. Build & Run on a device/emulator targeting Android 16 (or higher).  

5. The sample project shows how to:
   - Create a live notification (with `.setOngoing(true)` and `.setRequestPromotedOngoing(true)`) :contentReference[oaicite:4]{index=4}  
   - Use a progress-style or other eligible style (e.g. `ProgressStyle`) instead of custom RemoteViews. :contentReference[oaicite:5]{index=5}  
   - Update the notification (e.g. progress changes), by calling `notify(...)` again with updated builder. :contentReference[oaicite:6]{index=6}  
   - Handle dismissal using a `deleteIntent`, so user-initiated dismissals can be detected and prevented from automatically re-posting the notification. :contentReference[oaicite:7]{index=7}

6. Modify the sample as per your need — e.g. real-time ride tracking, download manager, timers, etc.

---

## 📚 Code Example (Kotlin)

```kotlin
val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_notification)
    .setContentTitle("Task in Progress")
    .setContentText("Starting...")
    .setOngoing(true)
    .setRequestPromotedOngoing(true)
    .setOnlyAlertOnce(true)
    .setStyle(
      NotificationCompat.ProgressStyle()
        .setProgress(100, 0, false)
    )
    .setShortCriticalText("Now")
    .setWhen(System.currentTimeMillis() + 2 * 60 * 1000)
    .setShowWhen(true)

NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())

// Later, update progress
builder.setProgress(100, 50, false)
NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())

// Handle dismissal
val deleteIntent = PendingIntent.getBroadcast(
  context,
  0,
  Intent(context, NotificationDismissReceiver::class.java),
  PendingIntent.FLAG_IMMUTABLE,
)
builder.setDeleteIntent(deleteIntent)

```

# Contribution
You can contribute this project. Just Solve issue or update code and raise PR. I'll do code review and merge your changes into main branch.

- See Commit message guidelines https://initialcommit.com/blog/git-commit-messages-best-practices.
- Guidlines to create pull request [feature_name]_#your_nickname this should be the branch name.

# Licence
Copyright 2023 Kaushal Vasava

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

# Author
Kaushal Vasava

# Thank you
Contact us if you have any query on LinkedIn, Github, Twitter or
Email: kaushalvasava.app.feedback@gmail.com
