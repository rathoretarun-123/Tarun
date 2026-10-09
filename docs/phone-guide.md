# The Phone-Local AI 3D Animated Film Creator guide — trained in and hardcoded (v21)

The user gave two documents with "Train your director with these and hardcode them":

* `AI_Animated_Film_Maker_Directors_Manual.docx` — byte for byte the Director's Production Manual v2.0 already
  trained in at v20 (`docs/directors-manual.md`, `core/DirectorsManual.java`); nothing to add.
* `Phone_Local_AI_3D_Animated_Film_Creator_Guide.docx` (v1.0, 9 October 2026) — new. Bundled as given in
  `app/src/main/assets/phone_local_film_creator_guide.md`, shown in ⚙ Settings → 📜 after the manual;
  `core/PhoneGuide.java` holds its modes, routes, reference record, shot card, camera rules, lip-sync criteria,
  render stages, export presets, failure handling, acceptance checklist, the map from each section to the code,
  and the table of where it disagrees with the documents already in force. Nothing trained before is removed.

## Where the guide and the documents in force disagree (what the studio does)

| Topic | The guide | In force | The studio |
|---|---|---|---|
| "Pixar-level" | Appendix C: do not describe the app as Pixar-level or promise studio parity; define measurable targets | the user's ask (item 14): Pixar / Disney level | the guide's own boundary is adopted and said plainly: every rule of every document is applied and measured by the QC (gates, frame-by-frame meter, checklist); a phone bending pictures is not a render farm |
| Slow dolly / pan / orbit for emotional beats | §9.2 | the locked tripod (v12) | the tripod wins: a locked close-up and a reaction cut |
| Phoneme → viseme timing | §10 | audio-only voices | the mouth from the voice's envelope and sound at 100 Hz, shut in silence, drawn only where a mouth was found |
| Reconstruct only with enough views | §2.1, §7.1 | the figure model from one picture (v18) | real angles always win; a made view stands in only for a missing angle |
| Separate dialogue / music / effects tracks | §10.1 | one mix | the mix ducks music under lines; separate tracks are not exported |
| Compose / Flutter / ONNX / LiteRT | §3 | plain Android Java, no Gradle, no native libraries, keyless | kept; every "AI assist" is deterministic studio code |

## The fourteen items, what was done

| # | Ask | Done | Where |
|---|---|---|---|
| 1 | Missing picture while making the film: upload from library / photos / camera, up to 10 angles, at that page | **📷 … — no picture yet: add angles** buttons on the progress screen and the check screen for every character, place and thing without a picture (used from the next make); phone (up to 10 at once), camera, or the tarunkahani library | `MainActivity.missingCard`, `anglesFor`, `saveAngles` |
| 1, 2 | One picture holding several angles → separate pictures of the same thing | `Angles.split`: the background removed, the figures grouped (parts closer than 2 % of the width belong together), noise dropped, pieces left to right; verified on a two-figure sheet of the sample cast | `core/Angles.java` |
| 2 | Studio: up to 10 angles per character / place / object, each saved in the library | **📷 10 angles** on every character card, **📷 Angles** on every place card, a new **Things** section with an angles button per object the story names; every picture into the library as the same thing (person / view / place / object) | `MainActivity` Studio cards, `objectsCard` |
| 2 | The angle of each picture | `Angles.guess`: real eyes are dark spots on skin — a "face" with bright eye spots or as wide as the body is a back; eyes off-centre = three-quarter; one eye / narrow = side; else front; same-angle duplicates step on to the next missing angle. On the sample's 14 fronts and backs: 13 right (the green-skinned witch's front reads as a back; the Studio card lets you fix a view) | `core/Angles.java`, `tools/jvm/AnglesCheck` |
| 3 | Objects from the library or the user, 10 angles | the Things section; the first picture becomes the insert of the thing itself; every angle is kept in the library as an object | `objectsCard`, `saveAngles` (obj) |
| 4 | Auto-selection of pictures, sounds and voices "always wrong" | the guide's rule §1.1 / §14: never silently guess between two plausible matches — a picture that fits two targets almost alike, or a target two pictures fit almost alike, is not placed and is named in the director's note ("choose in the Studio"); the sureness threshold raised to 0.66 (a name match still wins). Voices already need 0.8 and a named voice wins. | `AutoLibrary.pictures` |
| 5 | More scenes for a smoother story | an **establishing bridge** (the new place alone, a slow push, 2.6 s) wherever the place changes between scenes without a title card — the manual's "orient before fast cutting"; counted in the QC; the missing-picture buttons ask for the pictures | `Director.direct`, `Renderer.drawCard` |
| 6 | 3D maker not using the uploaded pictures | dolls are **reference-conditioned**: the library picture that fits the description best (traits, worn colours, the name counting double, fit ≥ 45 %) lends its two main clothing colours and its hair colour; the proposal names the reference. Real angles replace made views; the style cue (light, saturation, skins) stays | `Studio3DArt.referenceLook` |
| 7 | Still shaky; lip-sync; new lips | the speaking head's accent halved, the frightened tremor cut to a third; no painted mouth where no mouth was found (`faceKnown`); the lower "tongue" blob that read as a new lip removed; the mouth is the real lips parted by the face mesh | `Renderer.rigState/bodyFor/drawSprite` |
| 9 | Time left wrong | the time left is now the steps before the video from their own progress plus the video from **this phone's measured speed** (ms per frame per megapixel, saved after every render); during the video, the measured rate; before the first film ever made on a phone it says nothing rather than a wrong number | `FilmJob.updateEta/estimatedRenderSeconds` |
| 10 | Resizing by output (Facebook etc.) | the five formats keep their own framing (never a crop of a 16:9 master, the guide §11.2); the chooser names Facebook video (16:9), Facebook Stories (9:16), Facebook post (1:1) and feed (4:5) | `askBeforeMaking` |
| 11 | Background-only pictures; characters placed by the script; 10 angles | a place's first picture is its plate (feet on its floor line, characters placed by the script — as before), its **second picture its reverse angle**, drawn behind every reverse shot (the listener's face, over-the-shoulder, the POV of the other character) — manifest `scene|<n>r|file` | `Film.Cam.reverse`, `Film.Seg.backdropReverse`, `Renderer.bd` |
| 12 | Upload to the library any time from the home screen | **➕ Add pictures, voices or sounds to the library** on the home screen (phone pictures, camera, record a voice, sounds, voice samples) | `MainActivity.addToLibrary` |
| 13 | Faster, use the RAM | drawing threads sized from the cores and the free memory (heap room and half the phone's free memory, each worker holding its frame bitmap and two finished frames), up to 6 instead of 4; the thread count and memory are written in the QC | `FilmJob.renderVideo/memoryBudget` |
| 14 | Back button while making; return from the home screen | back from the progress screen goes home; the film goes on in the service; the home screen's **🎬 Film being made — open** card shows the stage and the time left | `onBackPressed`, `showHome` |
| 14b | Pixar / Disney level | see the precedence table: measured craft, no studio-parity claim | `PhoneGuide.PRECEDENCE` |
| — | Scenes "diffusing" / overlapping | the plain scene-to-scene dissolve (both scenes drawn over each other for 0.9 s) replaced by a **quick dip through black** (0.6 s): the last scene goes down, the new one comes up, never two at once; the dips to black / white for time passing and magic stay | `Renderer.render` |

## Verified

* Desktop: the sample (16:9, 60 s) with a reverse angle added for scene 1 — 948 shots planned, 8 bridges, 51 reaction cuts
  marked reverse, 0 boiling / 0 shaking in the frame-by-frame meter; frames checked by eye: the bridge (the practice
  ground alone), the dip (the garden going to black), the reverse shot (the listener over the speaker's shoulder).
* `AnglesCheck` on the sample cast: 13 of 14 fronts and backs read right; a two-figure sheet split into 2 pieces.
* Robolectric: `phoneGuideAnglesSplitBridgeAndReverse` (the guide hardcoded, the split, the angle guess on real
  fronts and backs, the bridge, the reverse backdrop and reverse cams, the dip frame, the reference-conditioned
  look, the home-screen button) plus the whole suite — see the README.

## v22 follow-up (the user's notes on the v21 film)

| Note | Done |
|---|---|
| Meshing finer; lip-sync creates additional lips | Meshes are at one cell per screen pixel already (`Rig.CELL_PX = 1`, up to 512 cells across a face). The "additional lips" were the painted opening: only the dark inside between the parted real lips is drawn now, once they are clearly apart; no teeth bar, no fallback mouth. Checked on a laughing close-up of the sample. |
| Movement still shaky | Pictures land on whole screen pixels in locked shots (`Renderer.pixelStep`); idle head sway and weight shift halved. |
| Scene diffusion wrong | The close-up's background is softened (half-size layer, at most two thirds in, from a real close-up on), not melted. |
| Monkey has the pagdi, Ratanlal still wears one | The bare head keeps nothing of the turban beside it (`Art.takeOffHeadwear`: a tail, a feather, a band's end); the sample's own insert picture of the snatch — artwork that shows the turban still on while the monkey holds it — is left out at that moment. Checked on the frame at 2:17.7. |
| More shots | A reaction shot from a moderate feeling (0.45); a two-shot held over two lines only in calm talk. |
| Director defines scenes and asks for pictures; upload on the film-making page | The plan card above *Make film* (and on the progress and check screens): every character, place and thing without a picture, the 10-angle upload and the library search; each new place gets its establishing bridge. |
| Faces cut in close-ups | The head with its hair takes at most 74 % of the frame with a margin; lip-sync at CU / MCU, never XCU. Checked on the frame at 1:08.5 (hair was cut at v21, whole at v22). |

## v23 follow-up

| Note | Done |
|---|---|
| Background blurred completely in close-ups | A faint softening only (`Renderer`: three-quarter-size layer, at most 30 % in, from a tight close-up on). |
| Upload of what is not found at the make-film screen | The make-film dialog's **📷 Pictures first** button lists everything without a picture and opens the 10-angle upload. |
| Background-only pictures, characters fitted by the director | As before (plates with their floor line); now said on every place card. |
| More shots / smooth movement | 30 frames per second (Settings → Smooth motion, on by default); the v22 reaction and hold rules. |
| YouTube in the format selection | "▶ YouTube video" (16:9) and "YouTube Shorts" (9:16) named first. |
| Camera focus: half the face cut | The over-the-shoulder shoulder takes at most 28 % of the frame width (it hid half the listener in 9:16); checked on a contact sheet of 12 close-ups. |
| More RAM, faster | Drawing threads on every core the free memory allows (up to 8), three frames in flight each. |
| 3D picture maker making bad pictures | `Studio3DArt.referencePicture`: the library picture that fits the description best is cut out and recoloured to the description's colours (the two most worn hues turned, shading kept) and proposed first, named after its source; the doll only when none fits or it is rejected. |

## v24 follow-up (the notes repeated)

| Note | Done |
|---|---|
| Blur in close-ups | Removed entirely: the place behind a close-up is drawn as it is. |
| More shots, smooth movement | The director cuts at 3.2 s (`TechnicalDirector.CUT_SECONDS`; the 4-s cap stays the limit); establishing shots held 3.1 s. Sample: 948 → 1038 shots, none over 3.2 s, 0 boiling / 0 shaking; 30 fps from v23. |
| More RAM | Four frames in flight per drawing thread, every core the free memory allows. |
| 3D picture maker | `Studio3DArt.referencePlace`: the library place picture that fits the scene's words best, graded for night or evening, proposed before any painted set and named after its source; a rejected one brings the painted set. Characters from the user's own pictures since v23. |
| Upload at the make-film screen; background-only pictures | v23: the dialog's **📷 Pictures first**; plates with the floor line, said on every place card. |

## v25 follow-up ("not able to add pictures of objects / in the popup / anywhere; the selection is still bad")

| Note | Cause found | Done |
|---|---|---|
| Not able to add pictures (popup, Studio, objects) | The upload's target lived only in the screen's memory; Android drops the screen behind the photo picker (a film being drawn takes most of the RAM), so the chosen pictures arrived with no target and were silently dropped. A picture decoded while the renderer held the heap could fail outright. | `anglesTarget` / `anglesFrom` / the story kept in the saved state **and** the preferences and restored in `onCreate`; the result falls back to them; a lost target says so instead of staying silent. `decodeBytes` retries at smaller sizes; the renderer's budget leaves max(96 MB, heap/5) to the screen. Android 13+'s photo picker (`ACTION_PICK_IMAGES`, up to 10) with the document chooser and the gallery as fallbacks. |
| Objects | Only the two word lists' things were listed; a story's own thing (पगड़ी…) could not be added. A photo with a real background was sometimes "split" into junk pieces. | "➕ A thing of the story (name it)" on the things card, the story screen and the popup; the things card lists both word lists and the user's things. A split counts only when every piece is figure-sized; camera photos are never split. |
| After adding | The user was sent to the Studio | Back to where the pictures were asked for; the make-film popup reopens. |
| Selection still bad | Pictures from Home carried camera file names (IMG_…), so they were matched by look alone at 0.66; nothing was learnt from the user's corrections. | Naming step after adding (person / place / thing); `is:`/`not:` marks learnt from the user's choices and ✖ in the Studio (`AutoLibrary.labelKey`), by the label's words so they hold in every story; a camera-named picture with no fitting words needs 0.82 on its look, else the user is asked (the guide §1.1: never silently guess). The character card says when the director chose the picture itself. |

## v26 follow-up (the user's 60 sheets)

| Note | Done |
|---|---|
| "Splitting of pictures uploaded in one image with 10 angles should be perfect" | Rebuilt: border background → flood-fill mask (white clothes stay in the figure) → grid lines cleared → recursive XY cut on empty rows/columns, soft gaps (a braid tip crossing the gap), and a cut at the thinnest place for touching figures only when the part is too long for one figure (side by side: over 1.3× as wide as tall; stacked: over 3.4× as tall as wide) and both halves are figure-tall → each piece cut out on its own with its holes. 60 real sheets: every figure found, none cut — 46 sheets of 10, 10 of 11, 4 of 12 (six on a row), 618 figures (firmly touching figures told apart by thinning the mask until it falls into two figure-sized groups alike in bulk and beside each other). |
| 10 images × 10 angles, activities, emotions → 100 per thing | `saveAngles`: up to 10 files, each up to 10 figure-sized pieces (100), best real piece per slot, the rest in the library as pictures of the same thing (`ofName`); places: wide, reverse, the rest in the library. |
| Drawn angles are very bad | Never drawn once real angles exist (`Studio3DArt.realAngles`, `makeAllViews`/`makeViews` skip; the Studio button is gone). |
| Objects uploadable everywhere | Studio things card, story screen, make-film popup, progress screen ("Pictures in this film"), each with "name it". |
| Film-making screen shows what was not uploaded or asked | "Pictures in this film" card on the progress screen: every character, place, thing and added scene with its status and an add/replace button. |
| Smaller shots, finer meshing | Cuts at 2.6 s (`TechnicalDirector.CUT_SECONDS`), establishing 2.5 s; meshes are already one cell per pixel (v17); pictures read at a size following the output. |
| Director adds scenes and asks for pictures | `ScenePlan` extras (journey into every new place) with key `<n>j`, asked for on three screens, used as the bridge by the `Director`. |
| Slide the mouth/eye page to pick the front | ◀ ▶ and swipe through every picture of the character; "Save" makes it the front. |
| RAM > 8 GB | Honest limit: the Java heap Android grants (largeHeap) and the cores bound the renderer, not the phone's RAM; stated in the README. |
| Natural effects / physics "perfect", "world class" | No claim made; unchanged this version. |
| GitHub sources | Searched (Animated Drawings, Pose Animator, Stretchy Studio, Inochi2D, DragonBones); none runs on the phone as is; recorded in the README. |

## v27 follow-up ("use the different emotions and activity pics uploaded by the user, make it absolutely less shaky, check the director uses the correct split picture for a shot")

| Note | Done |
|---|---|
| Use the uploaded emotion and activity pictures before the app-made ones | Every split figure becomes a pose picture (`pose\|key\|file\|angle\|pose\|emotion\|hRatio\|face points` in cast.txt) read by `PoseSense`; the director (`Casting.cast`) chooses the fitting picture for every character in every shot — angle (back behind the shoulder, side while walking, three-quarter in a two-shot), pose (sitting, lying, running, fighting, pointing, waving…), feeling (the spoken line's, else the moment's) — the rigged front only when no picture fits better. No drawn view is made where real pictures exist (v26). |
| Absolutely less shaky | A chosen picture is drawn as it is for the whole shot (`Renderer.chosenPicture`): no sway, walk cycle, bending, head tilt or nod; pixel-snapped; only breathing and lip movement. The idle weight shift of the rigged front halved. One picture per shot, never a change inside a shot. |
| Director uses the correct split picture for a particular shot | Tested: `directorUsesTheRightPictureForEachShot` (walking entrance → sideways walking picture; crying line → crying picture; sit → sitting picture; behind the shoulder → back picture; named in the shot list under PICTURES USED). The reading of the sheets: `poseSenseReadsTheUsersSheets` (five real sheets). |
| "Make the movements more smooth, as you now have 100 pictures doing different activities" (v26) | v28: the step cycle — the character's walking/running/standing pictures of the shot's angle shown one per step (`Casting.stepCycle`, `Renderer.chosenPicture`), with the step's rise and lean; the shot list names them. |
| "The app should be able to increase or decrease resolution ... of each picture" (v26) | v28: sheets read at up to 2600 px (heap permitting) so the pieces are sharp in close-ups; pictures are read at a size following the output (v26). |
| "Splitting of pictures uploaded in one image with 10 angles should be perfect, it's not happening now" (repeated after v28) | v29: the cause was the path, not the splitter — only the Pictures button split; the picker, the library placement, the director's placement, the phone/camera pick and the mouth/eye page saved the sheet whole. All of them now go through `SheetSaver` (sheet decided by content). |
| Reliability of the reading | Angles and poses: good on the 60 sheets (silhouette measures). Feelings: conservative — a doubtful face stays neutral (a saturated mouth box is hair or clothing); the review dialog after every upload ("What each picture shows") lets the user correct every reading, and the library keeps the corrected reading for the next story. |
