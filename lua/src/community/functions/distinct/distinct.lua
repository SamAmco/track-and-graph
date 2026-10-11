-- Lua Function to filter out duplicate data points based on selected fields
-- Only the first data point encountered for each distinct key is passed through

local enum = require("tng.config").enum

local NIL_KEY = {}
local NAN_KEY = {}

local function normalize_key(value)
	if value == nil then
		return NIL_KEY
	end

	-- NaN cannot safely be used as a table key. Treat all NaN values as equal.
	if type(value) == "number" and value ~= value then
		return NAN_KEY
	end

	return value
end

local function has_been_seen(seen, keys)
	local node = seen
	for index = 1, #keys - 1 do
		local key = normalize_key(keys[index])
		local next_node = node[key]
		if not next_node then
			next_node = {}
			node[key] = next_node
		end
		node = next_node
	end

	local final_key = normalize_key(keys[#keys])
	if node[final_key] then
		return true
	end

	node[final_key] = true
	return false
end

return {
	id = "distinct",
	version = "1.0.0",
	inputCount = 1,
	categories = { "_filter" },
	title = {
		["en"] = "Distinct",
		["af"] = "Uniek",
		["sq"] = "Të dallueshme",
		["am"] = "ልዩ",
		["hy"] = "Չկրկնվող",
		["az"] = "Fərqli",
		["bn"] = "স্বতন্ত্র",
		["eu"] = "Desberdinak",
		["be"] = "Унікальныя",
		["bg"] = "Уникални",
		["my"] = "မတူညီသော",
		["ca"] = "Diferents",
		["zh-Hans"] = "去重",
		["zh-Hant"] = "不重複",
		["hr"] = "Jedinstveno",
		["cs"] = "Jedinečné",
		["da"] = "Unikke",
		["nl"] = "Uniek",
		["et"] = "Eristuvad",
		["fil"] = "Natatangi",
		["fi"] = "Erottuvat",
		["fr"] = "Distinct",
		["gl"] = "Distintos",
		["ka"] = "უნიკალური",
		["de"] = "Eindeutig",
		["el"] = "Διακριτά",
		["gu"] = "અલગ",
		["hi"] = "अद्वितीय",
		["hu"] = "Egyedi",
		["is"] = "Aðgreind",
		["id"] = "Unik",
		["it"] = "Distinti",
		["ja"] = "重複除外",
		["kn"] = "ವಿಶಿಷ್ಟ",
		["kk"] = "Бірегей",
		["km"] = "ខុសគ្នា",
		["ko"] = "고유",
		["ky"] = "Кайталанбаган",
		["lo"] = "ແຕກຕ່າງກັນ",
		["lv"] = "Atšķirīgie",
		["lt"] = "Unikalūs",
		["mk"] = "Различни",
		["ms"] = "Berbeza",
		["ml"] = "വ്യത്യസ്തം",
		["mr"] = "भिन्न",
		["mn"] = "Ялгаатай",
		["ne"] = "विशिष्ट",
		["no"] = "Unike",
		["pl"] = "Unikalne",
		["pt"] = "Distintos",
		["pa"] = "ਵਿਲੱਖਣ",
		["ro"] = "Distinct",
		["rm"] = "Distinct",
		["ru"] = "Уникальные",
		["sr"] = "Jedinstveni",
		["si"] = "වෙනස්",
		["sk"] = "Jedinečné",
		["sl"] = "Različni",
		["es"] = "Distintos",
		["sw"] = "Tofauti",
		["sv"] = "Unika",
		["ta"] = "தனித்துவமானது",
		["te"] = "విభిన్నమైనవి",
		["th"] = "ไม่ซ้ำกัน",
		["tr"] = "Benzersiz",
		["uk"] = "Унікальні",
		["vi"] = "Không trùng lặp",
	},
	description = {
		["en"] = [[
Filters out duplicate data points based on the selected fields. Data points are traversed in reverse chronological order, so the most recent data point for each distinct combination is kept.

- **All Fields** - Compare value, label, and note
- **Value Only** - Compare value only
- **Label Only** - Compare label only
- **Note Only** - Compare note only
- **Value and Label** - Compare value and label
- **Value and Note** - Compare value and note
- **Label and Note** - Compare label and note
		]],
		["af"] = [[
Filter duplikaatdatapunte uit op grond van die gekose velde. Datapunte word in omgekeerde chronologiese volgorde deurgegaan, sodat die mees onlangse datapunt vir elke unieke kombinasie behou word.

- **Alle velde** - Vergelyk waarde, etiket en nota
- **Slegs waarde** - Vergelyk slegs waarde
- **Slegs etiket** - Vergelyk slegs etiket
- **Slegs nota** - Vergelyk slegs nota
- **Waarde en etiket** - Vergelyk waarde en etiket
- **Waarde en nota** - Vergelyk waarde en nota
- **Etiket en nota** - Vergelyk etiket en nota
		]],
		["sq"] = [[
Filtron pikat e të dhënave të përsëritura bazuar në fushat e zgjedhura. Pikat e të dhënave përshkohen në rend kronologjik të kundërt, ndaj ruhet pika më e fundit e të dhënave për çdo kombinim të dallueshëm.

- **Të gjitha fushat** - Krahason vlerën, etiketën dhe shënimin
- **Vetëm vlera** - Krahason vetëm vlerën
- **Vetëm etiketa** - Krahason vetëm etiketën
- **Vetëm shënimi** - Krahason vetëm shënimin
- **Vlera dhe etiketa** - Krahason vlerën dhe etiketën
- **Vlera dhe shënimi** - Krahason vlerën dhe shënimin
- **Etiketa dhe shënimi** - Krahason etiketën dhe shënimin
		]],
		["am"] = [[
በተመረጡት መስኮች መሠረት የተደጋገሙ የውሂብ ነጥቦችን ያጣራል። የውሂብ ነጥቦቹ ከአዲሱ ወደ አሮጌው በተቃራኒ የጊዜ ቅደም ተከተል ስለሚታዩ፣ ለእያንዳንዱ ልዩ ጥምረት በጣም የቅርብ ጊዜው የውሂብ ነጥብ ይቀመጣል።

- **ሁሉም መስኮች** - ዋጋን፣ መለያን እና ማስታወሻን ያወዳድሩ
- **ዋጋ ብቻ** - ዋጋን ብቻ ያወዳድሩ
- **መለያ ብቻ** - መለያን ብቻ ያወዳድሩ
- **ማስታወሻ ብቻ** - ማስታወሻን ብቻ ያወዳድሩ
- **ዋጋ እና መለያ** - ዋጋን እና መለያን ያወዳድሩ
- **ዋጋ እና ማስታወሻ** - ዋጋን እና ማስታወሻን ያወዳድሩ
- **መለያ እና ማስታወሻ** - መለያን እና ማስታወሻን ያወዳድሩ
		]],
		["hy"] = [[
Զտում է կրկնվող տվյալների կետերը՝ հիմնվելով ընտրված դաշտերի վրա։ Տվյալների կետերն անցնում են հակառակ ժամանակագրական հերթականությամբ, ուստի յուրաքանչյուր չկրկնվող համակցության համար պահպանվում է ամենավերջին տվյալների կետը։

- **Բոլոր դաշտերը** - Համեմատել արժեքը, պիտակը և նշումը
- **Միայն արժեքը** - Համեմատել միայն արժեքը
- **Միայն պիտակը** - Համեմատել միայն պիտակը
- **Միայն նշումը** - Համեմատել միայն նշումը
- **Արժեքը և պիտակը** - Համեմատել արժեքը և պիտակը
- **Արժեքը և նշումը** - Համեմատել արժեքը և նշումը
- **Պիտակը և նշումը** - Համեմատել պիտակը և նշումը
		]],
		["az"] = [[
Seçilmiş sahələrə əsasən təkrarlanan məlumat nöqtələrini filtrləyir. Məlumat nöqtələri tərs xronoloji ardıcıllıqla nəzərdən keçirilir, buna görə də hər fərqli kombinasiya üçün ən son məlumat nöqtəsi saxlanılır.

- **Bütün sahələr** - Dəyəri, etiketi və qeydi müqayisə et
- **Yalnız dəyər** - Yalnız dəyəri müqayisə et
- **Yalnız etiket** - Yalnız etiketi müqayisə et
- **Yalnız qeyd** - Yalnız qeydi müqayisə et
- **Dəyər və etiket** - Dəyəri və etiketi müqayisə et
- **Dəyər və qeyd** - Dəyəri və qeydi müqayisə et
- **Etiket və qeyd** - Etiketi və qeydi müqayisə et
		]],
		["bn"] = [[
নির্বাচিত ক্ষেত্রগুলোর ভিত্তিতে ডুপ্লিকেট ডেটা পয়েন্ট বাদ দেয়। ডেটা পয়েন্টগুলো বিপরীত কালানুক্রমিক ক্রমে পর্যালোচনা করা হয়, তাই প্রতিটি স্বতন্ত্র সমন্বয়ের সর্বশেষ ডেটা পয়েন্টটি রাখা হয়।

- **সব ক্ষেত্র** - মান, লেবেল এবং নোট তুলনা করুন
- **শুধু মান** - শুধু মান তুলনা করুন
- **শুধু লেবেল** - শুধু লেবেল তুলনা করুন
- **শুধু নোট** - শুধু নোট তুলনা করুন
- **মান ও লেবেল** - মান এবং লেবেল তুলনা করুন
- **মান ও নোট** - মান এবং নোট তুলনা করুন
- **লেবেল ও নোট** - লেবেল এবং নোট তুলনা করুন
		]],
		["eu"] = [[
Bikoiztutako datu-puntuak iragazten ditu, hautatutako eremuetan oinarrituta. Datu-puntuak alderantzizko ordena kronologikoan zeharkatzen dira; beraz, konbinazio desberdin bakoitzeko datu-punturik berriena gordetzen da.

- **Eremu guztiak** - Balioa, etiketa eta oharra alderatu
- **Balioa soilik** - Balioa soilik alderatu
- **Etiketa soilik** - Etiketa soilik alderatu
- **Oharra soilik** - Oharra soilik alderatu
- **Balioa eta etiketa** - Balioa eta etiketa alderatu
- **Balioa eta oharra** - Balioa eta oharra alderatu
- **Etiketa eta oharra** - Etiketa eta oharra alderatu
		]],
		["be"] = [[
Адфільтроўвае дублікаты пунктаў даных на аснове выбраных палёў. Пункты даных праглядаюцца ў зваротным храналагічным парадку, таму захоўваецца самы апошні пункт даных для кожнай унікальнай камбінацыі.

- **Усе палі** — параўноўваць значэнне, метку і нататку
- **Толькі значэнне** — параўноўваць толькі значэнне
- **Толькі метка** — параўноўваць толькі метку
- **Толькі нататка** — параўноўваць толькі нататку
- **Значэнне і метка** — параўноўваць значэнне і метку
- **Значэнне і нататка** — параўноўваць значэнне і нататку
- **Метка і нататка** — параўноўваць метку і нататку
		]],
		["bg"] = [[
Филтрира дублиращите се точки от данни въз основа на избраните полета. Точките от данни се обхождат в обратен хронологичен ред, така че се запазва най-скорошната точка от данни за всяка уникална комбинация.

- **Всички полета** – Сравнява стойността, етикета и бележката
- **Само стойност** – Сравнява само стойността
- **Само етикет** – Сравнява само етикета
- **Само бележка** – Сравнява само бележката
- **Стойност и етикет** – Сравнява стойността и етикета
- **Стойност и бележка** – Сравнява стойността и бележката
- **Етикет и бележка** – Сравнява етикета и бележката
		]],
		["my"] = [[
ရွေးချယ်ထားသော အကွက်များအပေါ် အခြေခံ၍ ထပ်နေသော ဒေတာအချက်များကို ဖယ်ရှားသည်။ ဒေတာအချက်များကို အချိန်စဉ်ပြောင်းပြန်အတိုင်း လှည့်လည်စစ်ဆေးသောကြောင့် မတူညီသော ပေါင်းစပ်မှုတစ်ခုစီအတွက် နောက်ဆုံးရ ဒေတာအချက်ကို ထိန်းသိမ်းထားသည်။

- **အကွက်အားလုံး** - တန်ဖိုး၊ အညွှန်းနှင့် မှတ်စုကို နှိုင်းယှဉ်ပါ
- **တန်ဖိုးသာ** - တန်ဖိုးကိုသာ နှိုင်းယှဉ်ပါ
- **အညွှန်းသာ** - အညွှန်းကိုသာ နှိုင်းယှဉ်ပါ
- **မှတ်စုသာ** - မှတ်စုကိုသာ နှိုင်းယှဉ်ပါ
- **တန်ဖိုးနှင့် အညွှန်း** - တန်ဖိုးနှင့် အညွှန်းကို နှိုင်းယှဉ်ပါ
- **တန်ဖိုးနှင့် မှတ်စု** - တန်ဖိုးနှင့် မှတ်စုကို နှိုင်းယှဉ်ပါ
- **အညွှန်းနှင့် မှတ်စု** - အညွှန်းနှင့် မှတ်စုကို နှိုင်းယှဉ်ပါ
		]],
		["ca"] = [[
Filtra els punts de dades duplicats segons els camps seleccionats. Els punts de dades es recorren en ordre cronològic invers, de manera que es conserva el punt de dades més recent de cada combinació diferent.

- **Tots els camps** - Compara el valor, l'etiqueta i la nota
- **Només el valor** - Compara només el valor
- **Només l'etiqueta** - Compara només l'etiqueta
- **Només la nota** - Compara només la nota
- **Valor i etiqueta** - Compara el valor i l'etiqueta
- **Valor i nota** - Compara el valor i la nota
- **Etiqueta i nota** - Compara l'etiqueta i la nota
		]],
		["zh-Hans"] = [[
根据所选字段过滤重复数据点。数据点按时间倒序遍历，因此会保留每个不同组合中最新的数据点。

- **所有字段** - 比较数值、标签和备注
- **仅数值** - 仅比较数值
- **仅标签** - 仅比较标签
- **仅备注** - 仅比较备注
- **数值和标签** - 比较数值和标签
- **数值和备注** - 比较数值和备注
- **标签和备注** - 比较标签和备注
		]],
		["zh-Hant"] = [[
根據選取的欄位篩除重複的資料點。資料點會依反向時間順序遍歷，因此每個不重複組合只會保留最新的資料點。

- **所有欄位** - 比較數值、標籤和備註
- **僅數值** - 僅比較數值
- **僅標籤** - 僅比較標籤
- **僅備註** - 僅比較備註
- **數值和標籤** - 比較數值和標籤
- **數值和備註** - 比較數值和備註
- **標籤和備註** - 比較標籤和備註
		]],
		["hr"] = [[
Filtrira duplicirane podatkovne točke na temelju odabranih polja. Podatkovne točke obrađuju se obrnutim kronološkim redoslijedom, pa se zadržava najnovija podatkovna točka za svaku jedinstvenu kombinaciju.

- **Sva polja** - Uspoređuju se vrijednost, oznaka i bilješka
- **Samo vrijednost** - Uspoređuje se samo vrijednost
- **Samo oznaka** - Uspoređuje se samo oznaka
- **Samo bilješka** - Uspoređuje se samo bilješka
- **Vrijednost i oznaka** - Uspoređuju se vrijednost i oznaka
- **Vrijednost i bilješka** - Uspoređuju se vrijednost i bilješka
- **Oznaka i bilješka** - Uspoređuju se oznaka i bilješka
		]],
		["cs"] = [[
Odstraní duplicitní datové body na základě vybraných polí. Datové body prochází v obráceném chronologickém pořadí, takže se zachová nejnovější datový bod pro každou jedinečnou kombinaci.

- **Všechna pole** - Porovnat hodnotu, štítek a poznámku
- **Pouze hodnota** - Porovnat pouze hodnotu
- **Pouze štítek** - Porovnat pouze štítek
- **Pouze poznámka** - Porovnat pouze poznámku
- **Hodnota a štítek** - Porovnat hodnotu a štítek
- **Hodnota a poznámka** - Porovnat hodnotu a poznámku
- **Štítek a poznámka** - Porovnat štítek a poznámku
		]],
		["da"] = [[
Filtrerer duplikate datapunkter fra baseret på de valgte felter. Datapunkterne gennemgås i omvendt kronologisk rækkefølge, så det seneste datapunkt for hver unikke kombination bevares.

- **Alle felter** - Sammenlign værdi, etiket og note
- **Kun værdi** - Sammenlign kun værdi
- **Kun etiket** - Sammenlign kun etiket
- **Kun note** - Sammenlign kun note
- **Værdi og etiket** - Sammenlign værdi og etiket
- **Værdi og note** - Sammenlign værdi og note
- **Etiket og note** - Sammenlign etiket og note
		]],
		["nl"] = [[
Filtert dubbele gegevenspunten op basis van de geselecteerde velden. Gegevenspunten worden in omgekeerd chronologische volgorde doorlopen, zodat het meest recente gegevenspunt voor elke unieke combinatie behouden blijft.

- **Alle velden** - Waarde, label en notitie vergelijken
- **Alleen waarde** - Alleen de waarde vergelijken
- **Alleen label** - Alleen het label vergelijken
- **Alleen notitie** - Alleen de notitie vergelijken
- **Waarde en label** - Waarde en label vergelijken
- **Waarde en notitie** - Waarde en notitie vergelijken
- **Label en notitie** - Label en notitie vergelijken
		]],
		["et"] = [[
Filtreerib valitud väljade põhjal dubleerivad andmepunktid välja. Andmepunktid läbivad töötluse vastupidises kronoloogilises järjekorras, seega säilitatakse iga eristatava kombinatsiooni kõige uuem andmepunkt.

- **Kõik väljad** – Võrdle väärtust, silti ja märkust
- **Ainult väärtus** – Võrdle ainult väärtust
- **Ainult silt** – Võrdle ainult silti
- **Ainult märkus** – Võrdle ainult märkust
- **Väärtus ja silt** – Võrdle väärtust ja silti
- **Väärtus ja märkus** – Võrdle väärtust ja märkust
- **Silt ja märkus** – Võrdle silti ja märkust
		]],
		["fil"] = [[
Inaalis ang mga dobleng data point batay sa mga napiling field. Tinitingnan ang mga data point mula sa pinakabago patungo sa pinakaluma, kaya pinananatili ang pinakabagong data point para sa bawat natatanging kombinasyon.

- **Lahat ng Field** - Ihambing ang value, label, at note
- **Value Lamang** - Ihambing ang value lamang
- **Label Lamang** - Ihambing ang label lamang
- **Note Lamang** - Ihambing ang note lamang
- **Value at Label** - Ihambing ang value at label
- **Value at Note** - Ihambing ang value at note
- **Label at Note** - Ihambing ang label at note
		]],
		["fi"] = [[
Suodattaa päällekkäiset datapisteet pois valittujen kenttien perusteella. Datapisteet käydään läpi käänteisessä aikajärjestyksessä, joten kunkin erottuvan yhdistelmän uusin datapiste säilytetään.

- **Kaikki kentät** – Vertaa arvoa, tunnistetta ja muistiinpanoa
- **Vain arvo** – Vertaa vain arvoa
- **Vain tunniste** – Vertaa vain tunnistetta
- **Vain muistiinpano** – Vertaa vain muistiinpanoa
- **Arvo ja tunniste** – Vertaa arvoa ja tunnistetta
- **Arvo ja muistiinpano** – Vertaa arvoa ja muistiinpanoa
- **Tunniste ja muistiinpano** – Vertaa tunnistetta ja muistiinpanoa
		]],
		["fr"] = [[
Filtre les points de données en double selon les champs sélectionnés. Les points de données sont parcourus dans l’ordre antéchronologique, de sorte que le point de données le plus récent pour chaque combinaison distincte est conservé.

- **Tous les champs** - Compare la valeur, le libellé et la note
- **Valeur uniquement** - Compare uniquement la valeur
- **Libellé uniquement** - Compare uniquement le libellé
- **Note uniquement** - Compare uniquement la note
- **Valeur et libellé** - Compare la valeur et le libellé
- **Valeur et note** - Compare la valeur et la note
- **Libellé et note** - Compare le libellé et la note
		]],
		["gl"] = [[
Filtra os puntos de datos duplicados en función dos campos seleccionados. Os puntos de datos percorrense en orde cronolóxica inversa, polo que se conserva o punto de datos máis recente de cada combinación distinta.

- **Todos os campos** - Compara o valor, a etiqueta e a nota
- **Só o valor** - Compara só o valor
- **Só a etiqueta** - Compara só a etiqueta
- **Só a nota** - Compara só a nota
- **Valor e etiqueta** - Compara o valor e a etiqueta
- **Valor e nota** - Compara o valor e a nota
- **Etiqueta e nota** - Compara a etiqueta e a nota
		]],
		["ka"] = [[
ფილტრავს დუბლირებულ მონაცემთა წერტილებს არჩეული ველების მიხედვით. მონაცემთა წერტილები განიხილება უკუ ქრონოლოგიური თანმიმდევრობით, ამიტომ თითოეული უნიკალური კომბინაციისთვის ინახება უახლესი მონაცემთა წერტილი.

- **ყველა ველი** - მნიშვნელობის, იარლიყისა და შენიშვნის შედარება
- **მხოლოდ მნიშვნელობა** - მხოლოდ მნიშვნელობის შედარება
- **მხოლოდ იარლიყი** - მხოლოდ იარლიყის შედარება
- **მხოლოდ შენიშვნა** - მხოლოდ შენიშვნის შედარება
- **მნიშვნელობა და იარლიყი** - მნიშვნელობისა და იარლიყის შედარება
- **მნიშვნელობა და შენიშვნა** - მნიშვნელობისა და შენიშვნის შედარება
- **იარლიყი და შენიშვნა** - იარლიყისა და შენიშვნის შედარება
		]],
		["de"] = [[
Filtert doppelte Datenpunkte anhand der ausgewählten Felder heraus. Die Datenpunkte werden in umgekehrt chronologischer Reihenfolge durchlaufen, sodass der aktuellste Datenpunkt für jede eindeutige Kombination beibehalten wird.

- **Alle Felder** – Wert, Label und Notiz vergleichen
- **Nur Wert** – Nur den Wert vergleichen
- **Nur Label** – Nur das Label vergleichen
- **Nur Notiz** – Nur die Notiz vergleichen
- **Wert und Label** – Wert und Label vergleichen
- **Wert und Notiz** – Wert und Notiz vergleichen
- **Label und Notiz** – Label und Notiz vergleichen
		]],
		["el"] = [[
Φιλτράρει τα διπλότυπα σημεία δεδομένων με βάση τα επιλεγμένα πεδία. Τα σημεία δεδομένων διατρέχονται με αντίστροφη χρονολογική σειρά, ώστε να διατηρείται το πιο πρόσφατο σημείο δεδομένων για κάθε διακριτό συνδυασμό.

- **Όλα τα πεδία** - Σύγκριση τιμής, ετικέτας και σημείωσης
- **Μόνο τιμή** - Σύγκριση μόνο της τιμής
- **Μόνο ετικέτα** - Σύγκριση μόνο της ετικέτας
- **Μόνο σημείωση** - Σύγκριση μόνο της σημείωσης
- **Τιμή και ετικέτα** - Σύγκριση τιμής και ετικέτας
- **Τιμή και σημείωση** - Σύγκριση τιμής και σημείωσης
- **Ετικέτα και σημείωση** - Σύγκριση ετικέτας και σημείωσης
		]],
		["gu"] = [[
પસંદ કરેલા ક્ષેત્રોના આધારે ડુપ્લિકેટ ડેટા પૉઇન્ટ્સને ફિલ્ટર કરે છે. ડેટા પૉઇન્ટ્સને નવીનતમથી જૂનાતમ ક્રમમાં તપાસવામાં આવે છે, તેથી દરેક અલગ સંયોજન માટેનો સૌથી તાજેતરનો ડેટા પૉઇન્ટ રાખવામાં આવે છે.

- **બધા ક્ષેત્રો** - મૂલ્ય, લેબલ અને નોંધની તુલના કરો
- **માત્ર મૂલ્ય** - માત્ર મૂલ્યની તુલના કરો
- **માત્ર લેબલ** - માત્ર લેબલની તુલના કરો
- **માત્ર નોંધ** - માત્ર નોંધની તુલના કરો
- **મૂલ્ય અને લેબલ** - મૂલ્ય અને લેબલની તુલના કરો
- **મૂલ્ય અને નોંધ** - મૂલ્ય અને નોંધની તુલના કરો
- **લેબલ અને નોંધ** - લેબલ અને નોંધની તુલના કરો
		]],
		["hi"] = [[
चयनित फ़ील्ड के आधार पर डुप्लिकेट डेटा बिंदुओं को फ़िल्टर करता है। डेटा बिंदुओं को नवीनतम से पुराने क्रम में उल्टे कालानुक्रमिक क्रम में देखा जाता है, इसलिए प्रत्येक अद्वितीय संयोजन के लिए सबसे हाल का डेटा बिंदु रखा जाता है।

- **सभी फ़ील्ड** - मान, लेबल और नोट की तुलना करें
- **केवल मान** - केवल मान की तुलना करें
- **केवल लेबल** - केवल लेबल की तुलना करें
- **केवल नोट** - केवल नोट की तुलना करें
- **मान और लेबल** - मान और लेबल की तुलना करें
- **मान और नोट** - मान और नोट की तुलना करें
- **लेबल और नोट** - लेबल और नोट की तुलना करें
		]],
		["hu"] = [[
Kiszűri a kiválasztott mezők alapján az ismétlődő adatpontokat. Az adatpontok fordított időrendi sorrendben kerülnek feldolgozásra, így minden egyedi kombinációhoz a legutóbbi adatpont marad meg.

- **Összes mező** - Érték, címke és megjegyzés összehasonlítása
- **Csak érték** - Csak az érték összehasonlítása
- **Csak címke** - Csak a címke összehasonlítása
- **Csak megjegyzés** - Csak a megjegyzés összehasonlítása
- **Érték és címke** - Az érték és a címke összehasonlítása
- **Érték és megjegyzés** - Az érték és a megjegyzés összehasonlítása
- **Címke és megjegyzés** - A címke és a megjegyzés összehasonlítása
		]],
		["is"] = [[
Síar út tvíteknar gagnapunkta út frá völdum reitum. Gagnapunktar eru yfirfarnir í öfugri tímaröð, þannig að nýjasti gagnapunkturinn fyrir hverja aðgreinda samsetningu er geymdur.

- **Allir reitir** - Berðu saman gildi, merki og athugasemd
- **Aðeins gildi** - Berðu aðeins saman gildi
- **Aðeins merki** - Berðu aðeins saman merki
- **Aðeins athugasemd** - Berðu aðeins saman athugasemd
- **Gildi og merki** - Berðu saman gildi og merki
- **Gildi og athugasemd** - Berðu saman gildi og athugasemd
- **Merki og athugasemd** - Berðu saman merki og athugasemd
		]],
		["id"] = [[
Menyaring titik data duplikat berdasarkan bidang yang dipilih. Titik data ditelusuri dalam urutan kronologis terbalik, sehingga titik data terbaru untuk setiap kombinasi unik dipertahankan.

- **Semua Bidang** - Bandingkan nilai, label, dan catatan
- **Nilai Saja** - Bandingkan nilai saja
- **Label Saja** - Bandingkan label saja
- **Catatan Saja** - Bandingkan catatan saja
- **Nilai dan Label** - Bandingkan nilai dan label
- **Nilai dan Catatan** - Bandingkan nilai dan catatan
- **Label dan Catatan** - Bandingkan label dan catatan
		]],
		["it"] = [[
Filtra i punti dati duplicati in base ai campi selezionati. I punti dati vengono attraversati in ordine cronologico inverso, quindi viene mantenuto il punto dati più recente per ogni combinazione distinta.

- **Tutti i campi** - Confronta valore, etichetta e nota
- **Solo valore** - Confronta solo il valore
- **Solo etichetta** - Confronta solo l’etichetta
- **Solo nota** - Confronta solo la nota
- **Valore ed etichetta** - Confronta valore ed etichetta
- **Valore e nota** - Confronta valore e nota
- **Etichetta e nota** - Confronta etichetta e nota
		]],
		["ja"] = [[
選択したフィールドに基づいて、重複するデータポイントを除外します。データポイントは新しいものから古いものへ逆時系列順に処理されるため、各一意の組み合わせについて最新のデータポイントが保持されます。

- **すべてのフィールド** - 値、ラベル、メモを比較
- **値のみ** - 値のみを比較
- **ラベルのみ** - ラベルのみを比較
- **メモのみ** - メモのみを比較
- **値とラベル** - 値とラベルを比較
- **値とメモ** - 値とメモを比較
- **ラベルとメモ** - ラベルとメモを比較
		]],
		["kn"] = [[
ಆಯ್ಕೆಮಾಡಿದ ಕ್ಷೇತ್ರಗಳ ಆಧಾರದ ಮೇಲೆ ನಕಲಿ ಡೇಟಾ ಪಾಯಿಂಟ್‌ಗಳನ್ನು ತೆಗೆದುಹಾಕುತ್ತದೆ. ಡೇಟಾ ಪಾಯಿಂಟ್‌ಗಳನ್ನು ಇತ್ತೀಚಿನದರಿಂದ ಹಳೆಯದರವರೆಗೆ ಕ್ರಮದಲ್ಲಿ ಪರಿಶೀಲಿಸಲಾಗುತ್ತದೆ, ಆದ್ದರಿಂದ ಪ್ರತಿಯೊಂದು ವಿಶಿಷ್ಟ ಸಂಯೋಜನೆಗೆ ಇತ್ತೀಚಿನ ಡೇಟಾ ಪಾಯಿಂಟ್ ಉಳಿಯುತ್ತದೆ.

- **ಎಲ್ಲಾ ಕ್ಷೇತ್ರಗಳು** - ಮೌಲ್ಯ, ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿಯನ್ನು ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮಾತ್ರ** - ಮೌಲ್ಯವನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಲೇಬಲ್ ಮಾತ್ರ** - ಲೇಬಲ್ ಅನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಟಿಪ್ಪಣಿ ಮಾತ್ರ** - ಟಿಪ್ಪಣಿಯನ್ನು ಮಾತ್ರ ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮತ್ತು ಲೇಬಲ್** - ಮೌಲ್ಯ ಮತ್ತು ಲೇಬಲ್ ಅನ್ನು ಹೋಲಿಸಿ
- **ಮೌಲ್ಯ ಮತ್ತು ಟಿಪ್ಪಣಿ** - ಮೌಲ್ಯ ಮತ್ತು ಟಿಪ್ಪಣಿಯನ್ನು ಹೋಲಿಸಿ
- **ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿ** - ಲೇಬಲ್ ಮತ್ತು ಟಿಪ್ಪಣಿಯನ್ನು ಹೋಲಿಸಿ
		]],
		["kk"] = [[
Таңдалған өрістерге негізделіп, қайталанатын деректер нүктелерін сүзеді. Деректер нүктелері кері хронологиялық ретпен қаралады, сондықтан әрбір бірегей тіркесім үшін ең соңғы деректер нүктесі сақталады.

- **Барлық өрістер** - Мәнді, белгіні және ескертпені салыстыру
- **Тек мән** - Тек мәнді салыстыру
- **Тек белгі** - Тек белгіні салыстыру
- **Тек ескертпе** - Тек ескертпені салыстыру
- **Мән және белгі** - Мән мен белгіні салыстыру
- **Мән және ескертпе** - Мән мен ескертпені салыстыру
- **Белгі және ескертпе** - Белгі мен ескертпені салыстыру
		]],
		["km"] = [[
ច្រោះចេញចំណុចទិន្នន័យស្ទួន ដោយផ្អែកលើវាលដែលបានជ្រើសរើស។ ចំណុចទិន្នន័យត្រូវបានដំណើរការតាមលំដាប់ពេលវេលាបញ្ច្រាស ដូច្នេះចំណុចទិន្នន័យថ្មីបំផុតសម្រាប់បន្សំផ្សេងគ្នានីមួយៗនឹងត្រូវបានរក្សាទុក។

- **គ្រប់វាល** - ប្រៀបធៀបតម្លៃ ស្លាក និងចំណាំ
- **តម្លៃតែប៉ុណ្ណោះ** - ប្រៀបធៀបតម្លៃតែប៉ុណ្ណោះ
- **ស្លាកតែប៉ុណ្ណោះ** - ប្រៀបធៀបស្លាកតែប៉ុណ្ណោះ
- **ចំណាំតែប៉ុណ្ណោះ** - ប្រៀបធៀបចំណាំតែប៉ុណ្ណោះ
- **តម្លៃ និងស្លាក** - ប្រៀបធៀបតម្លៃ និងស្លាក
- **តម្លៃ និងចំណាំ** - ប្រៀបធៀបតម្លៃ និងចំណាំ
- **ស្លាក និងចំណាំ** - ប្រៀបធៀបស្លាក និងចំណាំ
		]],
		["ko"] = [[
선택한 필드를 기준으로 중복 데이터 포인트를 필터링합니다. 데이터 포인트는 시간 역순으로 처리되므로 각 고유한 조합에서 가장 최근 데이터 포인트가 유지됩니다.

- **모든 필드** - 값, 라벨, 메모 비교
- **값만** - 값만 비교
- **라벨만** - 라벨만 비교
- **메모만** - 메모만 비교
- **값 및 라벨** - 값과 라벨 비교
- **값 및 메모** - 값과 메모 비교
- **라벨 및 메모** - 라벨과 메모 비교
		]],
		["ky"] = [[
Тандалган талаалардын негизинде кайталанган маалымат чекиттерин алып салат. Маалымат чекиттери хронологиялык тескери тартипте каралат, ошондуктан ар бир кайталанбаган айкалыш үчүн эң акыркы маалымат чекити сакталат.

- **Бардык талаалар** - маанини, энбелгини жана эскертмени салыштыруу
- **Маани гана** - маанини гана салыштыруу
- **Энбелги гана** - энбелгини гана салыштыруу
- **Эскертме гана** - эскертмени гана салыштыруу
- **Маани жана энбелги** - маанини жана энбелгини салыштыруу
- **Маани жана эскертме** - маанини жана эскертмени салыштыруу
- **Энбелги жана эскертме** - энбелгини жана эскертмени салыштыруу
		]],
		["lo"] = [[
ກັ່ນຕອງຈຸດຂໍ້ມູນທີ່ຊໍ້າກັນອອກ ໂດຍອີງຕາມຟິວທີ່ເລືອກ. ຈຸດຂໍ້ມູນຈະຖືກປະມວນຜົນຕາມລໍາດັບເວລາຍ້ອນກັບ ດັ່ງນັ້ນຈຸດຂໍ້ມູນຫຼ້າສຸດຂອງແຕ່ລະການປະສົມປະສານທີ່ແຕກຕ່າງກັນຈະຖືກຮັກສາໄວ້.

- **ທຸກຟິວ** - ປຽບທຽບຄ່າ, ປ້າຍກຳກັບ ແລະ ໝາຍເຫດ
- **ສະເພາະຄ່າ** - ປຽບທຽບສະເພາະຄ່າ
- **ສະເພາະປ້າຍກຳກັບ** - ປຽບທຽບສະເພາະປ້າຍກຳກັບ
- **ສະເພາະໝາຍເຫດ** - ປຽບທຽບສະເພາະໝາຍເຫດ
- **ຄ່າ ແລະ ປ້າຍກຳກັບ** - ປຽບທຽບຄ່າ ແລະ ປ້າຍກຳກັບ
- **ຄ່າ ແລະ ໝາຍເຫດ** - ປຽບທຽບຄ່າ ແລະ ໝາຍເຫດ
- **ປ້າຍກຳກັບ ແລະ ໝາຍເຫດ** - ປຽບທຽບປ້າຍກຳກັບ ແລະ ໝາຍເຫດ
		]],
		["lv"] = [[
Filtrē ārā dublētos datu punktus, pamatojoties uz atlasītajiem laukiem. Datu punkti tiek apstrādāti apgrieztā hronoloģiskā secībā, tāpēc tiek saglabāts jaunākais datu punkts katrai atšķirīgajai kombinācijai.

- **Visi lauki** — salīdzina vērtību, etiķeti un piezīmi
- **Tikai vērtība** — salīdzina tikai vērtību
- **Tikai etiķete** — salīdzina tikai etiķeti
- **Tikai piezīme** — salīdzina tikai piezīmi
- **Vērtība un etiķete** — salīdzina vērtību un etiķeti
- **Vērtība un piezīme** — salīdzina vērtību un piezīmi
- **Etiķete un piezīme** — salīdzina etiķeti un piezīmi
		]],
		["lt"] = [[
Pašalina pasikartojančius duomenų taškus pagal pasirinktus laukus. Duomenų taškai peržiūrimi atvirkštine chronologine tvarka, todėl išsaugomas naujausias kiekvieno unikalaus derinio duomenų taškas.

- **Visi laukai** – lyginama reikšmė, etiketė ir pastaba
- **Tik reikšmė** – lyginama tik reikšmė
- **Tik etiketė** – lyginama tik etiketė
- **Tik pastaba** – lyginama tik pastaba
- **Reikšmė ir etiketė** – lyginamos reikšmė ir etiketė
- **Reikšmė ir pastaba** – lyginamos reikšmė ir pastaba
- **Etiketė ir pastaba** – lyginamos etiketė ir pastaba
		]],
		["mk"] = [[
Ги отстранува дупликатите на податоци врз основа на избраните полиња. Податочните точки се обработуваат по обратен хронолошки редослед, па се задржува најновата податочна точка за секоја различна комбинација.

- **Сите полиња** - Споредува вредност, ознака и белешка
- **Само вредност** - Споредува само вредност
- **Само ознака** - Споредува само ознака
- **Само белешка** - Споредува само белешка
- **Вредност и ознака** - Споредува вредност и ознака
- **Вредност и белешка** - Споредува вредност и белешка
- **Ознака и белешка** - Споредува ознака и белешка
		]],
		["ms"] = [[
Menapis titik data pendua berdasarkan medan yang dipilih. Titik data dilalui dalam susunan kronologi terbalik, jadi titik data paling terkini bagi setiap gabungan berbeza dikekalkan.

- **Semua Medan** - Bandingkan nilai, label dan nota
- **Nilai Sahaja** - Bandingkan nilai sahaja
- **Label Sahaja** - Bandingkan label sahaja
- **Nota Sahaja** - Bandingkan nota sahaja
- **Nilai dan Label** - Bandingkan nilai dan label
- **Nilai dan Nota** - Bandingkan nilai dan nota
- **Label dan Nota** - Bandingkan label dan nota
		]],
		["ml"] = [[
തിരഞ്ഞെടുത്ത ഫീൽഡുകളെ അടിസ്ഥാനമാക്കി ആവർത്തിച്ചുള്ള ഡാറ്റാ പോയിന്റുകൾ നീക്കം ചെയ്യുന്നു. ഡാറ്റാ പോയിന്റുകൾ ഏറ്റവും പുതിയതിൽ നിന്ന് പഴയതിലേക്കുള്ള ക്രമത്തിൽ പരിശോധിക്കുന്നതിനാൽ, ഓരോ വ്യത്യസ്ത സംയോജനത്തിന്റെയും ഏറ്റവും പുതിയ ഡാറ്റാ പോയിന്റ് നിലനിർത്തുന്നു.

- **എല്ലാ ഫീൽഡുകളും** - മൂല്യം, ലേബൽ, കുറിപ്പ് എന്നിവ താരതമ്യം ചെയ്യുക
- **മൂല്യം മാത്രം** - മൂല്യം മാത്രം താരതമ്യം ചെയ്യുക
- **ലേബൽ മാത്രം** - ലേബൽ മാത്രം താരതമ്യം ചെയ്യുക
- **കുറിപ്പ് മാത്രം** - കുറിപ്പ് മാത്രം താരതമ്യം ചെയ്യുക
- **മൂല്യവും ലേബലും** - മൂല്യവും ലേബലും താരതമ്യം ചെയ്യുക
- **മൂല്യവും കുറിപ്പും** - മൂല്യവും കുറിപ്പും താരതമ്യം ചെയ്യുക
- **ലേബലും കുറിപ്പും** - ലേബലും കുറിപ്പും താരതമ്യം ചെയ്യുക
		]],
		["mr"] = [[
निवडलेल्या फील्डच्या आधारे डुप्लिकेट डेटा पॉइंट्स वगळते. डेटा पॉइंट्स उलट कालानुक्रमाने तपासले जातात, त्यामुळे प्रत्येक भिन्न संयोगासाठी सर्वात अलीकडील डेटा पॉइंट ठेवला जातो.

- **सर्व फील्ड** - मूल्य, लेबल आणि नोंद यांची तुलना करा
- **फक्त मूल्य** - फक्त मूल्याची तुलना करा
- **फक्त लेबल** - फक्त लेबलची तुलना करा
- **फक्त नोंद** - फक्त नोंदीची तुलना करा
- **मूल्य आणि लेबल** - मूल्य आणि लेबलची तुलना करा
- **मूल्य आणि नोंद** - मूल्य आणि नोंदीची तुलना करा
- **लेबल आणि नोंद** - लेबल आणि नोंदीची तुलना करा
		]],
		["mn"] = [[
Сонгосон талбарууд дээр үндэслэн давхардсан өгөгдлийн цэгүүдийг шүүнэ. Өгөгдлийн цэгүүдийг урвуу он цагийн дарааллаар боловсруулдаг тул ялгаатай хослол бүрийн хамгийн сүүлийн өгөгдлийн цэгийг хадгална.

- **Бүх талбар** - Утга, шошго болон тэмдэглэлийг харьцуулах
- **Зөвхөн утга** - Зөвхөн утгыг харьцуулах
- **Зөвхөн шошго** - Зөвхөн шошгыг харьцуулах
- **Зөвхөн тэмдэглэл** - Зөвхөн тэмдэглэлийг харьцуулах
- **Утга ба шошго** - Утга болон шошгыг харьцуулах
- **Утга ба тэмдэглэл** - Утга болон тэмдэглэлийг харьцуулах
- **Шошго ба тэмдэглэл** - Шошго болон тэмдэглэлийг харьцуулах
		]],
		["ne"] = [[
चयन गरिएका क्षेत्रहरूका आधारमा दोहोरिएका डेटा बिन्दुहरू हटाउँछ। डेटा बिन्दुहरू उल्टो कालानुक्रमिक क्रममा प्रशोधन गरिन्छन्, त्यसैले प्रत्येक विशिष्ट संयोजनका लागि सबैभन्दा पछिल्लो डेटा बिन्दु राखिन्छ।

- **सबै क्षेत्रहरू** - मान, लेबल र नोट तुलना गर्नुहोस्
- **मान मात्र** - मान मात्र तुलना गर्नुहोस्
- **लेबल मात्र** - लेबल मात्र तुलना गर्नुहोस्
- **नोट मात्र** - नोट मात्र तुलना गर्नुहोस्
- **मान र लेबल** - मान र लेबल तुलना गर्नुहोस्
- **मान र नोट** - मान र नोट तुलना गर्नुहोस्
- **लेबल र नोट** - लेबल र नोट तुलना गर्नुहोस्
		]],
		["no"] = [[
Filtrerer bort dupliserte datapunkter basert på de valgte feltene. Datapunktene gås gjennom i omvendt kronologisk rekkefølge, slik at det nyeste datapunktet for hver unike kombinasjon beholdes.

- **Alle felt** – Sammenlign verdi, etikett og notat
- **Bare verdi** – Sammenlign bare verdi
- **Bare etikett** – Sammenlign bare etikett
- **Bare notat** – Sammenlign bare notat
- **Verdi og etikett** – Sammenlign verdi og etikett
- **Verdi og notat** – Sammenlign verdi og notat
- **Etikett og notat** – Sammenlign etikett og notat
		]],
		["pl"] = [[
Usuwa zduplikowane punkty danych na podstawie wybranych pól. Punkty danych są przetwarzane w odwrotnej kolejności chronologicznej, dlatego zachowywany jest najnowszy punkt danych dla każdej unikalnej kombinacji.

- **Wszystkie pola** - Porównuj wartość, etykietę i notatkę
- **Tylko wartość** - Porównuj tylko wartość
- **Tylko etykietę** - Porównuj tylko etykietę
- **Tylko notatkę** - Porównuj tylko notatkę
- **Wartość i etykieta** - Porównuj wartość i etykietę
- **Wartość i notatka** - Porównuj wartość i notatkę
- **Etykieta i notatka** - Porównuj etykietę i notatkę
		]],
		["pt"] = [[
Filtra los puntos de datos duplicados según los campos seleccionados. Los puntos de datos se recorren en orden cronológico inverso, por lo que se conserva el punto de datos más reciente para cada combinación distinta.

- **Todos los campos** - Compara el valor, la etiqueta y la nota
- **Solo el valor** - Compara solo el valor
- **Solo la etiqueta** - Compara solo la etiqueta
- **Solo la nota** - Compara solo la nota
- **Valor y etiqueta** - Compara el valor y la etiqueta
- **Valor y nota** - Compara el valor y la nota
- **Etiqueta y nota** - Compara la etiqueta y la nota
		]],
		["pa"] = [[
ਚੁਣੇ ਹੋਏ ਖੇਤਰਾਂ ਦੇ ਆਧਾਰ ’ਤੇ ਦੁਹਰਾਏ ਗਏ ਡਾਟਾ ਬਿੰਦੂਆਂ ਨੂੰ ਫਿਲਟਰ ਕਰਦਾ ਹੈ। ਡਾਟਾ ਬਿੰਦੂਆਂ ਨੂੰ ਉਲਟ ਕਾਲਕ੍ਰਮ ਵਿੱਚ ਦੇਖਿਆ ਜਾਂਦਾ ਹੈ, ਇਸ ਲਈ ਹਰ ਵਿਲੱਖਣ ਸੰਯੋਜਨ ਲਈ ਸਭ ਤੋਂ ਨਵਾਂ ਡਾਟਾ ਬਿੰਦੂ ਰੱਖਿਆ ਜਾਂਦਾ ਹੈ।

- **ਸਾਰੇ ਖੇਤਰ** - ਮੁੱਲ, ਲੇਬਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਮੁੱਲ** - ਸਿਰਫ਼ ਮੁੱਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਲੇਬਲ** - ਸਿਰਫ਼ ਲੇਬਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਸਿਰਫ਼ ਨੋਟ** - ਸਿਰਫ਼ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਮੁੱਲ ਅਤੇ ਲੇਬਲ** - ਮੁੱਲ ਅਤੇ ਲੇਬਲ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਮੁੱਲ ਅਤੇ ਨੋਟ** - ਮੁੱਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
- **ਲੇਬਲ ਅਤੇ ਨੋਟ** - ਲੇਬਲ ਅਤੇ ਨੋਟ ਦੀ ਤੁਲਨਾ ਕਰੋ
		]],
		["ro"] = [[
Filtrează punctele de date duplicate pe baza câmpurilor selectate. Punctele de date sunt parcurse în ordine cronologică inversă, astfel încât este păstrat cel mai recent punct de date pentru fiecare combinație distinctă.

- **Toate câmpurile** - Compară valoarea, eticheta și nota
- **Doar valoarea** - Compară doar valoarea
- **Doar eticheta** - Compară doar eticheta
- **Doar nota** - Compară doar nota
- **Valoarea și eticheta** - Compară valoarea și eticheta
- **Valoarea și nota** - Compară valoarea și nota
- **Eticheta și nota** - Compară eticheta și nota
		]],
		["rm"] = [[
Elimina ils puncts da datas duplicats tenor ils champs selecziunads. Ils puncts da datas vegnan traversads en successiun cronologica inversa, uschia che per mintga cumbinaziun distincta vegn conservà il punct da datas il pli recent.

- **Tuts ils champs** - Cumparar la valur, l’etichetta e la nota
- **Mo la valur** - Cumparar mo la valur
- **Mo l’etichetta** - Cumparar mo l’etichetta
- **Mo la nota** - Cumparar mo la nota
- **Valur ed etichetta** - Cumparar la valur e l’etichetta
- **Valur e nota** - Cumparar la valur e la nota
- **Etichetta e nota** - Cumparar l’etichetta e la nota
		]],
		["ru"] = [[
Отфильтровывает повторяющиеся точки данных на основе выбранных полей. Точки данных просматриваются в обратном хронологическом порядке, поэтому сохраняется самая последняя точка данных для каждой уникальной комбинации.

- **Все поля** — сравнивать значение, метку и примечание
- **Только значение** — сравнивать только значение
- **Только метку** — сравнивать только метку
- **Только примечание** — сравнивать только примечание
- **Значение и метку** — сравнивать значение и метку
- **Значение и примечание** — сравнивать значение и примечание
- **Метка и примечание** — сравнивать метку и примечание
		]],
		["sr"] = [[
Filtrira duplikate tačaka podataka na osnovu izabranih polja. Tačke podataka se obrađuju obrnutim hronološkim redosledom, tako da se zadržava najnovija tačka podataka za svaku jedinstvenu kombinaciju.

- **Sva polja** - Upoređuju se vrednost, oznaka i beleška
- **Samo vrednost** - Upoređuje se samo vrednost
- **Samo oznaka** - Upoređuje se samo oznaka
- **Samo beleška** - Upoređuje se samo beleška
- **Vrednost i oznaka** - Upoređuju se vrednost i oznaka
- **Vrednost i beleška** - Upoređuju se vrednost i beleška
- **Oznaka i beleška** - Upoređuju se oznaka i beleška
		]],
		["si"] = [[
තෝරාගත් ක්ෂේත්‍ර මත පදනම්ව අනුපිටපත් දත්ත ලක්ෂ්‍ය ඉවත් කරයි. දත්ත ලක්ෂ්‍ය නවතම සිට පැරණිතම අනුපිළිවෙළට සලකා බලන බැවින්, එක් එක් වෙනස් සංයෝජනය සඳහා නවතම දත්ත ලක්ෂ්‍යය තබා ගනී.

- **සියලු ක්ෂේත්‍ර** - අගය, ලේබලය සහ සටහන සසඳන්න
- **අගය පමණි** - අගය පමණක් සසඳන්න
- **ලේබලය පමණි** - ලේබලය පමණක් සසඳන්න
- **සටහන පමණි** - සටහන පමණක් සසඳන්න
- **අගය සහ ලේබලය** - අගය සහ ලේබලය සසඳන්න
- **අගය සහ සටහන** - අගය සහ සටහන සසඳන්න
- **ලේබලය සහ සටහන** - ලේබලය සහ සටහන සසඳන්න
		]],
		["sk"] = [[
Odfiltruje duplicitné dátové body na základe vybraných polí. Dátové body sa prechádzajú v opačnom chronologickom poradí, takže pre každú jedinečnú kombináciu sa zachová najnovší dátový bod.

- **Všetky polia** – Porovnáva hodnotu, označenie a poznámku
- **Iba hodnota** – Porovnáva iba hodnotu
- **Iba označenie** – Porovnáva iba označenie
- **Iba poznámka** – Porovnáva iba poznámku
- **Hodnota a označenie** – Porovnáva hodnotu a označenie
- **Hodnota a poznámka** – Porovnáva hodnotu a poznámku
- **Označenie a poznámka** – Porovnáva označenie a poznámku
		]],
		["sl"] = [[
Odstrani podvojene podatkovne točke na podlagi izbranih polj. Podatkovne točke se obdelajo v obratnem kronološkem vrstnem redu, zato se za vsako različno kombinacijo ohrani najnovejša podatkovna točka.

- **Vsa polja** - Primerjaj vrednost, oznako in opombo
- **Samo vrednost** - Primerjaj samo vrednost
- **Samo oznako** - Primerjaj samo oznako
- **Samo opombo** - Primerjaj samo opombo
- **Vrednost in oznaka** - Primerjaj vrednost in oznako
- **Vrednost in opomba** - Primerjaj vrednost in opombo
- **Oznaka in opomba** - Primerjaj oznako in opombo
		]],
		["es"] = [[
Filtra los puntos de datos duplicados según los campos seleccionados. Los puntos de datos se recorren en orden cronológico inverso, por lo que se conserva el punto de datos más reciente de cada combinación distinta.

- **Todos los campos** - Comparar valor, etiqueta y nota
- **Solo valor** - Comparar solo el valor
- **Solo etiqueta** - Comparar solo la etiqueta
- **Solo nota** - Comparar solo la nota
- **Valor y etiqueta** - Comparar valor y etiqueta
- **Valor y nota** - Comparar valor y nota
- **Etiqueta y nota** - Comparar etiqueta y nota
		]],
		["sw"] = [[
Huondoa vipengele vya data vinavyojirudia kulingana na sehemu zilizochaguliwa. Vipengele vya data hupitiwa kwa mpangilio wa kuanzia wa hivi karibuni kurudi nyuma, hivyo kipengele cha data cha hivi karibuni kwa kila mchanganyiko wa kipekee huhifadhiwa.

- **Sehemu Zote** - Linganisha thamani, lebo na dokezo
- **Thamani Pekee** - Linganisha thamani pekee
- **Lebo Pekee** - Linganisha lebo pekee
- **Dokezo Pekee** - Linganisha dokezo pekee
- **Thamani na Lebo** - Linganisha thamani na lebo
- **Thamani na Dokezo** - Linganisha thamani na dokezo
- **Lebo na Dokezo** - Linganisha lebo na dokezo
		]],
		["sv"] = [[
Filtrerar bort dubbletter av datapunkter baserat på de valda fälten. Datapunkterna gås igenom i omvänd kronologisk ordning, så att den senaste datapunkten för varje unik kombination behålls.

- **Alla fält** – Jämför värde, etikett och anteckning
- **Endast värde** – Jämför endast värde
- **Endast etikett** – Jämför endast etikett
- **Endast anteckning** – Jämför endast anteckning
- **Värde och etikett** – Jämför värde och etikett
- **Värde och anteckning** – Jämför värde och anteckning
- **Etikett och anteckning** – Jämför etikett och anteckning
		]],
		["ta"] = [[
தேர்ந்தெடுக்கப்பட்ட புலங்களின் அடிப்படையில் நகல் தரவுப் புள்ளிகளை வடிகட்டுகிறது. தரவுப் புள்ளிகள் காலவரிசையில் பின்னோக்கி ஆய்வு செய்யப்படுவதால், ஒவ்வொரு தனித்துவமான சேர்க்கைக்கும் சமீபத்திய தரவுப் புள்ளி வைக்கப்படுகிறது.

- **அனைத்து புலங்களும்** - மதிப்பு, லேபிள் மற்றும் குறிப்பை ஒப்பிடு
- **மதிப்பு மட்டும்** - மதிப்பை மட்டும் ஒப்பிடு
- **லேபிள் மட்டும்** - லேபிளை மட்டும் ஒப்பிடு
- **குறிப்பு மட்டும்** - குறிப்பை மட்டும் ஒப்பிடு
- **மதிப்பு மற்றும் லேபிள்** - மதிப்பு மற்றும் லேபிளை ஒப்பிடு
- **மதிப்பு மற்றும் குறிப்பு** - மதிப்பு மற்றும் குறிப்பை ஒப்பிடு
- **லேபிள் மற்றும் குறிப்பு** - லேபிள் மற்றும் குறிப்பை ஒப்பிடு
		]],
		["te"] = [[
ఎంచుకున్న ఫీల్డ్‌ల ఆధారంగా నకిలీ డేటా పాయింట్‌లను తొలగిస్తుంది. డేటా పాయింట్‌లు తాజా నుండి పాత క్రమంలో వెనుకకు పరిశీలించబడతాయి, కాబట్టి ప్రతి విభిన్న కలయికకు సంబంధించిన అత్యంత తాజా డేటా పాయింట్ ఉంచబడుతుంది.

- **అన్ని ఫీల్డ్‌లు** - విలువ, లేబుల్ మరియు గమనికను పోల్చండి
- **విలువ మాత్రమే** - విలువను మాత్రమే పోల్చండి
- **లేబుల్ మాత్రమే** - లేబుల్‌ను మాత్రమే పోల్చండి
- **గమనిక మాత్రమే** - గమనికను మాత్రమే పోల్చండి
- **విలువ మరియు లేబుల్** - విలువ మరియు లేబుల్‌ను పోల్చండి
- **విలువ మరియు గమనిక** - విలువ మరియు గమనికను పోల్చండి
- **లేబుల్ మరియు గమనిక** - లేబుల్ మరియు గమనికను పోల్చండి
		]],
		["th"] = [[
กรองจุดข้อมูลที่ซ้ำกันออกโดยอิงตามฟิลด์ที่เลือก ระบบจะประมวลผลจุดข้อมูลจากใหม่ไปเก่า ดังนั้นจึงเก็บจุดข้อมูลล่าสุดของชุดค่าผสมที่ไม่ซ้ำกันแต่ละชุดไว้

- **ทุกฟิลด์** - เปรียบเทียบค่า ป้ายกำกับ และหมายเหตุ
- **ค่าเท่านั้น** - เปรียบเทียบเฉพาะค่า
- **ป้ายกำกับเท่านั้น** - เปรียบเทียบเฉพาะป้ายกำกับ
- **หมายเหตุเท่านั้น** - เปรียบเทียบเฉพาะหมายเหตุ
- **ค่าและป้ายกำกับ** - เปรียบเทียบค่าและป้ายกำกับ
- **ค่าและหมายเหตุ** - เปรียบเทียบค่าและหมายเหตุ
- **ป้ายกำกับและหมายเหตุ** - เปรียบเทียบป้ายกำกับและหมายเหตุ
		]],
		["tr"] = [[
Seçilen alanlara göre yinelenen veri noktalarını filtreler. Veri noktaları ters kronolojik sırayla taranır; böylece her benzersiz kombinasyon için en son veri noktası tutulur.

- **Tüm Alanlar** - Değer, etiket ve not karşılaştırılır
- **Yalnızca Değer** - Yalnızca değer karşılaştırılır
- **Yalnızca Etiket** - Yalnızca etiket karşılaştırılır
- **Yalnızca Not** - Yalnızca not karşılaştırılır
- **Değer ve Etiket** - Değer ve etiket karşılaştırılır
- **Değer ve Not** - Değer ve not karşılaştırılır
- **Etiket ve Not** - Etiket ve not karşılaştırılır
		]],
		["uk"] = [[
Відфільтровує дублікати точок даних на основі вибраних полів. Точки даних переглядаються у зворотному хронологічному порядку, тому для кожної унікальної комбінації зберігається найновіша точка даних.

- **Усі поля** — порівнювати значення, мітку й примітку
- **Лише значення** — порівнювати лише значення
- **Лише мітку** — порівнювати лише мітку
- **Лише примітку** — порівнювати лише примітку
- **Значення й мітку** — порівнювати значення та мітку
- **Значення й примітку** — порівнювати значення та примітку
- **Мітку й примітку** — порівнювати мітку та примітку
		]],
		["vi"] = [[
Lọc bỏ các điểm dữ liệu trùng lặp dựa trên các trường đã chọn. Các điểm dữ liệu được duyệt theo thứ tự thời gian ngược, vì vậy điểm dữ liệu gần đây nhất cho mỗi tổ hợp khác biệt sẽ được giữ lại.

- **Tất cả các trường** - So sánh giá trị, nhãn và ghi chú
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

	generator = function(source, config)
		local compare_by = config and config.compare_by or "_all_fields"
		local seen = {}

		return function()
			while true do
				local data_point = source.dp()
				if not data_point then
					return nil
				end

				local keys
				if compare_by == "_value_only" then
					keys = { normalize_key(data_point.value) }
				elseif compare_by == "_label_only" then
					keys = { normalize_key(data_point.label) }
				elseif compare_by == "_note_only" then
					keys = { normalize_key(data_point.note) }
				elseif compare_by == "_value_and_label" then
					keys = { normalize_key(data_point.value), normalize_key(data_point.label) }
				elseif compare_by == "_value_and_note" then
					keys = { normalize_key(data_point.value), normalize_key(data_point.note) }
				elseif compare_by == "_label_and_note" then
					keys = { normalize_key(data_point.label), normalize_key(data_point.note) }
				else
					keys = {
						normalize_key(data_point.value),
						normalize_key(data_point.label),
						normalize_key(data_point.note),
					}
				end

				if not has_been_seen(seen, keys) then
					return data_point
				end
			end
		end
	end,
}
