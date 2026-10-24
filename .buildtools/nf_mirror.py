"""Mirror the few NeoForge Maven artifacts the JVM cannot fetch itself.

The JVM's TLS stack (JSSE) is reset during the handshake with maven.neoforged.net
(CDN77) on this network, while Python/OpenSSL and .NET reach it fine.  So we fetch
what Gradle still needs with Python and serve it to Gradle from a local file:// repo.
"""
import os
import ssl
import sys
import time
import urllib.error
import urllib.request

BASE = "https://maven.neoforged.net/releases"
REPO = os.path.join(os.path.dirname(os.path.abspath(__file__)), "local-repo")
CTX = ssl.create_default_context()

# group:artifact:version:classifier:extension  (classifier may be empty)
WANTED = [
    # Gradle side: the neoFormRuntimeTool configuration
    "net.neoforged:neoform-runtime:2.0.31::pom",
    "net.neoforged:neoform-runtime:2.0.31::module",
    "net.neoforged:neoform-runtime:2.0.31::jar",
    "net.neoforged:neoform-runtime:2.0.31:all:jar",
    "net.neoforged.jst:jst-cli-bundle:2.0.11::pom",
    "net.neoforged.jst:jst-cli-bundle:2.0.11::module",
    "net.neoforged.jst:jst-cli-bundle:2.0.11::jar",
    # runtime dependencies of neoform-runtime, in case they are not cached
    "net.neoforged:srgutils:1.0.10::pom",
    "net.neoforged:srgutils:1.0.10::jar",
    "net.neoforged.installertools:problems-api:3.0.3::pom",
    "net.neoforged.installertools:problems-api:3.0.3::jar",
    "io.codechicken:DiffPatch:2.0.0.36::pom",
    "io.codechicken:DiffPatch:2.0.0.36:all:jar",
]


def fetch(url, tries=8):
    last = None
    for i in range(tries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "dsh-build/1.0"})
            with urllib.request.urlopen(req, timeout=60, context=CTX) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            last = e
        except Exception as e:  # noqa: BLE001 - transient socket/TLS resets
            last = e
        time.sleep(0.7 * (i + 1))
    raise RuntimeError("failed %s: %s" % (url, last))


def mirror(spec):
    group, artifact, version, classifier, ext = spec.split(":")
    rel = "/".join(group.split(".")) + "/%s/%s" % (artifact, version)
    name = "%s-%s%s.%s" % (artifact, version, ("-" + classifier) if classifier else "", ext)
    dst = os.path.join(REPO, rel.replace("/", os.sep), name)

    if os.path.exists(dst) and os.path.getsize(dst) > 0:
        return "cached", dst, os.path.getsize(dst)

    data = fetch("%s/%s/%s" % (BASE, rel, name))
    if data is None:
        return "404", dst, 0

    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with open(dst, "wb") as fh:
        fh.write(data)
    return "ok", dst, len(data)


def main():
    print("mirror root:", REPO)
    for spec in WANTED:
        status, dst, size = mirror(spec)
        print("  %-6s %8d  %s" % (status, size, os.path.relpath(dst, REPO)))
    print("done")


if __name__ == "__main__":
    sys.exit(main())
