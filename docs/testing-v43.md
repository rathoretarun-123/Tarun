# Testing v43 — vehicles, doors, the kitchen, everyday life and nature, asking for pictures, uploads

Your message: the aim is a Pixar- or Disney-level film from a script and a few uploads, without any error; the
director should ask for any prop, object or place it has no picture of, and uploading should be smooth; check the
tyres turning on a car or a cycle, car doors opening and closing, getting on and off a bike or a cycle and putting
the stand; almost all the normal activities of every character and the normal things of nature, with the words for
them, tested; wind, rain, umbrella and make-up smooth; a room's door opening and closing; a meal cooked in the
kitchen; a 300 MB limit.

What follows is what was checked, what was wrong, what was fixed, and what is still not right.

## 1. Vehicles (`tools/testdata/v43vehicles`, ten stories)

A bicycle got on, ridden and left on its stand; a scooter started and waited for, a pillion getting on behind, the
ride, the stop; a car driven off to the office; a car arriving and the family getting in; an auto hailed; a school
bus; one rider following another; a motorbike arriving and parked on its stand; a jeep with friends; the same in
English. Rendered frame by frame with your family's pictures.

* **The wheels turned backwards when going left** (the turning was worked out from where the vehicle stood, times
  the way it faced). They now turn by the ground covered: checked in the test for every ride to the left — it never
  rolls back.
* **Car doors:** a car's door opens towards the camera and shuts again as the driver gets in, and the rear door as the
  family gets in one by one (seen in the back window), and both open at the stop for them to get out — with the
  sound of each door. In the ten stories: 8 doors opened, 6 shut (the others still open as the part ends).
* **Getting on and off a two-wheeler:** the rider sits astride in profile (your side picture; the three-quarter one
  where there is no side picture — your front picture had them standing inside the scooter). The kickstand goes up
  as they ride and down when they stop; a pillion gets on after the rider and off first.
* **Other mistakes found and fixed:** a family car taken for a taxi with a stranger driving; the arriving car stopping
  half out of the picture (the family walked off the edge to its rear door); a U-turn flipping the vehicle in one
  frame (now it turns round); "चलाता" read as "आता" (arrives) and "बजाता" as "जाता" (goes); a toy-sized scooter at
  the actors' feet in the street (now true size, at the kerb); a rider still sliding along after getting off.

## 2. Room doors (`tools/testdata/v43home/home_02.txt`)

"Someone knocks; वृंदा gets up and opens the door; तरुण comes in; प्रिया goes out; तरुण shuts the door."

* **First render:** at the knock वृंदा walked to the door and "knocked" from inside; when तरुण came in he walked
  through her; when he went to shut the door he stopped on the very spot where she stood (for two seconds the two
  were one figure); the open door was hard to read.
* **Now:** the knock is heard at the door with nobody named ("कोई"), and those in the room only turn to it. वृंदा
  gets up from the sofa, goes to the door, opens it and steps back into the room; तरुण comes in and stops before her.
  The door stands open (प्रिया goes out through it) until तरुण shuts it; anyone standing in a doorway makes way for
  the one going to it. The open door swings away into a lit passage, the shut door has its panels and knob.

## 3. The kitchen (`tools/testdata/v43home/home_01.txt`)

"प्रिया chops vegetables and cooks; makes tea; makes rotis on the tawa; makes dal in the cooker and then a tadka."

* **First render:** the counter stood beside her and hid the arm that should be working; the pots were small; one
  sentence with two dishes cooked only one.
* **Now:** she stands behind the counter (it hides her from the hips down, at the height her hands hang in your
  picture), and her hands are seen at the work: the knife rocking on the board, the ladle round the kadhai, the tea
  stirred and poured into two cups, the belan rolling the roti, the chimta turning it on the tawa as it puffs, a hand
  on the cooker while it whistles, the tadka spluttering and poured on the dal; each dish stays on the counter when
  done. The four sentences cook six dishes at one counter, in turn, with their sounds (chopping, sizzling, boiling,
  the whistle, the pour).

## 4. Everyday life and nature — 405 sentences (`tools/testdata/v43probe/probe.tsv`)

405 stage directions, Hindi and English, in 20 groups: morning, meals, the kitchen, chores, study, posture, moving
about, gestures, feelings, doors, vehicles, carrying, play, exercise, festivals, mishaps, rain-gear, weather, the sky,
nature. Each is staged in a little story and checked for what must happen (and what must not).

| | v42 | v43 |
|---|---|---|
| sentences staged right | 280 of 405 | **405 of 405** |
| Hindi | 183 of 261 | 261 of 261 |
| English | 97 of 144 | 144 of 144 |

The causes that were found, and fixed:

* **Words inside other words:** "लगाती" was read as "गाती" (singing), "कराहता" as "राहत" (relief), "घुटने" as the
  bell's "टन", "ठोकर खाकर" as eating, "फूल बरसाता" and "rainbow" and "petals rain down" as rain, "window" as wind,
  "waves crash on the shore" as a waving hand, "crash" as a computer glitch, "hailstones" as hailing a taxi.
  Everyday words are now read where a word begins, and these were taken one by one.
* **The one the sentence is about:** "माँ की आवाज़ सुनकर राहुल की आँख खुल जाती है" gave माँ the waking. Now the
  subject is found after a participle (…सुनकर, …देखकर) and never in the object ("माँ को", "माँ के पास"). Two people
  doing two things in one sentence ("माँ फिसल जाती हैं और राहुल उनका हाथ पकड़कर उन्हें उठाता है") each do theirs.
* **A sound in the sentence hid the action:** "धड़ाम से ज़मीन पर गिरता है" made everyone startle and the fall was
  lost; the one making the sound no longer startles at it, and the action is staged.
* **Words that were missing:** getting off a bus, an auto or a car nobody was seen riding (it arrives with them in
  it); lying or sleeping on the sofa; sitting up in bed; a sip of tea, licking an ice cream, a bite, serving food,
  showing, taking a thing from someone, clinging, pointing, jogging, bhangra, a kick sending the ball off, the phone
  answered, shaving, ironing, hanging out the washing, watering the pots, a pencil sketch, the aarti's lamp, the
  school uniform, sweat in the blazing sun, a hand over the eyes, peeping from behind the door, a tray of tea or a
  basket carried (in the hand until given or put down), hail, drops beginning to fall, the clouds rumbling, the wind
  shaking the branches, fog in which nothing can be seen, the mist clearing, smoke spreading, pigeons flying off,
  birds chirping, dry leaves falling, sunrise and sunset and night falling within a scene, the full moon.
* **Read wrongly before the director saw it:** "puts on red lipstick" changed her clothes to red (make-up is no change
  of clothes now); an umbrella named anywhere in the story was given to the character from the first frame (now from
  the sentence that takes it up); "Rahul lights a diya." was turned into Hinglish letters (a short English line with
  a name in it stays English).

## 5. Smoothness

* **Umbrella:** it opened in one frame; now it opens over 0.7 s when taken up, and as the rain comes on.
* **Make-up:** the mirror and the brush appeared in the hands before the hands moved; now they come as the hands rise
  (the make-up itself already grew in slowly).
* **The sky:** sunset, nightfall and sunrise change the light over five seconds; the full moon fades in. A first
  render made night too dark to see faces (the moon's darkening came on top of the night's) — now one darkening only.
* Wind and rain: as before (checked in the rain film).

## 6. Asking for pictures, and uploads

* **Things:** the things your script lists ("वस्तुओं का वर्णन") are now asked for (they never were), once for the
  whole story, with what the script says they look like; the things a direction names are asked for by whole words
  only ("key" was asked for because of "monkey", "फल" because of "सफल") and never a person's or a place's name. The
  make-film window lists the things still without a picture (optional — the director draws the everyday ones).
* **Places:** "समय: शाम" (the hour) was asked for as a place; no longer. A place used in several scenes got its
  picture in the first scene only, and the film waited for the others forever; a picture given for one scene is now
  the picture of every scene in that place.
* **Uploads:** any picture up to 300 MB is taken; a large one is read straight at the film's working size, never
  whole into memory (one huge picture used to fail the whole batch). A picture that cannot be read is named in the
  result (it was dropped silently unless all failed). The window shows "reading picture 12 of 40". A file over
  300 MB is refused with a note.

## 7. The last checks, on the final code

* The 405 sentences: 405 of 405.
* All the test stories directed with your pictures (the 100 soak stories, the 34 earlier ones, the 20 of v41, the 25
  of v42, the 10 vehicle stories, the 2 home stories, the 5 of v40): 196 stories, 13 328 s of film, none crashed; three things flagged, the same three as in v42 (two people standing on one spot for a moment in two of the 134 older stories, and one line spoken by someone not in the scene); these are not fixed.
* The desktop copies of the app tests: 5 of 5.
* The whole app on the phone emulator (Robolectric): ROBO_LINE
* Version 43.0; the APK is `release/KahaniFilm.apk`.

## 8. Said plainly — and sorry

* This is not a Pixar or Disney film maker, and I cannot make it one: the characters are your flat pictures on a fine
  mesh (not 3D models that turn), the places are your pictures with drawn things in front, and the director works
  from rules for the words it knows, not from understanding the story.
* "No bug at all" is not something I can promise. Testing finds and fixes the kinds of mistakes it looks for — this
  round found and fixed many — and some will remain. When you see one, tell me the sentence and what went wrong.
