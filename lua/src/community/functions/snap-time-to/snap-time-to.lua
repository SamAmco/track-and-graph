-- Lua Function to snap data point timestamps to a specific time of day
-- This function adjusts timestamps to the specified time of day based on the direction (next, previous, or nearest)

local localtime = require("tng.config").localtime
local enum = require("tng.config").enum
local core = require("tng.core")

return {
  -- Configuration metadata
  id = "snap-time-to",
  version = "1.0.2",
  inputCount = 1,
  categories = { "_time" },

  title = {
  	["en"] = "Snap Time To",
  	["af"] = "Belyn Tyd met",
  	["sq"] = "Përputh kohën me",
  	["am"] = "ጊዜን ወደ አጠጋጋ",
  	["hy"] = "Ժամանակը համապատասխանեցնել",
  	["az"] = "Vaxtı ...-a uyğunlaşdır",
  	["bn"] = "সময় এতে স্ন্যাপ করুন",
  	["eu"] = "Doitu ordua hona",
  	["be"] = "Прывязаць час да",
  	["bg"] = "Привеждане на часа към",
  	["my"] = "အချိန်သို့ ချိန်ညှိရန်",
  	["ca"] = "Ajusta l’hora a",
  	["zh-Hans"] = "对齐到指定时间",
  	["zh-Hant"] = "貼齊至時間",
  	["hr"] = "Poravnaj vrijeme s",
  	["cs"] = "Přichytit čas k",
  	["da"] = "Fastlås tid til",
  	["nl"] = "Tijd afronden op",
  	["et"] = "Joonda kellaaeg",
  	["fil"] = "Iayon ang Oras sa",
  	["fi"] = "Kohdista aika",
  	["fr"] = "Aligner l’heure sur",
  	["gl"] = "Axustar a hora a",
  	["ka"] = "დროის მიმაგრება",
  	["de"] = "Zeit anpassen an",
  	["el"] = "Προσαρμογή ώρας σε",
  	["gu"] = "સમયને આ પર સ્નૅપ કરો",
  	["hi"] = "समय पर स्नैप करें",
  	["hu"] = "Idő igazítása ehhez",
  	["is"] = "Samræma tíma við",
  	["id"] = "Sesuaikan Waktu ke",
  	["it"] = "Allinea l'orario a",
  	["ja"] = "時刻にスナップ",
  	["kn"] = "ಸಮಯವನ್ನು ಇದಕ್ಕೆ ಹೊಂದಿಸಿ",
  	["kk"] = "Уақытты дәл келтіру",
  	["km"] = "កែតម្រូវពេលវេលាទៅ",
  	["ko"] = "다음 시간으로 맞추기",
  	["ky"] = "Убакытты төмөнкүгө тегиздөө",
  	["lo"] = "ປັບເວລາໄປຫາ",
  	["lv"] = "Pielāgot laiku",
  	["lt"] = "Pritraukti laiką prie",
  	["mk"] = "Прицврсти го времето на",
  	["ms"] = "Selaraskan Masa Kepada",
  	["ml"] = "സമയം ഇതിലേക്ക് സ്‌നാപ്പ് ചെയ്യുക",
  	["mr"] = "वेळ यावर स्नॅप करा",
  	["mn"] = "Цагийг дараахт тааруулах",
  	["ne"] = "समयमा स्न्याप गर्नुहोस्",
  	["no"] = "Juster tid til",
  	["pl"] = "Dopasuj czas do",
  	["pt"] = "Ajustar hora para",
  	["pa"] = "ਸਮੇਂ ਨੂੰ ਇਸ ’ਤੇ ਸਨੈਪ ਕਰੋ",
  	["ro"] = "Fixează ora la",
  	["rm"] = "Adattar il temp a",
  	["ru"] = "Привязать время к",
  	["sr"] = "Poravnaj vreme sa",
  	["si"] = "වේලාවට ගැළපීම",
  	["sk"] = "Prichytiť čas k",
  	["sl"] = "Prilagodi čas na",
  	["es"] = "Ajustar hora a",
  	["sw"] = "Pangilia Muda kwa",
  	["sv"] = "Justera tid till",
  	["ta"] = "நேரத்தை இதற்கு பொருத்து",
  	["te"] = "సమయాన్ని దీనికి స్నాప్ చేయి",
  	["th"] = "ปรับเวลาไปยัง",
  	["tr"] = "Zamanı Şuna Hizala",
  	["uk"] = "Прив’язати час до",
  	["vi"] = "Căn thời gian đến",
  },

  description = {
  	["en"] = [[
Snaps data point timestamps to a specific time of day.

- Time of Day: The target time (e.g., 09:30:00)
- Direction: Next, Previous, or Nearest occurrence of that time
  	]],
  	["af"] = [[
Belyn datapunt-tydstempels met ’n spesifieke tyd van die dag.

- Tyd van die dag: Die teikentyd (bv. 09:30:00)
- Rigting: Volgende, vorige of naaste voorkoms van daardie tyd
  	]],
  	["sq"] = [[
Përputh vulat kohore të pikave të të dhënave me një orë specifike të ditës.

- Ora e ditës: Ora e synuar (p.sh., 09:30:00)
- Drejtimi: Shfaqja e ardhshme, e mëparshme ose më e afërt e asaj ore
  	]],
  	["am"] = [[
የውሂብ ነጥቦችን የጊዜ ማህተሞች ወደ የቀኑ የተወሰነ ሰዓት ያጠጋጋል።

- የቀኑ ሰዓት፦ የታለመው ሰዓት (ለምሳሌ፦ 09:30:00)
- አቅጣጫ፦ ቀጣዩ፣ ያለፈው ወይም በጣም ቅርብ የሆነው የዚያ ሰዓት መከሰት
  	]],
  	["hy"] = [[
Տվյալակետերի ժամանակացույցերը համապատասխանեցնում է օրվա որոշակի ժամի։

- Օրվա ժամ՝ թիրախային ժամը (օրինակ՝ 09:30:00)
- Ուղղություն՝ այդ ժամի հաջորդ, նախորդ կամ ամենամոտ հանդիպումը
  	]],
  	["az"] = [[
Məlumat nöqtələrinin zaman damğalarını günün müəyyən vaxtına uyğunlaşdırır.

- Günün vaxtı: Hədəf vaxt (məsələn, 09:30:00)
- İstiqamət: Həmin vaxtın Növbəti, Əvvəlki və ya Ən yaxın baş verməsi
  	]],
  	["bn"] = [[
ডেটা পয়েন্টের টাইমস্ট্যাম্পকে দিনের নির্দিষ্ট সময়ে স্ন্যাপ করে।

- দিনের সময়: লক্ষ্য সময় (যেমন, 09:30:00)
- দিক: ওই সময়ের পরবর্তী, পূর্ববর্তী বা নিকটতম উপস্থিতি
  	]],
  	["eu"] = [[
Datu-puntuen denbora-zigiluak eguneko ordu zehatz batera doitzen ditu.

- Eguneko ordua: Helburuko ordua (adib., 09:30:00)
- Norabidea: Ordu horren hurrengo, aurreko edo hurbileneko agerraldia
  	]],
  	["be"] = [[
Прывязвае меткі часу кропак даных да пэўнага часу сутак.

- Час сутак: мэтавы час (напрыклад, 09:30:00)
- Напрамак: наступнае, папярэдняе або найбліжэйшае ўваходжанне гэтага часу
  	]],
  	["bg"] = [[
Привежда времевите отпечатъци на точките от данни към определен час от денонощието.

- Час от денонощието: Целевият час (напр. 09:30:00)
- Посока: Следващо, предишно или най-близко настъпване на този час
  	]],
  	["my"] = [[
ဒေတာအမှတ်များ၏ timestamp များကို နေ့စဉ်အချိန်တစ်ခုသို့ ချိန်ညှိသည်။

- နေ့စဉ်အချိန်: ရည်ရွယ်သည့်အချိန် (ဥပမာ၊ 09:30:00)
- ဦးတည်ချက်: ထိုအချိန်၏ နောက်တစ်ကြိမ်၊ ယခင်တစ်ကြိမ် သို့မဟုတ် အနီးဆုံးဖြစ်စဉ်
  	]],
  	["ca"] = [[
Ajusta les marques de temps dels punts de dades a una hora concreta del dia.

- Hora del dia: L’hora objectiu (p. ex., 09:30:00)
- Direcció: Ocurrència següent, anterior o més propera d’aquesta hora
  	]],
  	["zh-Hans"] = [[
将数据点时间戳对齐到一天中的特定时间。

- 时间：目标时间（例如 09:30:00）
- 方向：该时间的下一次、上一次或最近一次出现
  	]],
  	["zh-Hant"] = [[
將資料點的時間戳記貼齊至一天中的特定時間。

- 時間：目標時間（例如 09:30:00）
- 方向：該時間的下一個、上一個或最近一次出現時間
  	]],
  	["hr"] = [[
Poravnava vremenske oznake podatkovnih točaka s određenim vremenom dana.

- Vrijeme dana: Ciljano vrijeme (npr. 09:30:00)
- Smjer: Sljedeća, prethodna ili najbliža pojava tog vremena
  	]],
  	["cs"] = [[
Přichytí časová razítka datových bodů ke konkrétnímu času dne.

- Čas dne: Cílový čas (např. 09:30:00)
- Směr: Další, předchozí nebo nejbližší výskyt tohoto času
  	]],
  	["da"] = [[
Fastlåser datapunkters tidsstempler til et bestemt tidspunkt på dagen.

- Tidspunkt på dagen: Mål-tidspunktet (f.eks. 09:30:00)
- Retning: Næste, forrige eller nærmeste forekomst af dette tidspunkt
  	]],
  	["nl"] = [[
Rondt tijdstempels van gegevenspunten af op een specifiek tijdstip.

- Tijdstip: De gewenste tijd (bijv. 09:30:00)
- Richting: Volgende, vorige of dichtstbijzijnde keer dat tijdstip
  	]],
  	["et"] = [[
Joondab andmepunktide ajatemplid kindlale kellaajale.

- Kellaaeg: sihtaeg (nt 09:30:00)
- Suund: järgmine, eelmine või lähim selle aja esinemine
  	]],
  	["fil"] = [[
Ina-adjust ang mga timestamp ng data point sa isang partikular na oras ng araw.

- Time of Day: Target na oras (hal., 09:30:00)
- Direction: Susunod, nakaraan, o pinakamalapit na paglitaw ng oras na iyon
  	]],
  	["fi"] = [[
Kohdistaa datapisteiden aikaleimat tiettyyn kellonaikaan.

- Kellonaika: Kohdeaika (esim. 09:30:00)
- Suunta: Seuraava, edellinen tai lähin tämän ajan esiintymä
  	]],
  	["fr"] = [[
Aligne les horodatages des points de données sur une heure précise de la journée.

- Heure de la journée : Heure cible (par ex. 09:30:00)
- Direction : Occurrence suivante, précédente ou la plus proche de cette heure
  	]],
  	["gl"] = [[
Axusta as marcas temporais dos puntos de datos a unha hora específica do día.

- Hora do día: A hora de destino (por exemplo, 09:30:00)
- Dirección: Seguinte, anterior ou aparición máis próxima desa hora
  	]],
  	["ka"] = [[
მონაცემთა წერტილების დროის ნიშნულებს დღის კონკრეტულ დროზე ასწორებს.

- დღის დრო: სამიზნე დრო (მაგ., 09:30:00)
- მიმართულება: ამ დროის შემდეგი, წინა ან უახლოესი დადგომა
  	]],
  	["de"] = [[
Passt die Zeitstempel von Datenpunkten an eine bestimmte Tageszeit an.

- Tageszeit: Die Zielzeit (z. B. 09:30:00)
- Richtung: Nächstes, vorheriges oder nächstgelegenes Auftreten dieser Zeit
  	]],
  	["el"] = [[
Προσαρμόζει τις χρονικές σημάνσεις των σημείων δεδομένων σε μια συγκεκριμένη ώρα της ημέρας.

- Ώρα ημέρας: Η ώρα-στόχος (π.χ. 09:30:00)
- Κατεύθυνση: Επόμενη, Προηγούμενη ή Πλησιέστερη εμφάνιση αυτής της ώρας
  	]],
  	["gu"] = [[
ડેટા પોઇન્ટના ટાઇમસ્ટેમ્પ્સને દિવસના ચોક્કસ સમય પર સ્નૅપ કરે છે.

- દિવસનો સમય: લક્ષ્ય સમય (દા.ત., 09:30:00)
- દિશા: તે સમયની આગલી, પાછલી અથવા સૌથી નજીકની ઘટના
  	]],
  	["hi"] = [[
डेटा पॉइंट के टाइमस्टैम्प को दिन के किसी विशिष्ट समय पर स्नैप करता है।

- दिन का समय: लक्ष्य समय (जैसे, 09:30:00)
- दिशा: उस समय की अगली, पिछली या निकटतम घटना
  	]],
  	["hu"] = [[
Az adatpontok időbélyegét egy adott napszakra igazítja.

- Napszak: A célidő (például 09:30:00)
- Irány: Az adott idő következő, előző vagy legközelebbi előfordulása
  	]],
  	["is"] = [[
Samræmir tímamerki gagnapunkta við ákveðinn tíma dags.

- Tími dags: Markmiðs­tíminn (t.d. 09:30:00)
- Stefna: Næsta, fyrri eða næsta tímasetning miðað við nálægð
  	]],
  	["id"] = [[
Menyesuaikan stempel waktu titik data ke waktu tertentu dalam sehari.

- Waktu: Waktu target (misalnya, 09:30:00)
- Arah: Kemunculan berikutnya, sebelumnya, atau terdekat dari waktu tersebut
  	]],
  	["it"] = [[
Allinea i timestamp dei punti dati a un orario specifico della giornata.

- Ora del giorno: l'orario di destinazione (ad es. 09:30:00)
- Direzione: occorrenza successiva, precedente o più vicina di quell'orario
  	]],
  	["ja"] = [[
データポイントのタイムスタンプを、特定の時刻にスナップします。

- 時刻: 対象時刻（例: 09:30:00）
- 方向: その時刻の次、前、または最も近い出現
  	]],
  	["kn"] = [[
ಡೇಟಾ ಬಿಂದುಗಳ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್‌ಗಳನ್ನು ದಿನದ ನಿರ್ದಿಷ್ಟ ಸಮಯಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ.

- ದಿನದ ಸಮಯ: ಗುರಿ ಸಮಯ (ಉದಾ., 09:30:00)
- ದಿಕ್ಕು: ಆ ಸಮಯದ ಮುಂದಿನ, ಹಿಂದಿನ ಅಥವಾ ಸಮೀಪದ ಸಂಭವ
  	]],
  	["kk"] = [[
Дерек нүктелерінің уақыт белгілерін тәуліктің белгілі бір уақытына дәл келтіреді.

- Тәулік уақыты: Мақсатты уақыт (мысалы, 09:30:00)
- Бағыт: Сол уақыттың келесі, алдыңғы немесе ең жақын кездесуі
  	]],
  	["km"] = [[
កែតម្រូវត្រាពេលវេលារបស់ចំណុចទិន្នន័យទៅពេលវេលាជាក់លាក់មួយក្នុងថ្ងៃ។

- ពេលវេលាក្នុងថ្ងៃ៖ ពេលវេលាគោលដៅ (ឧ. 09:30:00)
- ទិសដៅ៖ លើកបន្ទាប់ លើកមុន ឬលើកដែលជិតបំផុតនៃពេលវេលានោះ
  	]],
  	["ko"] = [[
데이터 포인트 타임스탬프를 특정 시간으로 맞춥니다.

- 시간: 목표 시간(예: 09:30:00)
- 방향: 해당 시간이 다음에 나타나는 시점, 이전에 나타나는 시점 또는 가장 가까운 시점
  	]],
  	["ky"] = [[
Маалымат чекиттеринин убакыт белгилерин күндүн белгилүү бир убактысына тегиздейт.

- Күндүн убактысы: Максаттуу убакыт (мисалы, 09:30:00)
- Багыт: Ошол убакыттын кийинки, мурунку же эң жакын учуру
  	]],
  	["lo"] = [[
ປັບເວລາຂອງຈຸດຂໍ້ມູນໄປຫາເວລາສະເພາະຂອງມື້.

- ເວລາຂອງມື້: ເວລາເປົ້າໝາຍ (ເຊັ່ນ, 09:30:00)
- ທິດທາງ: ຄັ້ງຖັດໄປ, ຄັ້ງກ່ອນ, ຫຼືຄັ້ງທີ່ໃກ້ທີ່ສຸດຂອງເວລານັ້ນ
  	]],
  	["lv"] = [[
Pielāgo datu punktu laika zīmogus noteiktam dienas laikam.

- Dienas laiks: mērķa laiks (piem., 09:30:00)
- Virziens: nākamā, iepriekšējā vai tuvākā šī laika iestāšanās reize
  	]],
  	["lt"] = [[
Pritraukia duomenų taškų laiko žymas prie konkretaus paros laiko.

- Paros laikas: Tikslinis laikas (pvz., 09:30:00)
- Kryptis: Kitas, ankstesnis arba artimiausias to laiko pasireiškimas
  	]],
  	["mk"] = [[
Ги прицврстува временските печати на точките на податоци на одредено време во денот.

- Време во денот: Целното време (на пр., 09:30:00)
- Насока: Следна, претходна или најблиска појава на тоа време
  	]],
  	["ms"] = [[
Menyelaraskan cap masa titik data kepada waktu tertentu dalam sehari.

- Waktu: Waktu sasaran (contoh, 09:30:00)
- Arah: Kejadian seterusnya, sebelumnya atau terdekat bagi waktu tersebut
  	]],
  	["ml"] = [[
ഡാറ്റാ പോയിന്റ് timestamp-കളെ ദിവസത്തിലെ നിർദ്ദിഷ്ട സമയത്തിലേക്ക് സ്‌നാപ്പ് ചെയ്യുന്നു.

- Time of Day: ലക്ഷ്യ സമയം (ഉദാ., 09:30:00)
- Direction: ആ സമയത്തിന്റെ അടുത്ത, മുമ്പത്തെ, അല്ലെങ്കിൽ ഏറ്റവും അടുത്ത സംഭവനം
  	]],
  	["mr"] = [[
डेटा पॉइंटचे टाइमस्टॅम्प दिवसातील विशिष्ट वेळेवर स्नॅप करते.

- दिवसातील वेळ: लक्ष्य वेळ (उदा., 09:30:00)
- दिशा: त्या वेळेची पुढील, मागील किंवा सर्वात जवळची घटना
  	]],
  	["mn"] = [[
Өгөгдлийн цэгийн цагийн тэмдгүүдийг өдрийн тодорхой цагт тааруулна.

- Өдрийн цаг: Зорилтот цаг (жишээ нь, 09:30:00)
- Чиглэл: Тухайн цагийн дараагийн, өмнөх эсвэл хамгийн ойрын тохиолдол
  	]],
  	["ne"] = [[
डेटा बिन्दुका टाइमस्ट्याम्पलाई दिनको निर्दिष्ट समयमा स्न्याप गर्छ।

- दिनको समय: लक्षित समय (जस्तै, 09:30:00)
- दिशा: उक्त समयको अर्को, अघिल्लो वा सबैभन्दा नजिकको घटना
  	]],
  	["no"] = [[
Justerer tidsstemplene for datapunkter til et bestemt klokkeslett.

- Klokkeslett: Måltidspunktet (f.eks. 09:30:00)
- Retning: Neste, forrige eller nærmeste forekomst av dette klokkeslettet
  	]],
  	["pl"] = [[
Dopasowuje znaczniki czasu punktów danych do określonej pory dnia.

- Pora dnia: Docelowa godzina (np. 09:30:00)
- Kierunek: Następne, poprzednie lub najbliższe wystąpienie tej godziny
  	]],
  	["pt"] = [[
Ajusta os carimbos de data/hora dos pontos de dados para uma hora específica do dia.

- Hora do dia: A hora de destino (por exemplo, 09:30:00)
- Direção: Ocorrência seguinte, anterior ou mais próxima dessa hora
  	]],
  	["pa"] = [[
ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੇ ਟਾਈਮਸਟੈਂਪ ਨੂੰ ਦਿਨ ਦੇ ਕਿਸੇ ਖਾਸ ਸਮੇਂ ’ਤੇ ਸਨੈਪ ਕਰਦਾ ਹੈ।

- ਦਿਨ ਦਾ ਸਮਾਂ: ਲਕਸ਼ਿਤ ਸਮਾਂ (ਉਦਾਹਰਨ ਲਈ, 09:30:00)
- ਦਿਸ਼ਾ: ਉਸ ਸਮੇਂ ਦੀ ਅਗਲੀ, ਪਿਛਲੀ, ਜਾਂ ਸਭ ਤੋਂ ਨੇੜਲੀ ਘਟਨਾ
  	]],
  	["ro"] = [[
Fixează marcajele temporale ale punctelor de date la o anumită oră a zilei.

- Ora zilei: Ora țintă (de exemplu, 09:30:00)
- Direcție: Următoarea, precedenta sau cea mai apropiată apariție a acelei ore
  	]],
  	["rm"] = [[
Adatta ils timestamps dals puncts da datas ad in temp dal di specific.

- Temp dal di: Il temp da destinaziun (p.ex. 09:30:00)
- Direcziun: La proxima, precedenta u pli datiers occurrenza da quel temp
  	]],
  	["ru"] = [[
Привязывает временные метки точек данных к определённому времени суток.

- Время суток: Целевое время (например, 09:30:00)
- Направление: Следующее, предыдущее или ближайшее вхождение этого времени
  	]],
  	["sr"] = [[
Poravnava vremenske oznake tačaka podataka sa određenim vremenom u danu.

- Vreme u danu: Ciljno vreme (npr. 09:30:00)
- Smer: Sledeće, prethodno ili najbliže pojavljivanje tog vremena
  	]],
  	["si"] = [[
දත්ත ලක්ෂ්‍යවල වේලා මුද්‍රා නිශ්චිත දවසේ වේලාවකට ගළපයි.

- දවසේ වේලාව: ඉලක්ක වේලාව (උදා., 09:30:00)
- දිශාව: එම වේලාවේ ඊළඟ, පෙර, හෝ ආසන්නතම සිදුවීම
  	]],
  	["sk"] = [[
Prichytí časové pečiatky údajových bodov ku konkrétnemu času dňa.

- Čas dňa: Cieľový čas (napr. 09:30:00)
- Smer: Nasledujúci, predchádzajúci alebo najbližší výskyt daného času
  	]],
  	["sl"] = [[
Prilagodi časovne žige podatkovnih točk določenemu času dneva.

- Čas dneva: Ciljni čas (npr. 09:30:00)
- Smer: Naslednji, prejšnji ali najbližji pojav tega časa
  	]],
  	["es"] = [[
Ajusta las marcas de tiempo de los puntos de datos a una hora específica del día.

- Hora del día: La hora de destino (por ejemplo, 09:30:00)
- Dirección: Próxima, anterior o más cercana a la aparición de esa hora
  	]],
  	["sw"] = [[
Hupangilia mihuri ya muda ya nukta za data kwa muda maalum wa siku.

- Muda wa Siku: Muda unaolengwa (kwa mfano, 09:30:00)
- Mwelekeo: Tukio linalofuata, lililotangulia au lililo karibu zaidi la muda huo
  	]],
  	["sv"] = [[
Justerar datapunkternas tidsstämplar till en specifik tid på dagen.

- Tid på dagen: Måltiden (t.ex. 09:30:00)
- Riktning: Nästa, föregående eller närmaste förekomst av den tiden
  	]],
  	["ta"] = [[
தரவுப் புள்ளி நேரமுத்திரைகளை குறிப்பிட்ட நாளின் நேரத்திற்கு பொருத்துகிறது.

- நாளின் நேரம்: இலக்கு நேரம் (எ.கா., 09:30:00)
- திசை: அந்த நேரத்தின் அடுத்த, முந்தைய அல்லது அருகிலுள்ள நிகழ்வு
  	]],
  	["te"] = [[
డేటా పాయింట్ టైమ్‌స్టాంప్‌లను రోజులోని నిర్దిష్ట సమయానికి స్నాప్ చేస్తుంది.

- రోజులో సమయం: లక్ష్య సమయం (ఉదా., 09:30:00)
- దిశ: ఆ సమయం యొక్క తదుపరి, మునుపటి లేదా సమీప సంభవం
  	]],
  	["th"] = [[
ปรับเวลาประทับของจุดข้อมูลไปเป็นเวลาใดเวลาหนึ่งของวันที่กำหนด

- เวลาของวัน: เวลาปลายทาง (เช่น 09:30:00)
- ทิศทาง: ครั้งถัดไป ครั้งก่อนหน้า หรือครั้งที่ใกล้ที่สุดของเวลานั้น
  	]],
  	["tr"] = [[
Veri noktası zaman damgalarını belirli bir günün saatine hizalar.

- Günün Saati: Hedef saat (ör. 09:30:00)
- Yön: Bu saatin Sonraki, Önceki veya En Yakın oluşumu
  	]],
  	["uk"] = [[
Прив’язує часові мітки точок даних до певного часу доби.

- Час доби: Цільовий час (наприклад, 09:30:00)
- Напрямок: Наступне, попереднє або найближче входження цього часу
  	]],
  	["vi"] = [[
Căn dấu thời gian của điểm dữ liệu đến một thời điểm cụ thể trong ngày.

- Thời điểm trong ngày: Thời gian mục tiêu (ví dụ: 09:30:00)
- Hướng: Lần xuất hiện tiếp theo, trước đó hoặc gần nhất của thời điểm đó
  	]],
  },

  config = {
    localtime {
      id = "target_time",
      name = {
      	["en"] = "Time of Day",
      	["af"] = "Tyd van die dag",
      	["sq"] = "Ora e ditës",
      	["am"] = "የቀኑ ሰዓት",
      	["hy"] = "Օրվա ժամ",
      	["az"] = "Günün vaxtı",
      	["bn"] = "দিনের সময়",
      	["eu"] = "Eguneko ordua",
      	["be"] = "Час сутак",
      	["bg"] = "Час от денонощието",
      	["my"] = "နေ့စဉ်အချိန်",
      	["ca"] = "Hora del dia",
      	["zh-Hans"] = "时间",
      	["zh-Hant"] = "時間",
      	["hr"] = "Vrijeme dana",
      	["cs"] = "Čas dne",
      	["da"] = "Tidspunkt på dagen",
      	["nl"] = "Tijdstip",
      	["et"] = "Kellaaeg",
      	["fil"] = "Oras ng Araw",
      	["fi"] = "Kellonaika",
      	["fr"] = "Heure de la journée",
      	["gl"] = "Hora do día",
      	["ka"] = "დღის დრო",
      	["de"] = "Tageszeit",
      	["el"] = "Ώρα ημέρας",
      	["gu"] = "દિવસનો સમય",
      	["hi"] = "दिन का समय",
      	["hu"] = "Napszak",
      	["is"] = "Tími dags",
      	["id"] = "Waktu",
      	["it"] = "Ora del giorno",
      	["ja"] = "時刻",
      	["kn"] = "ದಿನದ ಸಮಯ",
      	["kk"] = "Тәулік уақыты",
      	["km"] = "ពេលវេលាក្នុងថ្ងៃ",
      	["ko"] = "시간",
      	["ky"] = "Күндүн убактысы",
      	["lo"] = "ເວລາຂອງມື້",
      	["lv"] = "Dienas laiks",
      	["lt"] = "Paros laikas",
      	["mk"] = "Време во денот",
      	["ms"] = "Waktu",
      	["ml"] = "ദിവസത്തിലെ സമയം",
      	["mr"] = "दिवसातील वेळ",
      	["mn"] = "Өдрийн цаг",
      	["ne"] = "दिनको समय",
      	["no"] = "Klokkeslett",
      	["pl"] = "Pora dnia",
      	["pt"] = "Hora do dia",
      	["pa"] = "ਦਿਨ ਦਾ ਸਮਾਂ",
      	["ro"] = "Ora zilei",
      	["rm"] = "Temp dal di",
      	["ru"] = "Время суток",
      	["sr"] = "Vreme u danu",
      	["si"] = "දවසේ වේලාව",
      	["sk"] = "Čas dňa",
      	["sl"] = "Čas dneva",
      	["es"] = "Hora del día",
      	["sw"] = "Muda wa Siku",
      	["sv"] = "Tid på dagen",
      	["ta"] = "நாளின் நேரம்",
      	["te"] = "రోజులో సమయం",
      	["th"] = "เวลาของวัน",
      	["tr"] = "Günün Saati",
      	["uk"] = "Час доби",
      	["vi"] = "Thời điểm trong ngày",
      },
      default = 9 * core.DURATION.HOUR, -- 09:00:00
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
    local target_time = config and config.target_time or error("Target time is required")
    local direction = config and config.direction or error("Direction is required")

    return function()
      local data_point = source.dp()
      if not data_point then
        return nil
      end

      -- Get the date components of the data point
      local date = core.date(data_point)

      -- Calculate the target time on the same date
      local same_day_target = core.time({
        year = date.year,
        month = date.month,
        day = date.day,
        hour = 0,
        min = 0,
        sec = 0,
        zone = date.zone
      })
      same_day_target = core.shift(same_day_target, target_time)

      local new_timestamp

      if direction == "_next" then
        -- Find next occurrence of target time
        if data_point.timestamp <= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Next day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY)
        end
      elseif direction == "_last" then
        -- Find previous occurrence of target time
        if data_point.timestamp >= same_day_target.timestamp then
          new_timestamp = same_day_target
        else
          -- Previous day
          new_timestamp = core.shift(same_day_target, core.PERIOD.DAY, -1)
        end
      else -- "_nearest"
        -- Find nearest occurrence of target time
        local other_target
        if data_point.timestamp <= same_day_target.timestamp then
          other_target = core.shift(same_day_target, core.PERIOD.DAY, -1)
        else
          other_target = core.shift(same_day_target, core.PERIOD.DAY)
        end

        local diff_same = math.abs(data_point.timestamp - same_day_target.timestamp)
        local diff_other = math.abs(data_point.timestamp - other_target.timestamp)

        if diff_same <= diff_other then
          new_timestamp = same_day_target
        else
          new_timestamp = other_target
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
