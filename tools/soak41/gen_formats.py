# v41: 20 test stories, each written a different way (headings, quotes, layouts), with what a reader must find in each
# (expected.json): the cast, places, things, scenes and every spoken line with its speaker.
import json, os, sys
out = sys.argv[1]
os.makedirs(out, exist_ok=True)

FAM = [("तरुण", "42 साल, पिता", "हल्की दाढ़ी, नीली टी-शर्ट"), ("प्रिया", "40 साल, माँ", "सूती कुर्ता, बाल जुड़े में"),
       ("वृंदा", "11 साल, बड़ी बेटी", "चश्मा, पीली टी-शर्ट"), ("वनुशा", "7 साल, छोटी बेटी", "गुलाबी फ्रॉक")]
RAHUL = [("राहुल", "8 साल, लड़का", "हरी टी-शर्ट, स्कूल बैग")]
MEN = [("अर्जुन", "30 साल, दोस्त", "क्रीम शर्ट"), ("कबीर", "31 साल, दोस्त", "नीली जैकेट"), ("समीर", "29 साल, दोस्त", "भूरा स्वेटर")]
EN = [("Maya", "9 years, a girl in a red frock", ""), ("Kabir", "12 years, a boy in a blue hoodie", ""), ("Grandpa", "70 years, white kurta and glasses", "")]

# (cast, places, things, scenes) — scenes: (place index, time, [beats]); beat: ("d", text) or ("s", speaker, manner, text)
STORIES = [
 (FAM, [("घर का लिविंग रूम", "सोफा, TV, खिड़की"), ("सोसाइटी का पार्क", "हरी घास, झूला, स्लाइड")], [("चाय की ट्रे", "स्टील की ट्रे, चार कप"), ("गेंद", "लाल रबर की गेंद")],
  [(0, "सुबह", [("d", "प्रिया चाय की ट्रे लेकर आती है और सोफे पर बैठ जाती है।"), ("s", "प्रिया", "मुस्कुराते हुए", "चाय तैयार है, सब आ जाओ!"),
                ("s", "वनुशा", "उछलते हुए", "मम्मी, आज पार्क चलेंगे?"), ("d", "तरुण अखबार रखता है और हँसता है।"), ("s", "तरुण", "", "हाँ बेटा, नाश्ते के बाद।")]),
   (1, "शाम", [("d", "वृंदा गेंद फेंकती है और वनुशा उसके पीछे दौड़ती है।"), ("s", "वृंदा", "हँसते हुए", "पकड़ो, पकड़ो!"), ("s", "वनुशा", "", "मैंने पकड़ ली!")])]),
 (RAHUL, [("स्कूल का मैदान", "बड़ा मैदान, गोलपोस्ट, पेड़")], [("फुटबॉल", "काली-सफ़ेद फुटबॉल")],
  [(0, "दोपहर", [("d", "राहुल फुटबॉल को किक मारता है।"), ("s", "राहुल", "खुश होकर", "गोल! मैंने गोल कर दिया!"), ("d", "राहुल दौड़ते हुए मैदान का चक्कर लगाता है।"), ("s", "राहुल", "हाँफते हुए", "आज मज़ा आ गया।")])]),
 (MEN, [("नदी का किनारा", "पत्थर, बहता पानी, बरगद का पेड़")], [("मछली पकड़ने की छड़", "बाँस की छड़"), ("थर्मस", "चाय का थर्मस")],
  [(0, "सुबह", [("d", "अर्जुन पत्थर पर बैठकर छड़ पानी में डालता है।"), ("s", "अर्जुन", "धीरे से", "चुप रहो, मछली डर जाएगी।"), ("s", "कबीर", "हँसते हुए", "तुम्हारी मछली तो सपनों में ही आती है!"),
                ("d", "समीर थर्मस से चाय डालता है।"), ("s", "समीर", "", "पहले चाय पी लो, फिर मछली।")])]),
 (EN, [("the kitchen", "a stove, pots and a fridge"), ("the garden", "roses, a bench and a small pond")], [("cookie jar", "a glass jar of cookies")],
  [(0, "Morning", [("d", "Maya climbs on a chair to reach the cookie jar."), ("s", "Maya", "whispering", "Just one cookie, nobody will know."), ("s", "Grandpa", "smiling", "I know, little one!"),
                   ("d", "Maya laughs and gives him a cookie.")]),
   (1, "Afternoon", [("d", "Kabir and Maya sit on the bench."), ("s", "Kabir", "", "Look, a frog in the pond!"), ("s", "Maya", "surprised", "It is so tiny!")])]),
 (FAM, [("बाज़ार", "सब्ज़ी की दुकानें, भीड़, ठेले")], [("थैला", "कपड़े का बड़ा थैला")],
  [(0, "दोपहर", [("d", "तरुण और प्रिया सब्ज़ी खरीदते हैं।"), ("s", "प्रिया", "मोल-भाव करते हुए", "भैया, टमाटर कैसे दिए?"), ("s", "तरुण", "", "थैला भर गया, अब घर चलें।"),
                 ("d", "वनुशा गुब्बारे वाले को देखती है।"), ("s", "वनुशा", "ज़िद करते हुए", "पापा, मुझे लाल गुब्बारा चाहिए!")])]),
 (RAHUL + [("दादी", "65 साल, दादी", "सफ़ेद साड़ी, चश्मा")], [("दादी का आँगन", "तुलसी का पौधा, चारपाई, आम का पेड़")], [("चारपाई", "रस्सी की चारपाई")],
  [(0, "रात", [("d", "दादी चारपाई पर बैठी हैं, राहुल उनकी गोद में सिर रखता है।"), ("s", "राहुल", "नींद में", "दादी, एक कहानी सुनाओ ना।"), ("s", "दादी", "प्यार से", "एक था राजा, एक थी रानी…"),
               ("d", "राहुल सो जाता है।")])]),
 (MEN, [("ऑफिस", "कंप्यूटर, मेज़ें, बड़ी खिड़की")], [("लैपटॉप", "चाँदी का लैपटॉप")],
  [(0, "सुबह", [("d", "कबीर लैपटॉप खोलता है और माथा पकड़ लेता है।"), ("s", "कबीर", "घबराते हुए", "रिपोर्ट गायब हो गई!"), ("s", "अर्जुन", "शांत होकर", "घबराओ मत, बैकअप में होगी।"),
                ("s", "समीर", "", "मिल गई! यह रही।"), ("d", "तीनों ताली बजाते हैं।")])]),
 (FAM, [("समुद्र तट", "रेत, लहरें, नारियल के पेड़")], [("रेत का किला", "बाल्टी और फावड़ा")],
  [(0, "शाम", [("d", "वृंदा और वनुशा रेत का किला बनाती हैं।"), ("s", "वृंदा", "", "किले पर झंडा भी लगाएँगे।"), ("d", "एक लहर आती है और किला बहा ले जाती है।"),
               ("s", "वनुशा", "रोते हुए", "मेरा किला!"), ("s", "प्रिया", "प्यार से", "कोई बात नहीं, फिर से बना लेंगे।")])]),
 (EN, [("the street", "wet road, puddles, parked cars")], [("umbrella", "a big yellow umbrella")],
  [(0, "Evening", [("d", "It starts raining heavily."), ("s", "Kabir", "shouting", "Run, Grandpa, run!"), ("d", "Grandpa opens the yellow umbrella."), ("s", "Grandpa", "laughing", "Why run? Enjoy the rain!")])]),
 (RAHUL, [("सोसाइटी का पार्क", "हरी घास, लाल झूला, स्लाइड")], [("झूला", "लाल रंग का झूला"), ("स्लाइड", "नीली-पीली स्लाइड")],
  [(0, "शाम", [("d", "राहुल दौड़ते हुए झूले पर बैठ जाता है और झूलने लगता है।"), ("s", "राहुल", "चिल्लाते हुए", "और ऊँचा! और ऊँचा!"), ("d", "राहुल सीढ़ी चढ़कर स्लाइड से नीचे फिसलता है।"),
               ("s", "राहुल", "हँसते हुए", "वाह, कितना मज़ा!")])]),
 (FAM, [("रसोई", "गैस, बर्तन, फ्रिज")], [("कड़ाही", "लोहे की कड़ाही")],
  [(0, "रात", [("d", "प्रिया कड़ाही में पकौड़े तलती है।"), ("s", "प्रिया", "", "गरम-गरम पकौड़े तैयार हैं।"), ("s", "तरुण", "सूँघते हुए", "खुशबू से ही भूख लग गई!"),
               ("d", "वृंदा पानी पीती है।"), ("s", "वृंदा", "", "मम्मी, चटनी कहाँ है?")])]),
 (MEN, [("पहाड़ी रास्ता", "चीड़ के पेड़, ठंडी हवा, पगडंडी")], [("बैग", "पहाड़ चढ़ने का बैग")],
  [(0, "सुबह", [("d", "तीनों दोस्त पगडंडी पर चलते हैं।"), ("s", "समीर", "हाँफते हुए", "और कितना चलना है?"), ("s", "अर्जुन", "", "बस वो चोटी, फिर आराम।"),
               ("d", "घना कोहरा छा जाता है।"), ("s", "कबीर", "डरते हुए", "कुछ दिख नहीं रहा!")])]),
 (EN, [("the living room", "a sofa, a TV and a window"), ("the park", "green grass, a swing, a slide")], [("kite", "a red paper kite")],
  [(0, "Morning", [("s", "Maya", "excited", "Grandpa, will you fly my kite?"), ("s", "Grandpa", "", "Of course! Let us go to the park.")]),
   (1, "Afternoon", [("d", "Grandpa holds the kite and Maya runs."), ("s", "Maya", "laughing", "It is flying!"), ("s", "Kabir", "", "Higher, Maya, higher!")])]),
 (FAM, [("घर की छत", "पानी की टंकी, खुला आसमान, पतंगें")], [("पतंग", "हरी पतंग"), ("चरखी", "लकड़ी की चरखी")],
  [(0, "दोपहर", [("d", "तरुण पतंग उड़ाता है और वनुशा चरखी पकड़ती है।"), ("s", "तरुण", "जोश में", "वो काटा!"), ("s", "वनुशा", "ताली बजाते हुए", "पापा जीत गए!"),
                 ("s", "वृंदा", "", "अब मेरी बारी।")])]),
 (RAHUL + [("माँ", "35 साल, माँ", "नीली साड़ी")], [("बेडरूम", "बिस्तर, अलमारी, खिड़की")], [("स्कूल बैग", "नीला स्कूल बैग")],
  [(0, "सुबह", [("d", "माँ राहुल को उठाती है।"), ("s", "माँ", "", "उठो राहुल, स्कूल की देर हो जाएगी।"), ("s", "राहुल", "उबासी लेते हुए", "पाँच मिनट और, माँ…"),
               ("d", "राहुल बैग उठाकर दौड़ता है।"), ("s", "राहुल", "", "बाय माँ!")])]),
 (MEN, [("ढाबा", "खाट, चूल्हा, सड़क किनारा")], [("चाय का गिलास", "काँच का गिलास")],
  [(0, "रात", [("d", "अर्जुन, कबीर और समीर खाट पर बैठकर चाय पीते हैं।"), ("s", "अर्जुन", "", "ढाबे की चाय का जवाब नहीं।"), ("s", "कबीर", "हँसते हुए", "और पराठे का भी!"),
               ("s", "समीर", "सोचते हुए", "कल फिर आएँगे।")])]),
 (FAM, [("गाँव का खेत", "सरसों के पीले खेत, कच्चा रास्ता")], [("ट्रैक्टर", "लाल ट्रैक्टर")],
  [(0, "सुबह", [("d", "तरुण ट्रैक्टर के पास खड़ा है।"), ("s", "वृंदा", "हैरान होकर", "पापा, कितने पीले फूल हैं!"), ("s", "तरुण", "", "यह सरसों है, बेटा।"),
                ("d", "तितलियाँ उड़ती हैं और वनुशा उनके पीछे दौड़ती है।"), ("s", "वनुशा", "", "तितली रानी, रुको!")])]),
 (EN, [("the school", "a classroom, desks, a blackboard")], [("chalk", "a white chalk")],
  [(0, "Morning", [("d", "Kabir writes on the blackboard."), ("s", "Kabir", "proudly", "Two plus two is four!"), ("s", "Maya", "", "Easy! Ask something harder."), ("s", "Grandpa", "laughing", "What is seven times eight?")])]),
 (RAHUL, [("सोसाइटी की गली", "बारिश के बाद गीली सड़क, पानी के छोटे-छोटे गड्ढे")], [("कागज़ की नाव", "रंगीन कागज़ की नाव")],
  [(0, "शाम", [("d", "बारिश रुक जाती है। आसमान में इंद्रधनुष निकलता है।"), ("d", "राहुल पानी में कागज़ की नाव छोड़ता है।"), ("s", "राहुल", "खुश होकर", "चल मेरी नाव, चल!"),
               ("d", "राहुल पानी के गड्ढे में कूदता है।"), ("s", "राहुल", "हँसते हुए", "छपाक!")])]),
 (FAM, [("अस्पताल", "सफ़ेद दीवारें, बिस्तर, खिड़की")], [("फूलों का गुलदस्ता", "पीले फूल")],
  [(0, "दोपहर", [("d", "वनुशा बिस्तर पर लेटी है, उसके पैर पर पट्टी बँधी है।"), ("s", "वनुशा", "उदास होकर", "मुझे घर जाना है।"), ("d", "वृंदा फूलों का गुलदस्ता देती है।"),
                 ("s", "वृंदा", "प्यार से", "जल्दी ठीक हो जाओ, छोटी।"), ("s", "प्रिया", "", "कल हम घर चलेंगे।")])]),
]

# 20 ways of writing it
def quote(t, k):
    return {0: '"%s"', 1: '“%s”', 2: '„%s“', 3: "''%s''", 4: '«%s»'}[k % 5] % t

def write(i, cast, places, things, scenes):
    f = i % 10
    en = cast is EN
    L = []
    title = {0: "शीर्षक: ", 1: "फिल्म का नाम: ", 2: "कहानी - ", 3: "Title: ", 4: "# ", 5: "शीर्षक - ", 6: "फ़िल्म का नाम - ", 7: "Film name: ", 8: "", 9: "कहानी का नाम: "}[f]
    L += [title + ("Test %d" % (i + 1) if en else "टेस्ट कहानी %d" % (i + 1)), ""]
    ch = ["पात्र परिचय", "कलाकार:", "चरित्र परिचय", "Characters", "CAST", "किरदार", "पात्रों का विवरण -", "Who's who", "भूमिकाएँ", "Starring"][f]
    pl = ["जगह का परिचय", "स्थल:", "पृष्ठभूमि", "Locations", "SETS", "जगहें", "स्थानों का विवरण -", "Places", "लोकेशन", "Backgrounds"][f]
    th = ["वस्तुएँ", "सामग्री:", "उपकरण", "Props", "PROPS", "चीज़ें", "वस्तुओं का विवरण -", "Things", "सामान", "Equipment"][f]
    L += [ch, ""]
    for n, (nm, who, look) in enumerate(cast):
        if f in (1, 5, 7): L.append("%s - %s, %s" % (nm, who, look) if look else "%s - %s" % (nm, who))
        elif f in (3, 9): L.append("* %s: %s%s" % (nm, who, (", " + look) if look else ""))
        else:
            L.append("%d. %s - %s" % (n + 1, nm, who))
            if look: L.append(("रूप: " if not en else "Look: ") + look)
    L += ["", pl, ""]
    for n, (pn, pd) in enumerate(places):
        L.append("%d. %s - %s" % (n + 1, pn, pd) if f not in (3, 9) else "* %s: %s" % (pn, pd))
    L += ["", th, ""]
    for n, (tn, td) in enumerate(things):
        L.append("%d. %s - %s" % (n + 1, tn, td) if f not in (3, 9) else "* %s: %s" % (tn, td))
    L += ["", ["---", "स्क्रिप्ट शुरू", "कहानी आरंभ", "Script starts", "FADE IN:", "---", "पटकथा शुरू", "The story begins", "---", "Screenplay begins"][f], ""]
    hi_ord = ["पहला", "दूसरा", "तीसरा", "चौथा"]
    en_num = ["One", "Two", "Three", "Four"]
    for si, (pi, tm, beats) in enumerate(scenes):
        pn = places[pi][0]
        head = [lambda: "दृश्य %d: %s - %s" % (si + 1, pn, tm), lambda: "सीन नंबर %d – %s, %s" % (si + 1, pn, tm),
                lambda: "%s दृश्य - %s (%s)" % (hi_ord[si], pn, tm), lambda: "Scene %s: %s - %s" % (en_num[si], pn, tm),
                lambda: "INT. %s - %s" % (pn.upper() if en else pn, tm.upper() if en else tm), lambda: "दृश्य संख्या %d - %s" % (si + 1, pn),
                lambda: "दृश्य-%d | %s | %s" % (si + 1, pn, tm), lambda: "SCENE %s: %s" % (["I", "II", "III", "IV"][si], pn),
                lambda: "सीन %d: %s" % (si + 1, pn), lambda: "Scene No. %d - %s - %s" % (si + 1, pn, tm)][f]()
        L += [head, ""]
        if f in (5, 8): L += ["स्थान: " + pn if not en else "Location: " + pn, ("समय: " + tm) if not en else ("Time: " + tm), ""]
        for bk, b in enumerate(beats):
            if b[0] == "d":
                L.append(["[%s]", "(%s)", "%s", "(%s)", "%s", "[%s]", "(%s)", "%s", "[%s]", "(%s)"][f] % b[1])
            else:
                _, sp, mn, tx = b
                q = quote(tx, i + bk)
                if f == 4:      # the screenplay way: the name alone, a (manner) line, the words (unquoted or quoted)
                    L.append(sp.upper() if en else sp)
                    if mn: L.append("(%s)" % mn)
                    L.append(tx if bk % 2 else q)
                elif f == 2:    # name, (manner), the quoted words, no colon
                    L.append("%s%s %s" % (sp, " (%s)" % mn if mn else "", q))
                elif f == 6:    # the name on its line ending with a colon, the words on the next
                    L.append("%s%s:" % (sp, " (%s)" % mn if mn else ""))
                    L.append(q)
                elif f == 7 and len(tx) > 20:     # a line split over two lines
                    cut = tx.rfind(" ", 0, len(tx) // 2 + 4)
                    L.append("%s%s: %s" % (sp, " (%s)" % mn if mn else "", q[:cut + 1].rstrip()))
                    L.append(q[cut + 1:])
                elif f == 8:    # dash form
                    L.append("%s%s - %s" % (sp, " [%s]" % mn if mn else "", q))
                elif f == 9 and not mn and not en:   # "X ने कहा, "…""
                    L.append("%s ने कहा, %s" % (sp, q))
                else:
                    L.append("%s%s: %s" % (sp, " (%s)" % mn if mn else "", q))
            L.append("")
    return "\n".join(L) + "\n"

exp = {}
for i, (cast, places, things, scenes) in enumerate(STORIES):
    name = "format_%02d.txt" % (i + 1)
    open(os.path.join(out, name), "w", encoding="utf-8").write(write(i, cast, places, things, scenes))
    exp[name] = {"cast": [c[0] for c in cast], "places": len(places), "things": len(things), "scenes": len(scenes),
                 "lines": [[b[1], b[3]] for sc in scenes for b in sc[2] if b[0] == "s"]}
json.dump(exp, open(os.path.join(out, "expected.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
# the same as plain lines for the Java checker: file, cast, places, things, scenes, then speaker::words for each line
with open(os.path.join(out, "expected.tsv"), "w", encoding="utf-8") as fo:
    for k, v in exp.items():
        fo.write("\t".join([k, ",".join(v["cast"]), str(v["places"]), str(v["things"]), str(v["scenes"])] + [a + "::" + b for a, b in v["lines"]]) + "\n")
print(len(STORIES), "stories")
