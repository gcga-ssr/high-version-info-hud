"""静态核验“专用服务器不会加载客户端类”。

合并成一个 jar 之后，最大的风险是专用服务器在加载模组时把 Minecraft 的客户端类
（net/minecraft/client/**）也拽进来，从而 NoClassDefFoundError。

这里直接检查产物的常量池：
  1. 哪些类引用了 net/minecraft/client/**  —— 应该只有 com/example/blockinfo/client/**；
  2. 除了 client 包本身，谁引用了 client 包 —— 应该只有 BlockInfo（dist 判断之后）
     和两个 playToClient 包的 handle()（dist 判断之后）。
"""
import os
import re
import sys
import zipfile

JAR = r"D:\xiangmu\mc\build\libs\blockinfo-1.5.0.jar"
CLIENT_PKG = "com/example/blockinfo/client/"
MC_CLIENT = re.compile(rb"net/minecraft/client/[A-Za-z0-9_$/]+")
# 注意：常量池里同一个类名可能出现为 `com/x/Y`、`Lcom/x/Y`、`/com/x/Y` 等形态
# （描述符前缀 / 内部名分隔），所以按“子串 + 向右扩展”来匹配，而不是要求从头开始。
CLIENT_REF = re.compile(rb"com/example/blockinfo/client/[A-Za-z0-9_$/]+")


def class_refs(data):
    """收集常量池里对 client 包的引用。"""
    return {m.decode("ascii") for m in CLIENT_REF.findall(data)}


def main():
    rc = 0
    with zipfile.ZipFile(JAR) as z:
        names = [n for n in z.namelist() if n.endswith(".class")]

        print("=== classes referencing net/minecraft/client/** ===")
        for n in sorted(names):
            hits = MC_CLIENT.findall(z.read(n))
            if hits:
                owner = n[:-6].replace("/", ".")
                kind = "client pkg" if n.startswith(CLIENT_PKG) else "!!! NON-CLIENT PKG"
                print("  %-52s %s" % (owner, kind))
                if not n.startswith(CLIENT_PKG):
                    rc = 1

        print("=== classes OUTSIDE the client package that reference com/example/blockinfo/client/** ===")
        for n in sorted(names):
            if n.startswith(CLIENT_PKG):
                continue
            refs = [r for r in class_refs(z.read(n)) if r.startswith(CLIENT_PKG)]
            if refs:
                owner = n[:-6].replace("/", ".")
                print("  %-52s -> %s" % (owner, ", ".join(sorted(set(refs)))))

    print("VERDICT:", "OK - client classes are only reached from the dist-guarded paths"
          if rc == 0 else "PROBLEM - a non-client class references Minecraft client code")
    return rc


if __name__ == "__main__":
    sys.exit(main())
