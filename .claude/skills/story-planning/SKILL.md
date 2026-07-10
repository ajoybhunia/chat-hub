---
name: story-planning
description: >
  Collaborative planning skill for a GitHub issue. Takes issue content and codebase context
  as input. Runs an iterative clarification loop until the user explicitly agrees to a plan,
  then saves it to docs/<story_number>/plan.md. Used by the user-story-implementation skill —
  load this skill at the start of Phase 1 and follow it completely before returning to the
  orchestrator.
---

# Story Planning Skill

Produce a written plan the user has **explicitly agreed to**. Do not exit this skill until
the user says "Agreed" (or equivalent). Save the result at `docs/<story_number>/plan.md`.

---

## Inputs expected before starting

The orchestrator must have already completed Phase 0. Before doing anything else, read
the confirmed context file:

```bash
cat docs/<story_number>/context.md
```

If the file does not exist or is empty, stop and ask the orchestrator to run Phase 0 first.

The context file contains:
- Issue title, body, labels, state, and comments
- Linked issue relationships
- Image captions (visual context)
- Codebase findings: repo structure, relevant files, test framework, tech stack

---

## Step 1 — Initial Analysis

Present the following to the user before asking any questions:

- **Story summary** — what the issue is asking for, in your own words
- **Scope** — what appears in scope vs. out of scope
- **Impact areas** — which modules, files, or systems will be touched
- **Open questions** — things you need answered before committing to an approach
- **Initial assumptions** — things you are assuming to make progress; label each `[ASSUMPTION]`

Example assumption label:
> `[ASSUMPTION] We will reuse the existing AuthService rather than introduce a new one. Please confirm.`

---

## Step 2 — The Planning Loop

Repeat until the user explicitly agrees:

```
┌──────────────────────────────────────────────────────────┐
│  SEEK CLARIFICATION  →  STATE ASSUMPTIONS               │
│       →  DRAFT / UPDATE PLAN  →  REQUEST AGREEMENT      │
│                                                          │
│  User disagrees or has feedback  →  loop back           │
│  User agrees  →  save plan and exit                     │
└──────────────────────────────────────────────────────────┘
```

**SEEK CLARIFICATION**
- Ask numbered, targeted questions — no more than 5 at a time
- Prioritise the most blocking questions first
- Group related questions together

**STATE ASSUMPTIONS**
- Every unresolved unknown you are deciding yourself must be labelled `[ASSUMPTION]`
- Never silently resolve an ambiguity

**DRAFT / UPDATE PLAN**

Write or revise the plan using this structure:

```markdown
# Plan: <Issue Title> (#<NUMBER>)

## Objective
<One paragraph: what success looks like>

## Scope
### In Scope
- ...
### Out of Scope
- ...

## Approach
<Narrative: technical approach, key design decisions, trade-offs considered>

## Affected Areas
| Area | Files / Modules | Change Type |
|------|----------------|-------------|
| ...  | ...            | Add / Modify / Delete |

## Assumptions
1. [ASSUMPTION] ...
2. [ASSUMPTION] ...

## Open Questions (resolved)
| # | Question | Answer / Decision |
|---|----------|------------------|

## Risks & Mitigations
| Risk | Mitigation |
|------|-----------|
```

**REQUEST AGREEMENT**

End every loop iteration with:
> "Does this plan look good? Please reply **Agreed** to proceed, or share any changes you'd like."

---

## Step 3 — Save the Plan

Once the user agrees:

```bash
# docs/<story_number>/ already exists (created in Phase 0a)
# Write the agreed plan to docs/<story_number>/plan.md
```

Confirm to the user:
> "Plan saved to `docs/<story_number>/plan.md`."

Then signal to the orchestrator: **planning complete, return to implement-user-story skill**.

---

## Edge Cases

**Issue is too large to plan in one session**
Suggest splitting into child issues: `gh issue create` to draft them, then plan each separately.

**User keeps changing scope**
After 3+ loop iterations with major scope changes, surface this explicitly:
> "We've revised scope several times. Would it help to time-box this story more narrowly first?"