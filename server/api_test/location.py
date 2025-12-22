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

    dept_id = None
    for name, method, path, payload in tests:
        status, body = request(method, path, payload)
        print_result(name, status, body)
        if name == "create_department":
            data = parse_json(body)
            if data and isinstance(data.get("data"), dict):
                dept_id = data["data"].get("id")

    if dept_id is None:
        print("create_department failed, skip follow-up tests")
        return

    room_no = f"A-{suffix[-3:]}"
    create_payload = {
        "deptId": dept_id,
        "roomNo": room_no,
        "area": 60.5,
        "remark": "测试",
    }

    status, body = request("POST", "/locations", create_payload)
    print_result("create_location", status, body)
    data = parse_json(body)
    location_id = None
    if data and isinstance(data.get("data"), dict):
        location_id = data["data"].get("id")

    if location_id is None:
        print("create_location failed, skip follow-up tests")
        return

    followups = [
        ("get_location", "GET", f"/locations/{location_id}", None),
        ("list_locations", "GET", "/locations?page=1&size=10", None),
        ("list_locations_filter", "GET", f"/locations?deptId={dept_id}&keyword={room_no}&page=1&size=10", None),
        (
            "update_location",
            "PUT",
            f"/locations/{location_id}",
            {
                "deptId": dept_id,
                "roomNo": f"{room_no}-更新",
                "area": 88.8,
                "remark": "更新",
            },
        ),
        ("get_location_after_update", "GET", f"/locations/{location_id}", None),
        ("delete_location", "DELETE", f"/locations/{location_id}", None),
        ("get_location_after_delete", "GET", f"/locations/{location_id}", None),
        ("delete_department", "DELETE", f"/departments/{dept_id}", None),
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
