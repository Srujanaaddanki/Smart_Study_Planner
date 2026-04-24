# AI Smart Study Planner 🚀

A production-grade, AI-driven study planning application built with modern Android technologies. This app generates personalized, actionable study schedules based on your subjects and specific topics, helping you master any skill with structured progress tracking and smart reminders.

## ✨ Key Features

- **🧠 AI Schedule Generation**: Generates detailed study plans using Groq/Gemini AI based on a single subject input.
- **🎯 Granular Topic Selection**: Don't just study a subject—select the specific topics you want to focus on.
- **📊 Interactive Progress Dashboard**: Real-time mastery tracking that reflects active tasks and moves completed ones to history.
- **🔔 Smart Reminders**: Reliable `AlarmManager` integration with customizable notification offsets (5, 10, 15 mins).
- **📝 Task-Level Control**:
  - Mark as Complete/Pending.
  - Reschedule or Delete individual tasks.
  - Add Custom Tasks manually to any day.
  - Regenerate specific tasks or entire plans.
- **☁️ Firebase Integration**: 
  - **Firestore**: Professional data structure for active plans and history.
  - **Auth**: Secure user management (ready for login/signup).
- **📴 Offline-First**: Built with Room database for seamless offline access and background syncing.
- **🎨 Premium UI/UX**: Designed with Material 3, Featuring elevated cards, dynamic bottom sheets, and grouped layouts.

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Local Database**: Room
- **Cloud Database**: Firebase Firestore
- **AI Engine**: Groq API (LLaMA 3)
- **Background Tasks**: AlarmManager + BroadcastReceiver

## 📱 Screenshots (Mockups)

| Subject Input | Topic Selection | Active Schedule | Task Options |
| :---: | :---: | :---: | :---: |
| 📝 | ✅ | 📅 | ⚙️ |

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Groq API Key (Set in `AiRepositoryImpl.kt`)
- Firebase Project (Configured with `google-services.json`)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Gowthami1214/Smart_Study_Planner.git
   ```
2. Open the project in Android Studio.
3. Add your `google-services.json` to the `app/` directory.
4. Replace the placeholder API key in `AiRepositoryImpl.kt` with your own.
5. Build and Run!

## 📂 Firestore Structure

```text
users/
  {userId}/
    plans/
      {planId}/
        tasks/
          {taskId} -> [StudyPlanItem]
    history/
      {historyId} -> [CompletedTaskRecord]
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue for any feature requests or bug reports.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Built with ❤️ by [Gowthami](https://github.com/Gowthami1214)
