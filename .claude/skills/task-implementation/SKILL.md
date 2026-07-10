---
name: task-implementation
description: >
  Single-task implementation skill. Given a specific task from docs/<story_number>/tasks.md,
  implements it with production-quality code and tests, presents it for user review, handles
  feedback cycles, and commits once approved. Called repeatedly by the user-story-implementation
  orchestrator — once per task. Load this skill at the start of each task and follow it
  completely before returning to the orchestrator.
---

# Task Implementation Skill

Implement one task from the story's task list, get it reviewed and approved, then commit it.
Do not move to the next task — that decision belongs to the orchestrator.

---

## Inputs expected before starting

The orchestrator must confirm the following are available:

- The task definition from `docs/<story_number>/tasks.md` — description, acceptance
  criteria, affected files, test requirements, dependencies
- Codebase context from Phase 0 (Context Summary) — code style, test framework,
  relevant files and patterns
- All prior tasks in this story are already committed

If any of these are missing, ask the orchestrator to provide them before proceeding.

---

## Step 1 — Announce

State clearly what you are about to do:

> "Starting **Task \<N\> / \<Total\>: \<Title\>**.
> Approach: [one sentence describing the implementation strategy]."

---

## Step 2 — Implement

- Write the implementation code
- Follow the code style, naming conventions, and patterns observed in Phase 0
- Touch **only** the files relevant to this task — no opportunistic refactors
- Add inline comments for any non-obvious logic
- Reference the task's "Files Likely Affected" list as a starting point, but use
  your own judgement if the actual scope differs — note any deviations

---

## Step 3 — Write Tests

Write tests **as part of this step**, not after commit.

- Cover every acceptance criterion listed in the task definition
- Cover the edge cases specified in the task's test requirements
- Follow the existing test framework, file naming, and assertion conventions
- Place test files where the project convention dictates (co-located or in a
  dedicated `tests/` directory)

---

## Step 4 — Run Tests

```bash
# Adapt to the project's tooling:
# npm test | pytest | go test ./... | mvn test | bundle exec rspec | etc.
```

Rules:
- All tests must be **green** before requesting review — no exceptions
- If tests fail: fix the code or tests, then re-run; do not skip ahead
- If a **pre-existing, unrelated** test is already failing, call it out explicitly:
  > "Note: `<test name>` was already failing before this task and is unrelated to
  > these changes."

---

## Step 5 — Present for Review

Show the user all of the following:

1. **Summary** — what was changed and why, in plain language
2. **Diff / changed files** — key file paths and the nature of each change
3. **Test results** — pass count, and any notable coverage additions
4. **Acceptance criteria checklist** — every criterion from the task, ticked off:
   ```
   - [x] Criterion 1
   - [x] Criterion 2
   - [ ] Criterion 3  ← if not yet met, explain why and what's still needed
   ```

Then ask:
> "Please review. Reply **Approved** to commit, or share your feedback."

---

## Step 6 — Handle Feedback

For each round of feedback:

1. Apply the requested changes
2. Re-run the full test suite
3. Re-present for review (Steps 4–5)
4. Repeat until the user replies **Approved**

If the feedback implies a problem that cannot be resolved within this task's scope —
for example, a missing design decision, a conflict with another module, or a required
change to the plan — **do not attempt to silently absorb it**. Instead, signal the
orchestrator:

> "This feedback suggests the issue may be outside this task's scope:
> [describe the problem].
> I recommend returning to the orchestrator to decide whether to:
> 1. Revise the **task breakdown** (Phase 2)
> 2. Revise the **plan** (Phase 1)
> 3. Proceed with a documented in-task adjustment"

---

## Step 7 — Commit

Once the user approves and all tests pass:

```bash
git add <relevant files>
git commit -m "<type>(<scope>): <subject>

<body: what was done and why, referencing the story>

Closes #<story_number>   ← include ONLY on the final task of the story
Task: <N>/<total> - <task title>"
```

**Commit message conventions:**

| Field | Rule |
|-------|------|
| Type | `feat` `fix` `refactor` `test` `docs` `chore` |
| Scope | The module or area affected, e.g. `auth`, `api`, `db` |
| Subject | Imperative mood, ≤72 chars, no trailing period |
| Body | Explain *what* and *why*, not *how* |
| `Closes` | Only on the **last** task; omit on all earlier tasks |

---

## Step 8 — Signal to Orchestrator

After committing, return control to the orchestrator:

> "Task \<N\> committed ✓. Returning to orchestrator."

Do not start the next task. The orchestrator decides whether to proceed, pause, or
navigate to a different phase.