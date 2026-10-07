"""Loopback-only PaddleOCR service used by FinBridge's local bill provider."""
import os
import tempfile
from pathlib import Path
from flask import Flask, jsonify, request
from paddleocr import PaddleOCR

app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 5 * 1024 * 1024

# Vietnamese is supported by PaddleOCR's multilingual recognition pipeline.
ocr = PaddleOCR(lang="vi", ocr_version="PP-OCRv3", use_doc_orientation_classify=False,
                use_doc_unwarping=False, use_textline_orientation=False)

@app.get("/health")
def health():
    return jsonify({"status": "ok", "engine": "PaddleOCR", "language": "vi"})

@app.post("/api/ocr")
def recognize():
    uploaded = request.files.get("file")
    if uploaded is None or not uploaded.filename:
        return jsonify({"message": "Choose an image first."}), 400
    suffix = Path(uploaded.filename).suffix.lower()
    if uploaded.mimetype not in {"image/jpeg", "image/png", "image/webp"} and suffix not in {".jpg", ".jpeg", ".png", ".webp"}:
        return jsonify({"message": "Use a JPG, PNG or WEBP image."}), 400
    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(prefix="finbridge-ocr-", suffix=suffix, delete=False) as temp:
            temp_path = temp.name
            uploaded.save(temp)
        results = ocr.predict(temp_path)
        lines, scores = [], []
        for item in results:
            payload = item.json
            payload = payload() if callable(payload) else payload
            data = payload.get("res", payload) if isinstance(payload, dict) else {}
            lines.extend(text.strip() for text in data.get("rec_texts", []) if text and text.strip())
            scores.extend(float(score) for score in data.get("rec_scores", []) if score is not None)
        confidence = sum(scores) / len(scores) if scores else 0.0
        return jsonify({"text": "\n".join(lines)[:30000], "confidence": confidence})
    except Exception:
        # Never return local paths, stack traces, or uploaded contents to the caller.
        return jsonify({"message": "PaddleOCR could not process the image. Try a clearer JPG, PNG or WEBP."}), 503
    finally:
        if temp_path:
            try: os.unlink(temp_path)
            except OSError: pass

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=int(os.environ.get("BILL_OCR_PORT", "8099")), threaded=False)
