#!/usr/bin/env python3
import http.server
import socketserver
import os
import json
import urllib.parse

PORT = 8088
DIRECTORY = "/Users/vishalkumar/Desktop/vishika"
DEBUG_APK_DIR = os.path.join(DIRECTORY, "app/build/outputs/apk/debug")

class UpdateHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        super().end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == '/api/debug-apk':
            # Check for APK in debug folder
            default_apk = os.path.join(DEBUG_APK_DIR, "app-debug.apk")
            chosen_file = None
            if os.path.isfile(default_apk):
                chosen_file = default_apk
            elif os.path.isdir(DEBUG_APK_DIR):
                apk_files = [os.path.join(DEBUG_APK_DIR, f) for f in os.listdir(DEBUG_APK_DIR) if f.endswith(".apk")]
                if apk_files:
                    apk_files.sort(key=lambda x: os.path.getmtime(x), reverse=True)
                    chosen_file = apk_files[0]

            if chosen_file and os.path.isfile(chosen_file):
                stat = os.stat(chosen_file)
                data = {
                    "exists": True,
                    "filename": os.path.basename(chosen_file),
                    "sizeBytes": stat.st_size,
                    "modifiedTime": int(stat.st_mtime * 1000),
                    "path": chosen_file,
                    "downloadUrl": "/api/download-debug-apk"
                }
                body = json.dumps(data).encode('utf-8')
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            else:
                body = json.dumps({"exists": False}).encode('utf-8')
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            return

        elif parsed.path == '/api/download-debug-apk':
            default_apk = os.path.join(DEBUG_APK_DIR, "app-debug.apk")
            chosen_file = None
            if os.path.isfile(default_apk):
                chosen_file = default_apk
            elif os.path.isdir(DEBUG_APK_DIR):
                apk_files = [os.path.join(DEBUG_APK_DIR, f) for f in os.listdir(DEBUG_APK_DIR) if f.endswith(".apk")]
                if apk_files:
                    apk_files.sort(key=lambda x: os.path.getmtime(x), reverse=True)
                    chosen_file = apk_files[0]

            if chosen_file and os.path.isfile(chosen_file):
                stat = os.stat(chosen_file)
                self.send_response(200)
                self.send_header('Content-Type', 'application/vnd.android.package-archive')
                self.send_header('Content-Disposition', f'attachment; filename="{os.path.basename(chosen_file)}"')
                self.send_header('Content-Length', str(stat.st_size))
                self.end_headers()
                with open(chosen_file, 'rb') as f:
                    while chunk := f.read(65536):
                        self.wfile.write(chunk)
            else:
                self.send_error(404, "APK not found")
            return

        super().do_GET()

if __name__ == '__main__':
    with socketserver.TCPServer(("", PORT), UpdateHandler) as httpd:
        print(f"Vishika Updater Server running at http://localhost:{PORT}/update_dashboard.html")
        httpd.serve_forever()
