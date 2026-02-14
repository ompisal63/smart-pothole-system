from dotenv import load_dotenv
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(dotenv_path=BASE_DIR / ".env")

from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
import cv2
import tensorflow as tf

from app.routes.complaint import router as complaint_router
from app.routes.auth import router as auth_router


app = FastAPI()

app.include_router(complaint_router)
app.include_router(auth_router)



# Allow Android app to call this API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load your trained model 
model = tf.keras.models.load_model("smart_pothole_model.h5")
print("🔥 USING MODEL FILE:", model.name)

IMG_SIZE = 128

def preprocess_image(image_bytes):
    np_img = np.frombuffer(image_bytes, np.uint8)
    img = cv2.imdecode(np_img, cv2.IMREAD_COLOR)
    img = cv2.resize(img, (IMG_SIZE, IMG_SIZE))
    img = img.astype("float32") / 255.0
    img = np.expand_dims(img, axis=0)
    return img


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    print("✅ API HIT")

    image_bytes = await file.read()
    print("📦 Image bytes length:", len(image_bytes))

    img = preprocess_image(image_bytes)
    print("🖼 Image shape:", img.shape)

    prediction = model.predict(img)
    print("🤖 Raw prediction:", prediction)

    confidence = float(prediction[0][0])
    print("📊 Confidence:", confidence)

    return {
        "confidence": confidence,
        "is_pothole": confidence >= 0.93
    }
