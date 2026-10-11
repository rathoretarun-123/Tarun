# The v41 soak

`gen_stories.py OUT` writes 100 test stories (80 in Hindi in the script format of "बारिश वाला रविवार", 20 in English):
the family of four, राहुल, and the three men of "अलाव के पास" — the casts the user's own pictures cover — in homes,
the park, lanes and roads, the market, by a river, on a beach, on a hill and a roof, at four times of day, with chores,
eating and drinking, sitting, lying, sleeping, walking in and out, the swing and the slide, cycles, bikes, scooters,
autos and the family car, rain, thunder, wind, snow, fog and rainbows, an umbrella, a puddle jump, hugs, held hands and
a dozen feelings, and lines said in a dozen manners. The stories are made at random (seed 41) to stress the director;
they are not meant to read well.

`com.tarun.kahani.jvm.SoakCheck DIR FAMILY_PROJECT RAHUL_PROJECT MEN_PROJECT` directs every story (with the user's
pictures when the cast is theirs) and checks: no crash; keys in time order; nothing NaN; acts that end after they
start; every speaker in the scene, visible, inside the stage's width and inside the camera's frame during the line;
nobody left standing beyond the stage's edges; nobody walking seated or sliding lying down (a ride or a slide
aside); no flash cuts or wild zooms; segments in order.
