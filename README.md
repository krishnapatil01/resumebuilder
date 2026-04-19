🚀 AutoResume AI
Smart Resume Builder, Analyzer & Job Tracking System

AutoResume AI is a full-stack web application built using Java Spring Boot that helps users create professional resumes, analyze them against job descriptions, and track job applications with data-driven insights.

It combines resume building + ATS optimization + job tracking + consistency analysis in one platform.

🌐 Live Demo:
👉 https://resumebuilder-jmm2.onrender.com/

🎯 Problem Statement

Job seekers often struggle with:

Creating ATS-friendly resumes
Understanding how well their resume matches a job
Tracking multiple job applications
Maintaining consistency in job applications

AutoResume AI solves all of these in one system.

✨ Key Features
📝 Resume Builder
Form-based resume creation
Structured sections:
Personal Details
Education
Skills
Experience
Projects
Achievements
Download/export resume functionality
📄 Resume Upload & Optimization
Upload resumes (PDF/DOCX)
Extract content from resume
Get suggestions to improve ATS compatibility
📊 Job Description Analysis
Paste job description
Used for intelligent comparison with resume
🤖 Resume–Job Matching Engine
Calculates Fit Score (%)
Detects Missing Skills
Highlights:
✅ Matching keywords
❌ Missing keywords
📌 Job Application Tracker

Manage all job applications in one place.

Fields include:

Company Name
Role
Job Description
Platform (LinkedIn, Naukri, etc.)
Status (Applied / Interview / Rejected / Offer)
Date

Auto Processing:

Fit score calculation
Missing skills storage
📈 Analytics Dashboard
Total Applications
Average Fit Score
Interview Rate
Weekly Activity
Skill Gap Trends
🔥 Consistency Tracking
Tracks job application frequency
Shows:
Weekly activity
Gaps in applications
Streak tracking
💡 Smart Suggestions
Recommends skills to improve
Based on:
Resume gaps
Job descriptions
🛠️ Tech Stack
Backend
Java
Spring Boot
Spring MVC
REST APIs
Frontend
HTML
CSS
JavaScript
Database
H2 Database (File-based persistent storage)
🧠 Core Logic
Keyword Matching Algorithm for Fit Score
Skill Extraction from Resume & JD
Missing Skill Identification
Resume Optimization Suggestions
🗂️ System Architecture (High-Level)
Frontend (HTML/CSS/JS)
        ↓
Spring Boot Backend (REST APIs)
        ↓
Business Logic (Resume Analysis Engine)
        ↓
H2 Database (Persistent Storage)
🔌 API Overview
Method	Endpoint	Description
POST	/applications	Add job application
GET	/applications	Get all applications
GET	/dashboard	Dashboard statistics
POST	/analyze	Resume & JD analysis
🚀 Getting Started
1️⃣ Clone Repository
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
2️⃣ Run Application
mvn spring-boot:run
3️⃣ Access Application
http://localhost:8080
🔑 H2 Database Setup

Access H2 Console:

http://localhost:8080/h2-console
Configuration:
JDBC URL:
jdbc:h2:file:./data/resumedb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
Username: sa
Password: (leave empty)
💡 Important Note
Uses file-based H2 database
Data is persisted across server restarts
Database files stored in /data folder
📊 Use Cases
Students building their first resume
Freshers preparing for placements
Job seekers tracking applications
Improving resume-job match accuracy
🚀 Future Enhancements
User authentication & login system
Advanced NLP (semantic matching)
Resume auto-improvement suggestions
Email/job alerts integration
Migration to MySQL/PostgreSQL
Interview preparation module
👨‍💻 Author

Krishna Patil,
Shruti Ghadge,
Mona More
📍 Pune, Maharashtra

⭐ Support

If you found this project useful:

⭐ Star this repository
🍴 Fork and contribute
📢 Share with others
