-- Lua Function to ceiling values
-- Rounds each data point's value up to the nearest multiple of a specified number

local number = require("tng.config").number

return {
	-- Configuration metadata
	id = "ceil",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Ceiling",
		["af"] = "Afronding Opwaarts",
		["sq"] = "Rrumbullakim lart",
		["am"] = "ወደ ላይ ማጠጋጋት",
		["hy"] = "Կլորացում վերև",
		["az"] = "Yuxarı yuvarlaqlaşdırma",
		["bn"] = "ঊর্ধ্বমুখী রাউন্ড",
		["eu"] = "Gorantz biribildu",
		["be"] = "Акругленне ўверх",
		["bg"] = "Закръгляне нагоре",
		["my"] = "အပေါ်သို့ ပတ်လည်တန်ဖိုး",
		["ca"] = "Arrodoniment cap amunt",
		["zh-Hans"] = "向上取整",
		["zh-Hant"] = "無條件進位",
		["hr"] = "Zaokruživanje prema gore",
		["cs"] = "Zaokrouhlení nahoru",
		["da"] = "Loft",
		["nl"] = "Afronden naar boven",
		["et"] = "Ümardamine üles",
		["fil"] = "Ceiling",
		["fi"] = "Pyöristys ylöspäin",
		["fr"] = "Plafond",
		["gl"] = "Teito",
		["ka"] = "ზემოთ დამრგვალება",
		["de"] = "Aufrunden",
		["el"] = "Στρογγυλοποίηση προς τα πάνω",
		["gu"] = "ઉપરની પૂર્ણાંક ગોળાઈ",
		["hi"] = "सीलिंग",
		["hu"] = "Felfelé kerekítés",
		["is"] = "Hækka í næsta heilt margfeldi",
		["id"] = "Pembulatan Ke Atas",
		["it"] = "Arrotondamento per eccesso",
		["ja"] = "切り上げ",
		["kn"] = "ಮೇಲ್ಮುಖ ಪೂರ್ಣಾಂಕೀಕರಣ",
		["kk"] = "Жоғарыға дөңгелектеу",
		["km"] = "បង្គត់ឡើង",
		["ko"] = "올림",
		["ky"] = "Жогору тегеректөө",
		["lo"] = "ປັດຂຶ້ນ",
		["lv"] = "Noapaļošana uz augšu",
		["lt"] = "Apvalinimas aukštyn",
		["mk"] = "Заокружување нагоре",
		["ms"] = "Pembundaran Ke Atas",
		["ml"] = "മുകളിലേക്ക് റൗണ്ട് ചെയ്യുക",
		["mr"] = "कमाल मर्यादा",
		["mn"] = "Дээш тоймлох",
		["ne"] = "माथिल्लो राउन्ड",
		["no"] = "Rund opp",
		["pl"] = "Zaokrąglanie w górę",
		["pt"] = "Arredondamento para cima",
		["pa"] = "ਉੱਪਰ ਵੱਲ ਗੋਲਾਕਾਰ",
		["ro"] = "Rotunjire în sus",
		["rm"] = "Arrotondament en l’aut",
		["ru"] = "Округление вверх",
		["sr"] = "Zaokruživanje naviše",
		["si"] = "ඉහළට වට කිරීම",
		["sk"] = "Zaokrúhlenie nahor",
		["sl"] = "Zaokroževanje navzgor",
		["es"] = "Techo",
		["sw"] = "Kuzungusha Juu",
		["sv"] = "Avrunda uppåt",
		["ta"] = "மேல் முழுமையாக்கம்",
		["te"] = "సీలింగ్",
		["th"] = "ปัดขึ้น",
		["tr"] = "Yukarı Yuvarla",
		["uk"] = "Округлення вгору",
		["vi"] = "Làm tròn lên",
	},
	description = {
		["en"] = [[
Rounds each data point's value up to the nearest multiple of a specified number.

Configuration:
- **Nearest**: Round up to the nearest multiple of this number (default: 1.0)
		]],
		["af"] = [[
Rond elke datapunt se waarde opwaarts af tot die naaste veelvoud van ’n gespesifiseerde getal.

Konfigurasie:
- **Naaste**: Rond opwaarts af tot die naaste veelvoud van hierdie getal (verstek: 1.0)
		]],
		["sq"] = [[
Rrumbullakon vlerën e çdo pike të të dhënave lart, në shumëfishin më të afërt të një numri të specifikuar.

Konfigurimi:
- **Më i afërti**: Rrumbullako lart në shumëfishin më të afërt të këtij numri (parazgjedhja: 1.0)
		]],
		["am"] = [[
የእያንዳንዱን የውሂብ ነጥብ ዋጋ ወደተገለጸው ቁጥር ቅርብ ወደሆነው ብዜት ያጠጋጋል።

ውቅር፦
- **ቅርብ ብዜት**፦ ወደዚህ ቁጥር ቅርብ ወደሆነው ብዜት ያጠጋጋል (ነባሪ፦ 1.0)
		]],
		["hy"] = [[
Յուրաքանչյուր տվյալակետի արժեքը կլորացնում է վերև՝ մինչև նշված թվի ամենամոտ բազմապատիկը։

Կազմաձևում՝
- **Ամենամոտը**․ կլորացնել վերև՝ մինչև այս թվի ամենամոտ բազմապատիկը (կանխադրված՝ 1.0)
		]],
		["az"] = [[
Hər məlumat nöqtəsinin qiymətini göstərilən ədədin ən yaxın qatına yuxarı yuvarlaqlaşdırır.

Konfiqurasiya:
- **Ən yaxın**: Bu ədədin ən yaxın qatına yuxarı yuvarlaqlaşdır (standart: 1.0)
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্টের মানকে নির্দিষ্ট একটি সংখ্যার নিকটতম গুণিতকে ঊর্ধ্বমুখী রাউন্ড করে।

কনফিগারেশন:
- **নিকটতম**: এই সংখ্যার নিকটতম গুণিতকে ঊর্ধ্বমুখী রাউন্ড করুন (ডিফল্ট: 1.0)
		]],
		["eu"] = [[
Datu-puntu bakoitzaren balioa zehaztutako zenbaki baten hurbileneko multiplo gorenera biribiltzen du.

Konfigurazioa:
- **Hurbilena**: Biribildu gorantz zenbaki honen hurbileneko multiplora (lehenetsia: 1.0)
		]],
		["be"] = [[
Акругляе значэнне кожнай кропкі даных уверх да бліжэйшага кратнага зададзенага ліку.

Канфігурацыя:
- **Бліжэйшае кратнае**: акругляць уверх да бліжэйшага кратнага гэтага ліку (па змаўчанні: 1.0)
		]],
		["bg"] = [[
Закръгля стойността на всяка точка от данни нагоре до най-близкото кратно на зададено число.

Конфигурация:
- **Най-близко кратно**: Закръгляне нагоре до най-близкото кратно на това число (по подразбиране: 1.0)
		]],
		["my"] = [[
ဒေတာမှတ်တစ်ခုချင်းစီ၏ တန်ဖိုးကို သတ်မှတ်ထားသော ကိန်းတစ်ခု၏ အနီးဆုံးမြှောက်ကိန်းသို့ အပေါ်သို့ ပတ်လည်တန်ဖိုးပြောင်းသည်။

ဖွဲ့စည်းမှု:
- **အနီးဆုံး**: ဤကိန်း၏ အနီးဆုံးမြှောက်ကိန်းသို့ အပေါ်သို့ ပတ်လည်တန်ဖိုးပြောင်းသည် (မူလ: 1.0)
		]],
		["ca"] = [[
Arrodoneix el valor de cada punt de dades cap amunt fins al múltiple més proper d’un nombre especificat.

Configuració:
- **Múltiple més proper**: Arrodoneix cap amunt fins al múltiple més proper d’aquest nombre (per defecte: 1.0)
		]],
		["zh-Hans"] = [[
将每个数据点的值向上取整到指定数字的最近倍数。

配置：
- **最近倍数**：向上取整到此数字的最近倍数（默认：1.0）
		]],
		["zh-Hant"] = [[
將每個資料點的值向上取整至指定數字的最接近倍數。

設定：
- **最接近倍數**：向上取整至此數字的最接近倍數（預設：1.0）
		]],
		["hr"] = [[
Zaokružuje vrijednost svake podatkovne točke prema gore na najbliži višekratnik zadanog broja.

Konfiguracija:
- **Najbliži višekratnik**: Zaokruživanje prema gore na najbliži višekratnik ovog broja (zadano: 1.0)
		]],
		["cs"] = [[
Zaokrouhlí hodnotu každého datového bodu nahoru na nejbližší násobek zadaného čísla.

Konfigurace:
- **Nejbližší násobek**: Zaokrouhlit nahoru na nejbližší násobek tohoto čísla (výchozí: 1.0)
		]],
		["da"] = [[
Runder hver datapunkts værdi op til det nærmeste multiplum af et angivet tal.

Konfiguration:
- **Nærmeste**: Rund op til det nærmeste multiplum af dette tal (standard: 1.0)
		]],
		["nl"] = [[
Rondt de waarde van elk gegevenspunt naar boven af op het dichtstbijzijnde veelvoud van een opgegeven getal.

Configuratie:
- **Dichtstbijzijnde**: Naar boven afronden op het dichtstbijzijnde veelvoud van dit getal (standaard: 1.0)
		]],
		["et"] = [[
Ümardab iga andmepunkti väärtuse määratud arvu lähima suurema kordkorrani.

Seadistus:
- **Lähim kordkord**: ümardamine selle arvu lähima suurema kordkorrani (vaikimisi: 1,0)
		]],
		["fil"] = [[
Pina-round up ang halaga ng bawat data point sa pinakamalapit na multiple ng tinukoy na numero.

Configuration:
- **Nearest**: I-round up sa pinakamalapit na multiple ng numerong ito (default: 1.0)
		]],
		["fi"] = [[
Pyöristää kunkin datapisteen arvon ylöspäin määritetyn luvun lähimpään monikertaan.

Määritys:
- **Lähin monikerta**: Pyöristä ylöspäin tämän luvun lähimpään monikertaan (oletus: 1.0)
		]],
		["fr"] = [[
Arrondit vers le haut la valeur de chaque point de données au multiple le plus proche d’un nombre indiqué.

Configuration :
- **Multiple** : Arrondir vers le haut au multiple le plus proche de ce nombre (par défaut : 1.0)
		]],
		["gl"] = [[
Redondea o valor de cada punto de datos cara arriba ao múltiplo máis próximo dun número especificado.

Configuración:
- **Múltiplo máis próximo**: Redondear cara arriba ao múltiplo máis próximo deste número (predeterminado: 1.0)
		]],
		["ka"] = [[
თითოეული მონაცემის წერტილის მნიშვნელობას ამრგვალებს მითითებული რიცხვის უახლოეს ჯერადამდე, ზემოთ.

კონფიგურაცია:
- **უახლოესი**: ამ რიცხვის უახლოეს ჯერადამდე ზემოთ დამრგვალება (ნაგულისხმევი: 1.0)
		]],
		["de"] = [[
Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl auf.

Konfiguration:
- **Nächstes Vielfaches**: Auf das nächste Vielfache dieser Zahl aufrunden (Standard: 1.0)
		]],
		["el"] = [[
Στρογγυλοποιεί την τιμή κάθε σημείου δεδομένων προς τα πάνω στο πλησιέστερο πολλαπλάσιο ενός καθορισμένου αριθμού.

Διαμόρφωση:
- **Πλησιέστερο**: Στρογγυλοποίηση προς τα πάνω στο πλησιέστερο πολλαπλάσιο αυτού του αριθμού (προεπιλογή: 1.0)
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટના મૂલ્યને નિર્દિષ્ટ સંખ્યાના સૌથી નજીકના ગુણાંક સુધી ઉપરની તરફ ગોળ કરે છે.

ગોઠવણી:
- **નજીકનો ગુણાંક**: આ સંખ્યાના સૌથી નજીકના ગુણાંક સુધી ઉપર ગોળ કરો (ડિફૉલ્ટ: 1.0)
		]],
		["hi"] = [[
हर डेटा पॉइंट के मान को निर्दिष्ट संख्या के निकटतम गुणज तक ऊपर की ओर पूर्णांकित करता है।

कॉन्फ़िगरेशन:
- **निकटतम**: इस संख्या के निकटतम गुणज तक ऊपर की ओर पूर्णांकित करें (डिफ़ॉल्ट: 1.0)
		]],
		["hu"] = [[
Az egyes adatpontok értékét a megadott szám legközelebbi többszörösére kerekíti felfelé.

Konfiguráció:
- **Legközelebbi**: Felfelé kerekítés a szám legközelebbi többszörösére (alapértelmezett: 1.0)
		]],
		["is"] = [[
Hækka gildi hvers gagnapunkts í næsta margfeldi tilgreindrar tölu.

Stillingar:
- **Næsta margfeldi**: Hækka í næsta margfeldi þessarar tölu (sjálfgefið: 1.0)
		]],
		["id"] = [[
Membulatkan nilai setiap titik data ke atas hingga kelipatan terdekat dari angka yang ditentukan.

Konfigurasi:
- **Terdekat**: Membulatkan ke atas hingga kelipatan terdekat dari angka ini (default: 1.0)
		]],
		["it"] = [[
Arrotonda per eccesso il valore di ogni punto dati al multiplo più vicino di un numero specificato.

Configurazione:
- **Più vicino**: arrotonda per eccesso al multiplo più vicino di questo numero (predefinito: 1.0)
		]],
		["ja"] = [[
各データポイントの値を、指定した数の最も近い倍数に切り上げます。

設定:
- **最近傍**: この数の最も近い倍数に切り上げます（デフォルト: 1.0）
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯವನ್ನು ನಿರ್ದಿಷ್ಟ ಸಂಖ್ಯೆಯ ಸಮೀಪದ ಗುಣಕದವರೆಗೆ ಮೇಲಕ್ಕೆ ಪೂರ್ಣಾಂಕಗೊಳಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಸಮೀಪದ ಗುಣಕ**: ಈ ಸಂಖ್ಯೆಯ ಸಮೀಪದ ಗುಣಕದವರೆಗೆ ಮೇಲಕ್ಕೆ ಪೂರ್ಣಾಂಕಗೊಳಿಸಿ (ಡೀಫಾಲ್ಟ್: 1.0)
		]],
		["kk"] = [[
Әр дерек нүктесінің мәнін көрсетілген санның ең жақын еселігіне жоғары қарай дөңгелектейді.

Конфигурация:
- **Ең жақын еселік**: Осы санның ең жақын еселігіне жоғары қарай дөңгелектеу (әдепкі: 1.0)
		]],
		["km"] = [[
បង្គត់តម្លៃនៃចំណុចទិន្នន័យនីមួយៗឡើងទៅពហុគុណជិតបំផុតនៃចំនួនដែលបានកំណត់។

ការកំណត់៖
- **ជិតបំផុត**៖ បង្គត់ឡើងទៅពហុគុណជិតបំផុតនៃចំនួននេះ (លំនាំដើម៖ 1.0)
		]],
		["ko"] = [[
각 데이터 포인트의 값을 지정한 수의 가장 가까운 배수로 올림합니다.

구성:
- **가장 가까운 배수**: 이 수의 가장 가까운 배수로 올림합니다(기본값: 1.0)
		]],
		["ky"] = [[
Ар бир маалымат чекитинин маанисин көрсөтүлгөн сандын эң жакын эселигине жогору карай тегеректейт.

Тууралоо:
- **Эң жакын**: Ушул сандын эң жакын эселигине жогору карай тегеректөө (демейки: 1.0)
		]],
		["lo"] = [[
ປັດຄ່າຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດຂຶ້ນເປັນຈຳນວນທະວີຄູນທີ່ໃກ້ທີ່ສຸດຂອງຈຳນວນທີ່ກຳນົດ.

ການຕັ້ງຄ່າ:
- **ທະວີຄູນທີ່ໃກ້ທີ່ສຸດ**: ປັດຂຶ້ນເປັນຈຳນວນທະວີຄູນທີ່ໃກ້ທີ່ສຸດຂອງຈຳນວນນີ້ (ຄ່າເລີ່ມຕົ້ນ: 1.0)
		]],
		["lv"] = [[
Noapaļo katra datu punkta vērtību uz augšu līdz tuvākajam norādītā skaitļa daudzkārtnim.

Konfigurācija:
- **Tuvākais daudzkārtnis**: Noapaļot uz augšu līdz tuvākajam šī skaitļa daudzkārtnim (noklusējums: 1.0)
		]],
		["lt"] = [[
Kiekvieno duomenų taško reikšmę suapvalina aukštyn iki artimiausio nurodyto skaičiaus kartotinio.

Konfigūracija:
- **Artimiausias kartotinis**: Skaičius, kurio kartotinio link apvalinti aukštyn (numatytoji reikšmė: 1.0)
		]],
		["mk"] = [[
Ја заокружува вредноста на секоја точка на податоци нагоре до најблискиот умножок на зададен број.

Конфигурација:
- **Најблизок умножок**: Заокружи нагоре до најблискиот умножок на овој број (стандардно: 1.0)
		]],
		["ms"] = [[
Membundarkan nilai setiap titik data ke atas kepada gandaan terdekat bagi nombor yang ditentukan.

Konfigurasi:
- **Gandaan Terdekat**: Bundarkan ke atas kepada gandaan terdekat bagi nombor ini (lalai: 1.0)
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യം നിർദ്ദിഷ്ട സംഖ്യയുടെ ഏറ്റവും അടുത്ത ഗുണിതത്തിലേക്ക് മുകളിലേക്ക് റൗണ്ട് ചെയ്യുന്നു.

കോൺഫിഗറേഷൻ:
- **Nearest**: ഈ സംഖ്യയുടെ ഏറ്റവും അടുത്ത ഗുണിതത്തിലേക്ക് മുകളിലേക്ക് റൗണ്ട് ചെയ്യുക (സ്ഥിരസ്ഥിതി: 1.0)
		]],
		["mr"] = [[
प्रत्येक डेटा बिंदूचे मूल्य निर्दिष्ट संख्येच्या जवळच्या पटीपर्यंत वरच्या दिशेने पूर्णांकित करते.

कॉन्फिगरेशन:
- **जवळचा गुणक**: या संख्येच्या जवळच्या पटीपर्यंत वरच्या दिशेने पूर्णांकित करा (डीफॉल्ट: 1.0)
		]],
		["mn"] = [[
Өгөгдлийн цэг бүрийн утгыг заасан тооны хамгийн ойрын үржвэр хүртэл дээш тоймлоно.

Тохиргоо:
- **Хамгийн ойр**: Энэ тооны хамгийн ойрын үржвэр хүртэл дээш тоймлох (анхдагч: 1.0)
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दुको मानलाई निर्दिष्ट संख्याको सबैभन्दा नजिकको गुणजमा माथितिर राउन्ड गर्छ।

कन्फिगरेसन:
- **नजिकको**: यस संख्याको सबैभन्दा नजिकको गुणजमा माथितिर राउन्ड गर्ने (पूर्वनिर्धारित: 1.0)
		]],
		["no"] = [[
Runder verdien til hvert datapunkt opp til nærmeste multiplum av et angitt tall.

Konfigurasjon:
- **Nærmeste**: Rund opp til nærmeste multiplum av dette tallet (standard: 1.0)
		]],
		["pl"] = [[
Zaokrągla wartość każdego punktu danych w górę do najbliższej wielokrotności określonej liczby.

Konfiguracja:
- **Najbliższa wielokrotność**: Zaokrąglaj w górę do najbliższej wielokrotności tej liczby (domyślnie: 1.0)
		]],
		["pt"] = [[
Arredonda o valor de cada ponto de dados para cima, até ao múltiplo mais próximo de um número especificado.

Configuração:
- **Mais próximo**: Arredondar para cima até ao múltiplo mais próximo deste número (predefinição: 1.0)
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਨੂੰ ਨਿਰਧਾਰਤ ਸੰਖਿਆ ਦੇ ਸਭ ਤੋਂ ਨੇੜਲੇ ਗੁਣਜ ਤੱਕ ਉੱਪਰ ਵੱਲ ਗੋਲ ਕਰਦਾ ਹੈ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਨੇੜਲਾ ਗੁਣਜ**: ਇਸ ਸੰਖਿਆ ਦੇ ਸਭ ਤੋਂ ਨੇੜਲੇ ਗੁਣਜ ਤੱਕ ਉੱਪਰ ਵੱਲ ਗੋਲ ਕਰੋ (ਡਿਫਾਲਟ: 1.0)
		]],
		["ro"] = [[
Rotunjește în sus valoarea fiecărui punct de date la cel mai apropiat multiplu al unui număr specificat.

Configurare:
- **Cel mai apropiat**: Rotunjește în sus la cel mai apropiat multiplu al acestui număr (implicit: 1.0)
		]],
		["rm"] = [[
Arrotonda la valur da mintga punct da datas en l’aut al pli proxim multipel d’in dumber specificà.

Configuraziun:
- **Il pli proxim**: Arrotondar en l’aut al multipel il pli proxim da quest dumber (default: 1.0)
		]],
		["ru"] = [[
Округляет значение каждой точки данных вверх до ближайшего кратного заданного числа.

Конфигурация:
- **Ближайшее кратное**: Округлять вверх до ближайшего кратного этого числа (по умолчанию: 1.0)
		]],
		["sr"] = [[
Zaokružuje vrednost svake tačke podataka naviše na najbliži umnožak zadatog broja.

Konfiguracija:
- **Najbliži umnožak**: Zaokruživanje naviše na najbliži umnožak ovog broja (podrazumevano: 1.0)
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයකම අගය නිශ්චිත සංඛ්‍යාවක ආසන්නතම ගුණාකාරයට ඉහළට වට කරයි.

වින්‍යාසය:
- **ආසන්නතමය**: මෙම සංඛ්‍යාවේ ආසන්නතම ගුණාකාරයට ඉහළට වට කරන්න (පෙරනිමිය: 1.0)
		]],
		["sk"] = [[
Zaokrúhli hodnotu každého údajového bodu nahor na najbližší násobok zadaného čísla.

Konfigurácia:
- **Najbližší násobok**: Zaokrúhli nahor na najbližší násobok tohto čísla (predvolené: 1.0)
		]],
		["sl"] = [[
Zaokroži vrednost vsake podatkovne točke navzgor na najbližji večkratnik določenega števila.

Konfiguracija:
- **Najbližji večkratnik**: Zaokroži navzgor na najbližji večkratnik tega števila (privzeto: 1.0)
		]],
		["es"] = [[
Redondea hacia arriba el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
- **Múltiplo**: Redondear hacia arriba al múltiplo más cercano de este número (predeterminado: 1.0)
		]],
		["sw"] = [[
Huzungusha thamani ya kila nukta ya data kwenda juu hadi kizidisho cha karibu zaidi cha nambari maalum.

Usanidi:
- **Karibu**: Zungusha juu hadi kizidisho cha karibu zaidi cha nambari hii (chaguo-msingi: 1.0)
		]],
		["sv"] = [[
Avrundar värdet för varje datapunkt uppåt till närmaste multipel av ett angivet tal.

Konfiguration:
- **Närmaste**: Avrunda uppåt till närmaste multipel av detta tal (standard: 1.0)
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளியின் மதிப்பையும் குறிப்பிட்ட எண்ணின் அருகிலுள்ள மடங்கிற்கு மேல்நோக்கி முழுமையாக்குகிறது.

உள்ளமைவு:
- **அருகிலுள்ள மடங்கு**: இந்த எண்ணின் அருகிலுள்ள மடங்கிற்கு மேல்நோக்கி முழுமையாக்கு (இயல்புநிலை: 1.0)
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్ విలువను పేర్కొన్న సంఖ్యకు సమీప గుణితానికి పైకి రౌండ్ చేస్తుంది.

కాన్ఫిగరేషన్:
- **సమీప గుణితం**: ఈ సంఖ్యకు సమీప గుణితానికి పైకి రౌండ్ చేయండి (డిఫాల్ట్: 1.0)
		]],
		["th"] = [[
ปัดค่าของจุดข้อมูลแต่ละจุดขึ้นเป็นพหุคูณที่ใกล้ที่สุดของตัวเลขที่กำหนด

การกำหนดค่า:
- **พหุคูณที่ใกล้ที่สุด**: ปัดขึ้นเป็นพหุคูณที่ใกล้ที่สุดของตัวเลขนี้ (ค่าเริ่มต้น: 1.0)
		]],
		["tr"] = [[
Her veri noktasının değerini, belirtilen sayının en yakın katına yukarı yuvarlar.

Yapılandırma:
- **En Yakın**: Bu sayının en yakın katına yukarı yuvarla (varsayılan: 1.0)
		]],
		["uk"] = [[
Округлює значення кожної точки даних вгору до найближчого кратного заданого числа.

Конфігурація:
- **Найближче кратне**: Округлювати вгору до найближчого кратного цього числа (типово: 1.0)
		]],
		["vi"] = [[
Làm tròn giá trị của từng điểm dữ liệu lên bội số gần nhất của một số được chỉ định.

Cấu hình:
- **Bội số gần nhất**: Làm tròn lên bội số gần nhất của số này (mặc định: 1.0)
		]],
	},
	config = {
		number {
			id = "nearest",
			default = 1.0,
			name = {
				["en"] = "Nearest",
				["af"] = "Naaste",
				["sq"] = "Më i afërti",
				["am"] = "ቅርብ ብዜት",
				["hy"] = "Ամենամոտը",
				["az"] = "Ən yaxın",
				["bn"] = "নিকটতম",
				["eu"] = "Hurbilena",
				["be"] = "Бліжэйшае кратнае",
				["bg"] = "Най-близко кратно",
				["my"] = "အနီးဆုံး",
				["ca"] = "Múltiple més proper",
				["zh-Hans"] = "最近倍数",
				["zh-Hant"] = "最接近倍數",
				["hr"] = "Najbliži višekratnik",
				["cs"] = "Nejbližší násobek",
				["da"] = "Nærmeste",
				["nl"] = "Dichtstbijzijnde",
				["et"] = "Lähim kordkord",
				["fil"] = "Nearest",
				["fi"] = "Lähin monikerta",
				["fr"] = "Multiple",
				["gl"] = "Múltiplo máis próximo",
				["ka"] = "უახლოესი",
				["de"] = "Nächstes Vielfaches",
				["el"] = "Πλησιέστερο",
				["gu"] = "નજીકનો ગુણાંક",
				["hi"] = "निकटतम",
				["hu"] = "Legközelebbi",
				["is"] = "Næsta margfeldi",
				["id"] = "Terdekat",
				["it"] = "Più vicino",
				["ja"] = "最近傍",
				["kn"] = "ಸಮೀಪದ ಗುಣಕ",
				["kk"] = "Ең жақын еселік",
				["km"] = "ជិតបំផុត",
				["ko"] = "가장 가까운 배수",
				["ky"] = "Эң жакын",
				["lo"] = "ທະວີຄູນທີ່ໃກ້ທີ່ສຸດ",
				["lv"] = "Tuvākais daudzkārtnis",
				["lt"] = "Artimiausias kartotinis",
				["mk"] = "Најблизок умножок",
				["ms"] = "Gandaan Terdekat",
				["ml"] = "ഏറ്റവും അടുത്തത്",
				["mr"] = "जवळचा गुणक",
				["mn"] = "Хамгийн ойр",
				["ne"] = "नजिकको",
				["no"] = "Nærmeste",
				["pl"] = "Najbliższa wielokrotność",
				["pt"] = "Mais próximo",
				["pa"] = "ਨੇੜਲਾ ਗੁਣਜ",
				["ro"] = "Cel mai apropiat",
				["rm"] = "Il pli proxim",
				["ru"] = "Ближайшее кратное",
				["sr"] = "Najbliži umnožak",
				["si"] = "ආසන්නතමය",
				["sk"] = "Najbližší násobok",
				["sl"] = "Najbližji večkratnik",
				["es"] = "Múltiplo",
				["sw"] = "Karibu",
				["sv"] = "Närmaste",
				["ta"] = "அருகிலுள்ள மடங்கு",
				["te"] = "సమీప గుణితం",
				["th"] = "พหุคูณที่ใกล้ที่สุด",
				["tr"] = "En Yakın",
				["uk"] = "Найближче кратне",
				["vi"] = "Bội số gần nhất",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local nearest = config and config.nearest or 1.0

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Ceiling to nearest multiple
			data_point.value = math.ceil(data_point.value / nearest) * nearest

			return data_point
		end
	end,
}
