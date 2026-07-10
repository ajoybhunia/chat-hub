---
name: task-creation
description: >
  Task breakdown skill for an agreed story plan. Reads docs/<story_number>/plan.md and
  decomposes it into discrete, independently-implementable technical tasks with acceptance
  criteria. Saves the result to docs/<story_number>/tasks.md. Used by the
  user-story-implementation skill — load this skill at the start of Phase 2 and follow it
  completely before returning to the orchestrator.
---

# Task Creation Skill

Decompose the agreed plan into technical tasks, each with clear acceptance criteria.
Do not exit this skill until the user has confirmed the task list. Save the result at
`docs/<story_number>/tasks.md`.

---

## Inputs expected before starting

The orchestrator must have already confirmed:
- `docs/<story_number>/plan.md` exists and the user has agreed to it
- Codebase context from Phase 0 is still available in the conversation

If the plan file is missing or was never agreed to, return to the `story-planning` skill first.

---

## Step 1 — Read the Plan

```bash
cat docs/<story_number>/plan.md
```

Use the Affected Areas table and Approach section to drive the task breakdown.

---

## Step 2 — Decompose into Tasks

Create one task per cohesive unit of work. Tasks should be:
- **Independently implementable** — each can be coded, tested, and committed on its own
- **Sequenced by dependency** — earlier tasks must not depend on later ones
- **Scoped to a single concern** — avoid "and also" tasks; split them

Each task follows this template:

```markdown
## Task <N>: <Short Title>

**Description**
What needs to be done, referencing the relevant section of the plan.

**Acceptance Criteria**
- [ ] Criterion 1 (observable and testable)
- [ ] Criterion 2
- [ ] ...

**Files Likely Affected**
- `path/to/file.ext` — reason

**Test Requirements**
- Unit: ...
- Integration: ... (if applicable)
- Edge cases to cover: ...

**Dependencies**
- Depends on Task <M> (if any)
- None (if first or independent)

**Estimated Complexity**
S / M / L
```

---

## Step 3 — Mid-Breakdown Gap Check

While decomposing, if you discover:
- A missing design decision not covered by the plan
- Scope that was implied but never stated
- A technical dependency the plan didn't account for

**Stop immediately and notify the user:**
> "While breaking down the tasks, I found a gap: [describe gap].
> I recommend we [proposed resolution]. How would you like to proceed?
>
> 1. Go back to **story-planning** to update the plan
> 2. Proceed with this assumption: [state it explicitly]
> 3. Defer as a follow-up issue"

Do not silently paper over gaps with assumptions unless the user explicitly chooses option 2.

---

## Step 4 — Save the Tasks

Once the full task list is drafted:

```bash
# Write task list to docs/<story_number>/tasks.md
```

Present the complete task list to the user, then ask:
> "Here are the N tasks I've defined. Does this breakdown look complete?
> Any tasks to add, remove, or split before we start implementing?"

Wait for explicit confirmation before exiting this skill.

---

## Step 5 — Return to Orchestrator

Once the user confirms the task list:
> "Tasks saved to `docs/<story_number>/tasks.md`."

Signal to the orchestrator: **task creation complete, return to implement-user-story skill**.

---

## Edge Cases

**Too many tasks (8+)**
Surface this to the user:
> "This breakdown has N tasks, which may span multiple sessions. Would you like to
> prioritise a subset for this session, or proceed with the full list?"

**Circular dependencies**
If two tasks depend on each other, flag it immediately and propose a split or merge.

**Task requires a technology not currently in the stack**
Flag it as a decision point before writing the task:
> "Task N requires [technology]. This isn't currently in the stack. Should we
> add it (update the plan) or find an alternative approach?"