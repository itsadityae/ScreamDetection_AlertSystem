🚨 Scream Detection and Alert System

An AI-powered Android app that detects human screams in real-time and sends emergency alerts with location info to predefined contacts. Built using CNN, MFCC features, and Twilio for enhanced personal safety.

📱 Key Features

🎤 Real-time scream detection using a trained CNN model

📍 Sends emergency alerts with location via Twilio & GPS

🎛️ Trained on noise-augmented public datasets (Kaggle, GitHub)

📊 Achieved 84%+ validation accuracy, 85% precision

📱 Fully built Android app with Java & Android Studio

🔐 Focus on real-world usability, privacy, and reliability

📊 Model Performance

Metric	Value

Training Accuracy	88.60%
Validation Accuracy	83.98%
Precision	85.20%
Recall	82.50%
F1-Score	83.80%

🧪 How It Works

📂 Audio samples are preprocessed (noise reduction + MFCCs)

🧠 CNN model classifies audio as scream or non-scream

📡 If scream is detected:

Location is fetched via GPS/Geolocation API

Emergency SMS sent via Twilio API


🛠 Tech Stack

ML & Audio: TensorFlow, MFCC (Librosa), Python

Backend: Twilio API, Google Geolocation API

Mobile App: Java, Android Studio

Deployment: Android device



🌱 Future Enhancements

🔋 Battery optimization for background audio monitoring

🔊 Advanced noise filtering with deep learning

⌚ Integration with smartwatches and wearables

🌐 Multilingual & regional scream detection

🆘 Direct alert integration with emergency services

🔗 Live Demo

🌐 [ScreamGuard Website](https://screamguard.netlify.app)

👤 Author

Aditya
Computer Science Student | Developer 
