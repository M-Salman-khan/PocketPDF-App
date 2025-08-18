from flask import Flask, request, send_file, jsonify
import os
import subprocess
import platform
from werkzeug.utils import secure_filename

app = Flask(__name__)
UPLOAD_FOLDER = "uploads"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ✅ Cross-platform Ghostscript command
def get_ghostscript_cmd():
    if platform.system() == "Windows":
        return "C:\\Program Files\\gs\\gs10.05.1\\bin\\gswin64c.exe"
    else:
        return "gs"  # Works on Linux/Render

# ✅ Compression function
def compress_pdf_ghostscript(input_path, output_path, quality="screen"):
    gs_cmd = get_ghostscript_cmd()
    command = [
        gs_cmd,
        "-sDEVICE=pdfwrite",
        "-dCompatibilityLevel=1.4",
        f"-dPDFSETTINGS=/{quality}",
        "-dNOPAUSE",
        "-dQUIET",
        "-dBATCH",
        f"-sOutputFile={output_path}",
        input_path
    ]
    try:
        # Set timeout in seconds (e.g., 90s)
        subprocess.run(command, check=True, timeout=90)
    except subprocess.TimeoutExpired:
        raise RuntimeError("PDF compression timed out. Try a smaller file or lower quality.")
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Ghostscript failed: {e}")
@app.route("/")
def index():
    try:
        return "Server is currently in Running state ✅"
    except Exception as err:
        return f"An error occured {err}", 500
# ✅ API endpoint to compress PDF
@app.route("/compress", methods=["POST"])
def compress_pdf():
    if 'pdf' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['pdf']
    quality = request.form.get("quality", "ebook")

    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    filename = secure_filename(file.filename)
    input_path = os.path.join(UPLOAD_FOLDER, filename)
    output_filename = f"compressed_{quality}_{filename}"
    output_path = os.path.join(UPLOAD_FOLDER, output_filename)

    file.save(input_path)

    try:
        compress_pdf_ghostscript(input_path, output_path, quality)
        return send_file(output_path, as_attachment=True, download_name=output_filename)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ✅ App entrypoint
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=10000, debug=True)
