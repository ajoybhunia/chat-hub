#!/usr/bin/env python3
"""
Usage: gh issue view <NUMBER> --json body,comments | python3 extract-image-urls.py

Reads GitHub issue JSON from stdin and prints one image URL per line,
deduplicated and in order of appearance.
"""

import json
import re
import sys

data = json.load(sys.stdin)

sources = [data.get("body") or ""]
for c in data.get("comments", []):
    sources.append(c.get("body") or "")

combined = "\n".join(sources)

urls = re.findall(r"!\[.*?\]\((https?://[^)]+)\)", combined)
urls += re.findall(
    r"https://(?:user-images\.githubusercontent\.com|github\.com/user-attachments)[^\s)\"]+",
    combined,
)

for u in dict.fromkeys(urls):  # deduplicate, preserve order
    print(u)
