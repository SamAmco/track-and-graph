-- Lua Function to round values
-- Rounds each data point's value to the nearest multiple of a specified number

local number = require("tng.config").number

return {
	-- Configuration metadata
	id = "round",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Round",
		["af"] = "Afrond",
		["sq"] = "Rrumbullako",
		["am"] = "አጠጋጋ",
		["hy"] = "Կլորացնել",
		["az"] = "Yuvarlaqlaşdırma",
		["bn"] = "রাউন্ড",
		["eu"] = "Biribildu",
		["be"] = "Акругленне",
		["bg"] = "Закръгляне",
		["my"] = "ပတ်လည်တန်ဖိုးချရန်",
		["ca"] = "Arrodoneix",
		["zh-Hans"] = "四舍五入",
		["zh-Hant"] = "四捨五入",
		["hr"] = "Zaokruži",
		["cs"] = "Zaokrouhlit",
		["da"] = "Afrund",
		["nl"] = "Afronden",
		["et"] = "Ümarda",
		["fil"] = "I-round",
		["fi"] = "Pyöristä",
		["fr"] = "Arrondir",
		["gl"] = "Redondear",
		["ka"] = "დამრგვალება",
		["de"] = "Runden",
		["el"] = "Στρογγυλοποίηση",
		["gu"] = "ગોળ કરો",
		["hi"] = "राउंड",
		["hu"] = "Kerekítés",
		["is"] = "Námunda",
		["id"] = "Bulatkan",
		["it"] = "Arrotonda",
		["ja"] = "丸め",
		["kn"] = "ಪೂರ್ಣಾಂಕಗೊಳಿಸಿ",
		["kk"] = "Дөңгелектеу",
		["km"] = "បង្គត់",
		["ko"] = "반올림",
		["ky"] = "Тегеректөө",
		["lo"] = "ປັດ",
		["lv"] = "Noapaļot",
		["lt"] = "Apvalinti",
		["mk"] = "Заокружи",
		["ms"] = "Bundarkan",
		["ml"] = "റൗണ്ട് ചെയ്യുക",
		["mr"] = "राउंड",
		["mn"] = "Тоймлох",
		["ne"] = "राउन्ड",
		["no"] = "Avrund",
		["pl"] = "Zaokrąglanie",
		["pt"] = "Arredondar",
		["pa"] = "ਰਾਊਂਡ",
		["ro"] = "Rotunjire",
		["rm"] = "Rundar",
		["ru"] = "Округление",
		["sr"] = "Zaokruži",
		["si"] = "වට කරන්න",
		["sk"] = "Zaokrúhliť",
		["sl"] = "Zaokroži",
		["es"] = "Redondear",
		["sw"] = "Zungusha",
		["sv"] = "Avrunda",
		["ta"] = "சுற்று",
		["te"] = "రౌండ్",
		["th"] = "ปัดเศษ",
		["tr"] = "Yuvarla",
		["uk"] = "Округлення",
		["vi"] = "Làm tròn",
	},
	description = {
		["en"] = [[
Rounds each data point's value to the nearest multiple of a specified number.

Configuration:
- **Nearest**: Round to the nearest multiple of this number (default: 1.0)
		]],
		["af"] = [[
Rond elke datapunt se waarde af tot die naaste veelvoud van ’n gespesifiseerde getal.

Konfigurasie:
- **Naaste**: Rond af tot die naaste veelvoud van hierdie getal (verstek: 1.0)
		]],
		["sq"] = [[
Rrumbullakon vlerën e çdo pike të të dhënave në shumëfishin më të afërt të një numri të specifikuar.

Konfigurimi:
- **Më i afërti**: Rrumbullako në shumëfishin më të afërt të këtij numri (parazgjedhja: 1.0)
		]],
		["am"] = [[
የእያንዳንዱን የውሂብ ነጥብ ዋጋ ወደ ተገለጸው ቁጥር ቅርብ ወደሆነ ብዜት ያጠጋጋል።

ውቅር፦
- **ቅርብ**፦ ወደዚህ ቁጥር ቅርብ ወደሆነ ብዜት ያጠጋጋል (ነባሪ፦ 1.0)
		]],
		["hy"] = [[
Յուրաքանչյուր տվյալակետի արժեքը կլորացնում է մինչև նշված թվի ամենամոտ բազմապատիկը։

Կազմաձևում՝
- **Ամենամոտը**․ կլորացնել մինչև այս թվի ամենամոտ բազմապատիկը (կանխադրված՝ 1.0)
		]],
		["az"] = [[
Hər məlumat nöqtəsinin qiymətini göstərilən ədədin ən yaxın qatına yuvarlaqlaşdırır.

Konfiqurasiya:
- **Ən yaxın**: Bu ədədin ən yaxın qatına yuvarlaqlaşdır (standart: 1.0)
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্টের মানকে নির্দিষ্ট একটি সংখ্যার নিকটতম গুণিতকে রাউন্ড করে।

কনফিগারেশন:
- **নিকটতম**: এই সংখ্যার নিকটতম গুণিতকে রাউন্ড করুন (ডিফল্ট: 1.0)
		]],
		["eu"] = [[
Datu-puntu bakoitzaren balioa zehaztutako zenbaki baten hurbileneko multiplora biribiltzen du.

Konfigurazioa:
- **Hurbilena**: Biribildu zenbaki honen hurbileneko multiplora (lehenetsia: 1.0)
		]],
		["be"] = [[
Акругляе значэнне кожнай кропкі даных да бліжэйшага кратнага зададзенага ліку.

Канфігурацыя:
- **Бліжэйшае кратнае**: акругляць да бліжэйшага кратнага гэтага ліку (па змаўчанні: 1.0)
		]],
		["bg"] = [[
Закръгля стойността на всяка точка от данни до най-близкото кратно на зададено число.

Конфигурация:
- **Най-близко кратно**: Закръгляне до най-близкото кратно на това число (по подразбиране: 1.0)
		]],
		["my"] = [[
ဒေတာအမှတ်တစ်ခုချင်းစီ၏ တန်ဖိုးကို သတ်မှတ်ထားသော ကိန်း၏ အနီးဆုံးမြှောက်ကိန်းအထိ ပတ်လည်တန်ဖိုးချသည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **အနီးဆုံး**: ဤကိန်း၏ အနီးဆုံးမြှောက်ကိန်းအထိ ပတ်လည်တန်ဖိုးချရန် (မူလ: 1.0)
		]],
		["ca"] = [[
Arrodoneix el valor de cada punt de dades fins al múltiple més proper d’un nombre especificat.

Configuració:
- **Múltiple més proper**: Arrodoneix fins al múltiple més proper d’aquest nombre (per defecte: 1.0)
		]],
		["zh-Hans"] = [[
将每个数据点的值四舍五入到指定数字的最近倍数。

配置：
- **最近倍数**：四舍五入到此数字的最近倍数（默认：1.0）
		]],
		["zh-Hant"] = [[
將每個資料點的值四捨五入至指定數字的最接近倍數。

設定：
- **最接近倍數**：四捨五入至此數字的最接近倍數（預設：1.0）
		]],
		["hr"] = [[
Zaokružuje vrijednost svake podatkovne točke na najbliži višekratnik zadanog broja.

Konfiguracija:
- **Najbliži višekratnik**: Zaokruživanje na najbliži višekratnik ovog broja (zadano: 1.0)
		]],
		["cs"] = [[
Zaokrouhlí hodnotu každého datového bodu na nejbližší násobek zadaného čísla.

Konfigurace:
- **Nejbližší násobek**: Zaokrouhlit na nejbližší násobek tohoto čísla (výchozí: 1.0)
		]],
		["da"] = [[
Afrunder hver datapunkts værdi til det nærmeste multiplum af et angivet tal.

Konfiguration:
- **Nærmeste**: Afrund til det nærmeste multiplum af dette tal (standard: 1.0)
		]],
		["nl"] = [[
Rondt de waarde van elk datapunt af op het dichtstbijzijnde veelvoud van een opgegeven getal.

Configuratie:
- **Dichtstbijzijnde**: Afronden op het dichtstbijzijnde veelvoud van dit getal (standaard: 1.0)
		]],
		["et"] = [[
Ümardab iga andmepunkti väärtuse määratud arvu lähima kordkorrani.

Seadistus:
- **Lähim kordkord**: ümardamine selle arvu lähima kordkorrani (vaikimisi: 1,0)
		]],
		["fil"] = [[
Pina-round ang halaga ng bawat data point sa pinakamalapit na multiple ng tinukoy na numero.

Configuration:
- **Nearest**: I-round sa pinakamalapit na multiple ng numerong ito (default: 1.0)
		]],
		["fi"] = [[
Pyöristää kunkin datapisteen arvon määritetyn luvun lähimpään monikertaan.

Määritys:
- **Lähin monikerta**: Pyöristä tämän luvun lähimpään monikertaan (oletus: 1.0)
		]],
		["fr"] = [[
Arrondit la valeur de chaque point de données au multiple le plus proche d’un nombre indiqué.

Configuration :
- **Multiple** : Arrondir au multiple le plus proche de ce nombre (par défaut : 1.0)
		]],
		["gl"] = [[
Redondea o valor de cada punto de datos ao múltiplo máis próximo dun número especificado.

Configuración:
- **Múltiplo máis próximo**: Redondear ao múltiplo máis próximo deste número (predeterminado: 1.0)
		]],
		["ka"] = [[
თითოეული მონაცემის წერტილის მნიშვნელობას ამრგვალებს მითითებული რიცხვის უახლოეს ჯერადამდე.

კონფიგურაცია:
- **უახლოესი**: ამ რიცხვის უახლოეს ჯერადამდე დამრგვალება (ნაგულისხმევი: 1.0)
		]],
		["de"] = [[
Rundet den Wert jedes Datenpunkts auf das nächste Vielfache einer angegebenen Zahl.

Konfiguration:
- **Nächstes Vielfaches**: Auf das nächste Vielfache dieser Zahl runden (Standard: 1.0)
		]],
		["el"] = [[
Στρογγυλοποιεί την τιμή κάθε σημείου δεδομένων στο πλησιέστερο πολλαπλάσιο ενός καθορισμένου αριθμού.

Διαμόρφωση:
- **Πλησιέστερο**: Στρογγυλοποίηση στο πλησιέστερο πολλαπλάσιο αυτού του αριθμού (προεπιλογή: 1.0)
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટના મૂલ્યને નિર્દિષ્ટ સંખ્યાના સૌથી નજીકના ગુણાંક સુધી ગોળ કરે છે.

ગોઠવણી:
- **નજીકનો ગુણાંક**: આ સંખ્યાના સૌથી નજીકના ગુણાંક સુધી ગોળ કરો (ડિફૉલ્ટ: 1.0)
		]],
		["hi"] = [[
हर डेटा पॉइंट के मान को निर्दिष्ट संख्या के निकटतम गुणज तक पूर्णांकित करता है।

कॉन्फ़िगरेशन:
- **निकटतम**: इस संख्या के निकटतम गुणज तक पूर्णांकित करें (डिफ़ॉल्ट: 1.0)
		]],
		["hu"] = [[
Az egyes adatpontok értékét a megadott szám legközelebbi többszörösére kerekíti.

Konfiguráció:
- **Legközelebbi**: Kerekítés a szám legközelebbi többszörösére (alapértelmezett: 1.0)
		]],
		["is"] = [[
Námundar gildi hvers gagnapunkts að næsta margfeldi tilgreindrar tölu.

Stillingar:
- **Næsta margfeldi**: Námunda að næsta margfeldi þessarar tölu (sjálfgefið: 1.0)
		]],
		["id"] = [[
Membulatkan nilai setiap titik data ke kelipatan terdekat dari angka yang ditentukan.

Konfigurasi:
- **Terdekat**: Membulatkan ke kelipatan terdekat dari angka ini (default: 1.0)
		]],
		["it"] = [[
Arrotonda il valore di ogni punto dati al multiplo più vicino di un numero specificato.

Configurazione:
- **Più vicino**: arrotonda al multiplo più vicino di questo numero (predefinito: 1.0)
		]],
		["ja"] = [[
各データポイントの値を、指定した数の最も近い倍数に丸めます。

設定:
- **最近傍**: この数の最も近い倍数に丸めます（デフォルト: 1.0）
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯವನ್ನು ನಿರ್ದಿಷ್ಟ ಸಂಖ್ಯೆಯ ಸಮೀಪದ ಗುಣಕದವರೆಗೆ ಪೂರ್ಣಾಂಕಗೊಳಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಸಮೀಪದ ಗುಣಕ**: ಈ ಸಂಖ್ಯೆಯ ಸಮೀಪದ ಗುಣಕದವರೆಗೆ ಪೂರ್ಣಾಂಕಗೊಳಿಸಿ (ಡೀಫಾಲ್ಟ್: 1.0)
		]],
		["kk"] = [[
Әр дерек нүктесінің мәнін көрсетілген санның ең жақын еселігіне дөңгелектейді.

Конфигурация:
- **Ең жақын еселік**: Осы санның ең жақын еселігіне дөңгелектеу (әдепкі: 1.0)
		]],
		["km"] = [[
បង្គត់តម្លៃរបស់ចំណុចទិន្នន័យនីមួយៗទៅរកពហុគុណដែលជិតបំផុតនៃលេខដែលបានបញ្ជាក់។

ការកំណត់រចនា៖
- **ជិតបំផុត**៖ បង្គត់ទៅរកពហុគុណដែលជិតបំផុតនៃលេខនេះ (លំនាំដើម៖ 1.0)
		]],
		["ko"] = [[
각 데이터 포인트의 값을 지정한 수의 가장 가까운 배수로 반올림합니다.

구성:
- **가장 가까운 배수**: 이 수의 가장 가까운 배수로 반올림합니다(기본값: 1.0)
		]],
		["ky"] = [[
Ар бир маалымат чекитинин маанисин көрсөтүлгөн сандын эң жакын эселигине тегеректейт.

Тууралоо:
- **Эң жакын**: Ушул сандын эң жакын эселигине тегеректөө (демейки: 1.0)
		]],
		["lo"] = [[
ປັດຄ່າຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດໄປຫາຈຳນວນທະວີຄູນທີ່ໃກ້ທີ່ສຸດຂອງຈຳນວນທີ່ກຳນົດ.

ການກຳນົດຄ່າ:
- **ໃກ້ທີ່ສຸດ**: ປັດໄປຫາຈຳນວນທະວີຄູນທີ່ໃກ້ທີ່ສຸດຂອງຈຳນວນນີ້ (ຄ່າເລີ່ມຕົ້ນ: 1.0)
		]],
		["lv"] = [[
Noapaļo katra datu punkta vērtību līdz tuvākajam norādītā skaitļa daudzkārtnim.

Konfigurācija:
- **Tuvākais**: noapaļot līdz tuvākajam šī skaitļa daudzkārtnim (noklusējums: 1.0)
		]],
		["lt"] = [[
Kiekvieno duomenų taško reikšmę suapvalina iki artimiausio nurodyto skaičiaus kartotinio.

Konfigūracija:
- **Artimiausias kartotinis**: Skaičius, kurio kartotinio link apvalinti (numatytoji reikšmė: 1.0)
		]],
		["mk"] = [[
Ја заокружува вредноста на секоја точка на податоци до најблискиот умножок на зададен број.

Конфигурација:
- **Најблизок умножок**: Заокружи до најблискиот умножок на овој број (стандардно: 1.0)
		]],
		["ms"] = [[
Membundarkan nilai setiap titik data kepada gandaan terdekat bagi nombor yang ditentukan.

Konfigurasi:
- **Gandaan Terdekat**: Bundarkan kepada gandaan terdekat bagi nombor ini (lalai: 1.0)
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യം നിർദ്ദിഷ്ട സംഖ്യയുടെ ഏറ്റവും അടുത്ത ഗുണിതത്തിലേക്ക് റൗണ്ട് ചെയ്യുന്നു.

കോൺഫിഗറേഷൻ:
- **Nearest**: ഈ സംഖ്യയുടെ ഏറ്റവും അടുത്ത ഗുണിതത്തിലേക്ക് റൗണ്ട് ചെയ്യുക (സ്ഥിരസ്ഥിതി: 1.0)
		]],
		["mr"] = [[
प्रत्येक डेटा पॉइंटचे मूल्य निर्दिष्ट संख्येच्या सर्वात जवळच्या पटीत पूर्णांकित करते.

कॉन्फिगरेशन:
- **सर्वात जवळची पटी**: या संख्येच्या सर्वात जवळच्या पटीत पूर्णांकित करा (डीफॉल्ट: 1.0)
		]],
		["mn"] = [[
Өгөгдлийн цэг бүрийн утгыг заасан тооны хамгийн ойрын үржвэр хүртэл тоймлоно.

Тохиргоо:
- **Хамгийн ойр**: Энэ тооны хамгийн ойрын үржвэр хүртэл тоймлох (анхдагч: 1.0)
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दुको मानलाई निर्दिष्ट संख्याको सबैभन्दा नजिकको गुणजमा राउन्ड गर्छ।

कन्फिगरेसन:
- **नजिकको**: यस संख्याको सबैभन्दा नजिकको गुणजमा राउन्ड गर्ने (पूर्वनिर्धारित: 1.0)
		]],
		["no"] = [[
Runder verdien til hvert datapunkt til nærmeste multiplum av et angitt tall.

Konfigurasjon:
- **Nærmeste**: Rund til nærmeste multiplum av dette tallet (standard: 1.0)
		]],
		["pl"] = [[
Zaokrągla wartość każdego punktu danych do najbliższej wielokrotności określonej liczby.

Konfiguracja:
- **Najbliższa wielokrotność**: Zaokrąglaj do najbliższej wielokrotności tej liczby (domyślnie: 1.0)
		]],
		["pt"] = [[
Arredonda o valor de cada ponto de dados para o múltiplo mais próximo de um número especificado.

Configuração:
- **Mais próximo**: Arredondar para o múltiplo mais próximo deste número (predefinição: 1.0)
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਨੂੰ ਨਿਰਧਾਰਤ ਸੰਖਿਆ ਦੇ ਸਭ ਤੋਂ ਨੇੜਲੇ ਗੁਣਜ ਤੱਕ ਰਾਊਂਡ ਕਰਦਾ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਨੇੜਲਾ ਗੁਣਜ**: ਇਸ ਸੰਖਿਆ ਦੇ ਸਭ ਤੋਂ ਨੇੜਲੇ ਗੁਣਜ ਤੱਕ ਰਾਊਂਡ ਕਰੋ (ਮੂਲ: 1.0)
		]],
		["ro"] = [[
Rotunjește valoarea fiecărui punct de date la cel mai apropiat multiplu al unui număr specificat.

Configurare:
- **Cel mai apropiat**: Rotunjește la cel mai apropiat multiplu al acestui număr (implicit: 1.0)
		]],
		["rm"] = [[
Runda la valur da mintga punct da datas al multipel il pli datiers d'in numer spezificà.

Configuraziun:
- **Il pli datiers**: Rundar al multipel il pli datiers da quest numer (standard: 1.0)
		]],
		["ru"] = [[
Округляет значение каждой точки данных до ближайшего кратного заданного числа.

Конфигурация:
- **Ближайшее кратное**: Округлять до ближайшего кратного этого числа (по умолчанию: 1.0)
		]],
		["sr"] = [[
Zaokružuje vrednost svake tačke podataka na najbliži umnožak zadatog broja.

Konfiguracija:
- **Najbliži umnožak**: Zaokruživanje na najbliži umnožak ovog broja (podrazumevano: 1.0)
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයකම අගය නිශ්චිත සංඛ්‍යාවක ආසන්නතම ගුණාකාරයට වට කරයි.

වින්‍යාසය:
- **ආසන්නතම**: මෙම සංඛ්‍යාවේ ආසන්නතම ගුණාකාරයට වට කරන්න (පෙරනිමිය: 1.0)
		]],
		["sk"] = [[
Zaokrúhli hodnotu každého údajového bodu na najbližší násobok zadaného čísla.

Konfigurácia:
- **Najbližší násobok**: Zaokrúhli na najbližší násobok tohto čísla (predvolené: 1.0)
		]],
		["sl"] = [[
Zaokroži vrednost vsake podatkovne točke na najbližji večkratnik določenega števila.

Konfiguracija:
- **Najbližji večkratnik**: Zaokroži na najbližji večkratnik tega števila (privzeto: 1.0)
		]],
		["es"] = [[
Redondea el valor de cada punto de datos al múltiplo más cercano de un número especificado.

Configuración:
- **Múltiplo**: Redondear al múltiplo más cercano de este número (predeterminado: 1.0)
		]],
		["sw"] = [[
Huzungusha thamani ya kila nukta ya data hadi kizidisho cha karibu zaidi cha nambari maalum.

Usanidi:
- **Karibu**: Zungusha hadi kizidisho cha karibu zaidi cha nambari hii (chaguo-msingi: 1.0)
		]],
		["sv"] = [[
Avrundar värdet för varje datapunkt till närmaste multipel av ett angivet tal.

Konfiguration:
- **Närmaste**: Avrunda till närmaste multipel av detta tal (standard: 1.0)
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளியின் மதிப்பையும் குறிப்பிட்ட எண்ணின் அருகிலுள்ள மடங்கிற்கு முழுமையாக்குகிறது.

உள்ளமைவு:
- **அருகிலுள்ள மடங்கு**: இந்த எண்ணின் அருகிலுள்ள மடங்கிற்கு முழுமையாக்கு (இயல்புநிலை: 1.0)
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్ విలువను పేర్కొన్న సంఖ్యకు సమీప గుణితానికి రౌండ్ చేస్తుంది.

కాన్ఫిగరేషన్:
- **సమీప గుణితం**: ఈ సంఖ్యకు సమీప గుణితానికి రౌండ్ చేయండి (డిఫాల్ట్: 1.0)
		]],
		["th"] = [[
ปัดค่าของจุดข้อมูลแต่ละจุดเป็นพหุคูณที่ใกล้ที่สุดของตัวเลขที่กำหนด

การกำหนดค่า:
- **พหุคูณที่ใกล้ที่สุด**: ปัดเป็นพหุคูณที่ใกล้ที่สุดของตัวเลขนี้ (ค่าเริ่มต้น: 1.0)
		]],
		["tr"] = [[
Her veri noktasının değerini, belirtilen sayının en yakın katına yuvarlar.

Yapılandırma:
- **En Yakın**: Bu sayının en yakın katına yuvarla (varsayılan: 1.0)
		]],
		["uk"] = [[
Округлює значення кожної точки даних до найближчого кратного заданого числа.

Конфігурація:
- **Найближче кратне**: Округлювати до найближчого кратного цього числа (типово: 1.0)
		]],
		["vi"] = [[
Làm tròn giá trị của từng điểm dữ liệu đến bội số gần nhất của một số được chỉ định.

Cấu hình:
- **Bội số gần nhất**: Làm tròn đến bội số gần nhất của số này (mặc định: 1.0)
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
				["am"] = "ቅርብ",
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
				["lo"] = "ໃກ້ທີ່ສຸດ",
				["lv"] = "Tuvākais",
				["lt"] = "Artimiausias kartotinis",
				["mk"] = "Најблизок умножок",
				["ms"] = "Gandaan Terdekat",
				["ml"] = "ഏറ്റവും അടുത്തത്",
				["mr"] = "सर्वात जवळची पटी",
				["mn"] = "Хамгийн ойр",
				["ne"] = "नजिकको",
				["no"] = "Nærmeste",
				["pl"] = "Najbliższa wielokrotność",
				["pt"] = "Mais próximo",
				["pa"] = "ਨੇੜਲਾ ਗੁਣਜ",
				["ro"] = "Cel mai apropiat",
				["rm"] = "Il pli datiers",
				["ru"] = "Ближайшее кратное",
				["sr"] = "Najbliži umnožak",
				["si"] = "ආසන්නතම",
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

			-- Round to nearest multiple
			data_point.value = math.floor((data_point.value / nearest) + 0.5) * nearest

			return data_point
		end
	end,
}
