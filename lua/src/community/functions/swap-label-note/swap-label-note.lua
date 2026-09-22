-- Lua Function to swap label and note fields
-- Swaps the label and note of each data point

return {
	-- Configuration metadata
	id = "swap-label-note",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_transform"},
	title = {
		["en"] = "Swap Label and Note",
		["af"] = "Ruil Etiket en Nota Om",
		["sq"] = "Ndërro etiketën dhe shënimin",
		["am"] = "መለያና ማስታወሻ ቀያይር",
		["hy"] = "Փոխանակել պիտակը և նշումը",
		["az"] = "Etiketi və qeydi dəyiş",
		["bn"] = "লেবেল ও নোট অদলবদল করুন",
		["eu"] = "Trukatu etiketa eta oharra",
		["be"] = "Памяняць ярлык і заўвагу",
		["bg"] = "Размяна на етикет и бележка",
		["my"] = "အညွှန်းနှင့် မှတ်စုကို လဲရန်",
		["ca"] = "Intercanvia l’etiqueta i la nota",
		["zh-Hans"] = "交换标签和备注",
		["zh-Hant"] = "交換標籤和備註",
		["hr"] = "Zamijeni oznaku i bilješku",
		["cs"] = "Prohodit štítek a poznámku",
		["da"] = "Byt etiket og note",
		["nl"] = "Label en notitie omwisselen",
		["et"] = "Vaheta silt ja märkus",
		["fil"] = "Pagpalitin ang Label at Note",
		["fi"] = "Vaihda selite ja muistiinpano",
		["fr"] = "Échanger le libellé et la note",
		["gl"] = "Intercambiar etiqueta e nota",
		["ka"] = "იარლიყისა და შენიშვნის გაცვლა",
		["de"] = "Label und Notiz tauschen",
		["el"] = "Ανταλλαγή ετικέτας και σημείωσης",
		["gu"] = "લેબલ અને નોંધની અદલાબદલી કરો",
		["hi"] = "लेबल और नोट बदलें",
		["hu"] = "Címke és megjegyzés felcserélése",
		["is"] = "Víxla merki og athugasemd",
		["id"] = "Tukar Label dan Catatan",
		["it"] = "Scambia etichetta e nota",
		["ja"] = "ラベルとメモを入れ替え",
		["kn"] = "ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿ ವಿನಿಮಯಿಸಿ",
		["kk"] = "Жапсырма мен ескертпенің орнын ауыстыру",
		["km"] = "ប្ដូរស្លាក និងកំណត់ចំណាំ",
		["ko"] = "라벨과 메모 바꾸기",
		["ky"] = "Энбелги менен эскертменин ордун алмаштыруу",
		["lo"] = "ສະຫຼັບປ້າຍກຳກັບແລະບັນທຶກ",
		["lv"] = "Apmainīt vietām etiķeti un piezīmi",
		["lt"] = "Sukeisti etiketę ir pastabą",
		["mk"] = "Замени ги ознаката и белешката",
		["ms"] = "Tukar Label dan Nota",
		["ml"] = "ലേബലും കുറിപ്പും കൈമാറ്റം ചെയ്യുക",
		["mr"] = "लेबल आणि नोंद अदलाबदल करा",
		["mn"] = "Шошго ба тэмдэглэлийг солих",
		["ne"] = "लेबल र नोट साट्नुहोस्",
		["no"] = "Bytt etikett og notat",
		["pl"] = "Zamień etykietę i notatkę",
		["pt"] = "Trocar rótulo e nota",
		["pa"] = "ਲੇਬਲ ਅਤੇ ਨੋਟ ਬਦਲੋ",
		["ro"] = "Schimbă eticheta și nota",
		["rm"] = "Barattar l'etichetta e la nota",
		["ru"] = "Поменять местами метку и заметку",
		["sr"] = "Zameni oznaku i belešku",
		["si"] = "ලේබලය සහ සටහන මාරු කරන්න",
		["sk"] = "Vymeniť označenie a poznámku",
		["sl"] = "Zamenjaj oznako in opombo",
		["es"] = "Intercambiar etiqueta y nota",
		["sw"] = "Badilisha Lebo na Dokezo",
		["sv"] = "Byt plats på etikett och anteckning",
		["ta"] = "லேபிளையும் குறிப்பையும் மாற்று",
		["te"] = "లేబుల్ మరియు నోట్ మార్చి వేయి",
		["th"] = "สลับป้ายกำกับและบันทึก",
		["tr"] = "Etiket ve Notu Değiştir",
		["uk"] = "Поміняти місцями мітку та примітку",
		["vi"] = "Hoán đổi nhãn và ghi chú",
	},
	description = {
		["en"] = [[
Swaps the label and note fields of each data point.
		]],
		["af"] = [[
Ruil die etiket- en notavelde van elke datapunt om.
		]],
		["sq"] = [[
Ndërron fushat e etiketës dhe shënimit të çdo pike të të dhënave.
		]],
		["am"] = [[
የእያንዳንዱን የውሂብ ነጥብ የመለያና የማስታወሻ መስኮች ይቀያይራል።
		]],
		["hy"] = [[
Փոխանակում է յուրաքանչյուր տվյալակետի պիտակի և նշման դաշտերը։
		]],
		["az"] = [[
Hər məlumat nöqtəsinin etiket və qeyd sahələrini dəyişir.
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্টের লেবেল ও নোট ক্ষেত্র অদলবদল করে।
		]],
		["eu"] = [[
Datu-puntu bakoitzaren etiketa- eta ohar-eremuak trukatzen ditu.
		]],
		["be"] = [[
Мяняе месцамі палі ярлыка і заўвагі кожнай кропкі даных.
		]],
		["bg"] = [[
Разменя полетата за етикет и бележка на всяка точка от данни.
		]],
		["my"] = [[
ဒေတာအမှတ်တစ်ခုစီ၏ အညွှန်းနှင့် မှတ်စုအကွက်များကို လဲပေးသည်။
		]],
		["ca"] = [[
Intercanvia els camps d’etiqueta i nota de cada punt de dades.
		]],
		["zh-Hans"] = [[
交换每个数据点的标签和备注字段。
		]],
		["zh-Hant"] = [[
交換每個資料點的標籤和備註欄位。
		]],
		["hr"] = [[
Zamjenjuje polja oznake i bilješke svake podatkovne točke.
		]],
		["cs"] = [[
Prohodí pole štítku a poznámky u každého datového bodu.
		]],
		["da"] = [[
Bytter etiket- og notefelterne for hvert datapunkt.
		]],
		["nl"] = [[
Wisselt de label- en notitievelden van elk gegevenspunt om.
		]],
		["et"] = [[
Vahetab iga andmepunkti sildi- ja märkmevälja.
		]],
		["fil"] = [[
Pinagpapalit ang mga field na label at note ng bawat data point.
		]],
		["fi"] = [[
Vaihtaa kunkin datapisteen selite- ja muistiinpanokentät keskenään.
		]],
		["fr"] = [[
Échange les champs de libellé et de note de chaque point de données.
		]],
		["gl"] = [[
Intercambia os campos de etiqueta e nota de cada punto de datos.
		]],
		["ka"] = [[
თითოეული მონაცემის წერტილის იარლიყისა და შენიშვნის ველებს ცვლის.
		]],
		["de"] = [[
Vertauscht die Label- und Notizfelder jedes Datenpunkts.
		]],
		["el"] = [[
Ανταλλάσσει τα πεδία ετικέτας και σημείωσης κάθε σημείου δεδομένων.
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટના લેબલ અને નોંધ ફીલ્ડ્સની અદલાબદલી કરે છે.
		]],
		["hi"] = [[
हर डेटा पॉइंट के लेबल और नोट फ़ील्ड को आपस में बदलता है।
		]],
		["hu"] = [[
Felcseréli az egyes adatpontok címke- és megjegyzésmezőjét.
		]],
		["is"] = [[
Víxlar um merki- og athugasemdareitum hvers gagnapunkts.
		]],
		["id"] = [[
Menukar kolom label dan catatan pada setiap titik data.
		]],
		["it"] = [[
Scambia i campi dell'etichetta e della nota di ogni punto dati.
		]],
		["ja"] = [[
各データポイントのラベルとメモのフィールドを入れ替えます。
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿ ಕ್ಷೇತ್ರಗಳನ್ನು ವಿನಿಮಯಿಸುತ್ತದೆ.
		]],
		["kk"] = [[
Әр дерек нүктесінің жапсырмасы мен ескертпе өрістерінің орындарын ауыстырады.
		]],
		["km"] = [[
ប្ដូរវាលស្លាក និងកំណត់ចំណាំរបស់ចំណុចទិន្នន័យនីមួយៗ។
		]],
		["ko"] = [[
각 데이터 포인트의 라벨과 메모 필드를 서로 바꿉니다.
		]],
		["ky"] = [[
Ар бир маалымат чекитинин энбелги жана эскертме талааларынын ордун алмаштырат.
		]],
		["lo"] = [[
ສະຫຼັບຊ່ອງປ້າຍກຳກັບແລະບັນທຶກຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດ.
		]],
		["lv"] = [[
Apmaina vietām katra datu punkta etiķetes un piezīmes laukus.
		]],
		["lt"] = [[
Sukeičia kiekvieno duomenų taško etiketės ir pastabos laukus vietomis.
		]],
		["mk"] = [[
Ги заменува полињата за ознака и белешка на секоја точка на податоци.
		]],
		["ms"] = [[
Menukar medan label dan nota bagi setiap titik data.
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും label, note ഫീൽഡുകൾ കൈമാറ്റം ചെയ്യുന്നു.
		]],
		["mr"] = [[
प्रत्येक डेटा पॉइंटची लेबल आणि नोंद फील्ड अदलाबदल करते.
		]],
		["mn"] = [[
Өгөгдлийн цэг бүрийн шошго болон тэмдэглэлийн талбарыг солино.
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दुको लेबल र नोट फिल्ड साट्छ।
		]],
		["no"] = [[
Bytter etikett- og notatfeltene for hvert datapunkt.
		]],
		["pl"] = [[
Zamienia pola etykiety i notatki każdego punktu danych.
		]],
		["pt"] = [[
Troca os campos de rótulo e nota de cada ponto de dados.
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਲੇਬਲ ਅਤੇ ਨੋਟ ਖੇਤਰਾਂ ਨੂੰ ਆਪਸ ਵਿੱਚ ਬਦਲਦਾ ਹੈ।
		]],
		["ro"] = [[
Schimbă câmpurile etichetă și notă ale fiecărui punct de date.
		]],
		["rm"] = [[
Baratta ils champs d'etichetta e da nota da mintga punct da datas.
		]],
		["ru"] = [[
Меняет местами поля метки и заметки каждой точки данных.
		]],
		["sr"] = [[
Zamenjuje polja oznake i beleške svake tačke podataka.
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයකම ලේබලය සහ සටහන් ක්ෂේත්‍ර මාරු කරයි.
		]],
		["sk"] = [[
Vymení polia označenia a poznámky každého údajového bodu.
		]],
		["sl"] = [[
Zamenja polji oznake in opombe vsake podatkovne točke.
		]],
		["es"] = [[
Intercambia los campos de etiqueta y nota de cada punto de datos.
		]],
		["sw"] = [[
Hubadilisha sehemu za lebo na dokezo za kila nukta ya data.
		]],
		["sv"] = [[
Byter plats på etikett- och anteckningsfälten för varje datapunkt.
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளியின் லேபிள் மற்றும் குறிப்பு புலங்களை மாற்றுகிறது.
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్‌లోని లేబుల్, నోట్ ఫీల్డ్‌లను మార్చి వేస్తుంది.
		]],
		["th"] = [[
สลับฟิลด์ป้ายกำกับและบันทึกของจุดข้อมูลแต่ละจุด
		]],
		["tr"] = [[
Her veri noktasının etiket ve not alanlarını değiştirir.
		]],
		["uk"] = [[
Міняє місцями поля мітки та примітки кожної точки даних.
		]],
		["vi"] = [[
Hoán đổi các trường nhãn và ghi chú của mỗi điểm dữ liệu.
		]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			-- Swap label and note
			local temp = data_point.label
			data_point.label = data_point.note
			data_point.note = temp

			return data_point
		end
	end,
}
