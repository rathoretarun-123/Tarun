# Studio 3D — the studio's own picture maker (v17, v18)

When a character or a place has no picture, the director can now build one in three dimensions on the phone,
without any service or key: `core/Studio3D.java` (the renderer), `core/Doll3D.java` (characters), `core/Set3D.java`
(places), `app/Studio3DArt.java` (saving into the story), the 🧊 buttons in the Studio.

## What it renders

* A software renderer in plain Java (no OpenGL, so the same code runs in the tests and on the phone): rounded
  solids (ellipsoids, tapered capsules, boxes, rings, discs) rasterized with a depth buffer and perspective-correct
  interpolation; 2x supersampling for clean edges.
* The two-light model of the Pixar-Lead protocol: one key light with a soft shadow (a 1024² shadow map with
  filtered edges and a slope-scaled bias, focused on the stage of a place) and one bounce from the ground; a rim
  light on the edges; sky and ground ambient; ambient occlusion in the creases; atmospheric fog for far things;
  a gentle depth of field for places; a filmic shoulder on the brights.
* Materials as the handbook's chapter 7 asks: skin with controlled specular and subsurface warmth, hair with
  coherent highlights, cloth rough and soft, silk, metal with tinted reflections, stone matte, leaf, wood, glass
  eyes with a catch-light, and glowing things (lamps, screens, the eyes of a monster).

## Characters (`Doll3D`)

Built from the same `Look` the drawn puppet uses, with the puppet's proportions (so a 3D picture and a drawn one
agree on height, build and costume): people, children, witches, monsters and monkeys standing front-on in an open
A-pose; animals and birds side-on, as the animal rig expects; robots of metal with screen eyes and an antenna.
Costumes (lehenga, saree with its pallu, kurta, achkan with a flaring hem and a sash, uniform, armour, cloak,
hoodie with a pocket, t-shirt and shorts, suit and tie, coat, jeans), hair (braid, pigtails, bun, ponytail, long,
curly), headwear (turban with a kalgi, crown, witch hat, hood, pallu, horns), glasses, necklaces, earrings, bangles,
anklets, mustaches, beards, bindi, tilak, scars, fangs, glowing eyes, props (sword in its scabbard, spear, axe,
mace, wand, katar, shield, satchel, chains, phone, controller, laptop) and the species of animals and birds.

The picture comes with its eye and mouth points, so the rig knows the face exactly (they are written into the
story's manifest like a mouth the user set by hand). A character can be turned: `Doll3D.make(look, size, seed,
angle, emotion)` gives the front (0°), three-quarter (45°), side (90°) and back (180°) views the handbook's master
sheet asks for, with neutral, happy, sad, angry, surprised or scared expressions (brows, mouth, eyelids).
`Doll3D.masterSheet` puts the four views on one plate (📐 in the Studio).

## Places (`Set3D`)

One builder per kind of place the painted sets know — garden with the palace, courtyard, gate, cave inside and at
its mouth, forest, celebration (string lights and diyas), hall with its throne and chandeliers, village, city
rooftop with planters of solar flowers and a skyline of lit towers, basement with arcade machines and a tubelight,
a room with a window, shelves, a table and a candle, a street with buildings, lamp posts and an auto-rickshaw —
at morning, day, evening or night (sky, key light, fog and practicals follow the hour). The plate's floor line
(where the characters stand) is known exactly and written into the manifest.

## Where it is used

* `FilmJob`: after the AI pictures (if any), whatever still has no picture is built in 3D (⚙ Settings → "Studio 3D",
  on by default; also a checkbox in the dialog before making).
* The Studio: 🧊 3D picture and 📐 Front · side · back on every character, 🧊 3D place on every scene, and
  "🧊 Build all missing pictures in 3D now".
* The lock sheets and location plates then show the 3D pictures; the film animates them like any picture.

## v18: from the user's own pictures (`Figure3D`), proposals, views, the library

* **The figure from a picture.** `Figure3D.build` reads the cut-out row by row: the opaque runs of each row are the head,
  the body, an arm beside the body, each leg; each run becomes a ring of an elliptical solid (head round, body flatter,
  arms and legs round, a skirt in between), with a round core and flat flaps for hair, ribbons and tails beside it; the
  rings of neighbouring rows are joined, the skull above the eyes is a sphere a quarter wider than the face (the face is
  the span of skin at the eye rows). The picture is the texture: the front half shows it, the back half shows it mirrored
  (the costume goes round), the back and the sides of the head show the band of hair above the hairline repeated
  (never the face). The soft edge of the cut-out is un-mixed from the old background first. The material keeps 68 % of
  the picture's own light and shade. Views: three-quarter (−45°), side in mid-stride (−90°, the legs apart in depth,
  the arms opposite), back (180°), facing right like the front picture; the face points are carried through the
  three-quarter view. A side-on animal picture gets no views (the film mirrors it).
* **Style cue.** `StyleCue` reads the light side, warmth, saturation, contrast and skin tones of the pictures the story
  already has; dolls and places are lit from that side and graded toward those values; the views are graded too.
* **Proposals.** `Studio3DArt` writes `propose|char|…`, `propose|scene|…`, `propose|view|…` lines with the scene maker
  rubric's score and verdict; the character and scene cards and the progress screen show them with ✔ Use / ✖ Reject;
  `accept` turns them into `char|`, `scene|` and `view|` lines and adds them to the app's own library; `reject`
  deletes the files and remembers the rejection (`rejected3d.<kind>.<key>`), so the film job never proposes them again
  by itself. The job waits (`proposalsWaiting`) until every proposal is decided.
* **Views in the film.** `Art` loads `view|name|angle|file|face points` lines into `Sprite.views` (each with its own
  rig); the renderer takes the side view while walking, the three-quarter view in a two-shot when turned to the other
  character, and the back view as the over-the-shoulder foreground that the director plans for reaction shots once
  the back view exists; every shot's "PICTURES USED" line in the quality check says which.
* **One picture per shot.** `FilmJob.shotPictures` saves the first frame of every shot as `shots/<shot id>.jpg`.
* **The user's own views.** 📷 Back / 📷 Side on the character card save a `view|` line directly (no proposal: the
  user chose it). `makeViews` then fills only the angles that are missing (`viewFiles`), `makeAllViews` runs for a
  character until `allViews` is true, and `makeCharacter` never proposes a doll view at an angle the user has given.
  The sample story ships eleven real back views (`char_<name>_back.jpg`, `view|<name>|180|…` lines in `sample/cast.txt`).
* **Free models from GitHub (v19).** `core/FreeModels.java` is the hardcoded catalogue: KayKit's Adventurers
  (CC0: Knight, Mage, Rogue, Rogue_Hooded, Barbarian) and the Khronos glTF samples (CC BY 4.0: Fox, RiggedFigure),
  each with the words of a description that fit it (Hindi and English), the character kinds it may stand in for, the
  props it carries and its licence and credit line. `Studio3DArt.freeModel` fetches the GLB (raw.githubusercontent.com,
  no key), `Glb.load` poses it from its idle animation and keeps only the props the description names
  (`FreeModels.propsFor`), the style cue grades it, and the front and three views become proposals; the GLB stays
  with the story (`model_<hash>.glb`), the credit goes into the production file ("FREE 3D MODELS AND SERVICES USED").
  A rejected model sets `rejected3d.model.<key>` so the studio's own doll is proposed next time.
* **The glTF reader (v19).** `core/Glb.java` reads binary and JSON glTF 2.0 (a `Fetcher` brings the .bin and
  pictures named beside a .gltf; data URIs are decoded), the node hierarchy with TRS or matrices, skins (joints,
  inverse bind matrices, JOINTS_0 / WEIGHTS_0) and animations: the first keyframe of the first animation named in
  `Options.pose` ("Idle", "Unarmed_Idle"…) poses the model, so a free character stands naturally instead of in a
  T-pose; meshes under a hand slot (KayKit's `handslot.l/r`, or nodes named weapon/prop/socket/attach) are dropped
  unless `Options.props` names them or `allProps` is set. Verified on the desktop with the KayKit Knight and Mage
  (41 joints, "Idle") and the Khronos Fox and CesiumMan (`tools/jvm GlbSheet`).
* **Free image-to-3D demos (v19).** `ImageTo3D.freeSpaces` tries the free demos of TripoSR, InstantMesh and
  Hunyuan3D-2 on Hugging Face Spaces in turn through Gradio's HTTP API (`/gradio_api/upload`, `/gradio_api/call/<step>`,
  the event stream's `complete` event, the GLB's `url`); the steps are each project's own demo app's functions
  (`preprocess` → `generate`; `preprocess` → `generate_mvs` → `make3d`; `shape_generation`), the inputs are filled from
  the demo's `/gradio_api/info` (the picture to the first image input, the description to a caption box, the demo's
  defaults for the rest). Five minutes at most per demo, a failed demo is not asked again in the film
  (`forgetFailures` at the film's start), then the figure model. Not run against the live demos from the build
  machine (its network does not reach hf.space); the request building and the reply reading are unit-tested.
* **The description on the back (v19).** `Figure3D.details` adds what the front picture cannot show, from the
  description's `Look`, in the picture's own colours (`Model.hairColor`, `Model.bodyColor`): a braid or a ponytail
  down the back with its ribbon, long hair over the shoulder blades, a monkey's or an animal's tail, a fairy's wings —
  unless the front picture already shows the hair hanging (a braid over the shoulder is on the back by the mirror;
  `frontShowsLongHair`).
* **Views in the library.** A back or side picture is a library item of kind `view` (built-in ones carry
  `meta of=<the front picture's id>`, uploaded ones `meta view=<angle>;ofName=<character>`); `AutoLibrary.pictures`
  never offers a view as a front picture, and `AutoLibrary.views` gives a character the views kept with the library
  picture it received (or named for it) before every film.
* **An image-to-3D service.** With a Meshy key, `ImageTo3D.meshy` sends the cut-out and `Glb.load` reads the model;
  `Glb.render` draws the views through the same camera as the figure. Built from the public API; not run with a live key.
* **The library.** Every accepted picture goes into the app's own library "tarunkahani" (the app's private folder with a
  private backup in Downloads/tarunkahani) — never into the camera or photos library.

## Cost and limits

* On the build computer a character takes 0.1–0.3 s, a place 0.8–1.5 s (1280x720, 2x supersampled). A phone is
  a few times slower.
* The dolls are stylized rounded figures, not the photographic AI renders: clean, consistent, instantly made,
  with exact face points. For a photographic look the free AI picture service (internet) still makes the picture
  and Studio 3D fills what the service cannot.
* A single front picture (yours or an AI's) cannot be turned round by the app; only its own 3D dolls can.
