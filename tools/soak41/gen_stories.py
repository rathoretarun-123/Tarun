# Generates 100 test stories (80 Hindi in the user's script format, 20 English) over everyday life, play, vehicles,
# weather and feelings, with the casts the user's own pictures cover (the family, राहुल, the three men).
import random, os, sys
out = sys.argv[1]
os.makedirs(out, exist_ok=True)

FAMILY = [("तरुण", "42 साल, पिता", "सामान्य कद, हल्की दाढ़ी, नीली टी-शर्ट", "m"),
          ("प्रिया", "40 साल, माँ", "बाल जुड़े में, सूती कुर्ता", "f"),
          ("वृंदा", "11 साल, बड़ी बेटी", "चश्मा, पोनीटेल, पीली टी-शर्ट", "f"),
          ("वनुशा", "7 साल, छोटी बेटी", "क्लिप, गुलाबी फ्रॉक", "f")]
RAHUL = [("राहुल", "8 साल, लड़का", "हरी टी-शर्ट, नीली निक्कर, स्कूल बैग", "m")]
MEN = [("अर्जुन", "30 साल, दोस्त", "क्रीम शर्ट", "m"), ("कबीर", "31 साल, दोस्त", "नीली जैकेट", "m"), ("समीर", "29 साल, दोस्त", "भूरा स्वेटर", "m")]

HOME = [("घर का लिविंग रूम", "सोफा, TV, खिड़की, एक कोने में रसोई।"), ("रसोई", "गैस, बर्तन, फ्रिज।"), ("बेडरूम", "बिस्तर, अलमारी, खिड़की।")]
PARK = [("सोसाइटी का पार्क", "हरी घास, लाल झूला, स्लाइड, पेड़।")]
STREET = [("सोसाइटी की गली", "बारिश के बाद गीली सड़क, पानी के छोटे-छोटे गड्ढे।"), ("बाहर की सड़क", "पेड़, खड़ी गाड़ियाँ, सामने बाज़ार।"), ("बाज़ार", "सब्ज़ी और फल की दुकानें, भीड़।")]
NATURE = [("नदी का किनारा", "पत्थर, बहता पानी, पेड़।"), ("गाँव का खेत", "सरसों के पीले खेत, कच्चा रास्ता।"), ("समुद्र तट", "रेत, लहरें, नारियल के पेड़।"), ("पहाड़ी रास्ता", "चीड़ के पेड़, ठंडी हवा।"), ("घर की छत", "पानी की टंकी, खुला आसमान, पतंगें।")]
TIMES = ["सुबह", "दोपहर", "शाम", "रात"]

def fem(n, cast):
    for c in cast:
        if c[0] == n: return c[3] == "f"
    return False
def v(n, cast, m, f): return f if fem(n, cast) else m

CHORES = [lambda a, c: f"{a} रसोई में चाय बनाती है।" if fem(a, c) else f"{a} रसोई में चाय बनाता है।",
          lambda a, c: f"{a} सोफे पर बैठकर अखबार पढ़{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} झाड़ू लगा{v(a, c, 'ता', 'ती')} है और फिर पौधों में पानी डाल{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} बर्तन धो{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} किताब पढ़{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} फोन पर बात कर{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} अपने बालों में कंघी कर{v(a, c, 'ता', 'ती')} है और खिड़की से बाहर देख{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} उबासी लेते हुए आ{v(a, c, 'ता', 'ती')} है और आँखें मल{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} टीवी देख{v(a, c, 'ता', 'ती')} है।",
          lambda a, c: f"{a} कपड़े तह कर{v(a, c, 'ता', 'ती')} है।"]
POSTURE = [lambda a, c: f"{a} कुर्सी पर बैठ जा{v(a, c, 'ता', 'ती')} है।",
           lambda a, c: f"{a} बिस्तर पर लेट जा{v(a, c, 'ता', 'ती')} है।",
           lambda a, c: f"{a} खड़ा हो जाता है।" if not fem(a, c) else f"{a} खड़ी हो जाती है।",
           lambda a, c: f"{a} थककर सो जा{v(a, c, 'ता', 'ती')} है।"]
MOVE = [lambda a, c: f"{a} खिड़की के पास जा{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} दौड़ते हुए आ{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} धीरे-धीरे चलकर दूसरी तरफ जा{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} कमरे से बाहर चला जाता है।" if not fem(a, c) else f"{a} कमरे से बाहर चली जाती है।"]
EAT = [lambda a, c: f"{a} चाय पी{v(a, c, 'ता', 'ती')} है।", lambda a, c: f"{a} पानी पी{v(a, c, 'ता', 'ती')} है।",
       lambda a, c: f"{a} समोसा खा{v(a, c, 'ता', 'ती')} है।"]
PLAY = [lambda a, c: f"{a} झूले पर बैठ{v(a, c, 'ता', 'ती')} है और धीरे-धीरे झूल{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} तेज़ी से झूलने लग{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} सीढ़ी चढ़कर स्लाइड से नीचे फिसल{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} घास पर लेट जा{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} तितली के पीछे दौड़{v(a, c, 'ता', 'ती')} है।"]
RIDE = [lambda a, c: f"{a} साइकिल चला{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} बाइक चलाकर आ{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} स्कूटर चला{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} ऑटो में बैठकर चला जाता है।" if not fem(a, c) else f"{a} ऑटो में बैठकर चली जाती है।"]
FEEL = [lambda a, c: f"{a} उदास होकर बैठ जा{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} रोने लग{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} ज़ोर से हँस{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} गुस्से में पैर पटक{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} हैरान होकर देख{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} डर के मारे काँपने लग{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} गुस्से का नाटक कर{v(a, c, 'ता', 'ती')} है, फिर हँस पड़{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} ताली बजा{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} नाचने लग{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} हाथ हिलाकर बाय कर{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} योग कर{v(a, c, 'ता', 'ती')} है।",
        lambda a, c: f"{a} आसमान की ओर देख{v(a, c, 'ता', 'ती')} है।"]
WEATHER = ["अचानक बादल गरजते हैं और ज़ोर से बारिश शुरू हो जाती है।", "बारिश रुक जाती है। आसमान में इंद्रधनुष निकलता है।",
           "तेज़ हवा चलने लगती है।", "बर्फ गिरने लगती है।", "घना कोहरा छा जाता है।", "बिजली चमकती है और बादल गरजते हैं।", "पक्षी चहचहाते हैं।"]
PAIR = [lambda a, b, c: f"{a} {b} को गले लगा{v(a, c, 'ता', 'ती')} है।",
        lambda a, b, c: f"{a} {b} का हाथ पकड़{v(a, c, 'ता', 'ती')} है।",
        lambda a, b, c: f"{a} धीरे-धीरे चलकर {b} के पास जा{v(a, c, 'ता', 'ती')} है।",
        lambda a, b, c: f"{a} {b} को चाय देता है।" if not fem(a, c) else f"{a} {b} को चाय देती है।"]
GROUP = ["सब एक दूसरे को गले लगाते हैं और हँसते हुए घर की तरफ चल पड़ते हैं।", "सब साथ बैठकर खाना खाते हैं।",
         "सब ताली बजाते हैं।", "सब खिलखिलाकर हँसते हैं।", "सब सोफे पर बैठ जाते हैं।"]
MANNERS = ["मुस्कुराते हुए", "हैरान होकर", "डरते हुए", "गुस्से में", "प्यार से", "उदास होकर", "खुश होकर", "हँसते हुए", "धीरे से", "चिल्लाते हुए", "नींद में", "उछलते हुए"]
LINES = ["आज मौसम कितना अच्छा है!", "चलो, जल्दी करो!", "मुझे भूख लगी है।", "यह देखो, कितना सुंदर है!", "डरो मत, हम साथ हैं।",
         "मुझे यह बिल्कुल पसंद नहीं।", "अरे वाह, यह तो कमाल हो गया!", "पहले काम, फिर खेल।", "मैं थक गया हूँ।", "क्या तुमने यह सुना?",
         "आज हम बाज़ार चलेंगे।", "धीरे चलो, गिर जाओगे।", "मुझे माफ़ कर दो।", "कल फिर आएँगे।", "चाय तैयार है!"]

def story(i, rnd):
    kind = rnd.choice(["family", "family", "family", "rahul", "men"])
    cast = FAMILY if kind == "family" else RAHUL if kind == "rahul" else MEN
    if kind == "family" and rnd.random() < 0.3: cast = rnd.sample(FAMILY, rnd.choice([2, 3]))
    names = [c[0] for c in cast]
    pool = [HOME, PARK, STREET, NATURE]
    places = []
    for k in range(rnd.choice([2, 3])):
        places.append(rnd.choice(rnd.choice(pool)))
    if kind == "rahul" and rnd.random() < 0.6: places[0] = PARK[0]
    t = [f"शीर्षक: टेस्ट कहानी {i + 1}", "", "पात्र परिचय", ""]
    for n, (nm, age, look, g) in enumerate(cast):
        t += [f"{n + 1}. {nm} - {age}", f"रूप: {look}", ""]
    t += ["जगह का परिचय", ""]
    seen = []
    for p in places:
        if p not in seen: seen.append(p)
    for n, (pn, pd) in enumerate(seen): t.append(f"{n + 1}. {pn} - {pd}")
    t += ["", "---", "", "स्क्रिप्ट शुरू", ""]
    for si, (pn, pd) in enumerate(places):
        t += [f"दृश्य {si + 1}: {pn} - {rnd.choice(TIMES)}", ""]
        home = (pn, pd) in HOME; park = (pn, pd) in PARK; street = (pn, pd) in STREET
        for b in range(rnd.randint(4, 8)):
            a = rnd.choice(names)
            r = rnd.random()
            if r < 0.4:
                other = [x for x in names if x != a]
                speak = a
                t.append(f"{speak}: ({rnd.choice(MANNERS)}) {rnd.choice(LINES)}")
            else:
                cat = rnd.random()
                if home and cat < 0.45: d = rnd.choice(CHORES)(a, cast)
                elif park and cat < 0.5: d = rnd.choice(PLAY)(a, cast)
                elif street and cat < 0.35:
                    if len(names) >= 3 and rnd.random() < 0.4:
                        dr = names[0]; ps = names[1:3]
                        d = f"{dr} कार चलाता है। {ps[0]} और {ps[1]} कार में बैठते हैं।" if not fem(dr, cast) else rnd.choice(RIDE)(a, cast)
                    else: d = rnd.choice(RIDE)(a, cast)
                elif street and cat < 0.5:
                    d = rnd.choice([f"{a} छाता लेकर बाहर आ{v(a, cast, 'ता', 'ती')} है।", f"{a} पानी के गड्ढे में कूद{v(a, cast, 'ता', 'ती')} है और ज़ोर से हँस{v(a, cast, 'ता', 'ती')} है।"])
                elif not home and cat < 0.6: d = rnd.choice(WEATHER)
                elif cat < 0.7: d = rnd.choice(POSTURE + MOVE + EAT)(a, cast)
                elif cat < 0.82 and len(names) > 1:
                    b2 = rnd.choice([x for x in names if x != a]); d = rnd.choice(PAIR)(a, b2, cast)
                elif cat < 0.88 and len(names) > 2: d = rnd.choice(GROUP)
                else: d = rnd.choice(FEEL)(a, cast)
                t.append(f"[{d}]")
            t.append("")
    return "\n".join(t)

E_CAST = [("Maya", "9 years, a girl in a red frock", "f"), ("Kabir", "12 years, a boy in a blue hoodie", "m"), ("Grandpa", "70 years, white kurta and glasses, a walking stick", "m")]
E_PLACES = [("the living room", "a sofa, a TV and a window"), ("the park", "green grass, a red swing and a slide"), ("the street", "wet road, puddles, parked cars"), ("the beach", "sand, waves and palm trees"), ("the kitchen", "a stove, pots and a fridge")]
E_DIR = ["{a} makes tea in the kitchen.", "{a} sits on the sofa and reads the newspaper.", "{a} sweeps the floor and then waters the plants.",
         "{a} rides a bicycle.", "{a} drives a car. The others sit in the car.", "Suddenly it thunders and heavy rain starts.", "The rain stops. A rainbow appears in the sky.",
         "{a} jumps into a puddle and laughs loudly.", "{a} takes an umbrella and goes out.", "{a} hugs {b}.", "{a} holds {b}'s hand.", "{a} walks slowly to the window.",
         "{a} runs in.", "{a} sits on the swing and swings slowly.", "{a} climbs the ladder and slides down the slide.", "{a} lies down on the grass.",
         "{a} starts crying.", "{a} laughs loudly.", "{a} stamps a foot angrily.", "{a} looks surprised.", "{a} trembles with fear.", "{a} claps.", "{a} dances.",
         "{a} waves goodbye.", "{a} does yoga.", "{a} drinks water.", "{a} eats a samosa.", "{a} falls asleep on the bed.", "{a} leaves the room.", "They all hug and walk home laughing.",
         "Strong wind starts blowing.", "Snow starts falling.", "Thick fog covers everything."]
E_MAN = ["smiling", "surprised", "scared", "angrily", "lovingly", "sadly", "happily", "laughing", "softly", "shouting"]
E_LINES = ["What a lovely day!", "Hurry up!", "I am hungry.", "Look, how beautiful!", "Don't be afraid, we are together.", "I don't like this at all.", "Wow, that was amazing!", "Work first, then play.", "I am tired.", "Did you hear that?"]

def estory(i, rnd):
    cast = rnd.sample(E_CAST, rnd.choice([2, 3]))
    names = [c[0] for c in cast]
    places = [rnd.choice(E_PLACES) for _ in range(rnd.choice([2, 3]))]
    t = ["Characters:"]
    for n, (nm, d, g) in enumerate(cast): t.append(f"{n + 1}. {nm} ({d})")
    t.append("Places:")
    seen = []
    for p in places:
        if p not in seen: seen.append(p)
    for n, (pn, pd) in enumerate(seen): t.append(f"{n + 1}. {pn}: {pd}")
    t.append(f"Test story {i + 1}")
    for si, (pn, pd) in enumerate(places):
        t.append(f"Scene {si + 1}: At {pn}")
        t.append(f"(Place: {pn}. {rnd.choice(['Morning', 'Afternoon', 'Evening', 'Night'])}. {names[0]} stands near the middle.)")
        for b in range(rnd.randint(4, 8)):
            a = rnd.choice(names); b2 = rnd.choice([x for x in names if x != a])
            if rnd.random() < 0.4: t.append(f'{a} ({rnd.choice(E_MAN)}): "{rnd.choice(E_LINES)}"')
            else: t.append("(" + rnd.choice(E_DIR).format(a=a, b=b2) + ")")
    return "\n".join(t) + "\n"

rnd = random.Random(41)
for i in range(100):
    s = estory(i, rnd) if i % 5 == 4 else story(i, rnd)
    open(os.path.join(out, f"soak41_{i:03d}.txt"), "w", encoding="utf-8").write(s)
print("ok")
