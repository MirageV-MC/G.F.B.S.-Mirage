import argparse
import math
import os
import re
import sys
from dataclasses import dataclass
from pathlib import Path


VT_RE = re.compile(
    r"^(?P<prefix>\s*vt\s+)"
    r"(?P<u>[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)\s+"
    r"(?P<v>[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)"
    r"(?P<w>\s+[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)?"
    r"(?P<suffix>\s*(?:#.*)?)$"
)


def _wrap01(x: float) -> float:
    return x - math.floor(x)


def _clamp01(x: float) -> float:
    if x < 0.0:
        return 0.0
    if x > 1.0:
        return 1.0
    return x


@dataclass(frozen=True)
class ObjUvStats:
    vt_count: int
    out_of_range_count: int
    u_min: float | None
    u_max: float | None
    v_min: float | None
    v_max: float | None


def _update_range(cur_min: float | None, cur_max: float | None, x: float) -> tuple[float, float]:
    if cur_min is None or cur_max is None:
        return x, x
    return (min(cur_min, x), max(cur_max, x))


def _format_float(x: float, precision: int) -> str:
    return f"{x:.{precision}f}".rstrip("0").rstrip(".") if precision >= 0 else str(x)


def process_obj_text(
    text: str,
    *,
    mode: str,
    precision: int,
    flip_v: bool,
) -> tuple[str, ObjUvStats, bool]:
    lines = text.splitlines(keepends=True)
    vt_count = 0
    out_of_range_count = 0
    u_min = u_max = v_min = v_max = None
    changed = False

    if mode == "wrap":
        mapper = _wrap01
    elif mode == "clamp":
        mapper = _clamp01
    else:
        raise ValueError(f"Unsupported mode: {mode}")

    new_lines: list[str] = []
    for line in lines:
        m = VT_RE.match(line)
        if not m:
            new_lines.append(line)
            continue

        vt_count += 1
        u_raw = float(m.group("u"))
        v_raw = float(m.group("v"))
        if u_raw < 0.0 or u_raw > 1.0 or v_raw < 0.0 or v_raw > 1.0:
            out_of_range_count += 1

        u_min, u_max = _update_range(u_min, u_max, u_raw)
        v_min, v_max = _update_range(v_min, v_max, v_raw)

        u_new = mapper(u_raw)
        v_new = mapper(v_raw)
        if flip_v:
            v_new = 1.0 - v_new

        if (u_new != u_raw) or (v_new != v_raw):
            changed = True

        u_str = _format_float(u_new, precision)
        v_str = _format_float(v_new, precision)
        w = m.group("w") or ""
        rebuilt = f"{m.group('prefix')}{u_str} {v_str}{w}{m.group('suffix')}"
        if line.endswith("\r\n") and not rebuilt.endswith("\r\n"):
            rebuilt = rebuilt + "\r\n"
        elif line.endswith("\n") and not rebuilt.endswith("\n"):
            rebuilt = rebuilt + "\n"
        new_lines.append(rebuilt)

    stats = ObjUvStats(
        vt_count=vt_count,
        out_of_range_count=out_of_range_count,
        u_min=u_min,
        u_max=u_max,
        v_min=v_min,
        v_max=v_max,
    )
    return "".join(new_lines), stats, changed


def find_obj_files(root: Path, recursive: bool) -> list[Path]:
    if root.is_file():
        return [root]
    if recursive:
        return sorted(p for p in root.rglob("*.obj") if p.is_file())
    return sorted(p for p in root.glob("*.obj") if p.is_file())


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        prog="obj_uv_fix",
        description="将 OBJ 的 vt UV 纠正到 0-1，避免在 Minecraft 方块纹理图集下越界采样导致贴图拼贴。",
    )
    parser.add_argument("path", help="OBJ 文件或目录路径")
    parser.add_argument(
        "--mode",
        choices=["wrap", "clamp"],
        default="wrap",
        help="wrap=取小数部分(推荐)；clamp=直接夹到0-1(可能压缩拉伸)",
    )
    parser.add_argument(
        "--flip-v",
        action="store_true",
        help="对归一化后的 V 做 1-V（部分导出器/坐标系可能需要）",
    )
    parser.add_argument(
        "--precision",
        type=int,
        default=6,
        help="输出小数位数（默认 6）",
    )
    parser.add_argument(
        "--in-place",
        action="store_true",
        help="直接覆盖原文件（默认只输出到 stdout）",
    )
    parser.add_argument(
        "--backup",
        action="store_true",
        help="与 --in-place 一起使用：保存 .bak 备份",
    )
    parser.add_argument(
        "--recursive",
        action="store_true",
        help="当 path 是目录时递归扫描 *.obj",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="只检查是否存在越界 vt（返回码 0=全OK；2=发现越界/会被修复）",
    )
    args = parser.parse_args(argv)

    root = Path(args.path)
    if not root.exists():
        print(f"路径不存在: {root}", file=sys.stderr)
        return 1

    obj_files = find_obj_files(root, args.recursive)
    if not obj_files:
        print("未找到任何 .obj 文件", file=sys.stderr)
        return 1

    any_need_fix = False
    for p in obj_files:
        text = p.read_text(encoding="utf-8", errors="replace")
        new_text, stats, changed = process_obj_text(
            text,
            mode=args.mode,
            precision=args.precision,
            flip_v=args.flip_v,
        )

        need_fix = stats.out_of_range_count > 0
        any_need_fix = any_need_fix or need_fix

        rel = os.fspath(p)
        if args.check:
            status = "OK" if not need_fix else "NEED_FIX"
            print(
                f"{status}  {rel}  vt={stats.vt_count}  out={stats.out_of_range_count}  "
                f"u=[{stats.u_min},{stats.u_max}]  v=[{stats.v_min},{stats.v_max}]"
            )
            continue

        if args.in_place:
            if args.backup:
                backup_path = p.with_suffix(p.suffix + ".bak")
                if not backup_path.exists():
                    backup_path.write_text(text, encoding="utf-8")
            if changed:
                p.write_text(new_text, encoding="utf-8")
            print(
                f"WROTE  {rel}  vt={stats.vt_count}  out={stats.out_of_range_count}  "
                f"mode={args.mode}{' flipV' if args.flip_v else ''}"
            )
        else:
            if len(obj_files) != 1:
                print("未指定 --in-place 时仅支持单文件处理", file=sys.stderr)
                return 1
            sys.stdout.write(new_text)

    if args.check and any_need_fix:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
