name: Bug report
description: Create a report to help us improve
title: '[Bug]: '
labels: [bug]
assignees:
  - Briiqn
body:
  - type: markdown
    attributes:
      value: |
        # LCE Bug Report (Reunion)
        Please fill out the information below so we can accurately reproduce the issue.

  - type: checkboxes
    id: environment_checks
    attributes:
      label: Mandatory Reproduction Environment
      description: You MUST confirm these environments before submitting a bug report.
      options:
        - label: "**Reunion Build:** I am using a **Debug Build** (compiled without `-O3` optimization) to ensure accurate stack traces."
          required: true
        - label: "**Client Source:** I am running **smartcmd/MinecraftConsoles** directly from an **IDE** (e.g., Visual Studio 2022) rather than using a pre-compiled nightly binary."
          required: true

  - type: textarea
    id: description
    attributes:
      label: Describe the bug
      description: A clear and concise description of the issue. Specify if this is a crash, a protocol hang, or a logic error in packet translation.
    validations:
      required: true

  - type: input
    id: reunion_hash
    attributes:
      label: Reunion Commit Hash
      placeholder: e.g. 8bf0343
    validations:
      required: true

  - type: input
    id: lce_hash
    attributes:
      label: MinecraftConsoles Commit Hash
      placeholder: e.g. 8bf0343
    validations:
      required: true

  - type: input
    id: backend_version
    attributes:
      label: Backend Version
      placeholder: e.g. Paper 1.8.8 / Fabric 1.20.1
    validations:
      required: true

  - type: textarea
    id: reproduction
    attributes:
      label: To Reproduce
      description: Steps to reproduce the behavior.
      value: |
        1. Start the Reunion proxy in your terminal/IDE.
        2. Launch `Minecraft.Client` from your IDE (Visual Studio/CMake).
        3. Connect to the proxy (e.g., `127.0.0.1:25565`).
        4. Perform action: ....
        5. Observe the error in either the Proxy console or the IDE's output window.
    validations:
      required: true

  - type: textarea
    id: expected_behavior
    attributes:
      label: Expected behavior
      description: A clear description of what the LCE client should be doing (e.g., "The crafting UI should populate items without disconnecting the client").
    validations:
      required: true

  - type: textarea
    id: screenshots
    attributes:
      label: Screenshots / Media
      description: Capture any relevant UI bugs or in-game behavior from the `MinecraftConsoles` window. Drag and drop images/videos here.
    validations:
      required: false

  - type: textarea
    id: context
    attributes:
      label: Additional context
      description: Add any other context (e.g., `config.yml` settings, if `via.enabled` is true, or specific network forwarding modes like `BUNGEEGUARD`).
    validations:
      required: false

  - type: textarea
    id: logs
    attributes:
      label: Technical Logs & Stack Traces
      description: Because you are running in an IDE and using a Debug build of Reunion, please provide the full stack trace and any packet logs below.
      render: text
    validations:
      required: true
