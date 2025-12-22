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


def extract_id(body):
    data = parse_json(body)
    if data and isinstance(data.get("data"), dict):
        return data["data"].get("id")
    return None


def main():
    suffix = str(int(time.time()))
    emp_no = f"E{suffix[-6:]}"
    emp_name = f"员工{suffix[-4:]}"

    status, body = request("GET", "/health")
    print_result("health", status, body)

    status, body = request(
        "POST",
        "/assignees",
        {"empNo": emp_no, "name": emp_name, "phone": "13800000000", "remark": "测试"},
    )
    print_result("create_assignee", status, body)
    assignee_id = extract_id(body)
    if assignee_id is None:
        print("create_assignee failed, skip follow-up tests")
        return

    followups = [
        ("get_assignee", "GET", f"/assignees/{assignee_id}", None),
        ("list_assignees", "GET", "/assignees?page=1&size=10", None),
        ("list_assignees_filter", "GET", f"/assignees?keyword={emp_no}&page=1&size=10", None),
        (
            "update_assignee",
            "PUT",
            f"/assignees/{assignee_id}",
            {"empNo": emp_no, "name": f"{emp_name}-更新", "phone": "", "remark": "更新"},
        ),
        ("get_assignee_after_update", "GET", f"/assignees/{assignee_id}", None),
        ("list_assets_by_assignee", "GET", f"/assignees/{assignee_id}/assets?page=1&size=10", None),
        ("delete_assignee", "DELETE", f"/assignees/{assignee_id}", None),
        ("get_assignee_after_delete", "GET", f"/assignees/{assignee_id}", None),
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
