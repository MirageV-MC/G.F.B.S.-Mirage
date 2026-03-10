import argparse
import shutil
import subprocess
import sys
from pathlib import Path


ROOM_SIZE_PERCENT = 100
PRE_DELAY_MS = 0
WETNESS_PERCENT = 20
DAMPING_PERCENT = 4
LOW_TONE_PERCENT = 100
HIGH_TONE_PERCENT = 100
WET_GAIN_DB = 3
DRY_GAIN_DB = -2
STEREO_WIDTH_PERCENT = 64


def is_subpath(child: Path, parent: Path) -> bool:
    try:
        child.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def collect_ogg_files(input_dir: Path, output_dir: Path) -> list[Path]:
    files = []
    for p in input_dir.rglob("*.ogg"):
        if not p.is_file():
            continue
        if is_subpath(p, output_dir):
            continue
        files.append(p)
    return files


def build_filter() -> str:
    dry_gain = f"{DRY_GAIN_DB}dB"
    wet_gain = f"{WET_GAIN_DB}dB"

    wet_mix = max(0.0, min(1.0, WETNESS_PERCENT / 100.0))
    dry_mix = 1.0 - wet_mix

    width = max(0.0, min(1.0, STEREO_WIDTH_PERCENT / 100.0))
    left_right = 0.5 + 0.5 * width
    cross = 0.5 - 0.5 * width

    pre_delay = max(0, int(PRE_DELAY_MS))

    decay1 = 0.50 * (1.0 - DAMPING_PERCENT / 100.0)
    decay2 = 0.35 * (1.0 - DAMPING_PERCENT / 100.0)
    decay3 = 0.22 * (1.0 - DAMPING_PERCENT / 100.0)
    decay4 = 0.14 * (1.0 - DAMPING_PERCENT / 100.0)

    if ROOM_SIZE_PERCENT >= 80:
        delays = [60, 110, 170, 260]
    elif ROOM_SIZE_PERCENT >= 50:
        delays = [40, 80, 130, 190]
    else:
        delays = [25, 50, 90, 140]

    delays = [d + pre_delay for d in delays]

    left_delays = "|".join(str(d) for d in delays)
    right_delays = "|".join(str(d + 7) for d in delays)

    left_decays = "|".join(
        f"{x * wet_mix:.3f}" for x in [decay1, decay2, decay3, decay4]
    )
    right_decays = "|".join(
        f"{x * wet_mix:.3f}" for x in [decay1 * 0.97, decay2 * 1.03, decay3 * 0.96, decay4 * 1.02]
    )

    parts = [
        "[0:a]aformat=sample_fmts=fltp:channel_layouts=stereo,asplit=2[dry][wetbase]",
        f"[dry]volume={dry_gain}[dryg]",
        (
            f"[wetbase]"
            f"aecho=0.8:0.88:{left_delays}:{left_decays},"
            f"volume={wet_gain}"
            f"[wetl]"
        ),
        (
            f"[wetbase]"
            f"aecho=0.8:0.88:{right_delays}:{right_decays},"
            f"volume={wet_gain}"
            f"[wetr]"
        ),
        "[wetl][wetr]amerge=inputs=2[wetst]",
        (
            f"[wetst]pan=stereo|"
            f"c0={left_right:.3f}*c0+{cross:.3f}*c1|"
            f"c1={cross:.3f}*c0+{left_right:.3f}*c1"
            f"[wetwide]"
        ),
        f"[dryg][wetwide]amix=inputs=2:weights='{dry_mix:.3f} 1.000':normalize=0[mixout]",
    ]

    return ";".join(parts)


def process_file(ffmpeg: str, input_file: Path, output_file: Path) -> tuple[bool, str]:
    output_file.parent.mkdir(parents=True, exist_ok=True)

    filter_complex = build_filter()

    cmd = [
        ffmpeg,
        "-y",
        "-i",
        str(input_file),
        "-filter_complex",
        filter_complex,
        "-map",
        "[mixout]",
        "-c:a",
        "libvorbis",
        "-q:a",
        "6",
        str(output_file),
    ]

    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="replace",
    )

    if result.returncode != 0:
        return False, result.stderr.strip()

    return True, ""


def main() -> int:
    parser = argparse.ArgumentParser(description="批量给 OGG 添加混响")
    parser.add_argument("input_dir", type=Path, help="输入目录")
    parser.add_argument("output_dir", type=Path, help="输出目录")
    parser.add_argument("--overwrite", action="store_true", help="覆盖已存在文件")
    parser.add_argument("--ffmpeg", default="ffmpeg", help="ffmpeg 可执行文件路径")
    args = parser.parse_args()

    input_dir = args.input_dir.resolve()
    output_dir = args.output_dir.resolve()

    if not input_dir.exists() or not input_dir.is_dir():
        print(f"错误：输入目录无效：{input_dir}", file=sys.stderr)
        return 1

    if shutil.which(args.ffmpeg) is None and not Path(args.ffmpeg).exists():
        print("错误：找不到 ffmpeg，请先安装 ffmpeg 或用 --ffmpeg 指定 ffmpeg.exe 路径", file=sys.stderr)
        return 1

    if input_dir == output_dir:
        print("错误：输入目录和输出目录不能相同", file=sys.stderr)
        return 1

    files = collect_ogg_files(input_dir, output_dir)
    if not files:
        print("没有找到任何 .ogg 文件。")
        return 0

    total = len(files)
    success = 0
    failed = 0
    skipped = 0

    print(f"找到 {total} 个 .ogg 文件，开始处理...")

    for idx, input_file in enumerate(files, start=1):
        rel = input_file.relative_to(input_dir)
        output_file = output_dir / rel

        if output_file.exists() and not args.overwrite:
            skipped += 1
            print(f"[{idx}/{total}] 跳过（已存在）：{rel}")
            continue

        ok, err = process_file(args.ffmpeg, input_file, output_file)
        if ok:
            success += 1
            print(f"[{idx}/{total}] 完成：{rel}")
        else:
            failed += 1
            print(f"[{idx}/{total}] 失败：{rel}")
            if err:
                print(err, file=sys.stderr)

    print()
    print("处理结束")
    print(f"成功：{success}")
    print(f"失败：{failed}")
    print(f"跳过：{skipped}")
    print(f"输出目录：{output_dir}")

    return 0 if failed == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())