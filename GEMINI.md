@AGENTS.md

## Local tooling

`.claude/settings.local.json` is Claude's local permission history, with one-off commands and paths in it. It is not a
source of build commands or of Gemini's permissions: those are in `~/.gemini/antigravity-cli/settings.json`, and the
commands are the repository's scripts and `AGENTS.md`.
