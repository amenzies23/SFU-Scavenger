# **SFU Scavenger**
SFU Scavenger is an android application designed to streamline the process of creating, managing, and experiencing scavenger hunts. 

**View our project presentation and download the APK from our website:** https://sfu-scavenger-app-webpage.vercel.app/

## **Why We Built This**
The idea came from experiencing scavenger hunts at SFU through events like Frosh, hackathons, club socials, etc.

The traditional process was:
- Manual and time-consuming
- Hard to track team progress in real-time
- Difficult for players to see standings
- Hosts had to manually keep track of task submissions

We wanted to build a tool that would:
- Automate event logistics
- Make hunts smoother for both hosts and players
- Provide a modern multiplayer experience, with a social aspect to it
- Encourage more student involvment with campus activities

## Scope of the Project
### **Key Features**
- **User Authentication**
    - Email-based auth through Firebase Authentication
- **Full Multiplayer Game System**
    - Users can create or join public and private games
    - Game locations are displayed on an interactive map, where you can click an icon to view the game info, and join
- **Task System With Multiple Input types**
    - Text tasks
    - Photo tasks (stored securely in Firebase Storage)
    - QR code tasks
- **Team System With Real-Time Chat**
    - All chat messages are encrypted client-side before being stored in Firestore
- **Interactive In-Game Map System**
    - Displays task submission locations
    - Task submissions update live upon any team member submitting
    - Tapping a submission icon shows a detailed submission preview
    - Photo previews are implemented with image url caching to improve performance
- **Host Management Tools**
    - Create, edit, and launch scavenger hut games
    - Automatic + manual task verification
- **Profile and Friends System**
    - Customize your display name and profile picture
    - View user levels and experience
    - Add / remove friends
- **History Screen**
    - View detailed records of past scavenger hunt participations
    - View full details from each game, about the game itself, and all the tasks you submitted, and how you placed among other teams

## **Tech Stack**
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Google Maps SDK
- Firebase Authentication
- Firebase Firestore
- Firebase Storage

## **Running the App Locally**
Follow these steps to set up the Android project:
### **1. Clone the Repository**
`git clone https://github.com/amenzies23/SFU-Scavenger.git`
### **2. Open in Android Studio**
- Open the project directory
- Let Gradle sync fully
### **3. Add Required Secrets to `local.properties`**
- `API_KEY` (Google Maps)
  - Used for displaying the map
- `CHAT_SECRET` (AES Encrypion Key)
  - Randomly generated unique string for encrypting chat messages
```
API_KEY=your_google_maps_key_here
CHAT_SECRET=your_random_chat_secret_here
```
### 4. **Build & Run**
- Select a device / emulator
- Press run

## Privacy and Security
We have several layers of privacy and security in place to ensure data is handled securely:
- ### Firestore Rules
  - Areas of the database are set up with strict Firestore rules, so only users that should have access in this area, (ie. Members of a team in the chat), are able to access it
- ### Client-Side Chat Encryption
  - Chat messages are encrypted using AES-GCM
  - Firestore only stores the ciphertext
  - UI transparently decrypts messages in memory
- ### Image Security
  - Images are securely stored in Firebase Storage
  - Only authorized users have access to them

## Contributors
- Alex Menzies
- Ali Devjiani
- Karen Yao
- Raymond Chan
