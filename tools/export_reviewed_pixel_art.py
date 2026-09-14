"""Compile the hand-authored pixel maps in art/reviewed-pixel-items.json.

Run with --write to export; the default checks shipped pixels without changing them.
Only explicitly named sprites are touched. Requires Pillow.
"""
import argparse
import json
from pathlib import Path

from PIL import Image


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    source = json.loads((root / 'art/reviewed-pixel-items.json').read_text())
    palette = {key: tuple(bytes.fromhex(value)) for key, value in source['palette'].items()}
    compiled = []
    for sheet in [source, *source.get('additional_sheets', [])]:
        compiled.extend(compile_sheet(root, sheet, palette))
    mismatches = []
    for path, image in compiled:
        if args.write:
            path.parent.mkdir(parents=True, exist_ok=True)
            image.save(path)
        elif not path.exists():
            mismatches.append(str(path))
        else:
            with Image.open(path) as actual:
                if actual.size != image.size or actual.convert('RGBA').tobytes() != image.tobytes():
                    mismatches.append(str(path))
    if mismatches:
        parser.exit(1, 'Pixel export drift:\n' + '\n'.join(mismatches) + '\n')
    print(f"{'Exported' if args.write else 'Verified'} {len(compiled)} reviewed sprites")


def compile_sheet(root, sheet, palette):
    size = sheet['size']
    target = (root / sheet['destination']).resolve()
    if not target.is_relative_to(root):
        raise ValueError('Export destination must be inside this project')
    compiled = []
    margin = 2 if sheet.get('transparent_border', True) else 0
    for name, rows in sheet['sprites'].items():
        if Path(name).name != name or name in ('.', '..'):
            raise ValueError(f'Invalid sprite name: {name}')
        width = max(map(len, rows))
        if width > size - margin or len(rows) > size - margin:
            raise ValueError(f'{name}: pixel map must leave a transparent border')
        image = Image.new('RGBA', (size, size))
        left, top = (size - width) // 2, (size - len(rows)) // 2
        for y, row in enumerate(rows):
            for x, pixel in enumerate(row):
                image.putpixel((left + x, top + y), palette[pixel])
        compiled.append((target / f'{name}.png', image))
    return compiled


if __name__ == '__main__':
    main()
