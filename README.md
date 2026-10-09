# Kahani Film — v15

An Android app that turns your story into a cartoon film for children (6–15 years).
Paste a story — in **Hindi, English or Hinglish** — optionally add pictures and voices, and tap **🎬 Make film**.
The studio prepares the voices, music, natural sounds, scenes, camera and lip movement itself.
All screens and instructions in the app are in English; the story, dialogue and voices can be Hindi, English or Hinglish.

**APK:** [`release/KahaniFilm.apk`](release/KahaniFilm.apk) (Android 8.0 or newer)

## New in v16

* **The Technical Director protocol, audited rule by rule** (`docs/technical-director.md`, "The v16 audit"). Four places were only written down; now they are code:
  * **Pipeline steps 1–2 — Character Lock Sheets and Location Lock Plates as pictures, always:** before any shot, every character of the cast (photographed or drawn) is rendered standing, front view, on a plain plate exactly as the film draws them, and every place is rendered as the film shows it with no characters. They are saved with the film (`lock_char_N.jpg`, `lock_place_N.jpg`) and shown first in Human QC.
  * **Pipeline step 6 — "if the clip shakes or morphs, reduce motion 80 % and regenerate", done by the studio:** before the film is made, every shot is played frame by frame at check size (its start, middle and end, four frames in a row each) and every two frames in a row are compared. A shot whose picture changes more than a locked camera and one small action can explain has its motion cut by 80 % and is played again from the same first frame (`core/FinalQc.java`). The limits come from measuring four test films (532 shots), not from guessing; the shakes the story asks for and whole-frame story effects (a flash, lightning, a blackout, a glitch) are counted apart.
  * **Pipeline step 8 — the final QC at 0.25x:** the finished film is metered frame by frame as it is written (morphing, shake); floating is read from the renderer's own record of where every standing character's feet were drawn against the ground line; the finger-count rule is stated for drawn and photographed hands. The whole check is in the film's quality check: 📄 Descriptions → "THE LAST FILM MADE" (also `qc.txt` with the film).
  * **Lip-sync face fill (8.3) measured and validated:** every lip-sync shot's face fill is measured; one outside 65–75 % (or outside what the whole head, the centre 60 % of a narrow frame and the picture's sharpness allow) is reframed on the face; the quality check reports the average. The 30–90 s objective is reported against the script's length.
* **A fix found by the audit:** a scene in a "small living room" was staged in a dark mall basement ("mall" matched inside "small"). English place names are now whole words.

## New in v15

* **Any script, any century.** Tested on a new 21st-century story (a coder girl, a robo-dog, an AI witch, a corporate villain, a rooftop neon garden and a mall basement): the script reader now understands free-form scripts — roles after names (`तारा मल्होत्रा (16 वर्ष) - The Coder Didi`), lines whose manner comes before the colon (`इनाया अपने bag से diary निकालते हुए: "…"`), a laugh or a shout written inside the narration (`हँसता है - "हा हा हा!" -`), voices that are heard but never pictured (`मधुर AI आवाज़:`), off-screen lines (`एल्गोरा की आवाज़ मीठी होकर:`), a character's gender from the script's own verbs, scenes without a `(स्थान:)` line (they happen at the known place their title or first paragraph names, or where the last scene was), the hour from the scene's own words, and a `कहानी - …` line as the story's name.
* **Places and looks of today:** a city rooftop (skyline, drones, planters of solar flowers that glow at night), a closed mall's basement (arcade machines, a flickering green tubelight, a rusty shutter), a modern room and a city street — painted, lit (a green flickering key in the basement, the city's neon glow at night) and sounding (traffic, drones, an electric hum) like the old places. Drawn characters can wear a hoodie, a t-shirt and shorts, a suit, a trench coat or jeans; a ponytail, a hood, round or dark AR glasses (glowing red), an LED clip, earphones, light-up shoes; hold a phone, a game controller, a laptop or an RGB selfie stick; and a robot dog is made of metal with LED screens for eyes, an antenna, a little jacket and a bandana. A "Corporate Rakshas" is a man in a suit, not a monster; a "डायनासोर" on a t-shirt is no "डायन".
* **The director takes cues nobody hardcoded** (`core/Cues.java`): a sound written as a word — "टिम-टिम", "खट-खट", "फुस्स", "खटाक", "बीप", "कर-कर", "टप-टप", "पप-पप", "crash", any word in quotes — is made from its letters (taps, pops, clicks, thuds, hisses, jingles, buzzes); lights coming on, twinkling, flickering or going out, a glitch or a hack, a stolen data stream, a flashlight beam, a drone, a spark from a pulled wire, an explosion, steam, a notification popping up, hearts — each becomes an effect on the stage with its sound. Nineteen new synthesized sounds (hum, traffic, drone, beep, click, typing, buzz, glitch, power-down, spark, heartbeat, twinkle, blip, crackle, hiss, tap, boom…).
* **The protocol, refined and extended, hardcoded:** Pixar rules 2, 3, 6, 12, 15, 16 added; Braintrust Q5 (face same as the lock sheet); Disney's numbers (a jump stretches to 120 % and squashes to 80 %); a scared run steps on twos; a drawn character keeps a hair's breadth of hand-drawn wobble (Miyazaki's 10 %); music as a character (a heartbeat under scary parts); the cinema format's 20 % headroom; the eight-point final checklist answered in every descriptions file; the text as given in ⚙ Settings → 📜 The director's protocols.
* **Less clutter:** ⚙ Settings opens with one 🔑 keys card (Gemini, ElevenLabs, Freesound, Pixabay, Pexels — add, test, change in a small dialog; what is built in without a key listed below), then the director's switches in one card. The story screen is three cards (story, studio, film); the format and quality are asked in the dialog before making.
* **Finer meshes:** about one mesh cell per 1.5 screen pixels for characters and faces (was 2).

## New in v14

* **The Pixar-Lead protocol v4.0 is hardcoded** (`core/PixarLead.java`, every rule and how it is enforced in `docs/pixar-lead.md`; the text exactly as given in ⚙ Settings → 📜 The director's protocols). It works for **any script**, not only the sample:
  * **Story engine:** every script gets its six-beat spine (R4), its hero and its acts; the colour script follows the act (contrast rises to the climax, warmth returns at the end). The **Braintrust** asks its four questions every 5 shots (story clear without dialogue? a strong opinion? a ma pause? costume culturally accurate?) and checks R5 (at most four characters per 60 s) — suggestions in the descriptions and the quality check; the director decides.
  * **Deakins: two lights only.** Every frame is lit by one key light (its side, colour and strength from the place and the hour) and one bounce from the ground in the ground's own colour. Garden warm yellow, cave green with orange accents, night blue.
  * **Disney's 12 principles** as tags in every clip's prompt, plus **secondary action** while idle (a slow weight shift, a glance), and **timing by weight** (a monkey moves faster, a giant slower).
  * **Enhancers:** Miyazaki's *ma* — after two fast beats a quiet 1.8 s shot on the hero; Russo — one comic beat per scene (a head scratch and a shrug by whoever can carry a joke); Gunn — a scary shadow sweeps through a funny scene when the story has a villain; Nolan — steps by the floor's material (stone, marble, cave, earth; running steps for runs); Narsimha — a note wherever a costume has no real garment name; Spider-Verse — **animation on twos** (⚙ Settings: experts 24, learners 12, naughty characters 8 fps; off by default).
  * **Photo resizing for the format:** five formats — YouTube 16:9, Reels 9:16, square 1:1, Instagram portrait 4:5, cinema 2.39:1 — each with its own safe zones (eye line, headroom, caption zone, side margins) and **character scale lock** (60 % / 50 % / 65 % / 60 % of the frame height). AI pictures are made natively at the format's size (1920×1080, 1080×1920…), their real width and height are **verified** and a stretched one is made again. **First-frame checks:** head inside the frame, feet of full shots in the bottom 85–98 % with the shadow. The QC stills show the safe zones drawn over each first frame.
  * **Thumbnail and poster made separately** (never resized from a frame): after every film a 1280×720 thumbnail (the hero's face, 60 % of the frame, on the hero's palette colour) and a 1080×1920 poster (full body, 40 % empty at the top for the title) — 🖼 / 🪧 buttons on the player screen, shareable.
* **Any script:** characters whose scene is on the water stand **in the boat**, rolling and bobbing with it; a storm or heavy rain darkens the whole frame; the painted waterfall appears only in the palace garden. Two more test stories (`tools/testdata/`) run in the tests next to the sample.
* The command box understands "Instagram portrait" (4:5) and "cinema" (2.39:1).

## New in v13

* **The director asks before generating anything:**
  * when you tap 🎬 Make film it asks **where the film will be shown** (YouTube 16:9, Reels/Shorts 9:16 or square 1:1 — decided once, as the protocol says), lists the **pictures still missing** and asks whether AI may make them;
  * after planning, it makes the **first frame of every shot** (and shows each character's picture) and **waits for your check (Human QC)**. Tap any shot to fix it: *calmer* (motion cut by 80%), *closer*, *wider*, *show the listener instead* (when lip-sync looks wrong), or *no cut here*. Tap **Approve** and the film is made exactly from that plan. (⚙ Settings → Human QC, on by default.)
* **Pixel-level meshes for everything:** characters, faces, animals, places, title and end pages and insert shots are bent through about one mesh cell per 2 screen pixels. Characters lying down or hanging upside down now breathe and speak through their meshes too.
* **A more cinematic, classier finish on every frame:** a filmic tone curve (soft highlights, gently lifted shadows), a soft glow around bright light, richer colour where it was dull, warm highlights and cool shadows, and a **colour script** — warmer for happy parts, cooler and quieter for sad or tense ones, blended across cuts.
* **Rim light:** every character catches the place's key light along the edge of their outline (warm sunlight, softer indoors and at night), so they sit in the picture instead of looking pasted on.
* **Framing:** a single character in a wider shot stands on a third of the frame with room in front of their gaze; close-ups always keep the whole head, hair or turban in the frame with room for small moves.
* **The protocol exactly as you gave it** is stored in the app and can be read in ⚙ Settings → 📜 The director's protocol.

## New in v12

* **The Technical Director protocol is hardcoded** (`core/TechnicalDirector.java`, full text in `docs/technical-director.md`) and enforced by the director, not only written down:
  * every spoken line is shown in **front-facing close-ups framed on the face**, with **at most six words per shot** (a long line becomes e.g. 4 + 5 words with the listener's silent reaction between); the speaker stops walking to speak and the head stays still;
  * **no character moves 15 % of the frame in one shot** (runs are slowed to walking pace where the story's timing allows; otherwise the action is cut into several still shots);
  * **one action per shot**, shots of about **3 seconds and never over 4**, a locked camera in every shot (also in vertical films, where the camera used to pan after the speaker);
  * the **validation layer** checks every shot of the film and every prompt in the descriptions file, and the **error correction** fixes what fails; the shot list's quality check reports the result.
* **Meshes down to pixel level:** each picture gets about one mesh cell per 2 screen pixels (up to 256 × 512 over a person and 256 × 256 over the face in a close-up). The face layer is cut from your **full-resolution** picture, so close-ups are sharp.
* **Smoother, more realistic movement:** gestures ease in and out, a change of feeling settles over half a second, characters bend a little before setting off and settle after stopping, hair and hems follow through after the head turns, walking no longer hops, and a new instruction never makes anyone jump.
* **Resizing as the protocol says:** FINAL_AR (16:9, 9:16 or 1:1) is decided once. Places, title and end pictures made by AI are made in that shape (never cropped from another); every shot is framed for that shape with whole characters, 15 % empty at the sides, headroom, faces in the centre 60 %, and no neighbour cut in half at the edge of a close-up.

## New in v11

* **Steady lip sync, no second lips:** the face is drawn as a finer layer that now moves exactly with the body under it, so a second pair of lips or eyes can no longer show through. The body no longer bobs with every syllable (that was the shaking), the head stays almost still while speaking, and the mouth glides from syllable to syllable, opening a moment before the sound as real speakers do. No painted lips are drawn over the real ones.
* **Finer meshes:** 80 × 160 points over a person, 160 × 80 over an animal, 96 × 96 over the face and 192 × 108 over every place.
* **Eyes and mouth found much better on clear pictures:** on the 10 sample characters the automatic eye points are now about 0.10 eye-distances from the hand-placed ones (was 0.58) and the mouth 0.15 (was 0.66). It handles dark skin, eyebrows (no longer taken for eyes), turbans and jewellery, and mouths under a moustache.
* **Backgrounds removed from busy photos:** a character photo with a garden, room or crowd behind it is cut out by learning the character's and the background's colours (on test pictures 88 % match, was 51 %).
* **Resizing for Instagram / Facebook / WhatsApp without distortion:** pictures are always cropped or fitted in proportion, never stretched; a picture of a very different shape is shown whole over a soft enlarged copy of itself.
* **Caps and turbans come off for real:** when a monkey snatches a guard's cap or turban, the guard is shown bare-headed (from his own picture) and the monkey wears that very cap. "Give my cap back!" in the dialogue is enough for the director to show it that way.
* **Characters stand on each background's own floor**, found in the picture, with soft contact shadows. Out-of-focus backgrounds in close-ups are now smoothly blurred, not blocky.
* **Library:** the backup copy is now private (it no longer appears in the phone's gallery or music player; old copies are moved once). Every sound has ▶/⏸ and ■. When online, the director asks the AI about library pictures it is unsure of (once per picture) and uses library pictures of objects in the story (a mirror, a bell, a letter) as close-up inserts.
* **Pause** while a film is being made (⏸ Pause / ▶ Resume).
* **Story reading with AI** no longer says "AI answer is not usable": a story already written as a screenplay is read directly, long stories are read in parts, and the AI's formatting is cleaned.
* **Swords** rest at the side and swing only in a fight.

**Background pictures** work best when they show only the place, with no people or animals in them: the director then places the characters on the floor of the picture at the right size.

## Quick guide

1. **Sign in** with your Gmail the first time. The app then asks for microphone, camera and notification permissions — tap **Allow**. **Log out** is at the top of the home screen and in ⚙ Settings.
2. **➕ New film** → paste your story. The light **🧹 Clear the whole story** button right under the story box removes it (it asks first).
   * Anything before **Scene 1** / **दृश्य 1** (writer's notes, the character list, the places) is only read for looks, voices and places. It is **never acted or spoken**; the film starts at Scene 1.
   * Character lists can be numbered (`1. Meera (9 years):` with lines under it) or one per line (`Meera (9 years): a curious girl in a yellow frock`, `Tommy – a small white dog`), in Hindi or English.
   * If the story is plain prose (no separate dialogue lines), tap **🤖 Read with AI**. It turns the story into a script with characters, dresses, places, feelings and sounds. Your original story is kept.
   * Script lines look like `Name (feeling): "dialogue"`, scenes start with `Scene 1:` (or `दृश्य 1:`), and a place goes in brackets: `(Place: ...)`.
   * **Mixed languages**: one story can have Hindi lines, English lines and Hinglish lines. Each line is spoken in its own language, and every character keeps the same voice character in both languages.
   * **Hinglish** (Hindi in English letters, e.g. `Meena (hanste hue): "Raju! Mera ribbon wapas do!"`) is understood. It is spoken with Hindi voices, while names, the title and subtitles keep your own spelling. English words inside Hinglish (like "sorry", "thank you") are spoken in English.
3. **🎭 Open studio** lists every character and place, the title and end pages, and what is **still missing**.
   * **You don't have to press anything for the library.** Before every film the director looks through your library by itself:
     * a character, place, title or end page without a picture gets the library picture that clearly fits it (saved under the same name, the same words as its description, or the same look)
     * a character without a voice gets the saved voice named after it, or one that clearly matches the voice the script describes
     * when a story needs rain, a river, the sea, a market, a forest… and your library has no recording of it, a free real recording is fetched (with internet), saved to your library and used — also in every later story
     * only confident matches are used; the progress screen shows "📚 Taken from your library: …", and the studio shows them like your own choices, so you can change any of them
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
* **Where it is:** in the app's own storage on the phone (`/data/data/com.tarun.kahani/files/library`, folders `pic`, `voice`, `sound`), plus a private backup copy in **Downloads/KahaniFilm/Library** (saved as `.kfbak` files, so galleries and music players don't show them). The library is not inside the app, so the app stays small however much you add. Nothing is ever deleted unless you delete it.
* **Add many at once:** 📚 Library → **➕ Add many (photos, sounds, voices)** opens the phone's picker with your whole gallery and files. Long-press to select as many photos, sounds and voice samples as you like, at any time. Pictures, voices and sounds are sorted by themselves (an audio file whose name says "voice", "आवाज़", "dialogue" or "sample" becomes a voice; other audio becomes a sound). The 📂 buttons in each tab also take many at once.
* **Pictures from your older stories** (also from versions before the library existed) are copied into the library once, named after their character or place and described with that story's words. A picture that is already in the library is recognised and not added twice.
* Each file is saved with its own description next to it, so even after a crash nothing is lost: files the list does not know are taken back in on the next start.
* A backup copy goes to **Downloads/KahaniFilm/Library**. It stays even if the app is uninstalled. After reinstalling, open 📚 Library → **♻ Restore from backup** and choose that folder.
* Everything in the library can be used in any later story: the director matches it to the new story's descriptions.

## The director

* **Trained on two guides** (see `docs/director-training.md` and `docs/technical-director.md`):
  * a Pixar-style directing guide: emotion decides the shot, scenes build to a peak and release, reactions are shown and allowed to breathe, close-ups are kept for turning points, light follows mood, each character has its own body language, giants are revealed, time jumps dip to black
  * the Technical Director protocol (on by default): a locked tripod for every shot, shots of about 3 seconds, lines of more than six words in front-facing close-ups, long lines split into short shots with the listener in between, faces in the safe zone with headroom, feet on the ground with contact shadows, no camera shake unless the story asks
* **Descriptions for other apps** (📄 under the story, and in the studio): Character Lock Sheets (costume copied word for word, height in feet, voice), Location Lock Plates (foreground, midground, background, ground, light), every object in the story, every background sound, effect and voice, and a table of **every shot** with the six fields (CHARACTERS, PLACEMENT, ACTION, GROUNDING, LIGHTING, CAMERA), lip-sync split into close-ups of at most six words, a ready first-frame image prompt and a video prompt from that frame, and the validation checklist. Make anything you like in another app and upload it: the studio places it by name.
* After each film, the director's **shot list and quality check** (in the guide's shot format) is part of the descriptions file.

## What the film contains

* A **title page** with a picture and music. Then the story plays like a film, with no "Scene 1" cards, and finishes with **समाप्त** (Hindi/Hinglish) or **The End** (English).
* Character and place descriptions are **never read aloud**. A narrator voice is used only when the story has a narrator.
* Subtitles are off by default. Type "add subtitles" to turn them on.
* **Pixel-level meshes:** every picture is bent through a grid of about one cell per 2 screen pixels — at least 80 × 160 over a person (160 × 80 over an animal) and up to 256 × 512 in a close-up, 96 × 96 to 256 × 256 over the face, and up to 960 × 960 over a place (a place where nothing moves is mapped exactly to the pixel).
* **Finer physics:** denser, thinner rain with more splashes, six ripple rings after a stone, more spray drops that slow in the air, more snow, leaves, petals, dust and fireflies.
* **Lip-sync with the real lips:** the face mesh opens the jaw — the lower lip and chin come down and the lips part, showing teeth and tongue inside. The mouth shape follows the sound of each moment: wide for "ee" and "s", round for "oo" and "o", closed between words.
* **Follow-through:** loose hair and the hem of a skirt keep swaying a little after every move (more while walking), and breathing gently lifts the shoulders.
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
* **Scenes flow into each other** with a soft dissolve, without "Scene 1" cards.
* **Nature and weather appear when the story says so — or only implies it** ("they ran for shelter", "the lamp flickered in the wind"). The director reads the words itself, and with internet an AI reads the story once more for anything only implied:
  * **rain** falls in streaks with splashes on the ground, clothes and hair **get wet** (darker, shiny) and drip, and dry slowly afterwards
  * **wind** blows hair, dupattas, skirts and leaves, grass and plants sway, and leaves or petals fly
  * **storms** darken the sky with heavy clouds and lightning flashes with thunder; boats pitch and tremble
  * **snow**, **fog**, **dust storms**, **earthquakes** (the picture shakes, dust falls, people are scared), a **rainbow** after rain, **stars** and **fireflies** at night, **birds** in the morning sky
  * **fire**: campfires, **candles, diyas and torches** flicker and **light up the night** around them
  * **water**: the sea and shore waves roll; a river or waterfall in your place picture flows when the studio can find it; a **stone thrown into a pond** splashes and sends out rings of ripples
  * **physics**: thrown balls and stones fly in a real arc and bounce, fruit falls from trees under gravity, a jump crouches, flies and lands
* **Sitting down and getting up** are smooth movements. Kings and queens sit on a **golden throne**, others on a stool or a rock, or on the floor outdoors. People also **bow with namaste, wave, nod and turn**. A hand held at the face (like a guard twirling his moustache) keeps fidgeting instead of staying frozen.
* **Animal pictures move like animals:** the head turns and nods, the **jaw opens with the words** (lip-sync), ears flick and fold back when angry or scared, the **tail wags** when happy, hangs when sad and tucks in when afraid, legs walk in pairs, and an animal lies down by folding its legs. The background seen between the legs is cut out too.
* **Animal voices:** a talking lion or tiger rumbles, an elephant booms, a mouse or sparrow squeaks, a crow is hoarse, a cat or parrot is a little nasal. The words stay clear.

## Sound

* **Stereo:** each voice comes from where its speaker stands on screen (left, middle or right), and nature and crowd backgrounds are spread wide around you. Phones with one speaker simply play both sides together.
* **The sound of the place:** voices and effects get the echo of the place on screen — a cave rings for about two seconds, a palace hall about one and a half, a courtyard gives a short slap, and outdoors stays almost dry. The echo glides from place to place when the scene changes.
* **Dialogue polish, like a film's sound editor:** rumble below ~90 Hz is removed, every line is brought to the same speaking loudness (phone, natural, AI voices and your own recordings sit together), and a gentle compressor keeps every word clear. Music and backgrounds dip under speech.
* **Breaths:** a soft breath before long or emotional lines.

## Voices

* **Natural voices (default, needs internet, no key):** Microsoft's free neural voices sound like real people. Hindi uses Swara (female) and Madhur (male); English has Indian, British and children's voices. Every character gets its own pitch and speed. This is the free voice behind Edge's "Read aloud" and is not an official service for apps, so it could stop working. If it does, the app falls back to the phone's own voice and tells you.
* **Your voice samples** move the voice towards your sample's pitch and tone. This is not true voice cloning. For a fully real voice, use **Record lines in your own voice**.
* **AI voices (Gemini, optional)** are expressive, but the free daily limit is very small, so they are off by default.
* **ElevenLabs (optional, your own key): the most lifelike voices**, in Hindi and English (multilingual model). Add the key in ⚙ Settings → Free pictures & sounds. Each character gets the voice from your account that fits it best — gender, age and the voice the script describes ("deep", "sweet", "raspy"…) — and keeps it in every later film. Feelings make the voice steadier or more expressive. The free plan gives about 10,000 characters a month; when they run out, or without a key, the free natural voices are used. A character with your own voice sample keeps your sample.
* The first test video I made earlier used **espeak**, a robotic computer voice, because the build machine has no good voice. The phone never uses espeak.

## Keys in Settings

Each tool that can take a key has its **own card in ⚙ Settings**, with what it does, where to get the key, a field, **Save**, **Test** and **Clear**: Google Gemini, ElevenLabs, Freesound, Pixabay and Pexels. A card at the top lists what is **built in and needs no key**: Microsoft Edge natural voices, Pollinations (AI pictures and story reading), Openverse and Wikimedia Commons (free pictures and real sounds) and the phone's own voice.
No keys come inside the app: a key is a personal password tied to one account, and one built into an app (whose code is public) can be read by anyone, gets misused, and is switched off by the service. Each key is free to make and stays only on your phone.

## Free pictures and sounds (optional keys)

* Without any key, the director searches **Openverse** and **Wikimedia Commons** for free-licence pictures and real sound recordings. Each saved item keeps its source, licence and creator.
* For more and better results, add free keys in ⚙ Settings → **Free pictures & sounds**:
  * Freesound: freesound.org/apiv2/apply
  * Pixabay: pixabay.com/api/docs
  * Pexels: pexels.com/api
* **Keys stay only on your phone.** They are never built into the app or put on GitHub, because this repository is public and a key placed here could be misused by anyone.

## Free AI (optional)

The app works **without any key**. Story reading and AI pictures use free services, and free pictures and sounds can be searched on Openverse / Wikimedia; all of these need internet.
For better story reading, picture recognition and expressive AI voices, add a **free Google Gemini key** in ⚙ Settings (aistudio.google.com/apikey). The key stays only on your phone. Never send it to anyone or put it on GitHub.

## Honest limits

* **"Pixar level":** Pixar films are made from full 3D models with thousands of animation controls, hand-animated by teams and rendered on large computer farms. This app bends your pictures on a phone. The fine meshes, real-lip lip-sync, follow-through, light, depth, stereo sound and room echo bring it much closer to a cinematic film, but it is **not** Pixar quality and cannot be. The picture quality depends mostly on your pictures: 3D-style pictures give a 3D-looking film.
* **Speed with the finest meshes:** drawing always at the finest mesh takes longer. On the build computer a test film drew about 35 % slower than v9. v11's finer meshes add more work; how much slower v11 is was not measured. On a phone, a 10-minute 720p film may take roughly 25–50 minutes. This is an estimate; it has not been measured on a phone.
* **Close-ups** come in at most about 2.9× so pictures stay sharp; a close-up of a small child character shows the face and upper body rather than the face alone.
* **Voices:** the free natural voices (Microsoft neural) already sound like real people. ElevenLabs sounds closest to real actors but needs your key and has a monthly limit. The test films made on the build computer use a robotic computer voice (espeak) because it has no internet; the phone never uses it.
* **ElevenLabs was tested only against a mock server** built from its published API (voices list, text-to-speech, key header, quota error), not the live service.
* **Pixar level:** the film now follows the Technical Director protocol shot by shot, but it is still made from your flat pictures bent on a phone. It is not, and cannot be, Pixar quality (3D models, hand animation, render farms).
* **Face filling 65–75 % of the frame** is used only where the picture is sharp enough and the whole head (hair, turban) fits; otherwise the face is as large as allowed (on the sample pictures most close-ups show the head and shoulders with the face filling about 40–60 %).
* **Very fast moves:** where the story's timing leaves no room to slow a run, a few shots still move more than 15 % of the frame (11 of 277 shots in the sample story); the quality check lists them.
* **Vertical (9:16) films from landscape backgrounds:** your own landscape picture is shown with "cover" (filled, never stretched), so its left and right sides are outside a vertical frame; each shot is centred on the action. For a native vertical background, add a portrait picture of the place (or let the AI make missing places, which are now made in 9:16 directly).
* **Speed:** pixel-level meshes, the glow and the colour finish cost drawing time. On the build computer (one core, desktop Java) 40 s of film at 1280 × 720 took 254 s to draw (3.8 frames per second). How long a whole film takes on a phone has not been measured; expect it to take longer than v12.
* **Human QC waits for you:** with it on, the film is not made until you approve the shots (the notification says so). Turn it off in ⚙ Settings if you want the film made in one go.
* **The frame-by-frame check (v16) catches what a viewer at 0.25x would see as the whole picture boiling or shaking:** two frames in a row differing by more than 14 grey levels per cell on average, or more than half the frame changing at once. It cannot judge the fine stability of a small character; that comes from the meshes themselves (the same picture bent by one continuous mesh). On the build computer it costs 40–450 ms per shot (drawn characters cheap, photographed meshes dear): about 2 minutes for the 296-shot sample film. On a phone it has not been timed; expect a few minutes before "Mixing music and sounds".
* **Taking off a cap or turban** works when it differs in colour from the skin and hair (most turbans, caps and crowns). The bare scalp is painted in the forehead's skin colour; hair hidden under the cap can't be known, so the head is shown bald. A black cap on black hair may not be found, and then the character keeps it on.
* **Floor detection** compares the colours at the bottom of the place picture with the rest. On the 6 sample places it put the feet within 3 % of the hand-set positions; a picture whose floor looks like its walls may put the feet a little high or low.
* **Eye and mouth finding** was measured on the 10 sample characters only; on one (an open laughing mouth under a curled moustache) the mouth point is still half an eye-distance off. Check the points with 👄 when lips look wrong.
* **Mouth shapes** come from how bright the sound is, so they follow vowels roughly (wide / round / closed). They do not come from a phoneme-by-phoneme analysis.
* **v14, the Pixar-Lead protocol:** the story spine, acts and turning points are found from the words of the script (trouble words, fight / rescue words, celebration words); on an unusual script the "One day" or climax beat may land on a neighbouring scene — the Braintrust's notes are suggestions, the script itself is never changed. "Two lights only" is a model of the light (a key and a bounce), painted over flat pictures, not a 3D lighting calculation. **Animation on twos** holds each character's pose for 2–3 frames (the camera, the place and the lips stay smooth) — a stylistic choice, off by default. The thumbnail's "face 60 %" uses the picture's own sharpness limit, so on a small picture the face is smaller. AI pictures at the format's native size (1920×1080) depend on the free picture service accepting that size; when it does not, the app falls back to 1280 on the long side, still in the right shape. The five formats keep the picture whole: a 2.39:1 film from a 16:9 place picture shows its middle band.
* **In a boat:** the characters stand on the deck and move with it when the place says they are on the water; they do not row or climb in and out.
* **v15, free-form scripts and cues:** the reader resolves speakers by the names in the character list; a line spoken inside the narration needs a dash or a speech verb before the quote and a known character (or the last speaker) in the sentence; quoted sound words stay sounds. The cue engine reads words, so a sound it has never seen is made from its first letter (a "थड़" thuds, a "सर्र" hisses) — not always what you imagined. Drawn modern clothes are simple shapes (a hoodie, a suit); your own pictures or the AI's are always richer. The new painted places are two (rooftop, basement) plus a generic room and street; a script set in a hospital or a school gets the room.

* **3D:** the film looks 3D when its pictures are 3D-style: your uploads, or the studio's AI pictures, which are always requested in 3D animated style. The camera, depth, light and parallax add a 2.5D cinematic feel.
  * The characters are still flat pictures that move, turn and lip-sync. They are not rigged 3D models, so they don't walk around in true 3D like a Pixar film. That isn't possible with the phone engine.
  * Characters drawn by the studio offline are flat cartoons.
* **Picture characters' movement** is a bending of the flat picture. Small and medium movements look natural. A picture cannot raise its arm over its head or truly turn around (turning flips it). **Sitting** folds the legs and puts a throne, stool or rock in front, which reads as sitting, but a standing photo never becomes a real seated pose. Arms drawn touching the body move less cleanly, and the expressions are only as good as the eye and mouth points (check them with 👄).
* **Animal pictures** work best side-on, on a plain background. The head, tail and legs are found from the outline. When no tail is found, the tail does not move. A front-facing animal photo only nods and opens its jaw.
* **Weather and water are painted effects** that follow simple physics rules (gravity, wind, bounce, ripples). They are not a full fluid simulation. A river or sea in **your** place picture flows only where the studio is sure it found water; it is careful, so on many pictures it finds none, and then nothing in the picture moves wrongly. Studio-drawn places get painted waves.
* **Finding things in the library by itself** uses confident matches only (a name, matching words, or a clear look). A picture with no name and an unusual look may not be used; place it once in the studio, and it is used from then on.
* **"Training" the director** on Nolan, Pixar or Disney films or on videos is not something this app can do: there is no way to learn from videos on the phone or on the build machine. What the director knows instead is written in as rules: establishing shots, reaction shots, push-ins, low and high angles, match-cut dissolves between scenes, light, depth and mood music.
* **Recognising sounds offline** is a guess. On the 47 sounds built into the app, the kind (background, effect, music, voices) was right for 43; what it sounds like was close for about two thirds (fire, village, palace and cave sounds were confused). Those are the same sounds the rules were tuned on, so expect less on your own recordings. The words you give always count first. AI listening (Gemini key) was not tested against the live service.
* **Voice effects** were checked by measuring them (roughness, wobble, brightness), not by listening tests with people.
* **Recognising pictures offline** is a best guess. On the sample story's 10 character pictures, the right character was the first guess for 7 and in the top three for 8. The three guards wear identical uniforms and can't be told apart. Places were right for 3 of 8 offline. Check the suggestions; with internet, the AI vision model does this much better.
* **Voice matching** measures pitch reliably. Speed and tone are rougher readings: they were checked with computer voices at known speeds, not with many real recordings. If the story doesn't describe a voice, only age and gender are used.
* **Speed:** a 10-minute 720p film should take roughly 15–30 minutes on a phone with 8 GB of RAM, and longer at 1080p. This is an estimate; it has not been measured on a phone.
* **Hinglish spelling:** Hinglish is changed into Hindi letters by rules plus a word list, so unusual words or names can be pronounced a little wrong. **Read with AI** (online) writes a proper Hindi script and fixes this.
* **Online features were not tested live.** The AI, internet search, natural voices, Gemini, the AI reading of implied nature, the free-recording download and Freesound / Pixabay / Pexels can't be reached from the machine the app was built on. They were tested against mock servers built from the services' protocols, not the live services. For the same reason, the build machine could not download free pictures, sounds or voice samples to put inside the app; the phone fetches them itself when a story needs them.

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
  * `Nature` is the nature engine: it finds sky, water, waterfalls and plants in place pictures and draws weather, fire and light, water and projectiles as pure functions of time (so frames drawn in parallel agree). `Film.Weather` holds the weather timeline; `Director.weatherFrom` / `natureFrom` read it from the words and `ScriptAI.natureCues` from an AI reading.
  * `Rig` also handles animals (`buildAnimal`): head with jaw and ears, tail and legs found from the outline. Meshes are adaptive (`bodyMesh(…, screenPx)`, up to 48×96 / 96×48, face 48×48); the face mesh opens the jaw and shapes the lips from `Film.Line.shape` (`Mixer.shape`: brightness of the voice, 100 Hz).
  * `Mixer` makes a stereo soundtrack: panned dialogue (`panOf`), wide ambience (`LoopClip.wide`), `dialogue()` polish (high-pass, loudness), a dialogue compressor, `breath()`, and `Room` (Freeverb-style stereo room per place) on the voice/effects send. `VideoWriter` writes 2-channel AAC.
  * `Eleven` is the ElevenLabs client (voices list, text-to-speech, cast assignment by gender/age/voice words).
  * `SoundSense` classifies sounds offline; `SoundWords` links English, Hindi and Hinglish sound words; `Director` places the user's effects on matching actions.
  * `VoiceMatch` turns a character's description into the voice it should have and scores measured samples (`VoiceFx.features`: pitch, liveliness, syllable rate, brightness, roughness) against it.
  * `Director.scoreMusic` builds the in-scene score with swells, dips and stingers. `Renderer` draws the cinematic look: ground-anchored parallax, camera roll and drift, light, a blurred foreground, grain and letterbox.
  * `Edits` and `CommandParser` turn the plain-English change requests typed after the preview into edits (an LLM handles anything the rules miss).
  * `Bible` writes the production file. `Toon` turns photos into cartoons.
* `app/src/main/java/com/tarun/kahani/app` holds the Android side (UI in English):
  * `MainActivity`, `Picker`, `Library`, `AudioIO`, `Voices`
  * `AutoLibrary` (the director's own look through the library before every film, and adopting older stories' pictures), `FreeSounds` (real recordings for backgrounds)
  * `FilmJob` + `FilmService` (foreground service, wake lock, notification)
  * `VideoWriter` (MediaCodec), `FilesProvider`, `Prefs`
* **Build:** run `./build.sh`; it doesn't need Gradle.
  * Tools: `aapt2`, `dalvik-exchange`, `zipalign`, `apksigner` and `android-sdk-platform-23` from Ubuntu, plus JDK 17+.
  * Resources are linked against the Android 14 framework from Robolectric's `android-all` jar.
* **Tests:**
  * `tools/robotest` (Robolectric, real Android graphics): screens with an English-only check, pickers, the command box, Hinglish reading, pictures, voices and sounds from the library placed by description, a full film job in 9:16, frames drawn through `android.graphics.Canvas`, the library used without any button (pictures, voices, older stories adopted once), the protocols on three scripts, and the v16 lock sheets, plates and frame-by-frame check. Run with `gradle test`.
  * `tools/jvm`: `MakeFilm` (desktop MP4 with espeak; env `ASPECT`, `EDITS`, `SAMPLE=name=wav`, `AUDIO_ONLY`, `QC=0` to skip the frame-by-frame pre-check; a stills folder receives the lock sheets, the plates, the thumbnail and the poster; `SHOTS=file` writes the shot list with every check), `BibleDump`, `CmdTest`, `RigSheet` (picture characters in many poses and feelings, for checking by eye), `AnimalSheet` (animal pictures: tail, ears, jaw, legs, lying down).
