package com.tarun.kahani.core;

import java.util.Locale;

/**
 * v34: the director's training for the situations of everyday stories — glasses and goggles, a walking stick,
 * crutches, a walking frame, a wheelchair, a sling, a plaster, a bandage, a blindfold, an eye patch, an umbrella in
 * the rain, a change of clothes in the middle of the story — and for the realism of the picture and the sound. Each
 * rule is written with the code that applies it; the film's shot list says what was done for its own story; the
 * protocols screen shows this summary beside the other guides.
 */
public final class SituationsGuide {
    private SituationsGuide() {}

    public static final String TITLE = "SITUATIONS AND REALISM — the director's training for everyday stories (v34–v37)";

    /** {the rule, where the studio applies it}. */
    public static final String[][] RULES = {
            {"Read what the story says a character wears or uses", "LookDesigner: spectacles, dark glasses, goggles, a blindfold, an eye patch; a walking stick (a bare \"छड़ी\" only for an old person), crutches, a walking frame, a wheelchair; an arm in a sling, a leg in plaster, a bandaged head; an umbrella — from the description or from the action (ScriptParser.aidsFromActions)"},
            {"A change of look is an event in time", "ScriptParser.costumesFromActions: new clothes, glasses on or off, a bandage or plaster put on or taken off become a costume from that beat; only the named part heals; crutches go with the plaster; in \"the doctor bandages Rohan's leg\" the owner of the leg is the one hurt"},
            {"Stage the change, do not just swap it", "Director.newLook: new clothes get a reveal — medium-wide, a slow push-in, a sparkle, the character proud, the others surprised then delighted; a new injury: a wince and the others' concern; a bandage off: relief all round; glasses: no fuss"},
            {"How they move is who they are", "Director.mobility: a wheelchair rolls seated and never stands, runs or jumps; a stick, crutches, a walking frame or a leg in plaster walk a third slower with a limp (Renderer: the step onto the weak leg sinks deeper), hands on the stick, crutches or frame; never running, never jumping"},
            {"Never a joke, never pity", "Director: comic beats never go to a character with a walking aid or an injury; the aid is drawn plainly, as part of the character"},
            {"Frame a seated character at their own eye level", "Director.camOn: a character in a wheelchair or on a chair is framed at their eye height, the camera never looking down on them; close-ups by the face (faceCam) as for everyone"},
            {"A blindfolded character does not look", "Director.pointOfView: no point-of-view shot for a blindfold; she turns toward the sound and listens; no blinks painted over a blindfold, dark glasses or an eye patch (Renderer)"},
            {"Weather touches the characters", "Renderer: an umbrella opens over the head of whoever carries one while it rains outdoors, and keeps them dry; rain drums on it (Director.umbrellaRain); wet ground shows a faint reflection under the feet"},
            {"Light grounds the characters", "Renderer.sunShadow: under the open sky the sun casts each character's soft shadow across the ground — long in the morning and evening, short at noon, away from the sun (the rim light's side), at night only a faint one from the moon (v36), none indoors or under rain, cloud, fog or snow; the park, the street, the rooftop and the festival ground count as under the sky (Renderer.sunlit)"},
            {"Eyes on whoever speaks", "Renderer.gaze: a listener's eyes go to the one speaking, a speaker's to the one nearest; between, a small quick shift of the eyes every one and a half to two and a half seconds (fewer and smaller while they rest on someone); in a photograph the iris moves inside the face mesh while the corners of the eyes stay (Rig.faceMesh), in a drawn character the pupils move — so the shot list's GAZE line is what the frames do"},
            {"Stage directions move people", "Director.travelFrom: \"runs across the grass\" runs to the other side, \"walks slowly home\", \"runs out of the room\" or \"घर चला जाता है\" walks off the stage and is gone (only to the edge when they speak again in the scene), \"comes back\" or \"वापस आती है\" walks back in after a moment off the stage (in the new clothes when they changed), \"runs to Maya\" ends beside her; running only when the words say so and never faster than the body allows; the sample story's own staging is never doubled"},
            {"Everyone looks at what matters", "Director.watch, Renderer.gaze: someone walking in, walking off, coming back, revealed in new clothes, hurt or healed is watched by everyone on the stage; one standing with their back to it who is free turns round, a quick turn; after an exit they look after them a moment; a blindfolded character listens toward it instead"},
            {"The weather changes how people behave", "Director.skyReaction: when rain or snow begins, everyone outdoors looks up; without an umbrella they cover their heads (in snow they shiver); out in the rain without an umbrella they hurry, walking in or across (never someone on a walking aid)"},
            {"A blindfolded character feels the way", "Director.travelFrom and enter: careful steps with a hand out in front, never running"},
            {"Rooms have doors", "Director.door: in a room, a hall or a basement a door is heard on the side someone comes in or leaves by (Synth: the latch, the hinge, the door shutting)"},
            {"Steps come from where the feet are", "Director.placeSteps, Mixer: footsteps, a stick, crutches, a wheelchair and anklets move across the stereo with the walker, fade as they walk off the stage and grow as they come on"},
            {"The camera lets them go", "Director.frameAround: a still, wide frame for a walk — someone walking off leaves it (the camera never chases them off the stage), a run across crosses it, someone coming back walks into it; no walk on the stage lasts more than 4.5 s"},
            {"Every movement is heard", "Director: footsteps for every walk on the floor of the place (stone rings, earth thuds), running steps, a stick's tap, crutches, a wheelchair rolling — for entrances and for every move across the stage (walkSounds)"},
            {"The user's pictures come first", "FilmJob: the film waits for the user's pictures before the 3D maker makes any; Studio3DArt.referencePicture: a character with no picture is drawn from the closest uploaded figure of the same kind, recoloured, its face details painted on (FaceProps); the 3D doll only for what an uploaded figure cannot show (a wheelchair, crutches, a sling, a mount, more heads or arms) or when the user rejects the borrowed figure"},
            {"Lips glide, they do not flap", "Mixer.envelope: the mouth keeps half its opening through a dip shorter than 90 ms, changes at most a quarter of a full opening per frame, opens from closed at the start of a line and closes at its end; shut in every pause"},
            // v35
            {"Wind moves what grows, never what is built", "Nature.backdropMesh, gustField: in a picture of a place outdoors the grass, leaves and tree crowns (read from their colour and texture, also above the skyline) sway with the wind in slow gusts that travel across the picture, the tips more than the roots, every frame a small smooth step; walls, roads, people and the sky stay still; indoors nothing sways; Sets.paintLive: painted trees and grass sway the same way; Synth, Mixer: the wind carries a rustle of leaves and swells with each gust the picture shows"},
            {"A sharp picture", "Art.sizesFor: the pictures are read as large as the phone's free memory allows for the film's size (with half and quarter copies for small figures, Art.mip); Cutout.defringe: the halo of the old background is taken off a photo's edge; FilmLook: a light unsharp mask before the grade; VideoWriter: the High profile and variable bitrate where the phone has them; Director.faceBox: a close-up is never enlarged past one and a half times the picture's own pixels"},
            {"Bodies as the story tells them", "LookDesigner.conditionIn, Puppet, Rig, Doll3D: a limp (an uneven step, never running), one arm (an empty sleeve; one hand does the work and claps on the thigh), one leg (crutches, or an artificial leg), fewer fingers, one eye (a closed lid or a patch), blind (a cane, the hand out, no point-of-view shot, listening), deaf (a hearing aid; no startle at a sound they cannot hear — they notice the others' faces and turn; signed lines framed with the hands, the words in the subtitle)"},
            {"Everyday actions are movements, not cuts", "Director.postureFrom: sitting down on a chair, a sofa, a bed, a stool or the floor — the weight forward over the feet, slower from a low seat, the furniture creaks; getting up leans forward first; lying down sits first, then lies back; asleep the eyes shut, the breath slows and a blanket covers them in bed; waking opens the eyes, sits up, stretches and yawns, and gets up when the story says so; one asleep at the end of a part is still asleep when the next part in a room begins"},
            {"Seated means on the seat", "Puppet.seatHeight: a drawn character's hips rest on the seat's top (a chair 0.30 of their height, a sofa 0.27, a bed 0.28, a throne 0.335), the knees forward and the shins down to the floor; on the floor cross-legged, on a mat indoors; a child's furniture is a child's size; the seat is under the sitter, only armrests come in front; Rig: a photo's thighs (or a saree's lap) fold towards the camera, on the floor the shins fold under too"},
            {"Eating and drinking", "Director.mealFrom: a plate in one hand, the other to the mouth and back with chewing; a cup, a glass or a bottle raised to the lips (tea steams; a bottle tips back further), a sip or gulps heard, the cup set down with a clink; a photo cannot bend its arm, so the director shows it in a close-up where the cup or a morsel comes up into the frame to the lips in a hand of the character's own skin (Renderer.drawToLips)"},
            {"Ask for the picture that shows it", "Director.poseNeeds, AutoLibrary: one who sits or lies down in the story and has only a standing photo is asked (optionally — the film never waits for it) for a picture of them sitting or lying; the user's own pictures are always used first (Casting); meanwhile a seated speaker from a standing photo is framed from the waist up"},
            // v36
            {"Wind moves what hangs and grows", "Renderer: outdoors a gust field (Nature.gustField) moves each character's hair, a saree's pallu, a dupatta and a cloak (Puppet.clothTail: the tip lags the root and flutters), an animal's fur, mane and tail (Rig, Puppet.drawAnimal); indoors nothing blows"},
            {"Flames look like flames", "Nature.fire, Nature.tongue: layered tongues (a dark red outside, orange, a yellow-white core) that rise, narrow, flicker and lean with the wind; sparks and smoke drift the same way; a diya's and a torch's flame the same"},
            {"Fire lights and casts shadows", "Renderer.gatherLights: a fire, diyas, candles and torches light the scene in a warm pool that is strongest near them (at night the dark gathers away from them), each character gets a warm rim on the side of the fire and a shadow on the ground away from it (Renderer.fireShadow); a fire belongs to its own part and does not leak into the next"},
            {"Sun and moon cast shadows", "Renderer.sunShadow: the sun's shadow long in the morning and evening and short at noon; at night under the moon a faint cool shadow; the time of day is read from the place line (\"रात का समय\", \"सुबह\", \"evening\")"},
            {"Lighting a lamp is shown", "Director: \"दीया जलाती है\", lighting candles, a torch or a fire gets a frame low and wide enough to see the flames catch"},
            {"Everyday tasks with their tools and sounds", "Director.taskFrom: cooking at a stove (a ladle stirring a kadhai, the gas flame, steam, sizzling), sweeping with a jhadu (the swish), washing dishes at a bucket (scrubbing), reading a book or a newspaper (pages turning), writing in a notebook (the pen scratching), a phone call (the phone stays at the ear through the call's lines), brushing teeth, combing hair, watering plants (the stream from the can, pouring)"},
            // v37
            {"Riding and driving", "Director.activityFrom, Props.vehicle: a bicycle (a child's is a child's size; the bell, the chain), a motorbike, a scooter (its apron over the shins), a car (the driver seen through the glass), a bus and an auto-rickshaw (the engine or the putter, a horn); the wheels turn with the distance gone; the rider sits on the seat with the hands on the handlebar or the wheel; one who rides in a bus, a car or an auto without driving sits inside as a passenger and a driver is at the wheel; two named cyclists ride side by side; riding off leaves the stage; the paint stands out from the rider's clothes"},
            {"The playground", "Director.activityFrom, Props: a swing (an A-frame; the child, the seat and the chains swing together about the top bar, higher then lower, the chains creak), a slide (up the ladder, sit at the top, down the chute with the arms up and a laugh, stand up at its foot), a see-saw (two children face each other, one end up as the other goes down), a merry-go-round (round and round, holding the ring)"},
            {"Exercise", "Director.activityFrom, Renderer: dumbbell curls, a skipping rope (the rope passes over the head and under the feet as they hop, its tap heard), squats, yoga (arms up, a bend to each side, namaste; eyes closed; on a mat), a kneeling push-up"},
            {"Make-up stays on", "Director.activityFrom, Renderer.madeUp, Puppet: a mirror in one hand; kajal to the eye, lipstick to the lips, a bindi between the brows, powder or face paint on the cheek (the hand reaches the place on the face), mehndi drawn on the palm with a cone — and it stays on in every later scene of the film; \"मेकअप करती है\" is powder, kajal and lipstick in turn"},
            {"A bath is shown the family way", "Director.activityFrom, Props.curtain, Props.bathWater: always behind a curtain from just below the shoulders (indoors a tiled corner and a bucket; outdoors the water up to the shoulders), water poured over the head from a mug, eyes shut against it; afterwards a towel round the shoulders and wet hair for a while"},
            {"Clothes are changed behind a screen", "Director (a change of clothes on the stage): a folding screen in front, only the head above it, the old clothes thrown over its top; the new clothes are on as they step out, then the reveal"},
            {"Affection the family way", "Director.activityFrom: a hug (they come together, both arms round), a peck on the cheek, or on the forehead for a child — the eyes close a moment, a small heart rises, a soft sound; the user's own words decide who; nothing more is shown"},
            {"Animated characters only", "Library.realPersonPhoto, MainActivity: no camera for pictures, no turning a photo of a person into an avatar, no internet search for a character's picture; a camera photo of a person is refused wherever it comes in (one picture, many at once, angles, a change of clothes), the AI is asked too when a key is set, and photos or photo avatars kept by an earlier version are never offered or used"},
            {"No age is written into the studio", "the studio makes family films: no age range is set for the audience anywhere in the code or in what it asks of picture makers"},
            {"More everyday tasks", "Director.taskFrom: drawing a rangoli on the floor (it grows ring by ring and stays), painting at an easel (the picture appears stroke by stroke)"},
    };

    /** What these rules cannot do on a phone, said plainly. */
    public static final String[][] LIMITS = {
            {"A picture the user gave does not show a wheelchair, a stick or a sling", "the director cannot draw them onto a photograph convincingly: it asks for a picture that shows them, or the 3D doll shows them"},
            {"Shadows are a soft silhouette on the ground", "not a ray-traced shadow; it follows the body's bend but not the exact shape of a hand"},
            {"Studio quality", "these rules make the staging, light and sound more natural; they do not make a phone's 2.5D film the equal of a studio's hand-animated, ray-traced one"},
            // v35
            {"A standing photo sitting down", "it can only be lowered and folded at the knees; in a saree or a long skirt it reads less clearly as seated than a drawn character or the user's own sitting picture — the director asks for that picture and frames from the waist up"},
            {"A photo's arm to the mouth", "a photo's arm does not bend: the cup or the morsel reaches the lips only in a close-up, where the hand at the hip is out of the frame; when the picture is too small for a close-up, the cup stays in the hand"},
            {"Wind in a photo of a place", "the plants are found by their colour and texture, so a green wall or a painted tree may sway a little and a dry brown bush may not"},
            {"Fingers and missing limbs on a photo", "fewer fingers are drawn only on drawn characters (and asked of an AI picture maker in words); a photo is never cut — a missing arm or leg on a photo shows as the photo shows it"},
            // v36
            {"A photo's tools", "a photo's arm does not bend: a phone goes to the ear and a toothbrush to the mouth in a hand of the photo's own skin; a ladle, a broom or a newspaper is drawn at the photo's hand, which does not stir or sweep"},
            {"Light from a fire", "a warm pool, a rim and a soft shadow on the ground — not light bouncing off every surface; a photo is tinted, not relit"},
            // v37
            {"A front-facing drawn character", "pedalling legs, a full push-up seen from the side and a slide's ladder climbed rung by rung are drawn simply (the seated legs do not pedal; a push-up is a kneeling push-up seen from the front)"},
            {"Photos on vehicles and playthings", "a photo is lowered onto the seat and folded at the knees; on a bicycle or a swing it reads less clearly than a drawn character or the user's own picture of it"},
            {"What is never shown", "nudity, a bath or a change of clothes in view, and anything more than a hug or a peck on the cheek or the forehead: the studio makes family films"},
            {"Photos of real people", "character pictures are animated, drawn or 3D-rendered only: a camera photo of a person is refused (its camera data, or the AI when a key is set); a screenshot of a photo without camera data can only be caught by the AI"},
    };

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append('\n');
        for (String[] r : RULES) b.append("  • ").append(r[0]).append(" — ").append(r[1]).append('\n');
        b.append("What a phone cannot do, said plainly:\n");
        for (String[] r : LIMITS) b.append("  • ").append(r[0]).append(" → ").append(r[1]).append('\n');
        SUMMARY = b.toString();
    }

    /** The shot list's line for this film: what the director did for its situations. */
    public static String report(Story story, Film film, int eyeLevel, int blindNoPov, int reveals, int concern, int relief) {
        int aids = 0, glasses = 0, injured = 0, umbrellas = 0, changes = 0;
        for (Story.CharacterDef c : story.characters) {
            if (c.look == null) continue;
            if (c.look.aid != Look.AID_NONE) aids++;
            if (c.look.glasses > 0) glasses++;
            if (c.look.injury != 0) injured++;
            if (c.look.umbrella) umbrellas++;
            changes += c.costumes.size();
        }
        int aidSounds = 0, umbrellaRain = 0, steps = 0;
        for (Film.Sfx x : film.sfx) {
            if (x.type == Film.SFX_STICK || x.type == Film.SFX_CRUTCH || x.type == Film.SFX_WHEELCHAIR) aidSounds++;
            else if (x.type == Film.SFX_UMBRELLA_RAIN) umbrellaRain++;
            else if (x.type == Film.SFX_STEPS || x.type == Film.SFX_STEPS_HARD || x.type == Film.SFX_STEPS_RUN || x.type == Film.SFX_STEPS_LIMP) steps++;
        }
        return String.format(Locale.US, "• Situations (v34 training): %d with a walking aid, %d with glasses, goggles, a blindfold or a patch, %d hurt, %d with an umbrella, "
                + "%d changes of look — %d reveals, %d moments of concern, %d of relief; %d shots framed at a seated character's eye level; %d point-of-view shots "
                + "turned into listening for a blindfold; sounds: %d footsteps, %d of walking aids, %d of rain on an umbrella%n",
                aids, glasses, injured, umbrellas, changes, reveals, concern, relief, eyeLevel, blindNoPov, steps, aidSounds, umbrellaRain);
    }
}
