import tensorflow as tf

# Load the trained model
model = tf.keras.models.load_model(r"C:\Users\adity\Desktop\Screamdetect\Website\audio_classification_model.h5")

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the TFLite model
with open("scream_detection.tflite", "wb") as f:
    f.write(tflite_model)

print("Model converted successfully!")
