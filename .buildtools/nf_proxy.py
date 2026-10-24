"""Local HTTP caching proxy for maven.neoforged.net.

Why: on this machine the JVM's TLS stack cannot complete a TLS handshake with
maven.neoforged.net (CDN77 closes the connection), so Gradle and the NeoForm
Runtime cannot download from it.  PowerShell/.NET and Python/OpenSSL reach it
fine.  This proxy fetches on demand with Python and serves plain HTTP on
localhost, so every JVM tool can resolve anything from that host.

Usage:  python nf_proxy.py [port]
Cache:  .buildtools/local-repo   (Maven layout, reused as a plain file:// repo too)
"""
import os
import ssl
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

UPSTREAM = "https://maven.neoforged.net"
CACHE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-repo")
CTX = ssl.create_default_context()
LOCK = threading.Lock()


def fetch_upstream(path, tries=8):
    last = None
    for i in range(tries):
        try:
            req = urllib.request.Request(
                UPSTREAM + path,
                headers={"User-Agent": "dsh-build/1.0", "Accept": "*/*"},
            )
            with urllib.request.urlopen(req, timeout=120, context=CTX) as resp:
                return resp.status, dict(resp.headers), resp.read()
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return 404, {}, b""
            last = e
        except Exception as e:  # noqa: BLE001 - transient resets expected
            last = e
        time.sleep(0.6 * (i + 1))
    raise RuntimeError("upstream failed for %s: %s" % (path, last))


def cache_path(path):
    rel = urllib.parse.unquote(path).lstrip("/")
    # keep the cache inside CACHE, refusing traversal
    parts = [p for p in rel.split("/") if p not in ("", ".", "..")]
    # the Maven-layout mirror (nf_mirror.py) stores artifacts without the
    # "/releases" prefix, so drop it to share one cache between both tools
    if parts and parts[0] == "releases":
        parts = parts[1:]
    return os.path.join(CACHE, *parts)


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        sys.stderr.write("[nf-proxy] %s\n" % (fmt % args))
        sys.stderr.flush()

    def _serve(self, head_only):
        path = urllib.parse.urlsplit(self.path).path
        dst = cache_path(path)

        body = None
        if os.path.exists(dst) and os.path.getsize(dst) > 0:
            with open(dst, "rb") as fh:
                body = fh.read()
            self.log_message("cache hit %s (%d bytes)", path, len(body))
        else:
            try:
                status, headers, data = fetch_upstream(path)
            except Exception as e:  # noqa: BLE001
                self.log_message("ERROR %s -> %s", path, e)
                self.send_response(502)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
            if status == 404:
                self.log_message("404 %s", path)
                self.send_response(404)
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
            body = data
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            tmp = dst + ".part"
            with open(tmp, "wb") as fh:
                fh.write(body)
            os.replace(tmp, dst)
            self.log_message("fetched %s (%d bytes)", path, len(body))

        self.send_response(200)
        self.send_header("Content-Type", "application/octet-stream")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if not head_only:
            self.wfile.write(body)

    def do_GET(self):
        # cache writes are atomic (os.replace), so no global lock is needed here
        self._serve(head_only=False)

    def do_HEAD(self):
        self._serve(head_only=True)


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8123
    os.makedirs(CACHE, exist_ok=True)
    srv = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    print("nf-proxy listening on http://127.0.0.1:%d  (upstream %s)" % (port, UPSTREAM), flush=True)
    srv.serve_forever()


if __name__ == "__main__":
    main()
