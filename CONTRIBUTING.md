# Contributing to OpenJUMP

Thanks for your interest in contributing! OpenJUMP is maintained by a small team of volunteers,
so clear, focused contributions are the most useful kind.

## Setting up a development environment

OJ uses Maven and works with Eclipse, IntelliJ IDEA, or any Maven-aware Java IDE. For a full
walkthrough of setting up **OJ Core** or the **HelloWorld Extension**, see the wiki:

https://ojwiki.soldin.de/index.php?title=Eclipse:_Set_up_project_and_example_extension_from_git_sources

To build from the command line:

```
mvn -B package -P snapshot
```

This is also what CI runs on every push, so it's the quickest way to confirm your change builds
cleanly before opening a PR.

## Before opening a pull request

- **Build and run your change locally first.** A PR without at least a local `mvn -B package -P
  snapshot` pass (and, for anything touching the UI, an actual run of the built application)
  isn't ready for review yet.
- **Keep it scoped.** One logical change per PR — please don't bundle an unrelated fix, refactor,
  or formatting pass in with the change you're actually proposing. Smaller PRs are easier to
  review and easier to revert if something goes wrong.
- **Reference the issue it addresses**, where one exists, in the PR description.
- If you're fixing a bug reported a while ago, it's worth confirming it still reproduces against
  current `main` first — some issues predate later refactors and may already be partially or
  fully resolved.

## Issues

Bug reports and proposals are welcome as GitHub issues. Please include the OpenJUMP version,
your operating system and Java version, and the steps to reproduce the problem. If you've
already found and fixed something, you're welcome to open a pull request that references the
issue.

## Licence

OpenJUMP is licensed under the GNU General Public License v2 (GPLv2). By submitting a
contribution, you agree it will be distributed under the same licence.

## Extensions

If your contribution is really a standalone plugin rather than a change to OJ Core, consider
whether it belongs in its own extension repository instead — see the README's Extensions section
for the naming convention (`xxx-extension`, `xxx-driver`) and the
[HelloWorldExtension](https://github.com/openjump-gis/helloworld-extension) example.
