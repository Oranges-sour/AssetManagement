import json
import sys
import urllib.error
import urllib.request

BASE_URL = "http://localhost:8080/orange/api"


def request(method, path, payload=None):
    url = f"{BASE_URL}{path}"
    data = None
    headers = {"Content-Type": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, body
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8")


def print_result(name, status, body):
    print(f"[{name}] HTTP {status}")
    print(body)
    print("-")


def main():
    tests = []

    tests.append(("health", "GET", "/health", None))
    tests.append((
        "create_department",
        "POST",
        "/departments",
        {"deptCode": "D001", "deptName": "行政部", "remark": "测试"},
    ))

    for name, method, path, payload in tests:
        status, body = request(method, path, payload)
        print_result(name, status, body)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
