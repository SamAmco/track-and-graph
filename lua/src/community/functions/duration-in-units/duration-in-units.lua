-- Converts duration data point values from seconds to a selected time unit.

local enum = require("tng.config").enum

local seconds_per_unit = {
	["_seconds"] = 1,
	["_minutes"] = 60,
	["_hours"] = 60 * 60,
	["_days"] = 24 * 60 * 60,
	["_weeks"] = 7 * 24 * 60 * 60,
	["_months"] = 30.44 * 24 * 60 * 60,
	["_years"] = 365.25 * 24 * 60 * 60,
}

return {
	id = "duration-in-units",
	version = "1.0.1",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Duration in Units",
		["af"] = "Duur in Eenhede",
		["sq"] = "Kohëzgjatja në njësi",
		["am"] = "ቆይታ በመለኪያዎች",
		["hy"] = "Տևողությունը միավորներով",
		["az"] = "Vahidlərdə müddət",
		["bn"] = "এককে সময়কাল",
		["eu"] = "Iraupena unitatetan",
		["be"] = "Працягласць у адзінках",
		["bg"] = "Продължителност в единици",
		["my"] = "ယူနစ်ဖြင့် ကြာချိန်",
		["ca"] = "Durada en unitats",
		["zh-Hans"] = "以单位表示时长",
		["zh-Hant"] = "以單位表示持續時間",
		["hr"] = "Trajanje u jedinicama",
		["cs"] = "Délka v jednotkách",
		["da"] = "Varighed i enheder",
		["nl"] = "Duur in eenheden",
		["et"] = "Kestus ühikutes",
		["fil"] = "Tagal sa mga Yunit",
		["fi"] = "Kesto yksikköinä",
		["fr"] = "Durée en unités",
		["gl"] = "Duración en unidades",
		["ka"] = "ხანგრძლივობა ერთეულებში",
		["de"] = "Dauer in Einheiten",
		["el"] = "Διάρκεια σε μονάδες",
		["gu"] = "એકમોમાં અવધિ",
		["hi"] = "इकाइयों में अवधि",
		["hu"] = "Időtartam egységekben",
		["is"] = "Tímalengd í einingum",
		["id"] = "Durasi dalam Satuan",
		["it"] = "Durata in unità",
		["ja"] = "単位での継続時間",
		["kn"] = "ಘಟಕಗಳಲ್ಲಿ ಅವಧಿ",
		["kk"] = "Бірліктердегі ұзақтық",
		["km"] = "រយៈពេលជាឯកតា",
		["ko"] = "단위로 표시한 기간",
		["ky"] = "Бирдиктердеги узактык",
		["lo"] = "ໄລຍະເວລາເປັນໜ່ວຍ",
		["lv"] = "Ilgums vienībās",
		["lt"] = "Trukmė vienetais",
		["mk"] = "Времетраење во единици",
		["ms"] = "Tempoh dalam Unit",
		["ml"] = "യൂണിറ്റുകളിലെ ദൈർഘ്യം",
		["mr"] = "एककांमधील कालावधी",
		["mn"] = "Хугацааг нэгжээр илэрхийлэх",
		["ne"] = "एकाइमा अवधि",
		["no"] = "Varighet i enheter",
		["pl"] = "Czas trwania w jednostkach",
		["pt"] = "Duração em unidades",
		["pa"] = "ਇਕਾਈਆਂ ਵਿੱਚ ਮਿਆਦ",
		["ro"] = "Durată în unități",
		["rm"] = "Durada en unitads",
		["ru"] = "Длительность в единицах",
		["sr"] = "Trajanje u jedinicama",
		["si"] = "ඒකකවල කාලසීමාව",
		["sk"] = "Trvanie v jednotkách",
		["sl"] = "Trajanje v enotah",
		["es"] = "Duración en unidades",
		["sw"] = "Muda kwa Vipimo",
		["sv"] = "Varaktighet i enheter",
		["ta"] = "அலகுகளில் கால அளவு",
		["te"] = "యూనిట్లలో వ్యవధి",
		["th"] = "ระยะเวลาในหน่วย",
		["tr"] = "Birim Cinsinden Süre",
		["uk"] = "Тривалість в одиницях",
		["vi"] = "Thời lượng theo đơn vị",
	},
	description = {
		["en"] = [[
Converts each duration data point's value from seconds to the selected time unit. The output values are ordinary numbers rather than durations.

Months use an average length of 30.44 days and years use an average length of 365.25 days.
		]],
		["af"] = [[
Skakel elke duurndatapunt se waarde van sekondes om na die gekose tydeenheid. Die uitvoerwaardes is gewone getalle eerder as duurwaardes.

Maande gebruik ’n gemiddelde lengte van 30.44 dae en jare gebruik ’n gemiddelde lengte van 365.25 dae.
		]],
		["sq"] = [[
Shndërron vlerën e çdo pike të të dhënave me kohëzgjatje nga sekonda në njësinë kohore të zgjedhur. Vlerat dalëse janë numra të zakonshëm, jo kohëzgjatje.

Muajt përdorin një gjatësi mesatare prej 30.44 ditësh dhe vitet një gjatësi mesatare prej 365.25 ditësh.
		]],
		["am"] = [[
የእያንዳንዱን የቆይታ የውሂብ ነጥብ ዋጋ ከሰከንዶች ወደተመረጠው የጊዜ መለኪያ ይቀይራል። የውጤት ዋጋዎች ከቆይታ ይልቅ መደበኛ ቁጥሮች ናቸው።

ወራት በአማካይ 30.44 ቀናት፣ ዓመታት ደግሞ በአማካይ 365.25 ቀናት እንደሚረዝሙ ይቆጠራል።
		]],
		["hy"] = [[
Յուրաքանչյուր տևողության տվյալակետի արժեքը վայրկյաններից փոխակերպում է ընտրված ժամանակի միավորի։ Ելքային արժեքները սովորական թվեր են, ոչ թե տևողություններ։

Ամիսների համար օգտագործվում է 30.44 օրվա, իսկ տարիների համար՝ 365.25 օրվա միջին տևողությունը։
		]],
		["az"] = [[
Hər müddət məlumat nöqtəsinin qiymətini saniyələrdən seçilmiş zaman vahidinə çevirir. Çıxış qiymətləri müddət deyil, adi ədədlərdir.

Aylar üçün orta hesabla 30.44 gün, illər üçün isə 365.25 gün istifadə olunur.
		]],
		["bn"] = [[
প্রতিটি সময়কাল ডেটা পয়েন্টের মানকে সেকেন্ড থেকে নির্বাচিত সময়ের এককে রূপান্তর করে। আউটপুট মানগুলো সময়কাল নয়, সাধারণ সংখ্যা হিসেবে থাকে।

মাসের গড় দৈর্ঘ্য 30.44 দিন এবং বছরের গড় দৈর্ঘ্য 365.25 দিন ধরা হয়।
		]],
		["eu"] = [[
Iraupen-datu-puntu bakoitzaren balioa segundotik hautatutako denbora-unitatera bihurtzen du. Irteerako balioak zenbaki arruntak dira, ez iraupenak.

Hilabeteek 30,44 eguneko batez besteko iraupena dute, eta urteek 365,25 egunekoa.
		]],
		["be"] = [[
Пераўтварае значэнне кожнай кропкі даных працягласці з секунд у выбраную адзінку часу. Выходныя значэнні — звычайныя лікі, а не працягласці.

Для месяцаў выкарыстоўваецца сярэдняя працягласць 30,44 дня, а для гадоў — 365,25 дня.
		]],
		["bg"] = [[
Преобразува стойността на всяка точка от данни за продължителност от секунди в избраната времева единица. Изходните стойности са обикновени числа, а не продължителности.

Месеците използват средна продължителност от 30.44 дни, а годините — средна продължителност от 365.25 дни.
		]],
		["my"] = [[
ကြာချိန်ဒေတာမှတ်တစ်ခုချင်းစီ၏ တန်ဖိုးကို စက္ကန့်မှ ရွေးချယ်ထားသော အချိန်ယူနစ်သို့ ပြောင်းသည်။ ထွက်တန်ဖိုးများသည် ကြာချိန်မဟုတ်ဘဲ ပုံမှန်ကိန်းများဖြစ်သည်။

လများအတွက် ပျမ်းမျှ ၃၀.၄၄ ရက်နှင့် နှစ်များအတွက် ပျမ်းမျှ ၃၆၅.၂၅ ရက်ကို အသုံးပြုသည်။
		]],
		["ca"] = [[
Converteix el valor de cada punt de dades de durada de segons a la unitat de temps seleccionada. Els valors de sortida són nombres ordinaris, no durades.

Els mesos utilitzen una durada mitjana de 30,44 dies i els anys, una durada mitjana de 365,25 dies.
		]],
		["zh-Hans"] = [[
将每个时长数据点的值从秒转换为所选时间单位。输出值为普通数字，而不是时长。

月份按平均 30.44 天计算，年份按平均 365.25 天计算。
		]],
		["zh-Hant"] = [[
將每個持續時間資料點的值從秒轉換為所選時間單位。輸出值是一般數字，而非持續時間。

月份使用 30.44 天的平均長度，年份使用 365.25 天的平均長度。
		]],
		["hr"] = [[
Pretvara vrijednost svake podatkovne točke trajanja iz sekundi u odabranu vremensku jedinicu. Izlazne vrijednosti obični su brojevi, a ne trajanja.

Mjeseci koriste prosječnu duljinu od 30.44 dana, a godine prosječnu duljinu od 365.25 dana.
		]],
		["cs"] = [[
Převede hodnotu každého datového bodu typu délka ze sekund na vybranou časovou jednotku. Výstupní hodnoty jsou běžná čísla, nikoli délky.

Měsíce používají průměrnou délku 30,44 dne a roky průměrnou délku 365,25 dne.
		]],
		["da"] = [[
Konverterer hvert varighedsdatapunkts værdi fra sekunder til den valgte tidsenhed. Outputværdierne er almindelige tal og ikke varigheder.

Måneder bruger en gennemsnitlig længde på 30.44 dage, og år bruger en gennemsnitlig længde på 365.25 dage.
		]],
		["nl"] = [[
Zet de waarde van elk duurgegevenspunt om van seconden naar de geselecteerde tijdseenheid. De uitvoerwaarden zijn gewone getallen en geen tijdsduren.

Maanden gebruiken een gemiddelde lengte van 30,44 dagen en jaren een gemiddelde lengte van 365,25 dagen.
		]],
		["et"] = [[
Teisendab iga kestusega andmepunkti väärtuse sekunditest valitud ajaühikusse. Väljundväärtused on tavalised arvud, mitte kestused.

Kuude keskmiseks pikkuseks kasutatakse 30,44 päeva ja aastate keskmiseks pikkuseks 365,25 päeva.
		]],
		["fil"] = [[
Kino-convert ang halaga ng bawat duration data point mula segundo sa napiling yunit ng oras. Karaniwang numero ang mga output value sa halip na mga duration.

Gumagamit ang mga buwan ng average na haba na 30.44 araw at ang mga taon ng average na haba na 365.25 araw.
		]],
		["fi"] = [[
Muuttaa kunkin kestodatapisteen arvon sekunneista valituksi aikayksiköksi. Tulokset ovat tavallisia lukuja eivätkä kestoja.

Kuukauden keskimääräisenä pituutena käytetään 30,44 päivää ja vuoden 365,25 päivää.
		]],
		["fr"] = [[
Convertit la valeur de chaque point de données de type durée, des secondes vers l’unité de temps sélectionnée. Les valeurs produites sont des nombres ordinaires et non des durées.

Les mois utilisent une durée moyenne de 30,44 jours et les années une durée moyenne de 365,25 jours.
		]],
		["gl"] = [[
Converte o valor de cada punto de datos de duración de segundos á unidade temporal seleccionada. Os valores de saída son números ordinarios, non duracións.

Os meses usan unha duración media de 30.44 días e os anos unha duración media de 365.25 días.
		]],
		["ka"] = [[
თითოეული ხანგრძლივობის მონაცემის წერტილის მნიშვნელობას წამებიდან არჩეულ დროის ერთეულში გარდაქმნის. გამოტანილი მნიშვნელობები ჩვეულებრივი რიცხვებია და არა ხანგრძლივობები.

თვეებისთვის გამოიყენება 30.44 დღის, ხოლო წლებისთვის 365.25 დღის საშუალო ხანგრძლივობა.
		]],
		["de"] = [[
Wandelt den Wert jedes Dauerdatenpunkts von Sekunden in die ausgewählte Zeiteinheit um. Die Ausgabewerte sind gewöhnliche Zahlen und keine Dauern.

Für Monate wird eine durchschnittliche Länge von 30,44 Tagen und für Jahre eine durchschnittliche Länge von 365,25 Tagen verwendet.
		]],
		["el"] = [[
Μετατρέπει την τιμή κάθε σημείου δεδομένων διάρκειας από δευτερόλεπτα στην επιλεγμένη μονάδα χρόνου. Οι τιμές εξόδου είναι απλοί αριθμοί και όχι διάρκειες.

Οι μήνες χρησιμοποιούν μέση διάρκεια 30.44 ημερών και τα έτη μέση διάρκεια 365.25 ημερών.
		]],
		["gu"] = [[
દરેક અવધિ ડેટા પોઇન્ટના મૂલ્યને સેકન્ડમાંથી પસંદ કરેલા સમય એકમમાં રૂપાંતરિત કરે છે. આઉટપુટ મૂલ્યો અવધિ તરીકે નહીં પરંતુ સામાન્ય સંખ્યાઓ હોય છે.

મહિનાઓ માટે 30.44 દિવસની અને વર્ષો માટે 365.25 દિવસની સરેરાશ અવધિ વપરાય છે.
		]],
		["hi"] = [[
हर अवधि डेटा पॉइंट के मान को सेकंड से चयनित समय इकाई में बदलता है। आउटपुट मान अवधि के बजाय सामान्य संख्याएँ होते हैं।

महीनों के लिए 30.44 दिनों और वर्षों के लिए 365.25 दिनों की औसत लंबाई उपयोग की जाती है।
		]],
		["hu"] = [[
Az egyes időtartam-adatpontok értékét másodpercről a kiválasztott időegységre alakítja. A kimeneti értékek közönséges számok, nem időtartamok.

A hónapok átlagos hossza 30,44 nap, az éveké 365,25 nap.
		]],
		["is"] = [[
Breytir gildi hvers tímalengdargagnapunkts úr sekúndum í valda tímaeiningu. Úttaksgildin eru venjulegar tölur en ekki tímalengdir.

Mánuðir nota meðallengdina 30.44 daga og ár nota meðallengdina 365.25 daga.
		]],
		["id"] = [[
Mengonversi nilai setiap titik data durasi dari detik ke satuan waktu yang dipilih. Nilai keluaran berupa angka biasa, bukan durasi.

Bulan menggunakan panjang rata-rata 30.44 hari dan tahun menggunakan panjang rata-rata 365.25 hari.
		]],
		["it"] = [[
Converte il valore di ogni punto dati di durata da secondi all'unità di tempo selezionata. I valori di output sono numeri ordinari anziché durate.

I mesi usano una durata media di 30,44 giorni e gli anni una durata media di 365,25 giorni.
		]],
		["ja"] = [[
各継続時間データポイントの値を、秒から選択した時間単位に変換します。出力値は継続時間ではなく通常の数値です。

月は平均30.44日、年は平均365.25日として計算します。
		]],
		["kn"] = [[
ಪ್ರತಿ ಅವಧಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯವನ್ನು ಸೆಕೆಂಡುಗಳಿಂದ ಆಯ್ಕೆಮಾಡಿದ ಸಮಯ ಘಟಕಕ್ಕೆ ಪರಿವರ್ತಿಸುತ್ತದೆ. ಔಟ್‌ಪುಟ್ ಮೌಲ್ಯಗಳು ಅವಧಿಗಳ ಬದಲು ಸಾಮಾನ್ಯ ಸಂಖ್ಯೆಗಳಾಗಿರುತ್ತವೆ.

ತಿಂಗಳುಗಳಿಗೆ ಸರಾಸರಿ 30.44 ದಿನಗಳು ಮತ್ತು ವರ್ಷಗಳಿಗೆ ಸರಾಸರಿ 365.25 ದಿನಗಳನ್ನು ಬಳಸಲಾಗುತ್ತದೆ.
		]],
		["kk"] = [[
Әр ұзақтық дерек нүктесінің мәнін секундтан таңдалған уақыт бірлігіне түрлендіреді. Шығыс мәндері ұзақтық емес, кәдімгі сандар болады.

Айлар үшін орташа 30.44 күн, ал жылдар үшін орташа 365.25 күн қолданылады.
		]],
		["km"] = [[
បម្លែងតម្លៃនៃចំណុចទិន្នន័យរយៈពេលនីមួយៗពីវិនាទីទៅឯកតាពេលវេលាដែលបានជ្រើស។ តម្លៃលទ្ធផលគឺជាលេខធម្មតា មិនមែនជារយៈពេលទេ។

ខែប្រើរយៈពេលមធ្យម 30.44 ថ្ងៃ ហើយឆ្នាំប្រើរយៈពេលមធ្យម 365.25 ថ្ងៃ។
		]],
		["ko"] = [[
각 기간 데이터 포인트의 값을 초에서 선택한 시간 단위로 변환합니다. 출력 값은 기간이 아닌 일반 숫자입니다.

월은 평균 30.44일, 년은 평균 365.25일을 사용합니다.
		]],
		["ky"] = [[
Ар бир узактык маалымат чекитинин маанисин секунддан тандалган убакыт бирдигине айландырат. Чыгуу маанилери узактык эмес, кадимки сандар болот.

Айлардын орточо узактыгы 30.44 күн, жылдардыкы 365.25 күн деп алынат.
		]],
		["lo"] = [[
ແປງຄ່າໄລຍະເວລາຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດຈາກວິນາທີເປັນໜ່ວຍເວລາທີ່ເລືອກ. ຄ່າຜົນລັບເປັນຈຳນວນທົ່ວໄປ ບໍ່ແມ່ນໄລຍະເວລາ.

ເດືອນໃຊ້ຄວາມຍາວສະເລ່ຍ 30.44 ມື້ ແລະ ປີໃຊ້ຄວາມຍາວສະເລ່ຍ 365.25 ມື້.
		]],
		["lv"] = [[
Pārvērš katra ilguma datu punkta vērtību no sekundēm atlasītajā laika vienībā. Izvades vērtības ir parasti skaitļi, nevis ilgumi.

Mēnešiem tiek izmantots vidējais ilgums — 30,44 dienas, bet gadiem — 365,25 dienas.
		]],
		["lt"] = [[
Kiekvieno trukmės duomenų taško reikšmę iš sekundžių konvertuoja į pasirinktą laiko vienetą. Išvesties reikšmės yra įprasti skaičiai, o ne trukmės.

Mėnesiams naudojama vidutinė 30,44 dienos trukmė, o metams – 365,25 dienos.
		]],
		["mk"] = [[
Ја претвора вредноста на секоја точка на податоци за времетраење од секунди во избраната временска единица. Излезните вредности се обични броеви, а не времетраења.

Месеците користат просечна должина од 30.44 дена, а годините просечна должина од 365.25 дена.
		]],
		["ms"] = [[
Menukar nilai setiap titik data tempoh daripada saat kepada unit masa yang dipilih. Nilai output ialah nombor biasa dan bukannya tempoh.

Bulan menggunakan panjang purata 30.44 hari dan tahun menggunakan panjang purata 365.25 hari.
		]],
		["ml"] = [[
ഓരോ duration ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യം സെക്കൻഡുകളിൽ നിന്ന് തിരഞ്ഞെടുത്ത സമയ യൂണിറ്റിലേക്ക് മാറ്റുന്നു. ഔട്ട്പുട്ട് മൂല്യങ്ങൾ duration-കൾക്ക് പകരം സാധാരണ സംഖ്യകളായിരിക്കും.

മാസങ്ങൾക്ക് ശരാശരി 30.44 ദിവസവും വർഷങ്ങൾക്ക് ശരാശരി 365.25 ദിവസവും ഉപയോഗിക്കുന്നു.
		]],
		["mr"] = [[
प्रत्येक कालावधी डेटा बिंदूचे मूल्य सेकंदांमधून निवडलेल्या कालावधी एककात रूपांतरित करते. आउटपुट मूल्ये कालावधीऐवजी सामान्य संख्या असतात.

महिन्यांसाठी 30.44 दिवसांची आणि वर्षांसाठी 365.25 दिवसांची सरासरी लांबी वापरली जाते.
		]],
		["mn"] = [[
Хугацааны өгөгдлийн цэг бүрийн утгыг секундээс сонгосон хугацааны нэгж рүү хөрвүүлнэ. Гаралтын утгууд нь хугацаа биш, энгийн тоо байна.

Сарыг дунджаар 30.44 хоног, жилийг дунджаар 365.25 хоног гэж тооцно.
		]],
		["ne"] = [[
प्रत्येक अवधि डेटा बिन्दुको मानलाई सेकेन्डबाट चयन गरिएको समय एकाइमा रूपान्तरण गर्छ। आउटपुट मानहरू अवधिका रूपमा नभई सामान्य संख्याका रूपमा हुन्छन्।

महिनाका लागि ३०.४४ दिन र वर्षका लागि ३६५.२५ दिनको औसत लम्बाइ प्रयोग गरिन्छ।
		]],
		["no"] = [[
Konverterer verdien til hvert varighetsdatapunkt fra sekunder til den valgte tidsenheten. Utgangsverdiene er vanlige tall, ikke varigheter.

Måneder bruker en gjennomsnittlig lengde på 30,44 dager, og år bruker en gjennomsnittlig lengde på 365,25 dager.
		]],
		["pl"] = [[
Konwertuje wartość każdego punktu danych typu czas trwania z sekund na wybraną jednostkę czasu. Wartości wyjściowe są zwykłymi liczbami, a nie czasami trwania.

Miesiące używają średniej długości 30,44 dnia, a lata — 365,25 dnia.
		]],
		["pt"] = [[
Converte o valor de cada ponto de dados de duração de segundos para a unidade de tempo selecionada. Os valores de saída são números comuns, não durações.

Os meses usam uma duração média de 30,44 dias e os anos usam uma duração média de 365,25 dias.
		]],
		["pa"] = [[
ਹਰੇਕ ਮਿਆਦ ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਨੂੰ ਸਕਿੰਟਾਂ ਤੋਂ ਚੁਣੀ ਹੋਈ ਸਮੇਂ ਦੀ ਇਕਾਈ ਵਿੱਚ ਬਦਲਦਾ ਹੈ। ਆਉਟਪੁੱਟ ਮੁੱਲ ਮਿਆਦਾਂ ਦੀ ਬਜਾਏ ਆਮ ਸੰਖਿਆਵਾਂ ਹੁੰਦੇ ਹਨ।

ਮਹੀਨਿਆਂ ਲਈ 30.44 ਦਿਨ ਅਤੇ ਸਾਲਾਂ ਲਈ 365.25 ਦਿਨਾਂ ਦੀ ਔਸਤ ਮਿਆਦ ਵਰਤੀ ਜਾਂਦੀ ਹੈ।
		]],
		["ro"] = [[
Convertește valoarea fiecărui punct de date de tip durată din secunde în unitatea de timp selectată. Valorile rezultate sunt numere obișnuite, nu durate.

Lunile folosesc o durată medie de 30,44 zile, iar anii folosesc o durată medie de 365,25 zile.
		]],
		["rm"] = [[
Converta la valur da mintga punct da datas da durada da secundas en l’unitad temporala selecziunada. Las valurs d’output èn dumbers ordinaris e betg duradas.

Ils mais utiliseschan ina lunghezza media da 30.44 dis e ils onns ina lunghezza media da 365.25 dis.
		]],
		["ru"] = [[
Преобразует значение каждой точки данных с длительностью из секунд в выбранную единицу времени. Выходные значения являются обычными числами, а не длительностями.

Для месяцев используется средняя продолжительность 30,44 дня, а для лет — 365,25 дня.
		]],
		["sr"] = [[
Pretvara vrednost svake tačke podataka tipa trajanje iz sekundi u izabranu vremensku jedinicu. Izlazne vrednosti su obični brojevi, a ne trajanja.

Meseci koriste prosečnu dužinu od 30.44 dana, a godine prosečnu dužinu od 365.25 dana.
		]],
		["si"] = [[
සෑම කාලසීමා දත්ත ලක්ෂ්‍යයකම අගය තත්පරවලින් තෝරාගත් කාල ඒකකයට පරිවර්තනය කරයි. ප්‍රතිදාන අගයන් කාලසීමා නොව සාමාන්‍ය සංඛ්‍යා වේ.

මාස සඳහා දින 30.44ක සහ වසර සඳහා දින 365.25ක සාමාන්‍ය දිගක් භාවිත කරයි.
		]],
		["sk"] = [[
Prevedie hodnotu každého údajového bodu trvania zo sekúnd na vybranú časovú jednotku. Výstupné hodnoty sú bežné čísla, nie trvania.

Mesiace používajú priemernú dĺžku 30.44 dňa a roky priemernú dĺžku 365.25 dňa.
		]],
		["sl"] = [[
Pretvori vrednost vsake podatkovne točke trajanja iz sekund v izbrano časovno enoto. Izhodne vrednosti so običajna števila, ne trajanja.

Meseci uporabljajo povprečno dolžino 30,44 dni, leta pa povprečno dolžino 365,25 dni.
		]],
		["es"] = [[
Convierte el valor de cada punto de datos de duración de segundos a la unidad de tiempo seleccionada. Los valores de salida son números normales, no duraciones.

Los meses usan una duración media de 30.44 días y los años, de 365.25 días.
		]],
		["sw"] = [[
Hubadilisha thamani ya kila nukta ya data ya muda kutoka sekunde kuwa kipimo cha muda kilichochaguliwa. Thamani za matokeo ni nambari za kawaida badala ya muda.

Miezi hutumia wastani wa siku 30.44 na miaka hutumia wastani wa siku 365.25.
		]],
		["sv"] = [[
Omvandlar varje varaktighetsdatapunkts värde från sekunder till den valda tidsenheten. Utvärdena är vanliga tal i stället för varaktigheter.

Månader använder en genomsnittlig längd på 30.44 dagar och år använder en genomsnittlig längd på 365.25 dagar.
		]],
		["ta"] = [[
ஒவ்வொரு கால அளவு தரவுப் புள்ளியின் மதிப்பையும் வினாடிகளிலிருந்து தேர்ந்தெடுக்கப்பட்ட நேர அலகாக மாற்றுகிறது. வெளியீட்டு மதிப்புகள் கால அளவுகளாக அல்லாமல் சாதாரண எண்களாக இருக்கும்.

மாதங்களுக்கு சராசரியாக 30.44 நாட்களும், ஆண்டுகளுக்கு 365.25 நாட்களும் பயன்படுத்தப்படுகின்றன.
		]],
		["te"] = [[
ప్రతి వ్యవధి డేటా పాయింట్ విలువను సెకన్ల నుండి ఎంచుకున్న సమయ యూనిట్‌కు మారుస్తుంది. అవుట్‌పుట్ విలువలు వ్యవధులుగా కాకుండా సాధారణ సంఖ్యలుగా ఉంటాయి.

నెలలకు సగటు 30.44 రోజులు, సంవత్సరాలకు సగటు 365.25 రోజులు ఉపయోగించబడతాయి.
		]],
		["th"] = [[
แปลงค่าของจุดข้อมูลระยะเวลาแต่ละจุดจากวินาทีเป็นหน่วยเวลาที่เลือก ค่าผลลัพธ์เป็นตัวเลขทั่วไป ไม่ใช่ระยะเวลา

เดือนใช้ความยาวเฉลี่ย 30.44 วัน และปีใช้ความยาวเฉลี่ย 365.25 วัน
		]],
		["tr"] = [[
Her süre veri noktasının değerini saniyeden seçilen zaman birimine dönüştürür. Çıktı değerleri süre yerine normal sayılardır.

Aylar için ortalama 30.44 gün, yıllar için ortalama 365.25 gün kullanılır.
		]],
		["uk"] = [[
Перетворює значення кожної точки даних тривалості із секунд у вибрану одиницю часу. Вихідні значення є звичайними числами, а не тривалостями.

Для місяців використовується середня тривалість 30,44 дня, а для років — 365,25 дня.
		]],
		["vi"] = [[
Chuyển đổi giá trị của từng điểm dữ liệu thời lượng từ giây sang đơn vị thời gian đã chọn. Giá trị đầu ra là các số thông thường thay vì thời lượng.

Tháng sử dụng độ dài trung bình 30.44 ngày và năm sử dụng độ dài trung bình 365.25 ngày.
		]],
	},
	config = {
		enum {
			id = "unit",
			name = {
				["en"] = "Time Unit",
				["af"] = "Tydseenheid",
				["sq"] = "Njësia kohore",
				["am"] = "የጊዜ መለኪያ",
				["hy"] = "Ժամանակի միավոր",
				["az"] = "Zaman vahidi",
				["bn"] = "সময়ের একক",
				["eu"] = "Denbora-unitatea",
				["be"] = "Адзінка часу",
				["bg"] = "Времева единица",
				["my"] = "အချိန်ယူနစ်",
				["ca"] = "Unitat de temps",
				["zh-Hans"] = "时间单位",
				["zh-Hant"] = "時間單位",
				["hr"] = "Vremenska jedinica",
				["cs"] = "Časová jednotka",
				["da"] = "Tidsenhed",
				["nl"] = "Tijdseenheid",
				["et"] = "Ajaühik",
				["fil"] = "Yunit ng Oras",
				["fi"] = "Aikayksikkö",
				["fr"] = "Unité de temps",
				["gl"] = "Unidade temporal",
				["ka"] = "დროის ერთეული",
				["de"] = "Zeiteinheit",
				["el"] = "Μονάδα χρόνου",
				["gu"] = "સમય એકમ",
				["hi"] = "समय इकाई",
				["hu"] = "Időegység",
				["is"] = "Tímaeining",
				["id"] = "Satuan Waktu",
				["it"] = "Unità di tempo",
				["ja"] = "時間単位",
				["kn"] = "ಸಮಯ ಘಟಕ",
				["kk"] = "Уақыт бірлігі",
				["km"] = "ឯកតាពេលវេលា",
				["ko"] = "시간 단위",
				["ky"] = "Убакыт бирдиги",
				["lo"] = "ໜ່ວຍເວລາ",
				["lv"] = "Laika vienība",
				["lt"] = "Laiko vienetas",
				["mk"] = "Временска единица",
				["ms"] = "Unit Masa",
				["ml"] = "സമയ യൂണിറ്റ്",
				["mr"] = "कालावधी एकक",
				["mn"] = "Хугацааны нэгж",
				["ne"] = "समय एकाइ",
				["no"] = "Tidsenhet",
				["pl"] = "Jednostka czasu",
				["pt"] = "Unidade de tempo",
				["pa"] = "ਸਮੇਂ ਦੀ ਇਕਾਈ",
				["ro"] = "Unitate de timp",
				["rm"] = "Unitad temporala",
				["ru"] = "Единица времени",
				["sr"] = "Vremenska jedinica",
				["si"] = "කාල ඒකකය",
				["sk"] = "Časová jednotka",
				["sl"] = "Časovna enota",
				["es"] = "Unidad de tiempo",
				["sw"] = "Kipimo cha Muda",
				["sv"] = "Tidsenhet",
				["ta"] = "நேர அலகு",
				["te"] = "సమయ యూనిట్",
				["th"] = "หน่วยเวลา",
				["tr"] = "Zaman Birimi",
				["uk"] = "Одиниця часу",
				["vi"] = "Đơn vị thời gian",
			},
			options = {
				"_seconds",
				"_minutes",
				"_hours",
				"_days",
				"_weeks",
				"_months",
				"_years",
			},
			default = "_hours",
		},
	},

	generator = function(source, config)
		local unit = config and config.unit or "_hours"
		local divisor = seconds_per_unit[unit]

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			data_point.value = data_point.value / divisor
			return data_point
		end
	end,
}
