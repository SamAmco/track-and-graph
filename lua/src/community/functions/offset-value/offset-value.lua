-- Lua Function to offset data point values by a configurable number
-- This function adds a constant offset to all incoming data point values

local number = require("tng.config").number

return {
    -- Configuration metadata
    id = "offset-value",
    version = "1.0.1",
    inputCount = 1,
    categories = {"_arithmetic"},
    title = {
    	["en"] = "Offset Value",
    	["af"] = "Waardeverstelling",
    	["sq"] = "Zhvendosja e vlerës",
    	["am"] = "የእሴት ማካካሻ",
    	["hy"] = "Արժեքի շեղում",
    	["az"] = "Ofset qiyməti",
    	["bn"] = "মান অফসেট করুন",
    	["eu"] = "Desplazatu balioa",
    	["be"] = "Зрух значэння",
    	["bg"] = "Отместване на стойността",
    	["my"] = "တန်ဖိုးအော့ဖ်ဆက်",
    	["ca"] = "Desplaçament del valor",
    	["zh-Hans"] = "偏移值",
    	["zh-Hant"] = "偏移值",
    	["hr"] = "Pomak vrijednosti",
    	["cs"] = "Posun hodnoty",
    	["da"] = "Forskyd værdi",
    	["nl"] = "Waarde verschuiven",
    	["et"] = "Nihkeväärtus",
    	["fil"] = "Offset na Halaga",
    	["fi"] = "Siirrä arvoa",
    	["fr"] = "Décaler la valeur",
    	["gl"] = "Desprazamento do valor",
    	["ka"] = "მნიშვნელობის წანაცვლება",
    	["de"] = "Wertoffset",
    	["el"] = "Μετατόπιση τιμής",
    	["gu"] = "ઑફસેટ મૂલ્ય",
    	["hi"] = "ऑफ़सेट मान",
    	["hu"] = "Érték eltolása",
    	["is"] = "Hliðra gildi",
    	["id"] = "Offset Nilai",
    	["it"] = "Offset del valore",
    	["ja"] = "値をオフセット",
    	["kn"] = "ಆಫ್‌ಸೆಟ್ ಮೌಲ್ಯ",
    	["kk"] = "Мәнге ығысу қосу",
    	["km"] = "អុហ្វសិតតម្លៃ",
    	["ko"] = "오프셋 값",
    	["ky"] = "Мааниге жылыш кошуу",
    	["lo"] = "ຄ່າຊົດເຊີຍ",
    	["lv"] = "Nobīdes vērtība",
    	["lt"] = "Reikšmės poslinkis",
    	["mk"] = "Поместување на вредноста",
    	["ms"] = "Ofset Nilai",
    	["ml"] = "ഓഫ്സെറ്റ് മൂല്യം",
    	["mr"] = "मूल्य ऑफसेट",
    	["mn"] = "Утгын шилжилт",
    	["ne"] = "अफसेट मान",
    	["no"] = "Forskyv verdi",
    	["pl"] = "Przesunięcie wartości",
    	["pt"] = "Deslocamento do valor",
    	["pa"] = "ਆਫਸੈੱਟ ਮੁੱਲ",
    	["ro"] = "Decalaj de valoare",
    	["rm"] = "Valur d'offset",
    	["ru"] = "Смещение значения",
    	["sr"] = "Pomeraj vrednosti",
    	["si"] = "අගය විස්ථාපනය",
    	["sk"] = "Posun hodnoty",
    	["sl"] = "Zamik vrednosti",
    	["es"] = "Desplazar valor",
    	["sw"] = "Thamani ya Offset",
    	["sv"] = "Förskjut värde",
    	["ta"] = "ஆஃப்செட் மதிப்பு",
    	["te"] = "ఆఫ్‌సెట్ విలువ",
    	["th"] = "ออฟเซ็ตค่า",
    	["tr"] = "Değer Kaydırması",
    	["uk"] = "Зміщення значення",
    	["vi"] = "Độ lệch giá trị",
    },
    description = {
    	["en"] = [[
Adds a constant offset to all incoming data point values.

Configuration:
- **Offset**: The number to add to all values (default: 0.0). Use negative values to subtract.
    	]],
    	["af"] = [[
Voeg ’n konstante verstelling by alle inkomende datapuntwaardes.

Konfigurasie:
- **Verstelling**: Die getal wat by alle waardes gevoeg word (verstek: 0.0). Gebruik negatiewe waardes om af te trek.
    	]],
    	["sq"] = [[
I shton një zhvendosje konstante të gjitha vlerave hyrëse të pikave të të dhënave.

Konfigurimi:
- **Zhvendosja**: Numri që u shtohet të gjitha vlerave (parazgjedhja: 0.0). Përdor vlera negative për zbritje.
    	]],
    	["am"] = [[
ቋሚ ማካካሻን በሁሉም የሚገቡ የውሂብ ነጥብ እሴቶች ላይ ይጨምራል።

ውቅር፦
- **ማካካሻ**፦ በሁሉም እሴቶች ላይ የሚጨመር ቁጥር (ነባሪ፦ 0.0)። ለመቀነስ አሉታዊ እሴቶችን ይጠቀሙ።
    	]],
    	["hy"] = [[
Բոլոր մուտքային տվյալակետերի արժեքներին ավելացնում է հաստատուն շեղում։

Կազմաձևում՝
- **Շեղում**․ բոլոր արժեքներին ավելացվող թիվը (կանխադրված՝ 0.0)։ Հանելու համար օգտագործեք բացասական արժեքներ։
    	]],
    	["az"] = [[
Daxil olan bütün məlumat nöqtələrinin qiymətlərinə sabit ofset əlavə edir.

Konfiqurasiya:
- **Ofset**: Bütün qiymətlərə əlavə ediləcək ədəd (standart: 0.0). Çıxmaq üçün mənfi qiymətlərdən istifadə edin.
    	]],
    	["bn"] = [[
সমস্ত আগত ডেটা পয়েন্টের মানের সঙ্গে একটি ধ্রুবক অফসেট যোগ করে।

কনফিগারেশন:
- **অফসেট**: সব মানের সঙ্গে যে সংখ্যা যোগ করা হবে (ডিফল্ট: 0.0)। বিয়োগ করতে ঋণাত্মক মান ব্যবহার করুন।
    	]],
    	["eu"] = [[
Desplazamendu konstante bat gehitzen die sarrerako datu-puntuen balio guztiei.

Konfigurazioa:
- **Desplazamendua**: Balio guztiei gehitu beharreko zenbakia (lehenetsia: 0.0). Erabili balio negatiboak kenketa egiteko.
    	]],
    	["be"] = [[
Дадае пастаянны зрух да ўсіх уваходных значэнняў кропак даных.

Канфігурацыя:
- **Зрух**: лік, які дадаецца да ўсіх значэнняў (па змаўчанні: 0.0). Выкарыстоўвайце адмоўныя значэнні для аднімання.
    	]],
    	["bg"] = [[
Добавя постоянно отместване към всички входящи стойности на точките от данни.

Конфигурация:
- **Отместване**: Числото, което се добавя към всички стойности (по подразбиране: 0.0). Използвайте отрицателни стойности за изваждане.
    	]],
    	["my"] = [[
ဝင်လာသော ဒေတာအမှတ်တန်ဖိုးများအားလုံးထံ အမြဲတမ်းအော့ဖ်ဆက်တန်ဖိုးတစ်ခု ပေါင်းထည့်သည်။

ပြင်ဆင်သတ်မှတ်မှုများ:
- **အော့ဖ်ဆက်**: တန်ဖိုးအားလုံးထံ ပေါင်းထည့်မည့် ကိန်း (မူလ: 0.0)။ နုတ်ရန် အနုတ်တန်ဖိုးများကို အသုံးပြုပါ။
    	]],
    	["ca"] = [[
Afegeix un desplaçament constant a tots els valors dels punts de dades entrants.

Configuració:
- **Desplaçament**: El nombre que cal afegir a tots els valors (per defecte: 0.0). Utilitza valors negatius per restar.
    	]],
    	["zh-Hans"] = [[
为所有传入数据点的值添加固定偏移量。

配置：
- **偏移量**：要添加到所有值的数字（默认：0.0）。使用负值进行减法。
    	]],
    	["zh-Hant"] = [[
為所有輸入資料點的值加上固定偏移量。

設定：
- **偏移量**：要加到所有值的數字（預設：0.0）。使用負值可進行減法。
    	]],
    	["hr"] = [[
Dodaje konstantni pomak svim dolaznim vrijednostima podatkovnih točaka.

Konfiguracija:
- **Pomak**: Broj koji se dodaje svim vrijednostima (zadano: 0.0). Upotrijebite negativne vrijednosti za oduzimanje.
    	]],
    	["cs"] = [[
Přičte ke všem příchozím hodnotám datových bodů konstantní posun.

Konfigurace:
- **Posun**: Číslo, které se přičte ke všem hodnotám (výchozí: 0.0). Pro odečtení použijte záporné hodnoty.
    	]],
    	["da"] = [[
Tilføjer en konstant forskydning til alle indgående datapunkters værdier.

Konfiguration:
- **Forskydning**: Tallet, der skal lægges til alle værdier (standard: 0.0). Brug negative værdier til at trække fra.
    	]],
    	["nl"] = [[
Voegt een vaste verschuiving toe aan alle binnenkomende waarden van datapunten.

Configuratie:
- **Verschuiving**: Het getal dat bij alle waarden wordt opgeteld (standaard: 0.0). Gebruik negatieve waarden om af te trekken.
    	]],
    	["et"] = [[
Liidab kõikidele sisendandmepunktide väärtustele konstantse nihke.

Seadistus:
- **Nihe**: kõikidele väärtustele liidetav arv (vaikimisi: 0,0). Lahutamiseks kasuta negatiivseid väärtusi.
    	]],
    	["fil"] = [[
Nagdadaragdag ng constant offset sa lahat ng papasok na halaga ng data point.

Configuration:
- **Offset**: Numerong idaragdag sa lahat ng halaga (default: 0.0). Gumamit ng negatibong halaga para magbawas.
    	]],
    	["fi"] = [[
Lisää vakiosiirtymän kaikkiin saapuvien datapisteiden arvoihin.

Määritys:
- **Siirtymä**: Kaikkiin arvoihin lisättävä luku (oletus: 0.0). Käytä negatiivisia arvoja vähentämiseen.
    	]],
    	["fr"] = [[
Ajoute un décalage constant à toutes les valeurs des points de données entrants.

Configuration :
- **Décalage** : Nombre à ajouter à toutes les valeurs (par défaut : 0.0). Utilisez des valeurs négatives pour soustraire.
    	]],
    	["gl"] = [[
Engade un desprazamento constante a todos os valores dos puntos de datos recibidos.

Configuración:
- **Desprazamento**: O número que se engadirá a todos os valores (predeterminado: 0.0). Usa valores negativos para restar.
    	]],
    	["ka"] = [[
ყველა შემომავალი მონაცემის წერტილის მნიშვნელობას უმატებს მუდმივ წანაცვლებას.

კონფიგურაცია:
- **წანაცვლება**: რიცხვი, რომელიც ყველა მნიშვნელობას უნდა დაემატოს (ნაგულისხმევი: 0.0). გამოკლებისთვის გამოიყენეთ უარყოფითი მნიშვნელობები.
    	]],
    	["de"] = [[
Addiert einen konstanten Offset zu allen eingehenden Datenpunktwerten.

Konfiguration:
- **Offset**: Die zu allen Werten zu addierende Zahl (Standard: 0.0). Verwende negative Werte zum Subtrahieren.
    	]],
    	["el"] = [[
Προσθέτει μια σταθερή μετατόπιση σε όλες τις εισερχόμενες τιμές σημείων δεδομένων.

Διαμόρφωση:
- **Μετατόπιση**: Ο αριθμός που θα προστεθεί σε όλες τις τιμές (προεπιλογή: 0.0). Χρησιμοποιήστε αρνητικές τιμές για αφαίρεση.
    	]],
    	["gu"] = [[
આવતા તમામ ડેટા પોઇન્ટના મૂલ્યોમાં એક સ્થિર ઑફસેટ ઉમેરે છે.

ગોઠવણી:
- **ઑફસેટ**: બધા મૂલ્યોમાં ઉમેરવાની સંખ્યા (ડિફૉલ્ટ: 0.0). બાદબાકી કરવા માટે ઋણ મૂલ્યો વાપરો.
    	]],
    	["hi"] = [[
सभी आने वाले डेटा पॉइंट के मानों में एक स्थिर ऑफ़सेट जोड़ता है।

कॉन्फ़िगरेशन:
- **ऑफ़सेट**: सभी मानों में जोड़ने वाली संख्या (डिफ़ॉल्ट: 0.0)। घटाने के लिए ऋणात्मक मानों का उपयोग करें।
    	]],
    	["hu"] = [[
Állandó eltolást ad az összes beérkező adatpont értékéhez.

Konfiguráció:
- **Eltolás**: Az összes értékhez hozzáadandó szám (alapértelmezett: 0.0). Kivonáshoz használjon negatív értéket.
    	]],
    	["is"] = [[
Leggur fasta hliðrun við öll gildi innkomandi gagnapunkta.

Stillingar:
- **Hliðrun**: Talan sem leggja á við öll gildi (sjálfgefið: 0.0). Notaðu neikvæð gildi til að draga frá.
    	]],
    	["id"] = [[
Menambahkan offset konstan ke semua nilai titik data yang masuk.

Konfigurasi:
- **Offset**: Angka yang ditambahkan ke semua nilai (default: 0.0). Gunakan nilai negatif untuk mengurangi.
    	]],
    	["it"] = [[
Aggiunge un offset costante a tutti i valori dei punti dati in arrivo.

Configurazione:
- **Offset**: il numero da aggiungere a tutti i valori (predefinito: 0.0). Usa valori negativi per sottrarre.
    	]],
    	["ja"] = [[
入力されたすべてのデータポイントの値に、一定のオフセットを加えます。

設定:
- **オフセット**: すべての値に加える数（デフォルト: 0.0）。減算するには負の値を使用します。
    	]],
    	["kn"] = [[
ಬರುವ ಎಲ್ಲಾ ಡೇಟಾ ಬಿಂದುಗಳ ಮೌಲ್ಯಗಳಿಗೆ ಸ್ಥಿರ ಆಫ್‌ಸೆಟ್ ಸೇರಿಸುತ್ತದೆ.

ಕಾನ್ಫಿಗರೇಶನ್:
- **ಆಫ್‌ಸೆಟ್**: ಎಲ್ಲಾ ಮೌಲ್ಯಗಳಿಗೆ ಸೇರಿಸಬೇಕಾದ ಸಂಖ್ಯೆ (ಡೀಫಾಲ್ಟ್: 0.0). ಕಡಿತಗೊಳಿಸಲು ಋಣ ಮೌಲ್ಯಗಳನ್ನು ಬಳಸಿ.
    	]],
    	["kk"] = [[
Барлық кіріс дерек нүктелерінің мәндеріне тұрақты ығысуды қосады.

Конфигурация:
- **Ығысу**: Барлық мәндерге қосылатын сан (әдепкі: 0.0). Азайту үшін теріс мәндерді пайдаланыңыз.
    	]],
    	["km"] = [[
បន្ថែមអុហ្វសិតថេរទៅតម្លៃរបស់ចំណុចទិន្នន័យចូលទាំងអស់។

ការកំណត់រចនា៖
- **អុហ្វសិត**៖ លេខដែលត្រូវបន្ថែមទៅតម្លៃទាំងអស់ (លំនាំដើម៖ 0.0)។ ប្រើតម្លៃអវិជ្ជមានដើម្បីដក។
    	]],
    	["ko"] = [[
들어오는 모든 데이터 포인트 값에 일정한 오프셋을 더합니다.

구성:
- **오프셋**: 모든 값에 더할 수(기본값: 0.0). 빼려면 음수를 사용합니다.
    	]],
    	["ky"] = [[
Бардык келген маалымат чекиттеринин маанилерине туруктуу жылыш кошот.

Тууралоо:
- **Жылыш**: Бардык маанилерге кошула турган сан (демейки: 0.0). Алуу үчүн терс маанилерди колдонуңуз.
    	]],
    	["lo"] = [[
ເພີ່ມຄ່າຊົດເຊີຍຄົງທີ່ໃຫ້ກັບຄ່າຂອງຈຸດຂໍ້ມູນຂາເຂົ້າທັງໝົດ.

ການກຳນົດຄ່າ:
- **ຄ່າຊົດເຊີຍ**: ຈຳນວນທີ່ຈະເພີ່ມໃຫ້ຄ່າທັງໝົດ (ຄ່າເລີ່ມຕົ້ນ: 0.0). ໃຊ້ຄ່າລົບເພື່ອຫັກອອກ.
    	]],
    	["lv"] = [[
Pieskaita nemainīgu nobīdi visām ienākošo datu punktu vērtībām.

Konfigurācija:
- **Nobīde**: Skaitlis, ko pieskaitīt visām vērtībām (pēc noklusējuma: 0.0). Izmantojiet negatīvas vērtības, lai atņemtu.
    	]],
    	["lt"] = [[
Prie visų gaunamų duomenų taškų reikšmių prideda pastovų poslinkį.

Konfigūracija:
- **Poslinkis**: Skaičius, kurį pridėti prie visų reikšmių (numatytoji reikšmė: 0.0). Norėdami atimti, naudokite neigiamas reikšmes.
    	]],
    	["mk"] = [[
Додава константно поместување на сите влезни вредности на точките на податоци.

Конфигурација:
- **Поместување**: Бројот што се додава на сите вредности (стандардно: 0.0). Користете негативни вредности за одземање.
    	]],
    	["ms"] = [[
Menambah ofset malar kepada semua nilai titik data yang masuk.

Konfigurasi:
- **Ofset**: Nombor untuk ditambah kepada semua nilai (lalai: 0.0). Gunakan nilai negatif untuk menolak.
    	]],
    	["ml"] = [[
എത്തുന്ന എല്ലാ ഡാറ്റാ പോയിന്റ് മൂല്യങ്ങളിലേക്കും ഒരു സ്ഥിരമായ ഓഫ്സെറ്റ് ചേർക്കുന്നു.

കോൺഫിഗറേഷൻ:
- **Offset**: എല്ലാ മൂല്യങ്ങളിലേക്കും ചേർക്കേണ്ട സംഖ്യ (സ്ഥിരസ്ഥിതി: 0.0). കുറയ്ക്കാൻ നെഗറ്റീവ് മൂല്യങ്ങൾ ഉപയോഗിക്കുക.
    	]],
    	["mr"] = [[
येणाऱ्या सर्व डेटा पॉइंट्सच्या मूल्यांमध्ये स्थिर ऑफसेट जोडते.

कॉन्फिगरेशन:
- **ऑफसेट**: सर्व मूल्यांमध्ये जोडायची संख्या (डीफॉल्ट: 0.0). वजा करण्यासाठी ऋण मूल्ये वापरा.
    	]],
    	["mn"] = [[
Ирж буй бүх өгөгдлийн цэгийн утгад тогтмол зөрүү нэмнэ.

Тохиргоо:
- **Зөрүү**: Бүх утгад нэмэх тоо (анхдагч: 0.0). Хасахын тулд сөрөг утга ашиглана.
    	]],
    	["ne"] = [[
आउने सबै डेटा बिन्दुका मानमा स्थिर अफसेट थप्छ।

कन्फिगरेसन:
- **अफसेट**: सबै मानमा थप्ने संख्या (पूर्वनिर्धारित: 0.0)। घटाउन ऋणात्मक मान प्रयोग गर्नुहोस्।
    	]],
    	["no"] = [[
Legger til en konstant forskyvning i verdiene til alle innkommende datapunkter.

Konfigurasjon:
- **Forskyvning**: Tallet som skal legges til alle verdier (standard: 0.0). Bruk negative verdier for å trekke fra.
    	]],
    	["pl"] = [[
Dodaje stałe przesunięcie do wszystkich przychodzących wartości punktów danych.

Konfiguracja:
- **Przesunięcie**: Liczba dodawana do wszystkich wartości (domyślnie: 0.0). Użyj wartości ujemnych, aby odejmować.
    	]],
    	["pt"] = [[
Adiciona um deslocamento constante a todos os valores dos pontos de dados recebidos.

Configuração:
- **Deslocamento**: O número a adicionar a todos os valores (predefinição: 0.0). Use valores negativos para subtrair.
    	]],
    	["pa"] = [[
ਆਉਣ ਵਾਲੇ ਸਾਰੇ ਡਾਟਾ ਪੁਆਇੰਟ ਮੁੱਲਾਂ ਵਿੱਚ ਇੱਕ ਸਥਿਰ ਆਫਸੈੱਟ ਜੋੜਦਾ ਹੈ।

ਕੌਂਫਿਗਰੇਸ਼ਨ:
- **ਆਫਸੈੱਟ**: ਸਾਰੇ ਮੁੱਲਾਂ ਵਿੱਚ ਜੋੜਨ ਲਈ ਸੰਖਿਆ (ਮੂਲ: 0.0)। ਘਟਾਉਣ ਲਈ ਨਕਾਰਾਤਮਕ ਮੁੱਲ ਵਰਤੋ।
    	]],
    	["ro"] = [[
Adaugă un decalaj constant la toate valorile punctelor de date primite.

Configurare:
- **Decalaj**: Numărul adăugat la toate valorile (implicit: 0.0). Folosește valori negative pentru scădere.
    	]],
    	["rm"] = [[
Agiunta in offset constant a tut las valurs dals puncts da datas entrants.

Configuraziun:
- **Offset**: Il numer d'agiuntar a tut las valurs (standard: 0.0). Utilisescha valurs negativas per sottrair.
    	]],
    	["ru"] = [[
Добавляет постоянное смещение ко всем входящим значениям точек данных.

Конфигурация:
- **Смещение**: Число, добавляемое ко всем значениям (по умолчанию: 0.0). Используйте отрицательные значения для вычитания.
    	]],
    	["sr"] = [[
Dodaje konstantni pomeraj svim dolaznim vrednostima tačaka podataka.

Konfiguracija:
- **Pomeraj**: Broj koji se dodaje svim vrednostima (podrazumevano: 0.0). Koristite negativne vrednosti za oduzimanje.
    	]],
    	["si"] = [[
ලැබෙන සියලු දත්ත ලක්ෂ්‍ය අගයන්ට ස්ථිර විස්ථාපනයක් එකතු කරයි.

වින්‍යාසය:
- **විස්ථාපනය**: සියලු අගයන්ට එකතු කළ යුතු සංඛ්‍යාව (පෙරනිමිය: 0.0). අඩු කිරීමට ඍණ අගයන් භාවිත කරන්න.
    	]],
    	["sk"] = [[
Pridá konštantný posun ku všetkým prichádzajúcim hodnotám údajových bodov.

Konfigurácia:
- **Posun**: Číslo, ktoré sa pripočíta ku všetkým hodnotám (predvolené: 0.0). Na odčítanie použite záporné hodnoty.
    	]],
    	["sl"] = [[
Vsem vhodnim vrednostim podatkovnih točk doda stalni zamik.

Konfiguracija:
- **Zamik**: Število, ki se doda vsem vrednostim (privzeto: 0.0). Za odštevanje uporabite negativne vrednosti.
    	]],
    	["es"] = [[
Suma un desplazamiento constante a todos los valores de los puntos de datos entrantes.

Configuración:
- **Desplazamiento**: El número que se sumará a todos los valores (predeterminado: 0.0). Usa valores negativos para restar.
    	]],
    	["sw"] = [[
Huongeza offset isiyobadilika kwenye thamani zote za nukta za data zinazoingia.

Usanidi:
- **Offset**: Nambari ya kuongeza kwenye thamani zote (chaguo-msingi: 0.0). Tumia thamani hasi kutoa.
    	]],
    	["sv"] = [[
Lägger till en konstant förskjutning till alla inkommande datapunkters värden.

Konfiguration:
- **Förskjutning**: Talet som ska läggas till alla värden (standard: 0.0). Använd negativa värden för att subtrahera.
    	]],
    	["ta"] = [[
உள்வரும் அனைத்து தரவுப் புள்ளி மதிப்புகளுக்கும் நிலையான ஆஃப்செட்டைச் சேர்க்கிறது.

உள்ளமைவு:
- **ஆஃப்செட்**: அனைத்து மதிப்புகளுக்கும் சேர்க்க வேண்டிய எண் (இயல்புநிலை: 0.0). கழிக்க எதிர்மறை மதிப்புகளைப் பயன்படுத்தவும்.
    	]],
    	["te"] = [[
అన్ని ఇన్‌కమింగ్ డేటా పాయింట్ విలువలకు స్థిర ఆఫ్‌సెట్‌ను జోడిస్తుంది.

కాన్ఫిగరేషన్:
- **ఆఫ్‌సెట్**: అన్ని విలువలకు జోడించాల్సిన సంఖ్య (డిఫాల్ట్: 0.0). తీసివేయడానికి ప్రతికూల విలువలను ఉపయోగించండి.
    	]],
    	["th"] = [[
บวกออฟเซ็ตคงที่ให้กับค่าของจุดข้อมูลขาเข้าทั้งหมด

การกำหนดค่า:
- **ออฟเซ็ต**: ตัวเลขที่ใช้บวกให้กับค่าทั้งหมด (ค่าเริ่มต้น: 0.0) ใช้ค่าลบเพื่อทำการลบ
    	]],
    	["tr"] = [[
Gelen tüm veri noktası değerlerine sabit bir kaydırma ekler.

Yapılandırma:
- **Kaydırma**: Tüm değerlere eklenecek sayı (varsayılan: 0.0). Çıkarmak için negatif değerler kullanın.
    	]],
    	["uk"] = [[
Додає постійне зміщення до всіх вхідних значень точок даних.

Конфігурація:
- **Зміщення**: Число, яке потрібно додати до всіх значень (типово: 0.0). Використовуйте від’ємні значення для віднімання.
    	]],
    	["vi"] = [[
Cộng một độ lệch không đổi vào tất cả giá trị điểm dữ liệu đầu vào.

Cấu hình:
- **Độ lệch**: Số cộng vào tất cả giá trị (mặc định: 0.0). Dùng giá trị âm để trừ.
    	]],
    },
    config = {
        number {
            id = "offset",
            default = 0.0,
            name = {
            	["en"] = "Offset",
            	["af"] = "Verstelling",
            	["sq"] = "Zhvendosja",
            	["am"] = "ማካካሻ",
            	["hy"] = "Շեղում",
            	["az"] = "Ofset",
            	["bn"] = "অফসেট",
            	["eu"] = "Desplazamendua",
            	["be"] = "Зрух",
            	["bg"] = "Отместване",
            	["my"] = "အော့ဖ်ဆက်",
            	["ca"] = "Desplaçament",
            	["zh-Hans"] = "偏移量",
            	["zh-Hant"] = "偏移量",
            	["hr"] = "Pomak",
            	["cs"] = "Posun",
            	["da"] = "Forskydning",
            	["nl"] = "Verschuiving",
            	["et"] = "Nihe",
            	["fil"] = "Offset",
            	["fi"] = "Siirtymä",
            	["fr"] = "Décalage",
            	["gl"] = "Desprazamento",
            	["ka"] = "წანაცვლება",
            	["de"] = "Offset",
            	["el"] = "Μετατόπιση",
            	["gu"] = "ઑફસેટ",
            	["hi"] = "ऑफ़सेट",
            	["hu"] = "Eltolás",
            	["is"] = "Hliðrun",
            	["id"] = "Offset",
            	["it"] = "Offset",
            	["ja"] = "オフセット",
            	["kn"] = "ಆಫ್‌ಸೆಟ್",
            	["kk"] = "Ығысу",
            	["km"] = "អុហ្វសិត",
            	["ko"] = "오프셋",
            	["ky"] = "Жылыш",
            	["lo"] = "ຄ່າຊົດເຊີຍ",
            	["lv"] = "Nobīde",
            	["lt"] = "Poslinkis",
            	["mk"] = "Поместување",
            	["ms"] = "Ofset",
            	["ml"] = "ഓഫ്സെറ്റ്",
            	["mr"] = "ऑफसेट",
            	["mn"] = "Зөрүү",
            	["ne"] = "अफसेट",
            	["no"] = "Forskyvning",
            	["pl"] = "Przesunięcie",
            	["pt"] = "Deslocamento",
            	["pa"] = "ਆਫਸੈੱਟ",
            	["ro"] = "Decalaj",
            	["rm"] = "Offset",
            	["ru"] = "Смещение",
            	["sr"] = "Pomeraj",
            	["si"] = "විස්ථාපනය",
            	["sk"] = "Posun",
            	["sl"] = "Zamik",
            	["es"] = "Desplazamiento",
            	["sw"] = "Offset",
            	["sv"] = "Förskjutning",
            	["ta"] = "ஆஃப்செட்",
            	["te"] = "ఆఫ్‌సెట్",
            	["th"] = "ออฟเซ็ต",
            	["tr"] = "Kaydırma",
            	["uk"] = "Зміщення",
            	["vi"] = "Độ lệch",
            }
        }
    },

    -- Generator function
    generator = function(source, config)
        local offset = config and config.offset or 0.0

        return function()
            local data_point = source.dp()
            if not data_point then return nil end

            data_point.value = data_point.value + offset

            return data_point
        end
    end
}
