# Contributing

Before opening a PR, read this.

## Who Can Contribute

Reunion sits at the intersection of the LCE protocol and Java protocol development. This is a niche area and the codebase reflects that. We expect contributors to have a demonstrable background in at least one of:

- **LCE protocol development**: prior work on LCE networking, reverse engineering, tooling, or implementations (e.g. LCEMP or similar)
- **Java Edition protocol development**: meaningful experience with Protocol 47 (1.8.x), ViaVersion internals, BungeeCord/Velocity, or packet-level Java networking
- **A visible open-source history** that shows you know what you're doing in this space

If your background is primarily frontend or you're new to Java networking, this is not the right project to start with.

**Alternate accounts are not allowed.** If you're found using an alt to bypass a ban or obscure your identity, both accounts get banned.

## LLM Usage

LLM use is allowed, but only with moderation. We're not against the tools: we're against shipping code you don't understand.

Acceptable:
- Using an LLM to draft boilerplate, write Javadocs, or get unstuck
- Using LLM output as a starting point that you then actually read and adapt

Not acceptable:
- Copying LLM output into a PR without reading it
- Submitting code that references classes or patterns that don't exist in this codebase
- Packet handling logic that looks right but has never been run

**Do not commit without testing.** Reunion is used in production. Boot the proxy, verify your change, and test against the relevant TU version(s) before opening a PR. Any commit that is obviously LLM slop from someone who doesn't know what they're doing will result in a permanent ban from contributing.

## Pull Requests

- One change per PR: don't bundle a bug fix with a refactor
- Write a clear description of what changed, why, and how you tested it
- Match the existing code style, don't reformat unrelated code
- Don't open PRs for incomplete work unless it's a draft discussed beforehand

## Forks & Backporting

We do **not** accept contributions that add support for unofficial client forks or community-backported content. See [Forks & Backporting](README.md#forks--backporting) in the README.
