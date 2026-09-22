-- Lua Function to multiply data point values by a configurable number
-- This function multiplies all incoming data point values by a specified multiplier

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "multiply",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
    	["en"] = "Multiply Values",
    	["af"] = "Vermenigvuldig Waardes",
    	["sq"] = "Shumëzo vlerat",
    	["am"] = "እሴቶችን አባዛ",
    	["hy"] = "Բազմապատկել արժեքները",
    	["az"] = "Qiymətləri vur",
    	["bn"] = "মান গুণ করুন",
    	["eu"] = "Biderkatu balioak",
    	["be"] = "Памножыць значэнні",
    	["bg"] = "Умножение на стойности",
    	["my"] = "တန်ဖိုးများ မြှောက်ရန်",
    	["ca"] = "Multiplica els valors",
    	["zh-Hans"] = "乘数据点值",
    	["zh-Hant"] = "乘法運算",
    	["hr"] = "Množi vrijednosti",
    	["cs"] = "Násobit hodnoty",
    	["da"] = "Multiplicer værdier",
    	["nl"] = "Waarden vermenigvuldigen",
    	["et"] = "Korruta väärtusi",
    	["fil"] = "I-multiply ang mga Halaga",
    	["fi"] = "Kerro arvot",
    	["fr"] = "Multiplier les valeurs",
    	["gl"] = "Multiplicar valores",
    	["ka"] = "მნიშვნელობების გამრავლება",
    	["de"] = "Werte multiplizieren",
    	["el"] = "Πολλαπλασιασμός τιμών",
    	["gu"] = "મૂલ્યોનો ગુણાકાર કરો",
    	["hi"] = "मानों को गुणा करें",
    	["hu"] = "Értékek szorzása",
    	["is"] = "Margfalda gildi",
    	["id"] = "Kalikan Nilai",
    	["it"] = "Moltiplica valori",
    	["ja"] = "値を乗算",
    	["kn"] = "ಮೌಲ್ಯಗಳನ್ನು ಗುಣಿಸಿ",
    	["kk"] = "Мәндерді көбейту",
    	["km"] = "គុណតម្លៃ",
    	["ko"] = "값 곱하기",
    	["ky"] = "Маанилерди көбөйтүү",
    	["lo"] = "ຄູນຄ່າ",
    	["lv"] = "Reizināt vērtības",
    	["lt"] = "Dauginti reikšmes",
    	["mk"] = "Множи ги вредностите",
    	["ms"] = "Darab Nilai",
    	["ml"] = "മൂല്യങ്ങൾ ഗുണിക്കുക",
    	["mr"] = "मूल्यांचा गुणाकार करा",
    	["mn"] = "Утгуудыг үржүүлэх",
    	["ne"] = "मानहरू गुणन गर्नुहोस्",
    	["no"] = "Multipliser verdier",
    	["pl"] = "Mnóż wartości",
    	["pt"] = "Multiplicar valores",
    	["pa"] = "ਮੁੱਲ ਗੁਣਾ ਕਰੋ",
    	["ro"] = "Înmulțește valorile",
    	["rm"] = "Multiplicar valurs",
    	["ru"] = "Умножить значения",
    	["sr"] = "Pomnoži vrednosti",
    	["si"] = "අගයන් ගුණ කරන්න",
    	["sk"] = "Násobiť hodnoty",
    	["sl"] = "Množi vrednosti",
    	["es"] = "Multiplicar valores",
    	["sw"] = "Zidisha Thamani",
    	["sv"] = "Multiplicera värden",
    	["ta"] = "மதிப்புகளைப் பெருக்கு",
    	["te"] = "విలువలను గుణించు",
    	["th"] = "คูณค่า",
    	["tr"] = "Değerleri Çarp",
    	["uk"] = "Множити значення",
    	["vi"] = "Nhân giá trị",
    },
    description = {
    	["en"] = [[
Multiplies all incoming data point values by a specified multiplier.

Configuration:
- **Multiplier**: The number to multiply all values by (default: 1.0)
    	]],
    	["af"] = [[
Vermenigvuldig alle inkomende datapuntwaardes met ’n gespesifiseerde vermenigvuldiger.

Konfigurasie:
- **Vermenigvuldiger**: Die getal waarmee alle waardes vermenigvuldig word (verstek: 1.0)
    	]],
    	["sq"] = [[
Shumëzon të gjitha vlerat hyrëse të pikave të të dhënave me një shumëzues të specifikuar.

Konfigurimi:
- **Shumëzuesi**: Numri me të cilin shumëzohen të gjitha vlerat (parazgjedhja: 1.0)
    	]],
    	["am"] = [[
ሁሉንም የሚገቡ የውሂብ ነጥብ እሴቶችን በተገለጸ ብዜት ያባዛል።

ውቅር፦
- **ብዜት**፦ ሁሉንም እሴቶች ለማባዛት የሚያገለግል ቁጥር (ነባሪ፦ 1.0)
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի արժեքները բազմապատկում է նշված բազմապատկիչով։

Կազմաձևում՝
- **Բազմապատկիչ**․ բոլոր արժեքները բազմապատկելու թիվը (կանխադրված՝ 1.0)
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qiymətlərini göstərilən vuruğa vurur.

Konfiqurasiya:
- **Vurğu**: Bütün qiymətlərin vurulacağı ədəd (standart: 1.0)
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের মানকে নির্দিষ্ট গুণক দিয়ে গুণ করে।

কনফিগারেশন:
- **গুণক**: সব মানকে যে সংখ্যা দিয়ে গুণ করা হবে (ডিফল্ট: 1.0)
    	]],
    	["eu"] = [[
Sarrerako datu-puntuen balio guztiak biderkatzaile zehatz batez biderkatzen ditu.

Konfigurazioa:
- **Biderkatzailea**: Balio guztiak biderkatzeko zenbakia (lehenetsia: 1.0)
    	]],
    	["be"] = [[
Памнажае ўсе ўваходныя значэнні кропак даных на зададзены множнік.

Канфігурацыя:
- **Множнік**: лік, на які памнажаць усе значэнні (па змаўчанні: 1.0)
    	]],
    	["bg"] = [[
Умножава всички входящи стойности на точките от данни със зададен множител.

Конфигурация:
- **Множител**: Числото, с което се умножават всички стойности (по подразбиране: 1.0)
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်တန်ဖိုးများအားလုံးကို သတ်မှတ်ထားသော မြှောက်ကိန်းဖြင့် မြှောက်သည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **မြှောက်ကိန်း**: တန်ဖိုးအားလုံးကို မြှောက်မည့် ကိန်း (မူလ: 1.0)
    	]],
    	["ca"] = [[
Multiplica tots els valors dels punts de dades entrants per un multiplicador especificat.

Configuració:
- **Multiplicador**: El nombre pel qual es multipliquen tots els valors (per defecte: 1.0)
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的值乘以指定倍数。

配置：
- **倍数**：所有值要乘以的数字（默认：1.0）
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的值乘以指定的乘數。

設定：
- **乘數**：所有值要乘以的數字（預設：1.0）
    	]],
    	["hr"] = [[
Množi sve dolazne vrijednosti podatkovnih točaka zadanim množiteljem.

Konfiguracija:
- **Množitelj**: Broj kojim se množe sve vrijednosti (zadano: 1.0)
    	]],
    	["cs"] = [[
Vynásobí všechny příchozí hodnoty datových bodů zadaným násobitelem.

Konfigurace:
- **Násobitel**: Číslo, kterým se všechny hodnoty vynásobí (výchozí: 1.0)
    	]],
    	["da"] = [[
Multiplicerer alle indgående datapunkters værdier med en angivet multiplikator.

Konfiguration:
- **Multiplikator**: Tallet, som alle værdier multipliceres med (standard: 1.0)
    	]],
    	["nl"] = [[
Vermenigvuldigt alle binnenkomende waarden van datapunten met een opgegeven vermenigvuldigingsfactor.

Configuratie:
- **Vermenigvuldigingsfactor**: Het getal waarmee alle waarden worden vermenigvuldigd (standaard: 1.0)
    	]],
    	["et"] = [[
Korrutab kõik sisendandmepunktide väärtused määratud kordajaga.

Seadistus:
- **Kordaja**: arv, millega kõik väärtused korrutatakse (vaikimisi: 1,0)
    	]],
    	["fil"] = [[
Minu-multiply ang lahat ng papasok na halaga ng data point sa tinukoy na multiplier.

Configuration:
- **Multiplier**: Numerong ipinamultiply sa lahat ng halaga (default: 1.0)
    	]],
    	["fi"] = [[
Kertoo kaikki saapuvien datapisteiden arvot määritetyllä kertoimella.

Määritys:
- **Kerroin**: Luku, jolla kaikki arvot kerrotaan (oletus: 1.0)
    	]],
    	["fr"] = [[
Multiplie toutes les valeurs des points de données entrants par un multiplicateur indiqué.

Configuration :
- **Multiplicateur** : Nombre par lequel multiplier toutes les valeurs (par défaut : 1.0)
    	]],
    	["gl"] = [[
Multiplica todos os valores dos puntos de datos recibidos por un multiplicador especificado.

Configuración:
- **Multiplicador**: O número polo que se multiplican todos os valores (predeterminado: 1.0)
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის მნიშვნელობას ამრავლებს მითითებულ გამამრავლებელზე.

კონფიგურაცია:
- **გამამრავლებელი**: რიცხვი, რომელზეც ყველა მნიშვნელობა უნდა გამრავლდეს (ნაგულისხმევი: 1.0)
    	]],
    	["de"] = [[
Multipliziert alle eingehenden Datenpunktwerte mit einem angegebenen Multiplikator.

Konfiguration:
- **Multiplikator**: Die Zahl, mit der alle Werte multipliziert werden (Standard: 1.0)
    	]],
    	["el"] = [[
Πολλαπλασιάζει όλες τις εισερχόμενες τιμές σημείων δεδομένων με έναν καθορισμένο πολλαπλασιαστή.

Διαμόρφωση:
- **Πολλαπλασιαστής**: Ο αριθμός με τον οποίο θα πολλαπλασιαστούν όλες οι τιμές (προεπιλογή: 1.0)
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના મૂલ્યોનો નિર્દિષ્ટ ગુણક વડે ગુણાકાર કરે છે.

ગોઠવણી:
- **ગુણક**: બધા મૂલ્યોનો જેના વડે ગુણાકાર કરવાનો છે તે સંખ્યા (ડિફૉલ્ટ: 1.0)
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के मानों को निर्दिष्ट गुणक से गुणा करता है।

कॉन्फ़िगरेशन:
- **गुणक**: सभी मानों को गुणा करने वाली संख्या (डिफ़ॉल्ट: 1.0)
    	]],
    	["hu"] = [[
Az összes beérkező adatpont értékét a megadott szorzóval szorozza.

Konfiguráció:
- **Szorzó**: Az összes érték szorzásához használt szám (alapértelmezett: 1.0)
    	]],
    	["is"] = [[
Margfaldar öll gildi innkomandi gagnapunkta með tilgreindum margfaldara.

Stillingar:
- **Margfaldari**: Talan sem margfalda á öll gildi með (sjálfgefið: 1.0)
    	]],
    	["id"] = [[
Mengalikan semua nilai titik data yang masuk dengan pengali yang ditentukan.

Konfigurasi:
- **Pengali**: Angka yang digunakan untuk mengalikan semua nilai (default: 1.0)
    	]],
    	["it"] = [[
Moltiplica tutti i valori dei punti dati in arrivo per un moltiplicatore specificato.

Configurazione:
- **Moltiplicatore**: il numero per cui moltiplicare tutti i valori (predefinito: 1.0)
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントの値に、指定した乗数を掛けます。

設定:
- **乗数**: すべての値に掛ける数（デフォルト: 1.0）
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ಗುಣಕದಿಂದ ಗುಣಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಗುಣಕ**: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳನ್ನು ಗುಣಿಸುವ ಸಂಖ್ಯೆ (ಡೀಫಾಲ್ಟ್: 1.0)
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің мәндерін көрсетілген көбейткішке көбейтеді.

Конфигурация:
- **Көбейткіш**: Барлық мәндерді көбейтуге арналған сан (әдепкі: 1.0)
    	]],
    	["km"] = [[
គុណតម្លៃរបស់ចំណុចទិន្នន័យចូលទាំងអស់ដោយមេគុណដែលបានបញ្ជាក់។

ការកំណត់រចនា៖
- **មេគុណ**៖ លេខដែលត្រូវគុណតម្លៃទាំងអស់ (លំនាំដើម៖ 1.0)
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 값에 지정한 배수를 곱합니다.

구성:
- **배수**: 모든 값에 곱할 수(기본값: 1.0)
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин маанилерин көрсөтүлгөн көбөйткүчкө көбөйтөт.

Тууралоо:
- **Көбөйткүч**: Бардык маанилер көбөйтүлө турган сан (демейки: 1.0)
    	]],
    	["lo"] = [[
ຄູນຄ່າຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດດ້ວຍຕົວຄູນທີ່ກຳນົດ.

ການກຳນົດຄ່າ:
- **ຕົວຄູນ**: ຈຳນວນທີ່ຈະໃຊ້ຄູນຄ່າທັງໝົດ (ຄ່າເລີ່ມຕົ້ນ: 1.0)
    	]],
    	["lv"] = [[
Reizina visu ienākošo datu punktu vērtības ar norādīto reizinātāju.

Konfigurācija:
- **Reizinātājs**: Skaitlis, ar kuru reizināt visas vērtības (pēc noklusējuma: 1.0)
    	]],
    	["lt"] = [[
Visas gaunamų duomenų taškų reikšmes padaugina iš nurodyto daugiklio.

Konfigūracija:
- **Daugiklis**: Skaičius, iš kurio dauginti visas reikšmes (numatytoji reikšmė: 1.0)
    	]],
    	["mk"] = [[
Ги множи сите влезни вредности на точките на податоци со зададен множител.

Конфигурација:
- **Множител**: Бројот со кој се множат сите вредности (стандардно: 1.0)
    	]],
    	["ms"] = [[
Mendarab semua nilai titik data yang masuk dengan pengganda yang ditentukan.

Konfigurasi:
- **Pengganda**: Nombor untuk mendarab semua nilai (lalai: 1.0)
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളെയും നിർദ്ദിഷ്ട ഗുണകം കൊണ്ട് ഗുണിക്കുന്നു.

കോൺഫിഗറേഷൻ:
- **Multiplier**: എല്ലാ മൂല്യങ്ങളെയും ഗുണിക്കേണ്ട സംഖ്യ (സ്ഥിരസ്ഥിതി: 1.0)
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सच्या मूल्यांचा निर्दिष्ट गुणकाने गुणाकार करते.

कॉन्फिगरेशन:
- **गुणक**: सर्व मूल्यांचा गुणाकार करण्यासाठीची संख्या (डीफॉल्ट: 1.0)
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн утгыг заасан үржүүлэгчээр үржүүлнэ.

Тохиргоо:
- **Үржүүлэгч**: Бүх утгыг үржүүлэх тоо (анхдагч: 1.0)
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका मानहरूलाई निर्दिष्ट गुणकले गुणन गर्छ।

कन्फिगरेसन:
- **गुणक**: सबै मानलाई गुणन गर्ने संख्या (पूर्वनिर्धारित: 1.0)
    	]],
    	["no"] = [[
Multipliserer verdiene til alle innkommende datapunkter med en angitt multiplikator.

Konfigurasjon:
- **Multiplikator**: Tallet alle verdier skal multipliseres med (standard: 1.0)
    	]],
    	["pl"] = [[
Mnoży wszystkie przychodzące wartości punktów danych przez określony mnożnik.

Konfiguracja:
- **Mnożnik**: Liczba, przez którą mnożone są wszystkie wartości (domyślnie: 1.0)
    	]],
    	["pt"] = [[
Multiplica todos os valores dos pontos de dados recebidos por um multiplicador especificado.

Configuração:
- **Multiplicador**: O número pelo qual multiplicar todos os valores (predefinição: 1.0)
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਮੁੱਲਾਂ ਨੂੰ ਨਿਰਧਾਰਤ ਗੁਣਕ ਨਾਲ ਗੁਣਾ ਕਰਦਾ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਗੁਣਕ**: ਸਾਰੇ ਮੁੱਲਾਂ ਨੂੰ ਗੁਣਾ ਕਰਨ ਲਈ ਸੰਖਿਆ (ਮੂਲ: 1.0)
    	]],
    	["ro"] = [[
Înmulțește toate valorile punctelor de date primite cu un multiplicator specificat.

Configurare:
- **Multiplicator**: Numărul cu care se înmulțesc toate valorile (implicit: 1.0)
    	]],
    	["rm"] = [[
Multiplichescha tut las valurs dals puncts da datas entrants cun in multiplicatur spezificà.

Configuraziun:
- **Multiplicatur**: Il numer cun il qual tut las valurs vegnan multiplicadas (standard: 1.0)
    	]],
    	["ru"] = [[
Умножает все входящие значения точек данных на заданный множитель.

Конфигурация:
- **Множитель**: Число, на которое умножаются все значения (по умолчанию: 1.0)
    	]],
    	["sr"] = [[
Množi sve dolazne vrednosti tačaka podataka zadatim množiteljem.

Konfiguracija:
- **Množilac**: Broj kojim se množe sve vrednosti (podrazumevano: 1.0)
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය අගයන් නිශ්චිත ගුණකයකින් ගුණ කරයි.

වින්‍යාසය:
- **ගුණකය**: සියලු අගයන් ගුණ කිරීමට භාවිත කරන සංඛ්‍යාව (පෙරනිමිය: 1.0)
    	]],
    	["sk"] = [[
Vynásobí všetky prichádzajúce hodnoty údajových bodov zadaným násobiteľom.

Konfigurácia:
- **Násobiteľ**: Číslo, ktorým sa vynásobia všetky hodnoty (predvolené: 1.0)
    	]],
    	["sl"] = [[
Pomnoži vse vhodne vrednosti podatkovnih točk z določenim množiteljem.

Konfiguracija:
- **Množitelj**: Število, s katerim se pomnožijo vse vrednosti (privzeto: 1.0)
    	]],
    	["es"] = [[
Multiplica todos los valores de los puntos de datos entrantes por un multiplicador especificado.

Configuración:
- **Multiplicador**: El número por el que se multiplicarán todos los valores (predeterminado: 1.0)
    	]],
    	["sw"] = [[
Huzidisha thamani zote za nukta za data zinazoingia kwa kizidishi maalum.

Usanidi:
- **Kizidishi**: Nambari ya kuzidisha thamani zote (chaguo-msingi: 1.0)
    	]],
    	["sv"] = [[
Multiplicerar alla inkommande datapunkters värden med en angiven multiplikator.

Konfiguration:
- **Multiplikator**: Talet som alla värden ska multipliceras med (standard: 1.0)
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி மதிப்புகளையும் குறிப்பிட்ட பெருக்கியால் பெருக்குகிறது.

உள்ளமைவு:
- **பெருக்கி**: அனைத்து மதிப்புகளையும் பெருக்க வேண்டிய எண் (இயல்புநிலை: 1.0)
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ విలువలను పేర్కొన్న గుణకంతో గుణిస్తుంది.

కాన్ఫిగరేషన్:
- **గుణకం**: అన్ని విలువలను గుణించాల్సిన సంఖ్య (డిఫాల్ట్: 1.0)
    	]],
    	["th"] = [[
คูณค่าของจุดข้อมูลขาเข้าทั้งหมดด้วยตัวคูณที่กำหนด

การกำหนดค่า:
- **ตัวคูณ**: ตัวเลขที่ใช้คูณค่าทั้งหมด (ค่าเริ่มต้น: 1.0)
    	]],
    	["tr"] = [[
Gelen tüm veri noktası değerlerini belirtilen çarpanla çarpar.

Yapılandırma:
- **Çarpan**: Tüm değerlerin çarpılacağı sayı (varsayılan: 1.0)
    	]],
    	["uk"] = [[
Множить усі вхідні значення точок даних на вказаний множник.

Конфігурація:
- **Множник**: Число, на яке множити всі значення (типово: 1.0)
    	]],
    	["vi"] = [[
Nhân tất cả giá trị điểm dữ liệu đầu vào với một hệ số được chỉ định.

Cấu hình:
- **Hệ số**: Số dùng để nhân tất cả giá trị (mặc định: 1.0)
    	]],
    },
    config = {
        number {
            id = "multiplier",
            name = {
            	["en"] = "Multiplier",
            	["af"] = "Vermenigvuldiger",
            	["sq"] = "Shumëzuesi",
            	["am"] = "ብዜት",
            	["hy"] = "Բազմապատկիչ",
            	["az"] = "Vurğu",
            	["bn"] = "গুণক",
            	["eu"] = "Biderkatzailea",
            	["be"] = "Множнік",
            	["bg"] = "Множител",
            	["my"] = "မြှောက်ကိန်း",
            	["ca"] = "Multiplicador",
            	["zh-Hans"] = "倍数",
            	["zh-Hant"] = "乘數",
            	["hr"] = "Množitelj",
            	["cs"] = "Násobitel",
            	["da"] = "Multiplikator",
            	["nl"] = "Vermenigvuldigingsfactor",
            	["et"] = "Kordaja",
            	["fil"] = "Multiplier",
            	["fi"] = "Kerroin",
            	["fr"] = "Multiplicateur",
            	["gl"] = "Multiplicador",
            	["ka"] = "გამამრავლებელი",
            	["de"] = "Multiplikator",
            	["el"] = "Πολλαπλασιαστής",
            	["gu"] = "ગુણક",
            	["hi"] = "गुणक",
            	["hu"] = "Szorzó",
            	["is"] = "Margfaldari",
            	["id"] = "Pengali",
            	["it"] = "Moltiplicatore",
            	["ja"] = "乗数",
            	["kn"] = "ಗುಣಕ",
            	["kk"] = "Көбейткіш",
            	["km"] = "មេគុណ",
            	["ko"] = "배수",
            	["ky"] = "Көбөйткүч",
            	["lo"] = "ຕົວຄູນ",
            	["lv"] = "Reizinātājs",
            	["lt"] = "Daugiklis",
            	["mk"] = "Множител",
            	["ms"] = "Pengganda",
            	["ml"] = "ഗുണകം",
            	["mr"] = "गुणक",
            	["mn"] = "Үржүүлэгч",
            	["ne"] = "गुणक",
            	["no"] = "Multiplikator",
            	["pl"] = "Mnożnik",
            	["pt"] = "Multiplicador",
            	["pa"] = "ਗੁਣਕ",
            	["ro"] = "Multiplicator",
            	["rm"] = "Multiplicatur",
            	["ru"] = "Множитель",
            	["sr"] = "Množilac",
            	["si"] = "ගුණකය",
            	["sk"] = "Násobiteľ",
            	["sl"] = "Množitelj",
            	["es"] = "Multiplicador",
            	["sw"] = "Kizidishi",
            	["sv"] = "Multiplikator",
            	["ta"] = "பெருக்கி",
            	["te"] = "గుణకం",
            	["th"] = "ตัวคูณ",
            	["tr"] = "Çarpan",
            	["uk"] = "Множник",
            	["vi"] = "Hệ số",
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local multiplier = config and config.multiplier or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value * multiplier

            return data_point
        end
    end
}
