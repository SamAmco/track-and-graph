-- Lua Function to filter out consecutive duplicates based on selected fields
-- Only passes through data points when the selected fields change from the previous one

local enum = require("tng.config").enum

return {
	-- Configuration metadata
	id = "distinct-until-changed",
	version = "1.0.1",
	inputCount = 1,
	categories = { "_filter" },
	title = {
		["en"] = "Distinct Until Changed",
		["af"] = "Uniek Totdat Dit Verander",
		["sq"] = "Të ndryshme derisa të ndryshojnë",
		["am"] = "እስኪቀየር ድረስ የተለዩ",
		["hy"] = "Կրկնությունները մինչև փոփոխությունը",
		["az"] = "Dəyişənədək fərqli",
		["bn"] = "পরিবর্তন না হওয়া পর্যন্ত স্বতন্ত্র",
		["eu"] = "Desberdina aldatu arte",
		["be"] = "Унікальныя да змены",
		["bg"] = "Различни до промяна",
		["my"] = "ပြောင်းလဲသည်အထိ ထပ်တူမဟုတ်သော",
		["ca"] = "Diferents fins que canviïn",
		["zh-Hans"] = "变化前去重",
		["zh-Hant"] = "變更前保持唯一",
		["hr"] = "Različito do promjene",
		["cs"] = "Odlišné do změny",
		["da"] = "Unikke indtil ændring",
		["nl"] = "Uniek totdat gewijzigd",
		["et"] = "Kuni muutumiseni erinevad",
		["fil"] = "Natatangi Hanggang Magbago",
		["fi"] = "Erota muutokseen asti",
		["fr"] = "Distinct jusqu’à modification",
		["gl"] = "Distintos ata cambiar",
		["ka"] = "განსხვავებული ცვლილებამდე",
		["de"] = "Eindeutig bis zur Änderung",
		["el"] = "Διακριτές τιμές μέχρι την αλλαγή",
		["gu"] = "બદલાય ત્યાં સુધી અનન્ય",
		["hi"] = "बदलने तक अलग",
		["hu"] = "Különböző érték változásig",
		["is"] = "Einstakt þar til breyting verður",
		["id"] = "Berbeda hingga Berubah",
		["it"] = "Distinti fino al cambiamento",
		["ja"] = "変更されるまで重複除外",
		["kn"] = "ಬದಲಾಗುವವರೆಗೆ ವಿಭಿನ್ನ",
		["kk"] = "Өзгергенге дейінгі бірегей мәндер",
		["km"] = "ខុសគ្នារហូតដល់មានការផ្លាស់ប្តូរ",
		["ko"] = "변경될 때까지 중복 제거",
		["ky"] = "Өзгөргөнгө чейин кайталанбаган",
		["lo"] = "ຄ່າບໍ່ຊ້ຳຈົນກວ່າຈະປ່ຽນ",
		["lv"] = "Atšķirīgās vērtības līdz izmaiņai",
		["lt"] = "Skirtingi, kol pasikeičia",
		["mk"] = "Различни до промена",
		["ms"] = "Berbeza Sehingga Berubah",
		["ml"] = "മാറുന്നതുവരെ വ്യത്യസ്തം",
		["mr"] = "बदल होईपर्यंत वेगळे",
		["mn"] = "Өөрчлөгдөх хүртэл давхардлыг хасах",
		["ne"] = "परिवर्तन नहुँदासम्म फरक",
		["no"] = "Unike til endring",
		["pl"] = "Unikalne do zmiany",
		["pt"] = "Distintos até alteração",
		["pa"] = "ਬਦਲਣ ਤੱਕ ਵਿਲੱਖਣ",
		["ro"] = "Distinct până la schimbare",
		["rm"] = "Distinct fin al midament",
		["ru"] = "Уникальные до изменения",
		["sr"] = "Različito dok se ne promeni",
		["si"] = "වෙනස් වන තෙක් අනන්‍ය",
		["sk"] = "Jedinečné do zmeny",
		["sl"] = "Razločno do spremembe",
		["es"] = "Distintos hasta que cambien",
		["sw"] = "Tofauti Hadi Ibague",
		["sv"] = "Unika tills ändring",
		["ta"] = "மாறும் வரை தனித்துவமாக்கு",
		["te"] = "మారే వరకు ప్రత్యేకమైనవి",
		["th"] = "ค่าที่แตกต่างจนกว่าจะเปลี่ยน",
		["tr"] = "Değişene Kadar Benzersiz",
		["uk"] = "Унікальні до зміни",
		["vi"] = "Lọc giá trị khác cho đến khi thay đổi",
	},
	description = {
		["en"] = [[
Filters out consecutive duplicates based on the selected fields. Only data points where the selected fields differ from the previous one will pass through.

- **All Fields** - Compare value, label, and note
- **Value Only** - Compare value only
- **Label Only** - Compare label only
- **Note Only** - Compare note only
- **Value and Label** - Compare value and label
- **Value and Note** - Compare value and note
- **Label and Note** - Compare label and note
		]],
		["af"] = [[
Filtreer opeenvolgende duplikate uit op grond van die gekose velde. Slegs datapunte waarvan die gekose velde van die vorige een verskil, word deurgegee.

- **Alle velde** - Vergelyk waarde, etiket en nota
- **Slegs waarde** - Vergelyk slegs waarde
- **Slegs etiket** - Vergelyk slegs etiket
- **Slegs nota** - Vergelyk slegs nota
- **Waarde en etiket** - Vergelyk waarde en etiket
- **Waarde en nota** - Vergelyk waarde en nota
- **Etiket en nota** - Vergelyk etiket en nota
		]],
		["sq"] = [[
Filtron dublikatat e njëpasnjëshme bazuar në fushat e zgjedhura. Vetëm pikat e të dhënave ku fushat e zgjedhura ndryshojnë nga pika e mëparshme do të kalojnë.

- **Të gjitha fushat** - Krahason vlerën, etiketën dhe shënimin
- **Vetëm vlera** - Krahason vetëm vlerën
- **Vetëm etiketa** - Krahason vetëm etiketën
- **Vetëm shënimi** - Krahason vetëm shënimin
- **Vlera dhe etiketa** - Krahason vlerën dhe etiketën
- **Vlera dhe shënimi** - Krahason vlerën dhe shënimin
- **Etiketa dhe shënimi** - Krahason etiketën dhe shënimin
		]],
		["am"] = [[
በተመረጡት መስኮች መሠረት ተከታታይ ድግግሞሾችን ያስወግዳል። ከቀዳሚው የተለዩ የተመረጡ መስኮች ያሏቸው የውሂብ ነጥቦች ብቻ ያልፋሉ።

- **ሁሉም መስኮች** - ዋጋ፣ መለያ እና ማስታወሻ ያወዳድሩ
- **ዋጋ ብቻ** - ዋጋን ብቻ ያወዳድሩ
- **መለያ ብቻ** - መለያን ብቻ ያወዳድሩ
- **ማስታወሻ ብቻ** - ማስታወሻን ብቻ ያወዳድሩ
- **ዋጋ እና መለያ** - ዋጋን እና መለያን ያወዳድሩ
- **ዋጋ እና ማስታወሻ** - ዋጋን እና ማስታወሻን ያወዳድሩ
- **መለያ እና ማስታወሻ** - መለያን እና ማስታወሻን ያወዳድሩ
		]],
		["hy"] = [[
Հեռացնում է ընտրված դաշտերի հիման վրա հաջորդական կրկնությունները։ Անցնում են միայն այն տվյալակետերը, որոնց ընտրված դաշտերը տարբերվում են նախորդից։

- **Բոլոր դաշտերը** - համեմատել արժեքը, պիտակը և նշումը
- **Միայն արժեքը** - համեմատել միայն արժեքը
- **Միայն պիտակը** - համեմատել միայն պիտակը
- **Միայն նշումը** - համեմատել միայն նշումը
- **Արժեքը և պիտակը** - համեմատել արժեքը և պիտակը
- **Արժեքը և նշումը** - համեմատել արժեքը և նշումը
- **Պիտակը և նշումը** - համեմատել պիտակը և նշումը
		]],
		["az"] = [[
Seçilmiş sahələrə əsasən ardıcıl təkrarları süzgəcdən keçirir. Yalnız seçilmiş sahələri əvvəlkindən fərqlənən məlumat nöqtələri keçir.

- **Bütün sahələr** - Qiyməti, etiketi və qeydi müqayisə et
- **Yalnız qiymət** - Yalnız qiyməti müqayisə et
- **Yalnız etiket** - Yalnız etiketi müqayisə et
- **Yalnız qeyd** - Yalnız qeydi müqayisə et
- **Qiymət və etiket** - Qiyməti və etiketi müqayisə et
- **Qiymət və qeyd** - Qiyməti və qeydi müqayisə et
- **Etiket və qeyd** - Etiketi və qeydi müqayisə et
		]],
		["bn"] = [[
নির্বাচিত ক্ষেত্রের ভিত্তিতে পরপর একই ডেটা পয়েন্টগুলো বাদ দেয়। নির্বাচিত ক্ষেত্রগুলো আগেরটির থেকে ভিন্ন হলে কেবল সেই ডেটা পয়েন্টগুলোই পাস করবে।

- **সব ক্ষেত্র** - মান, লেবেল ও নোট তুলনা করুন
- **শুধু মান** - শুধু মান তুলনা করুন
- **শুধু লেবেল** - শুধু লেবেল তুলনা করুন
- **শুধু নোট** - শুধু নোট তুলনা করুন
- **মান ও লেবেল** - মান ও লেবেল তুলনা করুন
- **মান ও নোট** - মান ও নোট তুলনা করুন
- **লেবেল ও নোট** - লেবেল ও নোট তুলনা করুন
		]],
		["eu"] = [[
Hautatutako eremuetan oinarritutako ondoz ondoko bikoiztuak iragazten ditu. Hautatutako eremuak aurrekoarekiko desberdinak diren datu-puntuak soilik igaroko dira.

- **Eremu guztiak** - Konparatu balioa, etiketa eta oharra
- **Balioa soilik** - Konparatu balioa soilik
- **Etiketa soilik** - Konparatu etiketa soilik
- **Oharra soilik** - Konparatu oharra soilik
- **Balioa eta etiketa** - Konparatu balioa eta etiketa
- **Balioa eta oharra** - Konparatu balioa eta oharra
- **Etiketa eta oharra** - Konparatu etiketa eta oharra
		]],
		["be"] = [[
Фільтруе паслядоўныя дублікаты на аснове выбраных палёў. Прапускаюцца толькі кропкі даных, у якіх выбраныя палі адрозніваюцца ад папярэдняй кропкі.

- **Усе палі** — параўноўваць значэнне, ярлык і заўвагу
- **Толькі значэнне** — параўноўваць толькі значэнне
- **Толькі ярлык** — параўноўваць толькі ярлык
- **Толькі заўвага** — параўноўваць толькі заўвагу
- **Значэнне і ярлык** — параўноўваць значэнне і ярлык
- **Значэнне і заўвага** — параўноўваць значэнне і заўвагу
- **Ярлык і заўвага** — параўноўваць ярлык і заўвагу
		]],
		["bg"] = [[
Филтрира последователните дубликати въз основа на избраните полета. Преминават само точките от данни, при които избраните полета се различават от предишната точка.

- **Всички полета** - Сравнява стойност, етикет и бележка
- **Само стойност** - Сравнява само стойността
- **Само етикет** - Сравнява само етикета
- **Само бележка** - Сравнява само бележката
- **Стойност и етикет** - Сравнява стойността и етикета
- **Стойност и бележка** - Сравнява стойността и бележката
- **Етикет и бележка** - Сравнява етикета и бележката
		]],
		["my"] = [[
ရွေးချယ်ထားသော အကွက်များအပေါ် အခြေခံ၍ ဆက်တိုက်ထပ်နေသော တန်ဖိုးများကို ဖယ်ရှားသည်။ ရွေးချယ်ထားသော အကွက်များသည် ယခင်အကွက်များနှင့် ကွဲပြားသော ဒေတာမှတ်များသာ ဖြတ်သန်းမည်။

- **အကွက်အားလုံး** - တန်ဖိုး၊ အညွှန်းနှင့် မှတ်စုကို နှိုင်းယှဉ်သည်
- **တန်ဖိုးသာ** - တန်ဖိုးကိုသာ နှိုင်းယှဉ်သည်
- **အညွှန်းသာ** - အညွှန်းကိုသာ နှိုင်းယှဉ်သည်
- **မှတ်စုသာ** - မှတ်စုကိုသာ နှိုင်းယှဉ်သည်
- **တန်ဖိုးနှင့် အညွှန်း** - တန်ဖိုးနှင့် အညွှန်းကို နှိုင်းယှဉ်သည်
- **တန်ဖိုးနှင့် မှတ်စု** - တန်ဖိုးနှင့် မှတ်စုကို နှိုင်းယှဉ်သည်
- **အညွှန်းနှင့် မှတ်စု** - အညွှန်းနှင့် မှတ်စုကို နှိုင်းယှဉ်သည်
		]],
		["ca"] = [[
Filtra els duplicats consecutius segons els camps seleccionats. Només passen els punts de dades en què els camps seleccionats difereixen dels anteriors.

- **Tots els camps** - Compara el valor, l’etiqueta i la nota
- **Només el valor** - Compara només el valor
- **Només l’etiqueta** - Compara només l’etiqueta
- **Només la nota** - Compara només la nota
- **Valor i etiqueta** - Compara el valor i l’etiqueta
- **Valor i nota** - Compara el valor i la nota
- **Etiqueta i nota** - Compara l’etiqueta i la nota
		]],
		["zh-Hans"] = [[
根据选定字段过滤连续重复项。只有选定字段与前一个数据点不同的数据点才会通过。

- **所有字段** - 比较值、标签和备注
- **仅值** - 仅比较值
- **仅标签** - 仅比较标签
- **仅备注** - 仅比较备注
- **值和标签** - 比较值和标签
- **值和备注** - 比较值和备注
- **标签和备注** - 比较标签和备注
		]],
		["zh-Hant"] = [[
根據所選欄位篩除連續重複項目。只有所選欄位與前一個資料點不同的資料點才會通過。

- **所有欄位** - 比較值、標籤和備註
- **僅值** - 僅比較值
- **僅標籤** - 僅比較標籤
- **僅備註** - 僅比較備註
- **值和標籤** - 比較值和標籤
- **值和備註** - 比較值和備註
- **標籤和備註** - 比較標籤和備註
		]],
		["hr"] = [[
Filtrira uzastopne duplikate na temelju odabranih polja. Prolaze samo podatkovne točke kod kojih se odabrana polja razlikuju od prethodne točke.

- **Sva polja** - Usporedi vrijednost, oznaku i bilješku
- **Samo vrijednost** - Usporedi samo vrijednost
- **Samo oznaku** - Usporedi samo oznaku
- **Samo bilješku** - Usporedi samo bilješku
- **Vrijednost i oznaku** - Usporedi vrijednost i oznaku
- **Vrijednost i bilješku** - Usporedi vrijednost i bilješku
- **Oznaku i bilješku** - Usporedi oznaku i bilješku
		]],
		["cs"] = [[
Odfiltruje po sobě jdoucí duplicity podle vybraných polí. Projdou pouze datové body, u nichž se vybraná pole liší od předchozího bodu.

- **Všechna pole** - Porovnat hodnotu, štítek a poznámku
- **Pouze hodnota** - Porovnat pouze hodnotu
- **Pouze štítek** - Porovnat pouze štítek
- **Pouze poznámka** - Porovnat pouze poznámku
- **Hodnota a štítek** - Porovnat hodnotu a štítek
- **Hodnota a poznámka** - Porovnat hodnotu a poznámku
- **Štítek a poznámka** - Porovnat štítek a poznámku
		]],
		["da"] = [[
Filtrerer fortløbende dubletter baseret på de valgte felter. Kun datapunkter, hvor de valgte felter adskiller sig fra det forrige, går videre.

- **Alle felter** - Sammenlign værdi, etiket og note
- **Kun værdi** - Sammenlign kun værdi
- **Kun etiket** - Sammenlign kun etiket
- **Kun note** - Sammenlign kun note
- **Værdi og etiket** - Sammenlign værdi og etiket
- **Værdi og note** - Sammenlign værdi og note
- **Etiket og note** - Sammenlign etiket og note
		]],
		["nl"] = [[
Filtert opeenvolgende duplicaten op basis van de geselecteerde velden. Alleen gegevenspunten waarbij de geselecteerde velden verschillen van het vorige punt worden doorgelaten.

- **Alle velden** - Waarde, label en notitie vergelijken
- **Alleen waarde** - Alleen waarde vergelijken
- **Alleen label** - Alleen label vergelijken
- **Alleen notitie** - Alleen notitie vergelijken
- **Waarde en label** - Waarde en label vergelijken
- **Waarde en notitie** - Waarde en notitie vergelijken
- **Label en notitie** - Label en notitie vergelijken
		]],
		["et"] = [[
Filtreerib valitud väljade alusel järjestikused duplikaadid välja. Läbivad ainult need andmepunktid, mille valitud väljad erinevad eelmisest.

- **Kõik väljad** – võrdle väärtust, silti ja märkust
- **Ainult väärtus** – võrdle ainult väärtust
- **Ainult silt** – võrdle ainult silti
- **Ainult märkus** – võrdle ainult märkust
- **Väärtus ja silt** – võrdle väärtust ja silti
- **Väärtus ja märkus** – võrdle väärtust ja märkust
- **Silt ja märkus** – võrdle silti ja märkust
		]],
		["fil"] = [[
Sinasala ang magkakasunod na duplicate batay sa mga napiling field. Tanging mga data point na naiiba ang napiling field mula sa nauna ang magpapatuloy.

- **All Fields** - Ihambing ang value, label, at note
- **Value Only** - Ihambing ang value lamang
- **Label Only** - Ihambing ang label lamang
- **Note Only** - Ihambing ang note lamang
- **Value and Label** - Ihambing ang value at label
- **Value and Note** - Ihambing ang value at note
- **Label and Note** - Ihambing ang label at note
		]],
		["fi"] = [[
Suodattaa peräkkäiset kaksoiskappaleet valittujen kenttien perusteella. Vain datapisteet, joiden valitut kentät poikkeavat edellisestä, päästetään läpi.

- **Kaikki kentät** - Vertaa arvoa, selitettä ja muistiinpanoa
- **Vain arvo** - Vertaa vain arvoa
- **Vain selite** - Vertaa vain selitettä
- **Vain muistiinpano** - Vertaa vain muistiinpanoa
- **Arvo ja selite** - Vertaa arvoa ja selitettä
- **Arvo ja muistiinpano** - Vertaa arvoa ja muistiinpanoa
- **Selite ja muistiinpano** - Vertaa selitettä ja muistiinpanoa
		]],
		["fr"] = [[
Filtre les doublons consécutifs selon les champs sélectionnés. Seuls les points de données dont les champs sélectionnés diffèrent du point précédent sont conservés.

- **Tous les champs** - Compare la valeur, le libellé et la note
- **Valeur uniquement** - Compare uniquement la valeur
- **Libellé uniquement** - Compare uniquement le libellé
- **Note uniquement** - Compare uniquement la note
- **Valeur et libellé** - Compare la valeur et le libellé
- **Valeur et note** - Compare la valeur et la note
- **Libellé et note** - Compare le libellé et la note
		]],
		["gl"] = [[
Filtra os duplicados consecutivos en función dos campos seleccionados. Só pasan os puntos de datos nos que os campos seleccionados difiren do anterior.

- **Todos os campos** - Compara o valor, a etiqueta e a nota
- **Só o valor** - Compara só o valor
- **Só a etiqueta** - Compara só a etiqueta
- **Só a nota** - Compara só a nota
- **Valor e etiqueta** - Compara o valor e a etiqueta
- **Valor e nota** - Compara o valor e a nota
- **Etiqueta e nota** - Compara a etiqueta e a nota
		]],
		["ka"] = [[
ფილტრავს ზედიზედ განმეორებულ მნიშვნელობებს არჩეული ველების მიხედვით. გაივლის მხოლოდ ის მონაცემთა წერტილები, რომელთა არჩეული ველები წინასგან განსხვავდება.

- **ყველა ველი** - მნიშვნელობის, იარლიყისა და შენიშვნის შედარება
- **მხოლოდ მნიშვნელობა** - მხოლოდ მნიშვნელობის შედარება
- **მხოლოდ იარლიყი** - მხოლოდ იარლიყის შედარება
- **მხოლოდ შენიშვნა** - მხოლოდ შენიშვნის შედარება
- **მნიშვნელობა და იარლიყი** - მნიშვნელობისა და იარლიყის შედარება
- **მნიშვნელობა და შენიშვნა** - მნიშვნელობისა და შენიშვნის შედარება
- **იარლიყი და შენიშვნა** - იარლიყისა და შენიშვნის შედარება
		]],
		["de"] = [[
Filtert aufeinanderfolgende Duplikate anhand der ausgewählten Felder heraus. Nur Datenpunkte, bei denen sich die ausgewählten Felder vom vorherigen unterscheiden, werden weitergegeben.

- **Alle Felder** – Wert, Label und Notiz vergleichen
- **Nur Wert** – Nur den Wert vergleichen
- **Nur Label** – Nur das Label vergleichen
- **Nur Notiz** – Nur die Notiz vergleichen
- **Wert und Label** – Wert und Label vergleichen
- **Wert und Notiz** – Wert und Notiz vergleichen
- **Label und Notiz** – Label und Notiz vergleichen
		]],
		["el"] = [[
Φιλτράρει τα διαδοχικά διπλότυπα με βάση τα επιλεγμένα πεδία. Περνούν μόνο τα σημεία δεδομένων στα οποία τα επιλεγμένα πεδία διαφέρουν από το προηγούμενο.

- **Όλα τα πεδία** - Σύγκριση τιμής, ετικέτας και σημείωσης
- **Μόνο τιμή** - Σύγκριση μόνο της τιμής
- **Μόνο ετικέτα** - Σύγκριση μόνο της ετικέτας
- **Μόνο σημείωση** - Σύγκριση μόνο της σημείωσης
- **Τιμή και ετικέτα** - Σύγκριση τιμής και ετικέτας
- **Τιμή και σημείωση** - Σύγκριση τιμής και σημείωσης
- **Ετικέτα και σημείωση** - Σύγκριση ετικέτας και σημείωσης
		]],
		["gu"] = [[
પસંદ કરેલા ફીલ્ડ્સના આધારે સતત ડુપ્લિકેટ્સને દૂર કરે છે. પસંદ કરેલા ફીલ્ડ્સ અગાઉના ફીલ્ડ્સથી અલગ હોય તેવા ડેટા પોઇન્ટ્સ જ પસાર થશે.

- **બધા ફીલ્ડ્સ** - મૂલ્ય, લેબલ અને નોંધની તુલના કરો
- **માત્ર મૂલ્ય** - માત્ર મૂલ્યની તુલના કરો
- **માત્ર લેબલ** - માત્ર લેબલની તુલના કરો
- **માત્ર નોંધ** - માત્ર નોંધની તુલના કરો
- **મૂલ્ય અને લેબલ** - મૂલ્ય અને લેબલની તુલના કરો
- **મૂલ્ય અને નોંધ** - મૂલ્ય અને નોંધની તુલના કરો
- **લેબલ અને નોંધ** - લેબલ અને નોંધની તુલના કરો
		]],
		["hi"] = [[
चयनित फ़ील्ड के आधार पर लगातार डुप्लिकेट हटाता है। केवल वे डेटा पॉइंट आगे जाते हैं जिनके चयनित फ़ील्ड पिछले डेटा पॉइंट से अलग हों।

- **सभी फ़ील्ड** - मान, लेबल और नोट की तुलना करें
- **केवल मान** - केवल मान की तुलना करें
- **केवल लेबल** - केवल लेबल की तुलना करें
- **केवल नोट** - केवल नोट की तुलना करें
- **मान और लेबल** - मान और लेबल की तुलना करें
- **मान और नोट** - मान और नोट की तुलना करें
- **लेबल और नोट** - लेबल और नोट की तुलना करें
		]],
		["hu"] = [[
Kiszűri az egymást követő duplikátumokat a kiválasztott mezők alapján. Csak azok az adatpontok haladnak tovább, amelyeknél a kiválasztott mezők eltérnek az előző adatpontétól.

- **Minden mező** - Érték, címke és megjegyzés összehasonlítása
- **Csak érték** - Csak az érték összehasonlítása
- **Csak címke** - Csak a címke összehasonlítása
- **Csak megjegyzés** - Csak a megjegyzés összehasonlítása
- **Érték és címke** - Az érték és a címke összehasonlítása
- **Érték és megjegyzés** - Az érték és a megjegyzés összehasonlítása
- **Címke és megjegyzés** - A címke és a megjegyzés összehasonlítása
		]],
		["is"] = [[
Síar út samfelldar endurtekningar samkvæmt völdum reitum. Aðeins gagnapunktar þar sem valdir reitir eru frábrugðnir fyrri gagnapunkti fara áfram.

- **Allir reitir** - Ber saman gildi, merki og athugasemd
- **Aðeins gildi** - Ber aðeins saman gildi
- **Aðeins merki** - Ber aðeins saman merki
- **Aðeins athugasemd** - Ber aðeins saman athugasemd
- **Gildi og merki** - Ber saman gildi og merki
- **Gildi og athugasemd** - Ber saman gildi og athugasemd
- **Merki og athugasemd** - Ber saman merki og athugasemd
		]],
		["id"] = [[
Menyaring duplikat berurutan berdasarkan kolom yang dipilih. Hanya titik data yang kolom pilihannya berbeda dari titik sebelumnya yang akan diteruskan.

- **Semua Kolom** - Membandingkan nilai, label, dan catatan
- **Nilai Saja** - Hanya membandingkan nilai
- **Label Saja** - Hanya membandingkan label
- **Catatan Saja** - Hanya membandingkan catatan
- **Nilai dan Label** - Membandingkan nilai dan label
- **Nilai dan Catatan** - Membandingkan nilai dan catatan
- **Label dan Catatan** - Membandingkan label dan catatan
		]],
		["it"] = [[
Filtra i duplicati consecutivi in base ai campi selezionati. Passano solo i punti dati i cui campi selezionati differiscono da quelli del precedente.

- **Tutti i campi** - Confronta valore, etichetta e nota
- **Solo valore** - Confronta solo il valore
- **Solo etichetta** - Confronta solo l'etichetta
- **Solo nota** - Confronta solo la nota
- **Valore ed etichetta** - Confronta valore ed etichetta
- **Valore e nota** - Confronta valore e nota
- **Etichetta e nota** - Confronta etichetta e nota
		]],
		["ja"] = [[
選択したフィールドに基づいて連続する重複を除外します。選択したフィールドが直前のデータポイントと異なるデータポイントだけが通過します。

- **すべてのフィールド** - 値、ラベル、メモを比較
- **値のみ** - 値のみを比較
- **ラベルのみ** - ラベルのみを比較
- **メモのみ** - メモのみを比較
- **値とラベル** - 値とラベルを比較
- **値とメモ** - 値とメモを比較
- **ラベルとメモ** - ラベルとメモを比較
		]],
		["kn"] = [[
ಆಯ್ಕೆಮಾಡಿದ ಕ್ಷೇತ್ರಗಳ ಆಧಾರದ ಮೇಲೆ ಸತತ ನಕಲುಗಳನ್ನು ಫಿಲ್ಟರ್ ಮಾಡುತ್ತದೆ. ಹಿಂದಿನದಕ್ಕಿಂತ ಆಯ್ಕೆಮಾಡಿದ ಕ್ಷೇತ್ರಗಳು ಭಿನ್ನವಾಗಿರುವ ಡೇಟಾ ಬಿಂದುಗಳು ಮಾತ್ರ ಮುಂದುವರಿಯುತ್ತವೆ.

- **ಎಲ್ಲಾ ಕ್ಷೇತ್ರಗಳು** - ಮೌಲ್ಯ, ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿಯನ್ನು ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮಾತ್ರ** - ಮೌಲ್ಯವನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಲೇಬಲ್ ಮಾತ್ರ** - ಲೇಬಲ್ ಅನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಟಿಪ್ಪಣಿ ಮಾತ್ರ** - ಟಿಪ್ಪಣಿಯನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮತ್ತು ಲೇಬಲ್** - ಮೌಲ್ಯ ಮತ್ತು ಲೇಬಲ್ ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮತ್ತು ಟಿಪ್ಪಣಿ** - ಮೌಲ್ಯ ಮತ್ತು ಟಿಪ್ಪಣಿ ಹೋಲಿಸಿ
- **ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿ** - ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿ ಹೋಲಿಸಿ
		]],
		["kk"] = [[
Таңдалған өрістерге негізделген қатарынан қайталанатын мәндерді сүзгіден өткізбейді. Тек таңдалған өрістері алдыңғы нүктеден өзгеше дерек нүктелері өтеді.

- **Барлық өрістер** — мәнді, жапсырманы және ескертпені салыстыру
- **Тек мән** — тек мәнді салыстыру
- **Тек жапсырма** — тек жапсырманы салыстыру
- **Тек ескертпе** — тек ескертпені салыстыру
- **Мән және жапсырма** — мән мен жапсырманы салыстыру
- **Мән және ескертпе** — мән мен ескертпені салыстыру
- **Жапсырма және ескертпе** — жапсырма мен ескертпені салыстыру
		]],
		["km"] = [[
ត្រងចំណុចទិន្នន័យស្ទួនជាប់គ្នា ដោយផ្អែកលើវាលដែលបានជ្រើស។ មានតែចំណុចទិន្នន័យដែលវាលដែលបានជ្រើសខុសពីចំណុចមុនប៉ុណ្ណោះ នឹងត្រូវបានបញ្ជូនបន្ត។

- **គ្រប់វាល** - ប្រៀបធៀបតម្លៃ ស្លាក និងចំណាំ
- **តម្លៃប៉ុណ្ណោះ** - ប្រៀបធៀបតម្លៃប៉ុណ្ណោះ
- **ស្លាកប៉ុណ្ណោះ** - ប្រៀបធៀបស្លាកប៉ុណ្ណោះ
- **ចំណាំប៉ុណ្ណោះ** - ប្រៀបធៀបចំណាំប៉ុណ្ណោះ
- **តម្លៃ និងស្លាក** - ប្រៀបធៀបតម្លៃ និងស្លាក
- **តម្លៃ និងចំណាំ** - ប្រៀបធៀបតម្លៃ និងចំណាំ
- **ស្លាក និងចំណាំ** - ប្រៀបធៀបស្លាក និងចំណាំ
		]],
		["ko"] = [[
선택한 필드를 기준으로 연속된 중복 항목을 필터링합니다. 선택한 필드가 이전 데이터 포인트와 다른 데이터 포인트만 통과합니다.

- **모든 필드** - 값, 라벨, 메모리 비교
- **값만** - 값만 비교
- **라벨만** - 라벨만 비교
- **메모만** - 메모만 비교
- **값 및 라벨** - 값과 라벨 비교
- **값 및 메모** - 값과 메모 비교
- **라벨 및 메모** - 라벨과 메모 비교
		]],
		["ky"] = [[
Тандалган талаалардын негизинде удаалаш кайталанууларды алып салат. Тандалган талаалары мурункусунан айырмаланган маалымат чекиттери гана өткөрүлөт.

- **Бардык талаалар** - Маанини, энбелгини жана эскертмени салыштыруу
- **Маани гана** - Маанини гана салыштыруу
- **Энбелги гана** - Энбелгини гана салыштыруу
- **Эскертме гана** - Эскертмени гана салыштыруу
- **Маани жана энбелги** - Маанини жана энбелгини салыштыруу
- **Маани жана эскертме** - Маанини жана эскертмени салыштыруу
- **Энбелги жана эскертме** - Энбелгини жана эскертмени салыштыруу
		]],
		["lo"] = [[
ກັ່ນຕອງຄ່າຊ້ຳທີ່ຕິດກັນໂດຍອີງຕາມຟິວທີ່ເລືອກ. ຈະຜ່ານສະເພາະຈຸດຂໍ້ມູນທີ່ຟິວທີ່ເລືອກແຕກຕ່າງຈາກຈຸດກ່ອນໜ້າ.

- **ຟິວທັງໝົດ** - ປຽບທຽບຄ່າ, ປ້າຍ ແລະ ໝາຍເຫດ
- **ຄ່າເທົ່ານັ້ນ** - ປຽບທຽບສະເພາະຄ່າ
- **ປ້າຍເທົ່ານັ້ນ** - ປຽບທຽບສະເພາະປ້າຍ
- **ໝາຍເຫດເທົ່ານັ້ນ** - ປຽບທຽບສະເພາະໝາຍເຫດ
- **ຄ່າ ແລະ ປ້າຍ** - ປຽບທຽບຄ່າ ແລະ ປ້າຍ
- **ຄ່າ ແລະ ໝາຍເຫດ** - ປຽບທຽບຄ່າ ແລະ ໝາຍເຫດ
- **ປ້າຍ ແລະ ໝາຍເຫດ** - ປຽບທຽບປ້າຍ ແລະ ໝາຍເຫດ
		]],
		["lv"] = [[
Filtrē secīgus dublikātus, pamatojoties uz atlasītajiem laukiem. Tiek iekļauti tikai tie datu punkti, kuru atlasītie lauki atšķiras no iepriekšējā punkta.

- **Visi lauki** - Salīdzināt vērtību, etiķeti un piezīmi
- **Tikai vērtība** - Salīdzināt tikai vērtību
- **Tikai etiķete** - Salīdzināt tikai etiķeti
- **Tikai piezīme** - Salīdzināt tikai piezīmi
- **Vērtība un etiķete** - Salīdzināt vērtību un etiķeti
- **Vērtība un piezīme** - Salīdzināt vērtību un piezīmi
- **Etiķete un piezīme** - Salīdzināt etiķeti un piezīmi
		]],
		["lt"] = [[
Išfiltruoja iš eilės einančius pasikartojimus pagal pasirinktus laukus. Toliau perduodami tik tie duomenų taškai, kurių pasirinkti laukai skiriasi nuo ankstesnio taško.

- **Visi laukai** – Lyginti reikšmę, etiketę ir pastabą
- **Tik reikšmė** – Lyginti tik reikšmę
- **Tik etiketė** – Lyginti tik etiketę
- **Tik pastaba** – Lyginti tik pastabą
- **Reikšmė ir etiketė** – Lyginti reikšmę ir etiketę
- **Reikšmė ir pastaba** – Lyginti reikšmę ir pastabą
- **Etiketė ir pastaba** – Lyginti etiketę ir pastabą
		]],
		["mk"] = [[
Ги филтрира последователните дупликати врз основа на избраните полиња. Само точките на податоци кај кои избраните полиња се разликуваат од претходната ќе поминат.

- **Сите полиња** - Спореди вредност, ознака и белешка
- **Само вредност** - Спореди само вредност
- **Само ознака** - Спореди само ознака
- **Само белешка** - Спореди само белешка
- **Вредност и ознака** - Спореди вредност и ознака
- **Вредност и белешка** - Спореди вредност и белешка
- **Ознака и белешка** - Спореди ознака и белешка
		]],
		["ms"] = [[
Menapis pendua berturutan berdasarkan medan yang dipilih. Hanya titik data yang medannya dipilih berbeza daripada titik data sebelumnya akan diteruskan.

- **Semua Medan** - Bandingkan nilai, label dan nota
- **Nilai Sahaja** - Bandingkan nilai sahaja
- **Label Sahaja** - Bandingkan label sahaja
- **Nota Sahaja** - Bandingkan nota sahaja
- **Nilai dan Label** - Bandingkan nilai dan label
- **Nilai dan Nota** - Bandingkan nilai dan nota
- **Label dan Nota** - Bandingkan label dan nota
		]],
		["ml"] = [[
തിരഞ്ഞെടുത്ത ഫീൽഡുകളെ അടിസ്ഥാനമാക്കിയുള്ള തുടർച്ചയായ ഡ്യൂപ്ലിക്കേറ്റുകൾ നീക്കം ചെയ്യുന്നു. തിരഞ്ഞെടുത്ത ഫീൽഡുകൾ മുമ്പത്തേതിൽ നിന്ന് വ്യത്യസ്തമായ ഡാറ്റാ പോയിന്റുകൾ മാത്രമേ കടന്നുപോകൂ.

- **All Fields** - value, label, note താരതമ്യം ചെയ്യുക
- **Value Only** - value മാത്രം താരതമ്യം ചെയ്യുക
- **Label Only** - label മാത്രം താരതമ്യം ചെയ്യുക
- **Note Only** - note മാത്രം താരതമ്യം ചെയ്യുക
- **Value and Label** - value, label എന്നിവ താരതമ്യം ചെയ്യുക
- **Value and Note** - value, note എന്നിവ താരതമ്യം ചെയ്യുക
- **Label and Note** - label, note എന്നിവ താരതമ്യം ചെയ്യുക
		]],
		["mr"] = [[
निवडलेल्या फील्डच्या आधारे सलग पुनरावृत्ती होणारे मूल्य गाळून टाकते. निवडलेली फील्ड मागील डेटा बिंदूपेक्षा वेगळी असलेले डेटा बिंदूच पुढे पाठवले जातात.

- **सर्व फील्ड** - मूल्य, लेबल आणि टिपणाची तुलना करा
- **फक्त मूल्य** - फक्त मूल्याची तुलना करा
- **फक्त लेबल** - फक्त लेबलची तुलना करा
- **फक्त टिपण** - फक्त टिपणाची तुलना करा
- **मूल्य आणि लेबल** - मूल्य आणि लेबलची तुलना करा
- **मूल्य आणि टिपण** - मूल्य आणि टिपणाची तुलना करा
- **लेबल आणि टिपण** - लेबल आणि टिपणाची तुलना करा
		]],
		["mn"] = [[
Сонгосон талбарууд дээр үндэслэн дараалсан давхардлыг шүүнэ. Сонгосон талбарууд өмнөхөөсөө ялгаатай өгөгдлийн цэгүүд л нэвтэрнэ.

- **Бүх талбар** - Утга, шошго болон тэмдэглэлийг харьцуулна
- **Зөвхөн утга** - Зөвхөн утгыг харьцуулна
- **Зөвхөн шошго** - Зөвхөн шошгыг харьцуулна
- **Зөвхөн тэмдэглэл** - Зөвхөн тэмдэглэлийг харьцуулна
- **Утга ба шошго** - Утга болон шошгыг харьцуулна
- **Утга ба тэмдэглэл** - Утга болон тэмдэглэлийг харьцуулна
- **Шошго ба тэмдэглэл** - Шошго болон тэмдэглэлийг харьцуулна
		]],
		["ne"] = [[
चयन गरिएका फिल्डका आधारमा लगातार दोहोरिएका मानहरू हटाउँछ। चयन गरिएका फिल्ड अघिल्लो फिल्डभन्दा फरक भएका डेटा बिन्दुहरू मात्र अघि पठाइन्छन्।

- **सबै फिल्डहरू** - मान, लेबल र नोट तुलना गर्नुहोस्
- **मान मात्र** - मान मात्र तुलना गर्नुहोस्
- **लेबल मात्र** - लेबल मात्र तुलना गर्नुहोस्
- **नोट मात्र** - नोट मात्र तुलना गर्नुहोस्
- **मान र लेबल** - मान र लेबल तुलना गर्नुहोस्
- **मान र नोट** - मान र नोट तुलना गर्नुहोस्
- **लेबल र नोट** - लेबल र नोट तुलना गर्नुहोस्
		]],
		["no"] = [[
Filtrerer bort påfølgende duplikater basert på de valgte feltene. Bare datapunkter der de valgte feltene skiller seg fra det forrige, slipper gjennom.

- **Alle felt** – Sammenlign verdi, etikett og notat
- **Bare verdi** – Sammenlign bare verdi
- **Bare etikett** – Sammenlign bare etikett
- **Bare notat** – Sammenlign bare notat
- **Verdi og etikett** – Sammenlign verdi og etikett
- **Verdi og notat** – Sammenlign verdi og notat
- **Etikett og notat** – Sammenlign etikett og notat
		]],
		["pl"] = [[
Odfiltrowuje kolejne duplikaty na podstawie wybranych pól. Przechodzą tylko punkty danych, w których wybrane pola różnią się od poprzedniego punktu.

- **Wszystkie pola** - Porównuj wartość, etykietę i notatkę
- **Tylko wartość** - Porównuj tylko wartość
- **Tylko etykietę** - Porównuj tylko etykietę
- **Tylko notatkę** - Porównuj tylko notatkę
- **Wartość i etykieta** - Porównuj wartość i etykietę
- **Wartość i notatka** - Porównuj wartość i notatkę
- **Etykieta i notatka** - Porównuj etykietę i notatkę
		]],
		["pt"] = [[
Filtra duplicados consecutivos com base nos campos selecionados. Apenas os pontos de dados em que os campos selecionados diferem do ponto anterior passam.

- **Todos os campos** - Comparar valor, rótulo e nota
- **Apenas valor** - Comparar apenas o valor
- **Apenas rótulo** - Comparar apenas o rótulo
- **Apenas nota** - Comparar apenas a nota
- **Valor e rótulo** - Comparar valor e rótulo
- **Valor e nota** - Comparar valor e nota
- **Rótulo e nota** - Comparar rótulo e nota
		]],
		["pa"] = [[
ਚੁਣੇ ਹੋਏ ਖੇਤਰਾਂ ਦੇ ਆਧਾਰ ’ਤੇ ਲਗਾਤਾਰ ਦੁਹਰਾਵਾਂ ਨੂੰ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਸਿਰਫ਼ ਉਹ ਡਾਟਾ ਪੁਆਇੰਟ ਅੱਗੇ ਜਾਂਦੇ ਹਨ ਜਿਨ੍ਹਾਂ ਦੇ ਚੁਣੇ ਹੋਏ ਖੇਤਰ ਪਿਛਲੇ ਪੁਆਇੰਟ ਤੋਂ ਵੱਖਰੇ ਹੋਣ।

- **ਸਾਰੇ ਖੇਤਰ** - ਮੁੱਲ, ਲੇਬਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਮੁੱਲ** - ਸਿਰਫ਼ ਮੁੱਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਲੇਬਲ** - ਸਿਰਫ਼ ਲੇਬਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਨੋਟ** - ਸਿਰਫ਼ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਮੁੱਲ ਅਤੇ ਲੇਬਲ** - ਮੁੱਲ ਅਤੇ ਲੇਬਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਮੁੱਲ ਅਤੇ ਨੋਟ** - ਮੁੱਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਲੇਬਲ ਅਤੇ ਨੋਟ** - ਲੇਬਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
		]],
		["ro"] = [[
Elimină duplicatele consecutive pe baza câmpurilor selectate. Trec mai departe doar punctele de date la care câmpurile selectate diferă de cele ale punctului anterior.

- **Toate câmpurile** - Compară valoarea, eticheta și nota
- **Doar valoarea** - Compară doar valoarea
- **Doar eticheta** - Compară doar eticheta
- **Doar nota** - Compară doar nota
- **Valoarea și eticheta** - Compară valoarea și eticheta
- **Valoarea și nota** - Compară valoarea și nota
- **Eticheta și nota** - Compară eticheta și nota
		]],
		["rm"] = [[
Filtra ora ils duplicats consecutivs tenor ils champs selecziunads. Mo ils puncts da datas nua che ils champs selecziunads sa differenzieschan dal precedent vegnan transmess.

- **Tuts champs** - Cumparegliar valur, etichetta e nota
- **Mo valur** - Cumparegliar mo la valur
- **Mo etichetta** - Cumparegliar mo l’etichetta
- **Mo nota** - Cumparegliar mo la nota
- **Valur ed etichetta** - Cumparegliar valur ed etichetta
- **Valur e nota** - Cumparegliar valur e nota
- **Etichetta e nota** - Cumparegliar etichetta e nota
		]],
		["ru"] = [[
Фильтрует последовательные дубликаты на основе выбранных полей. Пропускаются только точки данных, в которых выбранные поля отличаются от предыдущей точки.

- **Все поля** — Сравнивать значение, метку и заметку
- **Только значение** — Сравнивать только значение
- **Только метку** — Сравнивать только метку
- **Только заметку** — Сравнивать только заметку
- **Значение и метка** — Сравнивать значение и метку
- **Значение и заметку** — Сравнивать значение и заметку
- **Метка и заметка** — Сравнивать метку и заметку
		]],
		["sr"] = [[
Filtrira uzastopne duplikate na osnovu izabranih polja. Prolaze samo tačke podataka kod kojih se izabrana polja razlikuju od prethodne tačke.

- **Sva polja** - Poredi vrednost, oznaku i belešku
- **Samo vrednost** - Poredi samo vrednost
- **Samo oznaku** - Poredi samo oznaku
- **Samo belešku** - Poredi samo belešku
- **Vrednost i oznaku** - Poredi vrednost i oznaku
- **Vrednost i belešku** - Poredi vrednost i belešku
- **Oznaku i belešku** - Poredi oznaku i belešku
		]],
		["si"] = [[
තෝරාගත් ක්ෂේත්‍ර මත පදනම්ව අනුගාමී අනුපිටපත් ඉවත් කරයි. තෝරාගත් ක්ෂේත්‍ර පෙර දත්ත ලක්ෂ්‍යයෙන් වෙනස් වන දත්ත ලක්ෂ්‍ය පමණක් ඉදිරියට යවයි.

- **සියලු ක්ෂේත්‍ර** - අගය, ලේබලය සහ සටහන සසඳන්න
- **අගය පමණක්** - අගය පමණක් සසඳන්න
- **ලේබලය පමණක්** - ලේබලය පමණක් සසඳන්න
- **සටහන පමණක්** - සටහන පමණක් සසඳන්න
- **අගය සහ ලේබලය** - අගය සහ ලේබලය සසඳන්න
- **අගය සහ සටහන** - අගය සහ සටහන සසඳන්න
- **ලේබලය සහ සටහන** - ලේබලය සහ සටහන සසඳන්න
		]],
		["sk"] = [[
Odfiltruje po sebe idúce duplikáty na základe vybraných polí. Prejdú iba údajové body, v ktorých sa vybrané polia líšia od predchádzajúceho.

- **Všetky polia** - Porovnať hodnotu, označenie a poznámku
- **Iba hodnota** - Porovnať iba hodnotu
- **Iba označenie** - Porovnať iba označenie
- **Iba poznámka** - Porovnať iba poznámku
- **Hodnota a označenie** - Porovnať hodnotu a označenie
- **Hodnota a poznámka** - Porovnať hodnotu a poznámku
- **Označenie a poznámka** - Porovnať označenie a poznámku
		]],
		["sl"] = [[
Odstrani zaporedne dvojnike na podlagi izbranih polj. Prepustijo se le podatkovne točke, pri katerih se izbrana polja razlikujejo od prejšnje točke.

- **Vsa polja** – Primerjaj vrednost, oznako in opombo
- **Samo vrednost** – Primerjaj samo vrednost
- **Samo oznako** – Primerjaj samo oznako
- **Samo opombo** – Primerjaj samo opombo
- **Vrednost in oznako** – Primerjaj vrednost in oznako
- **Vrednost in opombo** – Primerjaj vrednost in opombo
- **Oznako in opombo** – Primerjaj oznako in opombo
		]],
		["es"] = [[
Filtra los duplicados consecutivos según los campos seleccionados. Solo pasan los puntos de datos cuyos campos seleccionados difieren de los del anterior.

- **Todos los campos** - Comparar valor, etiqueta y nota
- **Solo valor** - Comparar solo el valor
- **Solo etiqueta** - Comparar solo la etiqueta
- **Solo nota** - Comparar solo la nota
- **Valor y etiqueta** - Comparar valor y etiqueta
- **Valor y nota** - Comparar valor y nota
- **Etiqueta y nota** - Comparar etiqueta y nota
		]],
		["sw"] = [[
Huchuja marudio yanayofuatana kulingana na sehemu zilizochaguliwa. Ni nukta za data ambazo sehemu zilizochaguliwa zinatofautiana na za awali pekee ndizo zitapitishwa.

- **Sehemu Zote** - Linganisha thamani, lebo na dokezo
- **Thamani Pekee** - Linganisha thamani pekee
- **Lebo Pekee** - Linganisha lebo pekee
- **Dokezo Pekee** - Linganisha dokezo pekee
- **Thamani na Lebo** - Linganisha thamani na lebo
- **Thamani na Dokezo** - Linganisha thamani na dokezo
- **Lebo na Dokezo** - Linganisha lebo na dokezo
		]],
		["sv"] = [[
Filtrerar bort på varandra följande dubbletter baserat på valda fält. Endast datapunkter där de valda fälten skiljer sig från föregående datapunkt släpps igenom.

- **Alla fält** - Jämför värde, etikett och anteckning
- **Endast värde** - Jämför endast värde
- **Endast etikett** - Jämför endast etikett
- **Endast anteckning** - Jämför endast anteckning
- **Värde och etikett** - Jämför värde och etikett
- **Värde och anteckning** - Jämför värde och anteckning
- **Etikett och anteckning** - Jämför etikett och anteckning
		]],
		["ta"] = [[
தேர்ந்தெடுக்கப்பட்ட புலங்களின் அடிப்படையில் தொடர்ச்சியான நகல்களை வடிகட்டுகிறது. தேர்ந்தெடுக்கப்பட்ட புலங்கள் முந்தைய புள்ளியிலிருந்து வேறுபடும் தரவுப் புள்ளிகள் மட்டுமே தொடரும்.

- **அனைத்து புலங்களும்** - மதிப்பு, லேபிள் மற்றும் குறிப்பை ஒப்பிடு
- **மதிப்பு மட்டும்** - மதிப்பை மட்டும் ஒப்பிடு
- **லேபிள் மட்டும்** - லேபிளை மட்டும் ஒப்பிடு
- **குறிப்பு மட்டும்** - குறிப்பை மட்டும் ஒப்பிடு
- **மதிப்பு மற்றும் லேபிள்** - மதிப்பு மற்றும் லேபிளை ஒப்பிடு
- **மதிப்பு மற்றும் குறிப்பு** - மதிப்பு மற்றும் குறிப்பை ஒப்பிடு
- **லேபிள் மற்றும் குறிப்பு** - லேபிள் மற்றும் குறிப்பை ஒப்பிடு
		]],
		["te"] = [[
ఎంచుకున్న ఫీల్డ్‌ల ఆధారంగా వరుస నకళ్లను ఫిల్టర్ చేస్తుంది. ఎంచుకున్న ఫీల్డ్‌లు మునుపటి దానితో భిన్నంగా ఉన్న డేటా పాయింట్లు మాత్రమే కొనసాగుతాయి.

- **అన్ని ఫీల్డ్‌లు** - విలువ, లేబుల్, నోట్‌ను పోల్చండి
- **విలువ మాత్రమే** - విలువను మాత్రమే పోల్చండి
- **లేబుల్ మాత్రమే** - లేబుల్‌ను మాత్రమే పోల్చండి
- **నోట్ మాత్రమే** - నోట్‌ను మాత్రమే పోల్చండి
- **విలువ మరియు లేబుల్** - విలువ, లేబుల్‌ను పోల్చండి
- **విలువ మరియు నోట్** - విలువ, నోట్‌ను పోల్చండి
- **లేబుల్ మరియు నోట్** - లేబుల్, నోట్‌ను పోల్చండి
		]],
		["th"] = [[
กรองค่าซ้ำที่อยู่ติดกันโดยอิงตามฟิลด์ที่เลือก เฉพาะจุดข้อมูลที่ฟิลด์ที่เลือกแตกต่างจากจุดก่อนหน้าเท่านั้นที่จะถูกส่งต่อ

- **ทุกฟิลด์** - เปรียบเทียบค่า ป้ายกำกับ และบันทึก
- **ค่าเท่านั้น** - เปรียบเทียบเฉพาะค่า
- **ป้ายกำกับเท่านั้น** - เปรียบเทียบเฉพาะป้ายกำกับ
- **บันทึกเท่านั้น** - เปรียบเทียบเฉพาะบันทึก
- **ค่าและป้ายกำกับ** - เปรียบเทียบค่าและป้ายกำกับ
- **ค่าและบันทึก** - เปรียบเทียบค่าและบันทึก
- **ป้ายกำกับและบันทึก** - เปรียบเทียบป้ายกำกับและบันทึก
		]],
		["tr"] = [[
Seçilen alanlara göre ardışık yinelenenleri filtreler. Yalnızca seçilen alanları bir öncekinden farklı olan veri noktaları geçer.

- **Tüm Alanlar** - Değer, etiket ve notu karşılaştır
- **Yalnızca Değer** - Yalnızca değeri karşılaştır
- **Yalnızca Etiket** - Yalnızca etiketi karşılaştır
- **Yalnızca Not** - Yalnızca notu karşılaştır
- **Değer ve Etiket** - Değer ve etiketi karşılaştır
- **Değer ve Not** - Değer ve notu karşılaştır
- **Etiket ve Not** - Etiket ve notu karşılaştır
		]],
		["uk"] = [[
Відфільтровує послідовні дублікати на основі вибраних полів. Пропускаються лише точки даних, у яких вибрані поля відрізняються від попередньої точки.

- **Усі поля** — порівнювати значення, мітку та примітку
- **Лише значення** — порівнювати лише значення
- **Лише мітку** — порівнювати лише мітку
- **Лише примітку** — порівнювати лише примітку
- **Значення та мітку** — порівнювати значення та мітку
- **Значення та примітку** — порівнювати значення та примітку
- **Мітку та примітку** — порівнювати мітку та примітку
		]],
		["vi"] = [[
Loại bỏ các bản sao liên tiếp dựa trên các trường đã chọn. Chỉ những điểm dữ liệu có các trường đã chọn khác với điểm trước đó mới được giữ lại.

- **Tất cả trường** - So sánh giá trị, nhãn và ghi chú
- **Chỉ giá trị** - Chỉ so sánh giá trị
- **Chỉ nhãn** - Chỉ so sánh nhãn
- **Chỉ ghi chú** - Chỉ so sánh ghi chú
- **Giá trị và nhãn** - So sánh giá trị và nhãn
- **Giá trị và ghi chú** - So sánh giá trị và ghi chú
- **Nhãn và ghi chú** - So sánh nhãn và ghi chú
		]],
	},
	config = {
		enum {
			id = "compare_by",
			name = "_compare_by",
			options = {
				"_all_fields",
				"_value_only",
				"_label_only",
				"_note_only",
				"_value_and_label",
				"_value_and_note",
				"_label_and_note",
			},
			default = "_all_fields",
		},
	},

	-- Generator function
	generator = function(source, config)
		local compare_by = config and config.compare_by or "_all_fields"

		local last_value = nil
		local last_label = nil
		local last_note = nil

		return function()
			while true do
				local data_point = source.dp()
				if not data_point then
					return nil
				end

				local current_value = data_point.value
				local current_label = data_point.label
				local current_note = data_point.note

				local is_different = false

				if compare_by == "_all_fields" then
					is_different = (current_value ~= last_value)
						or (current_label ~= last_label)
						or (current_note ~= last_note)
				elseif compare_by == "_value_only" then
					is_different = (current_value ~= last_value)
				elseif compare_by == "_label_only" then
					is_different = (current_label ~= last_label)
				elseif compare_by == "_note_only" then
					is_different = (current_note ~= last_note)
				elseif compare_by == "_value_and_label" then
					is_different = (current_value ~= last_value) or (current_label ~= last_label)
				elseif compare_by == "_value_and_note" then
					is_different = (current_value ~= last_value) or (current_note ~= last_note)
				elseif compare_by == "_label_and_note" then
					is_different = (current_label ~= last_label) or (current_note ~= last_note)
				end

				if is_different then
					last_value = current_value
					last_label = current_label
					last_note = current_note
					return data_point
				end
			end
		end
	end,
}
