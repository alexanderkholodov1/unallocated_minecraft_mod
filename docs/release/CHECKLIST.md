# Release checklist — UNALLOCATED

Owner/captain checklist. Deadline: **2026-10-29 04:59 UTC** (2026-10-28 23:59 Ecuador, UTC−5). Aim to submit by **2026-10-29 04:00 UTC** to keep one hour of margin.

1. **Download the release artifact.** Use the run id and commit SHA of the green `v0.2.0` build:
   `gh run download <run> -n unallocated-release-<sha> -D .tmp/release-<sha>`
   Confirm the directory contains `release.json`, `unallocated-<version>.jar`, `unallocated-<version>.jar.sha256`, `UNALLOCATED-world.zip`, `screenshots/` and, if ffmpeg succeeded, `video/gameplay.mp4`.

2. **Fill the design text.** Create a JSON object with the values produced by the design package (template: `{"DESCRIPTION": "...", "WARNINGS": "...", "TEAM": "...", "CREDITS": "..."}`). Put the accepted known-issue text in a separate UTF-8 file. Do not invent story, plot or team facts.

3. **Run the bundle script.**
   `python3 scripts/release_bundle.py .tmp/release-<sha> --out dist --design .tmp/design.json --known-issues .tmp/known-issues.txt`
   It must exit `0` with no unfilled token. Exit `2` lists the tokens you still need to fill; exit `1` means a missing file, a hash mismatch or an invalid PNG. Never upload an unverified JAR.

4. **Create or edit the itch.io project.** Kind: **Download**. Title `UNALLOCATED`, short description from the page, and horror/content classification as requested by itch. Upload `dist/unallocated-<version>.jar` and `dist/UNALLOCATED-world.zip`; keep the `.jar.sha256` available or paste its hash in the description.

5. **Choose the cover image.** Pick one screenshot from `dist/screenshots/`, crop it to **630×500** pixels, and set it as the cover. Do not stretch it.

6. **Choose 3–5 screenshots.** Use `dist/screenshots/` (title button, map and story captures). Prefer readable, first-person images that show the experience without spoiling the ending.

7. **Add the gameplay video.** Upload `dist/gameplay.mp4` (time-compressed, no sound) or, preferably, a real-time take with sound recorded on a real PC. A short video is recommended by the jam.

8. **Verify the page body.** Paste `dist/itch-page.md` into the page body. Check that the Spanish text renders, that the JAR name and SHA-256 match the uploaded file, and that installation paths are correct for Windows, macOS and Linux. The owner-only note is an HTML comment and must stay invisible to players.

9. **Submit to the jam.** Open the jam page, choose **Submit your project**, select the itch project, and confirm the submission status. Submitting is separate from publishing the page.

10. **Clean-profile test from the itch page itself.** On a launcher profile with no other mods: download the JAR from the public page, install Fabric Loader and Minecraft as the page says, copy only the JAR to `mods/`, launch, accept the contract with **De acuerdo**, press **UNALLOCATED**, and play once to an ending in 10–15 minutes. Also test the `UNALLOCATED-world.zip` fallback once.

11. **Confirm the deadline.** The page must be public and submitted before **2026-10-29 04:59 UTC / 2026-10-28 23:59 Ecuador**. If something fails, fix the page or files, re-run steps 1–3, and re-upload before the deadline; do not push or publish from CI.
