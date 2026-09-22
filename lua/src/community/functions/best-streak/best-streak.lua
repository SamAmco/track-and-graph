-- Lua Function to calculate the running best streak of values above a reset threshold

local number = require("tng.config").number

return {
    id = "best-streak",
    version = "1.0.1",
    inputCount = 1,
    categories = { "_aggregation" },
    title = {
    	["en"] = "Best Streak",
    	["af"] = "Beste Reeks",
    	["sq"] = "Seria më e mirë",
    	["am"] = "ምርጥ ተከታታይነት",
    	["hy"] = "Լավագույն շարունակական շարք",
    	["az"] = "Ən yaxşı ardıcıllıq",
    	["bn"] = "সেরা ধারাবাহিকতা",
    	["eu"] = "Boladarik onena",
    	["be"] = "Лепшая серыя",
    	["bg"] = "Най-добра серия",
    	["my"] = "အကောင်းဆုံး ဆက်တိုက်မှတ်တမ်း",
    	["ca"] = "Millor ratxa",
    	["zh-Hans"] = "最佳连续记录",
    	["zh-Hant"] = "最佳連續紀錄",
    	["hr"] = "Najbolji niz",
    	["cs"] = "Nejlepší série",
    	["da"] = "Bedste serie",
    	["nl"] = "Beste reeks",
    	["et"] = "Parim jada",
    	["fil"] = "Pinakamahusay na Sunod-sunod",
    	["fi"] = "Paras putki",
    	["fr"] = "Meilleure série",
    	["gl"] = "Mellor racha",
    	["ka"] = "საუკეთესო სერია",
    	["de"] = "Beste Serie",
    	["el"] = "Καλύτερο σερί",
    	["gu"] = "શ્રેષ્ઠ સતત શ્રેણી",
    	["hi"] = "सर्वश्रेष्ठ स्ट्रीक",
    	["hu"] = "Legjobb sorozat",
    	["is"] = "Lengsta samfellda röð",
    	["id"] = "Rangkaian Terbaik",
    	["it"] = "Serie migliore",
    	["ja"] = "最長連続記録",
    	["kn"] = "ಅತ್ಯುತ್ತಮ ಸರಣಿ",
    	["kk"] = "Ең үздік серия",
    	["km"] = "លំដាប់ជាប់ល្អបំផុត",
    	["ko"] = "최고 연속 기록",
    	["ky"] = "Эң мыкты ырааттуулук",
    	["lo"] = "ຊ່ວງຕໍ່ເນື່ອງທີ່ດີທີ່ສຸດ",
    	["lv"] = "Labākā sērija",
    	["lt"] = "Geriausia serija",
    	["mk"] = "Најдобра низа",
    	["ms"] = "Rentetan Terbaik",
    	["ml"] = "മികച്ച തുടർച്ച",
    	["mr"] = "सर्वोत्तम सलग मालिका",
    	["mn"] = "Хамгийн урт дараалал",
    	["ne"] = "उत्कृष्ट लगातार क्रम",
    	["no"] = "Lengste rekke",
    	["pl"] = "Najlepsza seria",
    	["pt"] = "Melhor sequência",
    	["pa"] = "ਸਭ ਤੋਂ ਵਧੀਆ ਲੜੀ",
    	["ro"] = "Cea mai lungă serie",
    	["rm"] = "Migliur seria",
    	["ru"] = "Лучшая серия",
    	["sr"] = "Najbolji niz",
    	["si"] = "හොඳම අඛණ්ඩ මාලාව",
    	["sk"] = "Najlepšia séria",
    	["sl"] = "Najboljši niz",
    	["es"] = "Mejor racha",
    	["sw"] = "Mfululizo Bora",
    	["sv"] = "Bästa svit",
    	["ta"] = "சிறந்த தொடர்ச்சி",
    	["te"] = "అత్యుత్తమ వరుస",
    	["th"] = "สถิติช่วงต่อเนื่องที่ดีที่สุด",
    	["tr"] = "En İyi Seri",
    	["uk"] = "Найкраща серія",
    	["vi"] = "Chuỗi liên tiếp tốt nhất",
    },
    description = {
    	["en"] = [[
Calculates the best streak seen so far. A streak increases by one for each consecutive data point with a value greater than the reset threshold. Values less than or equal to the reset threshold reset the current streak to zero.

This function emits one output data point for each input data point, preserving the original timestamp, label, note, and offset. The output value is the best streak seen up to that point in time.

Configuration:
- **Reset Threshold**: Values less than or equal to this threshold reset the current streak. Defaults to 0.
    	]],
    	["af"] = [[
Bereken die beste reeks tot dusver. ’n Reeks neem met een toe vir elke opeenvolgende datapunt met ’n waarde groter as die terugsteldrempel. Waardes kleiner as of gelyk aan die terugsteldrempel stel die huidige reeks op nul terug.

Hierdie funksie lewer een uitvoerdatapunt vir elke invoerdatapunt, met behoud van die oorspronklike tydstempel, etiket, nota en verskuiwing. Die uitvoerwaarde is die beste reeks wat tot op daardie tydstip gesien is.

Konfigurasie:
- **Terugsteldrempel**: Waardes kleiner as of gelyk aan hierdie drempel stel die huidige reeks terug. Verstek is 0.
    	]],
    	["sq"] = [[
Llogarit serinë më të mirë të parë deri tani. Një seri rritet me një për çdo pikë të njëpasnjëshme të të dhënave me vlerë më të madhe se pragu i rivendosjes. Vlerat më të vogla ose të barabarta me pragun e rivendosjes e rivendosin serinë aktuale në zero.

Ky funksion nxjerr një pikë të dhënash për çdo pikë hyrëse, duke ruajtur vulën kohore, etiketën, shënimin dhe zhvendosjen origjinale. Vlera dalëse është seria më e mirë e parë deri në atë moment.

Konfigurimi:
- **Pragu i rivendosjes**: Vlerat më të vogla ose të barabarta me këtë prag e rivendosin serinë aktuale. Parazgjedhja është 0.
    	]],
    	["am"] = [[
እስካሁን የታየውን ምርጥ ተከታታይነት ያሰላል። ከመነሻ ገደቡ የሚበልጥ ዋጋ ላለው ተከታታይ የውሂብ ነጥብ ቁጥሩ በአንድ ይጨምራል። ከመነሻ ገደቡ ያነሱ ወይም እኩል የሆኑ ዋጋዎች የአሁኑን ተከታታይነት ወደ ዜሮ ይመልሳሉ።

ይህ ተግባር ለእያንዳንዱ የግቤት የውሂብ ነጥብ አንድ የውጤት የውሂብ ነጥብ ያወጣል፤ የመጀመሪያውን የጊዜ ማህተም፣ መለያ፣ ማስታወሻ እና ማካካሻ ይጠብቃል። የውጤቱ ዋጋ እስከዚያ ጊዜ ድረስ የታየው ምርጥ ተከታታይነት ነው።

ውቅር፦
- **የመነሻ ገደብ**፦ ከዚህ ገደብ ያነሱ ወይም እኩል የሆኑ ዋጋዎች የአሁኑን ተከታታይነት ይያስቆማሉ። ነባሪው 0 ነው።
    	]],
    	["hy"] = [[
Հաշվում է մինչ այժմ գրանցված լավագույն շարունակական շարքը։ Շարքը յուրաքանչյուր հաջորդական տվյալակետի համար ավելանում է մեկով, եթե արժեքը վերակայման շեմից մեծ է։ Վերակայման շեմից փոքր կամ հավասար արժեքները ընթացիկ շարքը զրոյացնում են։

Այս ֆունկցիան յուրաքանչյուր մուտքային տվյալակետի համար ստեղծում է մեկ ելքային տվյալակետ՝ պահպանելով սկզբնական ժամանակացույցը, պիտակը, նշումը և շեղումը։ Ելքային արժեքը մինչև տվյալ պահը գրանցված լավագույն շարքն է։

Կազմաձևում՝
- **Վերակայման շեմ**․ այս շեմից փոքր կամ հավասար արժեքները վերակայում են ընթացիկ շարքը։ Կանխադրվածը՝ 0։
    	]],
    	["az"] = [[
İndiyədək qeydə alınmış ən yaxşı ardıcıllığı hesablayır. Sıfırlama həddindən böyük qiymətə malik hər ardıcıl məlumat nöqtəsi üçün ardıcıllıq bir vahid artır. Sıfırlama həddindən kiçik və ya ona bərabər qiymətlər cari ardıcıllığı sıfıra qaytarır.

Bu funksiya hər giriş məlumat nöqtəsi üçün bir çıxış məlumat nöqtəsi yaradır və orijinal zaman damğasını, etiketi, qeydi və ofseti qoruyur. Çıxış qiyməti həmin vaxta qədər qeydə alınmış ən yaxşı ardıcıllıqdır.

Konfiqurasiya:
- **Sıfırlama həddi**: Bu həddən kiçik və ya ona bərabər qiymətlər cari ardıcıllığı sıfırlayır. Standart olaraq 0.
    	]],
    	["bn"] = [[
এখন পর্যন্ত দেখা সেরা ধারাবাহিকতা গণনা করে। রিসেট সীমার চেয়ে বেশি মানের প্রতিটি ধারাবাহিক ডেটা পয়েন্টের জন্য ধারাবাহিকতা এক করে বাড়ে। রিসেট সীমার সমান বা কম মান বর্তমান ধারাবাহিকতাকে শূন্যে ফিরিয়ে দেয়।

প্রতিটি ইনপুট ডেটা পয়েন্টের জন্য এই ফাংশনটি একটি আউটপুট ডেটা পয়েন্ট তৈরি করে এবং মূল টাইমস্ট্যাম্প, লেবেল, নোট ও অফসেট অপরিবর্তিত রাখে। আউটপুটের মান হলো ওই সময় পর্যন্ত দেখা সেরা ধারাবাহিকতা।

কনফিগারেশন:
- **রিসেট সীমা**: এই সীমার সমান বা কম মান বর্তমান ধারাবাহিকতা রিসেট করে। ডিফল্ট 0।
    	]],
    	["eu"] = [[
Orain arteko boladarik onena kalkulatzen du. Bolada bat bat handitzen da, berrezartze-atalasea baino balio handiagoa duen datu-puntu jarraitu bakoitzeko. Berrezartze-atalasea baino txikiagoak edo berdinak diren balioek uneko bolada zerora berrezartzen dute.

Funtzio honek irteerako datu-puntu bat sortzen du sarrerako datu-puntu bakoitzeko, jatorrizko denbora-zigilua, etiketa, oharra eta desplazamendua mantenduz. Irteerako balioa une horretara arteko boladarik onena da.

Konfigurazioa:
- **Berrezartze-atalasea**: Atalasea baino txikiagoak edo berdinak diren balioek uneko bolada berrezartzen dute. Lehenetsia: 0.
    	]],
    	["be"] = [[
Вылічвае найлепшую серыю на дадзены момант. Серыя павялічваецца на адзінку за кожную паслядоўную кропку даных са значэннем, большым за парог скіду. Значэнні, меншыя або роўныя парогу скіду, скідаюць бягучую серыю да нуля.

Функцыя стварае адну выходную кропку даных для кожнай уваходнай, захоўваючы зыходныя метку часу, ярлык, заўвагу і зрух. Выходнае значэнне — найлепшая серыя да гэтага моманту.

Канфігурацыя:
- **Парог скіду**: значэнні, меншыя або роўныя гэтаму парогу, скідаюць бягучую серыю. Па змаўчанні — 0.
    	]],
    	["bg"] = [[
Изчислява най-добрата серия до момента. Серията се увеличава с едно за всяка последователна точка от данни със стойност над прага за нулиране. Стойности, равни или по-малки от прага за нулиране, нулират текущата серия.

Тази функция генерира една изходна точка от данни за всяка входна точка, като запазва оригиналните времеви отпечатък, етикет, бележка и отместване. Изходната стойност е най-добрата серия до съответния момент.

Конфигурация:
- **Праг за нулиране**: Стойности, равни или по-малки от този праг, нулират текущата серия. По подразбиране: 0.
    	]],
    	["my"] = [[
ယခုအချိန်အထိ တွေ့ရှိထားသော အကောင်းဆုံး ဆက်တိုက်မှတ်တမ်းကို တွက်ချက်သည်။ ပြန်လည်သတ်မှတ်ကန့်သတ်ချက်ထက် ကြီးသော တန်ဖိုးရှိ ဒေတာမှတ်တစ်ခုစီအတွက် ဆက်တိုက်မှတ်တမ်းသည် ၁ တိုးသည်။ ပြန်လည်သတ်မှတ်ကန့်သတ်ချက်ထက် ငယ်သော သို့မဟုတ် တူညီသော တန်ဖိုးများက လက်ရှိဆက်တိုက်မှတ်တမ်းကို သုညသို့ ပြန်သတ်မှတ်သည်။

ဤလုပ်ဆောင်ချက်သည် ထည့်သွင်းဒေတာမှတ်တစ်ခုစီအတွက် ထွက်ဒေတာမှတ်တစ်ခု ထုတ်ပေးပြီး မူရင်းအချိန်တံဆိပ်၊ အညွှန်း၊ မှတ်စုနှင့် အော့ဖ်ဆက်တို့ကို ထိန်းသိမ်းသည်။ ထွက်တန်ဖိုးသည် ထိုအချိန်အထိ တွေ့ရှိထားသော အကောင်းဆုံး ဆက်တိုက်မှတ်တမ်းဖြစ်သည်။

ဖွဲ့စည်းမှု:
- **ပြန်လည်သတ်မှတ်ကန့်သတ်ချက်**: ဤကန့်သတ်ချက်ထက် ငယ်သော သို့မဟုတ် တူညီသော တန်ဖိုးများက လက်ရှိဆက်တိုက်မှတ်တမ်းကို ပြန်လည်သတ်မှတ်သည်။ မူလတန်ဖိုးမှာ ၀ ဖြစ်သည်။
    	]],
    	["ca"] = [[
Calcula la millor ratxa observada fins ara. La ratxa augmenta en un per cada punt de dades consecutiu amb un valor superior al llindar de reinici. Els valors iguals o inferiors al llindar de reinici restableixen la ratxa actual a zero.

Aquesta funció emet un punt de dades de sortida per cada punt de dades d’entrada i conserva la marca de temps, l’etiqueta, la nota i el desplaçament originals. El valor de sortida és la millor ratxa observada fins a aquell moment.

Configuració:
- **Llindar de reinici**: Els valors iguals o inferiors a aquest llindar restableixen la ratxa actual. Per defecte, 0.
    	]],
    	["zh-Hans"] = [[
计算截至目前的最佳连续记录。每当连续数据点的值大于重置阈值时，连续记录增加 1。小于或等于重置阈值的值会将当前连续记录重置为零。

此函数会为每个输入数据点输出一个数据点，并保留原始时间戳、标签、备注和偏移量。输出值是截至该时间点的最佳连续记录。

配置：
- **重置阈值**：小于或等于此阈值的值会重置当前连续记录。默认为 0。
    	]],
    	["zh-Hant"] = [[
計算截至目前為止的最佳連續紀錄。每個值大於重設閾值的連續資料點會使連續紀錄增加一。小於或等於重設閾值的值會將目前的連續紀錄重設為零。

此函式會為每個輸入資料點輸出一個資料點，保留原始的時間戳記、標籤、備註和偏移量。輸出值是截至該時間點為止的最佳連續紀錄。

設定：
- **重設閾值**：小於或等於此閾值的值會重設目前的連續紀錄。預設為 0。
    	]],
    	["hr"] = [[
Izračunava najbolji dosad zabilježeni niz. Niz se povećava za jedan za svaku uzastopnu podatkovnu točku s vrijednošću većom od praga za resetiranje. Vrijednosti manje ili jednake pragu za resetiranje vraćaju trenutni niz na nulu.

Ova funkcija daje jednu izlaznu podatkovnu točku za svaku ulaznu podatkovnu točku, uz očuvanje izvorne vremenske oznake, oznake, bilješke i pomaka. Izlazna vrijednost najbolji je niz zabilježen do tog trenutka.

Konfiguracija:
- **Prag za resetiranje**: Vrijednosti manje ili jednake ovom pragu vraćaju trenutni niz na nulu. Zadano je 0.
    	]],
    	["cs"] = [[
Vypočítá dosud nejlepší sérii. Série se prodlouží o jedna za každý po sobě jdoucí datový bod s hodnotou vyšší než práh resetování. Hodnoty menší nebo rovné prahu resetování vynulují aktuální sérii.

Tato funkce vytvoří jeden výstupní datový bod pro každý vstupní datový bod a zachová původní časové razítko, štítek, poznámku a posun. Výstupní hodnota je nejlepší série dosažená do daného okamžiku.

Konfigurace:
- **Práh resetování**: Hodnoty menší nebo rovné tomuto prahu vynulují aktuální sérii. Výchozí hodnota je 0.
    	]],
    	["da"] = [[
Beregner den bedste serie indtil videre. En serie øges med én for hvert fortløbende datapunkt med en værdi over nulstillingsgrænsen. Værdier, der er mindre end eller lig med nulstillingsgrænsen, nulstiller den aktuelle serie til nul.

Denne funktion udsender ét outputdatapunkt for hvert inputdatapunkt og bevarer det oprindelige tidsstempel, den oprindelige etiket, note og forskydning. Outputværdien er den bedste serie indtil det pågældende tidspunkt.

Konfiguration:
- **Nulstillingsgrænse**: Værdier, der er mindre end eller lig med denne grænse, nulstiller den aktuelle serie. Standardværdien er 0.
    	]],
    	["nl"] = [[
Berekent de beste reeks tot nu toe. Een reeks neemt met één toe voor elk opeenvolgend gegevenspunt met een waarde groter dan de resetdrempel. Waarden kleiner dan of gelijk aan de resetdrempel zetten de huidige reeks terug op nul.

Deze functie genereert één uitvoergegevenspunt voor elk invoergegevenspunt en behoudt de oorspronkelijke tijdstempel, het label, de notitie en de verschuiving. De uitvoerwaarde is de beste reeks tot dat moment.

Configuratie:
- **Resetdrempel**: Waarden kleiner dan of gelijk aan deze drempel zetten de huidige reeks terug. Standaard ingesteld op 0.
    	]],
    	["et"] = [[
Arvutab seni saavutatud parima jada. Jada pikeneb ühe võrra iga järjestikuse andmepunkti korral, mille väärtus on lähtestamislävest suurem. Lävest väiksemad või sellega võrdsed väärtused lähtestavad praeguse jada nulli.

See funktsioon väljastab iga sisendandmepunkti kohta ühe väljundandmepunkti, säilitades algse ajatempli, sildi, märkme ja nihke. Väljundväärtus on selle hetkeni saavutatud parim jada.

Seadistus:
- **Lähtestamislävi**: sellest lävest väiksemad või sellega võrdsed väärtused lähtestavad praeguse jada. Vaikimisi 0.
    	]],
    	["fil"] = [[
Kinakalkula ang pinakamahabang sunod-sunod na nakita hanggang ngayon. Nadadagdagan ng isa ang streak sa bawat magkakasunod na data point na may halagang higit sa reset threshold. Nire-reset sa zero ang kasalukuyang streak kapag ang halaga ay mas mababa o katumbas ng reset threshold.

Naglalabas ang function na ito ng isang output data point para sa bawat input data point, habang pinapanatili ang orihinal na timestamp, label, note, at offset. Ang output value ay ang pinakamahabang streak hanggang sa puntong iyon.

Configuration:
- **Reset Threshold**: Nire-reset ng mga halagang mas mababa o katumbas ng threshold na ito ang kasalukuyang streak. Default ay 0.
    	]],
    	["fi"] = [[
Laskee tähän mennessä pisimmän putken. Putki kasvaa yhdellä jokaisesta peräkkäisestä datapisteestä, jonka arvo ylittää nollauskynnysarvon. Nollauskynnysarvoa pienemmät tai sen kanssa yhtä suuret arvot nollaavat nykyisen putken.

Tämä funktio tuottaa yhden tuloksena syntyvän datapisteen jokaista syötedatapistettä kohden ja säilyttää alkuperäisen aikaleiman, selitteen, muistiinpanon ja siirtymän. Tuloksen arvo on siihen ajankohtaan mennessä havaittu pisin putki.

Määritys:
- **Nollauskynnysarvo**: Tätä kynnysarvoa pienemmät tai sen kanssa yhtä suuret arvot nollaavat nykyisen putken. Oletus on 0.
    	]],
    	["fr"] = [[
Calcule la meilleure série obtenue jusqu’à présent. Une série augmente de un pour chaque point de données consécutif dont la valeur est supérieure au seuil de réinitialisation. Les valeurs inférieures ou égales au seuil de réinitialisation remettent la série actuelle à zéro.

Cette fonction produit un point de données pour chaque point d’entrée, en conservant l’horodatage, le libellé, la note et le décalage d’origine. La valeur produite correspond à la meilleure série obtenue jusqu’à ce moment.

Configuration :
- **Seuil de réinitialisation** : Les valeurs inférieures ou égales à ce seuil réinitialisent la série actuelle. Valeur par défaut : 0.
    	]],
    	["gl"] = [[
Calcula a mellor racha rexistrada ata o momento. A racha aumenta en un por cada punto de datos consecutivo cun valor superior ao limiar de reinicio. Os valores inferiores ou iguais ao limiar de reinicio restablecen a racha actual a cero.

Esta función emite un punto de datos de saída por cada punto de datos de entrada, conservando o momento, a etiqueta, a nota e o desprazamento orixinais. O valor de saída é a mellor racha rexistrada ata ese momento.

Configuración:
- **Limiar de reinicio**: Os valores inferiores ou iguais a este limiar restablecen a racha actual. O valor predeterminado é 0.
    	]],
    	["ka"] = [[
ითვლის აქამდე დაფიქსირებულ საუკეთესო სერიას. სერია ერთით იზრდება ყოველი თანმიმდევრული მონაცემის წერტილისთვის, რომლის მნიშვნელობა აღდგენის ზღვარზე მეტია. ზღვარზე ნაკლები ან ტოლი მნიშვნელობები მიმდინარე სერიას ნულზე აბრუნებს.

ეს ფუნქცია თითოეული შემავალი მონაცემის წერტილისთვის გამოიტანს ერთ მონაცემის წერტილს და შეინარჩუნებს თავდაპირველ დროის ნიშნულს, იარლიყს, შენიშვნასა და წანაცვლებას. გამოტანილი მნიშვნელობა არის იმ მომენტამდე დაფიქსირებული საუკეთესო სერია.

კონფიგურაცია:
- **აღდგენის ზღვარი**: ამ ზღვარზე ნაკლები ან ტოლი მნიშვნელობები მიმდინარე სერიას აღადგენს. ნაგულისხმევია 0.
    	]],
    	["de"] = [[
Berechnet die bisher beste Serie. Eine Serie erhöht sich für jeden aufeinanderfolgenden Datenpunkt mit einem Wert über dem Rücksetzschwellenwert um eins. Werte kleiner oder gleich dem Rücksetzschwellenwert setzen die aktuelle Serie auf null zurück.

Diese Funktion erzeugt für jeden Eingabedatenpunkt einen Ausgabedatenpunkt und behält Zeitstempel, Label, Notiz und Offset bei. Der Ausgabewert ist die bis zu diesem Zeitpunkt beste Serie.

Konfiguration:
- **Rücksetzschwellenwert**: Werte kleiner oder gleich diesem Schwellenwert setzen die aktuelle Serie zurück. Standardwert ist 0.
    	]],
    	["el"] = [[
Υπολογίζει το καλύτερο σερί μέχρι στιγμής. Το σερί αυξάνεται κατά ένα για κάθε διαδοχικό σημείο δεδομένων με τιμή μεγαλύτερη από το όριο επαναφοράς. Τιμές μικρότερες ή ίσες με το όριο επαναφοράς μηδενίζουν το τρέχον σερί.

Η συνάρτηση παράγει ένα σημείο δεδομένων εξόδου για κάθε σημείο δεδομένων εισόδου, διατηρώντας την αρχική χρονική σήμανση, την ετικέτα, τη σημείωση και τη μετατόπιση.

Διαμόρφωση:
- **Όριο επαναφοράς**: Τιμές μικρότερες ή ίσες με αυτό το όριο μηδενίζουν το τρέχον σερί. Προεπιλογή: 0.
    	]],
    	["gu"] = [[
અત્યાર સુધીની શ્રેષ્ઠ સતત શ્રેણીની ગણતરી કરે છે. મર્યાદા કરતાં વધુ મૂલ્ય ધરાવતા દરેક સતત ડેટા પોઇન્ટ માટે શ્રેણી એકથી વધે છે. મર્યાદા કરતાં ઓછાં અથવા બરાબર મૂલ્યો વર્તમાન શ્રેણીને શૂન્ય પર ફરીથી સેટ કરે છે.

આ ફંક્શન દરેક ઇનપુટ ડેટા પોઇન્ટ માટે એક આઉટપુટ ડેટા પોઇન્ટ બનાવે છે અને મૂળ ટાઇમસ્ટેમ્પ, લેબલ, નોંધ અને ઑફસેટ જાળવે છે. આઉટપુટ મૂલ્ય તે સમયબિંદુ સુધીની શ્રેષ્ઠ સતત શ્રેણી છે.

ગોઠવણી:
- **રીસેટ મર્યાદા**: આ મર્યાદા કરતાં ઓછાં અથવા બરાબર મૂલ્યો વર્તમાન શ્રેણીને રીસેટ કરે છે. ડિફૉલ્ટ 0 છે.
    	]],
    	["hi"] = [[
अब तक की सर्वश्रेष्ठ स्ट्रीक की गणना करता है। रीसेट थ्रेशोल्ड से अधिक मान वाले प्रत्येक लगातार डेटा पॉइंट के लिए स्ट्रीक एक बढ़ती है। रीसेट थ्रेशोल्ड से कम या उसके बराबर मान वर्तमान स्ट्रीक को शून्य पर रीसेट कर देते हैं।

यह फ़ंक्शन प्रत्येक इनपुट डेटा पॉइंट के लिए एक आउटपुट डेटा पॉइंट देता है और मूल टाइमस्टैम्प, लेबल, नोट और ऑफ़सेट को बनाए रखता है। आउटपुट मान उस समय तक की सर्वश्रेष्ठ स्ट्रीक होती है।

कॉन्फ़िगरेशन:
- **रीसेट थ्रेशोल्ड**: इस थ्रेशोल्ड से कम या उसके बराबर मान वर्तमान स्ट्रीक को रीसेट करते हैं। डिफ़ॉल्ट 0 है।
    	]],
    	["hu"] = [[
Kiszámítja az eddig elért legjobb sorozatot. A sorozat eggyel nő minden olyan egymást követő adatponttal, amelynek értéke nagyobb a visszaállítási küszöbnél. A küszöbnél kisebb vagy azzal egyenlő értékek nullára állítják az aktuális sorozatot.

Ez a függvény minden bemeneti adatponthoz egy kimeneti adatpontot ad ki, megőrizve az eredeti időbélyeget, címkét, megjegyzést és eltolást. A kimeneti érték az adott időpontig elért legjobb sorozat.

Konfiguráció:
- **Visszaállítási küszöb**: Az ennél kisebb vagy azzal egyenlő értékek visszaállítják az aktuális sorozatot. Alapértelmezés szerint 0.
    	]],
    	["is"] = [[
Reiknar út lengstu samfelldu röðina hingað til. Röðin lengist um einn fyrir hvern samfelldan gagnapunkt með gildi yfir endurstillingarmörkunum. Gildi sem eru minni en eða jöfn endurstillingarmörkunum stilla núverandi röð aftur á núll.

Aðgerðin gefur frá sér einn úttaksgagnapunkt fyrir hvern innkomandi gagnapunkt og varðveitir upphaflegt tímamerki, merki, athugasemd og hliðrun. Úttaksgildið er lengsta samfellda röðin fram að þeim tímapunkti.

Stillingar:
- **Endurstillingarmörk**: Gildi sem eru minni en eða jöfn þessum mörkum stilla núverandi röð aftur á núll. Sjálfgefið er 0.
    	]],
    	["id"] = [[
Menghitung rangkaian terbaik yang terlihat sejauh ini. Rangkaian bertambah satu untuk setiap titik data berurutan dengan nilai lebih besar dari ambang reset. Nilai yang kurang dari atau sama dengan ambang reset mengatur ulang rangkaian saat ini menjadi nol.

Fungsi ini menghasilkan satu titik data keluaran untuk setiap titik data masukan, dengan mempertahankan stempel waktu, label, catatan, dan offset asli. Nilai keluaran adalah rangkaian terbaik yang terlihat hingga titik waktu tersebut.

Konfigurasi:
- **Ambang Reset**: Nilai yang kurang dari atau sama dengan ambang ini akan mengatur ulang rangkaian saat ini. Default-nya 0.
    	]],
    	["it"] = [[
Calcola la serie migliore registrata finora. Una serie aumenta di uno per ogni punto dati consecutivo con un valore superiore alla soglia di reimpostazione. I valori inferiori o uguali alla soglia di reimpostazione azzerano la serie corrente.

Questa funzione emette un punto dati di output per ogni punto dati di input, mantenendo il timestamp, l'etichetta, la nota e l'offset originali. Il valore di output è la serie migliore registrata fino a quel momento.

Configurazione:
- **Soglia di reimpostazione**: i valori inferiori o uguali a questa soglia azzerano la serie corrente. Il valore predefinito è 0.
    	]],
    	["ja"] = [[
これまでの最長連続記録を計算します。しきい値を超える値のデータポイントが連続するたびに、連続記録が1増加します。しきい値以下の値があると、現在の連続記録は0にリセットされます。

この関数は、入力データポイントごとに1つの出力データポイントを生成し、元のタイムスタンプ、ラベル、メモ、オフセットを保持します。出力値は、その時点までの最長連続記録です。

設定:
- **リセットしきい値**: このしきい値以下の値で現在の連続記録をリセットします。デフォルトは0です。
    	]],
    	["kn"] = [[
ಇಲ್ಲಿಯವರೆಗೆ ಕಂಡುಬಂದ ಅತ್ಯುತ್ತಮ ಸರಣಿಯನ್ನು ಲೆಕ್ಕಹಾಕುತ್ತದೆ. ಮರುಹೊಂದಿಸುವ ಮಿತಿಗಿಂತ ಹೆಚ್ಚಿನ ಮೌಲ್ಯವಿರುವ ಪ್ರತಿಯೊಂದು ಸತತ ಡೇಟಾ ಬಿಂದುವಿಗೆ ಸರಣಿ ಒಂದರಿಂದ ಹೆಚ್ಚುತ್ತದೆ. ಮಿತಿಗಿಂತ ಕಡಿಮೆ ಅಥವಾ ಸಮನಾದ ಮೌಲ್ಯಗಳು ಪ್ರಸ್ತುತ ಸರಣಿಯನ್ನು ಶೂನ್ಯಕ್ಕೆ ಮರುಹೊಂದಿಸುತ್ತವೆ.

ಈ ಫಂಕ್ಷನ್ ಪ್ರತಿ ಇನ್‌ಪುಟ್ ಡೇಟಾ ಬಿಂದುವಿಗೆ ಒಂದು ಔಟ್‌ಪುಟ್ ಡೇಟಾ ಬಿಂದುವನ್ನು ನೀಡುತ್ತದೆ ಮತ್ತು ಮೂಲ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್, ಲೇಬಲ್, ಟಿಪ್ಪಣಿ ಮತ್ತು ಆಫ್‌ಸೆಟ್ ಅನ್ನು ಉಳಿಸುತ್ತದೆ. ಔಟ್‌ಪುಟ್ ಮೌಲ್ಯವು ಆ ಸಮಯದವರೆಗೆ ಕಂಡುಬಂದ ಅತ್ಯುತ್ತಮ ಸರಣಿಯಾಗಿದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಮರುಹೊಂದಿಸುವ ಮಿತಿ**: ಈ ಮಿತಿಗಿಂತ ಕಡಿಮೆ ಅಥವಾ ಸಮನಾದ ಮೌಲ್ಯಗಳು ಪ್ರಸ್ತುತ ಸರಣಿಯನ್ನು ಮರುಹೊಂದಿಸುತ್ತವೆ. ಡೀಫಾಲ್ಟ್ 0.
    	]],
    	["kk"] = [[
Осы уақытқа дейінгі ең үздік серияны есептейді. Шек мәнінен жоғары әр қатарынан келетін дерек нүктесі серияны біреуге арттырады. Шекке тең немесе одан төмен мәндер ағымдағы серияны нөлге түсіреді.

Бұл функция әр кіріс дерек нүктесіне бір шығыс дерек нүктесін шығарып, бастапқы уақыт белгісін, жапсырмасын, ескертпесін және ығысуын сақтайды. Шығыс мәні — сол уақытқа дейінгі ең үздік серия.

Конфигурация:
- **Қалпына келтіру шегі**: Осы шекке тең немесе одан төмен мәндер ағымдағы серияны қалпына келтіреді. Әдепкі мәні — 0.
    	]],
    	["km"] = [[
គណនាលំដាប់ជាប់ល្អបំផុតរហូតមកដល់ពេលនេះ។ លំដាប់ជាប់កើនឡើងមួយសម្រាប់ចំណុចទិន្នន័យជាប់គ្នានីមួយៗដែលមានតម្លៃធំជាងកម្រិតកំណត់ឡើងវិញ។ តម្លៃតូចជាង ឬស្មើកម្រិតកំណត់ឡើងវិញ នឹងកំណត់លំដាប់ជាប់បច្ចុប្បន្នទៅសូន្យ។

អនុគមន៍នេះបញ្ចេញចំណុចទិន្នន័យលទ្ធផលមួយសម្រាប់ចំណុចទិន្នន័យបញ្ចូលនីមួយៗ ដោយរក្សាត្រាពេលវេលា ស្លាក ចំណាំ និងអុហ្វសិតដើម។ តម្លៃលទ្ធផលគឺលំដាប់ជាប់ល្អបំផុតរហូតដល់ពេលនោះ។

ការកំណត់៖
- **កម្រិតកំណត់ឡើងវិញ**៖ តម្លៃតូចជាង ឬស្មើកម្រិតនេះ នឹងកំណត់លំដាប់ជាប់បច្ចុប្បន្នឡើងវិញ។ លំនាំដើមគឺ 0។
    	]],
    	["ko"] = [[
현재까지의 최고 연속 기록을 계산합니다. 재설정 임계값보다 큰 값이 연속으로 입력될 때마다 연속 기록이 1씩 증가합니다. 재설정 임계값 이하의 값이 입력되면 현재 연속 기록이 0으로 재설정됩니다.

각 입력 데이터 포인트마다 하나의 출력 데이터 포인트를 생성하며, 원래의 타임스탬프, 라벨, 메모, 오프셋을 유지합니다. 출력 값은 해당 시점까지의 최고 연속 기록입니다.

구성:
- **재설정 임계값**: 이 임계값 이하의 값이 현재 연속 기록을 재설정합니다. 기본값은 0입니다.
    	]],
    	["ky"] = [[
Ушул убакка чейинки эң мыкты ырааттуулукту эсептейт. Чектен жогору мааниге ээ болгон ар бир удаалаш маалымат чекити үчүн ырааттуулук бирге өсөт. Чекке барабар же андан төмөн маанилер учурдагы ырааттуулукту нөлгө түшүрөт.

Бул функция ар бир кириш маалымат чекити үчүн бир чыгаруу маалымат чекитин түзүп, баштапкы убакыт белгисин, энбелгисин, эскертмесин жана жылышын сактайт. Чыгуу мааниси ошол учурга чейинки эң мыкты ырааттуулук болот.

Тууралоо:
- **Баштапкы абалга келтирүү чеги**: Ушул чекке барабар же андан төмөн маанилер учурдагы ырааттуулукту баштапкы абалга келтирет. Демейки мааниси: 0.
    	]],
    	["lo"] = [[
ຄຳນວນຊ່ວງຕໍ່ເນື່ອງທີ່ດີທີ່ສຸດຈົນເຖິງປັດຈຸບັນ. ຊ່ວງຕໍ່ເນື່ອງເພີ່ມຂຶ້ນ 1 ສຳລັບຈຸດຂໍ້ມູນຕິດຕໍ່ກັນແຕ່ລະຈຸດທີ່ມີຄ່າຫຼາຍກວ່າເກນຣີເຊັດ. ຄ່າທີ່ນ້ອຍກວ່າ ຫຼື ເທົ່າກັບເກນຣີເຊັດຈະຣີເຊັດຊ່ວງຕໍ່ເນື່ອງປັດຈຸບັນເປັນສູນ.

ຟັງຊັນນີ້ສ້າງຈຸດຂໍ້ມູນຜົນລັບ 1 ຈຸດສຳລັບທຸກຈຸດຂໍ້ມູນຂາເຂົ້າ, ໂດຍຮັກສາເວລາ, ປ້າຍກຳກັບ, ໝາຍເຫດ ແລະອັອບເຊັດເດີມໄວ້. ຄ່າຜົນລັບແມ່ນຊ່ວງຕໍ່ເນື່ອງທີ່ດີທີ່ສຸດຈົນເຖິງຈຸດເວລານັ້ນ.

ການກຳນົດຄ່າ:
- **ເກນຣີເຊັດ**: ຄ່າທີ່ນ້ອຍກວ່າ ຫຼື ເທົ່າກັບເກນນີ້ຈະຣີເຊັດຊ່ວງຕໍ່ເນື່ອງປັດຈຸບັນ. ຄ່າເລີ່ມຕົ້ນແມ່ນ 0.
    	]],
    	["lv"] = [[
Aprēķina līdz šim novēroto labāko sēriju. Sērija palielinās par vienu ar katru secīgu datu punktu, kura vērtība pārsniedz atiestatīšanas slieksni. Vērtības, kas ir mazākas vai vienādas ar atiestatīšanas slieksni, atiestata pašreizējo sēriju uz nulli.

Šī funkcija izvada vienu datu punktu par katru ievades datu punktu, saglabājot sākotnējo laika zīmogu, etiķeti, piezīmi un nobīdi. Izvades vērtība ir līdz attiecīgajam laika punktam novērotā labākā sērija.

Konfigurācija:
- **Atiestatīšanas slieksnis**: Vērtības, kas ir mazākas vai vienādas ar šo slieksni, atiestata pašreizējo sēriju. Noklusējums ir 0.
    	]],
    	["lt"] = [[
Apskaičiuoja iki šiol geriausią seriją. Serija padidėja vienetu už kiekvieną iš eilės einantį duomenų tašką, kurio reikšmė didesnė už atkūrimo ribą. Reikšmės, mažesnės arba lygios atkūrimo ribai, nustato dabartinę seriją į nulį.

Ši funkcija išveda po vieną duomenų tašką kiekvienam įvesties duomenų taškui, išsaugodama pradinę laiko žymą, etiketę, pastabą ir poslinkį. Išvesties reikšmė yra iki to laiko pasiekta geriausia serija.

Konfigūracija:
- **Atkūrimo riba**: Reikšmės, mažesnės arba lygios šiai ribai, nustato dabartinę seriją į nulį. Numatytoji reikšmė – 0.
    	]],
    	["mk"] = [[
Ја пресметува најдобрата низа досега. Низата се зголемува за еден за секоја последователна точка на податоци со вредност поголема од прагот за ресетирање. Вредностите помали или еднакви на прагот за ресетирање ја ресетираат тековната низа на нула.

Оваа функција испраќа една излезна точка на податоци за секоја влезна точка, зачувувајќи ги оригиналниот временски печат, ознака, белешка и поместување. Излезната вредност е најдобрата низа до тој момент.

Конфигурација:
- **Праг за ресетирање**: Вредностите помали или еднакви на овој праг ја ресетираат тековната низа. Стандардно е 0.
    	]],
    	["ms"] = [[
Mengira rentetan terbaik setakat ini. Rentetan meningkat satu bagi setiap titik data berturutan dengan nilai melebihi ambang tetapan semula. Nilai yang kurang daripada atau sama dengan ambang tetapan semula menetapkan semula rentetan semasa kepada sifar.

Fungsi ini menghasilkan satu titik data output bagi setiap titik data input, sambil mengekalkan cap masa, label, nota dan ofset asal. Nilai output ialah rentetan terbaik yang dicapai sehingga waktu tersebut.

Konfigurasi:
- **Ambang Tetapan Semula**: Nilai yang kurang daripada atau sama dengan ambang ini menetapkan semula rentetan semasa. Lalai ialah 0.
    	]],
    	["ml"] = [[
ഇതുവരെ കണ്ട മികച്ച തുടർച്ച കണക്കാക്കുന്നു. റീസെറ്റ് പരിധിയേക്കാൾ കൂടുതലുള്ള ഓരോ തുടർച്ചയായ ഡാറ്റാ പോയിന്റിനും തുടർച്ച ഒന്ന് വീതം വർധിക്കും. റീസെറ്റ് പരിധിയേക്കാൾ കുറവോ തുല്യമോ ആയ മൂല്യങ്ങൾ നിലവിലെ തുടർച്ചയെ പൂജ്യമായി റീസെറ്റ് ചെയ്യും.

ഓരോ ഇൻപുട്ട് ഡാറ്റാ പോയിന്റിനും ഒരു ഔട്ട്പുട്ട് ഡാറ്റാ പോയിന്റ് നൽകുന്നു; യഥാർത്ഥ timestamp, label, note, offset എന്നിവ നിലനിർത്തുന്നു. ഔട്ട്പുട്ട് മൂല്യം ആ സമയത്തേക്കുള്ള മികച്ച തുടർച്ചയായിരിക്കും.

കോൺഫിഗറേഷൻ:
- **Reset Threshold**: ഈ പരിധിയേക്കാൾ കുറവോ തുല്യമോ ആയ മൂല്യങ്ങൾ നിലവിലെ തുടർച്ച റീസെറ്റ് ചെയ്യും. സ്ഥിരസ്ഥിതി 0.
    	]],
    	["mr"] = [[
आतापर्यंतची सर्वोत्तम सलग मालिका मोजते. रीसेट मर्यादेपेक्षा जास्त मूल्य असलेल्या प्रत्येक सलग डेटा बिंदूसाठी सलग मालिका एकने वाढते. रीसेट मर्यादेपेक्षा कमी किंवा समान मूल्ये सध्याची सलग मालिका शून्यावर रीसेट करतात.

हे फंक्शन प्रत्येक इनपुट डेटा बिंदूसाठी एक आउटपुट डेटा बिंदू तयार करते आणि मूळ टाइमस्टॅम्प, लेबल, नोट आणि ऑफसेट कायम ठेवते. आउटपुट मूल्य त्या वेळेपर्यंतची सर्वोत्तम सलग मालिका असते.

कॉन्फिगरेशन:
- **रीसेट मर्यादा**: या मर्यादेपेक्षा कमी किंवा समान मूल्ये सध्याची सलग मालिका रीसेट करतात. डीफॉल्ट मूल्य 0 आहे.
    	]],
    	["mn"] = [[
Одоог хүртэлх хамгийн сайн дарааллыг тооцоолно. Босгоноос их утгатай дараалсан өгөгдлийн цэг бүрт дараалал нэгээр нэмэгдэнэ. Босготой тэнцүү буюу бага утга одоогийн дарааллыг тэг болгон шинэчилнэ.

Энэ функц оролтын өгөгдлийн цэг бүрт нэг гаралтын цэг үүсгэж, анхны цагийн тэмдэг, шошго, тэмдэглэл болон зөрүүг хадгална. Гаралтын утга нь тухайн цаг хүртэлх хамгийн сайн дараалал байна.

Тохиргоо:
- **Шинэчлэх босго**: Энэ босготой тэнцүү буюу бага утга одоогийн дарааллыг шинэчилнэ. Анхдагч утга нь 0.
    	]],
    	["ne"] = [[
अहिलेसम्म देखिएको उत्कृष्ट लगातार क्रम गणना गर्छ। रिसेट सीमाभन्दा ठूलो मान भएको प्रत्येक लगातार डेटा बिन्दुका लागि क्रम एकले बढ्छ। रिसेट सीमाभन्दा कम वा बराबर मानले हालको क्रमलाई शून्यमा रिसेट गर्छ।

यस फङ्सनले प्रत्येक इनपुट डेटा बिन्दुका लागि एउटा आउटपुट डेटा बिन्दु निकाल्छ र मूल टाइमस्ट्याम्प, लेबल, नोट तथा अफसेट कायम राख्छ। आउटपुट मान त्यस समयसम्म देखिएको उत्कृष्ट क्रम हो।

कन्फिगरेसन:
- **रिसेट सीमा**: यस सीमाभन्दा कम वा बराबर मानले हालको क्रम रिसेट गर्छ। पूर्वनिर्धारित मान ० हो।
    	]],
    	["no"] = [[
Beregner den lengste rekken hittil. Rekken øker med én for hvert påfølgende datapunkt med en verdi over tilbakestillingsterskelen. Verdier som er mindre enn eller lik tilbakestillingsterskelen, tilbakestiller den nåværende rekken til null.

Denne funksjonen produserer ett utgående datapunkt for hvert inngående datapunkt og bevarer det opprinnelige tidsstempelet, etiketten, notatet og forskyvningen. Utgangsverdien er den lengste rekken hittil på det tidspunktet.

Konfigurasjon:
- **Tilbakestillingsterskel**: Verdier som er mindre enn eller lik denne terskelen, tilbakestiller den nåværende rekken. Standard er 0.
    	]],
    	["pl"] = [[
Oblicza najlepszą dotychczasową serię. Seria zwiększa się o jeden dla każdego kolejnego punktu danych o wartości większej od progu resetowania. Wartości mniejsze lub równe progowi resetowania zerują bieżącą serię.

Funkcja generuje jeden wyjściowy punkt danych dla każdego wejściowego punktu danych, zachowując jego pierwotny znacznik czasu, etykietę, notatkę i przesunięcie. Wartością wyjściową jest najlepsza seria odnotowana do danego momentu.

Konfiguracja:
- **Próg resetowania**: Wartości mniejsze lub równe temu progowi resetują bieżącą serię. Domyślnie: 0.
    	]],
    	["pt"] = [[
Calcula a melhor sequência observada até ao momento. A sequência aumenta um valor por cada ponto de dados consecutivo com um valor superior ao limite de reinício. Valores menores ou iguais ao limite de reinício repõem a sequência atual a zero.

Esta função emite um ponto de dados de saída para cada ponto de dados de entrada, preservando o carimbo de data/hora, o rótulo, a nota e o deslocamento originais. O valor de saída é a melhor sequência observada até esse momento.

Configuração:
- **Limite de reinício**: Valores menores ou iguais a este limite repõem a sequência atual. Predefinição: 0.
    	]],
    	["pa"] = [[
ਹੁਣ ਤੱਕ ਦੀ ਸਭ ਤੋਂ ਵਧੀਆ ਲੜੀ ਦੀ ਗਣਨਾ ਕਰਦਾ ਹੈ। ਸੀਮਾ ਤੋਂ ਵੱਧ ਮੁੱਲ ਵਾਲੇ ਹਰੇਕ ਲਗਾਤਾਰ ਡਾਟਾ ਪੁਆਇੰਟ ਲਈ ਲੜੀ ਇੱਕ ਨਾਲ ਵਧਦੀ ਹੈ। ਸੀਮਾ ਤੋਂ ਘੱਟ ਜਾਂ ਬਰਾਬਰ ਮੁੱਲ ਮੌਜੂਦਾ ਲੜੀ ਨੂੰ ਸਿਫ਼ਰ ਉੱਤੇ ਰੀਸੈੱਟ ਕਰਦੇ ਹਨ।

ਇਹ ਫੰਕਸ਼ਨ ਹਰੇਕ ਇਨਪੁੱਟ ਡਾਟਾ ਪੁਆਇੰਟ ਲਈ ਇੱਕ ਆਉਟਪੁੱਟ ਡਾਟਾ ਪੁਆਇੰਟ ਜਾਰੀ ਕਰਦਾ ਹੈ ਅਤੇ ਮੂਲ ਟਾਈਮਸਟੈਂਪ, ਲੇਬਲ, ਨੋਟ ਅਤੇ ਆਫਸੈੱਟ ਨੂੰ ਕਾਇਮ ਰੱਖਦਾ ਹੈ। ਆਉਟਪੁੱਟ ਮੁੱਲ ਉਸ ਸਮੇਂ ਤੱਕ ਦੀ ਸਭ ਤੋਂ ਵਧੀਆ ਲੜੀ ਹੁੰਦਾ ਹੈ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਰੀਸੈੱਟ ਸੀਮਾ**: ਇਸ ਸੀਮਾ ਤੋਂ ਘੱਟ ਜਾਂ ਬਰਾਬਰ ਮੁੱਲ ਮੌਜੂਦਾ ਲੜੀ ਨੂੰ ਰੀਸੈੱਟ ਕਰਦੇ ਹਨ। ਡਿਫਾਲਟ 0 ਹੈ।
    	]],
    	["ro"] = [[
Calculează cea mai lungă serie înregistrată până acum. O serie crește cu unu pentru fiecare punct de date consecutiv cu o valoare mai mare decât pragul de resetare. Valorile mai mici sau egale cu pragul de resetare readuc seria curentă la zero.

Această funcție emite câte un punct de date rezultat pentru fiecare punct de date de intrare, păstrând marcajul temporal, eticheta, nota și decalajul originale. Valoarea rezultată este cea mai lungă serie înregistrată până la acel moment.

Configurare:
- **Prag de resetare**: Valorile mai mici sau egale cu acest prag resetează seria curentă. Valoarea implicită este 0.
    	]],
    	["rm"] = [[
Calcula la meglra seria observada fin ussa. Ina seria crescha per in per mintga punct da datas consecutiv cun ina valur pli gronda che la limita da reset. Valurs pli pitschnas u egualas a la limita da reset mettan la seria actuala a nulla.

Questa funcziun emetta in punct da datas d’output per mintga punct da datas d’input, mantegnend il timestamp, l’etichetta, la nota e l’offset originals. La valur d’output è la meglra seria observada fin a quel mument.

Configuraziun:
- **Limita da reset**: Valurs pli pitschnas u egualas a questa limita mettan la seria actuala a nulla. Default: 0.
    	]],
    	["ru"] = [[
Вычисляет лучшую достигнутую серию. Серия увеличивается на единицу для каждой последовательной точки данных со значением выше порога сброса. Значения, меньшие или равные порогу сброса, обнуляют текущую серию.

Эта функция создаёт одну выходную точку данных для каждой входной, сохраняя исходные временную метку, метку, заметку и смещение. Выходное значение — лучшая серия, достигнутая к этому моменту.

Конфигурация:
- **Порог сброса**: Значения, меньшие или равные этому порогу, сбрасывают текущую серию. По умолчанию: 0.
    	]],
    	["sr"] = [[
Izračunava do sada zabeleženi najbolji niz. Niz se povećava za jedan za svaku uzastopnu tačku podataka čija je vrednost veća od praga za resetovanje. Vrednosti manje ili jednake pragu za resetovanje vraćaju trenutni niz na nulu.

Ova funkcija emituje jednu izlaznu tačku podataka za svaku ulaznu tačku podataka, uz očuvane originalne vremenske oznake, oznake, beleške i pomeraje. Izlazna vrednost je najbolji niz zabeležen do tog trenutka.

Konfiguracija:
- **Prag za resetovanje**: Vrednosti manje ili jednake ovom pragu resetuju trenutni niz. Podrazumevano je 0.
    	]],
    	["si"] = [[
මෙතෙක් දක්නට ලැබුණු හොඳම අඛණ්ඩ මාලාව ගණනය කරයි. යළි පිහිටුවීමේ සීමාවට වඩා වැඩි අගයක් සහිත සෑම අනුගාමී දත්ත ලක්ෂ්‍යයක් සඳහාම අඛණ්ඩ මාලාව එකකින් වැඩි වේ. සීමාවට සමාන හෝ අඩු අගයන් වත්මන් අඛණ්ඩ මාලාව ශුන්‍යයට යළි පිහිටුවයි.

මෙම ශ්‍රිතය සෑම ආදාන දත්ත ලක්ෂ්‍යයක් සඳහාම එක් ප්‍රතිදාන දත්ත ලක්ෂ්‍යයක් නිකුත් කරමින්, මුල් වේලාමුද්‍රාව, ලේබලය, සටහන සහ ඕෆ්සෙට් රඳවා ගනී. ප්‍රතිදාන අගය එම කාල ලක්ෂ්‍යය දක්වා දක්නට ලැබුණු හොඳම අඛණ්ඩ මාලාවයි.

වින්‍යාසය:
- **යළි පිහිටුවීමේ සීමාව**: මෙම සීමාවට සමාන හෝ අඩු අගයන් වත්මන් අඛණ්ඩ මාලාව යළි පිහිටුවයි. පෙරනිමිය 0 වේ.
    	]],
    	["sk"] = [[
Vypočíta doteraz najlepšiu sériu. Séria sa zvýši o jeden pri každom po sebe idúcom údajovom bode s hodnotou vyššou než prah resetovania. Hodnoty menšie alebo rovné prahu resetovania vynulujú aktuálnu sériu.

Táto funkcia vytvorí jeden výstupný údajový bod pre každý vstupný údajový bod a zachová pôvodnú časovú pečiatku, označenie, poznámku a posun. Výstupná hodnota je najlepšia séria zaznamenaná do daného času.

Konfigurácia:
- **Prah resetovania**: Hodnoty menšie alebo rovné tomuto prahu vynulujú aktuálnu sériu. Predvolené: 0.
    	]],
    	["sl"] = [[
Izračuna najboljši dosedanji niz. Niz se poveča za ena pri vsaki zaporedni podatkovni točki z vrednostjo, večjo od praga ponastavitve. Vrednosti, manjše ali enake pragu ponastavitve, ponastavijo trenutni niz na nič.

Ta funkcija ustvari eno izhodno podatkovno točko za vsako vhodno podatkovno točko ter ohrani izvirni časovni žig, oznako, opombo in zamik. Izhodna vrednost je najboljši niz do te časovne točke.

Konfiguracija:
- **Prag ponastavitve**: Vrednosti, manjše ali enake temu pragu, ponastavijo trenutni niz. Privzeto je 0.
    	]],
    	["es"] = [[
Calcula la mejor racha registrada hasta el momento. Una racha aumenta en uno por cada punto de datos consecutivo con un valor superior al umbral de reinicio. Los valores menores o iguales al umbral de reinicio restablecen la racha actual a cero.

Esta función emite un punto de datos de salida por cada punto de datos de entrada y conserva la marca de tiempo, la etiqueta, la nota y el desplazamiento originales. El valor de salida es la mejor racha registrada hasta ese momento.

Configuración:
- **Umbral de reinicio**: Los valores menores o iguales a este umbral restablecen la racha actual. El valor predeterminado es 0.
    	]],
    	["sw"] = [[
Hukokotoa mfululizo bora ulioonekana hadi sasa. Mfululizo huongezeka kwa moja kwa kila nukta ya data mfululizo yenye thamani iliyo juu ya kikomo cha kuweka upya. Thamani zilizo chini ya au sawa na kikomo cha kuweka upya huweka mfululizo wa sasa kuwa sifuri.

Function hii hutoa nukta moja ya data ya matokeo kwa kila nukta ya data ya ingizo, huku ikihifadhi muhuri wa muda, lebo, dokezo na offset ya awali. Thamani ya matokeo ni mfululizo bora ulioonekana hadi wakati huo.

Usanidi:
- **Kikomo cha Kuweka Upya**: Thamani zilizo chini ya au sawa na kikomo hiki huweka mfululizo wa sasa upya. Chaguo-msingi ni 0.
    	]],
    	["sv"] = [[
Beräknar den bästa sviten hittills. En svit ökar med ett för varje på varandra följande datapunkt med ett värde över återställningströskeln. Värden som är mindre än eller lika med återställningströskeln återställer den aktuella sviten till noll.

Funktionen genererar en utgående datapunkt för varje ingående datapunkt och bevarar den ursprungliga tidsstämpeln, etiketten, anteckningen och förskjutningen. Utvärdet är den bästa sviten fram till den tidpunkten.

Konfiguration:
- **Återställningströskel**: Värden som är mindre än eller lika med denna tröskel återställer den aktuella sviten. Standardvärdet är 0.
    	]],
    	["ta"] = [[
இதுவரை காணப்பட்ட சிறந்த தொடர்ச்சியைக் கணக்கிடுகிறது. மீட்டமைப்பு வரம்பைவிட அதிகமான மதிப்பைக் கொண்ட ஒவ்வொரு தொடர்ச்சியான தரவுப் புள்ளிக்கும் தொடர்ச்சி ஒன்றால் அதிகரிக்கும். மீட்டமைப்பு வரம்பிற்குச் சமமான அல்லது குறைவான மதிப்புகள் தற்போதைய தொடர்ச்சியை பூஜ்ஜியமாக மீட்டமைக்கும்.

இந்த Function ஒவ்வொரு உள்ளீட்டுத் தரவுப் புள்ளிக்கும் ஒரு வெளியீட்டுத் தரவுப் புள்ளியை உருவாக்கி, அசல் நேரமுத்திரை, லேபிள், குறிப்பு மற்றும் ஆஃப்செட்டைத் தக்கவைக்கிறது. வெளியீட்டு மதிப்பு, அந்த நேரம் வரை காணப்பட்ட சிறந்த தொடர்ச்சியாக இருக்கும்.

உள்ளமைவு:
- **மீட்டமைப்பு வரம்பு**: இதற்குச் சமமான அல்லது குறைவான மதிப்புகள் தற்போதைய தொடர்ச்சியை மீட்டமைக்கும். இயல்புநிலை 0.
    	]],
    	["te"] = [[
ఇప్పటివరకు కనిపించిన అత్యుత్తమ వరుసను లెక్కిస్తుంది. రీసెట్ పరిమితి కంటే ఎక్కువ విలువ ఉన్న ప్రతి వరుస డేటా పాయింట్‌కు వరుస ఒకటితో పెరుగుతుంది. రీసెట్ పరిమితికి సమానమైన లేదా తక్కువ విలువలు ప్రస్తుత వరుసను సున్నాకు రీసెట్ చేస్తాయి.

ఈ ఫంక్షన్ ప్రతి ఇన్‌పుట్ డేటా పాయింట్‌కు ఒక అవుట్‌పుట్ డేటా పాయింట్‌ను ఉత్పత్తి చేస్తుంది; అసలు టైమ్‌స్టాంప్, లేబుల్, నోట్, ఆఫ్‌సెట్‌ను అలాగే ఉంచుతుంది. అవుట్‌పుట్ విలువ ఆ సమయం వరకు కనిపించిన అత్యుత్తమ వరుస.

కాన్ఫిగరేషన్:
- **రీసెట్ పరిమితి**: ఈ పరిమితికి సమానమైన లేదా తక్కువ విలువలు ప్రస్తుత వరుసను రీసెట్ చేస్తాయి. డిఫాల్ట్ 0.
    	]],
    	["th"] = [[
คำนวณช่วงต่อเนื่องที่ดีที่สุดจนถึงปัจจุบัน ช่วงต่อเนื่องจะเพิ่มขึ้นทีละหนึ่งสำหรับจุดข้อมูลต่อเนื่องแต่ละจุดที่มีค่ามากกว่าเกณฑ์รีเซ็ต ค่าที่น้อยกว่าหรือเท่ากับเกณฑ์รีเซ็ตจะรีเซ็ตช่วงต่อเนื่องปัจจุบันเป็นศูนย์

ฟังก์ชันนี้สร้างจุดข้อมูลผลลัพธ์หนึ่งจุดสำหรับจุดข้อมูลอินพุตแต่ละจุด โดยคงเวลา ป้ายกำกับ บันทึก และออฟเซ็ตเดิมไว้ ค่าผลลัพธ์คือช่วงต่อเนื่องที่ดีที่สุดจนถึงเวลานั้น

การกำหนดค่า:
- **เกณฑ์รีเซ็ต**: ค่าที่น้อยกว่าหรือเท่ากับเกณฑ์นี้จะรีเซ็ตช่วงต่อเนื่องปัจจุบัน ค่าเริ่มต้นคือ 0
    	]],
    	["tr"] = [[
Şimdiye kadarki en iyi seriyi hesaplar. Seri, sıfırlama eşiğinden büyük değere sahip her ardışık veri noktası için bir artar. Sıfırlama eşiğine eşit veya daha küçük değerler mevcut seriyi sıfırlar.

Bu işlev, her giriş veri noktası için bir çıktı veri noktası üretir ve özgün zaman damgasını, etiketi, notu ve kaydırmayı korur. Çıktı değeri, o ana kadarki en iyi seridir.

Yapılandırma:
- **Sıfırlama Eşiği**: Bu eşiğe eşit veya daha küçük değerler mevcut seriyi sıfırlar. Varsayılan değer 0'dır.
    	]],
    	["uk"] = [[
Обчислює найкращу серію на цей момент. Серія збільшується на одиницю для кожної послідовної точки даних зі значенням, більшим за поріг скидання. Значення, менші або рівні порогу скидання, скидають поточну серію до нуля.

Ця функція створює одну вихідну точку даних для кожної вхідної точки, зберігаючи початкові часову мітку, мітку, примітку та зміщення. Вихідне значення — найкраща серія до цього моменту.

Конфігурація:
- **Поріг скидання**: Значення, менші або рівні цьому порогу, скидають поточну серію. Типово: 0.
    	]],
    	["vi"] = [[
Tính chuỗi liên tiếp tốt nhất đạt được cho đến hiện tại. Chuỗi tăng một đơn vị với mỗi điểm dữ liệu liên tiếp có giá trị lớn hơn ngưỡng đặt lại. Các giá trị nhỏ hơn hoặc bằng ngưỡng đặt lại sẽ đưa chuỗi hiện tại về 0.

Hàm này tạo một điểm dữ liệu đầu ra cho mỗi điểm dữ liệu đầu vào, giữ nguyên dấu thời gian, nhãn, ghi chú và độ lệch ban đầu. Giá trị đầu ra là chuỗi liên tiếp tốt nhất đạt được cho đến thời điểm đó.

Cấu hình:
- **Ngưỡng đặt lại**: Các giá trị nhỏ hơn hoặc bằng ngưỡng này sẽ đặt lại chuỗi hiện tại. Mặc định là 0.
    	]],
    },
    config = {
        number {
            id = "reset_threshold",
            name = {
            	["en"] = "Reset Threshold",
            	["af"] = "Terugsteldrempel",
            	["sq"] = "Pragu i rivendosjes",
            	["am"] = "ዳግም ማስጀመሪያ ወሰን",
            	["hy"] = "Վերակայման շեմ",
            	["az"] = "Sıfırlama həddi",
            	["bn"] = "রিসেট সীমা",
            	["eu"] = "Berrezartze-atalasea",
            	["be"] = "Парог скіду",
            	["bg"] = "Праг за нулиране",
            	["my"] = "ပြန်လည်သတ်မှတ်ကန့်သတ်ချက်",
            	["ca"] = "Llindar de reinici",
            	["zh-Hans"] = "重置阈值",
            	["zh-Hant"] = "重設閾值",
            	["hr"] = "Prag za resetiranje",
            	["cs"] = "Práh resetování",
            	["da"] = "Nulstillingsgrænse",
            	["nl"] = "Resetdrempel",
            	["et"] = "Lähtestamislävi",
            	["fil"] = "Reset Threshold",
            	["fi"] = "Nollauskynnysarvo",
            	["fr"] = "Seuil de réinitialisation",
            	["gl"] = "Limiar de reinicio",
            	["ka"] = "აღდგენის ზღვარი",
            	["de"] = "Rücksetzschwellenwert",
            	["el"] = "Όριο επαναφοράς",
            	["gu"] = "રીસેટ મર્યાદા",
            	["hi"] = "रीसेट थ्रेशोल्ड",
            	["hu"] = "Visszaállítási küszöb",
            	["is"] = "Endurstillingarmörk",
            	["id"] = "Ambang Reset",
            	["it"] = "Soglia di reimpostazione",
            	["ja"] = "リセットしきい値",
            	["kn"] = "ಮರುಹೊಂದಿಸುವ ಮಿತಿ",
            	["kk"] = "Қалпына келтіру шегі",
            	["km"] = "កម្រិតកំណត់ឡើងវិញ",
            	["ko"] = "재설정 임계값",
            	["ky"] = "Баштапкы абалга келтирүү чеги",
            	["lo"] = "ຄ່າເກນການຣີເຊັດ",
            	["lv"] = "Atiestatīšanas slieksnis",
            	["lt"] = "Atkūrimo riba",
            	["mk"] = "Праг за ресетирање",
            	["ms"] = "Ambang Tetapan Semula",
            	["ml"] = "റീസെറ്റ് പരിധി",
            	["mr"] = "रीसेट मर्यादा",
            	["mn"] = "Шинэчлэх босго",
            	["ne"] = "रिसेट सीमा",
            	["no"] = "Tilbakestillingsterskel",
            	["pl"] = "Próg resetowania",
            	["pt"] = "Limite de reinício",
            	["pa"] = "ਰੀਸੈੱਟ ਸੀਮਾ",
            	["ro"] = "Prag de resetare",
            	["rm"] = "Limita da reset",
            	["ru"] = "Порог сброса",
            	["sr"] = "Prag za resetovanje",
            	["si"] = "යළි පිහිටුවීමේ සීමාව",
            	["sk"] = "Prah resetovania",
            	["sl"] = "Prag ponastavitve",
            	["es"] = "Umbral de reinicio",
            	["sw"] = "Kikomo cha Kuweka Upya",
            	["sv"] = "Återställningströskel",
            	["ta"] = "மீட்டமைப்பு வரம்பு",
            	["te"] = "రీసెట్ పరిమితి",
            	["th"] = "เกณฑ์รีเซ็ต",
            	["tr"] = "Sıfırlama Eşiği",
            	["uk"] = "Поріг скидання",
            	["vi"] = "Ngưỡng đặt lại",
            },
            default = 0.0,
        },
    },

    generator = function(source, config)
        local reset_threshold = config and config.reset_threshold or 0.0
        local all_points = source.dpall()

        local current_streak = 0
        local best_streak = 0

        for i = #all_points, 1, -1 do
            local data_point = all_points[i]

            if data_point.value <= reset_threshold then
                current_streak = 0
            else
                current_streak = current_streak + 1
                if current_streak > best_streak then
                    best_streak = current_streak
                end
            end

            data_point.value = best_streak
        end

        local index = 0
        return function()
            index = index + 1
            return all_points[index]
        end
    end,
}
