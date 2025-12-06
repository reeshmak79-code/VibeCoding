# 🚀 Complete Setup Guide - TrialSite Solutions

## ✅ Step 1: Install Required Software

### 1. Install Java JDK 17

**Download & Install:**
1. Go to: https://adoptium.net/temurin/releases/
2. Select:
   - Operating System: Windows
   - Architecture: x64
   - Package Type: JDK
   - Version: 17 (LTS)
3. Download and run the installer
4. **IMPORTANT:** Check "Set JAVA_HOME variable" during installation
5. **IMPORTANT:** Check "Add to PATH" during installation

**Verify Installation:**
Open a NEW PowerShell window and run:
```powershell
java -version
```
You should see: `openjdk version "17.x.x"`

If not found, you need to add Java to PATH manually:
```powershell
# Set JAVA_HOME (replace path if different)
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot"
setx PATH "%PATH%;%JAVA_HOME%\bin"
```
Then close and reopen PowerShell.

### 2. Verify Node.js (Already Installed ✅)

```powershell
node -v
npm -v
```

---

## ✅ Step 2: Start the Backend

1. Open PowerShell in the backend folder:
```powershell
cd C:\Users\reesh\VibeCoding\trialsite-app\backend
```

2. Start the Spring Boot server:
```powershell
.\mvnw.cmd spring-boot:run
```

**First time:** Maven will download all dependencies (takes 2-5 minutes)

**Success:** You'll see:
```
Started TrialSiteApplication in X seconds
```

Backend runs on: **http://localhost:8080**

---

## ✅ Step 3: Start the Frontend

1. Open a NEW PowerShell window
2. Navigate to frontend folder:
```powershell
cd C:\Users\reesh\VibeCoding\trialsite-app\frontend
```

3. Install dependencies (first time only):
```powershell
npm install
```

4. Start the development server:
```powershell
npm run dev
```

**Success:** You'll see:
```
VITE ready in 500ms
Local: http://localhost:5173/
```

Frontend runs on: **http://localhost:5173**

---

## ✅ Step 4: Test the Application

1. Open browser: http://localhost:5173
2. Click **"Sign up"**
3. Create account:
   - Full Name: John Doe
   - Username: john
   - Email: john@trialsite.com
   - Password: password123
4. Click **"Login"** and enter credentials
5. You should see the Dashboard! 🎉 

---

## 📁 Project Structure

```
trialsite-app/
├── backend/                    # Java Spring Boot
│   ├── src/
│   │   └── main/
│   │       ├── java/com/trialsite/
│   │       │   ├── TrialSiteApplication.java
│   │       │   ├── config/          # Security config
│   │       │   ├── controller/      # REST APIs
│   │       │   ├── dto/             # Request/Response objects
│   │       │   ├── model/           # Database entities
│   │       │   ├── repository/      # Database access
│   │       │   └── security/        # JWT & Auth
│   │       └── resources/
│   │           └── application.properties
│   ├── pom.xml                # Maven dependencies
│   └── mvnw.cmd               # Maven wrapper
│
├── frontend/                  # React App
│   ├── src/
│   │   ├── components/        # Reusable components
│   │   ├── context/           # Auth context
│   │   ├── pages/             # Login, Signup, Dashboard
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── package.json           # NPM dependencies
│
└── README.md
```

---

## 🐛 Troubleshooting

### Problem: Backend won't start

**"Java not recognized"**
```
Solution: Install Java JDK 17 and add to PATH (see Step 1)
```

**Port 8080 already in use**
```powershell
# Find what's using port 8080
netstat -ano | findstr :8080
# Kill the process (replace PID)
taskkill /PID <PID> /F
```

### Problem: Frontend won't start

**"npm not recognized"**
```
Solution: Install Node.js from https://nodejs.org
```

**Port 5173 already in use**
```
Solution: Kill the other Vite server or change port in vite.config.js
```

### Problem: Login shows error

**"Network Error" or "Cannot connect"**
```
Solution: 
1. Make sure backend is running on port 8080
2. Check backend terminal for errors
3. Try: http://localhost:8080/api/auth/signup in browser
```

**"Username already taken"**
```
Solution: Use a different username or login with existing one
```

---

## 📝 API Endpoints

### Authentication
- POST `/api/auth/signup` - Create new user
- POST `/api/auth/login` - Login and get JWT token
- GET `/api/auth/me` - Get current user (requires auth)

### Database
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/trialsite`
  - Username: `sa`
  - Password: (empty)

---

## ✅ Features Completed (Step 1)

- ✅ Spring Boot backend with REST APIs
- ✅ React frontend with modern UI (Ant Design)
- ✅ User authentication (JWT)
- ✅ Login/Signup pages
- ✅ Protected dashboard
- ✅ Session management
- ✅ Password encryption (BCrypt)
- ✅ CORS configuration
- ✅ H2 embedded database

---

## 🎯 Next Steps

**Step 2: Client Management**
- Add clients page
- Create/Edit/Delete clients
- Client details view
- Search and filter

**Step 3: Project Management**
- Projects linked to clients
- 6 service types
- Status workflow
- Deliverable tracking

**Step 4: Dashboard Enhancement**
- Real statistics
- Charts and graphs
- Activity timeline

**Step 5: Document Management**
- File uploads
- Document library
- Version control

---

## 💡 Tips

1. **Keep both terminals open** (backend + frontend)
2. **Backend changes:** Restart backend server
3. **Frontend changes:** Auto-reload in browser
4. **Clear browser cache** if seeing old data
5. **Check browser console (F12)** for errors

---
--Reeshma notes 

Option 2: Check what's in database
Open: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/trialsite
Username: sa
Password: (leave empty)
Click Connect

## 🎉 You're All Set!

Once Java is installed, you can start both servers and begin testing.

Need help? Check the troubleshooting section above.
