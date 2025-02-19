import http.server
import socketserver
import sys
import subprocess
import threading
import os
import time


def run_server(file_path):
    class CustomHandler(http.server.SimpleHTTPRequestHandler):
        def do_GET(self):
            if self.path == '/file.lua':
                try:
                    with open(file_path, 'rb') as file:
                        self.send_response(200)
                        self.send_header("Content-type", "application/octet-stream")
                        self.end_headers()
                        self.wfile.write(file.read())
                        print("File served successfully")
                        # Delay shutdown slightly to ensure response is sent
                        threading.Timer(1.0, httpd.shutdown).start()
                except Exception as e:
                    print(f"Error serving file: {e}")
                    self.send_error(500, f"Internal server error: {str(e)}")
            else:
                self.send_error(404, "File not found")

        def log_message(self, format, *args):
            # Suppress default logging
            pass

    PORT = 8000
    Handler = CustomHandler

    try:
        with socketserver.TCPServer(("0.0.0.0", PORT), Handler) as httpd:
            print(f"Server started successfully at http://localhost:{PORT}/file.lua")
            httpd.serve_forever()
    except Exception as e:
        print(f"Server error: {e}")
        raise


def send_deep_link():
    try:
        # First check if adb is available
        subprocess.run(["adb", "devices"], check=True, capture_output=True)
        
        deep_link = "trackandgraph://lua_inject?url=http://10.0.2.2:8000/file.lua"
        result = subprocess.run(
            ["adb", "shell", "am", "start", "-a",
             "android.intent.action.VIEW", "-d", deep_link],
            check=True,
            capture_output=True,
            text=True
        )
        print(f"Deep link sent successfully: {deep_link}")
        if result.stderr:
            print(f"ADB output: {result.stderr}")
    except subprocess.CalledProcessError as e:
        print(f"Failed to send deep link: {e}")
        print(f"ADB output: {e.stderr}")
        raise
    except FileNotFoundError:
        print("Error: 'adb' command not found. Make sure Android SDK platform tools are installed and in your PATH")
        raise


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python serve-and-deep-link-lua.py <file_path>")
        sys.exit(1)

    file_path = sys.argv[1]
    
    # Verify file exists and is readable
    if not os.path.exists(file_path):
        print(f"Error: File '{file_path}' does not exist")
        sys.exit(1)
    
    try:
        with open(file_path, 'rb') as f:
            pass
    except Exception as e:
        print(f"Error: Cannot read file '{file_path}': {e}")
        sys.exit(1)

    print(f"Starting server for file: {file_path}")
    
    server_thread = threading.Thread(target=run_server, args=(file_path,))
    server_thread.start()

    # Wait a moment for server to start
    time.sleep(1)

    try:
        send_deep_link()
    except Exception as e:
        print("Deep link injection failed. Shutting down server...")
        sys.exit(1)
