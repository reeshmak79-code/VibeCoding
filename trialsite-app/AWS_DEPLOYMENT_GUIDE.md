# 🚀 AWS Free Tier Deployment Guide - TrialSite Solutions

## ✅ Yes, You Can Deploy on AWS Free Tier!

Your application uses:
- **Backend:** Spring Boot (Java 17)
- **Frontend:** React + Vite
- **Database:** H2 (embedded file-based)
- **Total Monthly Cost:** ~$0-3 (mostly free!)

---

## 🎯 Deployment Architecture

```
User Browser
    ↓
CloudFront (CDN) → S3 (React Frontend)
    ↓
EC2 t2.micro (Spring Boot Backend + H2 Database)
```

---

## 📦 What You Need (Prerequisites)

1. **AWS Account** (Free Tier eligible for 12 months)
   - Sign up: https://aws.amazon.com/free/
   - Credit card required (won't be charged if staying in free tier)

2. **Tools to Install:**
   - AWS CLI: https://aws.amazon.com/cli/
   - SSH client (PuTTY for Windows or built-in terminal)

---

## 🔧 Step-by-Step Deployment

### **Part 1: Deploy Backend on EC2**

#### 1.1. Create EC2 Instance

1. **Login to AWS Console:** https://console.aws.amazon.com/
2. **Go to EC2:** Services → EC2 → Launch Instance
3. **Configure:**
   - Name: `trialsite-backend`
   - AMI: **Ubuntu Server 22.04 LTS** (Free tier eligible)
   - Instance type: **t2.micro** (Free tier eligible)
   - Key pair: Create new → Download `.pem` file (save it safely!)
   - Network settings:
     - **Security Group:** Select "Create security group" (default)
     - **Inbound Rules:**
       - ✅ **Allow SSH traffic from:** Check this box
         - Dropdown: Select "Anywhere" (0.0.0.0/0) for now, or "My IP" for better security
         - ⚠️ **Security Warning:** AWS will show a warning about allowing all IPs (0.0.0.0/0). For production, restrict to your IP only.
       - ✅ **Allow HTTP traffic from the internet:** Check this box
         - This enables port 80 for web traffic
       - ✅ **Allow HTTPS traffic from the internet:** Check this box (optional, for HTTPS later)
       - **To add Backend API (port 8080):**
         - Click "Add security group rule" button
         - Type: Custom TCP
         - Port range: 8080
         - Source: 0.0.0.0/0 (or restrict to specific IPs for better security)
         - Description: "Backend API access"
   - Storage: 30 GB (Free tier eligible)
4. **Launch Instance**

> **🔒 Security Note:** AWS will show a warning: "Rules with source of 0.0.0.0/0 allow all IP addresses to access your instance." 
> - For **development/testing:** Using "Anywhere" (0.0.0.0/0) is acceptable
> - For **production:** Restrict SSH to "My IP" only and limit other ports to specific IP ranges when possible
> - You can always edit security group rules later in the EC2 console

#### 1.2. Connect to EC2

**Windows (PowerShell):**
```powershell
# Move to where your .pem file is
cd C:\Users\reesh\Downloads

# Set permissions
icacls "your-key.pem" /inheritance:r /grant:r "%username%:R"

# Connect
ssh -i "your-key.pem" ubuntu@your-ec2-public-ip
```

**Example:**
```powershell
ssh -i "trialsite-key.pem" ubuntu@54.123.45.67
```

#### 1.3. Install Java 17 on EC2

```bash
# Update system
sudo apt update
sudo apt upgrade -y

# Install Java 17
sudo apt install openjdk-17-jdk -y

# Verify
java -version
```

#### 1.4. Upload Backend to EC2

**On your local machine (PowerShell):**

```powershell
# Navigate to backend folder
cd C:\Users\reesh\VibeCoding\trialsite-app\backend

# Build the application (creates .jar file)
.\mvnw.cmd clean package -DskipTests

# Upload to EC2 (replace with your IP and key path)
scp -i "C:\path\to\your-key.pem" target\trialsite-backend-1.0.0.jar ubuntu@your-ec2-ip:/home/ubuntu/
```

#### 1.5. Run Backend on EC2

**On EC2 (SSH terminal):**

```bash
# Create directory for uploads
mkdir -p uploads

# Run the application
java -jar trialsite-backend-1.0.0.jar

# Keep it running in background (using nohup)
nohup java -jar trialsite-backend-1.0.0.jar > app.log 2>&1 &

# Verify it's running
curl http://localhost:8080/api/auth/login
```

**Your backend is now live at:** `http://your-ec2-ip:8080`

---

### **Part 2: Deploy Frontend on S3 + CloudFront**

#### 2.1. Build Frontend for Production

**On your local machine:**

```powershell
cd C:\Users\reesh\VibeCoding\trialsite-app\frontend

# Update API URL to point to EC2
# Edit src/services/*.js files
```

**Update all service files to use EC2 IP:**

Create `frontend/.env.production`:
```env
VITE_API_URL=http://your-ec2-public-ip:8080
```

Update `frontend/vite.config.js`:
```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  // Production build configuration
  build: {
    outDir: 'dist'
  }
})
```

**Create an axios config file:**

`frontend/src/config/api.js`:
```javascript
export const API_BASE_URL = import.meta.env.PROD 
  ? 'http://your-ec2-public-ip:8080'
  : 'http://localhost:8080'
```

Update all service files to use this config:
```javascript
import axios from 'axios'
import { API_BASE_URL } from '../config/api'

const API_URL = `${API_BASE_URL}/api/auth`
```

**Build the frontend:**
```powershell
npm run build
# This creates a 'dist' folder with optimized files
```

#### 2.2. Create S3 Bucket

1. **Go to S3:** AWS Console → S3 → Create bucket
2. **Configure:**
   - Bucket name: `trialsite-frontend` (must be globally unique)
   - Region: Choose closest to you (e.g., us-east-1)
   - ✅ Uncheck "Block all public access"
   - ✅ Acknowledge public access warning
3. **Create bucket**

#### 2.3. Enable Static Website Hosting

1. Go to bucket → **Properties** tab
2. Scroll to **Static website hosting** → Edit
3. Enable it:
   - Index document: `index.html`
   - Error document: `index.html` (for React routing)
4. Save changes

#### 2.4. Set Bucket Policy (Make it Public)

1. Go to **Permissions** tab
2. **Bucket Policy** → Edit
3. Paste this policy (replace `your-bucket-name`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    }
  ]
}
```

4. Save changes

#### 2.5. Upload Frontend Files

**Option A: Using AWS Console**
1. Go to **Objects** tab
2. Click **Upload**
3. Add all files from `frontend/dist` folder
4. Click **Upload**

**Option B: Using AWS CLI (Faster)**
```powershell
# Install AWS CLI first
# Configure it: aws configure

# Upload files
aws s3 sync ./dist s3://your-bucket-name/ --delete
```

**Your frontend is now live at:** `http://your-bucket-name.s3-website-us-east-1.amazonaws.com`

---

### **Part 3: Configure CORS on Backend**

Update `backend/src/main/resources/application.properties`:

```properties
# CORS Configuration - Add your S3 URL
cors.allowed-origins=http://localhost:5173,http://your-bucket-name.s3-website-us-east-1.amazonaws.com,http://your-ec2-ip:8080
```

Rebuild and redeploy backend:
```powershell
.\mvnw.cmd clean package -DskipTests
scp -i "your-key.pem" target\trialsite-backend-1.0.0.jar ubuntu@your-ec2-ip:/home/ubuntu/
```

Restart on EC2:
```bash
pkill -f trialsite-backend
nohup java -jar trialsite-backend-1.0.0.jar > app.log 2>&1 &
```

---

## 🔒 Optional: Add HTTPS & Custom Domain

### **Option 1: CloudFront (Recommended)**

1. **Create CloudFront Distribution:**
   - Origin: Your S3 bucket
   - Price class: Use only North America and Europe (cheapest)
   - Default root object: `index.html`
   - Alternate domain names: `www.yourdomain.com`
   - SSL Certificate: Request from AWS Certificate Manager (free!)

2. **Benefits:**
   - HTTPS enabled
   - Faster global access (CDN)
   - Custom domain support
   - All within free tier!

### **Option 2: Elastic Load Balancer + SSL**
- For backend HTTPS
- More expensive (not free tier)

---

## 💰 Monthly Cost Breakdown

| Resource | Free Tier | After Free Tier (12 months) |
|----------|-----------|----------------------------|
| EC2 t2.micro | Free | ~$8.50/month |
| S3 Storage (5GB) | Free | ~$0.12/month |
| CloudFront (50GB) | Free first year | ~$0.85/50GB |
| Data Transfer | 15GB free | ~$0.09/GB |
| **Total** | **$0** | **~$10-15/month** |

**Staying in Free Tier:**
- ✅ 1 EC2 t2.micro running 24/7 (750 hours/month)
- ✅ 30GB EBS storage
- ✅ 15GB data transfer out
- ✅ 1 million free requests to S3

---

## 🛠️ Alternative Options

### **Option 2: AWS Elastic Beanstalk (Easier but Less Control)**

**Pros:**
- Automatic deployment
- Load balancing built-in
- Auto-scaling
- Still free tier eligible

**Steps:**
1. Package your Spring Boot app
2. Create Elastic Beanstalk application
3. Upload JAR file
4. AWS handles the rest!

### **Option 3: AWS Amplify (Frontend Only)**

- Deploy React app directly
- Free tier: 1000 build minutes/month
- Automatic CI/CD from GitHub

---

## 🔧 Production Best Practices

### 1. **Use RDS Instead of H2**

H2 is file-based and can be lost if EC2 restarts. Upgrade to RDS PostgreSQL:

```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://your-rds-endpoint:5432/trialsite
spring.datasource.username=postgres
spring.datasource.password=your-secure-password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

**RDS Free Tier:**
- db.t2.micro instance
- 20GB storage
- 750 hours/month

### 2. **Create Systemd Service (Auto-restart)**

On EC2, create `/etc/systemd/system/trialsite.service`:

```ini
[Unit]
Description=TrialSite Solutions Backend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -jar /home/ubuntu/trialsite-backend-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl enable trialsite
sudo systemctl start trialsite
sudo systemctl status trialsite
```

### 3. **Setup Nginx as Reverse Proxy**

Install Nginx on EC2:
```bash
sudo apt install nginx -y
```

Configure `/etc/nginx/sites-available/trialsite`:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Enable:
```bash
sudo ln -s /etc/nginx/sites-available/trialsite /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 4. **Setup Monitoring**

**CloudWatch (Free Tier):**
- 10 custom metrics
- 10 alarms
- 1 million API requests

Enable detailed monitoring on EC2 to track:
- CPU usage
- Memory usage
- Disk usage

---

## 📝 Quick Deployment Checklist

- [ ] Create AWS account (Free Tier)
- [ ] Launch EC2 t2.micro instance
- [ ] Install Java 17 on EC2
- [ ] Build backend JAR file
- [ ] Upload backend to EC2
- [ ] Start backend service
- [ ] Create S3 bucket
- [ ] Enable static website hosting
- [ ] Build frontend with production config
- [ ] Upload frontend to S3
- [ ] Update CORS configuration
- [ ] Test the application
- [ ] (Optional) Setup CloudFront for HTTPS
- [ ] (Optional) Migrate to RDS PostgreSQL
- [ ] (Optional) Setup auto-restart with systemd

---

## 🆘 Troubleshooting

### Backend not accessible:
```bash
# Check if running
ps aux | grep java

# Check logs
tail -f app.log

# Check port
netstat -tulpn | grep 8080
```

### Frontend can't connect to backend:
1. Check EC2 security group allows port 8080
2. Verify CORS settings in `application.properties`
3. Check browser console for errors

### Database data lost after restart:
- Migrate to RDS or
- Store H2 data on EBS volume

---

## 🎉 You're Ready!

After following this guide, you'll have:
- ✅ Production-ready deployment
- ✅ Scalable architecture
- ✅ Mostly free (first 12 months)
- ✅ Professional setup

**Next Steps:** Add custom domain, HTTPS, monitoring, and backups!
