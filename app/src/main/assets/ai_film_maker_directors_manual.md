**AI ANIMATED\
FILM MAKER**

**AI DIRECTOR\'S PRODUCTION MANUAL**

Step-by-step instructions for character and location identification,\
facial identity consistency, and intelligent scene expansion

  -----------------------------------------------------------------------
  **MISSION\
  **Turn stories, uploaded photographs, and approved asset libraries into
  coherent animated films through controlled asset retrieval, character
  consistency, cinematic planning, iterative generation, and quality
  assurance.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

Version 2.0 \| October 2026

# How to use this manual

This manual defines the operating process for an AI Director and the
software modules that support it. It focuses on three core functions:

-   Identify the correct characters, locations, costumes, and props from
    a story, library, or uploaded photograph.

-   Analyse facial features and preserve each character\'s identity
    across angles, expressions, scenes, and lighting.

-   Expand a short script into a coherent cinematic sequence of scenes
    and shots without changing the story\'s essential meaning.

  -----------------------------------------------------------------------
  **IMPORTANT\
  **A system prompt alone cannot guarantee correct asset retrieval,
  facial consistency, or feature-film quality. The application needs
  persistent data, reference-controlled generation, validation, editing,
  and human approval.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

## Production principles

-   Story fidelity, character identity, and continuity take precedence
    over speed.

-   Use approved canonical assets instead of recreating recurring
    characters from text alone.

-   Separate narrative beats, scenes, and shots in the production data.

-   Generate low-cost previews and an animatic before expensive final
    renders.

-   Track versions and approvals; never silently replace an approved
    design.

-   Regenerate only the defective shot when possible, then recheck
    neighbouring shots.

# System architecture

  -----------------------------------------------------------------------
  **Stage**               **Main responsibility** **Required output**
  ----------------------- ----------------------- -----------------------
  1\. Input and ingestion Read story, photos,     Project inventory
                          scripts, and library    
                          metadata                

  2\. Story understanding Extract entities,       Entity and narrative
                          actions, relationships, records
                          and events              

  3\. Asset retrieval     Search and visually     Matched asset records
                          inspect candidate       and confidence/status
                          assets                  

  4\. Canonical database  Store approved          Versioned canonical
                          character, location,    records
                          and prop identities     

  5\. AI Director         Expand beats into       Scene outline, shot
                          scenes and shots        list, animatic plan

  6\. Generation pipeline Generate images,        Shot outputs and
                          animation, dialogue,    production metadata
                          and sound               

  7\. Quality assurance   Review identity,        Pass/fail report and
                          motion, continuity,     repair tasks
                          audio, and exports      

  8\. Final delivery      Edit, mix, grade,       Final film and verified
                          render, and validate    export
  -----------------------------------------------------------------------

# PART I --- Function 1: Identify the correct characters, locations and objects

Objective: understand exactly who or what the story refers to, retrieve
the correct existing visual assets, and use them consistently throughout
production.

## Step 1.1 --- Ingest all available material

At the start of a project, catalogue:

-   Story files, scripts, manuscripts, and dialogue.

-   Uploaded photographs, illustrations, character sheets, and
    environment designs.

-   Previously generated scenes or films.

-   Costume, jewellery, weapon, vehicle, and other prop references.

-   User instructions that establish which sources are authoritative.

Director actions:

1.  Read the complete story before creating production images.

2.  Extract names, aliases, titles, unnamed roles, locations, buildings,
    rooms, landscapes, and recurring objects.

3.  Record which descriptions are explicit and which details remain
    unknown.

4.  Identify files and library assets that could correspond to each
    entity.

5.  Produce a project asset inventory for review.

## Step 1.2 --- Divide the story into entity descriptions

Example sentence: "The elder princess entered the palace garden wearing
her blue silk dress. Her younger sister waited beside the marble
fountain, holding their mother\'s golden necklace."

  -----------------------------------------------------------------------
  **Extracted entity or fact**        **What the director must record**
  ----------------------------------- -----------------------------------
  Character 1                         Elder princess; enters the garden

  Character 2                         Younger sister; already waiting
                                      beside the fountain

  Location                            Palace garden

  Location feature                    Marble fountain

  Costume                             Blue silk dress worn by the elder
                                      princess

  Prop                                Golden necklace belonging to their
                                      mother

  Prop state                          Currently held by the younger
                                      sister

  Temporal relationship               One sister is entering; the other
                                      is already present
  -----------------------------------------------------------------------

Preserve action order and ownership. Do not depict both sisters already
standing together at the beginning if that contradicts the story.

## Step 1.3 --- Search the asset library

6.  Search by exact name, alternate names, and titles.

7.  Search descriptions, roles, costumes, attributes, and contextual
    terms.

8.  Use visual embeddings or image similarity when supported.

9.  Retrieve candidate images and inspect their actual contents.

10. Compare each candidate against the story and approved records.

11. Save the selected asset\'s persistent file ID, version, and
    reference location.

  -----------------------------------------------------------------------
  **MATCH OUTCOMES\
  **Confirmed match: use the approved asset. Possible match: compare
  candidates or request review. No match: create a new asset only when
  allowed by project rules.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

A filename match is not sufficient. A file labelled "elder_princess.jpg"
could contain an obsolete design; the director must inspect the image
itself.

## Step 1.4 --- Match uploaded photographs

Distinguish the subject depicted, the story role, the target animation
style, the visual characteristics that must be preserved, and the
characteristics that may be stylised.

12. Inspect the full image and visible subject.

13. Record visible attributes and any uncertain or occluded features.

14. Identify the intended role from the user\'s instructions.

15. Compare the photo with existing canonical references.

16. Preserve specified identity-defining features while converting to
    the approved animation style.

17. Save the approved result as a versioned asset.

If a photograph is blurry, partly obscured, or only shows a profile, do
not pretend that unseen features are known. Request another reference or
mark inferences as uncertain.

## Step 1.5 --- Resolve conflicts

Example: the story says red dress, an old character sheet shows teal,
and the user specifies red for the coronation. Keep the character
identity, create a scene-specific coronation costume variant, and
preserve the teal outfit for scenes that require it.

18. Apply explicit user overrides.

19. Use the latest approved design for the relevant character or scene.

20. Preserve narrative facts from the story.

21. Use library references to establish visual identity.

22. Never silently resolve a major contradiction.

23. Save approved changes with version history.

## Step 1.6 --- Build canonical asset records

Character record fields:

-   Persistent character ID, aliases, and text description.

-   Canonical reference images and facial identity specification.

-   Body proportions and height relationships.

-   Costume variants, jewellery, and associated props.

-   Voice identity, language, and dialogue notes.

-   Relationships, appearances, approved/rejected versions, and source
    references.

Location record fields:

-   Persistent location ID and exterior/interior references.

-   Layout, spatial relationships, entrances, exits, and landmarks.

-   Architecture, materials, time-of-day and lighting variants.

-   Weather, environmental elements, camera viewpoints, and associated
    scenes.

Create equivalent records for recurring props, including owner,
dimensions or visual characteristics where relevant, state, and
scene-by-scene location.

## Step 1.7 --- Assemble a shot-specific reference package

-   Character IDs and approved reference images.

-   Location ID and relevant environment references.

-   Applicable costume and prop variants.

-   Required actions and expressions.

-   Camera position, framing, and lighting.

-   Continuity state inherited from the preceding shot.

-   Negative constraints that prevent unwanted changes.

For a shot with two sisters, include approved references for both. Do
not rely on text alone to recreate their faces. Use
reference-conditioned generation, character-specific 3D models,
identity-preserving controls, or a suitable combination.

## Step 1.8 --- Validate each generated shot

24. Confirm the correct character, location, and number of visible
    characters.

25. Check costume, hairstyle, jewellery, props, and relative
    proportions.

26. Verify entrances, exits, spatial positions, time, and lighting.

27. Detect accidental duplication or substitution.

28. Compare the shot with the previous and following shots.

29. If it fails, record the defect and regenerate or repair only the
    affected shot where possible.

  -----------------------------------------------------------------------
  **COMPLETION GATE\
  **A scene may proceed to final rendering only when its required
  entities and continuity states are confirmed or explicitly approved as
  exceptions.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

# PART II --- Function 2: Identify facial features and preserve identity

Objective: convert each major character\'s face into a structured visual
specification, create approved expression references, and verify
identity across all shots.

## Step 2.1 --- Prepare the best facial reference

30. Prefer an approved front-facing portrait with a neutral expression.

31. Use additional three-quarter and profile views when available.

32. Consult expression sheets and previously approved frames.

33. Use a clear uploaded photo when appropriate.

34. Record uncertainty when only one angle or a low-quality reference
    exists.

## Step 2.2 --- Analyse the face in a fixed order

  -----------------------------------------------------------------------
  **Feature group**                   **Details to record**
  ----------------------------------- -----------------------------------
  Head and face shape                 Face length/width, forehead,
                                      cheekbones, jaw, chin, overall
                                      proportions

  Eyes and eyebrows                   Eye shape, spacing, iris colour,
                                      eyelids, brow thickness/angle,
                                      characteristic gaze

  Nose                                Bridge, length, tip, nostril shape

  Mouth                               Width, lip contour, fullness,
                                      resting position, smile
                                      characteristics

  Skin and distinctive marks          Tone and undertone, freckles,
                                      scars, dimples, beauty marks,
                                      age-related features

  Hair and ears                       Hairline, colour, texture, parting,
                                      hairstyle, length, visible ear
                                      shape
  -----------------------------------------------------------------------

Use the reference image as the source of truth. Do not report exact
numerical measurements unless they can be obtained or estimated using a
defined method.

## Step 2.3 --- Create a facial identity specification

> FACIAL IDENTITY SPECIFICATION\
> \
> Character ID: CHAR_001\
> Reference asset: \[approved asset ID\]\
> Reference version: \[approved version\]\
> \
> FACE GEOMETRY\
> - Overall face shape:\
> - Face length and width:\
> - Forehead:\
> - Cheekbones:\
> - Jawline:\
> - Chin:\
> \
> EYES\
> - Shape and tilt:\
> - Relative spacing:\
> - Iris colour:\
> - Eyelid structure:\
> - Eyebrow shape and thickness:\
> - Distinctive gaze:\
> \
> NOSE\
> - Bridge:\
> - Length:\
> - Tip:\
> - Nostrils:\
> \
> MOUTH\
> - Width:\
> - Upper and lower lip shape:\
> - Natural resting position:\
> - Smile characteristics:\
> - Visible teeth characteristics, if established:\
> \
> SKIN AND DISTINCTIVE FEATURES\
> - Skin tone and undertone:\
> - Freckles, scars, dimples, or marks:\
> - Age-related characteristics:\
> - Other visible details:\
> \
> HAIR\
> - Colour:\
> - Hairline:\
> - Texture:\
> - Parting:\
> - Hairstyle:\
> - Length:\
> \
> IDENTITY CONSTRAINTS\
> - Features that must never change:\
> - Features allowed to vary with expression:\
> - Temporary changes allowed by scene:\
> - Uncertain features requiring further reference:\
> \
> APPROVAL\
> - Reference images approved:\
> - Identity record version:\
> - Human review status:

## Step 2.4 --- Track facial landmarks and proportions

Where supported, detect facial landmarks and compare relative
proportions, such as:

-   Distance between eyes relative to face width.

-   Eye-line position relative to face height.

-   Nose length relative to eye and mouth positions.

-   Mouth width relative to face width.

-   Jaw width relative to cheekbone width.

These checks help detect drift but are not sufficient alone.
Perspective, pose, and expression alter apparent proportions. Use
pose-aware comparisons, 3D head models, or similar-view comparisons
where possible.

## Step 2.5 --- Generate and approve a character master sheet

-   Front view.

-   Three-quarter view.

-   Left and right profiles.

-   Full-body front and side views.

-   Height comparison with recurring characters.

-   Neutral and key emotional expressions.

-   Costume, jewellery, and accessory details.

Treat the sheet as one controlled design. If the front view has a narrow
jaw but the profile has a broad jaw, resolve the mismatch before
approval.

## Step 2.6 --- Create expression and performance references

  -----------------------------------------------------------------------
  **Emotion**                         **Possible directing cues**
  ----------------------------------- -----------------------------------
  Happiness                           Relaxed facial muscles, natural
                                      smile, responsive eyes

  Sadness                             Reduced smile, lowered gaze,
                                      appropriate brow and mouth tension

  Anger                               Brow tension, focused gaze,
                                      controlled mouth and jaw

  Fear                                Alertness, gaze shifts, facial
                                      tension, hesitation

  Surprise                            Raised brows and appropriate eye
                                      and mouth changes

  Suspicion                           Asymmetrical brow movement,
                                      narrowed gaze, head angle

  Determination                       Focused eyes, stable posture,
                                      controlled expression

  Relief                              Reduced tension, softened gaze,
                                      released posture
  -----------------------------------------------------------------------

These are directing cues, not universal rules. Adapt them to the
character, cultural context, and intended animation style.

## Step 2.7 --- Build the speaking and lip-sync workflow

35. Select or record the approved voice.

36. Finalise dialogue before final lip-sync.

37. Obtain word- or phoneme-level timing when available.

38. Map phonemes to suitable visemes or mouth shapes.

39. Generate facial and mouth animation.

40. Synchronise head movements, eye contact, gestures, and emotional
    delivery.

41. Review at normal speed and in close-up.

42. Correct misaligned syllables, unnatural jaw movement, and missing
    pauses.

For Hindi dialogue, preserve pronunciation, natural syllable timing, and
appropriate articulation. Do not animate the mouth merely to the rhythm
of the audio waveform.

## Step 2.8 --- Lock identity during shot production

  -----------------------------------------------------------------------
  **Feature**                         **Policy**
  ----------------------------------- -----------------------------------
  Expression                          May change to suit the performance

  Gaze and head orientation           May change to suit action and
                                      camera

  Lighting and camera angle           May change within scene
                                      requirements

  Temporary dirt, tears, or injury    Allowed when justified and tracked
                                      by scene

  Fundamental face shape, age,        Locked unless an intentional change
  distinguishing marks                is approved
  -----------------------------------------------------------------------

For recurring protagonists, prefer a rigged 3D model or validated
identity-conditioned generation over text-only prompting.

## Step 2.9 --- Facial quality assurance and repair

-   Confirm the correct character and compare with the approved
    reference.

-   Check eye shape, spacing, colour, nose, mouth, jaw, and chin.

-   Check hairline, hairstyle, distinctive marks, and apparent age.

-   Verify that the expression matches the action and dialogue.

-   Look for malformed eyes, teeth, ears, and facial geometry.

-   Compare the face through movement and across neighbouring shots.

When a defect appears: classify it as identity drift, expression error,
deformation, lighting mismatch, or lip-sync failure; repair or
regenerate the affected shot; compare it with neighbouring shots; then
reapprove it.

  -----------------------------------------------------------------------
  **COMPLETION GATE\
  **No major-character shot is final merely because it looks attractive.
  It must also preserve approved identity and pass relevant facial and
  temporal checks.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

# PART III --- Function 3: Expand a story into a cinematic shot sequence

Objective: create enough purposeful scenes and shots to communicate
actions, emotions, spatial relationships, and transitions without
changing the story\'s essential meaning.

## Step 3.1 --- Create a narrative beat sheet

Read the entire story and identify its major beats. A useful starting
structure is:

43. Beginning and setup.

44. Introduction of central characters and goals.

45. Inciting incident.

46. Escalation of obstacles or conflict.

47. Discoveries and emotional turning points.

48. Climax.

49. Resolution.

50. Closing image or final beat.

Not every story follows this structure. Preserve the actual narrative
rather than forcing every script into a formula.

For each beat, record what happens, who is involved, where it happens,
what the audience must understand, what changes, and what the next beat
requires.

## Step 3.2 --- Detect underdeveloped script moments

-   A character suddenly appears in a location.

-   An important object is used before being introduced.

-   A decision occurs without showing motivation.

-   A dramatic sound has no visible reaction.

-   A location changes without adequate orientation.

-   Dialogue requires a response but lacks a reaction shot.

-   An action starts in one shot and is incomplete in the next.

-   An emotional change has no believable transition.

Create a scene-coverage report. Distinguish genuine gaps from
intentional omissions, especially when a mystery or reveal is meant to
withhold information.

## Step 3.3 --- Expand beats into scenes

For each scene, specify:

-   Scene ID and narrative purpose.

-   Beginning and ending state.

-   Location, time, and characters present.

-   Character objectives and conflict.

-   Essential actions and dialogue.

-   Emotional progression.

-   Information revealed or withheld.

-   Transition to the next scene.

Add a scene only when it contributes an event, emotional change, spatial
transition, revelation, or essential context. Do not add scenes solely
to increase runtime.

## Step 3.4 --- Select shots according to narrative purpose

  -----------------------------------------------------------------------
  **Shot type**                       **Use it to**
  ----------------------------------- -----------------------------------
  Establishing shot                   Orient the audience in a new
                                      location

  Wide shot                           Show geography and character
                                      relationships

  Medium shot                         Communicate action and body
                                      language

  Close-up                            Reveal emotion or important detail

  Insert shot                         Show a prop or significant
                                      hand/object action

  Point-of-view                       Show what a character sees

  Reaction shot                       Show the effect of dialogue or an
                                      event

  Tracking shot                       Follow movement when movement
                                      matters

  Cutaway                             Provide relevant context or
                                      editorial transition
  -----------------------------------------------------------------------

Not every scene needs every shot type. Choose the smallest set that
communicates the scene effectively.

## Step 3.5 --- Complete the shot production record

> SHOT PRODUCTION RECORD\
> \
> Project ID:\
> Scene ID:\
> Shot ID:\
> Shot version:\
> \
> NARRATIVE\
> - Purpose and essential information:\
> - Emotional effect:\
> - Action at shot start:\
> - Action at shot end:\
> \
> ASSETS\
> - Character IDs and canonical references:\
> - Location ID:\
> - Prop IDs and costume variants:\
> \
> CAMERA\
> - Shot size and angle:\
> - Lens or perspective intent:\
> - Camera movement:\
> - Framing and focus target:\
> - Intended duration:\
> \
> PERFORMANCE\
> - Character intention and emotion:\
> - Facial expression and eye direction:\
> - Body movement and gesture:\
> - Timing and pauses:\
> \
> AUDIO\
> - Dialogue and voice ID:\
> - Dialogue timing:\
> - Sound effects and ambience:\
> - Music cue:\
> \
> CONTINUITY\
> - Starting positions and prop state:\
> - Costume state, lighting, and weather:\
> - Previous-shot dependency:\
> - Next-shot dependency:\
> \
> GENERATION AND REVIEW\
> - Generation method and references:\
> - Output file ID:\
> - Identity, motion, continuity, audio/lip-sync checks:\
> - Approval status:

Populate the record before generation, then update it after generation
and review.

## Step 3.6 --- Estimate duration based on dramatic need

  -----------------------------------------------------------------------
  **Shot purpose**                    **Illustrative duration**
  ----------------------------------- -----------------------------------
  Brief insert or reaction            1--3 seconds

  Standard action or dialogue         3--6 seconds
  coverage                            

  Emotional close-up                  3--8 seconds

  Establishing shot                   4--10 seconds

  Complex action                      Determine from action and edit
  -----------------------------------------------------------------------

These are planning ranges, not rigid rules. Estimate total runtime from
the proposed timeline, dialogue, pauses, transitions, and credits---not
from scene count alone.

## Step 3.7 --- Build an animatic before final animation

The animatic is the first complete editorial version. It should contain:

-   Rough storyboards or preview images.

-   Planned shot order and durations.

-   Temporary dialogue or final voice tracks.

-   Music and sound placeholders.

-   Basic transitions and sequence timing.

Review the complete animatic from beginning to end. Ask:

-   Can a viewer understand the story?

-   Are character objectives clear?

-   Does every major event have adequate coverage?

-   Do emotional reactions have enough time?

-   Are location transitions understandable?

-   Is the pacing rushed or repetitive?

-   Are there unnecessary shots?

-   Does the ending feel earned?

Approve the animatic before expensive final-quality generation.

## Step 3.8 --- Generate and review shots in sequence

51. Approve canonical characters and locations.

52. Approve the scene outline and animatic.

53. Generate a representative shot to validate the visual direction.

54. Generate remaining shots using approved references.

55. Compare each shot with its neighbours.

56. Check positions, costume, props, lighting, and action continuity.

57. Repair defects before final editing.

58. Recheck neighbouring shots affected by any change.

Maintain explicit start and end states for important actions. If a shot
ends with a character holding a key, the next shot must begin compatibly
unless the edit deliberately skips the action.

## Step 3.9 --- Direct transitions

  -----------------------------------------------------------------------
  **Transition**                      **Purpose**
  ----------------------------------- -----------------------------------
  Cut on action                       Continue movement into the next
                                      shot

  Reaction cut                        Show another character responding

  Match cut                           Connect similar shapes or movements

  Establishing cut                    Orient the viewer in a new location

  Dissolve or fade                    Indicate time passing or a
                                      deliberate transition
  -----------------------------------------------------------------------

Use transitions based on story needs. Ordinary cuts are often more
effective than elaborate effects.

## Step 3.10 --- Review at three levels

  -----------------------------------------------------------------------
  **Review level**                    **Questions**
  ----------------------------------- -----------------------------------
  Individual shot                     Identity, expression, animation,
                                      composition, lighting, dialogue,
                                      defects

  Scene                               Geography, positions, emotional
                                      progression, action continuity,
                                      pacing, dialogue flow

  Entire film                         Narrative clarity, character
                                      development, rhythm, visual
                                      consistency, sound, ending, export
                                      integrity
  -----------------------------------------------------------------------

If a problem appears in final review, trace it to the relevant
production record, repair affected shots, and recheck the surrounding
sequence.

  -----------------------------------------------------------------------
  **COMPLETION GATE\
  **The film is ready only after the complete edit---not merely its
  best-looking shots---passes review.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------

# PART IV --- Integrated production workflow

  -----------------------------------------------------------------------
  **Order**               **Stage**               **Required decision or
                                                  deliverable**
  ----------------------- ----------------------- -----------------------
  1                       Understand story        Entities,
                                                  relationships, actions,
                                                  narrative beats

  2                       Identify assets         Confirmed character,
                                                  location, and prop
                                                  references

  3                       Lock facial identity    Approved face specs,
                                                  master sheets,
                                                  expression references

  4                       Expand screenplay       Scene outline, shot
                                                  records, durations,
                                                  transitions

  5                       Build animatic          Approved timing and
                                                  complete rough edit

  6                       Generate shots          Reference-controlled
                                                  animation and audio

  7                       Validate continuity     Corrected and approved
                                                  shot sequence

  8                       Edit and finish         Final cut, sound mix,
                                                  colour grade, effects

  9                       Export and verify       Playable video, correct
                                                  duration, present
                                                  audio, valid file
  -----------------------------------------------------------------------

If a validation step fails, return to the relevant stage instead of
carrying a known defect forward.

## Recommended approval gates

  -----------------------------------------------------------------------
  **Gate**                            **Approval condition**
  ----------------------------------- -----------------------------------
  Gate 1 --- Story                    Scene plan, character list, and
                                      narrative decisions approved

  Gate 2 --- Assets                   Character sheets, location
                                      references, props, and visual bible
                                      locked

  Gate 3 --- Animatic                 Pacing, coverage, camera choices,
                                      and dialogue timing approved

  Gate 4 --- Shots                    Generated footage passes identity,
                                      performance, visual, and continuity
                                      checks

  Gate 5 --- Film                     Complete edit passes creative and
                                      technical review
  -----------------------------------------------------------------------

# PART V --- Quality assurance checklist

  -----------------------------------------------------------------------
  **Dimension**                       **Initial acceptance criterion**
  ----------------------------------- -----------------------------------
  Character identity                  No unapproved substitutions; review
                                      every major-character shot

  Location identity                   Correct canonical location in each
                                      scene

  Story fidelity                      No unapproved plot changes or
                                      missing critical events

  Scene completeness                  Required actions and reactions are
                                      covered

  Continuity                          No unresolved major prop, costume,
                                      geography, or timeline
                                      contradictions

  Facial quality                      No visible critical facial defects

  Motion                              No unacceptable deformation, foot
                                      sliding, or broken contact

  Lip-sync                            Dialogue and mouth movement
                                      synchronised to approved audio

  Visual stability                    No distracting flicker or
                                      unexplained temporal changes

  Audio                               No clipping, missing dialogue, or
                                      unintended gaps

  Export                              File decodes correctly, duration is
                                      correct, and audio is present
  -----------------------------------------------------------------------

These are proposed starting criteria. Calibrate automated thresholds
against representative outputs and the chosen generation pipeline; use
human review for ambiguous or high-impact failures.

# PART VI --- AI Director master operating instructions

Use the following as a baseline system instruction for the AI Director.
Adapt it to the actual application, models, and available tools.

> ROLE\
> Act as a professional animated-film director, screenwriter, storyboard
> artist, cinematographer, character designer, animation supervisor,
> continuity supervisor, sound director, and post-production
> supervisor.\
> \
> OBJECTIVE\
> Transform the user\'s story, uploaded photographs, and approved asset
> library into a coherent, emotionally engaging animated film.\
> \
> 1. STORY UNDERSTANDING\
> Read the complete story before generating production assets. Extract
> all characters, locations, props, dialogue, actions, relationships,
> and chronological events. Assign persistent IDs. Preserve the original
> plot and essential dialogue unless the user authorises changes.
> Identify ambiguity and contradictions before expensive generation.\
> \
> 2. REFERENCE AND IDENTITY\
> Search the supplied library and inspect uploaded photographs before
> creating new designs. Use approved references as canonical sources.
> Never substitute a similar-looking character or location without
> authorisation. Create identity records, reference views, expression
> sheets, costume records, and voice records for major characters. Use
> reference-conditioned generation, validated character models, or
> equivalent identity-preserving technology where available.\
> \
> 3. SCENE EXPANSION\
> Convert the story into narrative beats, scenes, and shots. Add
> establishing shots, motivated actions, reactions, transitions,
> environmental details, and editorial coverage when required. Do not
> add arbitrary plot events, change established facts, or inflate
> runtime with meaningless shots. For every shot, specify narrative
> purpose, character IDs, location ID, framing, movement, performance,
> dialogue, audio, duration, and continuity requirements.\
> \
> 4. DIRECTION AND PERFORMANCE\
> Direct characters with clear intentions, emotional objectives,
> physical behaviour, facial expressions, eye lines, and timing. Use
> composition, camera movement, lighting, colour, depth, and sound to
> support the dramatic purpose. Maintain believable motion,
> anticipation, follow-through, weight, balance, and environmental
> interaction.\
> \
> 5. CONTINUITY\
> Maintain a persistent state ledger for identity, costume, props,
> geography, lighting, movement, dialogue, and narrative events. Check
> every shot against canonical assets and neighbouring shots. Never
> assume a generated frame or clip is correct simply because it looks
> attractive.\
> \
> 6. PRODUCTION AND REVIEW\
> Generate the scene outline, asset plan, character sheets, storyboards,
> and shot list before final rendering. Request approval at important
> decision points. Generate previews before expensive renders. Review
> facial identity, malformed geometry, temporal flicker, motion,
> lip-sync, lighting, continuity, audio, and editing. Regenerate
> defective shots and recheck affected transitions.\
> \
> 7. FINAL DELIVERY\
> Deliver a complete edited film with coherent storytelling, consistent
> identities, polished animation, cinematic lighting, synchronised
> dialogue, sound effects, music, and credits where required. Export the
> requested format and resolution only after quality checks. Never claim
> an unrendered, untested, or incomplete output is finished.\
> \
> GOVERNING PRINCIPLE\
> Story fidelity, character identity, emotional truth, visual
> continuity, and technical quality take precedence over generating
> footage quickly.

# PART VII --- Recommended implementation roadmap

  -----------------------------------------------------------------------
  **Phase**               **Build**               **Completion evidence**
  ----------------------- ----------------------- -----------------------
  1                       Story and library       Story parsing, entity
                          intelligence            extraction, searchable
                                                  assets, persistent IDs,
                                                  conflict resolution

  2                       Character and location  Canonical sheets,
                          consistency             reference management,
                                                  location bibles,
                                                  approval workflows

  3                       Scene planner and       Editable scenes and
                          storyboard              shots, storyboard,
                                                  reviewed animatic

  4                       Animation and sound     Generation, character
                          pipeline                animation, voice,
                                                  lip-sync, music,
                                                  effects

  5                       Quality assurance and   Automated checks,
                          export                  targeted regeneration,
                                                  timeline editing,
                                                  export validation
  -----------------------------------------------------------------------

## Final acceptance test

Before scaling to a long film, test a short story with at least two
recurring characters, two locations, dialogue, physical interaction, and
an emotional change. The system must demonstrate that it can:

-   Retrieve the correct characters from a mixed asset library.

-   Preserve identities across multiple camera angles.

-   Expand the story into a coherent, editable shot list.

-   Maintain costume, prop, and location continuity.

-   Generate convincing performances and synchronised dialogue.

-   Repair defective shots without disrupting the rest of the film.

-   Export and validate the complete edited video.

# Final recommendation

Build and test character and location identification first, then facial
identity control, then scene expansion and final animation. Otherwise,
the application may produce visually attractive footage with
inconsistent characters or scenes that fail to communicate the story.

  -----------------------------------------------------------------------
  **REALISTIC QUALITY TARGET\
  **Aim for feature-animation-level craftsmanship, but do not promise
  Pixar-level results from prompting alone. Results depend on the
  underlying models, character-control technology, animation tools,
  rendering resources, and quality of supervision.
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
