# TrialSite Solutions - Project Management App

A complete project management application for TrialSite Solutions consulting business.

## Tech Stack

- **Backend:** Java Spring Boot, Spring Security, JWT Authentication
- **Frontend:** React, Ant Design, Axios
- **Database:** H2 (embedded - no installation needed)

## Project Structure

```
trialsite-app/
├── backend/          # Spring Boot backend
│   ├── src/
│   └── pom.xml
├── frontend/         # React frontend
│   ├── src/
│   └── package.json
└── README.md
```

## Setup Instructions

### Backend Setup

1. Navigate to backend folder:
```bash
cd backend
```

2. Run the Spring Boot application:
```bash
mvnw spring-boot:run
```
(On Windows, it will download Maven automatically on first run)

Backend will start on: http://localhost:8080

### Frontend Setup

1. Open a new terminal and navigate to frontend folder:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

Frontend will start on: http://localhost:5173

## First Time Usage

1. Open http://localhost:5173 in your browser
2. Click "Sign up" to create an account
3. Fill in:
   - Full Name: Your Name
   - Username: youruser
   - Email: your@email.com
   - Password: yourpassword (min 6 characters)
4. After signup, login with your credentials
5. You'll see the dashboard!

## Current Features (Step 1 - Complete ✅)

- ✅ User authentication (Login/Signup)
- ✅ JWT token-based security
- ✅ Protected dashboard
- ✅ Basic layout with navigation

## Coming Next

- **Step 2:** Client Management (CRUD operations)
- **Step 3:** Project Management with 6 service types
- **Step 4:** Dashboard with stats and charts
- **Step 5:** Document uploads

## Default Database

Uses H2 embedded database (stored in `backend/data/` folder).
- No PostgreSQL installation needed
- Data persists between restarts
- Can view database at: http://localhost:8080/h2-console
  - JDBC URL: jdbc:h2:file:./data/trialsite
  - Username: sa
  - Password: (leave empty)

## Troubleshooting

### Backend won't start
- Make sure Java 17+ is installed: `java -version`
- Check if port 8080 is available

### Frontend won't start
- Make sure Node.js is installed: `node -v`
- Delete `node_modules` and run `npm install` again
- Check if port 5173 is available

### Login not working
- Make sure backend is running on port 8080
- Check browser console for errors
- Try signup first if no users exist

