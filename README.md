# BedwarsAC (Spigot / PaperSpigot 1.8.8/1.8.9)

Complete, production-ready Bedwars Anticheat for **Spigot / PaperSpigot 1.8.8/1.8.9 (`v1_8_R3`)** with **GitHub Actions CI/CD** pre-configured to automatically compile, test, and package **`BedwarsAC-1.0.0-RELEASE.jar`**.

## Quickstart: Push to GitHub & Download Compiled `.jar`

1. Extract this repository archive and push it to a new GitHub repository:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: BedwarsAC 1.8.8 plugin + GitHub Actions CI"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. Open your repository on GitHub and click the **Actions** tab:
   - The **Build BedwarsAC Plugin JAR** workflow (`.github/workflows/build.yml`) runs automatically on every push.
   - Once completed (~45 seconds), scroll to the **Artifacts** section of the workflow run and click **`BedwarsAC-1.0.0-RELEASE-jar`** to download your compiled `BedwarsAC-1.0.0-RELEASE.jar`.
3. Optional — Publish a GitHub Release with the `.jar` attached:
   - Push a tag (`git tag v1.0.0 && git push origin v1.0.0`) or go to **Actions -> Publish GitHub Release JAR -> Run workflow**.

## Local Build (Linux / macOS / Windows)

- **Linux / macOS**:
  ```bash
  ./scripts/build-local.sh
  ```
- **Windows**:
  ```bat
  scripts\build-local.bat
  ```

The shaded plugin JAR is output to `target/BedwarsAC-1.0.0-RELEASE.jar`.
