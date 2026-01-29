from flask import Flask, request, jsonify
import requests
import os

app = Flask(__name__)

LOCAL_BACKEND = os.getenv("LOCAL_BACKEND_URL", "http://127.0.0.1:5000")

@app.route('/', defaults={'path': ''})
@app.route('/<path:path>')
def proxy_all(path):
    # 转发所有路径到本地后端
    url = f"{LOCAL_BACKEND}/{path}"
    resp = requests.request(
        method=request.method,
        url=url,
        headers={k: v for k, v in request.headers if k.lower() != 'host'},
        data=request.get_data(),
        params=request.args,
        cookies=request.cookies,
        timeout=120
    )
    excluded_headers = ['content-encoding', 'content-length', 'transfer-encoding', 'connection']
    headers = [(k, v) for k, v in resp.headers.items() if k.lower() not in excluded_headers]
    return (resp.content, resp.status_code, headers)

@app.route('/gateway-health')
def gateway_health():
    return "Gateway OK", 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)