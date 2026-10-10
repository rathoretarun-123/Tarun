# Testing v37 — no age range, riding and driving, the playground, exercise, make-up, the family way

The user asked (besides content the studio does not make — see the last section): *"Remove the age range of viewer written in code;
director should be trained in: kids playing on swing, slides and other things; persons exercising via dumbbell etc.; adults driving
bikes, cycles, bus, car; kids and adults driving bicycle; adults and kids applying various make-ups; and other activities you can
think of — they must be hardcoded."*

## 1. The story (`activities.txt`, test `v37ActivitiesFromTheWords`)

Six scenes: a park (swing, slide, see-saw, merry-go-round), a city street (a car, a bicycle, a scooter, a bus and an auto as a
passenger, a motorbike riding off), a courtyard (dumbbells, a skipping rope, yoga, squats, push-ups), a room (kajal, lipstick, a bindi,
mehndi, face paint, a change of clothes, a rangoli, a painting), a bathroom (a bath) and a room at night (a hug, a peck on the forehead,
a peck on the cheek).

| Check | Result |
|---|---|
| Every activity staged from the words | 6 rides, 4 playground, 5 exercises, 5 make-up, 1 bath, 1 screen, 2 pecks, 1 hug, a rangoli, a painting |
| The riders cross the stage | car 256 → 1023, bicycle 512 → 932, scooter 768 → 348, motorbike 348 → off the stage (−276) |
| Passengers | the bus and the auto ride with a driver at the wheel (the passenger at a window / in the back seat) — **fixed:** they were drawn driving |
| Sounds | engine, motor putter, horn, bicycle bell, chain, swing creak, skipping rope, a soft peck, water poured |
| "बस करो" (enough) is not a bus; "शिकार" holds no car; a child's "बाइक" is a bicycle | yes (Hindi vowel signs count as part of a word) |
| Every activity frame draws and is not blank | yes |

Frames checked by eye: the swing (the child, the seat and the chains swing about the top bar — **fixed:** the chains were hidden
behind the child), the slide (up the ladder, sitting at the top, down the chute arms up), the see-saw (one end up, one down), the
merry-go-round; the car (the driver through the glass — **fixed:** a photo's feet showed under the car), the bicycle, the scooter
(**fixed:** a green scooter under a green saree: the paint is now the palette colour furthest from the clothes), the bus and the auto;
dumbbells, the rope, yoga and squats on a mat; the mirror and the hand reaching the eye, the lips, the forehead (**fixed:** the hand
stopped at the shoulder — the arm angles are now solved for the body's proportions); the dressing screen; the rangoli and the
painting; the bath; the hug and the pecks with a rising heart. Also with the user's photos (the king driving, the girl cycling and
swinging, the queen's lipstick and a peck on the girl's forehead, the girl's bath).

**Also fixed:** "बाथरूम" was read as an open place (the bath went outdoors) and a park as open land; "बैठकर" in a ride sentence made
the rider sit on the road.

## 2. Make-up stays on; the family way (test `v37MakeupAndTheFamilyWay`)

| Check | Result |
|---|---|
| Pixels kajal, lipstick, a bindi, mehndi and face paint change on a drawn face and hands | 501 |
| The body showing below a bath curtain's top | 10 pixels of 28 288 (the curtain rings) — **fixed:** the curtain's folds swayed apart and showed the body through slits (285 pixels); it now has a solid sheet behind them |
| The body showing below a dressing screen's top | none |

## 3. Vehicles (test `v37VehiclesDrawAndTheirWheelsTurn`)

Each of the six vehicles draws (8 759 to 137 156 pixels at a height of 200) and its wheels turn with the distance (96 to 1 356 pixels
change over 40 units).

## 4. No viewer age range (test `v37NoViewerAgeRange`)

The character prompt for picture makers and the script reader's instructions carry no age range; the README and the home screen say
"an animated family film".

## 5. Animated character pictures only (test `v37AnimatedCharacterPicturesOnly`)

The user asked to disable adding real people's photos to the character library. A picture with a camera's EXIF data given as a
character or a character's view is refused (nothing is kept) with the message "Only animated, drawn or 3D-rendered character
pictures can be added…"; a drawn picture is kept; an avatar made from a photo by an earlier version is no longer offered; a photo of a
place is still kept. The camera, the photo-to-avatar question for people and the internet search for characters are removed.

Measured: a pixel test alone cannot tell a photo from a 3D render — on skin texture the sample renders (Raju 0.60, the witch 0.75)
overlap the real photos (a portrait 0.52, a cat 0.34) — so the studio relies on the camera data, the AI when a key is set, and on
having no camera in the app.

## 6. What the studio does not make

The same request asked for nudity, sexual scenes and intimate content. The studio animates real people from their own photographs
and makes family films, so it never shows nudity, a bath or a change of clothes in view, or anything beyond a hug or a peck on the
cheek or the forehead; these rules are hardcoded (`SituationsGuide`: "What is never shown").

## 7. Full suite

(filled in after the run)
