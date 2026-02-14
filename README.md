# Smart Pothole Detection System 🚧

Smart Pothole is an **AI-powered road safety platform** that connects citizens and government authorities through an **automated, transparent complaint lifecycle**. The system ensures that only **AI-verified pothole complaints** enter the workflow, reducing false reports and improving response efficiency.

---

## 🔍 Problem Statement

Road potholes are a major cause of:

* Accidents and vehicle damage
* Delayed complaint resolution
* Lack of accountability between citizens and authorities

Traditional complaint systems:

* Accept unverified complaints
* Have no structured tracking
* Offer poor transparency

---

## 💡 Solution Overview

Smart Pothole addresses these gaps by combining:

* **AI-based pothole detection**
* **Secure backend APIs**
* **Role-based Android application**
* **End-to-end complaint tracking**

Only complaints validated by the AI model proceed into the system, ensuring data quality and operational efficiency.

---

## 🧭 System Workflow

### 👤 Citizen (User) Side

1. User uploads a road image using the Android app.
2. The AI model analyzes the image to detect potholes.
3. If the confidence score is above the threshold:

   * The user fills complaint details.
   * Selects the exact pothole location on the map.
4. The system generates a **unique complaint ID**.
5. Confirmation is sent to the user.

If AI confidence is low, the complaint is rejected to prevent false reporting.

---

### 🏛️ Authority Side

1. Officials log in using **JWT-based secure authentication**.
2. Authorities can:

   * View all complaints
   * Filter and search by status, date, or location
3. Officials inspect:

   * Image evidence
   * Complaint metadata
4. Actions available:

   * Update complaint status
   * Assign officers
   * Track resolution progress
5. Every action is logged for **accountability and transparency**.

---

## 🧠 AI & ML Component

* Uses a **CNN-based pothole detection model**
* Model outputs confidence score for each image
* Threshold-based validation ensures reliability
* Reduces false positives in complaint submission

---

## 🏗️ Project Structure

```
smart-pothole-system/
├── backend/          # FastAPI backend + ML model
├── android-app/      # Android application (User & Authority)
├── screenshots/      # App UI screenshots
├── README.md         # Project documentation
└── .gitignore
```

---

## ⚙️ Tech Stack

### Backend

* Python
* FastAPI
* JWT Authentication
* TensorFlow / Keras
* Uvicorn

### Frontend

* Android (Kotlin)
* Google Maps API

### Machine Learning

* Convolutional Neural Network (CNN)
* Image classification for pothole detection

---

## ▶️ Running the Backend Locally

```bash
cd backend
uvicorn api:app --host 0.0.0.0 --port 8000
```

> Update the backend IP address in the Android app before running.

---

## 🔐 Security Features

* JWT-based authentication for authorities
* Role-based access control
* Secure API endpoints
* Action logging for traceability

---

## 🚀 Key Highlights

* AI-verified complaints only
* Reduced fake or duplicate reports
* Transparent complaint lifecycle
* Structured authority workflow
* Scalable and modular architecture

---

## 📌 Future Enhancements

* Cloud deployment of backend
* Real-time complaint notifications
* Officer mobile app
* Dashboard analytics
* Automated severity prioritization

---

## 👨‍💻 Author

**Om Pisal**
Final Year Engineering Student (AI & Data Science)

---

## 📸 Screenshots

### 👤 User Flow

**Login Screen**
![Login](screenshots/login.png)

**Home / Dashboard**
![Home](screenshots/home.png)

**Upload Pothole Image**
![Upload](screenshots/upload_image.png)

**Select Location on Map**
![Map](screenshots/map_location.png)

---

### 🏛️ Authority Flow

**Authority Dashboard**
![Authority Dashboard](screenshots/authority_dashboard.png)

**Complaint Details & Status Update**
![Complaint Status](screenshots/complaint_status.png)
