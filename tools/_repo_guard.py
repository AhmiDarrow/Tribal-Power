"""Keep Tribal Power's authoring tools inside this repository and the `tribalpower` namespace.

Several tools accept an optional project path as their first argument. Pointed at another checkout
(for example the Core modpack repo, whose `mods/*` hold other namespaces), `overhaul_art.py` repainted
every item and block it found there. Every tool that takes a path now resolves it through
`project_root()`:

* no path, or this repository's own path  -> this repository;
* any other path                          -> refused, unless `--allow-foreign-root` is passed.

Even with that flag the tools only ever write `assets/tribalpower/...` and `data/tribalpower/...`
under the chosen root. `overhaul_art.py` additionally refuses to paint any namespace other than
`tribalpower` unless `--allow-namespace=<ns>` is passed once per extra namespace.
"""
from __future__ import annotations

import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
NAMESPACE = "tribalpower"
FOREIGN_FLAG = "--allow-foreign-root"
NAMESPACE_FLAG = "--allow-namespace="


def positional(argv: list[str] | None = None) -> list[str]:
    argv = sys.argv if argv is None else argv
    return [a for a in argv[1:] if not a.startswith("--")]


def project_root(argv: list[str] | None = None) -> Path:
    """The project a tool may write into (see module docstring)."""
    argv = sys.argv if argv is None else argv
    args = positional(argv)
    if not args:
        return REPO
    root = Path(args[0]).resolve()
    if root == REPO:
        return REPO
    if FOREIGN_FLAG in argv:
        print(f"[repo_guard] writing {NAMESPACE} assets under foreign root {root} ({FOREIGN_FLAG})", file=sys.stderr)
        return root
    raise SystemExit(
        f"[repo_guard] refusing to write outside {REPO}: {root}\n"
        f"Tribal Power tools only paint this repository. Pass {FOREIGN_FLAG} to override (tribalpower namespace only)."
    )


def allowed_namespaces(argv: list[str] | None = None) -> set[str]:
    argv = sys.argv if argv is None else argv
    extra = {a[len(NAMESPACE_FLAG):] for a in argv[1:] if a.startswith(NAMESPACE_FLAG)}
    return {NAMESPACE} | {n for n in extra if n}


def check_inside(path: Path, root: Path) -> Path:
    """Raise if `path` would land outside `root` (after resolving `..` and links)."""
    resolved = Path(path).resolve()
    if not resolved.is_relative_to(root.resolve()):
        raise SystemExit(f"[repo_guard] refusing to write {resolved}: outside {root}")
    return resolved
