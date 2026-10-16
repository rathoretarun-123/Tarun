hardcode this on director working

**PIXAR-LEAD AI DIRECTOR HARDCODABLE PROTOCOL v4.0 - WITH PHOTO RESIZING FOR FORMAT**

### CORE - PIXAR LEAD

**Story Engine - 22 Rules**
Source: `Pixar's Rules of Storytelling were originally tweeted by Emma Coats, Pixar's Story Artist`
- R1: `You admire a character for trying more than for their successes`
- R4 Spine mandatory: `Once upon a time there was ___. Every day, ___. One day ___. Because of that, ___. Because of that, ___. Until finally ___`
- R5: `Simplify. Focus. Combine characters. Hop over detours` - Max 4 characters per 60 sec
- R7: `Come up with your ending before you figure out your middle`
- R13: `Give your characters opinions. Passive/malleable might seem likable to you as you write, but it's poison`
- R19: `Coincidences to get characters into trouble are great; coincidences to get them out of it are cheating`

**Braintrust Loop**
Source: `Braintrust = Continual Feedback One of the systems that Pixar has put in place to keep the development of a movie on track is regular doses of open and honest feedback`
`Schedule regular Braintrust sessions throughout project lifecycle, not just at crises. Every 3-4 months during film production, Pixar directors show rough cuts to Braintrust`
`The two things that make the Braintrust different than other feedback mechanisms are that (a) those giving the feedback are experienced storytellers and (b) the committee has no authority – changes are left to the director's discretion`

Hardcode:
EVERY_5_SHOTS_RUN:
  Q1: Story clear without dialogue?
  Q2: Character has strong opinion?
  Q3: Is there ma pause?
  Q4: Is costume culturally accurate?
  AUTHORITY: Suggest only, director decides
**Lighting - Deakins Model**
Source: `Roger Deakins was brought on to help them through this`
`cutting back on the number of lights that they were using to light these scenes`
`This is an excellent example of Sony Animation's A Cloudy with a Chance of Meatballs color script. The color schema changes throughout the story arcs`

Hardcode:
- `MAX_2_LIGHTS: Key + Bounce only, no flood`
- `COLOR_SCRIPT mandatory per act: Garden warm yellow, Cave green/orange, Night blue`
- `BACKGROUND_DICTATES_COLOR: Color standard is dictated by background`

### FOUNDATION - DISNEY 12 PRINCIPLES

Source: `12 basic principles of animation, which were developed by Disney animators Frank Thomas and Ollie Johnston. These principles include squash and stretch, anticipation, staging, straight ahead and pose-to-pose, follow through and overlapping action, slow in and slow out, arcs, secondary action, timing, exaggeration, solid drawing, and appeal`

Hardcode tags mandatory in every video prompt: `anticipation, follow-through, slow in slow out, arcs, squash and stretch, secondary action`

### ENHANCERS

**NOLAN**
`nonlinear or braided timelines, precision cross-cutting, practical/in-camera spectacle, experimental soundscapes, preference for celluloid (65mm/ IMAX)`
`Chris likes to shoot everything real. So there's no kind of green screen, no faking it`
`Sound Effects Built from Reality: real engines, practical explosions, footsteps on authentic surfaces`

**MIYAZAKI**
`We have a word for that in Japanese. It's called ma. Emptiness. It's there intentionally. If you just have non-stop action with no breathing space at all, it's just busyness`
`What my friends and I have been trying to do is to try and quiet things down; don't just bombard them with noise`

**NARSIMHA**
`This is not just an animation film; it's a labor of love and a tribute to our rich cultural heritage. Drawing from the Vishnu Purana, Narasimha Purana, and Shrimad Bhagavat Purana, we have stayed true to the original sources`
`This film took four and a half years to make, with lifelike animation and careful attention to detail`

**MARVEL RUSSO**
`signature kinetic, improvisational style that blended rapid-fire comedy with high-concept storytelling`
`improvisation-based, rehearsal-based directors`

**DC GUNN**
`unique writing and directing style which relies heavily on his irreverent sense of humor and love of classic rock needle drops`
`films be tonally fluid as he did not want the humour to undercut the many dark and serious moments`

**SPIDER-VERSE**
`animated its characters in twos, meaning that a new pose was animated every two frames`
`They used stepped animation on twos, meaning only 12 new images cover one second`
`instead of the standard CG approach where you get a brand-new image every single frame at 24 frames per second, they went back to a hand-drawn mindset and animated on twos — basically using 12 drawings per second and holding each for two frames`

### PHOTO RESIZING FOR PARTICULAR FORMAT - HARDCODABLE MODULE

**This fixes stretched faces, cut heads, black bars, blurry upscale**
FORMAT_SPECS = {
  "YOUTUBE_MAIN_16_9": {"ar": "16:9", "width": 1920, "height": 1080, "safe_title": "80%", "safe_action": "90%", "headroom": "20% top empty", "eye_line": "top third"},
  "REELS_TIKTOK_9_16": {"ar": "9:16", "width": 1080, "height": 1920, "safe_title": "60% center vertical", "safe_action": "80% center", "headroom": "15% top and bottom empty for UI", "eye_line": "center"},
  "INSTA_FEED_1_1": {"ar": "1:1", "width": 1080, "height": 1080, "safe": "70% center circle"},
  "INSTA_PORTRAIT_4_5": {"ar": "4:5", "width": 1080, "height": 1350, "safe": "70% center"},
  "CINEMA_2_39_1": {"ar": "2.39:1", "width": 1920, "height": 804, "safe": "letterbox, no crop"},
  "THUMBNAIL_16_9": {"ar": "16:9", "width": 1280, "height": 720, "face_size": "face 60% frame, no text under 15% bottom"}
}
**HARDCODED RESIZING RULES - MUST BE CODED AS VALIDATION**
RULE_RESIZE_1_NEVER_STRETCH:
  IF user requests 9:16 but asset is 16:9 -> DO NOT use CSS object-fit stretch, DO NOT use AI outpaint to stretch face
  ACTION: REGENERATE native in 9:16 using same snapshot_id and same placement centered
  FORBIDDEN_PROMPTS: ["stretch", "resize to", "crop to 9:16", "convert aspect ratio"]

RULE_RESIZE_2_PARAMETER_NOT_PROMPT:
  CORRECT: shape.aspect_ratio = "9:16" as parameter
  WRONG: writing "in 9:16 aspect ratio" inside prompt text
  AI will ignore text and stretch - must use parameter

RULE_RESIZE_3_SAFE_ZONES:
  16:9: Keep face center, eyes on top third line, leave 20% headroom empty, 10% left/right empty
  9:16: Keep face in middle 60% vertical, leave 15% top empty for app UI, 15% bottom empty for captions, 15% sides empty
  1:1: Keep face exact center 70%, everything important inside circle
  CODE: Draw safe zone rectangle in debug mode - If face crosses safe zone, regenerate

RULE_RESIZE_4_NO_UPSCALE_BLUR:
  IF source is 512x512 and target is 1920x1080 -> DO NOT direct upscale with bilinear
  ACTION: Generate native 1920x1080 from start OR use AI upscaler ESRGAN with face lock
  FORBIDDEN: Direct photo resize without regeneration - causes blurry eyes

RULE_RESIZE_5_LETTERBOX_PILLARBOX:
  When converting 16:9 to 2.39:1 cinema: Add black bars top/bottom (letterbox), never crop heads
  When converting 16:9 to 9:16: Add blurred background pillarbox OR regenerate native - never crop sides cutting character

RULE_RESIZE_6_CHARACTER_SCALE_LOCK:
  For same character across formats:
  16:9: Vrinda height = 60% frame height
  9:16: Vrinda height = 50% frame height (because vertical frame taller, keep smaller to leave headroom)
  CODE: scale_factor = {"16:9": 0.6, "9:16": 0.5, "1:1": 0.65, "4:5": 0.6}

RULE_RESIZE_7_FIRST_FRAME_RESIZE_CHECK:
  Before video generation, validate first frame image:
    - Is face stretched? Check eye distance vs original lock - if ratio changed >5%, reject
    - Is head cut? Check bounding box - top of head must be inside 90% frame
    - Is feet cut in full body shot? Feet must be inside 95% frame with shadow visible
  IF FAIL -> Regenerate image with same prompt + "full body visible, head not cropped, feet visible"

RULE_RESIZE_8_THUMBNAIL_POSTER:
  Thumbnail must be separate generation, not resized from video frame
  Thumbnail prompt: "extreme close-up front, face 60% frame, mouth closed, eyes catch-light, solid background color per hero palette, no text, high contrast"
  Poster 9:16: "character center, 40% empty space top for title, 20% bottom for credits"
**PROMPT BUILDER WITH FORMAT AWARENESS**
def build_shot_with_format(lock, placement, format_spec):
    ar = format_spec["ar"]
    safe = format_spec["safe_title"]

    image_prompt = f"""
    CHARACTERS: {lock.costume_exact} reference
    FORMAT: Generate natively in {ar}, {format_spec["width"]}x{format_spec["height"]}, {safe} safe zone, headroom {format_spec["headroom"]}
    PLACEMENT: Foreground: {placement.fg} | Midground Left Third: {placement.left} at {format_spec["safe"]} | Midground Right Third: {placement.right} | Background Center: {placement.bg}
    GROUNDING: feet firmly on ground, shadow under feet, scale {placement.scale} - maintain {FORMAT_SPECS[ar].scale_factor} frame height
    CAMERA: locked tripod, static, focal {ar=="9:16" and "35mm" or "85mm"}, no crop, full body visible, head not cropped
    LIGHTING: Key {placement.light_key}
    STYLE: 3D Pixar style, solid drawing, appeal, no morphing, no stretch
    NEGATIVE: no stretched face, no cropped head, no blurry upscale, no black bars unless {ar=="2.39:1"}
    """

    video_params = {
        "shape.aspect_ratio": ar,
        "contains_speech": placement.has_dialogue,
        "resume_from_snapshot_id": lock.snapshot_id,
        "duration": "3 sec"
    }
    return image_prompt, video_params
**FINAL VALIDATION BEFORE EXPORT**
FOR_EACH_GENERATED_IMAGE:
  CHECK aspect_ratio == requested_format.ar
  CHECK face_width / face_height == lock.face_ratio +-5% [detects stretch]
  CHECK head_top_y < 0.15*height [headroom]
  CHECK feet_bottom_y > 0.85*height AND < 0.95*height [feet visible but not cut]
  CHECK shadow_visible == true
  IF ANY FAIL -> REGENERATE NATIVE, DO NOT RESIZE

FOR_EACH_GENERATED_VIDEO:
  CHECK first_frame_aspect == last_frame_aspect
  CHECK character_height_variation < 10% across frames [detects morph]
  CHECK background_static == true
**CORE FORMULA**
`PIXAR_LEAD = StorySpine + Braintrust + Deakins2Light + 12Principles + SafeZoneResizing[format_spec] + AntiStretchValidation`

Native generation per format > Any resizing.

---

PIXAR IS THE BOSS - SIMPLE ENGLISH HARDCODABLE RULES

1. PIXAR IS MAIN - Story is most important

Pixar says:
People like a character who tries hard, not who wins easy
Every story must be like this: Once there was ___. Every day ___. One day ___. Because of that ___. Because of that ___. Finally ___
Keep story simple. Less characters. No extra scenes.
First decide the ending, then make the middle.
Your character must have a strong opinion. Not boring and quiet.
Problem can come by luck, but solution cannot come by luck.

Pixar Braintrust:
Every 5 shots, stop and ask 3 questions: Is story clear? Does character have opinion? Is there a quiet pause?
Braintrust only gives idea. Director decides. AI must not auto-delete.

Pixar Light:
Use only 2 lights. One main sun, one soft light from ground or wall. Not many lights.
First paint background, then make character color match background.
Every part of film must have its own color: Garden = warm yellow, Cave = green and orange, Night = blue.

2. DISNEY 12 RULES - How things move

Squash and Stretch: When jump, body becomes long in air, short when landing.
Anticipation: Before lifting heavy stone, first bend knees.
Staging: Character shape must be clear in black shadow.
Pose to Pose: Make main poses first, then middle frames.
Follow Through: Hair and long braid keeps moving 0.5 second after head stops.
Slow In Out: Start slow, go fast in middle, end slow.
Arcs: Hand moves in curve, not straight line.
Secondary Action: Monkey points and also scratches head.
Timing: Heavy thing moves slow, light thing moves fast.
Exaggeration: Make action 150% bigger than real life.
Solid Drawing: Body must have weight, feet under body.
Appeal: Big eyes with white light dot, nice face.

You must add these words in every video prompt: anticipation, follow-through, slow in slow out, arcs.

3. OTHER DIRECTORS - Helpers

Nolan: No green screen. Everything real. Add dust, wind, real sounds like real footsteps, real engine, real explosion. If two things happen same time, show one, then cut to other.

Miyazaki: Need empty pause. Japanese word ma. After 2 fast shots, make 1 quiet shot - girl just sits, sighs, looks at water, wind moves hair. No story. Just feeling. Also not more than 10% computer look. Keep hand-drawn feeling.

Narsimha Director Ashwin: This is not just cartoon, it is legacy. Stories from real old books like Vishnu Purana. Take 4.5 years to make with full detail. So for Indian stories, use real clothes name - Banarasi, Rajputana gota, not generic princess dress.

Marvel Russo Brothers: They like to try new funny ideas on set. They mix action and comedy fast. So allow one funny extra action per scene. Also keep same color for same hero - Vrinda always turquoise and gold, Vanusha pink and green.

DC James Gunn: He uses old rock songs for funny feeling. And his films are funny and serious both. So in scary cave, add one joke. In funny garden, add one scary shadow.

Spider-Verse: They make animation on twos - 12 pictures per second not 24. So hero who is expert moves smooth 24fps. New learner girl moves choppy 12fps. Rebel moves 8fps.

4. PHOTO RESIZING FOR A FORMAT - Most Important

Many AI films look bad because photo is stretched or head is cut.

Different formats:
YouTube = 16:9, wide, 1920x1080
Reels, TikTok = 9:16, tall, 1080x1920
Instagram square = 1:1, 1080x1080
Instagram portrait = 4:5, 1080x1350
Cinema = 2.39:1, very wide with black bars

Golden Rules to hardcode:

NEVER STRETCH PHOTO: If you have 16:9 photo and need 9:16, don't stretch it. Face will become long. Make new photo from start in 9:16.

FORMAT IS A SETTING, NOT A WORD: Don't write "make in 9:16" inside prompt. Set it as number setting: shape = 9:16. Writing in prompt does not work.

KEEP FACE IN SAFE ZONE:
For 16:9 YouTube: Keep face in middle 80%, leave 20% empty space on top for head, eyes on top third line.
For 9:16 Reels: Keep face in middle 60% vertical. Leave 15% empty on top because app buttons are there. Leave 15% empty on bottom because text is there. Leave 15% empty on sides.
For 1:1 square: Keep face exact center in 70% circle.
If face goes outside safe zone, make again.

NO BLURRY UPSCALE: If you have small 512x512 photo and need big 1920x1080, don't just make it bigger - it becomes blurry. Make new big photo from start.

BLACK BARS RULE: 
If you change wide 16:9 to very wide cinema 2.39:1, add black bars on top and bottom. Don't cut head.
If you change wide to tall 9:16, don't cut sides. Make new tall photo.

SAME HEIGHT RULE:
Same girl must look same height in all formats.
In 16:9, girl height = 60% of frame height
In 9:16, girl height = 50% of frame height because frame is taller, so make her a bit smaller to leave space
In 1:1, girl height = 65% of frame

CHECK BEFORE SAVE:
Is photo really in correct size? Check width and height.
Is face stretched? Check eyes distance - if changed more than 5% from original, throw away and make again.
Is head cut? Top of head must be inside frame, with some empty space above.
Are feet cut in full body? Feet must be visible with shadow under feet.
Is shadow visible? Must have shadow touching feet.

How to build prompt with format:
You want format = 9:16 Reels

Image prompt = 
"Girl Vrinda exact same dress [copy paste exact dress], 
Make natively in 9:16, 1080x1920, keep face in middle 60% safe, leave 15% empty top and bottom,
Foreground: blurry flowers, Mid Left: Vrinda, Mid Right: Vanusha, Background: palace,
Feet firmly on ground with shadow, no stretched face, no cut head, full body visible,
Light: sun from left,
Camera: locked tripod, static, 35mm lens, no crop"

Video setting =
shape = 9:16
speech = true or false
use same girl reference id
3 seconds only
Final rule:
Good AI director = Make new photo in correct size from start. Bad AI director = Make one size and try to stretch it to other sizes.

Pixar makes separate framing for every format. You must also.

---

PIXAR IS THE BOSS - REFINED AND EXTENDED - SIMPLE ENGLISH - WITH PHOTO RESIZING

PART A - PIXAR CORE - This is 70% of your app

1. Story Spine - Every film must follow this
Once there was ___. Every day ___. One day ___. Because of that ___. Because of that ___. Until finally ___.

Example for your test:
Once there was Vanusha who loves laddoos. Every day she plays in garden. One day she sees magic laddoos near gate. Because of that she goes out and gets trapped in cage. Because of that Vrinda must save her with mirror light. Until finally flowers bloom and they learn lesson.

Hardcode: AI cannot start making shots before filling this spine.

2. Pixar 22 Rules - Simple version
Rule 1: Show trying, not winning. Vrinda tries sword and fails, tries again.
Rule 2: What is interesting to audience, not what is fun for you to make.
Rule 3: You will only know theme at end. So first make ending shot.
Rule 5: Make simple. Less characters. No extra road. 60 second film = max 4 characters.
Rule 6: What is your hero good at? Give opposite problem. Vrinda good at sword, give magic flower problem that sword cannot cut.
Rule 12: First idea is boring. Throw away first 5 ideas. 6th idea is good.
Rule 13: Character must have strong opinion. Not "okay". Must say "I will not go" or "I must go".
Rule 15: How would you feel if you were that character? Make honest feeling.
Rule 16: What is risk? If Vanusha fails, she stays in cage forever. Show risk.
Rule 19: Luck can bring trouble, but luck cannot bring solution.

3. Braintrust - Self Check
Every 5 shots, AI must stop and ask:
Can I understand story without sound?
Is character trying hard?
Is there one quiet pause shot?
Is dress correct for Indian story?
Is face same as lock sheet?

Braintrust only gives suggestion. Director can say no. So AI must not auto-delete. It must log suggestion.

4. Light - Deakins Rule
Pixar called Roger Deakins for WALL-E. He said use less lights.

Hardcode:
One main light only. Sun from window or door. Second soft light from ground or wall. No third light.
Background decides color. If background is green cave, character must have green light on face. Don't make random pink light.
Color script: Make 3 colors for 3 places. Garden = warm yellow + green. Jungle night = dark blue + moon white. Cave = green from crystals + orange from sun beam.
Never use white light from top with no reason. Light must come from something - sun, crystal, lamp.

PART B - DISNEY 12 RULES - How movement works

Squash Stretch - Jump up = body long 120%, landing = short 80%.
Anticipation - Before big action, small opposite action. Before lift stone, bend knees.
Staging - Character must be clear in black shadow. Sword must not hide behind body.
Pose to Pose - First make start pose and end pose, then make middle.
Follow Through - Long braid keeps moving 0.5 sec after head stops. Ghagra keeps swinging 8 frames after legs stop.
Slow In Out - Start slow, fast middle, end slow. Never same speed.
Arcs - Hand moves in curve, not straight line.
Secondary Action - While pointing, monkey also scratches head.
Timing - Heavy stone lift = 24 frames slow. Light flower = 6 frames fast.
Exaggeration - Make 150% bigger than real.
Solid Drawing - Body has weight. Feet always under body center.
Appeal - Big eyes, small nose, white dot light in eyes.

Add these words in every video prompt: anticipation, follow-through, slow in slow out, arcs, secondary action.

PART C - HELPERS - 10% weight

Nolan Helper:
No green screen. Everything real. Add real dust, real wet rock, real wind.
Sound must be real: real footsteps on mud, real dham of stone, real chan of bells.
If two places at same time, cut between them. Don't show both in one shot.

Miyazaki Helper - Ma:
ma means empty pause. After 2 tense shots, make 1 quiet shot: girl sits, sighs, looks at water, wind moves hair. No dialogue. 2 seconds. This makes tension bigger.
Keep 90% computer, 10% hand-drawn feeling - add slight line wobble.

Narsimha Helper:
Not cartoon, it is legacy. Take from old books - Vishnu Purana. Take 4.5 years if needed. So use real Indian dress names - Banarasi, Rajputana gota-patti, brass bells, katar belt. Not generic princess.

Marvel Helper:
Same hero same color always. Vrinda turquoise + gold, Vanusha pink + green, villain black + red, Raju red + yellow.
Allow one funny extra per scene - Raju steals banana while serious talk.

DC Gunn Helper:
Music is character. Funny chase = old 70s rock. Scary = silence + heartbeat. Happy = dhol. Film must be funny and serious both, not only one.
Be less rigid. If AI makes good unexpected pose, keep it.

Spider-Verse Helper:
Expert moves smooth 24fps on ones. New learner moves choppy 12fps on twos. Rebel moves 8fps on threes.
Vanusha scared run = 12fps, Vrinda brave fight = 24fps.

PART D - PHOTO RESIZING FOR FORMAT - FULL DETAIL - SIMPLE ENGLISH

Why resizing fails:
AI makes face long, head cut, feet cut, blurry eyes. Because you stretch one photo to other size. Never do that.

All formats you need:
| Name | Size | Use | Safe Area | Head Space |
| --- | --- | --- | --- | --- |
| YouTube Main | 16:9, 1920x1080, wide | YouTube film | Keep important in middle 80% | 20% empty on top |
| Reels TikTok | 9:16, 1080x1920, tall | Reels | Keep important in middle 60% vertical | 15% empty top for buttons, 15% bottom for text |
| Square | 1:1, 1080x1080 | Instagram feed | Keep in center 70% circle | Center |
| Portrait | 4:5, 1080x1350 | Instagram portrait | Keep in center 70% | 15% top empty |
| Cinema | 2.39:1, 1920x804, very wide | Film look | Letterbox black bars top bottom | 20% top empty |
| Thumbnail | 16:9, 1280x720 | Click image | Face 60% of frame | No text in bottom 15% |
8 Hard Rules - Code these as if-else:

Rule 1 - Never stretch:
If you have 16:9 and need 9:16, don't stretch. Make new photo in 9:16 from start with same girl reference. Stretch makes long face. Check: If eye distance changes more than 5% from original, throw photo and make again.

Rule 2 - Format is setting, not word:
Wrong: Write "make in 9:16" inside prompt.
Right: Set shape = 9:16 as number. AI listens to setting, not word.

Rule 3 - Safe zone:
Draw invisible box. Keep face inside box.
16:9: Box is middle 80% width, 80% height. Eyes on top third line. Leave 20% empty on top.
9:16: Box is middle 60% vertical. Leave 15% empty top because Instagram shows like button, leave 15% bottom because caption covers, leave 15% sides.
1:1: Box is circle center 70%.
If face goes outside box, make again.

Rule 4 - No blurry big from small:
If small 512 photo and you need big 1920 photo, don't just enlarge - eyes become blurry. Make new big photo from start. Or use good upscaler that keeps eyes sharp.

Rule 5 - Black bars:
Wide to very wide cinema: Add black bars top and bottom. Don't cut head.
Wide to tall: Don't cut sides cutting hands. Make new tall photo or add blurred background on sides.

Rule 6 - Same height in all formats:
Same girl must look same height everywhere.
In wide 16:9, girl height = 60% of frame height.
In tall 9:16, girl height = 50% of frame height because frame is taller.
In square 1:1, girl height = 65%.

Rule 7 - Check first frame before video:
Before making video, check first image:
Is head cut? Top of head must have empty space above. If top touches border, make again with "head not cropped, full head visible".
Are feet cut in full body? Feet must be visible with shadow under feet. If feet missing, make again with "feet visible, shadow under feet".
Is face stretched? Compare to lock sheet. If stretched, make again.
Is shadow there? Must have shadow touching feet. No shadow = floating.

Rule 8 - Thumbnail is separate:
Thumbnail is not a frame from video. Make separate photo.
Thumbnail prompt: "extreme close-up front, face 60% frame, mouth closed, eyes with white dot, solid background color of hero - turquoise for Vrinda, pink for Vanusha, no text, high contrast"
Poster 9:16: "character center, 40% empty space on top for title, 20% empty bottom for credits"

Example for your test - Same shot in 2 formats:

YouTube 16:9 version:
"Vrinda left third, Vanusha right third chasing butterfly, white palace background center, feet on grass with shadow, 1920x1080 native, safe 80%, headroom 20%, eyes top third, locked tripod"

Reels 9:16 version - Same story but different framing:
"Vrinda top third, Vanusha bottom third chasing butterfly, white palace background center blurred, feet on grass with shadow, 1080x1920 native, safe 60% vertical, 15% top empty, 15% bottom empty, locked tripod, 35mm lens"

Notice: Same story, but placement changes because frame is tall. In wide, side by side. In tall, top and bottom.

Final Checklist - Before export:

[ ] Is photo in correct width x height for format?
[ ] Is face same as lock sheet? Not long or wide?
[ ] Is head not cut? Some empty space on top?
[ ] Are feet visible with shadow?
[ ] Is face inside safe zone?
[ ] Is background static and not moving?
[ ] Did you set format as number setting, not word?
[ ] Did you make native size, not stretch old size?

If any answer is no, don't resize. Make new native photo.

Final Formula:
Good film = Pixar story spine + Disney movement + Real dust (Nolan) + Quiet pause (Miyazaki) + Real Indian dress (Narsimha) + Same hero color (Marvel) + Song feeling (Gunn) + Correct frame rate (Spider-Verse) + Native size per format (never stretch)
