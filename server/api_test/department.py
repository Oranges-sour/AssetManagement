import json
import sys
import time
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


def parse_json(body):
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        return None


def main():
    tests = []

    tests.append(("health", "GET", "/health", None))
    suffix = str(int(time.time()))
    dept_code = f"D{suffix[-6:]}"
    dept_name = f"行政部{suffix[-4:]}"
    tests.append(
        (
            "create_department",
            "POST",
            "/departments",
            {"deptCode": dept_code, "deptName": dept_name, "remark": "测试"},
        )
    )

    created_id = None
    for name, method, path, payload in tests:
        status, body = request(method, path, payload)
        print_result(name, status, body)
        if name == "create_department":
            data = parse_json(body)
            if data and isinstance(data.get("data"), dict):
                created_id = data["data"].get("id")

    if created_id is None:
        print("create_department failed, skip follow-up tests")
        return

    print(f"id={created_id}")

    followups = [
        ("get_department", "GET", f"/departments/{created_id}", None),
        ("list_departments", "GET", "/departments?page=1&size=10", None),
        (
            "update_department",
            "PUT",
            f"/departments/{created_id}",
            {
                "deptCode": dept_code,
                "deptName": f"{dept_name}-更新",
                "remark": "更新",
            },
        ),
        ("get_department_after_update", "GET", f"/departments/{created_id}", None),
        ("delete_department", "DELETE", f"/departments/{created_id}", None),
        ("get_department_after_delete", "GET", f"/departments/{created_id}", None),
    ]

    for name, method, path, payload in followups:
        status, body = request(method, path, payload)
        print_result(name, status, body)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
