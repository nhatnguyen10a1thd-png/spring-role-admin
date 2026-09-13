"""Verify the packaged WAR, its JSPs and context-path URLs with a real JVM.

Run after Maven package: python scripts/smoke_war.py
Only uses Python's standard library. The temporary server and in-memory database
are isolated from the application's normal demo/SQL Server data.
"""

import argparse
from html.parser import HTMLParser
from http.cookiejar import CookieJar
from pathlib import Path
import socket
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request


class Inputs(HTMLParser):
    def __init__(self):
        super().__init__()
        self.values = {}

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "input" and attrs.get("name"):
            self.values[attrs["name"]] = attrs.get("value", "")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--war", default="target/spring_admin-0.0.1-SNAPSHOT.war")
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    war = Path(args.war).resolve(strict=True)
    working = war.parent / "war-smoke"
    working.mkdir(exist_ok=True)
    with socket.socket() as available:
        available.bind(("127.0.0.1", 0))
        port = available.getsockname()[1]
    base = f"http://127.0.0.1:{port}/smoke"
    client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(CookieJar()))

    def get(path):
        with client.open(base + path, timeout=15) as response:
            assert response.status == 200, (path, response.status)
            return response.read().decode("utf-8")

    log_path = working / "server.log"
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(
            [args.java, "-jar", str(war), "--spring.profiles.active=demo",
             f"--server.port={port}", "--server.address=127.0.0.1",
             "--server.servlet.context-path=/smoke",
             "--spring.datasource.url=jdbc:h2:mem:war-smoke",
             "--spring.jpa.hibernate.ddl-auto=create-drop",
             "--app.bootstrap.enabled=true", "--app.bootstrap.username=war.admin",
             "--app.bootstrap.password=WarSmoke@123", "--app.bootstrap.email=war@example.com"],
            cwd=working, stdout=log, stderr=subprocess.STDOUT,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        try:
            deadline = time.monotonic() + 60
            while True:
                if process.poll() is not None:
                    raise RuntimeError(f"WAR exited during startup. See {log_path}")
                try:
                    login_html = get("/login")
                    break
                except (urllib.error.URLError, TimeoutError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError(f"WAR startup timed out. See {log_path}")
                    time.sleep(0.3)
            fields = Inputs()
            fields.feed(login_html)
            csrf = fields.values.get("_csrf")
            assert csrf, "Login JSP must render a CSRF token"
            data = urllib.parse.urlencode({"username": "war.admin", "password": "WarSmoke@123",
                                           "_csrf": csrf}).encode()
            with client.open(base + "/login", data=data, timeout=15) as response:
                dashboard = response.read().decode("utf-8")
                assert response.url.endswith("/smoke/admin"), response.url
                assert 'class="admin-shell"' in dashboard, "SiteMesh layout missing"
                assert "<sitemesh:write" not in dashboard, "SiteMesh tags were not processed"
                assert "/smoke/static/vendor/bootstrap/bootstrap.min.css" in dashboard
                assert "Danh mục" in dashboard, "UTF-8 content missing"
            for path in ("/admin/categories", "/admin/categories/new", "/admin/users", "/admin/users/new"):
                html = get(path)
                assert 'class="admin-shell"' in html, f"Missing decorator: {path}"
                assert "<sitemesh:write" not in html, f"Unprocessed decorator: {path}"
            assert "Bootstrap" in get("/static/vendor/bootstrap/bootstrap.min.css")
            assert "Bootstrap" in get("/static/vendor/bootstrap/bootstrap.bundle.min.js")
            print("PASS: executable WAR, login, JSP/JSTL, SiteMesh, Bootstrap and /smoke context path")
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=5)


if __name__ == "__main__":
    main()
