# Juno Offline Data Architecture

This document describes the offline data storage capabilities added to the Juno application.

## Overview

The offline data architecture allows Juno to function without an active internet connection by:

1. Storing data locally in SharedPreferences
2. Queueing changes made while offline
3. Synchronizing with Firebase when connection is restored
4. Showing appropriate user notifications about connection status

## Key Components

### 1. DataManager

A singleton class that handles:
- Local data storage via SharedPreferences
- Network status detection
- Sync operation management
- Toast notifications for users

**Location**: `app/src/main/java/com/example/juno/utils/DataManager.java`

### 2. ApiManager

Manages API calls with offline support:
- Caches API responses
- Stores pending API requests when offline
- Automatically retries failed requests when back online
- Falls back to cached data when offline

**Location**: `app/src/main/java/com/example/juno/utils/ApiManager.java`

### 3. NetworkManager

Monitors network connectivity:
- Provides real-time network status
- Notifies components of connectivity changes
- Works on all Android API levels

**Location**: `app/src/main/java/com/example/juno/utils/NetworkManager.java`

### 4. BaseActivity

Base class for activities that:
- Manages network status changes
- Handles data synchronization
- Shows appropriate UI indicators
- Provides consistent offline behavior across the app

**Location**: `app/src/main/java/com/example/juno/utils/BaseActivity.java`

### 5. TaskRepository

Repository with offline support for tasks:
- Stores tasks locally
- Handles CRUD operations while offline
- Syncs with Firebase when network is restored
- Updates UI with appropriate offline indicators

**Location**: `app/src/main/java/com/example/juno/repository/TaskRepository.java`

## How It Works

1. When online:
   - Data is read from Firebase and cached locally
   - Changes are made to Firebase directly
   - Local cache is updated

2. When offline:
   - Data is read from local cache
   - Changes are stored locally with sync status flags
   - Pending operations are queued

3. When connection is restored:
   - Pending operations are applied to Firebase
   - Local data is updated with server data
   - User is notified

## Usage Example

Activities should extend `BaseActivity` to get offline capability:

```java
public class MyActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Your code here
    }
    
    @Override
    protected void syncData() {
        super.syncData();
        // Your sync code here
    }
}
```

Use `TaskRepository` for task operations, which will automatically handle offline mode:

```java
TaskRepository taskRepository = new TaskRepository(context, userId);
taskRepository.createTask(task);  // Works online or offline
```

## User Experience

- When offline, users see a toast notification
- Changes made offline are saved locally
- When back online, changes are synchronized automatically
- Clear status indicators show when in offline mode 

# Juno - Task Management App

Juno is a task management app that helps you organize your tasks, track your progress, and boost productivity.

## Customization Features

### Light/Dark Mode
- Toggle between light and dark themes in the Settings
- System-wide theme that persists across app restarts
- Automatically applied to all screens and components

### Font Size
- Adjust text size throughout the app (12sp to 24sp)
- Slider control for precise size selection
- Changes apply to all text elements in the app

### Layout Density
- Choose between "Compact" and "Comfortable" layout styles
- Compact: More content fits on screen with tighter spacing
- Comfortable: More spacious layout with better readability

### Notification Styles
- Select from multiple notification presentation styles:
  - Standard: Shows title and description
  - Compact: Shows only the task title
  - Expanded: Shows title, description, and larger preview
  - Minimal: Shows title and priority only
- Enable/disable notifications entirely

## How to Use Customization Features

1. Open the app and tap the Settings icon in the top bar
2. Navigate to the different customization sections:
   - Appearance: Dark mode, font size, and layout density
   - Notifications: Enable/disable and set style
3. All changes are applied immediately and automatically saved

## Technical Implementation

The app uses several techniques to implement these customization features:

- SharedPreferences for storing user preferences
- AppCompatDelegate for theme management
- Custom ThemeUtils class for applying font sizing and layout styles
- NotificationUtils for customizing notification appearance
- Custom Application class for global theme application

## Development

To modify or extend the customization features:

1. The key files are located in:
   - `app/src/main/java/com/example/juno/SettingsActivity.java`
   - `app/src/main/java/com/example/juno/utils/ThemeUtils.java`
   - `app/src/main/java/com/example/juno/utils/NotificationUtils.java`
   - `app/src/main/res/values/themes.xml` and `app/src/main/res/values-night/themes.xml`

2. To add new customization options:
   - Add new preference keys in SettingsActivity
   - Update the settings UI in activity_settings.xml
   - Implement the application logic in the appropriate utility class
   - Apply the settings in relevant activities or adapters 

## Notifications Feature

The Notifications feature allows users to manage all app notifications in one place.

### Key Components

- **NotificationsActivity**: Central hub for viewing and managing all notifications
- **Notification Filtering**: Options to filter by read/unread and notification type
- **Batch Actions**: Mark multiple notifications as read or delete them at once
- **Real-time Updates**: Notifications update in real-time when new ones arrive

**Location**: `app/src/main/java/com/example/juno/NotificationsActivity.java`

## Analytics Feature

The Analytics feature provides users with insights about their task completion patterns and productivity.

### Key Components

- **AnalyticsActivity**: Displays task statistics and performance metrics
- **Completion Rate Tracking**: Shows percentage of tasks completed on time vs. overdue
- **Priority Distribution**: Visualizes task distribution across different priority levels
- **Time-based Analysis**: Tracks completion patterns over time
- **Performance Feedback**: Provides motivational feedback based on user performance

**Location**: `app/src/main/java/com/example/juno/AnalyticsActivity.java`

## Task Completion Tracking

Enhanced task completion tracking system to improve analytics accuracy.

### Key Components

- **Completion Timestamp**: Records exact time when tasks are marked as complete
- **Deadline Comparison**: Accurately determines if tasks were completed before or after deadlines
- **Improved Analytics**: Enables more precise reporting on user productivity patterns
- **Legacy Support**: Includes fallback logic for tasks without completion timestamps

**Implementation**:
- `Task.java`: Added `completedDate` field with getters and setters
- `AllTasksActivity.java`: Updated task completion logic to record timestamps
- `AnalyticsActivity.java`: Enhanced analytics to utilize completion timestamps

## Settings

The Settings screen allows users to customize their app experience.

### Key Components

- **Theme Selection**: Toggle between light and dark themes
- **Notification Preferences**: Control how and when notifications appear
- **Analytics Visibility**: Configure which metrics are displayed
- **Account Management**: Update user profile and preferences

**Location**: `app/src/main/java/com/example/juno/SettingsActivity.java` 