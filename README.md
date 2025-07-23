📢 Scream Detection and Alert System
A real-time Android application that detects human screams using a CNN-based machine learning model and instantly sends emergency alerts with precise location details. Built for personal safety and rapid response in noisy, real-world environments.

🚀 Features
🎙️ Scream Detection using a CNN trained on MFCC audio features

📈 High Accuracy:

Training Accuracy: 88.60%

Validation Accuracy: 83.98%

Precision: 85.20%, Recall: 82.50%, F1-score: 83.80%

🌍 Location Tracking via Google Geolocation API or device GPS

📲 Real-time Alerts to emergency contacts using Twilio API

📱 Android App built using Java & Android Studio

🔒 Privacy-focused with scope for secure data handling

🧪 Testing & Evaluation
Trained on publicly available scream/non-scream datasets from Kaggle and GitHub

Data augmented with real-world environmental noise

80:20 train-validation split

Audio features extracted using MFCC

Training run for 50 epochs using Adam Optimizer with early stopping

Evaluation metrics: Accuracy, Precision, Recall, and F1-score

Visualizations of model accuracy/loss trends across epochs confirm strong generalization

🛠️ Tech Stack
Programming: Python, Java

Libraries & Tools: TensorFlow, Librosa, Android Studio, Twilio API, Google Geolocation API

Platform: Android (Mobile Application)

📱 Application Preview
📎 Live Demo Website (ScreamGuard)

🔮 Future Scope
🔋 Battery-efficient continuous audio monitoring

🔊 Advanced noise filtering (e.g., deep learning-based denoisers)

⌚ Smartwatch and wearable integration

🔐 End-to-end encryption for location/audio data

🌐 Multi-language scream recognition

🆘 Direct integration with emergency services

