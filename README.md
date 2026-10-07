# Kahani Film — v6

An Android app that turns your story into a cartoon film for children (6–15 years).
Paste a story — in **Hindi, English or Hinglish** — optionally add pictures and voices, and tap **🎬 Make film**.
The studio prepares the voices, music, natural sounds, scenes, camera and lip movement itself.
All screens and instructions in the app are in English; the story, dialogue and voices can be Hindi, English or Hinglish.

**APK:** [`release/KahaniFilm.apk`](release/KahaniFilm.apk) (Android 8.0 or newer)

## Quick guide

1. **Sign in** with your Gmail the first time. The app then asks for microphone, camera and notification permissions — tap **Allow**. **Log out** is at the top of the home screen and in ⚙ Settings.
2. **➕ New film** → paste your story. **🧹 Clear** removes a pasted story.
   * If the story is plain prose (no separate dialogue lines), tap **🤖 Read with AI**. It turns the story into a script with characters, dresses, places, feelings and sounds. Your original story is kept.
   * Script lines look like `Name (feeling): "dialogue"`, scenes start with `Scene 1:` (or `दृश्य 1:`), and a place goes in brackets: `(Place: ...)`.
   * **Hinglish** (Hindi in English letters, e.g. `Meena (hanste hue): "Raju! Mera ribbon wapas do!"`) is understood. It is spoken with Hindi voices, while names, the title and subtitles keep your own spelling. English words inside Hinglish (like "sorry", "thank you") are spoken in English.
3. **🎭 Open studio** lists every character and place, the title and end pages, and what is **still missing**.
   * **📄 Production file**: describes every character, place, shot, voice and sound, with ready-made prompts. Download it, paste the prompts into ChatGPT / Grok / Meta AI, and make the pictures there.
   * **📥 Add many pictures at once**: save the pictures with the character or place name (e.g. `Vrinda.jpg` or `वृंदा.jpg`). The app recognises each name and puts the picture in the right place.
   * **🖼 Picture** for each character offers several sources:
     * your library, shown 4 at a time with **Next 4**
     * 📂 from your phone
     * 📷 the camera
     * 🌐 a free internet search
     * ✨ AI
     * 🎬 let the studio choose

     A real photo can also be turned into a **🎨 cartoon avatar**.
   * **🎙 Voice**: record 10–20 seconds or pick an audio file. **All of that character's lines are made in this voice**, close to your sample's pitch and tone, and changed with feeling (anger, fear, joy…). Tap **🔊** to listen first; tap it again to try another variation.
   * **🎙 Record lines in your own voice**: record any line in your (or your children's) real voice. The film uses it, and the lips move with it.
   * Each place gets a **🔊 Sound** (forest, waterfall, palace, night…) chosen automatically. You can change it.
   * Tap **👄** on a character picture and tap the mouth and eyes, so the lips and eyelids move in the right place.
4. **Where will you post it?** Choose one:
   * YouTube (16:9)
   * Instagram Reel / Shorts (9:16)
   * Facebook / Instagram post (1:1)

   Then choose the quality: 480p, 720p or 1080p.
5. **🎬 Make film**. You can lock the phone or use other apps meanwhile. The notification shows progress and **time left**.
6. **Watch and change.** The film plays inside the app. Type changes below it in English, Hindi or Hinglish, e.g.
   *increase brightness • lower the music • background sounds off • make Vrinda louder • make Khan's voice deeper • add subtitles • smaller file size • make it for Instagram • music thoda kam karo*

   Then tap **🔁 Make again**. Voices are reused, so this is quicker. When you like it, tap **💾 Download** (Gallery → Movies/KahaniFilm) or **📤 Share**.

## What the film contains

* A **title page** with a picture and music. Then the story plays like a film, with no "Scene 1" cards, and finishes with **समाप्त** (Hindi/Hinglish) or **The End** (English).
* Character and place descriptions are **never read aloud**. A narrator voice is used only when the story has a narrator.
* Subtitles are off by default. Type "add subtitles" to turn them on.
* The camera follows the speaker and comes close in emotional moments, with a soft background (depth of field). Colour grading follows the mood of the scene.
* Music, birds, waterfalls, wind, caves, crowds, swords and bells are real recordings.
* Films can be up to 30 minutes long. The sound is built in pieces and kept on storage, so long films don't run out of memory.

## Voices

* **Natural voices (default, needs internet, no key):** Microsoft's free neural voices sound like real people. Hindi uses Swara (female) and Madhur (male); English has Indian, British and children's voices. Every character gets its own pitch and speed. This is the free voice behind Edge's "Read aloud" and is not an official service for apps, so it could stop working. If it does, the app falls back to the phone's own voice and tells you.
* **Your voice samples** move the voice towards your sample's pitch and tone. This is not true voice cloning. For a fully real voice, use **Record lines in your own voice**.
* **AI voices (Gemini, optional)** are expressive, but the free daily limit is very small, so they are off by default.
* The first test video I made earlier used **espeak**, a robotic computer voice, because the build machine has no good voice. The phone never uses espeak.

## Free AI (optional)

The app works **without any key**. Story reading and AI pictures use free services, and free pictures and sounds can be searched on Openverse / Wikimedia; all of these need internet.
For better story reading, picture recognition and expressive AI voices, add a **free Google Gemini key** in ⚙ Settings (aistudio.google.com/apikey). The key stays only on your phone. Never send it to anyone or put it on GitHub.

## Honest limits

* **Animation:** the film is 2D cartoon, not 3D (Pixar-like). Lips move on your picture's mouth with the voice; the whole face does not move in 3D.
* **Speed:** a 10-minute 720p film should take roughly 15–30 minutes on a phone with 8 GB of RAM, and longer at 1080p. This is an estimate; it has not been measured on a phone.
* **Hinglish spelling:** Hinglish is changed into Hindi letters by rules plus a word list, so unusual words or names can be pronounced a little wrong. **Read with AI** (online) writes a proper Hindi script and fixes this.
* **Online features were not tested live.** The AI, internet search, natural voices and Gemini can't be reached from the machine the app was built on. They were tested against mock servers built from the services' protocols, not the live services.

---

## Developer notes

* `app/src/main/java/com/tarun/kahani/core` holds the platform-independent engine.
  * `ScriptParser` turns a script into a `Story`, and supports Hindi, English and Hinglish. `Hinglish` handles detection, rule- and dictionary-based Devanagari conversion, and mapping back to the user's spelling.
  * `ScriptAI` uses an LLM via `Cloud` to rewrite prose or Hinglish stories, map free-form edit requests to commands, and identify uploaded pictures.
  * `LookDesigner`, `Director`, `Renderer`, `Puppet` and `Sets` handle design, staging, camera and drawing.
  * `EdgeVoice` is a minimal WebSocket client for the Edge read-aloud neural voices. It handles the Sec-MS-GEC token and per-character/emotion SSML prosody, and returns MP3. It was verified against a protocol mock built from the `edge-tts` reference.
  * `Mixer.mixTo` streams the soundtrack in chunks with a limiter. `SoundLib` provides the recorded sounds. `VoiceFx` does pitch/tempo changes and voice-sample matching. `Grade` applies colour edits.
  * `Edits` and `CommandParser` handle the post-preview command box in English, Hindi and Hinglish.
  * `Bible` writes the production file. `Toon` turns photos into cartoons.
* `app/src/main/java/com/tarun/kahani/app` holds the Android side (UI in English):
  * `MainActivity`, `Picker`, `Library`, `AudioIO`, `Voices`
  * `FilmJob` + `FilmService` (foreground service, wake lock, notification)
  * `VideoWriter` (MediaCodec), `FilesProvider`, `Prefs`
* **Build:** run `./build.sh`; it doesn't need Gradle.
  * Tools: `aapt2`, `dalvik-exchange`, `zipalign`, `apksigner` and `android-sdk-platform-23` from Ubuntu, plus JDK 17+.
  * Resources are linked against the Android 14 framework from Robolectric's `android-all` jar.
* **Tests:**
  * `tools/robotest` (Robolectric, real Android graphics): screens with an English-only check, pickers, the command box, Hinglish reading, a full film job in 9:16, and frames drawn through `android.graphics.Canvas`. Run with `gradle test`.
  * `tools/jvm`: `MakeFilm` (desktop MP4 with espeak; env `ASPECT`, `EDITS`, `SAMPLE=name=wav`, `AUDIO_ONLY`), `BibleDump`, `CmdTest`.
