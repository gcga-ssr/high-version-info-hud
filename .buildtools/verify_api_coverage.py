"""Independently verify that the built jars only reference NeoForge API
classes that exist in the *oldest* release of the 1.21.1 line (21.1.1) -- and,
for completeness, in the newest cached one.

The mods were compiled against 21.1.1, so this is a cross-check of the
constant pools of the shipped class files rather than a re-run of the compiler.
"""
import os
import re
import sys
import zipfile

GC = os.path.join(os.environ["USERPROFILE"], ".gradle", "caches", "modules-2", "files-2.1")

# NeoForge API surfaces that can differ between 21.1.x builds.
#
# net.neoforged:mergetool 是 NeoForge 自己依赖的库，也是 net.neoforged.api.distmarker.Dist
# 的提供者：FMLEnvironment 里 `public static final Dist dist` 这个字段本身就要求该类在
# 模组构造之前就可解析，所以 Dist 一定在运行时存在（否则 FML 自己都初始化不了）。
SURFACES = {
    "21.1.1": [
        "net.neoforged/neoforge/21.1.1",
        "net.neoforged/bus/8.0.1",
        "net.neoforged.fancymodloader/loader/4.0.24",
        "net.neoforged/mergetool/2.0.0",
    ],
    "21.1.249": [
        "net.neoforged/neoforge/21.1.249",
        "net.neoforged/bus/8.0.5",
        "net.neoforged.fancymodloader/loader/4.0.44",
        "net.neoforged/mergetool/2.0.3",
    ],
}

MOD_JARS = [
    r"D:\xiangmu\mc\build\libs\blockinfo-1.5.0.jar",
]

REF = re.compile(rb"net/neoforged/[A-Za-z0-9_$/]+")


def jars_under(rel):
    out = []
    root = os.path.join(GC, rel.replace("/", os.sep))
    for dirpath, _dirs, files in os.walk(root):
        for f in files:
            if f.endswith(".jar"):
                out.append(os.path.join(dirpath, f))
    return out


def available(rel_dirs):
    names = set()
    for rel in rel_dirs:
        for jar in jars_under(rel):
            with zipfile.ZipFile(jar) as z:
                for n in z.namelist():
                    if n.endswith(".class"):
                        names.add(n[:-6])
    return names


def referenced(jar):
    refs = set()
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.endswith(".class"):
                data = z.read(n)
                for m in REF.findall(data):
                    refs.add(m.decode("ascii"))
    return refs


def main():
    rc = 0
    for target, rel_dirs in SURFACES.items():
        have = available(rel_dirs)
        print("=== target NeoForge %s: %d classes available ===" % (target, len(have)))
        for mod_jar in MOD_JARS:
            refs = referenced(mod_jar)
            missing = sorted(r for r in refs if r not in have)
            label = "%s (%s)" % (os.path.basename(mod_jar), os.path.basename(os.path.dirname(mod_jar)))
            print("  %-34s refs=%-3d missing=%d" % (label, len(refs), len(missing)))
            for m in missing:
                print("      MISSING %s" % m)
            if missing:
                rc = 1
    print("VERDICT:", "OK - every referenced NeoForge class exists on both ends of the 21.1.x line"
          if rc == 0 else "PROBLEM - see MISSING entries above")
    return rc


if __name__ == "__main__":
    sys.exit(main())
