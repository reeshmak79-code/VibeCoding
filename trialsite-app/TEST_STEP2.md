# 🧪 Test Step 2 - Client Management

## ⚡ Quick Test Guide

### Prerequisites
✅ Backend running on port 8080  
✅ Frontend running on port 5173  
✅ Logged into the app  

---

## 📝 Test Scenario 1: Add Clients

### Add Client #1 - Research Site
1. Click **"Clients"** in left sidebar
2. Click **"Add Client"** button (top right)
3. Fill form:
   ```
   Company Name: Johns Hopkins Research Center
   Contact Person: Dr. Sarah Johnson
   Email: sarah.johnson@jhrc.edu
   Phone: (410) 955-5000
   Type: Research Site
   Status: Active
   Specialty Areas: Hypertension, Diabetes, Chronic Kidney Disease
   Notes: Large academic medical center with extensive experience in cardiovascular trials
   ```
4. Click **"Create"**
5. ✅ Should see success message
6. ✅ Client appears in table
7. ✅ Statistics update (Total: 1, Active: 1)

### Add Client #2 - CRO
1. Click **"Add Client"** again
2. Fill form:
   ```
   Company Name: Velocity Clinical Research
   Contact Person: Michael Chen
   Email: m.chen@velocityclinical.com
   Phone: (919) 555-0123
   Type: CRO
   Status: Potential
   Specialty Areas: Multi-therapeutic
   Notes: Mid-size CRO interested in partnership for site identification
   ```
3. Click **"Create"**
4. ✅ Statistics update (Total: 2, Active: 1, Potential: 1)

### Add Client #3 - Sponsor
1. Add third client:
   ```
   Company Name: Phoenix Pharmaceuticals
   Contact Person: Dr. Lisa Martinez
   Email: lisa.martinez@phoenixpharma.com
   Phone: (858) 555-7890
   Type: Sponsor
   Status: Active
   Specialty Areas: NASH, Idiopathic Pulmonary Fibrosis
   Notes: Biotech company with 2 Phase III trials starting Q1
   ```

---

## 📝 Test Scenario 2: Search & Filter

### Test Search
1. In search box, type: **"hopkins"**
2. Press Enter
3. ✅ Should see only Johns Hopkins Research Center
4. Clear search (X button)
5. ✅ All clients visible again

### Test Search by Email
1. Search: **"chen"**
2. ✅ Should find Velocity Clinical Research

### Test Filter by Type
1. Click filter icon in **Type** column
2. Select **"Research Site"**
3. ✅ Shows only Research Sites
4. Clear filter

### Test Filter by Status
1. Click filter icon in **Status** column
2. Select **"Active"**
3. ✅ Shows only Active clients
4. Clear filter

---

## 📝 Test Scenario 3: Edit Client

1. Find "Velocity Clinical Research"
2. Click **"Edit"** button
3. Change:
   ```
   Status: Potential → Active
   Notes: "Partnership signed! Starting Q1 2024"
   ```
4. Click **"Update"**
5. ✅ Success message appears
6. ✅ Table updates immediately
7. ✅ Statistics update (Active: 3, Potential: 0)

---

## 📝 Test Scenario 4: Delete Client

1. Add a test client:
   ```
   Company Name: Test Client DELETE ME
   Contact Person: Test Person
   Email: test@test.com
   Phone: 555-1234
   Type: Sponsor
   Status: Potential
   ```
2. Click **"Delete"** on this client
3. Confirm deletion
4. ✅ Success message
5. ✅ Client removed from table
6. ✅ Statistics updated

---

## 📝 Test Scenario 5: Sort & Pagination

### Test Sorting
1. Click **"Company Name"** column header
2. ✅ Sorts A-Z
3. Click again
4. ✅ Sorts Z-A

### Test Pagination
1. If you have many clients:
2. Change page size (bottom right) to "5 per page"
3. Navigate between pages
4. ✅ Pagination works smoothly

---

## 📝 Test Scenario 6: Navigation

### Test Sidebar Navigation
1. Click **"Dashboard"** in sidebar
2. ✅ See dashboard with updated statistics
3. ✅ "Total Clients" shows correct count
4. Click **"Clients"** in sidebar
5. ✅ Returns to clients page
6. ✅ All data still there

### Test Quick Action
1. Go to Dashboard
2. Click **"Add New Client"** under Quick Actions
3. ✅ Navigates to Clients page
4. ✅ Opens Add Client modal automatically

---

## 📝 Test Scenario 7: Validation

### Test Required Fields
1. Click "Add Client"
2. Leave all fields empty
3. Click "Create"
4. ✅ Should show validation errors:
   - "Please enter company name"
   - "Please enter contact person"
   - "Please enter email"
   - etc.

### Test Email Validation
1. Try email: **"notanemail"**
2. ✅ Should show "Please enter a valid email"

### Test Form Cancel
1. Fill form partially
2. Click "Cancel"
3. ✅ Modal closes
4. ✅ Data not saved

---

## 📝 Test Scenario 8: Data Persistence

### Test After Refresh
1. Add a client
2. Refresh browser (F5)
3. ✅ Still logged in
4. ✅ Client data persists
5. ✅ Statistics still correct

### Test After Logout/Login
1. Add/Edit some clients
2. Logout
3. Login again
4. Go to Clients page
5. ✅ All data still there

---

## 🐛 Expected Behavior

### Table Features
✅ Colored tags for Type (blue, green, purple, orange, cyan)  
✅ Colored tags for Status (green=Active, yellow=Potential, etc.)  
✅ Edit and Delete buttons on each row  
✅ Responsive design (works on mobile)  

### Form Features
✅ Modal opens smoothly  
✅ All fields accessible  
✅ Dropdowns work properly  
✅ Validation messages clear  
✅ Create/Update buttons work  

### Statistics
✅ Update immediately after add/edit/delete  
✅ Show correct counts  
✅ Visible on both Dashboard and Clients page  

---

## 🎉 Success Criteria

You should be able to:
- [x] Add multiple clients
- [x] See them in the table
- [x] Search and find specific clients
- [x] Filter by type and status
- [x] Edit client details
- [x] Delete clients
- [x] See statistics update in real-time
- [x] Navigate between pages smoothly
- [x] All data persists after refresh

---

## 🚨 If Something Doesn't Work

### Backend Not Updating?
```powershell
# Stop backend (Ctrl+C)
# Restart:
cd C:\Users\reesh\VibeCoding\trialsite-app\backend
.\mvnw.cmd spring-boot:run
```

### Frontend Not Showing Changes?
```powershell
# Frontend auto-reloads, but if needed:
# Stop (Ctrl+C) and restart:
cd C:\Users\reesh\VibeCoding\trialsite-app\frontend
npm run dev
```

### Data Not Saving?
- Check backend terminal for errors
- Check browser console (F12) for errors
- Verify backend is running on port 8080

### Can't See Clients Menu?
- Make sure you're logged in
- Try refreshing the page
- Clear browser cache

---

**✅ If all tests pass, Step 2 is working perfectly!**

Ready for Step 3? 🚀
