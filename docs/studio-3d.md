# Studio 3D — the studio's own picture maker (v17)

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

## Cost and limits

* On the build computer a character takes 0.1–0.3 s, a place 0.8–1.5 s (1280x720, 2x supersampled). A phone is
  a few times slower.
* The dolls are stylized rounded figures, not the photographic AI renders: clean, consistent, instantly made,
  with exact face points. For a photographic look the free AI picture service (internet) still makes the picture
  and Studio 3D fills what the service cannot.
* A single front picture (yours or an AI's) cannot be turned round by the app; only its own 3D dolls can.
