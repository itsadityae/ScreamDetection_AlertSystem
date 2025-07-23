import requests
import sounddevice as sd
import numpy as np
import librosa
import tensorflow as tf
from twilio.rest import Client

TWILIO_ACCOUNT_SID = "*********************************************"  # Replace with your Twilio Account SID
TWILIO_AUTH_TOKEN = "*********************************************"  # Replace with your Twilio Auth Token
TWILIO_PHONE_NUMBER = "+1**********"  # Replace with your Twilio phone number
ALERT_PHONE_NUMBER = "+91***********"  # Replace with your phone number

# Replace with your actual Google API key
GOOGLE_API_KEY = "*********************************************"


model = tf.keras.models.load_model(r"C:\Users\adity\Desktop\Projects\Screamdetect\Website\audio_classification_model.h5")

SAMPLE_RATE = 22050
DURATION = 1
BUFFER_SIZE = SAMPLE_RATE * DURATION

def get_location():
    try:
        url = f"https://www.googleapis.com/geolocation/v1/geolocate?key={GOOGLE_API_KEY}"
        response = requests.post(url, json={})
        data = response.json()

        if "location" in data:
            lat, lng = data["location"]["lat"], data["location"]["lng"]

            # Get address using Reverse Geocoding
            geocode_url = f"https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={GOOGLE_API_KEY}"
            geocode_response = requests.get(geocode_url).json()

            if geocode_response["status"] == "OK":
                address = geocode_response["results"][0]["formatted_address"]
            else:
                address = "Address not found"

            # Google Maps link for easy tracking
            maps_link = f"https://www.google.com/maps/search/?api=1&query={lat},{lng}"

            return f"{address}\n🌍 Google Maps: {maps_link}"

        return "Location not available"
    
    except Exception as e:
        return f"Error getting location: {e}"


def send_sms_alert():
    location = get_location()
    message_body = f"🚨 ALERT: Scream detected!\nAditya is in Danger!\n📍 Location: {location}"
    
    client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
    message = client.messages.create(
        body=message_body,
        from_=TWILIO_PHONE_NUMBER,
        to=ALERT_PHONE_NUMBER
    )
    print(f"SMS sent! Message SID: {message.sid}")

def extract_features_from_audio(audio):
    mfccs = librosa.feature.mfcc(y=audio, sr=SAMPLE_RATE, n_mfcc=40)
    return np.mean(mfccs.T, axis=0)

# Callback function to process live audio
def callback(indata, frames, time, status):
    if status:
        print(status)

    audio_data = np.squeeze(indata)

    features = extract_features_from_audio(audio_data)
    features = np.expand_dims(features, axis=0)

    prediction = model.predict(features)
    
    label = "SCREAM DETECTED!" if prediction[0][0] > 0.5 else "No scream"
    confidence = float(prediction[0][0])

    print(f"Prediction: {label} | Confidence: {confidence:.2f}")

    if prediction[0][0] > 0.5:
        send_sms_alert()

# Start real-time audio processing
def start_realtime_detection():
    with sd.InputStream(callback=callback, samplerate=SAMPLE_RATE, channels=1, blocksize=BUFFER_SIZE):
        print("Listening for screams... Press Enter to stop.")
        input()

# Run the real-time detection
if __name__ == "__main__":
    start_realtime_detection()
