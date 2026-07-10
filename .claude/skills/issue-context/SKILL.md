---
name: issue-context
description: >
  GitHub issue information gathering and codebase context skill. Given a GitHub issue number,
  fetches the full issue (body, comments, linked issues), downloads and reads all attached
  images, surveys the codebase structure and relevant files, and produces a structured
  context summary ready for planning. Used by the user-story-implementation orchestrator —
  load this skill at the start of Phase 0 and follow it completely before returning.
---

# Issue Context Skill

Gather everything needed to understand a GitHub issue and the codebase it lives in.
Produce a structured **Context Summary** and signal when it is ready for the planning phase.

Do not proceed to planning until all four steps below are complete.

---

## Step 1 — Fetch the Issue

### 1.1 Primary issue

```bash
gh issue view <NUMBER> --json title,body,labels,assignees,milestone,comments
```

If `gh` returns an error:
- Run `gh auth status` to check authentication
- If not authenticated, stop and ask the user to run `gh auth login`
- If authenticated but issue not found, ask the user to verify the number and repo

### 1.2 Linked issues and sub-issues

Scan the body and comments for references to other issues (`#123`, closing keywords like
`closes #45`, or explicit sub-issue lists). Fetch each one:

```bash
gh issue view <LINKED_NUMBER> --json title,body,state
```

Note the relationship (blocks / blocked-by / related / sub-issue) for each.

---

## Step 2 — Fetch Attached Images

GitHub issue bodies and comments often contain screenshots, mockups, diagrams, and annotated
error outputs. The `gh` CLI returns these only as markdown URLs — it does not download the
content. Always extract and fetch them so visual context is not lost.

### 2.1 Extract all image URLs

```bash
gh issue view <NUMBER> --json body,comments \
  | python3 .claude/skills/issue-context/scripts/extract-image-urls.py
```

If no URLs are printed, skip to Step 3 silently.

### 2.2 Download each image

```bash
mkdir -p /tmp/story-<NUMBER>-images

# For each URL, derive a filename from the last path segment and download:
curl -sL "<IMAGE_URL>" -o /tmp/story-<NUMBER>-images/<filename>

# If the URL has no recognisable extension, default to .png
# (most GitHub drag-and-drop uploads are PNG)
```

### 2.3 Read images into context

Use the `view` tool on each downloaded file so the image content is visible — not just
the filename:

```
view /tmp/story-<NUMBER>-images/<filename>    ← repeat for each image
```

### 2.4 Caption each image

After viewing, write a one-line description of what each image shows. These captions travel
with the Context Summary into planning. Example:

```
Images:
  1. error-modal.png     — screenshot of the broken error modal on Safari iOS
  2. new-design.png      — Figma mockup of the proposed redesigned modal
  3. db-schema.png       — annotated ERD showing the new `payments` table relationship
```

**Edge cases:**
- *Image URL returns 404 or redirects to a login page:* Mark it as "inaccessible" in the
  summary and ask the user to share it directly before proceeding.
- *Image is a GIF or video:* Note its existence; describe the first visible frame if
  the tool supports it, otherwise flag it to the user.

---

## Step 3 — Survey the Codebase

Survey the repository so the planning phase can reference real files and patterns rather
than speaking generically.

### 3.1 Repo structure

```bash
# Get a high-level view of the layout
find . -maxdepth 3 -not -path '*/\.*' -not -path '*/node_modules/*' | head -80
```

### 3.2 Relevant files

Search for files, functions, types, and modules related to the story's domain keywords:

```bash
grep -r "<domain keyword>" --include="*.ts" -l    # adapt extension to tech stack
rg "<domain keyword>" -l                           # if ripgrep is available
```

Read the most relevant files (or key sections) to understand current behaviour.

### 3.3 Test framework and conventions

```bash
# Identify the test runner and file naming conventions
find . -name "*.test.*" -o -name "*.spec.*" -o -name "*_test.*" | head -20
```

Note: test runner (jest / pytest / go test / etc.), file naming pattern, assertion style,
and where test files live relative to source files.

### 3.4 Tech stack

Read the project's dependency manifest(s):

```bash
# Pick the relevant ones for this repo:
cat package.json
cat requirements.txt
cat go.mod
cat pom.xml
cat Gemfile
```

### 3.5 Existing documentation

```bash
ls docs/
# Read any ADRs, architecture notes, or prior plans that touch the story's domain
```

---

## Step 4 — Produce the Context Summary

Write a structured summary that will be handed to the planning sub-skill as its starting
input. Every section must be concrete — file names, not generalities.

```
## Context Summary: Issue #<NUMBER> — <Title>

### Issue
- **State:** open / closed
- **Labels:** ...
- **Milestone:** ...

### What the issue asks for
<2–4 sentences in your own words>

### Linked issues
| # | Title | Relationship | State |
|---|-------|-------------|-------|

### Images
| File | What it shows |
|------|--------------|

### Codebase findings
| Area              | Detail |
|-------------------|--------|
| Repo structure    | ... |
| Relevant files    | ... |
| Test framework    | ... |
| Tech stack        | ... |
| Relevant docs     | ... |

### Initial observations
<Anything notable about the code that bears on this story:
patterns to follow, potential risk areas, existing similar implementations>
```

---

## Step 5 — User Confirmation and Save

Present the full Context Summary to the user and ask:
> "Does this context look accurate? Reply **Agreed** to proceed to planning, or let me know what needs correcting."

If the user requests corrections, update the relevant section(s) and re-present before asking again.

Once the user agrees, write the confirmed Context Summary to disk:

```bash
# docs/<NUMBER>/ must already exist (created by setup-story-dir.sh in Phase 0a)
# Write the context summary:
# Write the full Context Summary markdown to docs/<NUMBER>/context.md
```

Confirm to the user:
> "Context saved to `docs/<NUMBER>/context.md`. Ready to begin planning."

Then return to the `user-story-implementation` orchestrator — Phase 0 is complete.