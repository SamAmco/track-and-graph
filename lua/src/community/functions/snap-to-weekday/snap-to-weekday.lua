-- Lua Function to snap data point timestamps to the same local time on a specific weekday
-- This function adjusts timestamps to the same local time but on the specified weekday based on the direction (next, last, or nearest)

local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-to-weekday",
  version = "1.0.1",
  inputCount = 1,
  categories = { "_time" },

  title = {
  	["en"] = "Snap To Weekday",
  	["af"] = "Belyn met Weekdag",
  	["sq"] = "Përputh me ditën e javës",
  	["am"] = "ወደ የሳምንቱ ቀን አጠጋጋ",
  	["hy"] = "Համապատասխանեցնել շաբաթվա օրվան",
  	["az"] = "Həftə gününə uyğunlaşdır",
  	["bn"] = "সপ্তাহের দিনে স্ন্যাপ করুন",
  	["eu"] = "Doitu asteko egunera",
  	["be"] = "Прывязаць да дня тыдня",
  	["bg"] = "Привеждане към ден от седмицата",
  	["my"] = "အပတ်စဉ်နေ့သို့ ချိန်ညှိရန်",
  	["ca"] = "Ajusta al dia de la setmana",
  	["zh-Hans"] = "对齐到星期几",
  	["zh-Hant"] = "貼齊至星期",
  	["hr"] = "Poravnaj s danom u tjednu",
  	["cs"] = "Přichytit ke dni v týdnu",
  	["da"] = "Fastlås til ugedag",
  	["nl"] = "Afronden op weekdag",
  	["et"] = "Joonda nädalapäevale",
  	["fil"] = "Iayon sa Araw ng Linggo",
  	["fi"] = "Kohdista viikonpäivään",
  	["fr"] = "Aligner sur un jour de la semaine",
  	["gl"] = "Axustar ao día da semana",
  	["ka"] = "კვირის დღეზე მიმაგრება",
  	["de"] = "An Wochentag ausrichten",
  	["el"] = "Προσαρμογή σε ημέρα εβδομάδας",
  	["gu"] = "અઠવાડિયાના દિવસ પર સ્નૅપ કરો",
  	["hi"] = "सप्ताह के दिन पर स्नैप करें",
  	["hu"] = "Igazítás hét napjához",
  	["is"] = "Samræma við vikudag",
  	["id"] = "Sesuaikan ke Hari dalam Minggu",
  	["it"] = "Allinea al giorno della settimana",
  	["ja"] = "曜日にスナップ",
  	["kn"] = "ವಾರದ ದಿನಕ್ಕೆ ಹೊಂದಿಸಿ",
  	["kk"] = "Апта күніне дәл келтіру",
  	["km"] = "កែតម្រូវទៅថ្ងៃក្នុងសប្ដាហ៍",
  	["ko"] = "요일로 맞추기",
  	["ky"] = "Аптанын күнүнө тегиздөө",
  	["lo"] = "ປັບໄປຫາມື້ໃນອາທິດ",
  	["lv"] = "Pielāgot nedēļas dienai",
  	["lt"] = "Pritraukti prie savaitės dienos",
  	["mk"] = "Прицврсти на ден во неделата",
  	["ms"] = "Selaraskan Kepada Hari Minggu",
  	["ml"] = "ആഴ്ചയിലെ ദിവസത്തിലേക്ക് സ്‌നാപ്പ് ചെയ്യുക",
  	["mr"] = "आठवड्याच्या दिवसावर स्नॅप करा",
  	["mn"] = "Гарагт тааруулах",
  	["ne"] = "हप्ताको दिनमा स्न्याप गर्नुहोस्",
  	["no"] = "Juster til ukedag",
  	["pl"] = "Dopasuj do dnia tygodnia",
  	["pt"] = "Ajustar para o dia da semana",
  	["pa"] = "ਹਫ਼ਤੇ ਦੇ ਦਿਨ ’ਤੇ ਸਨੈਪ ਕਰੋ",
  	["ro"] = "Fixează la ziua săptămânii",
  	["rm"] = "Adattar a la di dad emna",
  	["ru"] = "Привязать к дню недели",
  	["sr"] = "Poravnaj sa danom u nedelji",
  	["si"] = "සතියේ දිනයකට ගැළපීම",
  	["sk"] = "Prichytiť k dňu v týždni",
  	["sl"] = "Prilagodi na dan v tednu",
  	["es"] = "Ajustar al día de la semana",
  	["sw"] = "Pangilia kwa Siku ya Wiki",
  	["sv"] = "Justera till veckodag",
  	["ta"] = "வாரநாளுக்கு பொருத்து",
  	["te"] = "వారపు రోజుకు స్నాప్ చేయి",
  	["th"] = "ปรับไปยังวันในสัปดาห์",
  	["tr"] = "Hafta Gününe Hizala",
  	["uk"] = "Прив’язати до дня тижня",
  	["vi"] = "Căn đến ngày trong tuần",
  },

  description = {
  	["en"] = [[
Snaps data point timestamps to the same local time on a specific weekday.

- Weekday: The target day of the week (Monday through Sunday)
- Direction: Last, Nearest, or Next occurrence of that local time on that weekday

The data point keeps its original time of day but moves to the specified weekday.
  	]],
  	["af"] = [[
Belyn datapunt-tydstempels met dieselfde plaaslike tyd op ’n spesifieke weekdag.

- Weekdag: Die teikendag van die week (Maandag tot Sondag)
- Rigting: Laaste, naaste of volgende voorkoms van daardie plaaslike tyd op die weekdag

Die datapunt behou sy oorspronklike tyd van die dag, maar skuif na die gespesifiseerde weekdag.
  	]],
  	["sq"] = [[
Përputh vulat kohore të pikave të të dhënave me të njëjtën orë lokale në një ditë specifike të javës.

- Dita e javës: Dita e synuar e javës (nga e hëna deri të dielën)
- Drejtimi: Shfaqja e fundit, më e afërt ose e ardhshme e asaj ore lokale në atë ditë jave

Pika e të dhënave ruan orën e saj origjinale të ditës, por zhvendoset në ditën e specifikuar të javës.
  	]],
  	["am"] = [[
የውሂብ ነጥቦችን የጊዜ ማህተሞች በተወሰነ የሳምንቱ ቀን ላይ ወዳለው ተመሳሳይ የአካባቢ ሰዓት ያጠጋጋል።

- የሳምንቱ ቀን፦ የታለመው ቀን (ከሰኞ እስከ እሁድ)
- አቅጣጫ፦ በዚያ የሳምንቱ ቀን ያለው የዚያ የአካባቢ ሰዓት የመጨረሻ፣ በጣም ቅርብ ወይም ቀጣይ መከሰት

የውሂብ ነጥቡ የመጀመሪያውን የቀኑ ሰዓት ይጠብቃል፣ ወደተገለጸው የሳምንቱ ቀን ግን ይንቀሳቀሳል።
  	]],
  	["hy"] = [[
Տվյալակետերի ժամանակացույցերը համապատասխանեցնում է շաբաթվա որոշակի օրվա նույն տեղական ժամին։

- Շաբաթվա օր՝ թիրախային օրը (երկուշաբթիից կիրակի)
- Ուղղություն՝ այդ օրվա և տեղական ժամի վերջին, ամենամոտ կամ հաջորդ հանդիպումը

Տվյալակետը պահպանում է օրվա սկզբնական ժամը, սակայն տեղափոխվում է նշված շաբաթվա օր։
  	]],
  	["az"] = [[
Məlumat nöqtələrinin zaman damğalarını müəyyən həftə günündəki eyni yerli vaxta uyğunlaşdırır.

- Həftə günü: Hədəf həftə günü (Bazar ertəsindən bazara qədər)
- İstiqamət: Həmin həftə günündəki yerli vaxtın Sonuncu, Ən yaxın və ya Növbəti baş verməsi

Məlumat nöqtəsi orijinal gün vaxtını saxlayır, lakin göstərilən həftə gününə keçir.
  	]],
  	["bn"] = [[
ডেটা পয়েন্টের টাইমস্ট্যাম্পকে নির্দিষ্ট সপ্তাহের দিনে একই স্থানীয় সময়ে স্ন্যাপ করে।

- সপ্তাহের দিন: সপ্তাহের লক্ষ্য দিন (সোমবার থেকে রবিবার)
- দিক: ওই সপ্তাহের দিনে স্থানীয় সময়ের সর্বশেষ, নিকটতম বা পরবর্তী উপস্থিতি

ডেটা পয়েন্ট তার মূল দিনের সময় বজায় রাখে, তবে নির্দিষ্ট সপ্তাহের দিনে চলে যায়।
  	]],
  	["eu"] = [[
Datu-puntuen denbora-zigiluak asteko egun zehatz bateko tokiko ordu berera doitzen ditu.

- Asteko eguna: Helburuko asteko eguna (astelehena eta igandea artean)
- Norabidea: Asteko egun horretako tokiko orduaren azken, hurbileneko edo hurrengo agerraldia

Datu-puntuak jatorrizko eguneko ordua mantentzen du, baina zehaztutako asteko egunera mugitzen da.
  	]],
  	["be"] = [[
Прывязвае меткі часу кропак даных да таго ж мясцовага часу ў пэўны дзень тыдня.

- Дзень тыдня: мэтавы дзень тыдня (з панядзелка па нядзелю)
- Напрамак: апошняе, найбліжэйшае або наступнае ўваходжанне гэтага мясцовага часу ў гэты дзень тыдня

Кропка даных захоўвае зыходны час сутак, але перамяшчаецца на зададзены дзень тыдня.
  	]],
  	["bg"] = [[
Привежда времевите отпечатъци на точките от данни към същия местен час в определен ден от седмицата.

- Ден от седмицата: Целевият ден от седмицата (от понеделник до неделя)
- Посока: Последното, най-близкото или следващото настъпване на този местен час в съответния ден от седмицата

Точката от данни запазва оригиналния си час от денонощието, но се премества към зададения ден от седмицата.
  	]],
  	["my"] = [[
ဒေတာအမှတ်များ၏ timestamp များကို သတ်မှတ်ထားသော အပတ်စဉ်နေ့တွင် တူညီသော ဒေသစံတော်ချိန်သို့ ချိန်ညှိသည်။

- အပတ်စဉ်နေ့: ရည်ရွယ်သည့် အပတ်စဉ်နေ့ (တနင်္လာမှ တနင်္ဂနွေအထိ)
- ဦးတည်ချက်: ထိုအပတ်စဉ်နေ့ရှိ ဒေသစံတော်ချိန်၏ နောက်ဆုံး၊ အနီးဆုံး သို့မဟုတ် နောက်တစ်ကြိမ်ဖြစ်စဉ်

ဒေတာအမှတ်သည် မူလနေ့စဉ်အချိန်ကို ထိန်းသိမ်းထားပြီး သတ်မှတ်ထားသော အပတ်စဉ်နေ့သို့ ရွှေ့သည်။
  	]],
  	["ca"] = [[
Ajusta les marques de temps dels punts de dades a la mateixa hora local d’un dia concret de la setmana.

- Dia de la setmana: El dia objectiu de la setmana (de dilluns a diumenge)
- Direcció: Darrera, més propera o següent ocurrència d’aquesta hora local en aquell dia de la setmana

El punt de dades conserva la seva hora del dia original, però es mou al dia de la setmana especificat.
  	]],
  	["zh-Hans"] = [[
将数据点时间戳对齐到指定星期几的相同本地时间。

- 星期几：目标星期（星期一至星期日）
- 方向：该星期几对应本地时间的上一次、最近一次或下一次出现

数据点会保留原本的时间，但移动到指定星期几。
  	]],
  	["zh-Hant"] = [[
將資料點的時間戳記貼齊至特定星期的相同當地時間。

- 星期：目標星期（星期一至星期日）
- 方向：該星期當地時間的上一個、最近或下一個出現時間

資料點會保留原本的一天中時間，但移至指定星期。
  	]],
  	["hr"] = [[
Poravnava vremenske oznake podatkovnih točaka s istim lokalnim vremenom određenog dana u tjednu.

- Dan u tjednu: Ciljani dan u tjednu (od ponedjeljka do nedjelje)
- Smjer: Posljednja, najbliža ili sljedeća pojava tog lokalnog vremena na tom danu u tjednu

Podatkovna točka zadržava izvorno vrijeme dana, ali se premješta na navedeni dan u tjednu.
  	]],
  	["cs"] = [[
Přichytí časová razítka datových bodů ke stejnému místnímu času v konkrétní den týdne.

- Den v týdnu: Cílový den týdne (pondělí až neděle)
- Směr: Poslední, nejbližší nebo další výskyt tohoto místního času v daný den týdne

Datový bod si zachová původní čas dne, ale přesune se na zadaný den týdne.
  	]],
  	["da"] = [[
Fastlåser datapunkters tidsstempler til samme lokale tidspunkt på en bestemt ugedag.

- Ugedag: Den ønskede ugedag (mandag til søndag)
- Retning: Seneste, nærmeste eller næste forekomst af dette lokale tidspunkt på den pågældende ugedag

Datapunktet bevarer det oprindelige tidspunkt på dagen, men flyttes til den angivne ugedag.
  	]],
  	["nl"] = [[
Rondt tijdstempels van gegevenspunten af op dezelfde lokale tijd op een specifieke weekdag.

- Weekdag: De gewenste dag van de week (maandag tot en met zondag)
- Richting: Laatste, dichtstbijzijnde of volgende keer dat lokale tijdstip op die weekdag

Het gegevenspunt behoudt het oorspronkelijke tijdstip, maar wordt verplaatst naar de opgegeven weekdag.
  	]],
  	["et"] = [[
Joondab andmepunktide ajatemplid kindla nädalapäeva samale kohalikule kellaajale.

- Nädalapäev: sihtnädalapäev (esmaspäevast pühapäevani)
- Suund: selle kohaliku kellaaja viimane, lähim või järgmine esinemine sel nädalapäeval

Andmepunkt säilitab algse kellaaja, kuid liigub määratud nädalapäevale.
  	]],
  	["fil"] = [[
Ina-adjust ang mga timestamp ng data point sa parehong lokal na oras sa isang partikular na araw ng linggo.

- Weekday: Target na araw ng linggo (Lunes hanggang Linggo)
- Direction: Huli, pinakamalapit, o susunod na paglitaw ng lokal na oras na iyon sa araw na iyon

Pinananatili ng data point ang orihinal nitong oras ng araw ngunit inililipat sa tinukoy na araw ng linggo.
  	]],
  	["fi"] = [[
Kohdistaa datapisteiden aikaleimat samaan paikalliseen kellonaikaan tiettynä viikonpäivänä.

- Viikonpäivä: Kohdeviikonpäivä (maanantaista sunnuntaihin)
- Suunta: Viimeisin, lähin tai seuraava tämän paikallisajan esiintymä kyseisenä viikonpäivänä

Datapiste säilyttää alkuperäisen kellonaikansa, mutta siirtyy määritettyyn viikonpäivään.
  	]],
  	["fr"] = [[
Aligne les horodatages des points de données sur la même heure locale d’un jour précis de la semaine.

- Jour de la semaine : Jour cible de la semaine (du lundi au dimanche)
- Direction : Dernière occurrence, occurrence la plus proche ou prochaine occurrence de cette heure locale ce jour-là

Le point de données conserve son heure d’origine, mais est déplacé au jour de la semaine indiqué.
  	]],
  	["gl"] = [[
Axusta as marcas temporais dos puntos de datos á mesma hora local nun día específico da semana.

- Día da semana: O día de destino da semana (de luns a domingo)
- Dirección: Última, máis próxima ou seguinte aparición desa hora local nese día da semana

O punto de datos conserva a súa hora do día orixinal, pero móvese ao día da semana especificado.
  	]],
  	["ka"] = [[
მონაცემთა წერტილების დროის ნიშნულებს კონკრეტულ კვირის დღეზე იმავე ადგილობრივ დროზე ასწორებს.

- კვირის დღე: კვირის სამიზნე დღე (ორშაბათიდან კვირამდე)
- მიმართულება: ამ ადგილობრივი დროის ბოლო, უახლოესი ან შემდეგი დადგომა მითითებულ კვირის დღეს

მონაცემის წერტილი ინარჩუნებს დღის თავდაპირველ დროს, მაგრამ გადადის მითითებულ კვირის დღეზე.
  	]],
  	["de"] = [[
Passt die Zeitstempel von Datenpunkten an dieselbe lokale Uhrzeit an einem bestimmten Wochentag an.

- Wochentag: Der Zielwochentag (Montag bis Sonntag)
- Richtung: Letztes, nächstgelegenes oder nächstes Auftreten dieser lokalen Uhrzeit an diesem Wochentag

Der Datenpunkt behält seine ursprüngliche Tageszeit bei, wird aber auf den angegebenen Wochentag verschoben.
  	]],
  	["el"] = [[
Προσαρμόζει τις χρονικές σημάνσεις των σημείων δεδομένων στην ίδια τοπική ώρα μιας συγκεκριμένης ημέρας της εβδομάδας.

- Ημέρα εβδομάδας: Η ημέρα-στόχος της εβδομάδας (Δευτέρα έως Κυριακή)
- Κατεύθυνση: Τελευταία, Πλησιέστερη ή Επόμενη εμφάνιση αυτής της τοπικής ώρας στη συγκεκριμένη ημέρα

Το σημείο δεδομένων διατηρεί την αρχική ώρα της ημέρας, αλλά μετακινείται στην καθορισμένη ημέρα.
  	]],
  	["gu"] = [[
ડેટા પોઇન્ટના ટાઇમસ્ટેમ્પ્સને ચોક્કસ અઠવાડિયાના દિવસે સમાન સ્થાનિક સમય પર સ્નૅપ કરે છે.

- અઠવાડિયાનો દિવસ: અઠવાડિયાનો લક્ષ્ય દિવસ (સોમવારથી રવિવાર)
- દિશા: તે અઠવાડિયાના દિવસે સ્થાનિક સમયની છેલ્લી, સૌથી નજીકની અથવા આગલી ઘટના

ડેટા પોઇન્ટ તેનો મૂળ દિવસનો સમય જાળવે છે પરંતુ નિર્દિષ્ટ અઠવાડિયાના દિવસે ખસે છે.
  	]],
  	["hi"] = [[
डेटा पॉइंट के टाइमस्टैम्प को किसी विशिष्ट सप्ताह के दिन के उसी स्थानीय समय पर स्नैप करता है।

- सप्ताह का दिन: सप्ताह का लक्ष्य दिन (सोमवार से रविवार)
- दिशा: उस सप्ताह के दिन के स्थानीय समय की पिछली, निकटतम या अगली घटना

डेटा पॉइंट दिन का अपना मूल समय बनाए रखता है, लेकिन निर्दिष्ट सप्ताह के दिन पर चला जाता है।
  	]],
  	["hu"] = [[
Az adatpontok időbélyegét egy adott hétköznap ugyanarra a helyi időre igazítja.

- Hét napja: A hét célzott napja (hétfőtől vasárnapig)
- Irány: Az adott helyi idő utolsó, legközelebbi vagy következő előfordulása azon a hétköznapon

Az adatpont megtartja eredeti napszakát, de a megadott hétköznapra kerül.
  	]],
  	["is"] = [[
Samræmir tímamerki gagnapunkta við sama staðartíma á tilteknum vikudegi.

- Vikudagur: Markmiðsdagur vikunnar (mánudagur til sunnudags)
- Stefna: Síðasta, næsta eða næsta tímasetning miðað við nálægð á þeim vikudegi

Gagnapunkturinn heldur upprunalegum tíma dags en færist á tilgreindan vikudag.
  	]],
  	["id"] = [[
Menyesuaikan stempel waktu titik data ke waktu lokal yang sama pada hari tertentu dalam seminggu.

- Hari dalam Minggu: Hari target dalam seminggu (Senin hingga Minggu)
- Arah: Kemunculan terakhir, terdekat, atau berikutnya dari waktu lokal tersebut pada hari itu

Titik data mempertahankan waktu aslinya, tetapi berpindah ke hari yang ditentukan.
  	]],
  	["it"] = [[
Allinea i timestamp dei punti dati alla stessa ora locale di uno specifico giorno della settimana.

- Giorno della settimana: il giorno di destinazione della settimana (da lunedì a domenica)
- Direzione: ultima, più vicina o successiva occorrenza di quell'ora locale in quel giorno della settimana

Il punto dati mantiene l'ora originale della giornata, ma viene spostato al giorno della settimana specificato.
  	]],
  	["ja"] = [[
データポイントのタイムスタンプを、特定の曜日の同じ現地時刻にスナップします。

- 曜日: 対象の曜日（月曜日から日曜日）
- 方向: その曜日の現地時刻における前回、最も近い、または次回

データポイントは元の時刻を維持したまま、指定した曜日に移動します。
  	]],
  	["kn"] = [[
ಡೇಟಾ ಬಿಂದುಗಳ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್‌ಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ವಾರದ ದಿನದ ಅದೇ ಸ್ಥಳೀಯ ಸಮಯಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ.

- ವಾರದ ದಿನ: ಗುರಿ ವಾರದ ದಿನ (ಸೋಮವಾರದಿಂದ ಭಾನುವಾರದವರೆಗೆ)
- ದಿಕ್ಕು: ಆ ವಾರದ ದಿನದ ಸ್ಥಳೀಯ ಸಮಯದ ಕೊನೆಯ, ಸಮೀಪದ ಅಥವಾ ಮುಂದಿನ ಸಂಭವ

ಡೇಟಾ ಬಿಂದುವು ತನ್ನ ಮೂಲ ದಿನದ ಸಮಯವನ್ನು ಉಳಿಸಿಕೊಂಡು ನಿರ್ದಿಷ್ಟ ವಾರದ ದಿನಕ್ಕೆ ಸರಿಯುತ್ತದೆ.
  	]],
  	["kk"] = [[
Дерек нүктелерінің уақыт белгілерін белгілі бір апта күніндегі жергілікті уақытпен бірдей уақытқа дәл келтіреді.

- Апта күні: Мақсатты апта күні (дүйсенбіден жексенбіге дейін)
- Бағыт: Сол апта күніндегі жергілікті уақыттың соңғы, ең жақын немесе келесі кездесуі

Дерек нүктесі өзінің бастапқы тәулік уақытын сақтайды, бірақ көрсетілген апта күніне жылжиды.
  	]],
  	["km"] = [[
កែតម្រូវត្រាពេលវេលារបស់ចំណុចទិន្នន័យទៅពេលវេលាមូលដ្ឋានដដែលនៅថ្ងៃជាក់លាក់មួយក្នុងសប្ដាហ៍។

- ថ្ងៃក្នុងសប្ដាហ៍៖ ថ្ងៃគោលដៅក្នុងសប្ដាហ៍ (ចន្ទដល់អាទិត្យ)
- ទិសដៅ៖ លើកចុងក្រោយ លើកដែលជិតបំផុត ឬលើកបន្ទាប់នៃពេលវេលាមូលដ្ឋាននោះនៅថ្ងៃនោះ

ចំណុចទិន្នន័យរក្សាពេលវេលាដើមក្នុងថ្ងៃ ប៉ុន្តែផ្លាស់ទីទៅថ្ងៃដែលបានបញ្ជាក់។
  	]],
  	["ko"] = [[
데이터 포인트 타임스탬프를 특정 요일의 동일한 현지 시간으로 맞춥니다.

- 요일: 목표 요일(월요일~일요일)
- 방향: 해당 요일의 현지 시간에서 마지막, 가장 가까운 또는 다음 발생 시점

데이터 포인트는 원래의 시간을 유지하면서 지정한 요일로 이동합니다.
  	]],
  	["ky"] = [[
Маалымат чекиттеринин убакыт белгилерин белгилүү бир апта күнүндөгү ошол эле жергиликтүү убакытка тегиздейт.

- Апта күнү: Максаттуу апта күнү (дүйшөмбүдөн жекшембиге чейин)
- Багыт: Ошол апта күнүндөгү жергиликтүү убакыттын акыркы, эң жакын же кийинки учуру

Маалымат чекити өзүнүн баштапкы күндөгү убактысын сактап, көрсөтүлгөн апта күнүнө жылдырылат.
  	]],
  	["lo"] = [[
ປັບເວລາຂອງຈຸດຂໍ້ມູນໄປຫາເວລາທ້ອງຖິ່ນດຽວກັນໃນມື້ທີ່ກຳນົດ.

- ມື້ໃນອາທິດ: ມື້ເປົ້າໝາຍໃນອາທິດ (ຈັນ ຫາ ອາທິດ)
- ທິດທາງ: ຄັ້ງຫຼ້າສຸດ, ຄັ້ງທີ່ໃກ້ທີ່ສຸດ, ຫຼືຄັ້ງຖັດໄປຂອງເວລາທ້ອງຖິ່ນນັ້ນໃນມື້ດັ່ງກ່າວ

ຈຸດຂໍ້ມູນຈະຮັກສາເວລາຂອງມື້ເດີມໄວ້ ແຕ່ຈະຍ້າຍໄປຫາມື້ທີ່ກຳນົດ.
  	]],
  	["lv"] = [[
Pielāgo datu punktu laika zīmogus tam pašam vietējam laikam noteiktā nedēļas dienā.

- Nedēļas diena: mērķa nedēļas diena (no pirmdienas līdz svētdienai)
- Virziens: pēdējā, tuvākā vai nākamā šī vietējā laika iestāšanās reize attiecīgajā nedēļas dienā

Datu punkts saglabā sākotnējo dienas laiku, bet tiek pārvietots uz norādīto nedēļas dienu.
  	]],
  	["lt"] = [[
Pritraukia duomenų taškų laiko žymas prie to paties vietinio laiko konkrečią savaitės dieną.

- Savaitės diena: Tikslinė savaitės diena (nuo pirmadienio iki sekmadienio)
- Kryptis: Paskutinis, artimiausias arba kitas to vietinio laiko pasireiškimas tą savaitės dieną

Duomenų taškas išsaugo pradinį paros laiką, bet perkeliamas į nurodytą savaitės dieną.
  	]],
  	["mk"] = [[
Ги прицврстува временските печати на точките на податоци на истото локално време во одреден ден од неделата.

- Ден во неделата: Целниот ден од неделата (од понеделник до недела)
- Насока: Последна, најблиска или следна појава на тоа локално време во тој ден

Точката на податоци го задржува своето првобитно време во денот, но се преместува на зададениот ден во неделата.
  	]],
  	["ms"] = [[
Menyelaraskan cap masa titik data kepada waktu tempatan yang sama pada hari minggu tertentu.

- Hari Minggu: Hari sasaran dalam minggu (Isnin hingga Ahad)
- Arah: Kejadian terakhir, terdekat atau seterusnya bagi waktu tempatan tersebut pada hari minggu itu

Titik data mengekalkan waktu asal dalam sehari tetapi berpindah ke hari minggu yang ditentukan.
  	]],
  	["ml"] = [[
ഡാറ്റാ പോയിന്റ് timestamp-കളെ നിർദ്ദിഷ്ട ആഴ്ചാദിനത്തിലെ അതേ പ്രാദേശിക സമയത്തിലേക്ക് സ്‌നാപ്പ് ചെയ്യുന്നു.

- Weekday: ലക്ഷ്യ ആഴ്ചാദിനം (തിങ്കൾ മുതൽ ഞായർ വരെ)
- Direction: ആ ആഴ്ചാദിനത്തിലെ ആ പ്രാദേശിക സമയത്തിന്റെ അവസാനത്തെ, ഏറ്റവും അടുത്ത, അല്ലെങ്കിൽ അടുത്ത സംഭവനം

ഡാറ്റാ പോയിന്റ് അതിന്റെ യഥാർത്ഥ ദിവസത്തിലെ സമയം നിലനിർത്തുകയും നിർദ്ദിഷ്ട ആഴ്ചാദിനത്തിലേക്ക് മാറുകയും ചെയ്യും.
  	]],
  	["mr"] = [[
डेटा पॉइंटचे टाइमस्टॅम्प विशिष्ट आठवड्याच्या दिवशी त्याच स्थानिक वेळेवर स्नॅप करते.

- आठवड्याचा दिवस: आठवड्याचा लक्ष्य दिवस (सोमवार ते रविवार)
- दिशा: त्या दिवशीच्या स्थानिक वेळेची मागील, सर्वात जवळची किंवा पुढील घटना

डेटा पॉइंटची दिवसातील मूळ वेळ कायम राहते, परंतु तो निर्दिष्ट आठवड्याच्या दिवशी हलवला जातो.
  	]],
  	["mn"] = [[
Өгөгдлийн цэгийн цагийн тэмдгүүдийг тодорхой гарагт тухайн орон нутгийн ижил цагт тааруулна.

- Гараг: Долоо хоногийн зорилтот өдөр (Даваагаас Ням хүртэл)
- Чиглэл: Тухайн гарагт энэ орон нутгийн цагийн сүүлийн, хамгийн ойрын эсвэл дараагийн тохиолдол

Өгөгдлийн цэг өдрийн анхны цагаа хадгалж, заасан гараг руу шилжинэ.
  	]],
  	["ne"] = [[
डेटा बिन्दुका टाइमस्ट्याम्पलाई निर्दिष्ट हप्ताको दिनको उही स्थानीय समयमा स्न्याप गर्छ।

- हप्ताको दिन: लक्षित हप्ताको दिन (सोमबारदेखि आइतबारसम्म)
- दिशा: त्यस हप्ताको दिनमा उक्त स्थानीय समयको पछिल्लो, सबैभन्दा नजिकको वा अर्को घटना

डेटा बिन्दुले आफ्नो मूल दिनको समय कायम राख्छ तर निर्दिष्ट हप्ताको दिनमा सर्छ।
  	]],
  	["no"] = [[
Justerer tidsstemplene for datapunkter til samme lokale klokkeslett på en bestemt ukedag.

- Ukedag: Den ønskede ukedagen (mandag til søndag)
- Retning: Siste, nærmeste eller neste forekomst av dette lokale klokkeslettet på den ukedagen

Datapunktet beholder sitt opprinnelige klokkeslett, men flyttes til den angitte ukedagen.
  	]],
  	["pl"] = [[
Dopasowuje znaczniki czasu punktów danych do tej samej lokalnej godziny w określonym dniu tygodnia.

- Dzień tygodnia: Docelowy dzień tygodnia (od poniedziałku do niedzieli)
- Kierunek: Ostatnie, najbliższe lub następne wystąpienie tej lokalnej godziny w danym dniu tygodnia

Punkt danych zachowuje pierwotną porę dnia, ale zostaje przeniesiony na określony dzień tygodnia.
  	]],
  	["pt"] = [[
Ajusta os carimbos de data/hora dos pontos de dados para a mesma hora local num dia específico da semana.

- Dia da semana: O dia de destino da semana (segunda-feira a domingo)
- Direção: Última, mais próxima ou próxima ocorrência dessa hora local nesse dia da semana

O ponto de dados mantém a hora do dia original, mas passa para o dia da semana especificado.
  	]],
  	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੇ ਟਾਈਮਸਟੈਂਪ ਨੂੰ ਕਿਸੇ ਖਾਸ ਹਫ਼ਤੇ ਦੇ ਦਿਨ ਦੇ ਉਸੇ ਸਥਾਨਕ ਸਮੇਂ ’ਤੇ ਸਨੈਪ ਕਰਦਾ ਹੈ।

- ਹਫ਼ਤੇ ਦਾ ਦਿਨ: ਹਫ਼ਤੇ ਦਾ ਲਕਸ਼ਿਤ ਦਿਨ (ਸੋਮਵਾਰ ਤੋਂ ਐਤਵਾਰ ਤੱਕ)
- ਦਿਸ਼ਾ: ਉਸ ਹਫ਼ਤੇ ਦੇ ਦਿਨ ਦੇ ਉਸ ਸਥਾਨਕ ਸਮੇਂ ਦੀ ਆਖਰੀ, ਸਭ ਤੋਂ ਨੇੜਲੀ, ਜਾਂ ਅਗਲੀ ਘਟਨਾ

ਡਾਟਾ ਪੁਆਇੰਟ ਦਿਨ ਦਾ ਆਪਣਾ ਮੂਲ ਸਮਾਂ ਰੱਖਦਾ ਹੈ, ਪਰ ਨਿਰਧਾਰਤ ਹਫ਼ਤੇ ਦੇ ਦਿਨ ’ਤੇ ਚਲਾ ਜਾਂਦਾ ਹੈ।
  	]],
  	["ro"] = [[
Fixează marcajele temporale ale punctelor de date la aceeași oră locală într-o anumită zi a săptămânii.

- Ziua săptămânii: Ziua țintă a săptămânii (de luni până duminică)
- Direcție: Ultima, cea mai apropiată sau următoarea apariție a acelei ore locale în ziua respectivă

Punctul de date își păstrează ora originală, dar este mutat în ziua specificată a săptămânii.
  	]],
  	["rm"] = [[
Adatta ils timestamps dals puncts da datas al medem temp local d'ina di dad emna specific.

- Di dad emna: Il di da destinaziun da l'emna (glindesdi fin dumengia)
- Direcziun: L'ultima, la pli datiers u la proxima occurrenza da quel temp local en quella di dad emna

Il punct da datas mantegna ses temp dal di original, ma vegn spustà a la di dad emna specifitgada.
  	]],
  	["ru"] = [[
Привязывает временные метки точек данных к тому же местному времени в определённый день недели.

- День недели: Целевой день недели (с понедельника по воскресенье)
- Направление: Последнее, ближайшее или следующее вхождение этого местного времени в указанный день недели

Точка данных сохраняет исходное время суток, но перемещается на указанный день недели.
  	]],
  	["sr"] = [[
Poravnava vremenske oznake tačaka podataka sa istim lokalnim vremenom određenog dana u nedelji.

- Dan u nedelji: Ciljni dan u nedelji (od ponedeljka do nedelje)
- Smer: Poslednje, najbliže ili sledeće pojavljivanje tog lokalnog vremena na tom danu u nedelji

Tačka podataka zadržava svoje originalno vreme u danu, ali se pomera na navedeni dan u nedelji.
  	]],
  	["si"] = [[
දත්ත ලක්ෂ්‍යවල වේලා මුද්‍රා නිශ්චිත සතියේ දිනයක එම දේශීය වේලාවට ගළපයි.

- සතියේ දිනය: සතියේ ඉලක්ක දිනය (සඳුදා සිට ඉරිදා දක්වා)
- දිශාව: එම දිනයේ එම දේශීය වේලාවේ අවසන්, ආසන්නතම, හෝ ඊළඟ සිදුවීම

දත්ත ලක්ෂ්‍යය එහි මුල් දවසේ වේලාව තබාගෙන,指定 කළ සතියේ දිනයට මාරු වේ.
  	]],
  	["sk"] = [[
Prichytí časové pečiatky údajových bodov k rovnakému miestnemu času v konkrétny deň týždňa.

- Deň v týždni: Cieľový deň týždňa (pondelok až nedeľa)
- Smer: Posledný, najbližší alebo nasledujúci výskyt daného miestneho času v daný deň týždňa

Údajový bod si zachová pôvodný čas dňa, ale presunie sa na určený deň týždňa.
  	]],
  	["sl"] = [[
Prilagodi časovne žige podatkovnih točk na isti lokalni čas določenega dneva v tednu.

- Dan v tednu: Ciljni dan v tednu (od ponedeljka do nedelje)
- Smer: Zadnji, najbližji ali naslednji pojav tega lokalnega časa na ta dan v tednu

Podatkovna točka ohrani izvirni čas dneva, vendar se premakne na določen dan v tednu.
  	]],
  	["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a la misma hora local en un día de la semana específico.

- Día de la semana: El día de destino de la semana (de lunes a domingo)
- Dirección: Última, más cercana o próxima aparición de esa hora local en ese día de la semana

El punto de datos conserva su hora del día original, pero se mueve al día de la semana especificado.
  	]],
  	["sw"] = [[
Hupangilia mihuri ya muda ya nukta za data kwa muda uleule wa ndani kwenye siku maalum ya wiki.

- Siku ya Wiki: Siku inayolengwa ya wiki (Jumatatu hadi Jumapili)
- Mwelekeo: Tukio la mwisho, lililo karibu zaidi au linalofuata la muda huo wa ndani kwenye siku hiyo ya wiki

Nukta ya data huhifadhi muda wake wa awali wa siku lakini huhamishwa hadi siku maalum ya wiki.
  	]],
  	["sv"] = [[
Justerar datapunkternas tidsstämplar till samma lokala tid på en specifik veckodag.

- Veckodag: Måldagen i veckan (måndag till söndag)
- Riktning: Senaste, närmaste eller nästa förekomst av den lokala tiden på veckodagen

Datapunkten behåller sin ursprungliga tid på dagen men flyttas till den angivna veckodagen.
  	]],
  	["ta"] = [[
தரவுப் புள்ளி நேரமுத்திரைகளை குறிப்பிட்ட வாரநாளில் அதே உள்ளூர் நேரத்திற்கு பொருத்துகிறது.

- வாரநாள்: இலக்கு வாரநாள் (திங்கள் முதல் ஞாயிறு வரை)
- திசை: அந்த வாரநாளில் உள்ளூர் நேரத்தின் கடைசி, அருகிலுள்ள அல்லது அடுத்த நிகழ்வு

தரவுப் புள்ளி அதன் அசல் நாளின் நேரத்தைத் தக்கவைத்து, குறிப்பிட்ட வாரநாளுக்கு நகரும்.
  	]],
  	["te"] = [[
డేటా పాయింట్ టైమ్‌స్టాంప్‌లను నిర్దిష్ట వారపు రోజుని అదే స్థానిక సమయానికి స్నాప్ చేస్తుంది.

- వారపు రోజు: లక్ష్య వారపు రోజు (సోమవారం నుండి ఆదివారం వరకు)
- దిశ: ఆ వారపు రోజుని స్థానిక సమయానికి చివరి, సమీప లేదా తదుపరి సంభవం

డేటా పాయింట్ తన అసలు రోజులోని సమయాన్ని ఉంచుకుని, పేర్కొన్న వారపు రోజుకు మారుతుంది.
  	]],
  	["th"] = [[
ปรับเวลาประทับของจุดข้อมูลไปเป็นเวลาเดียวกันในท้องถิ่นของวันในสัปดาห์ที่ระบุ

- วันในสัปดาห์: วันปลายทางของสัปดาห์ (จันทร์ถึงอาทิตย์)
- ทิศทาง: ครั้งล่าสุด ครั้งที่ใกล้ที่สุด หรือครั้งถัดไปของเวลาท้องถิ่นนั้นในวันดังกล่าว

จุดข้อมูลจะคงเวลาเดิมของวันไว้ แต่ย้ายไปยังวันที่ระบุในสัปดาห์
  	]],
  	["tr"] = [[
Veri noktası zaman damgalarını belirli bir hafta günündeki aynı yerel saate hizalar.

- Hafta Günü: Hedef hafta günü (Pazartesi'den Pazar'a)
- Yön: O hafta günündeki yerel saatin Son, En Yakın veya Sonraki oluşumu

Veri noktası özgün günün saatini korur, ancak belirtilen hafta gününe taşınır.
  	]],
  	["uk"] = [[
Прив’язує часові мітки точок даних до того самого місцевого часу в певний день тижня.

- День тижня: Цільовий день тижня (від понеділка до неділі)
- Напрямок: Останнє, найближче або наступне входження цього місцевого часу в цей день тижня

Точка даних зберігає початковий час доби, але переміщується на вказаний день тижня.
  	]],
  	["vi"] = [[
Căn dấu thời gian của điểm dữ liệu đến cùng giờ địa phương vào một ngày cụ thể trong tuần.

- Ngày trong tuần: Ngày mục tiêu trong tuần (Thứ Hai đến Chủ Nhật)
- Hướng: Lần xuất hiện cuối cùng, gần nhất hoặc tiếp theo của giờ địa phương đó vào ngày trong tuần ấy

Điểm dữ liệu giữ nguyên giờ trong ngày ban đầu nhưng được chuyển đến ngày trong tuần đã chỉ định.
  	]],
  },

  config = {
    enum {
      id = "target_weekday",
      name = {
      	["en"] = "Weekday",
      	["af"] = "Weekdag",
      	["sq"] = "Dita e javës",
      	["am"] = "የሳምንቱ ቀን",
      	["hy"] = "Շաբաթվա օր",
      	["az"] = "Həftə günü",
      	["bn"] = "সপ্তাহের দিন",
      	["eu"] = "Asteko eguna",
      	["be"] = "Дзень тыдня",
      	["bg"] = "Ден от седмицата",
      	["my"] = "အပတ်စဉ်နေ့",
      	["ca"] = "Dia de la setmana",
      	["zh-Hans"] = "星期几",
      	["zh-Hant"] = "星期",
      	["hr"] = "Dan u tjednu",
      	["cs"] = "Den v týdnu",
      	["da"] = "Ugedag",
      	["nl"] = "Weekdag",
      	["et"] = "Nädalapäev",
      	["fil"] = "Araw ng Linggo",
      	["fi"] = "Viikonpäivä",
      	["fr"] = "Jour de la semaine",
      	["gl"] = "Día da semana",
      	["ka"] = "კვირის დღე",
      	["de"] = "Wochentag",
      	["el"] = "Ημέρα εβδομάδας",
      	["gu"] = "અઠવાડિયાનો દિવસ",
      	["hi"] = "सप्ताह का दिन",
      	["hu"] = "Hét napja",
      	["is"] = "Vikudagur",
      	["id"] = "Hari dalam Minggu",
      	["it"] = "Giorno della settimana",
      	["ja"] = "曜日",
      	["kn"] = "ವಾರದ ದಿನ",
      	["kk"] = "Апта күні",
      	["km"] = "ថ្ងៃក្នុងសប្ដាហ៍",
      	["ko"] = "요일",
      	["ky"] = "Апта күнү",
      	["lo"] = "ມື້ໃນອາທິດ",
      	["lv"] = "Nedēļas diena",
      	["lt"] = "Savaitės diena",
      	["mk"] = "Ден во неделата",
      	["ms"] = "Hari Minggu",
      	["ml"] = "ആഴ്ചാദിനം",
      	["mr"] = "आठवड्याचा दिवस",
      	["mn"] = "Гараг",
      	["ne"] = "हप्ताको दिन",
      	["no"] = "Ukedag",
      	["pl"] = "Dzień tygodnia",
      	["pt"] = "Dia da semana",
      	["pa"] = "ਹਫ਼ਤੇ ਦਾ ਦਿਨ",
      	["ro"] = "Ziua săptămânii",
      	["rm"] = "Di dad emna",
      	["ru"] = "День недели",
      	["sr"] = "Dan u nedelji",
      	["si"] = "සතියේ දිනය",
      	["sk"] = "Deň v týždni",
      	["sl"] = "Dan v tednu",
      	["es"] = "Día de la semana",
      	["sw"] = "Siku ya Wiki",
      	["sv"] = "Veckodag",
      	["ta"] = "வாரநாள்",
      	["te"] = "వారపు రోజు",
      	["th"] = "วันในสัปดาห์",
      	["tr"] = "Hafta Günü",
      	["uk"] = "День тижня",
      	["vi"] = "Ngày trong tuần",
      },
      options = { "_monday", "_tuesday", "_wednesday", "_thursday", "_friday", "_saturday", "_sunday" },
      default = "_monday",
    },
    enum {
      id = "direction",
      name = {
      	["en"] = "Direction",
      	["af"] = "Rigting",
      	["sq"] = "Drejtimi",
      	["am"] = "አቅጣጫ",
      	["hy"] = "Ուղղություն",
      	["az"] = "İstiqamət",
      	["bn"] = "দিক",
      	["eu"] = "Norabidea",
      	["be"] = "Напрамак",
      	["bg"] = "Посока",
      	["my"] = "ဦးတည်ချက်",
      	["ca"] = "Direcció",
      	["zh-Hans"] = "方向",
      	["zh-Hant"] = "方向",
      	["hr"] = "Smjer",
      	["cs"] = "Směr",
      	["da"] = "Retning",
      	["nl"] = "Richting",
      	["et"] = "Suund",
      	["fil"] = "Direksyon",
      	["fi"] = "Suunta",
      	["fr"] = "Direction",
      	["gl"] = "Dirección",
      	["ka"] = "მიმართულება",
      	["de"] = "Richtung",
      	["el"] = "Κατεύθυνση",
      	["gu"] = "દિશા",
      	["hi"] = "दिशा",
      	["hu"] = "Irány",
      	["is"] = "Stefna",
      	["id"] = "Arah",
      	["it"] = "Direzione",
      	["ja"] = "方向",
      	["kn"] = "ದಿಕ್ಕು",
      	["kk"] = "Бағыт",
      	["km"] = "ទិសដៅ",
      	["ko"] = "방향",
      	["ky"] = "Багыт",
      	["lo"] = "ທິດທາງ",
      	["lv"] = "Virziens",
      	["lt"] = "Kryptis",
      	["mk"] = "Насока",
      	["ms"] = "Arah",
      	["ml"] = "ദിശ",
      	["mr"] = "दिशा",
      	["mn"] = "Чиглэл",
      	["ne"] = "दिशा",
      	["no"] = "Retning",
      	["pl"] = "Kierunek",
      	["pt"] = "Direção",
      	["pa"] = "ਦਿਸ਼ਾ",
      	["ro"] = "Direcție",
      	["rm"] = "Direcziun",
      	["ru"] = "Направление",
      	["sr"] = "Smer",
      	["si"] = "දිශාව",
      	["sk"] = "Smer",
      	["sl"] = "Smer",
      	["es"] = "Dirección",
      	["sw"] = "Mwelekeo",
      	["sv"] = "Riktning",
      	["ta"] = "திசை",
      	["te"] = "దిశ",
      	["th"] = "ทิศทาง",
      	["tr"] = "Yön",
      	["uk"] = "Напрямок",
      	["vi"] = "Hướng",
      },
      options = { "_next", "_nearest", "_last" },
      default = "_nearest",
    },
  },

  -- Generator function
  generator = function(source, config)
    local target_weekday = config and config.target_weekday or error("target_weekday is required")
    local direction = config and config.direction or error("direction is required")

    -- Map weekday strings to numbers (Monday = 1, Sunday = 7)
    local weekday_map = {
      ["_monday"] = 1,
      ["_tuesday"] = 2,
      ["_wednesday"] = 3,
      ["_thursday"] = 4,
      ["_friday"] = 5,
      ["_saturday"] = 6,
      ["_sunday"] = 7,
    }
    local target_wday = weekday_map[target_weekday]
    if not target_wday then
      error("Invalid weekday: " .. target_weekday)
    end

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)
      local current_wday = date.wday

      -- Calculate days difference to target weekday
      local days_to_target = (target_wday - current_wday) % 7

      -- Calculate the target time on the target weekday in the same week
      -- Use the original time components from the data point
      local next_target = core.shift(data_point, core.PERIOD.DAY, days_to_target)

      local new_timestamp

      if days_to_target == 0 then
        -- Already on target weekday, no change needed
        new_timestamp = data_point
      elseif direction == "_next" then
        new_timestamp = next_target
      elseif direction == "_last" then
        new_timestamp = core.shift(next_target, core.PERIOD.WEEK, -1)
      else -- "_nearest"
        -- Find nearest occurrence of same time on target weekday
        local last_target = core.shift(next_target, core.PERIOD.WEEK, -1)
        local next_diff = math.abs(next_target.timestamp - data_point.timestamp)
        local last_diff = math.abs(data_point.timestamp - last_target.timestamp)

        if next_diff < last_diff then
          new_timestamp = next_target
        else
          new_timestamp = last_target
        end
      end

      -- Return data point with adjusted timestamp
      return {
        timestamp = new_timestamp.timestamp,
        offset = new_timestamp.offset,
        value = data_point.value,
        label = data_point.label,
        note = data_point.note,
      }
    end
  end,
}
