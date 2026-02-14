import tensorflow as tf

_model = None

def load_model():
    global _model
    if _model is None:
        _model = tf.keras.models.load_model("smart_pothole_model.h5")
    return _model
