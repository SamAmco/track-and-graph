-- Lua Function to calculate time between data points
-- Outputs the duration in seconds between each data point and the previous one

local core = require("tng.core")
local checkbox = require("tng.config").checkbox

return {
	-- Configuration metadata
	id = "time-between",
	version = "1.0.2",
	inputCount = 1,
	categories = {"_time"},
	title = {
		["en"] = "Time Between",
		["af"] = "Tyd Tussenin",
		["sq"] = "Koha ndërmjet",
		["am"] = "በመካከላቸው ያለ ጊዜ",
		["hy"] = "Ժամանակը միջև",
		["az"] = "Arasındakı vaxt",
		["bn"] = "সময়ের ব্যবধান",
		["eu"] = "Denbora tartean",
		["be"] = "Час паміж",
		["bg"] = "Време между",
		["my"] = "အကြားအချိန်",
		["ca"] = "Temps entre punts",
		["zh-Hans"] = "时间间隔",
		["zh-Hant"] = "時間間隔",
		["hr"] = "Vrijeme između",
		["cs"] = "Čas mezi body",
		["da"] = "Tid mellem",
		["nl"] = "Tijd tussen",
		["et"] = "Ajavahe",
		["fil"] = "Oras sa Pagitan",
		["fi"] = "Aika välillä",
		["fr"] = "Temps entre les points",
		["gl"] = "Tempo entre puntos",
		["ka"] = "დროის შუალედი",
		["de"] = "Zeit zwischen Datenpunkten",
		["el"] = "Χρόνος μεταξύ",
		["gu"] = "વચ્ચેનો સમય",
		["hi"] = "बीच का समय",
		["hu"] = "Két időpont között",
		["is"] = "Tími á milli",
		["id"] = "Waktu Antara",
		["it"] = "Intervallo di tempo",
		["ja"] = "間隔時間",
		["kn"] = "ನಡುವಿನ ಸಮಯ",
		["kk"] = "Аралық уақыт",
		["km"] = "ចន្លោះពេល",
		["ko"] = "간 시간",
		["ky"] = "Арадагы убакыт",
		["lo"] = "ເວລາລະຫວ່າງ",
		["lv"] = "Laiks starp",
		["lt"] = "Laikas tarp",
		["mk"] = "Време помеѓу",
		["ms"] = "Masa Antara",
		["ml"] = "ഇടയിലെ സമയം",
		["mr"] = "मधील वेळ",
		["mn"] = "Хоорондох хугацаа",
		["ne"] = "बीचको समय",
		["no"] = "Tid mellom",
		["pl"] = "Czas między punktami",
		["pt"] = "Tempo entre",
		["pa"] = "ਵਿਚਕਾਰਲਾ ਸਮਾਂ",
		["ro"] = "Timp între puncte",
		["rm"] = "Temp tranter",
		["ru"] = "Время между точками",
		["sr"] = "Vreme između",
		["si"] = "අතර කාලය",
		["sk"] = "Čas medzi",
		["sl"] = "Čas med točkami",
		["es"] = "Tiempo entre puntos",
		["sw"] = "Muda Kati",
		["sv"] = "Tid mellan",
		["ta"] = "இடையிலான நேரம்",
		["te"] = "మధ్య సమయం",
		["th"] = "ช่วงเวลาระหว่าง",
		["tr"] = "Aradaki Süre",
		["uk"] = "Час між точками",
		["vi"] = "Thời gian giữa các điểm",
	},
	description = {
		["en"] = [[
Calculates the duration in seconds between each data point and the previous one. The output value is the time difference in seconds and can be treated as a duration.

Configuration:
- **Include Time to Last**: Include the time between now and the last data point (default: false)
		]],
		["af"] = [[
Bereken die duur in sekondes tussen elke datapunt en die vorige een. Die uitvoerwaarde is die tydsverskil in sekondes en kan as ’n duur hanteer word.

Konfigurasie:
- **Sluit Tyd tot Laaste In**: Sluit die tyd tussen nou en die laaste datapunt in (verstek: vals)
		]],
		["sq"] = [[
Llogarit kohëzgjatjen në sekonda midis çdo pike të të dhënave dhe pikës së mëparshme. Vlera dalëse është diferenca kohore në sekonda dhe mund të trajtohet si kohëzgjatje.

Konfigurimi:
- **Përfshi kohën deri te e fundit**: Përfshin kohën nga tani deri te pika e fundit e të dhënave (parazgjedhja: false)
		]],
		["am"] = [[
በእያንዳንዱ የውሂብ ነጥብና ከእሱ በፊት ባለው የውሂብ ነጥብ መካከል ያለውን ጊዜ በሰከንድ ያሰላል። የውጤት ዋጋው የጊዜ ልዩነቱ በሰከንድ ሲሆን እንደ የቆይታ ጊዜ ሊቆጠር ይችላል።

ውቅር፦
- **እስከ የመጨረሻው ጊዜ አካትት**፦ ከአሁን እስከ የመጨረሻው የውሂብ ነጥብ ያለውን ጊዜ አካትት (ነባሪ፦ false)
		]],
		["hy"] = [[
Հաշվում է յուրաքանչյուր տվյալակետի և նախորդի միջև տևողությունը՝ վայրկյաններով։ Ելքային արժեքը ժամանակային տարբերությունն է վայրկյաններով և կարող է դիտարկվել որպես տևողություն։

Կազմաձևում՝
- **Ներառել ժամանակը մինչև վերջինը**․ ներառել ներկա պահի և վերջին տվյալակետի միջև ժամանակը (կանխադրված՝ false)
		]],
		["az"] = [[
Hər məlumat nöqtəsi ilə əvvəlki məlumat nöqtəsi arasındakı müddəti saniyələrlə hesablayır. Çıxış qiyməti saniyələrlə vaxt fərqidir və müddət kimi qəbul edilə bilər.

Konfiqurasiya:
- **Sonuncuya qədərki vaxtı daxil et**: İndi ilə son məlumat nöqtəsi arasındakı vaxtı daxil et (standart: false)
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্ট এবং তার আগের ডেটা পয়েন্টের মধ্যে সেকেন্ডে সময়কাল গণনা করে। আউটপুট মান হলো সেকেন্ডে সময়ের পার্থক্য এবং এটিকে সময়কাল হিসেবে ব্যবহার করা যায়।

কনফিগারেশন:
- **সর্বশেষ পর্যন্ত সময় অন্তর্ভুক্ত করুন**: এখন থেকে সর্বশেষ ডেটা পয়েন্ট পর্যন্ত সময় অন্তর্ভুক্ত করুন (ডিফল্ট: false)
		]],
		["eu"] = [[
Datu-puntu bakoitzaren eta aurrekoaren arteko iraupena segundotan kalkulatzen du. Irteerako balioa denbora-aldea da, segundotan, eta iraupen gisa har daiteke.

Konfigurazioa:
- **Sartu azkenekorainoko denbora**: Sartu unetik azken datu-puntura arteko denbora (lehenetsia: false)
		]],
		["be"] = [[
Вылічвае працягласць у секундах паміж кожнай кропкай даных і папярэдняй. Выходнае значэнне — розніца ў часе ў секундах, якую можна разглядаць як працягласць.

Канфігурацыя:
- **Уключаць час да апошняй**: уключаць час паміж бягучым момантам і апошняй кропкай даных (па змаўчанні: false)
		]],
		["bg"] = [[
Изчислява продължителността в секунди между всяка точка от данни и предишната. Изходната стойност е разликата във времето в секунди и може да се третира като продължителност.

Конфигурация:
- **Включване на времето до последната**: Включва времето между настоящия момент и последната точка от данни (по подразбиране: false)
		]],
		["my"] = [[
ဒေတာအမှတ်တစ်ခုစီနှင့် ယခင်ဒေတာအမှတ်အကြား ကြာချိန်ကို စက္ကန့်ဖြင့် တွက်ချက်သည်။ ထွက်လာသောတန်ဖိုးသည် စက္ကန့်ဖြင့် အချိန်ကွာခြားချက်ဖြစ်ပြီး ကြာချိန်အဖြစ် သတ်မှတ်နိုင်သည်။

ဖွဲ့စည်းမှု:
- **နောက်ဆုံးအချိန်အထိ ထည့်သွင်းရန်**: ယခုအချိန်နှင့် နောက်ဆုံးဒေတာအမှတ်အကြား အချိန်ကို ထည့်သွင်းရန် (မူလ: false)
		]],
		["ca"] = [[
Calcula la durada en segons entre cada punt de dades i l’anterior. El valor de sortida és la diferència de temps en segons i es pot tractar com una durada.

Configuració:
- **Inclou el temps fins a l’últim**: Inclou el temps entre ara i l’últim punt de dades (per defecte: false)
		]],
		["zh-Hans"] = [[
计算每个数据点与前一个数据点之间的时长（秒）。输出值是以秒为单位的时间差，可视为时长。

配置：
- **包含到最后一个数据点的时间**：包含当前时间与最后一个数据点之间的时间（默认：false）
		]],
		["zh-Hant"] = [[
計算每個資料點與前一個資料點之間的持續時間（秒）。輸出值是以秒為單位的時間差，可視為持續時間。

設定：
- **包含至最後一筆的時間**：包含現在與最後一個資料點之間的時間（預設：false）
		]],
		["hr"] = [[
Izračunava trajanje u sekundama između svake podatkovne točke i prethodne. Izlazna vrijednost vremenska je razlika u sekundama i može se tretirati kao trajanje.

Konfiguracija:
- **Uključi vrijeme do posljednje**: Uključi vrijeme između sadašnjeg trenutka i posljednje podatkovne točke (zadano: false)
		]],
		["cs"] = [[
Vypočítá dobu v sekundách mezi každým datovým bodem a předchozím bodem. Výstupní hodnota je časový rozdíl v sekundách a lze s ní zacházet jako s délkou.

Konfigurace:
- **Zahrnout čas do posledního**: Zahrnout čas mezi nynějškem a posledním datovým bodem (výchozí: false)
		]],
		["da"] = [[
Beregner varigheden i sekunder mellem hvert datapunkt og det forrige. Outputværdien er tidsforskellen i sekunder og kan behandles som en varighed.

Konfiguration:
- **Medtag tid til seneste**: Medtag tiden mellem nu og det seneste datapunkt (standard: false)
		]],
		["nl"] = [[
Berekent de duur in seconden tussen elk gegevenspunt en het vorige. De uitvoerwaarde is het tijdsverschil in seconden en kan als een duur worden behandeld.

Configuratie:
- **Tijd tot laatste opnemen**: Neem de tijd tussen nu en het laatste gegevenspunt op (standaard: false)
		]],
		["et"] = [[
Arvutab iga andmepunkti ja sellele eelneva punkti vahelise kestuse sekundites. Väljundväärtus on ajavahe sekundites ja seda võib käsitleda kestusena.

Seadistus:
- **Kaasa aeg viimase punktini**: kaasa praeguse hetke ja viimase andmepunkti vaheline aeg (vaikimisi: väär)
		]],
		["fil"] = [[
Kinakalkula ang duration sa segundo sa pagitan ng bawat data point at ng nauna rito. Ang output value ay ang pagkakaiba ng oras sa segundo at maaaring ituring na duration.

Configuration:
- **Include Time to Last**: Isama ang oras mula ngayon hanggang sa huling data point (default: false)
		]],
		["fi"] = [[
Laskee kunkin datapisteen ja edellisen datapisteen välisen keston sekunteina. Tuloksen arvo on sekunteina ilmaistu aikaero, ja sitä voidaan käsitellä kestona.

Määritys:
- **Sisällytä aika viimeiseen**: Sisällytä nykyhetken ja viimeisen datapisteen välinen aika (oletus: false)
		]],
		["fr"] = [[
Calcule la durée en secondes entre chaque point de données et le précédent. La valeur produite correspond à la différence de temps en secondes et peut être traitée comme une durée.

Configuration :
- **Inclure le temps jusqu’au dernier** : Inclure le temps écoulé entre maintenant et le dernier point de données (par défaut : false)
		]],
		["gl"] = [[
Calcula a duración en segundos entre cada punto de datos e o anterior. O valor de saída é a diferenza temporal en segundos e pode tratarse como unha duración.

Configuración:
- **Incluír o tempo ata o último**: Incluír o tempo entre agora e o último punto de datos (predeterminado: false)
		]],
		["ka"] = [[
ითვლის წამებში გამოხატულ ხანგრძლივობას თითოეულ მონაცემის წერტილსა და მის წინა წერტილს შორის. გამოტანილი მნიშვნელობა არის დროის სხვაობა წამებში და შეიძლება ხანგრძლივობად განიხილებოდეს.

კონფიგურაცია:
- **ბოლომდე დროის ჩართვა**: ახლა მომენტსა და ბოლო მონაცემის წერტილს შორის დროის ჩართვა (ნაგულისხმევი: false)
		]],
		["de"] = [[
Berechnet die Dauer in Sekunden zwischen jedem Datenpunkt und dem vorherigen. Der Ausgabewert ist die Zeitdifferenz in Sekunden und kann als Dauer behandelt werden.

Konfiguration:
- **Zeit bis zum letzten einschließen**: Die Zeit zwischen jetzt und dem letzten Datenpunkt einschließen (Standard: false)
		]],
		["el"] = [[
Υπολογίζει τη διάρκεια σε δευτερόλεπτα μεταξύ κάθε σημείου δεδομένων και του προηγούμενου. Η τιμή εξόδου είναι η χρονική διαφορά σε δευτερόλεπτα και μπορεί να αντιμετωπιστεί ως διάρκεια.

Διαμόρφωση:
- **Συμπερίληψη χρόνου έως το τελευταίο**: Συμπεριλαμβάνει τον χρόνο μεταξύ τώρα και του τελευταίου σημείου δεδομένων (προεπιλογή: false)
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટ અને તેના અગાઉના પોઇન્ટ વચ્ચેની અવધિ સેકન્ડમાં ગણે છે. આઉટપુટ મૂલ્ય સેકન્ડમાં સમયનો તફાવત છે અને તેને અવધિ તરીકે ગણવામાં આવી શકે છે.

ગોઠવણી:
- **છેલ્લા પોઇન્ટ સુધીનો સમય સામેલ કરો**: અત્યારે અને છેલ્લા ડેટા પોઇન્ટ વચ્ચેનો સમય સામેલ કરો (ડિફૉલ્ટ: false)
		]],
		["hi"] = [[
हर डेटा पॉइंट और पिछले डेटा पॉइंट के बीच सेकंड में अवधि की गणना करता है। आउटपुट मान सेकंड में समय-अंतर होता है और इसे अवधि के रूप में माना जा सकता है।

कॉन्फ़िगरेशन:
- **अंतिम डेटा पॉइंट तक का समय शामिल करें**: अभी और अंतिम डेटा पॉइंट के बीच का समय शामिल करें (डिफ़ॉल्ट: false)
		]],
		["hu"] = [[
Kiszámítja az egyes adatpontok és az őket megelőző adatpont közötti időtartamot másodpercben. A kimeneti érték a másodpercben megadott időeltérés, és időtartamként kezelhető.

Konfiguráció:
- **Utolsó adatpontig hátralévő idő belefoglalása**: A jelenlegi időpont és az utolsó adatpont közötti idő belefoglalása (alapértelmezett: false)
		]],
		["is"] = [[
Reiknar út tímalengd í sekúndum milli hvers gagnapunkts og þess fyrri. Úttaksgildið er tímamunurinn í sekúndum og má meðhöndla sem tímalengd.

Stillingar:
- **Tími að síðasta**: Taka með tímann frá núinu að síðasta gagnapunkti (sjálfgefið: false)
		]],
		["id"] = [[
Menghitung durasi dalam detik antara setiap titik data dan titik sebelumnya. Nilai keluaran adalah selisih waktu dalam detik dan dapat diperlakukan sebagai durasi.

Konfigurasi:
- **Sertakan Waktu hingga Terakhir**: Sertakan waktu antara sekarang dan titik data terakhir (default: false)
		]],
		["it"] = [[
Calcola la durata in secondi tra ogni punto dati e quello precedente. Il valore di output è la differenza di tempo in secondi e può essere trattato come una durata.

Configurazione:
- **Includi il tempo fino all'ultimo**: include il tempo tra ora e l'ultimo punto dati (predefinito: false)
		]],
		["ja"] = [[
各データポイントと直前のデータポイントとの間の継続時間を秒単位で計算します。出力値は秒単位の時間差で、継続時間として扱えます。

設定:
- **最後のデータポイントまでの時間を含める**: 現在時刻から最後のデータポイントまでの時間を含める（デフォルト: false）
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದು ಮತ್ತು ಅದರ ಹಿಂದಿನ ಡೇಟಾ ಬಿಂದುವಿನ ನಡುವಿನ ಅವಧಿಯನ್ನು ಸೆಕೆಂಡುಗಳಲ್ಲಿ ಲೆಕ್ಕಹಾಕುತ್ತದೆ. ಔಟ್‌ಪುಟ್ ಮೌಲ್ಯವು ಸೆಕೆಂಡುಗಳಲ್ಲಿನ ಸಮಯ ವ್ಯತ್ಯಾಸವಾಗಿದ್ದು, ಅದನ್ನು ಅವಧಿಯಾಗಿ ಪರಿಗಣಿಸಬಹುದು.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಕೊನೆಯದಕ್ಕೆ ಸಮಯವನ್ನು ಒಳಗೊಂಡಿರಿ**: ಈಗಿನಿಂದ ಕೊನೆಯ ಡೇಟಾ ಬಿಂದುವಿನವರೆಗಿನ ಸಮಯವನ್ನು ಒಳಗೊಂಡಿರಿ (ಡೀಫಾಲ್ಟ್: false)
		]],
		["kk"] = [[
Әр дерек нүктесі мен алдыңғы дерек нүктесінің арасындағы ұзақтықты секундпен есептейді. Шығыс мәні — секундпен берілген уақыт айырмасы және оны ұзақтық ретінде қарастыруға болады.

Конфигурация:
- **Соңғысына дейінгі уақытты қосу**: Қазіргі уақыт пен соңғы дерек нүктесінің арасындағы уақытты қосу (әдепкі: false)
		]],
		["km"] = [[
គណនារយៈពេលជាវិនាទីរវាងចំណុចទិន្នន័យនីមួយៗ និងចំណុចមុន។ តម្លៃលទ្ធផលគឺភាពខុសគ្នានៃពេលវេលាជាវិនាទី ហើយអាចចាត់ទុកជារយៈពេលបាន។

ការកំណត់រចនា៖
- **រួមបញ្ចូលពេលវេលាដល់ចំណុចចុងក្រោយ**៖ រួមបញ្ចូលពេលវេលារវាងពេលនេះ និងចំណុចទិន្នន័យចុងក្រោយ (លំនាំដើម៖ false)
		]],
		["ko"] = [[
각 데이터 포인트와 이전 데이터 포인트 사이의 기간을 초 단위로 계산합니다. 출력 값은 초 단위 시간 차이이며 기간으로 취급할 수 있습니다.

구성:
- **마지막 항목까지의 시간 포함**: 현재 시각과 마지막 데이터 포인트 사이의 시간을 포함합니다(기본값: false)
		]],
		["ky"] = [[
Ар бир маалымат чекити менен анын мурунку маалымат чекитинин ортосундагы узактыкты секунддар менен эсептейт. Чыгуу мааниси секунддардагы убакыт айырмасы болуп, узактык катары каралышы мүмкүн.

Тууралоо:
- **Акыркы чекитке чейинки убакытты кошуу**: Азыркы убакыт менен акыркы маалымат чекитинин ортосундагы убакытты кошуу (демейки: false)
		]],
		["lo"] = [[
ຄຳນວນໄລຍະເວລາເປັນວິນາທີລະຫວ່າງຈຸດຂໍ້ມູນແຕ່ລະຈຸດກັບຈຸດກ່ອນໜ້າ. ຄ່າຜົນລັບແມ່ນຄວາມແຕກຕ່າງຂອງເວລາເປັນວິນາທີ ແລະສາມາດໃຊ້ເປັນໄລຍະເວລາໄດ້.

ການກຳນົດຄ່າ:
- **ລວມເວລາເຖິງຈຸດສຸດທ້າຍ**: ລວມເວລາລະຫວ່າງຕອນນີ້ກັບຈຸດຂໍ້ມູນສຸດທ້າຍ (ຄ່າເລີ່ມຕົ້ນ: false)
		]],
		["lv"] = [[
Aprēķina ilgumu sekundēs starp katru datu punktu un iepriekšējo. Izvades vērtība ir laika starpība sekundēs, un to var izmantot kā ilgumu.

Konfigurācija:
- **Iekļaut laiku līdz pēdējam**: iekļaut laiku no šī brīža līdz pēdējam datu punktam (noklusējums: false)
		]],
		["lt"] = [[
Apskaičiuoja trukmę sekundėmis tarp kiekvieno duomenų taško ir ankstesnio. Išvesties reikšmė yra laiko skirtumas sekundėmis ir gali būti laikoma trukme.

Konfigūracija:
- **Įtraukti laiką iki paskutinio**: Įtraukti laiką nuo dabar iki paskutinio duomenų taško (numatyta: false)
		]],
		["mk"] = [[
Го пресметува времетраењето во секунди помеѓу секоја точка на податоци и претходната. Излезната вредност е временската разлика во секунди и може да се третира како времетраење.

Конфигурација:
- **Вклучи време до последната**: Вклучи го времето помеѓу сега и последната точка на податоци (стандардно: false)
		]],
		["ms"] = [[
Mengira tempoh dalam saat antara setiap titik data dengan titik data sebelumnya. Nilai output ialah perbezaan masa dalam saat dan boleh dianggap sebagai tempoh.

Konfigurasi:
- **Sertakan Masa hingga Terakhir**: Sertakan masa antara sekarang dengan titik data terakhir (lalai: false)
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിനും മുമ്പത്തേതിനുമിടയിലെ ദൈർഘ്യം സെക്കൻഡുകളിൽ കണക്കാക്കുന്നു. ഔട്ട്പുട്ട് മൂല്യം സെക്കൻഡുകളിലെ സമയവ്യത്യാസമാണ്, അത് duration ആയി പരിഗണിക്കാം.

കോൺഫിഗറേഷൻ:
- **Include Time to Last**: ഇപ്പോഴത്തെ സമയത്തിനും അവസാന ഡാറ്റാ പോയിന്റിനുമിടയിലെ സമയം ഉൾപ്പെടുത്തുക (സ്ഥിരസ്ഥിതി: false)
		]],
		["mr"] = [[
प्रत्येक डेटा पॉइंट आणि त्याच्या आधीच्या पॉइंटमधील कालावधी सेकंदांत मोजते. आउटपुट मूल्य सेकंदांमधील वेळेचा फरक असतो आणि कालावधी म्हणून वापरता येतो.

कॉन्फिगरेशन:
- **शेवटपर्यंतचा वेळ समाविष्ट करा**: आता आणि शेवटच्या डेटा पॉइंटमधील वेळ समाविष्ट करा (डीफॉल्ट: false)
		]],
		["mn"] = [[
Өгөгдлийн цэг бүр болон өмнөх цэгийн хоорондох хугацааг секундээр тооцоолно. Гаралтын утга нь секунд дэх хугацааны зөрүү бөгөөд хугацаа гэж үзэж болно.

Тохиргоо:
- **Сүүлийн цэг хүртэлх хугацааг оруулах**: Одоогоос сүүлийн өгөгдлийн цэг хүртэлх хугацааг оруулах (анхдагч: false)
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दु र अघिल्लो डेटा बिन्दुबीचको अवधि सेकेन्डमा गणना गर्छ। आउटपुट मान सेकेन्डमा समय अन्तर हो र यसलाई अवधिका रूपमा प्रयोग गर्न सकिन्छ।

कन्फिगरेसन:
- **अन्तिमसम्मको समय समावेश गर्ने**: अहिले र अन्तिम डेटा बिन्दुबीचको समय समावेश गर्ने (पूर्वनिर्धारित: false)
		]],
		["no"] = [[
Beregner varigheten i sekunder mellom hvert datapunkt og det forrige. Utgangsverdien er tidsforskjellen i sekunder og kan behandles som en varighet.

Konfigurasjon:
- **Inkluder tid til siste**: Inkluder tiden mellom nå og det siste datapunktet (standard: false)
		]],
		["pl"] = [[
Oblicza czas trwania w sekundach między każdym punktem danych a poprzednim punktem. Wartością wyjściową jest różnica czasu w sekundach i można ją traktować jako czas trwania.

Konfiguracja:
- **Uwzględnij czas do ostatniego**: Uwzględnij czas między teraz a ostatnim punktem danych (domyślnie: false)
		]],
		["pt"] = [[
Calcula a duração em segundos entre cada ponto de dados e o anterior. O valor de saída é a diferença de tempo em segundos e pode ser tratado como uma duração.

Configuração:
- **Incluir tempo até ao último**: Incluir o tempo entre agora e o último ponto de dados (predefinição: false)
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਅਤੇ ਇਸ ਤੋਂ ਪਿਛਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਵਿਚਕਾਰ ਸਕਿੰਟਾਂ ਵਿੱਚ ਅੰਤਰਾਲ ਗਿਣਦਾ ਹੈ। ਆਉਟਪੁੱਟ ਮੁੱਲ ਸਕਿੰਟਾਂ ਵਿੱਚ ਸਮੇਂ ਦਾ ਅੰਤਰ ਹੁੰਦਾ ਹੈ ਅਤੇ ਇਸਨੂੰ ਅੰਤਰਾਲ ਵਜੋਂ ਵਰਤਿਆ ਜਾ ਸਕਦਾ ਹੈ।

ਸੰਰਚਨਾ:
- **ਆਖਰੀ ਤੱਕ ਦਾ ਸਮਾਂ ਸ਼ਾਮਲ ਕਰੋ**: ਹੁਣ ਅਤੇ ਆਖਰੀ ਡਾਟਾ ਪੁਆਇੰਟ ਵਿਚਕਾਰ ਦਾ ਸਮਾਂ ਸ਼ਾਮਲ ਕਰੋ (ਡਿਫ਼ਾਲਟ: false)
		]],
		["ro"] = [[
Calculează durata în secunde dintre fiecare punct de date și cel anterior. Valoarea rezultată este diferența de timp în secunde și poate fi tratată ca durată.

Configurare:
- **Include timpul până la ultimul**: Include timpul dintre momentul actual și ultimul punct de date (implicit: false)
		]],
		["rm"] = [[
Calcula la durada en secundas tranter mintga punct da datas ed il precedent. La valur da sortida è la differenza da temp en secundas e po vegnir tractada sco ina durada.

Configuraziun:
- **Includer il temp fin a l'ultim**: Includer il temp tranter uss e l'ultim punct da datas (defaut: false)
		]],
		["ru"] = [[
Вычисляет продолжительность в секундах между каждой точкой данных и предыдущей. Выходное значение — разница во времени в секундах, которую можно рассматривать как длительность.

Конфигурация:
- **Включать время до последней**: Включать время между текущим моментом и последней точкой данных (по умолчанию: false)
		]],
		["sr"] = [[
Izračunava trajanje u sekundama između svake tačke podataka i prethodne tačke. Izlazna vrednost je vremenska razlika u sekundama i može se tretirati kao trajanje.

Konfiguracija:
- **Uključi vreme do poslednje**: Uključuje vreme između sadašnjeg trenutka i poslednje tačke podataka (podrazumevano: false)
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයක් සහ ඊට පෙර දත්ත ලක්ෂ්‍යය අතර තත්පරවල කාල සීමාව ගණනය කරයි. ප්‍රතිදාන අගය තත්පරවල කාල වෙනස වන අතර එය කාල සීමාවක් ලෙස සැලකිය හැක.

වින්‍යාසය:
- **අවසන් එක දක්වා කාලය ඇතුළත් කරන්න**: දැන් සහ අවසන් දත්ත ලක්ෂ්‍යය අතර කාලය ඇතුළත් කරන්න (පෙරනිමිය: false)
		]],
		["sk"] = [[
Vypočíta trvanie v sekundách medzi každým údajovým bodom a predchádzajúcim bodom. Výstupná hodnota je časový rozdiel v sekundách a možno s ňou zaobchádzať ako s trvaním.

Konfigurácia:
- **Zahrnúť čas po posledný**: Zahrnúť čas medzi aktuálnym časom a posledným údajovým bodom (predvolené: false)
		]],
		["sl"] = [[
Izračuna trajanje v sekundah med vsako podatkovno točko in prejšnjo. Izhodna vrednost je časovna razlika v sekundah in jo je mogoče obravnavati kot trajanje.

Konfiguracija:
- **Vključi čas do zadnje točke**: Vključi čas med zdaj in zadnjo podatkovno točko (privzeto: false)
		]],
		["es"] = [[
Calcula la duración en segundos entre cada punto de datos y el anterior. El valor de salida es la diferencia de tiempo en segundos y puede tratarse como una duración.

Configuración:
- **Incluir tiempo hasta el último**: Incluir el tiempo entre ahora y el último punto de datos (predeterminado: false)
		]],
		["sw"] = [[
Hukokotoa muda kwa sekunde kati ya kila nukta ya data na nukta iliyotangulia. Thamani ya matokeo ni tofauti ya muda kwa sekunde na inaweza kuchukuliwa kuwa muda.

Usanidi:
- **Jumuisha Muda hadi ya Mwisho**: Jumuisha muda kati ya sasa na nukta ya mwisho ya data (chaguo-msingi: false)
		]],
		["sv"] = [[
Beräknar varaktigheten i sekunder mellan varje datapunkt och den föregående. Utvärdet är tidsskillnaden i sekunder och kan behandlas som en varaktighet.

Konfiguration:
- **Inkludera tid till senaste**: Inkludera tiden mellan nu och den senaste datapunkten (standard: false)
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளிக்கும் அதற்கு முந்தைய தரவுப் புள்ளிக்கும் இடையிலான கால அளவை வினாடிகளில் கணக்கிடுகிறது. வெளியீட்டு மதிப்பு வினாடிகளில் உள்ள நேர வேறுபாடாக இருந்து, கால அளவாகக் கருதப்படலாம்.

உள்ளமைவு:
- **கடைசி வரை உள்ள நேரத்தைச் சேர்**: இப்போதுக்கும் கடைசி தரவுப் புள்ளிக்கும் இடையிலான நேரத்தைச் சேர் (இயல்புநிலை: false)
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్‌కు దానికి ముందు ఉన్న డేటా పాయింట్ మధ్య వ్యవధిని సెకన్లలో లెక్కిస్తుంది. అవుట్‌పుట్ విలువ సెకన్లలోని సమయ వ్యత్యాసం, దీన్ని వ్యవధిగా పరిగణించవచ్చు.

కాన్ఫిగరేషన్:
- **చివరిదానికి సమయాన్ని చేర్చు**: ఇప్పటి సమయం మరియు చివరి డేటా పాయింట్ మధ్య సమయాన్ని చేర్చండి (డిఫాల్ట్: false)
		]],
		["th"] = [[
คำนวณระยะเวลาเป็นวินาทีระหว่างจุดข้อมูลแต่ละจุดกับจุดก่อนหน้า ค่าผลลัพธ์คือส่วนต่างเวลาเป็นวินาทีและสามารถถือเป็นระยะเวลาได้

การกำหนดค่า:
- **รวมเวลาถึงจุดล่าสุด**: รวมเวลาระหว่างตอนนี้กับจุดข้อมูลล่าสุด (ค่าเริ่มต้น: false)
		]],
		["tr"] = [[
Her veri noktası ile bir önceki arasındaki süreyi saniye cinsinden hesaplar. Çıktı değeri saniye cinsinden zaman farkıdır ve süre olarak değerlendirilebilir.

Yapılandırma:
- **Son Veriye Kadar Olan Süreyi Dahil Et**: Şimdi ile son veri noktası arasındaki süreyi dahil et (varsayılan: false)
		]],
		["uk"] = [[
Обчислює тривалість у секундах між кожною точкою даних і попередньою. Вихідне значення — різниця часу в секундах, яку можна трактувати як тривалість.

Конфігурація:
- **Включати час до останньої**: Включати час між поточним моментом і останньою точкою даних (типово: false)
		]],
		["vi"] = [[
Tính thời lượng tính bằng giây giữa mỗi điểm dữ liệu và điểm trước đó. Giá trị đầu ra là chênh lệch thời gian tính bằng giây và có thể được xem là một thời lượng.

Cấu hình:
- **Bao gồm thời gian đến điểm cuối**: Bao gồm khoảng thời gian từ điểm dữ liệu cuối cùng đến hiện tại (mặc định: false)
		]],
	},
	config = {
		checkbox {
			id = "include_last",
			default = false,
			name = {
				["en"] = "Include Time to Last",
				["af"] = "Sluit Tyd tot Laaste In",
				["sq"] = "Përfshi kohën deri te e fundit",
				["am"] = "እስከ የመጨረሻው ጊዜ አካትት",
				["hy"] = "Ներառել ժամանակը մինչև վերջինը",
				["az"] = "Sonuncuya qədərki vaxtı daxil et",
				["bn"] = "সর্বশেষ পর্যন্ত সময় অন্তর্ভুক্ত করুন",
				["eu"] = "Sartu azkenekorainoko denbora",
				["be"] = "Уключаць час да апошняй",
				["bg"] = "Включване на времето до последната",
				["my"] = "နောက်ဆုံးအချိန်အထိ ထည့်သွင်းရန်",
				["ca"] = "Inclou el temps fins a l’últim",
				["zh-Hans"] = "包含到最后一个数据点的时间",
				["zh-Hant"] = "包含至最後一筆的時間",
				["hr"] = "Uključi vrijeme do posljednje",
				["cs"] = "Zahrnout čas do posledního",
				["da"] = "Medtag tid til seneste",
				["nl"] = "Tijd tot laatste opnemen",
				["et"] = "Kaasa aeg viimase punktini",
				["fil"] = "Isama ang Oras Hanggang sa Huli",
				["fi"] = "Sisällytä aika viimeiseen",
				["fr"] = "Inclure le temps jusqu’au dernier",
				["gl"] = "Incluír o tempo ata o último",
				["ka"] = "ბოლომდე დროის ჩართვა",
				["de"] = "Zeit bis zum letzten einschließen",
				["el"] = "Συμπερίληψη χρόνου έως το τελευταίο",
				["gu"] = "છેલ્લા પોઇન્ટ સુધીનો સમય સામેલ કરો",
				["hi"] = "अंतिम डेटा पॉइंट तक का समय शामिल करें",
				["hu"] = "Utolsó adatpontig hátralévő idő belefoglalása",
				["is"] = "Taka með tíma að síðasta",
				["id"] = "Sertakan Waktu hingga Terakhir",
				["it"] = "Includi il tempo fino all'ultimo",
				["ja"] = "最後のデータポイントまでの時間を含める",
				["kn"] = "ಕೊನೆಯದಕ್ಕೆ ಸಮಯವನ್ನು ಒಳಗೊಂಡಿರಿ",
				["kk"] = "Соңғысына дейінгі уақытты қосу",
				["km"] = "រួមបញ្ចូលពេលវេលាដល់ចំណុចចុងក្រោយ",
				["ko"] = "마지막 항목까지의 시간 포함",
				["ky"] = "Акыркы чекитке чейинки убакытты кошуу",
				["lo"] = "ລວມເວລາເຖິງຈຸດສຸດທ້າຍ",
				["lv"] = "Iekļaut laiku līdz pēdējam",
				["lt"] = "Įtraukti laiką iki paskutinio",
				["mk"] = "Вклучи време до последната",
				["ms"] = "Sertakan Masa hingga Terakhir",
				["ml"] = "അവസാനത്തേക്കുള്ള സമയം ഉൾപ്പെടുത്തുക",
				["mr"] = "शेवटपर्यंतचा वेळ समाविष्ट करा",
				["mn"] = "Сүүлийн цэг хүртэлх хугацааг оруулах",
				["ne"] = "अन्तिमसम्मको समय समावेश गर्ने",
				["no"] = "Inkluder tid til siste",
				["pl"] = "Uwzględnij czas do ostatniego",
				["pt"] = "Incluir tempo até ao último",
				["pa"] = "ਆਖਰੀ ਤੱਕ ਦਾ ਸਮਾਂ ਸ਼ਾਮਲ ਕਰੋ",
				["ro"] = "Include timpul până la ultimul",
				["rm"] = "Includer il temp fin a l'ultim",
				["ru"] = "Включать время до последней",
				["sr"] = "Uključi vreme do poslednje",
				["si"] = "අවසන් එක දක්වා කාලය ඇතුළත් කරන්න",
				["sk"] = "Zahrnúť čas po posledný",
				["sl"] = "Vključi čas do zadnje točke",
				["es"] = "Incluir tiempo hasta el último",
				["sw"] = "Jumuisha Muda hadi ya Mwisho",
				["sv"] = "Inkludera tid till senaste",
				["ta"] = "கடைசி வரை உள்ள நேரத்தைச் சேர்",
				["te"] = "చివరిదానికి సమయాన్ని చేర్చు",
				["th"] = "รวมเวลาถึงจุดล่าสุด",
				["tr"] = "Son Veriye Kadar Olan Süreyi Dahil Et",
				["uk"] = "Включати час до останньої",
				["vi"] = "Bao gồm thời gian đến điểm cuối",
			},
		},
	},

	-- Generator function
	generator = function(source, config)
		local include_last = config and config.include_last or false
		local previous_point = nil

		return function()
			-- Initialize on first call
			if previous_point == nil then
				local first_point = source.dp()
				if not first_point then
					return nil
				end

				previous_point = first_point

				if include_last then
					-- Return synthetic point with time from now to last (oldest)
					local now = core.time().timestamp
					local duration_seconds = (now - first_point.timestamp) / 1000.0

					return {
						timestamp = first_point.timestamp,
						offset = first_point.offset,
						value = duration_seconds,
						label = "",
						note = "",
					}
				end
			end

			-- Get next data point
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Calculate duration from previous to current
			local duration_seconds = (previous_point.timestamp - data_point.timestamp) / 1000.0

			-- Create output point using previous point's identity
			local output_point = {
				timestamp = previous_point.timestamp,
				offset = previous_point.offset,
				value = duration_seconds,
				label = previous_point.label,
				note = previous_point.note,
			}

			-- Update state for next iteration
			previous_point = data_point

			return output_point
		end
	end,
}
