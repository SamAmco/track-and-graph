-- Lua Function to divide data point values by a configurable number
-- This function divides all incoming data point values by a specified divisor

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "divide",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
    	["en"] = "Divide Values",
    	["af"] = "Deel Waardes",
    	["sq"] = "Pjesëto vlerat",
    	["am"] = "ዋጋዎችን ክፈል",
    	["hy"] = "Բաժանել արժեքները",
    	["az"] = "Qiymətləri böl",
    	["bn"] = "মান ভাগ করুন",
    	["eu"] = "Zatitu balioak",
    	["be"] = "Падзяліць значэнні",
    	["bg"] = "Деление на стойности",
    	["my"] = "တန်ဖိုးများကို စားခြင်း",
    	["ca"] = "Divideix els valors",
    	["zh-Hans"] = "除数据点值",
    	["zh-Hant"] = "除法運算",
    	["hr"] = "Dijeli vrijednosti",
    	["cs"] = "Dělit hodnoty",
    	["da"] = "Divider værdier",
    	["nl"] = "Waarden delen",
    	["et"] = "Jaga väärtusi",
    	["fil"] = "Hatiin ang mga Halaga",
    	["fi"] = "Jaa arvot",
    	["fr"] = "Diviser les valeurs",
    	["gl"] = "Dividir valores",
    	["ka"] = "მნიშვნელობების გაყოფა",
    	["de"] = "Werte dividieren",
    	["el"] = "Διαίρεση τιμών",
    	["gu"] = "મૂલ્યોને ભાગો",
    	["hi"] = "मानों को विभाजित करें",
    	["hu"] = "Értékek osztása",
    	["is"] = "Deila gildum",
    	["id"] = "Bagi Nilai",
    	["it"] = "Dividi valori",
    	["ja"] = "値を除算",
    	["kn"] = "ಮೌಲ್ಯಗಳನ್ನು ಭಾಗಿಸಿ",
    	["kk"] = "Мәндерді бөлу",
    	["km"] = "ចែកតម្លៃ",
    	["ko"] = "값 나누기",
    	["ky"] = "Маанилерди бөлүү",
    	["lo"] = "ຫານຄ່າ",
    	["lv"] = "Dalīt vērtības",
    	["lt"] = "Dalyti reikšmes",
    	["mk"] = "Дели ги вредностите",
    	["ms"] = "Bahagi Nilai",
    	["ml"] = "മൂല്യങ്ങൾ വിഭജിക്കുക",
    	["mr"] = "मूल्ये भागा",
    	["mn"] = "Утгуудыг хуваах",
    	["ne"] = "मानहरू भाग गर्नुहोस्",
    	["no"] = "Del verdier",
    	["pl"] = "Dziel wartości",
    	["pt"] = "Dividir valores",
    	["pa"] = "ਮੁੱਲਾਂ ਨੂੰ ਭਾਗ ਦਿਓ",
    	["ro"] = "Împarte valorile",
    	["rm"] = "Divider las valurs",
    	["ru"] = "Делить значения",
    	["sr"] = "Podeli vrednosti",
    	["si"] = "අගයන් බෙදන්න",
    	["sk"] = "Deliť hodnoty",
    	["sl"] = "Deli vrednosti",
    	["es"] = "Dividir valores",
    	["sw"] = "Gawa Thamani",
    	["sv"] = "Dividera värden",
    	["ta"] = "மதிப்புகளை வகு",
    	["te"] = "విలువలను భాగించు",
    	["th"] = "หารค่า",
    	["tr"] = "Değerleri Böl",
    	["uk"] = "Ділити значення",
    	["vi"] = "Chia giá trị",
    },
    description = {
    	["en"] = [[
Divides all incoming data point values by a specified divisor.

Configuration:
- **Divisor**: The number to divide all values by (default: 1.0)
    	]],
    	["af"] = [[
Deel alle inkomende datapuntwaardes deur ’n gespesifiseerde deler.

Konfigurasie:
- **Deler**: Die getal waardeur alle waardes gedeel word (verstek: 1.0)
    	]],
    	["sq"] = [[
Pjesëton të gjitha vlerat hyrëse të pikave të të dhënave me një pjesëtues të specifikuar.

Konfigurimi:
- **Pjesëtuesi**: Numri me të cilin pjesëtohen të gjitha vlerat (parazgjedhja: 1.0)
    	]],
    	["am"] = [[
ሁሉንም የሚመጡ የውሂብ ነጥቦች ዋጋዎች በተገለጸው አካፋይ ይከፍላል።

ውቅር፦
- **አካፋይ**፦ ሁሉንም ዋጋዎች ለመክፈል የሚያገለግል ቁጥር (ነባሪ፦ 1.0)
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի արժեքները բաժանում է նշված բաժանարարի վրա։

Կազմաձևում՝
- **Բաժանարար**․ բոլոր արժեքները բաժանելու թիվը (կանխադրված՝ 1.0)
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qiymətlərini göstərilən bölənə bölür.

Konfiqurasiya:
- **Bölən**: Bütün qiymətlərin bölünəcəyi ədəd (standart: 1.0)
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের মানকে নির্দিষ্ট ভাজক দিয়ে ভাগ করে।

কনফিগারেশন:
- **ভাজক**: সব মানকে যে সংখ্যা দিয়ে ভাগ করা হবে (ডিফল্ট: 1.0)
    	]],
    	["eu"] = [[
Sarrerako datu-puntuen balio guztiak zatitzaile zehatz batez zatitzen ditu.

Konfigurazioa:
- **Zatitzailea**: Balio guztiak zatitzeko zenbakia (lehenetsia: 1.0)
    	]],
    	["be"] = [[
Дзеліць усе ўваходныя значэнні кропак даных на зададзены дзельнік.

Канфігурацыя:
- **Дзялільнік**: лік, на які дзяліць усе значэнні (па змаўчанні: 1.0)
    	]],
    	["bg"] = [[
Дели всички входящи стойности на точките от данни със зададен делител.

Конфигурация:
- **Делител**: Числото, на което се делят всички стойности (по подразбиране: 1.0)
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာမှတ်တန်ဖိုးအားလုံးကို သတ်မှတ်ထားသော စားကိန်းဖြင့် စားသည်။

ဖွဲ့စည်းမှု:
- **စားကိန်း**: တန်ဖိုးအားလုံးကို စားမည့် ကိန်း (မူလ: 1.0)
    	]],
    	["ca"] = [[
Divideix tots els valors dels punts de dades entrants per un divisor especificat.

Configuració:
- **Divisor**: El nombre pel qual es divideixen tots els valors (per defecte: 1.0)
    	]],
    	["zh-Hans"] = [[
将所有传入数据点的值除以指定除数。

配置：
- **除数**：所有值要除以的数字（默认：1.0）
    	]],
    	["zh-Hant"] = [[
將所有輸入資料點的值除以指定的除數。

設定：
- **除數**：所有值要除以的數字（預設：1.0）
    	]],
    	["hr"] = [[
Dijeli sve dolazne vrijednosti podatkovnih točaka zadanim djeliteljem.

Konfiguracija:
- **Djelitelj**: Broj kojim se dijele sve vrijednosti (zadano: 1.0)
    	]],
    	["cs"] = [[
Vydělí všechny příchozí hodnoty datových bodů zadaným dělitelem.

Konfigurace:
- **Dělitel**: Číslo, kterým se všechny hodnoty vydělí (výchozí: 1.0)
    	]],
    	["da"] = [[
Dividerer alle indgående datapunkters værdier med en angivet divisor.

Konfiguration:
- **Divisor**: Tallet, som alle værdier divideres med (standard: 1.0)
    	]],
    	["nl"] = [[
Deelt alle binnenkomende waarden van gegevenspunten door een opgegeven deler.

Configuratie:
- **Deler**: Het getal waardoor alle waarden worden gedeeld (standaard: 1.0)
    	]],
    	["et"] = [[
Jagab kõik sisendandmepunktide väärtused määratud jagajaga.

Seadistus:
- **Jagaja**: arv, millega kõik väärtused jagatakse (vaikimisi: 1,0)
    	]],
    	["fil"] = [[
Hinahati ang lahat ng papasok na halaga ng data point sa tinukoy na divisor.

Configuration:
- **Divisor**: Numerong ipambabaha sa lahat ng halaga (default: 1.0)
    	]],
    	["fi"] = [[
Jakaa kaikki saapuvien datapisteiden arvot määritetyllä jakajalla.

Määritys:
- **Jakaja**: Luku, jolla kaikki arvot jaetaan (oletus: 1.0)
    	]],
    	["fr"] = [[
Divise toutes les valeurs des points de données entrants par un diviseur indiqué.

Configuration :
- **Diviseur** : Nombre par lequel diviser toutes les valeurs (par défaut : 1.0)
    	]],
    	["gl"] = [[
Divide todos os valores dos puntos de datos recibidos por un divisor especificado.

Configuración:
- **Divisor**: O número polo que se dividen todos os valores (predeterminado: 1.0)
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის მნიშვნელობას ყოფს მითითებულ გამყოფზე.

კონფიგურაცია:
- **გამყოფი**: რიცხვი, რომელზეც ყველა მნიშვნელობა უნდა გაიყოს (ნაგულისხმევი: 1.0)
    	]],
    	["de"] = [[
Dividiert alle eingehenden Datenpunktwerte durch einen angegebenen Divisor.

Konfiguration:
- **Divisor**: Die Zahl, durch die alle Werte dividiert werden (Standard: 1.0)
    	]],
    	["el"] = [[
Διαιρεί όλες τις εισερχόμενες τιμές σημείων δεδομένων με έναν καθορισμένο διαιρέτη.

Διαμόρφωση:
- **Διαιρέτης**: Ο αριθμός με τον οποίο διαιρούνται όλες οι τιμές (προεπιλογή: 1.0)
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના મૂલ્યોને નિર્દિષ્ટ ભાજક વડે ભાગે છે.

ગોઠવણી:
- **ભાજક**: બધા મૂલ્યોને જેના વડે ભાગવાના છે તે સંખ્યા (ડિફૉલ્ટ: 1.0)
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के मानों को निर्दिष्ट भाजक से विभाजित करता है।

कॉन्फ़िगरेशन:
- **भाजक**: सभी मानों को विभाजित करने वाली संख्या (डिफ़ॉल्ट: 1.0)
    	]],
    	["hu"] = [[
Az összes beérkező adatpont értékét a megadott osztóval osztja.

Konfiguráció:
- **Osztó**: Az összes érték osztásához használt szám (alapértelmezett: 1.0)
    	]],
    	["is"] = [[
Deilir öllum gildum innkomandi gagnapunkta með tilgreindum deili.

Stillingar:
- **Deilir**: Talan sem deila á öllum gildum með (sjálfgefið: 1.0)
    	]],
    	["id"] = [[
Membagi semua nilai titik data yang masuk dengan pembagi yang ditentukan.

Konfigurasi:
- **Pembagi**: Angka yang digunakan untuk membagi semua nilai (default: 1.0)
    	]],
    	["it"] = [[
Divide tutti i valori dei punti dati in arrivo per un divisore specificato.

Configurazione:
- **Divisore**: il numero per cui dividere tutti i valori (predefinito: 1.0)
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントの値を、指定した除数で割ります。

設定:
- **除数**: すべての値を割る数（デフォルト: 1.0）
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳನ್ನು ನಿರ್ದಿಷ್ಟ ಭಾಜಕದಿಂದ ಭಾಗಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಭಾಜಕ**: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳನ್ನು ಭಾಗಿಸುವ ಸಂಖ್ಯೆ (ಡೀಫಾಲ್ಟ್: 1.0)
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің мәндерін көрсетілген бөлгішке бөледі.

Конфигурация:
- **Бөлгіш**: Барлық мәндерді бөлуге арналған сан (әдепкі: 1.0)
    	]],
    	["km"] = [[
ចែកតម្លៃចំណុចទិន្នន័យចូលទាំងអស់ដោយចំនួនចែកដែលបានកំណត់។

ការកំណត់៖
- **ចំនួនចែក**៖ ចំនួនដែលត្រូវប្រើចែកតម្លៃទាំងអស់ (លំនាំដើម៖ 1.0)
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 값을 지정한 제수로 나눕니다.

구성:
- **제수**: 모든 값을 나눌 수(기본값: 1.0)
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин маанилерин көрсөтүлгөн бөлгүчкө бөлөт.

Тууралоо:
- **Бөлгүч**: Бардык маанилер бөлүнө турган сан (демейки: 1.0)
    	]],
    	["lo"] = [[
ຫານຄ່າຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດດ້ວຍຕົວຫານທີ່ກຳນົດ.

ການຕັ້ງຄ່າ:
- **ຕົວຫານ**: ຈຳນວນທີ່ໃຊ້ຫານຄ່າທັງໝົດ (ຄ່າເລີ່ມຕົ້ນ: 1.0)
    	]],
    	["lv"] = [[
Dala visu ienākošo datu punktu vērtības ar norādīto dalītāju.

Konfigurācija:
- **Dalītājs**: Skaitlis, ar kuru dalīt visas vērtības (noklusējums: 1.0)
    	]],
    	["lt"] = [[
Visas gaunamų duomenų taškų reikšmes padalija iš nurodyto daliklio.

Konfigūracija:
- **Daliklis**: Skaičius, iš kurio dalyti visas reikšmes (numatytoji reikšmė: 1.0)
    	]],
    	["mk"] = [[
Ги дели сите влезни вредности на точките на податоци со зададен делител.

Конфигурација:
- **Делител**: Бројот со кој се делат сите вредности (стандардно: 1.0)
    	]],
    	["ms"] = [[
Membahagikan semua nilai titik data masuk dengan pembahagi yang ditentukan.

Konfigurasi:
- **Pembahagi**: Nombor untuk membahagikan semua nilai (lalai: 1.0)
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളെയും നിർദ്ദിഷ്ട വിഭാജകം കൊണ്ട് വിഭജിക്കുന്നു.

കോൺഫിഗറേഷൻ:
- **Divisor**: എല്ലാ മൂല്യങ്ങളെയും വിഭജിക്കേണ്ട സംഖ്യ (സ്ഥിരസ്ഥിതി: 1.0)
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा बिंदूंच्या मूल्यांना निर्दिष्ट भाजकाने भागते.

कॉन्फिगरेशन:
- **भाजक**: सर्व मूल्यांना ज्या संख्येने भागायचे ती संख्या (डीफॉल्ट: 1.0)
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн утгыг заасан хуваагчаар хуваана.

Тохиргоо:
- **Хуваагч**: Бүх утгыг хуваах тоо (анхдагч: 1.0)
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका मानहरूलाई निर्दिष्ट भाजकले भाग गर्छ।

कन्फिगरेसन:
- **भाजक**: सबै मानलाई भाग गर्ने संख्या (पूर्वनिर्धारित: 1.0)
    	]],
    	["no"] = [[
Deler verdiene til alle innkommende datapunkter på en angitt divisor.

Konfigurasjon:
- **Divisor**: Tallet alle verdier skal deles på (standard: 1.0)
    	]],
    	["pl"] = [[
Dzieli wszystkie przychodzące wartości punktów danych przez określony dzielnik.

Konfiguracja:
- **Dzielnik**: Liczba, przez którą dzielone są wszystkie wartości (domyślnie: 1.0)
    	]],
    	["pt"] = [[
Divide todos os valores dos pontos de dados recebidos por um divisor especificado.

Configuração:
- **Divisor**: O número pelo qual dividir todos os valores (predefinição: 1.0)
    	]],
    	["pa"] = [[
ਸਾਰੇ ਆਉਣ ਵਾਲੇ ਡਾਟਾ ਪੁਆਇੰਟਾਂ ਦੇ ਮੁੱਲਾਂ ਨੂੰ ਨਿਰਧਾਰਤ ਭਾਜਕ ਨਾਲ ਭਾਗ ਦਿੰਦਾ ਹੈ।

ਕਨਫਿਗਰੇਸ਼ਨ:
- **ਭਾਜਕ**: ਸਾਰੇ ਮੁੱਲਾਂ ਨੂੰ ਭਾਗ ਦੇਣ ਵਾਲੀ ਸੰਖਿਆ (ਡਿਫਾਲਟ: 1.0)
    	]],
    	["ro"] = [[
Împarte toate valorile punctelor de date primite la un divizor specificat.

Configurare:
- **Divizor**: Numărul la care se împart toate valorile (implicit: 1.0)
    	]],
    	["rm"] = [[
Divida tut las valurs dals puncts da datas entrants tras in divisur specificà.

Configuraziun:
- **Divisur**: Il dumber tras il qual divider tut las valurs (default: 1.0)
    	]],
    	["ru"] = [[
Делит все входящие значения точек данных на заданный делитель.

Конфигурация:
- **Делитель**: Число, на которое делятся все значения (по умолчанию: 1.0)
    	]],
    	["sr"] = [[
Deli sve dolazne vrednosti tačaka podataka zadatim deliocom.

Konfiguracija:
- **Delilac**: Broj kojim se dele sve vrednosti (podrazumevano: 1.0)
    	]],
    	["si"] = [[
එන සියලු දත්ත ලක්ෂ්‍ය අගයන් නිශ්චිත භාජකයකින් බෙදයි.

වින්‍යාසය:
- **භාජකය**: සියලු අගයන් බෙදීමට භාවිත කරන සංඛ්‍යාව (පෙරනිමිය: 1.0)
    	]],
    	["sk"] = [[
Vydelí všetky prichádzajúce hodnoty údajových bodov zadaným deliteľom.

Konfigurácia:
- **Deliteľ**: Číslo, ktorým sa vydelia všetky hodnoty (predvolené: 1.0)
    	]],
    	["sl"] = [[
Deli vse vhodne vrednosti podatkovnih točk z določenim deliteljem.

Konfiguracija:
- **Delitelj**: Število, s katerim se delijo vse vrednosti (privzeto: 1.0)
    	]],
    	["es"] = [[
Divide todos los valores de los puntos de datos entrantes entre un divisor especificado.

Configuración:
- **Divisor**: El número entre el que se dividirán todos los valores (predeterminado: 1.0)
    	]],
    	["sw"] = [[
Hugawa thamani zote za nukta za data zinazoingia kwa kigawanyo maalum.

Usanidi:
- **Kigawanyo**: Nambari ya kugawanya thamani zote (chaguo-msingi: 1.0)
    	]],
    	["sv"] = [[
Dividerar alla inkommande datapunkters värden med en angiven divisor.

Konfiguration:
- **Divisor**: Talet som alla värden ska divideras med (standard: 1.0)
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி மதிப்புகளையும் குறிப்பிட்ட வகுத்தியால் வகுக்கிறது.

உள்ளமைவு:
- **வகுத்தி**: அனைத்து மதிப்புகளையும் வகுக்க வேண்டிய எண் (இயல்புநிலை: 1.0)
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ విలువలను పేర్కొన్న భాగహారంతో భాగిస్తుంది.

కాన్ఫిగరేషన్:
- **భాగహారం**: అన్ని విలువలను భాగించాల్సిన సంఖ్య (డిఫాల్ట్: 1.0)
    	]],
    	["th"] = [[
หารค่าของจุดข้อมูลขาเข้าทั้งหมดด้วยตัวหารที่กำหนด

การกำหนดค่า:
- **ตัวหาร**: ตัวเลขที่ใช้หารค่าทั้งหมด (ค่าเริ่มต้น: 1.0)
    	]],
    	["tr"] = [[
Gelen tüm veri noktası değerlerini belirtilen bölen sayıya böler.

Yapılandırma:
- **Bölen**: Tüm değerlerin bölüneceği sayı (varsayılan: 1.0)
    	]],
    	["uk"] = [[
Ділить усі вхідні значення точок даних на вказаний дільник.

Конфігурація:
- **Дільник**: Число, на яке ділити всі значення (типово: 1.0)
    	]],
    	["vi"] = [[
Chia tất cả giá trị điểm dữ liệu đầu vào cho một số chia được chỉ định.

Cấu hình:
- **Số chia**: Số dùng để chia tất cả giá trị (mặc định: 1.0)
    	]],
    },
    config = {
        number {
            id = "divisor",
            default = 1.0,
            name = {
            	["en"] = "Divisor",
            	["af"] = "Deler",
            	["sq"] = "Pjesëtuesi",
            	["am"] = "አካፋይ",
            	["hy"] = "Բաժանարար",
            	["az"] = "Bölən",
            	["bn"] = "ভাজক",
            	["eu"] = "Zatitzailea",
            	["be"] = "Дзялільнік",
            	["bg"] = "Делител",
            	["my"] = "စားကိန်း",
            	["ca"] = "Divisor",
            	["zh-Hans"] = "除数",
            	["zh-Hant"] = "除數",
            	["hr"] = "Djelitelj",
            	["cs"] = "Dělitel",
            	["da"] = "Divisor",
            	["nl"] = "Deler",
            	["et"] = "Jagaja",
            	["fil"] = "Divisor",
            	["fi"] = "Jakaja",
            	["fr"] = "Diviseur",
            	["gl"] = "Divisor",
            	["ka"] = "გამყოფი",
            	["de"] = "Divisor",
            	["el"] = "Διαιρέτης",
            	["gu"] = "ભાજક",
            	["hi"] = "भाजक",
            	["hu"] = "Osztó",
            	["is"] = "Deilir",
            	["id"] = "Pembagi",
            	["it"] = "Divisore",
            	["ja"] = "除数",
            	["kn"] = "ಭಾಜಕ",
            	["kk"] = "Бөлгіш",
            	["km"] = "ចំនួនចែក",
            	["ko"] = "제수",
            	["ky"] = "Бөлгүч",
            	["lo"] = "ຕົວຫານ",
            	["lv"] = "Dalītājs",
            	["lt"] = "Daliklis",
            	["mk"] = "Делител",
            	["ms"] = "Pembahagi",
            	["ml"] = "വിഭാജകം",
            	["mr"] = "भाजक",
            	["mn"] = "Хуваагч",
            	["ne"] = "भाजक",
            	["no"] = "Divisor",
            	["pl"] = "Dzielnik",
            	["pt"] = "Divisor",
            	["pa"] = "ਭਾਜਕ",
            	["ro"] = "Divizor",
            	["rm"] = "Divisur",
            	["ru"] = "Делитель",
            	["sr"] = "Delilac",
            	["si"] = "භාජකය",
            	["sk"] = "Deliteľ",
            	["sl"] = "Delitelj",
            	["es"] = "Divisor",
            	["sw"] = "Kigawanyo",
            	["sv"] = "Divisor",
            	["ta"] = "வகுத்தி",
            	["te"] = "భాగహారం",
            	["th"] = "ตัวหาร",
            	["tr"] = "Bölen",
            	["uk"] = "Дільник",
            	["vi"] = "Số chia",
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local divisor = config and config.divisor or 1.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value / divisor

            return data_point
        end
    end
}
