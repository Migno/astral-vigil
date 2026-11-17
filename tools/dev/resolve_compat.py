"""Resolve the newest NeoForge Modrinth version *ids* for the compat instance.

Modrinth Maven resolves a version *number* to the version's primary file, which for multi-loader releases can be the
Fabric jar. Version ids are unambiguous, so stonecutter.properties.toml stores slug:<version id>.

Usage: python -I tools/dev/resolve_compat.py            -> prints the compat.mods lines for both targets
"""
import json
import urllib.parse
import urllib.request

UA = {"User-Agent": "astralvigil-dev-tools"}
TARGETS = {
    "1.21.1-neoforge": ("1.21.1", ["emi", "jade", "curios", "patchouli", "sodium", "iris", "placebo",
                                   "apothic-attributes", "apothic-enchanting", "apothic-spawners", "apotheosis",
                                   "rhino", "better-advanced-tooltips", "architectury-api", "kubejs"]),
    "26.1-neoforge": ("26.1.2", ["mezzconfig", "jei", "jade", "curios", "patchouli", "sodium", "iris", "placebo",
                                 "apothic-attributes", "apothic-enchanting", "apothic-spawners", "apotheosis",
                                 "rhino", "better-advanced-tooltips", "architectury-api", "kubejs"]),
}


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30) as r:
        return json.load(r)


def resolve(slug, mc):
    q = urllib.parse.urlencode({"loaders": json.dumps(["neoforge"]), "game_versions": json.dumps([mc])})
    versions = get(f"https://api.modrinth.com/v2/project/{slug}/version?{q}")
    for v in versions:  # newest first; prefer files whose name does not scream another loader
        files = [f for f in v["files"] if f["primary"]] or v["files"]
        name = files[0]["filename"].lower()
        if "fabric" in name and "neoforge" not in name:
            continue
        return v["id"], v["version_number"], files[0]["filename"]
    return None


if __name__ == "__main__":
    for target, (mc, slugs) in TARGETS.items():
        entries = []
        print(f"# {target}")
        for slug in slugs:
            r = resolve(slug, mc)
            if r is None:
                print(f"#   {slug}: NO NEOFORGE BUILD")
                continue
            vid, number, filename = r
            print(f"#   {slug}: {number} -> {filename} ({vid})")
            entries.append(f"{slug}:{vid}")
        print('compat.mods = "' + ",".join(entries) + '"')
