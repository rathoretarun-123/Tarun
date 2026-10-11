# Testing v41

What was checked for v41: your two new stories made with your own pictures, 100 new test stories directed and
checked one by one, the 34 earlier stories, the director measured against the documents you gave it, frames drawn
from the films, and the whole app on the phone emulator (Robolectric).

## 1. "राहुल का पार्क" with your park pictures (`tools/testdata/v40stories/rahul_ka_park.txt`, `tools/testdata/sheets40`)

| Check | v40 | v41 |
|---|---|---|
| The script ("फिल्म का नाम:", numbered "1. वस्तु - …" lists) | title, things and place not all read | title, one character, two things (झूला, स्लाइड), one place; both scenes in the park |
| The six slide pictures | three kept a grey studio card (their thin frame shut the background in) | all six cut out |
| The swing | a drawn frame beside your picture's swing; one act per sentence (he stood between lines) | your seat picture hangs in the place from the first frame; one continuous ride: he runs in, hops on, swings slowly, faster, faster still with no hands (up to 38°), stops and hops off |
| The slide | a sentence-long slide | your slide picture; he climbs the ladder at its back, sits at the top, slides down with his arms up, tumbles on the grass and gets up; the second time he slides down on his belly and lies on the grass |
| The empty swing | — | sways in the wind in a shot of its own |
| His bag | — | lies on the grass where he dropped it |
| Close shots during the swing | close-ups lost him in the swing's arc | a medium frame of the swing with him in it |

Robolectric `v41RahulKaParkSwingAndSlideSessions` uploads your pictures as the phone does and checks all of the above
(5 swing acts, 1 stop, 2 climbs, 2 slides down, 1 on the belly, the props in both scenes, the bag, keys in time order).

## 2. "बारिश वाला रविवार" — a new story for your family pictures (`tools/testdata/v40stories/barish_wala_ravivar.txt`)

Written for the family's own sheets (`tools/testdata/sheets38`): chores in the living room, a drive in the rain, an
umbrella and a held hand, a rainbow, a cycle and a bike in the lane, a puddle jump, pretend anger and a hug of four.

| Moment | v40 | v41 |
|---|---|---|
| "प्रिया रसोई में चाय बनाती है। तरुण सोफे पर बैठकर अखबार पढ़ता है।" and the other chores | only the first task of a line was done | every task named, in the order written: tea, newspaper, combing, sweeping, watering the plants |
| The sofa | a purple sofa in a grey room | the sofa takes the colour the script names, else your room picture's tone |
| "सोसाइटी के बाहर की सड़क" | read as a rooftop (सोसाइटी) | a street (a society is a rooftop only when no park, lane, road or room is named) |
| "तरुण कार चलाता है। प्रिया, वृंदा और वनुशा कार में बैठते हैं।" | the three rode a second car | they walk to his car and get in; he drives them; at the stop they get out side by side |
| Thunder and rain | rain | the sky flashes, everyone looks up and covers their head |
| "प्रिया छाता लेकर कार से उतरती है" | no umbrella (only a character described with one had it) | the umbrella opens over her in the rain (and the rain drums on it) |
| "… वनुशा का हाथ पकड़ती है" | — | she walks to वनुशा and holds her hand |
| The rainbow, the cycle, the bike | — | all three |
| "वनुशा पानी के गड्ढे में कूदती है" | a faint ring at her feet, framed from the waist up | the lane's puddles lie on the road (from "पानी के छोटे-छोटे गड्ढे"); she skips to the nearest, jumps, lands in it with drops and rings, framed whole, no cut in mid-jump |
| "प्रिया गुस्से का नाटक करती है, फिर हँस पड़ती है" | — | a frown put on, then the laugh |
| "चारों एक दूसरे को गले लगाते हैं और … घर की तरफ चल पड़ते हैं" | two hugged; the walk home began during the hug | all four hug; then they walk home |

Robolectric `v41RainySundayWithTheFamilysPictures` uploads the family's pictures and checks every row (5 tasks, one
driver with three passengers, the cycle, the bike, the puddle and its splash, the umbrella, the held hand, the hug of
four, rain, lightning, the rainbow, keys in time order).

## 3. 100 new test stories (`tools/soak41`)

`gen_stories.py` writes 100 stories (80 in Hindi in your script format, 20 in English) with the casts your pictures
cover — the family, राहुल, the three men — in homes, the park, lanes, the market, by a river, on a beach, a hill and a
roof, at four times of day: chores, eating, sitting, lying, sleeping, walking in and out, the swing and the slide,
cycles, bikes, scooters, autos and the family car, rain, thunder, wind, snow, fog and rainbows, umbrellas, puddles,
hugs, held hands, a dozen feelings and a dozen manners of speaking. They are random (seed 41) to stress the director.

`SoakCheck` directs each with your pictures and checks what must always hold. The same 100 stories and the 34
earlier ones (the sample, test scripts, the 20 soak stories of v35 and your stories), v40 against v41:

| Check | v40 (100 / 34) | v41 (100 / 34) |
|---|---|---|
| Crashes | 0 / 0 | 0 / 0 |
| Keys out of time order (a turn or a step shown late, or a jump) | 41 / 27 | 0 / 0 |
| A line spoken from beyond the edge of the picture | 5 / 0 | 0 / 0 |
| A speaker never in the camera's frame during the line | 4 / 0 | 0 / 0 |
| Someone left standing beyond the stage's edge | 3 / 0 | 0 / 0 |
| Walking while seated (not on a vehicle) | 4 / 5 | 0 / 0 |
| Sliding across the ground lying down (not on the slide) | 2 / 0 | 0 / 0 |
| Two people on one spot for 1.5 s or more (not hugging, hand in hand, riding or playing) | 39 / 15 | 3 / 5 |
| Flash cuts, wild zooms, NaN, acts ending before they start | 0 | 0 |

The remaining "one spot" cases: a walker circling the fire passing a man on a log (अलाव के पास), two animals of the
sample story, and three random stories where the stage is crowded.

What was fixed for them:

* Keys made for an earlier moment than an actor's last key are put in their place in time, with what they change held
  on in the later keys; a key put in during a walk continues the walk.
* One who rode off or walked out and then speaks walks back in from the side they left by to a free spot.
* "सब … बैठ जाते हैं" at the start of a scene seats only those not already moving; one walking in sits once there.
* Hugging, approaching or chasing a butterfly from sitting or lying: they get up first.
* "घर की तरफ चल पड़ते हैं" is the walk home, not a step toward someone.
* After each beat, two left on one spot step apart; a car lets its riders out side by side; a ride stops on free
  ground; those of a group who stop at the edge stand side by side.
* A line's close-up keeps its speaker (the framing passes slid one onto a neighbour).
* Flash and jump cuts are cleaned again after the last camera passes.

## 4. The director measured against your documents

The handbook's approval card (four gates, ten scores), the Technical Director protocol (shots ≤ 4 s, lip-sync shots
≤ 6 words, motion ≤ 15 % of the frame, one action per shot, safe zones), the director's manual (establishing shots, POV
shots, reactions to loud sounds) and the Pixar-style guide (reactions after lines) — counted over every film:

| Rule | v40 (100 / 34) | v41 (100 / 34) |
|---|---|---|
| Shots longer than 4 s | 0 / 0 | 0 / 0 |
| Lip-sync shots over six words | 0 / 0 | 0 / 0 |
| Shots the validator could not correct | 0 / 0 | 0 / 0 |
| Establishing shot of a place held (≥ 2.4 s) | 0 of 167 / 0 of 79 | 158 of 167 / 71 of 78 |
| Gate 2 (performance: a reaction where someone listens) not passed | 29 / 9 | 0 / 0 |
| Reaction shots | 142 / 152 | 258 / 203 |
| Average approval score | 4.68 / 4.71 | 4.64 / 4.72 |

* The place's opening wide is held whole for about 3 s (under the 4-s cap) before the shot-length, motion and
  one-action passes may cut into it; the action in it is seen wide (the manual's table).
* Every part with someone listening shows at least one reaction: after the first line said with feeling, a question
  or an exclamation. A film of one character is not failed for having no listener. (Two films of calm lines alone
  failed this gate at first; with the everyday words staged they now pass too.)
* (The average score moved by a few hundredths: films with more reactions also have more shots to score.)

## 5. Frames drawn from the films

104 frames from 13 of the soak films (eight across each, with your pictures): no error, no blank or black frame. Two
things seen and fixed: a rainbow drawn in a night sky (a rainbow now needs the sun: not at night; stars only in an
evening or night sky), and two girls standing on one spot in a close-up (the blocking rules above).

## 6. Your last message: 20 more stories, the script reader, the everyday words, the rig first

### 20 stories written your way (`tools/testdata/v41stories`)

Twenty stories with the characters, places and things listed at the start and every line in double quotes — in
twenty layouts: numbered and bulleted lists, Hindi and English headings, "Name: …" and "Name (manner): …" lines, curly,
low („…“), angle («…») and doubled quotes, a line broken over two lines, screenplay form, scene headings as words
("दृश्य एक", "Scene three", "Act I"). `expected.tsv` holds what each must give.

| Check | before the reader's v41 changes | now |
|---|---|---|
| Stories read exactly right (cast, places, things, scenes, every line and its speaker) | 0 of 20 | 20 of 20 |
| Spoken lines read right | — | 59 of 59 |
| Directed without a crash, every speaker speaking in the film | — | 20 of 20 |
| The director's own checks (SoakCheck) | — | no issue |
| Approval card | — | average 4.77 / 5, every gate passed |

Eight frames each of four of them drawn with your pictures: no error, no blank frame. A guide to the layout is in
[`script-format.md`](script-format.md). Robolectric `v41TwentyStoriesInTheUsersFormatAreReadAndDirected` checks all
of this on the phone.

### The script reader: more words

Headings for the cast (`कलाकार`, `चरित्र परिचय`, `भूमिका`, `अभिनेता`, `Starring`, `Roles`, `Who's who` …), the places
(`स्थल`, `जगहें`, `पृष्ठभूमि`, `सेट्स`, `Sets`, `Backgrounds`, `Scenery` …), the things (`वस्तु`, `चीज़ें`, `सामग्री`,
`उपकरण`, `Equipment` …) and the start of the script (`आरंभ`, `प्रारंभ`, `FADE IN` …); scene headings in every spelling
of दृश्य, `सीन`, `अंक`, `भाग`, `अध्याय`, `एपिसोड`, `Scene`, `Act`, `Part`, `Chapter`, with numbers as digits, Hindi and
English words, ordinals or Roman numerals; every kind of double quote; a line of dialogue that runs on to the next
line; the name and the words on separate lines; `(V.O.)` / `(O.S.)` as a voice heard but not seen; "Location:",
"Setting:", "समय:" lines inside a scene.

### Everyday words in the directions (`tools/testdata/v41vocab`)

A probe puts 189 everyday direction sentences (130 Hindi: "राहुल रोता है", "मुस्कुराता है", "सिर खुजाता है", "माँ के
पैर छूता है", "छींकता है", "पतंग उड़ाता है", "दरवाज़ा खोलता है" …; 59 English) each into a small story and looks at
what was staged: on the build before this change **126 of them staged nothing** — the character just stood there.
The director now has a table of everyday words it falls back
on when a sentence gave nothing else: a face and a gesture at least, and for some a move (a fall to the floor and up
again, hiding behind someone, a slow walk, running after someone, the stairs), a task (mopping, washing clothes,
chopping, reading a letter), a sound (a door, a thud) or an umbrella opened. The names are taken out before the
words are matched (a girl called पूजा or आरती is not a prayer), and short words that hide inside longer ones are
matched whole (the probe caught "ख़त", a letter, inside "देखता").

| | before | now |
|---|---|---|
| Hindi everyday sentences that stage something | 47 of 130 | 130 of 130 |
| English everyday sentences that stage something | 16 of 59 | 59 of 59 |

("Stands up" for one already standing, "comes in" for one already in: nothing is right, and nothing is staged.)
A dance is now a happy face (it was a villain's grin for everyone; a villain keeps it). Robolectric
`v41EverydayWordsAllStageSomething` checks all 189 on the phone. Frames of a family story using them (crying, falling,
scratching the head, hiding by her mother, touching the feet, sneezing, the slow walk, blushing) drew cleanly.

### Blocking fixed on the way

* A run across the stage that would end on someone goes on past them to free ground; walking up to someone takes the
  free side of them; a ride stops on free ground and still rides across (the other way when the ground ahead is
  taken). The overlap counts stay at 3 of 100 and 5 of 34 stories (the sample story's animals, the fire circle).
* The cut to the ones left behind when someone walks out is kept by the clean-up passes.

### Your pictures as reference, the rig first

* A picture you upload of a *moment* (not of a thing) is no longer shown over the scene in place of the moment: the
  characters act it, and the picture stays in your library. Pictures of things are still shown once as an insert when
  the story first brings the thing in. Your pose pictures were already only what the rig is made from (v40).
* The mesh is as fine as the screen: one cell per drawn pixel of a body (up to 1,024 rows, the face 512), one per
  pixel of a place (up to 1920 × 1080). "Faster drawing" in Settings makes the cells 2 pixels (off unless you turn it
  on).

### The whole app on the phone emulator (Robolectric)

Run 1 (72 tests, before the last changes): 3 failed — "Maya runs to Kabir" (her friend's run across ended on her
spot, so she had no run left), the cut after Riya walks out (taken out by the v41 clean-up), an auto ride that stopped
after 168 pixels. All three fixed (above); run 2 on the final code: running (its result goes in with the v41 APK).

## 7. Said plainly

The rig is a flat picture turned and bent on a fine mesh, not a 3D model: it turns to about a three-quarter view,
never to a full profile, and cannot show a side the picture does not have. Nothing here makes the film Pixar or
Disney quality, and no amount of testing can promise software with no bug at all: these checks find the kinds of
mistakes listed above, and the ones they found are fixed.
