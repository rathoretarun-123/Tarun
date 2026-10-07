# Kahani Film — v7

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
   * **Mixed languages**: one story can have Hindi lines, English lines and Hinglish lines. Each line is spoken in its own language, and every character keeps the same voice character in both languages.
   * **Hinglish** (Hindi in English letters, e.g. `Meena (hanste hue): "Raju! Mera ribbon wapas do!"`) is understood. It is spoken with Hindi voices, while names, the title and subtitles keep your own spelling. English words inside Hinglish (like "sorry", "thank you") are spoken in English.
3. **🎭 Open studio** lists every character and place, the title and end pages, and what is **still missing**.
   * **📄 Production file**: describes every character, place, shot, voice and sound, with ready-made prompts. Download it, paste the prompts into ChatGPT / Grok / Meta AI, and make the pictures there.
   * **📥 Add many pictures — the director places them**: no special file names needed. The director compares each picture with the script's descriptions and shows where each one goes; tap **Change** if one is wrong, then **Apply**.
     * With internet, an AI vision model identifies the pictures. It works without a key and works better with a Gemini key.
     * Offline, the studio reads the picture itself and compares it with every detail of the description:
       * **people**: dress colours, dress style (lehenga / skirt vs. trousers), moustache, beard, bindi, turban, crown or hat, grey or long hair, a spear in hand, fur (animals, monsters)
       * **places**: every detail that can be seen — forest, river or waterfall, sky, night, cave, palace, sand, snow, flowers, lamps — and the picture with the most matching details wins
     * Each picture goes to one character only, so two pictures never fight over the same role. Check the suggestions before you apply them.
   * **✨ Find pictures in my library for this story**: your saved pictures from earlier stories are matched to the new story's characters and places. A new story offers this by itself the first time you open the studio.
   * **🎙 Find voices in my library for this story**: your saved voice samples (from any story) are matched to the characters by the voice the script describes. Write it in the character's description, e.g. `आवाज़: भारी, धीमी आवाज़`, `Voice: sweet and soft`, `awaaz: patli aur chanchal`.
     * Each sample is measured once: pitch (deep male, male, female, child), speed (slow / fast), tone (smooth / raspy) and how lively it is.
     * It is then compared with the character's age, gender and kind, plus the words the script uses: deep, high, sweet / soft, raspy, slow, fast, loud.
     * Each voice goes to one character at most. You see why it fits ("fits: deep male, slow"), can listen with ▶, untick any, then **Apply**. A new story offers this by itself too.
   * **Real photos become animated avatars**: photos from the camera (or with camera data) are turned into the film's animated style automatically, and people are separated from busy backgrounds. For other pictures, one tap chooses "Make animated avatar" or "It's artwork — keep it".
   * **Missing pictures are created for you**: with internet, every character, place, title and end page you didn't give is created in **3D animated style** by a free AI service before the film is made. These pictures are saved to your library too. Offline, the studio draws them as cartoons.
   * **🖼 Picture** for each character offers several sources:
     * your library, shown 4 at a time with **Next 4**
     * 📂 from your phone
     * 📷 the camera
     * 🌐 a free internet search
     * ✨ AI
     * 🎬 let the studio choose

     A real photo can also be turned into a **🎨 cartoon avatar**.
   * **Voices follow the description** (`आवाज़: खरखरी, काँपती आवाज़`, `Voice: booming`, `awaaz: patli aur chanchal`):
     * deeper, higher, slower or faster
     * real sound effects for **raspy, breathy, trembling, booming / echoing, nasal, squeaky, robotic, ghostly, magical and growling** voices
     * a witch is a little raspy, a giant booms, an old person's voice trembles slightly, even without a description

     The character card shows "🎚 From the story: …" and the production file lists it.
   * **Each line follows its acting direction** in brackets: `(चिल्लाते हुए)` shouts, `(रोते हुए)` sobs and trembles, `(हँसते हुए)` laughs through the words, `(फुसफुसाते हुए)` whispers, `(हाँफते हुए)` pants, `(गाते हुए)` sings, `(धीरे-धीरे)` / `(जल्दी से)` slows down or speeds up, `(दूर से)` sounds far away. This works the same in English (`shouting`, `crying`, `slowly`…).
   * **🎙 Voice**: one list with your recorded voices and the studio's built-in voices, 4 at a time. ★ marks the voices that suit the character's age, gender and described voice, best first, and each of your voices shows what it sounds like and why it fits. **Best match** is the studio voice made deeper, higher, slower or faster as the description asks. Tap ▶ to hear one, or record 10–20 seconds or pick an audio file. **All of that character's lines are made in this voice**, close to your sample's pitch and tone, and changed with feeling (anger, fear, joy…). Tap **🔊** to listen first; tap it again to try another variation.
   * **🎙 Record lines in your own voice**: record any line in your (or your children's) real voice. The film uses it, and the lips move with it.
   * Each place gets a **🔊 Sound** (forest, waterfall, palace, night…) chosen automatically — **your own sounds first** when they fit the place's description. You can change it.
   * **Your own sounds** (📚 Library → 🔊 Sounds → Record or File): nature sounds, background voices (a market, a crowd), effects (a door, thunder, a horse) and music.
     * The studio listens to each one: is it a **background** that loops under a scene, an **effect** that plays once, **music**, or **background voices**, and what it sounds like (rain, river, wind, birds, crickets, bells, thumps, steps…). With a Gemini key the AI listens too and names it much better.
     * You then confirm it in one step: **What is this sound?** — words in English or Hindi (e.g. `rain, बारिश` or `horse galloping`) and its kind. ✎ in the library changes it later.
     * English and Hindi words are linked, so a sound called "rain" is found for a story that says "बारिश", and a crowd recording for "बाज़ार".
     * Backgrounds play under the places that match; effects play at the moment an action or a direction mentions them (e.g. `घोड़ा दौड़ता हुआ आया` plays your horse sound). The **Director's plan** in the studio lists where your sounds will play. Your sounds work in every story.
   * Tap **👄** on a character picture and tap the mouth and eyes, so the lips and eyelids move in the right place.
4. **Where will you post it?** Choose one:
   * YouTube (16:9)
   * Instagram Reel / Shorts (9:16)
   * Facebook / Instagram post (1:1)

   Then choose the quality: 480p, 720p or 1080p.
5. **🎬 Make film**. You can lock the phone or use other apps meanwhile. The notification shows progress and **time left**.
6. **Watch and change.** The film plays inside the app. Describe changes below it in plain English, e.g.
   *the music is too loud • I can't hear Vrinda • make Khan sound deeper and a bit slower • make the king's voice raspy • give the witch a trembling voice • the ghost should have an echo • brighter and warmer • make it black and white • turn the birds down and add captions • the film drags, speed it up • smaller file for WhatsApp • make it for Instagram*

   Then tap **🔁 Make again**. Voices are reused, so this is quicker. When you like it, tap **💾 Download** (Gallery → Movies/KahaniFilm) or **📤 Share**.

## Your library is permanent

* Every picture, voice and sound you add (or record, or that the studio makes with AI) is saved on the phone straight away and stays when the app is closed or the phone restarts.
* Each file is saved with its own description next to it, so even after a crash nothing is lost: files the list does not know are taken back in on the next start.
* A backup copy goes to **Downloads/KahaniFilm/Library**. It stays even if the app is uninstalled. After reinstalling, open 📚 Library → **♻ Restore from backup** and choose that folder.
* Everything in the library can be used in any later story: the director matches it to the new story's descriptions.

## What the film contains

* A **title page** with a picture and music. Then the story plays like a film, with no "Scene 1" cards, and finishes with **समाप्त** (Hindi/Hinglish) or **The End** (English).
* Character and place descriptions are **never read aloud**. A narrator voice is used only when the story has a narrator.
* Subtitles are off by default. Type "add subtitles" to turn them on.
* **Your pictures move like characters**, not like flat cut-outs:
  * the head nods, tilts and turns while talking
  * the arms swing out to gesture, point, clap, or when angry or surprised
  * the legs step when walking
  * the body leans, breathes, slumps when sad and leans back when scared
  * the **face changes with the feeling**: a smile with lifted cheeks, a frown, raised brows in surprise, worried brows when sad or scared, lowered brows and narrowed eyes in anger, squinting when laughing — blended smoothly from line to line, with the lips moving to the voice
  * the studio finds the head, shoulders, waist, legs and hands from the picture's outline, and the eyes and mouth from the face (tap 👄 to correct them)
* Characters drawn by the studio (when you give no picture) have full arms, legs and faces too.
* The camera follows the speaker and comes close in emotional moments, with a soft background (depth of field). Colour grading follows the mood of the scene.
* Music, birds, waterfalls, wind, caves, crowds, swords and bells are real recordings.
* **Music follows the situation inside every scene**:
  * It turns menacing when a villain speaks, tense when someone is scared, playful when they laugh, and to a soft sad piano in sorrow.
  * It swells before shocks and dips under whispers, and short drum hits mark surprises.
  * Your own music (from any story, tagged with words like "happy" or "tense") is used for matching moods.
* **Cinematic look**:
  * the background moves slower than the characters (depth) and blurs in close-ups
  * out-of-focus leaves pass in front of the lens
  * sun rays, a warm key light and cave haze
  * a slowly breathing camera, subtle film grain and slim cinema bars
* **A smarter director**:
  * crane-down establishing shots, two-shots and over-the-shoulder shots, and close-ups on strong feelings
  * slow push-ins on long lines
  * reaction shots of the listener after a shocking line
  * a low, tilted camera for menacing villains and a high angle for a frightened child
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

* **3D:** the film looks 3D when its pictures are 3D-style: your uploads, or the studio's AI pictures, which are always requested in 3D animated style. The camera, depth, light and parallax add a 2.5D cinematic feel.
  * The characters are still flat pictures that move, turn and lip-sync. They are not rigged 3D models, so they don't walk around in true 3D like a Pixar film. That isn't possible with the phone engine.
  * Characters drawn by the studio offline are flat cartoons.
* **Picture characters' movement** is a bending of the flat picture. Small and medium movements look natural. A picture cannot raise its arm over its head, turn around, or truly sit down: sitting and kneeling only fold the legs a little. Arms that are drawn touching the body move less cleanly, and the expressions are only as good as the eye and mouth points (check them with 👄).
* **Recognising sounds offline** is a guess. On the 47 sounds built into the app, the kind (background, effect, music, voices) was right for 43; what it sounds like was close for about two thirds (fire, village, palace and cave sounds were confused). Those are the same sounds the rules were tuned on, so expect less on your own recordings. The words you give always count first. AI listening (Gemini key) was not tested against the live service.
* **Voice effects** were checked by measuring them (roughness, wobble, brightness), not by listening tests with people.
* **Recognising pictures offline** is a best guess. On the sample story's 10 character pictures, the right character was the first guess for 7 and in the top three for 8. The three guards wear identical uniforms and can't be told apart. Places were right for 3 of 8 offline. Check the suggestions; with internet, the AI vision model does this much better.
* **Voice matching** measures pitch reliably. Speed and tone are rougher readings: they were checked with computer voices at known speeds, not with many real recordings. If the story doesn't describe a voice, only age and gender are used.
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
  * `PicSense` analyses pictures offline: photo vs artwork signals, figure vs place, clothing colours from the dress part of descriptions, traits (dress style, moustache, beard, bindi, headwear, hair, spear, fur) read from the figure that `Cutout` finds, place details, and person/background separation. `Toon.avatar` turns photos into avatars.
  * `VoiceStyle` reads voice qualities from descriptions and acting directions and applies them as sound processing (rasp, breath, vibrato/tremolo, growl, ring-mod robot, EQ, reverb, shimmer).
  * `Rig` bends picture characters with a bone-weighted mesh (head, arms, legs, lean, breathing) and a fine face mesh for expressions; `Gfx.imageMesh` draws it (Android `drawBitmapMesh`).
  * `SoundSense` classifies sounds offline; `SoundWords` links English, Hindi and Hinglish sound words; `Director` places the user's effects on matching actions.
  * `VoiceMatch` turns a character's description into the voice it should have and scores measured samples (`VoiceFx.features`: pitch, liveliness, syllable rate, brightness, roughness) against it.
  * `Director.scoreMusic` builds the in-scene score with swells, dips and stingers. `Renderer` draws the cinematic look: ground-anchored parallax, camera roll and drift, light, a blurred foreground, grain and letterbox.
  * `Edits` and `CommandParser` turn the plain-English change requests typed after the preview into edits (an LLM handles anything the rules miss).
  * `Bible` writes the production file. `Toon` turns photos into cartoons.
* `app/src/main/java/com/tarun/kahani/app` holds the Android side (UI in English):
  * `MainActivity`, `Picker`, `Library`, `AudioIO`, `Voices`
  * `FilmJob` + `FilmService` (foreground service, wake lock, notification)
  * `VideoWriter` (MediaCodec), `FilesProvider`, `Prefs`
* **Build:** run `./build.sh`; it doesn't need Gradle.
  * Tools: `aapt2`, `dalvik-exchange`, `zipalign`, `apksigner` and `android-sdk-platform-23` from Ubuntu, plus JDK 17+.
  * Resources are linked against the Android 14 framework from Robolectric's `android-all` jar.
* **Tests:**
  * `tools/robotest` (Robolectric, real Android graphics): screens with an English-only check, pickers, the command box, Hinglish reading, pictures, voices and sounds from the library placed by description, a full film job in 9:16, and frames drawn through `android.graphics.Canvas`. Run with `gradle test`.
  * `tools/jvm`: `MakeFilm` (desktop MP4 with espeak; env `ASPECT`, `EDITS`, `SAMPLE=name=wav`, `AUDIO_ONLY`), `BibleDump`, `CmdTest`, `RigSheet` (picture characters in many poses and feelings, for checking by eye).
