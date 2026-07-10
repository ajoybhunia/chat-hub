---
name: implement-user-story
description: >
  Use this skill whenever the user provides a GitHub issue number (in the format #123 or just
  a number) and wants to implement it. Orchestrates the full lifecycle: fetch issue, analyse
  code, collaborative planning, task breakdown, and incremental implementation with tests and
  commits. Trigger any time the user says things like "implement #42", "work on issue 15",
  "let's build story #7", "pick up this ticket", or hands you a GitHub issue number with
  intent to write code.
---

# User Story Implementation — Orchestrator

This skill is the **entry point and conductor** for implementing a GitHub issue end-to-end.
It owns the overall flow and phase gates. Detailed instructions for planning and task creation
live in separate sub-skills — **load each sub-skill only when you reach that phase**.

---

## Skill Map

```
implement-user-story             ← you are here (orchestrator)
│
├── Phase 0: Fetch & Analyse     ← load skills/issue-context/SKILL.md
│
├── Phase 1: Planning            ← load skills/story-planning/SKILL.md
│
├── Phase 2: Task Creation       ← load skills/task-creation/SKILL.md
│
├── Phase 3: Implementation      ← load skills/task-implementation/SKILL.md
│                                   (once per task)
└── Phase 4: Wrap-Up             ← handled in this file
```

> **Incremental loading rule:** Do NOT read sub-skill files upfront.
> Load each sub-skill file immediately before starting that phase, and only that phase.
> This keeps context focused and instructions relevant to the current step.

---

## Overall Phase Order

The phases below must be completed **in sequence**. Never skip a phase.
Each phase has an explicit gate — do not proceed until the gate condition is met.

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 (per task loop) → Phase 4
  ↑            ↑         ↑         ↑
  Gate:      Gate:     Gate:     Gate:
  Findings   Plan      Task      Code
  presented  agreed    list      approved
  to user    by user   confirmed + tests
                       by user   passing
```

Backward navigation is always allowed — see "Returning to a Prior Phase" below.

---

## Phase 0 — Fetch & Understand

### 0a — Establish the docs directory

Before loading the sub-skill, resolve the story number from the user's input and set up
the working directory:

```bash
bash .claude/skills/implement-user-story/scripts/setup-story-dir.sh <story_number>
```

If the directory already exists and contains files (`plan.md`, `tasks.md`, or others),
**stop and ask the user** before proceeding:

> "`docs/<story_number>/` already exists with the following files: \<list\>.
> This looks like a partial previous run. Would you like to:
> 1. **Resume** — skip phases whose output files are already present
> 2. **Start fresh** — delete the existing files and run all phases from the beginning"

If the user chooses **Resume**: skip any phase whose output file already exists and is
non-empty (`context.md` → skip Phase 0, `plan.md` → skip Phase 1, `tasks.md` → skip Phase 2),
but confirm with the user what was skipped before continuing.
If the user chooses **Start fresh**: `rm -f "$STORY_DIR"/*.md` then continue normally.

### 0b — Fetch & understand

*Delegated to sub-skill. Load it now.*

> **Load:** `skills/issue-context/SKILL.md`
> Read it fully before taking any action. Follow it completely.
> It will fetch the issue, download attached images, survey the codebase,
> and produce a structured Context Summary.

The issue-context skill will:
- Fetch the issue body, comments, labels, and any linked issues via `gh`
- Extract, download, and read all attached images into context
- Survey repo structure, relevant files, test conventions, and tech stack
- Produce a Context Summary, confirm it with the user, and save it to `docs/<story_number>/context.md`

**→ Phase 0 gate met when:** `docs/<story_number>/context.md` exists and the user has confirmed its contents.
**→ Then proceed to Phase 1.** The planning skill will read context from `docs/<story_number>/context.md` directly.

---

## Phase 1 — Collaborative Planning

*Delegated to sub-skill. Load it now.*

> **Load:** `skills/story-planning/SKILL.md`
> Read it fully before taking any planning action. Follow it completely.
> It will read context from `docs/<story_number>/context.md`.

The planning skill will:
- Run the clarification/assumption/plan/agreement loop
- Save the agreed plan to `docs/<story_number>/plan.md`
- Signal when planning is complete

**→ Phase 1 gate met when:** plan file exists and user has said "Agreed".
**→ Then proceed to Phase 2.**

---

## Phase 2 — Task Creation

*Delegated to sub-skill. Load it now.*

> **Load:** `skills/task-creation/SKILL.md`
> Read it fully before taking any task-creation action. Follow it completely.
> It will read the plan file and decompose it into tasks.

The task creation skill will:
- Decompose the plan into discrete, testable tasks
- Surface any plan gaps and offer to loop back to Phase 1
- Save the task list to `docs/<story_number>/tasks.md`
- Signal when the task list is confirmed

**→ Phase 2 gate met when:** task file exists and user has confirmed the list.
**→ Then proceed to Phase 3.**

---

## Phase 3 — Implementation (Task by Task)

*Delegated to sub-skill, one task at a time. Load it fresh for each task.*

Read `docs/<story_number>/tasks.md` to get the full task list and their dependency order.
Then, for each task in sequence:

> **Load:** `skills/task-implementation/SKILL.md`
> Pass it the current task definition and the Context Summary from Phase 0.
> Follow it completely for that one task.
> When the sub-skill signals "Task N committed ✓", return here before loading it again.

The task-implementation skill will:
- Implement the task following existing code conventions
- Write and run tests (all must pass before review)
- Present the work for user review with an acceptance criteria checklist
- Handle feedback cycles until the user approves
- Commit with a conventional commit message
- Signal back to the orchestrator when done

**Between tasks — orchestrator responsibilities:**

After each task commits, the orchestrator presents a structured checkpoint:

> "Task N committed ✓.
>
> Next up — **Task N+1: \<Title\>**
> \<one-sentence description of what it covers\>
>
> Before I start: does the plan still feel right, or would you like to adjust anything?
> - **Continue** — start Task N+1 as planned
> - **Adjust scope** — tweak this or a later task before proceeding
> - **Revise tasks** — go back to Phase 2 and rework the task breakdown
> - **Revise plan** — go back to Phase 1 and update the plan itself"

Wait for the user to respond before loading the sub-skill for the next task.
If the user picks "Continue" (or equivalent), proceed immediately.
If the user picks any adjustment option, handle it via "Returning to a Prior Phase" below.

**→ Phase 3 complete when all tasks are committed.**
**→ Then proceed to Phase 4.**

---

## Phase 4 — Wrap-Up

*Handled here.*

1. Run the full test suite one final time to confirm nothing broke across all tasks:
   ```bash
   # npm test / pytest / go test ./... / mvn test / etc.
   ```
2. Append a completion note to `docs/<story_number>/plan.md`:
   ```markdown
   ## Status: Completed
   Implemented in <N> tasks. All tests passing. Final commit: <hash>.
   ```
3. Offer to open a PR:
   ```bash
   gh pr create --title "<Issue Title>" --body "Closes #<NUMBER>" --draft
   ```
4. Give the user a brief summary of what was built.

---

## Returning to a Prior Phase

Backward navigation can be triggered two ways:

**Proactively** — user selects "Adjust scope", "Revise tasks", or "Revise plan" at a between-task checkpoint.

**Reactively** — during a task, it becomes clear the problem cannot be fixed without a broader change. Stop the sub-skill and surface:
> "This looks like it needs a broader change. Would you like to:
> 1. Go back to **Phase 2** — reload `task-creation` and revise the task breakdown
> 2. Go back to **Phase 1** — reload `story-planning` and update the plan
> 3. Proceed with a local in-task adjustment (describe it and I'll note it in tasks.md)"

In both cases: reload the appropriate sub-skill, re-run that phase fully, and update the docs file for that phase before resuming Phase 3.

---

## Key Principles

| Principle | Rule |
|-----------|------|
| Incremental skill loading | Load sub-skills only at the moment they are needed |
| Sequential phases | Plan → Tasks → Implement; never skip |
| No silent assumptions | Every assumption must be stated and confirmed |
| No broken builds | Tests must pass before every commit |
| One task at a time | Commit before moving to the next |
| User controls pace | Wait for explicit agreement at every phase gate |
| Always offer backward navigation | If something's wrong, surface it — don't paper over it |

---

## Edge Cases

**`gh` authentication or issue-not-found errors:**
Handled inside `skills/issue-context/SKILL.md`. The sub-skill will surface these before
returning to the orchestrator.

**Monorepo / unclear working directory:**
Confirm the repo root with the user before loading the issue-context skill.

**Conflicting task dependencies discovered late:**
Stop Phase 3, return to Phase 2 via the backward navigation offer above.