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
    dept_code = f"D{suffix[-6:]}"
    dept_name = f"行政部{suffix[-4:]}"
    room_no = f"A-{suffix[-3:]}"
    emp_no = f"E{suffix[-6:]}"
    emp_name = f"员工{suffix[-4:]}"

    status, body = request("GET", "/health")
    print_result("health", status, body)

    status, body = request(
        "POST",
        "/departments",
        {"deptCode": dept_code, "deptName": dept_name, "remark": "测试"},
    )
    print_result("create_department", status, body)
    dept_id = extract_id(body)
    if dept_id is None:
        print("create_department failed, skip follow-up tests")
        return

    status, body = request(
        "POST",
        "/locations",
        {"deptId": dept_id, "roomNo": room_no, "area": 60.5, "remark": "测试"},
    )
    print_result("create_location", status, body)
    location_id = extract_id(body)
    if location_id is None:
        print("create_location failed, skip follow-up tests")
        return

    status, body = request(
        "POST",
        "/assignees",
        {"empNo": emp_no, "name": emp_name, "phone": "13800000000", "remark": "测试"},
    )
    print_result("create_assignee", status, body)
    assignee_id = extract_id(body)
    if assignee_id is None:
        print("create_assignee failed, skip assign/return tests")

    asset_no = f"AS{suffix[-6:]}"
    asset_name = f"笔记本{suffix[-4:]}"

    status, body = request(
        "POST",
        "/assets",
        {
            "assetNo": asset_no,
            "assetName": asset_name,
            "value": 8000,
            "locationId": location_id,
            "assigneeId": None,
            "remark": "测试",
        },
    )
    print_result("create_asset", status, body)
    asset_id = extract_id(body)
    if asset_id is None:
        print("create_asset failed, skip follow-up tests")
        return

    followups = [
        ("get_asset", "GET", f"/assets/{asset_id}", None),
        ("list_assets", "GET", "/assets?page=1&size=10", None),
        (
            "list_assets_filter",
            "GET",
            f"/assets?keyword={asset_no}&deptId={dept_id}&locationId={location_id}&status=0&page=1&size=10",
            None,
        ),
        (
            "update_asset",
            "PUT",
            f"/assets/{asset_id}",
            {
                "assetNo": f"{asset_no}-更新",
                "assetName": f"{asset_name}-更新",
                "value": 9000.5,
                "locationId": location_id,
                "assigneeId": None,
                "remark": "更新",
            },
        ),
        ("get_asset_after_update", "GET", f"/assets/{asset_id}", None),
    ]

    for name, method, path, payload in followups:
        status, body = request(method, path, payload)
        print_result(name, status, body)

    if assignee_id is not None:
        status, body = request(
            "POST", f"/assets/{asset_id}/assign", {"assigneeId": assignee_id}
        )
        print_result("assign_asset", status, body)

        status, body = request("GET", f"/assets/{asset_id}")
        print_result("get_asset_after_assign", status, body)

        status, body = request("POST", f"/assets/{asset_id}/return")
        print_result("return_asset", status, body)

        status, body = request("GET", f"/assets/{asset_id}")
        print_result("get_asset_after_return", status, body)

    status, body = request("DELETE", f"/assets/{asset_id}")
    print_result("delete_asset", status, body)

    status, body = request("GET", f"/assets/{asset_id}")
    print_result("get_asset_after_delete", status, body)

    status, body = request("DELETE", f"/locations/{location_id}")
    print_result("delete_location", status, body)

    status, body = request("DELETE", f"/departments/{dept_id}")
    print_result("delete_department", status, body)

    if assignee_id is not None:
        status, body = request("DELETE", f"/assignees/{assignee_id}")
        print_result("delete_assignee", status, body)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        sys.exit(1)
