# Testing v44 — the family car, two on a bike, accidents, royal stories, snow and hail, animals and drinks, 100,000 stories

Your message: the family in the car (father or mother driving, another in the front seat, the family behind, the
steering, the feet on the brake and accelerator), accidents; two on a bike or a cycle, pedals and handlebar moving,
accidents; every normal activity of people and animals coded and tested (driving, doors, eating, drinking, alcohol,
walking, talking); rajkumari stories (sword practice, riding, fighting from the horse with sword and bow, hunting, the
throne); hail and snow in Hindi, snowfall and snow play; stories by the hundred and the thousand, 100,000 checked and
debugged; then the final APK and the code.

What follows is what was checked, what was wrong, what was fixed, and what is still not right.

## 1. Eight stories, rendered frame by frame with your family's pictures (`tools/testdata/v44stories`)

| story | what it asks for |
|---|---|
| s01 family car | तरुण at the wheel, प्रिया beside him, the girls behind; the steering, the accelerator, a dog, a hard brake |
| s02 scooter skid | getting on, the stand up, प्रिया behind, the scooter skids and both fall |
| s03 car crash | वृंदा on the back seat, the car hits a pole, smoke from the bonnet |
| s04 cycle for two | वनुशा on the carrier, वृंदा pedalling hard, losing her balance and falling |
| s05 rajkumari | sword practice, three arrows and the last in the gold, galloping and shooting from the horse, the throne, kneeling, the embrace |
| s06 hunt | riding into the forest, aiming at a deer, the arrow, the deer bolting |
| s07 snow | snowflakes, catching them, a snowman with a carrot nose, a snowball, hail and everyone running in |
| s08 farm and party | milking the cow, feeding the dog and the hens, whisky and wine, "चीयर्स", a walk on the phone |

What the frames showed, and what was done:

* **Car:** the family are seen inside through the windows, at their seats (the girls' heads raised to the windows);
  the close of the hands on the steering wheel and of the feet on the pedals was redrawn twice (the wheel at the top of
  the frame with the hands on its rim turning with it; the brake hanging from the dash and the accelerator hinged on
  the floor, the shoe's heel at the hinge). A seat sentence alone no longer drives the car off; one getting in no longer
  takes the others out of it; the mother named "beside him" is never the driver.
* **Cycle:** the second rider sat beside the cycle (now on the carrier); the close of the feet on the pedals had legs
  too thick and the knee out of the frame (now two-jointed legs, the knee bending, the shoes on the pedals).
* **Duel:** the two stood on one spot (now a blade's length apart, face to face); the swords hardly showed (now an
  edged blade, light on a dark line).
* **Horse:** it looked like a dog. It is now drawn as a horse — a deep body, a long arched neck with a mane, a long
  head with upright ears, a flowing tail, four jointed legs with hooves that gallop in turn, a bridle when ridden.
* **Bow:** your pictures' arms can only lift so far before the picture tears, so the bow stayed at the hip. A film
  does it the same way: the shot cuts to a close of the bow held out, the string drawn back to the cheek, held, loosed
  (the arrow leaves) — then the arrows are seen flying to the target and the close of the one in the gold. The target
  is put where no one stands between it and the archer (an arrow flew across तरुण); anyone still in the way steps
  behind the archer, or behind the target. "आख़िरी तीर सीधा निशाने के बीच में जा लगता है" no longer shoots a fourth
  arrow: it is the close of the last one in the gold.
* **Throne:** "घुटनों के बल बैठकर प्रणाम करती है" sat her on a stool; she now kneels (lower, bowing) and rises for the
  queen's embrace.
* **Hunt:** the deer was a box with spots (now a spotted deer with antlers, built like the horse); it bolted before the
  arrow was loosed (now as it leaves the bow); a hunt took a dog or a cat that had walked by for its quarry (now only
  game); it stood at the edge of the frame (now inside it).
* **Snow:** the far hills stayed green (now the land below the sky is white and the light cold); one child stayed out in
  the hail when "सब भागकर अंदर जाते हैं" — "सब" was taken as the two last named (now everyone on the stage).
* **Party:** "तरुण व्हिस्की … और प्रिया वाइन …" gave him wine and her nothing (now each drinks what their own part of
  the sentence says); the toast raises the glass each had; the pour is heard for the one who pours; the phone at the
  ear sat on the face (now at the ear's edge).

## 2. The sentences one by one

* v43's 405 everyday sentences: **405 of 405**.
* v44's 118 new sentences (car seats and driving, two-wheelers, pedalling, accidents, royal, hunt, drinks, snow, hail,
  animals, walking and talking), Hindi and English: **118 of 118**.

## 3. The story generator — 100,000 stories (`tools/jvm/…/StoryForge.java`)

It builds whole stories in your script format — a cast of 2 to 5 with names and relations, 1 to 4 scenes in the places
the sentences need, dialogue lines between the directions — out of the 523 sentences above, directs each with dolls,
and checks the film: every direction staged as it should be (in the window between the lines around it), and the film
itself sound — nobody speaking while out of sight, two people on one spot, anyone on two rides at once, walking while
seated, moving while lying, waiting off the edge of the picture, an effect that ends before it starts, a speaker never
in the frame. 1,000 of the stories are kept in `tools/testdata/v44forge` to read.

| | the first 100,000 (start of v44) | the final 100,000 |
|---|---|---|
| stories | 100,000 (seeds 1–100,000) | the same 100,000 |
| crashed | 0 | 0 |
| directions checked | 977,876 | 977,876 |
| staged as asked (pass) | 84.2 % | 97.4 % |
| state already held (context) | 2.1 % | 2.3 % |
| not staged (fail) | 13.6 % | 0.3 % |
| stories with a film fault (invariants) | 11,347 | 1,374 |
| time | 15 ms a story | 20 ms a story |

The faults still counted in the final run: two people on one spot for a moment (516 stories), an effect ending before it
starts in contrived chains of rides (244), walking while seated (283), waiting off the edge of the picture (200).

What it found, and what was fixed (each found in many stories at once):

* Getting off a bus or a rickshaw after riding one away: the bus now brings them back and they get off.
* "Pallavi — mother" was not "Mother" in "serves rotis to Mother" (the relation word itself was not a name for her).
* From the sofa "sits cross-legged on the floor" did nothing (now up and down onto the floor); hiding under the bed
  from a chair did nothing (now up and down into hiding).
* Watching TV: settled on the sofa, a laugh at the funny bit.
* "दरवाज़ा खोलकर बाहर चला जाता है" opened the door and stayed (now through it and gone).
* "ओलों से बचने के लिए सिर ढकता है" in hail already falling did nothing (now the hands over the head).
* A rider who had spoken was hidden from the scene's start because a skid "comes riding in" (now they ride from where
  they stand, turning for a run-up); a second ride out of a part hid the first ride's rider all the way (the bike rode
  with nobody on it).
* One on the back seat of a car who boards a bus rode both at once (now they get out first).
* A snow slide went into someone standing on the slope (now down a clear stretch).
* "Rides his motorbike to the market" stopped beside the others (now off to the market).
* One gone out of the picture who "gets in and starts the car" sat off the edge (now drives back in first).
* Two people left on one spot (a slide's foot, a mark): a last pass makes the one who came there — never the one
  about to speak — step aside to free ground.
* The checker itself was corrected where it was wrong: a wreck or an animal standing from the scene's start counts by
  its event (the crash, the bolt); a slide in the snow is seated movement; a fall's throw is no "moving while lying".

What is counted as **context** is a sentence whose state already held (already sitting, the sun already up, already
near the one they go to) — nothing to show. What still **fails** in the final run is listed in the table above.

## 4. The last checks, on the final code

* The eight v44 stories directed with your pictures: 8 of 8, none crashed, no problem found.
* The 523 sentences: 405 of 405 and 118 of 118.
* The 100,000 generated stories: 0 crashed; 97.4 % staged as asked, 2.3 % context, 0.3 % not staged.
* The app on the phone emulator (Robolectric): 83 tests — 80 passed, 1 skipped, 2 failed: the rainy-Sunday story had
  प्रिया drive the car (getting out of a car that stood parked brought a car back with her at the wheel) and the v43 slip
  test expected a flail where v44 makes a real fall. Both were found by this run and are fixed in v45 (see
  `docs/testing-v45.md`).
* Version 44.0; the APK is `release/KahaniFilm.apk`.

## 5. Still not right

* Your pictures are flat: their arms lift at most about 50° (a sword is raised that far; the bow is drawn in a
  close-up); kneeling shows as a lower figure with shorter legs; a glass reaches the lips only in a close-up.
* "दरबार में आकर सिंहासन पर बैठती है" sits her on the throne at the scene's start; she is not seen walking in.
* A story that drives a car away and then has someone "get out of the car" in the same scene can show the car parked
  in one colour and driven in another.
* The generated stories chain sentences no one would write together (four rides in a row); the few that still fail are
  of that kind or of the context kind.

## 6. Said plainly — and sorry

* This is not a Pixar or Disney film maker, and I cannot make it one: the characters are your flat pictures on a fine
  mesh (not 3D models that turn and bend), the places and props are drawn, and the director works from rules for the
  words it knows, not from understanding the story.
* "No bug at all" is not something I can promise. 100,000 stories find the kinds of mistakes they are built to look
  for — this round found and fixed many — and others will remain. When you see one, tell me the sentence and what went
  wrong.
