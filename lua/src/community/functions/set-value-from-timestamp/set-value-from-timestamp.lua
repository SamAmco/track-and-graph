-- Sets each data point's value to a chosen component of its timestamp

local core = require("tng.core")
local enum = require("tng.config").enum

local extractors = {
	["_year_value"] = function(date)
		return date.year
	end,
	["_month_of_year"] = function(date)
		return date.month
	end,
	["_day_of_month"] = function(date)
		return date.day
	end,
	["_day_of_week"] = function(date)
		return date.wday
	end,
	["_hour_of_day"] = function(date)
		return date.hour or 0
	end,
	["_minute_of_hour"] = function(date)
		return date.min or 0
	end,
	["_second_of_minute"] = function(date)
		return date.sec or 0
	end,
	["_duration_since_midnight"] = function(date)
		return (date.hour or 0) * 3600 + (date.min or 0) * 60 + (date.sec or 0)
	end,
}

return {
	id = "set-value-from-timestamp",
	version = "1.0.2",
	inputCount = 1,
	categories = { "_time" },
	title = {
		["en"] = "Set Value from Timestamp",
		["af"] = "Stel Waarde vanaf Tydstempel",
		["sq"] = "Vendos vlerën nga vula kohore",
		["am"] = "ዋጋን ከጊዜ ማህተም አዘጋጅ",
		["hy"] = "Սահմանել արժեքը ժամանակացույցից",
		["az"] = "Qiyməti zaman damğasından təyin et",
		["bn"] = "টাইমস্ট্যাম্প থেকে মান সেট করুন",
		["eu"] = "Ezarri balioa denbora-zigilutik",
		["be"] = "Задаць значэнне з меткі часу",
		["bg"] = "Задаване на стойност от времеви отпечатък",
		["my"] = "Timestamp မှ တန်ဖိုးသတ်မှတ်ရန်",
		["ca"] = "Estableix el valor a partir de la marca de temps",
		["zh-Hans"] = "根据时间戳设置值",
		["zh-Hant"] = "從時間戳記設定值",
		["hr"] = "Postavi vrijednost iz vremenske oznake",
		["cs"] = "Nastavit hodnotu z časového razítka",
		["da"] = "Angiv værdi fra tidsstempel",
		["nl"] = "Waarde instellen op basis van tijdstempel",
		["et"] = "Määra väärtus ajatempli põhjal",
		["fil"] = "Itakda ang Halaga mula sa Timestamp",
		["fi"] = "Aseta arvo aikaleimasta",
		["fr"] = "Définir la valeur depuis l’horodatage",
		["gl"] = "Establecer o valor desde a marca temporal",
		["ka"] = "მნიშვნელობის დაყენება დროის ნიშნულიდან",
		["de"] = "Wert aus Zeitstempel festlegen",
		["el"] = "Ορισμός τιμής από χρονική σήμανση",
		["gu"] = "ટાઇમસ્ટેમ્પમાંથી મૂલ્ય સેટ કરો",
		["hi"] = "टाइमस्टैम्प से मान सेट करें",
		["hu"] = "Érték beállítása időbélyegből",
		["is"] = "Stilla gildi út frá tímamerki",
		["id"] = "Atur Nilai dari Stempel Waktu",
		["it"] = "Imposta il valore dal timestamp",
		["ja"] = "タイムスタンプから値を設定",
		["kn"] = "ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್‌ನಿಂದ ಮೌಲ್ಯ ಹೊಂದಿಸಿ",
		["kk"] = "Мәнді уақыт белгісінен орнату",
		["km"] = "កំណត់តម្លៃពីត្រាពេលវេលា",
		["ko"] = "타임스탬프에서 값 설정",
		["ky"] = "Маанини убакыт белгисинен коюу",
		["lo"] = "ກຳນົດຄ່າຈາກເວລາ",
		["lv"] = "Iestatīt vērtību no laika zīmoga",
		["lt"] = "Nustatyti reikšmę iš laiko žymos",
		["mk"] = "Постави вредност од временски печат",
		["ms"] = "Tetapkan Nilai daripada Cap Masa",
		["ml"] = "Timestamp-ൽ നിന്ന് മൂല്യം സജ്ജീകരിക്കുക",
		["mr"] = "टाइमस्टॅम्पमधून मूल्य सेट करा",
		["mn"] = "Цагийн тэмдгээс утга тохируулах",
		["ne"] = "टाइमस्ट्याम्पबाट मान सेट गर्नुहोस्",
		["no"] = "Angi verdi fra tidsstempel",
		["pl"] = "Ustaw wartość ze znacznika czasu",
		["pt"] = "Definir valor a partir do carimbo de data/hora",
		["pa"] = "ਟਾਈਮਸਟੈਂਪ ਤੋਂ ਮੁੱਲ ਸੈੱਟ ਕਰੋ",
		["ro"] = "Setează valoarea din marcajul temporal",
		["rm"] = "Definir la valur dal timestamp",
		["ru"] = "Задать значение по временной метке",
		["sr"] = "Postavi vrednost iz vremenske oznake",
		["si"] = "Timestamp වෙතින් අගය සකසන්න",
		["sk"] = "Nastaviť hodnotu z časovej pečiatky",
		["sl"] = "Nastavi vrednost iz časovnega žiga",
		["es"] = "Establecer valor a partir de la marca de tiempo",
		["sw"] = "Weka Thamani kutoka kwa Muhuri wa Muda",
		["sv"] = "Ställ in värde från tidsstämpel",
		["ta"] = "நேரமுத்திரையிலிருந்து மதிப்பை அமை",
		["te"] = "టైమ్‌స్టాంప్ నుండి విలువను సెట్ చేయి",
		["th"] = "ตั้งค่าจากเวลาประทับ",
		["tr"] = "Zaman Damgasından Değer Ayarla",
		["uk"] = "Встановити значення з часової мітки",
		["vi"] = "Đặt giá trị từ dấu thời gian",
	},
	description = {
		["en"] = "Sets each data point's value to a chosen component of its timestamp.",
		["af"] = "Stel elke datapunt se waarde op ’n gekose komponent van sy tydstempel.",
		["sq"] = "Vendos vlerën e çdo pike të të dhënave në një përbërës të zgjedhur të vulës së saj kohore.",
		["am"] = "የእያንዳንዱን የውሂብ ነጥብ ዋጋ የጊዜ ማህተሙ ከሚያካትተው የተመረጠ ክፍል ጋር ያዛምዳል።",
		["hy"] = "Յուրաքանչյուր տվյալակետի արժեքը սահմանում է դրա ժամանակացույցի ընտրված բաղադրիչով։",
		["az"] = "Hər məlumat nöqtəsinin qiymətini onun zaman damğasının seçilmiş komponentinə təyin edir.",
		["bn"] = "প্রতিটি ডেটা পয়েন্টের মানকে তার টাইমস্ট্যাম্পের নির্বাচিত একটি উপাদানে সেট করে।",
		["eu"] = "Datu-puntu bakoitzaren balioa haren denbora-zigiluaren aukeratutako osagai batera ezartzen du.",
		["be"] = "Задае значэнне кожнай кропкі даных роўным выбранаму кампаненту яе меткі часу.",
		["bg"] = "Задава стойността на всяка точка от данни на избран компонент от нейния времеви отпечатък.",
		["my"] = "ဒေတာအမှတ်တစ်ခုစီ၏ တန်ဖိုးကို ၎င်း၏ timestamp မှ ရွေးချယ်ထားသော အစိတ်အပိုင်းအဖြစ် သတ်မှတ်သည်။",
		["ca"] = "Estableix el valor de cada punt de dades en un component seleccionat de la seva marca de temps.",
		["zh-Hans"] = "将每个数据点的值设置为其时间戳中选定的组成部分。",
		["zh-Hant"] = "將每個資料點的值設為其時間戳記中選定的部分。",
		["hr"] = "Postavlja vrijednost svake podatkovne točke na odabranu komponentu njezine vremenske oznake.",
		["cs"] = "Nastaví hodnotu každého datového bodu na vybranou složku jeho časového razítka.",
		["da"] = "Sætter hvert datapunkts værdi til en valgt del af dets tidsstempel.",
		["nl"] = "Stelt de waarde van elk gegevenspunt in op een gekozen onderdeel van de tijdstempel.",
		["et"] = "Määrab iga andmepunkti väärtuseks selle ajatempli valitud komponendi.",
		["fil"] = "Itinatakda ang halaga ng bawat data point sa piniling bahagi ng timestamp nito.",
		["fi"] = "Asettaa kunkin datapisteen arvoksi valitun aikaleiman osan.",
		["fr"] = "Définit la valeur de chaque point de données sur une composante choisie de son horodatage.",
		["gl"] = "Establece o valor de cada punto de datos nun compoñente seleccionado da súa marca temporal.",
		["ka"] = "თითოეული მონაცემის წერტილის მნიშვნელობას მისი დროის ნიშნულის არჩეულ კომპონენტზე აყენებს.",
		["de"] = "Setzt den Wert jedes Datenpunkts auf eine ausgewählte Komponente seines Zeitstempels.",
		["el"] = "Ορίζει την τιμή κάθε σημείου δεδομένων σε ένα επιλεγμένο στοιχείο της χρονικής σήμανσής του.",
		["gu"] = "દરેક ડેટા પોઇન્ટનું મૂલ્ય તેના ટાઇમસ્ટેમ્પના પસંદ કરેલા ઘટક પર સેટ કરે છે.",
		["hi"] = "हर डेटा पॉइंट के मान को उसके टाइमस्टैम्प के चुने गए घटक पर सेट करता है।",
		["hu"] = "Az egyes adatpontok értékét az időbélyegük kiválasztott összetevőjére állítja.",
		["is"] = "Stillir gildi hvers gagnapunkts á valinn hluta tímamerkis hans.",
		["id"] = "Mengatur nilai setiap titik data ke komponen tertentu dari stempel waktunya.",
		["it"] = "Imposta il valore di ogni punto dati su un componente scelto del relativo timestamp.",
		["ja"] = "各データポイントの値を、タイムスタンプの選択した要素に設定します。",
		["kn"] = "ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯವನ್ನು ಅದರ ಟೈಮ್‌ಸ್ಟ್ಯಾಂಪ್‌ನ ಆಯ್ಕೆಮಾಡಿದ ಘಟಕಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ.",
		["kk"] = "Әр дерек нүктесінің мәнін оның уақыт белгісінің таңдалған құрамдасына орнатады.",
		["km"] = "កំណត់តម្លៃរបស់ចំណុចទិន្នន័យនីមួយៗទៅជាផ្នែកដែលបានជ្រើសរើសនៃត្រាពេលវេលារបស់វា។",
		["ko"] = "각 데이터 포인트의 값을 타임스탬프에서 선택한 구성 요소로 설정합니다.",
		["ky"] = "Ар бир маалымат чекитинин маанисин анын убакыт белгисинин тандалган бөлүгүнө өзгөртөт.",
		["lo"] = "ກຳນົດຄ່າຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດເປັນສ່ວນປະກອບທີ່ເລືອກຈາກເວລາຂອງຈຸດນັ້ນ.",
		["lv"] = "Iestata katra datu punkta vērtību uz izvēlēto tā laika zīmoga komponenti.",
		["lt"] = "Nustato kiekvieno duomenų taško reikšmę pagal pasirinktą jo laiko žymos komponentą.",
		["mk"] = "Ја поставува вредноста на секоја точка на податоци на избрана компонента од нејзиниот временски печат.",
		["ms"] = "Menetapkan nilai setiap titik data kepada komponen cap masa yang dipilih.",
		["ml"] = "ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യം അതിന്റെ timestamp-ന്റെ തിരഞ്ഞെടുത്ത ഘടകമായി സജ്ജീകരിക്കുന്നു.",
		["mr"] = "प्रत्येक डेटा पॉइंटचे मूल्य त्याच्या टाइमस्टॅम्पमधील निवडलेल्या घटकावर सेट करते.",
		["mn"] = "Өгөгдлийн цэг бүрийн утгыг цагийн тэмдгийн сонгосон бүрэлдэхүүнээр тохируулна.",
		["ne"] = "प्रत्येक डेटा बिन्दुको मानलाई यसको टाइमस्ट्याम्पको चयन गरिएको भागमा सेट गर्छ।",
		["no"] = "Setter verdien til hvert datapunkt til en valgt del av tidsstempelet.",
		["pl"] = "Ustawia wartość każdego punktu danych na wybrany składnik jego znacznika czasu.",
		["pt"] = "Define o valor de cada ponto de dados como um componente escolhido do respetivo carimbo de data/hora.",
		["pa"] = "ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦਾ ਮੁੱਲ ਇਸਦੇ ਟਾਈਮਸਟੈਂਪ ਦੇ ਚੁਣੇ ਗਏ ਹਿੱਸੇ ’ਤੇ ਸੈੱਟ ਕਰਦਾ ਹੈ।",
		["ro"] = "Setează valoarea fiecărui punct de date la o componentă aleasă a marcajului său temporal.",
		["rm"] = "Imposta il valur da mintga punct da datas al cumpunent tschernì da ses timestamp.",
		["ru"] = "Задаёт значению каждой точки данных выбранный компонент её временной метки.",
		["sr"] = "Postavlja vrednost svake tačke podataka na izabranu komponentu njene vremenske oznake.",
		["si"] = "සෑම දත්ත ලක්ෂ්‍යයකම අගය එහි වේලා මුද්‍රාවේ තෝරාගත් සංරචකයකට සකසයි.",
		["sk"] = "Nastaví hodnotu každého údajového bodu na vybranú zložku jeho časovej pečiatky.",
		["sl"] = "Nastavi vrednost vsake podatkovne točke na izbrano sestavino njenega časovnega žiga.",
		["es"] = "Establece el valor de cada punto de datos en un componente seleccionado de su marca de tiempo.",
		["sw"] = "Huamua thamani ya kila nukta ya data kuwa sehemu iliyochaguliwa ya muhuri wake wa muda.",
		["sv"] = "Ställer in varje datapunkts värde till en vald del av dess tidsstämpel.",
		["ta"] = "ஒவ்வொரு தரவுப் புள்ளியின் மதிப்பையும் அதன் நேரமுத்திரையின் தேர்ந்தெடுக்கப்பட்ட கூறாக அமைக்கிறது.",
		["te"] = "ప్రతి డేటా పాయింట్ విలువను దాని టైమ్‌స్టాంప్‌లోని ఎంచుకున్న భాగంగా సెట్ చేస్తుంది.",
		["th"] = "ตั้งค่าของจุดข้อมูลแต่ละจุดเป็นส่วนประกอบที่เลือกของเวลาประทับ",
		["tr"] = "Her veri noktasının değerini, zaman damgasının seçilen bir bileşenine ayarlar.",
		["uk"] = "Встановлює для кожної точки даних значення, що відповідає вибраному компоненту її часової мітки.",
		["vi"] = "Đặt giá trị của mỗi điểm dữ liệu thành một thành phần được chọn trong dấu thời gian của điểm đó.",
	},
	config = {
		enum {
			id = "component",
			name = "_component",
			options = {
				"_year_value",
				"_month_of_year",
				"_day_of_month",
				"_day_of_week",
				"_hour_of_day",
				"_minute_of_hour",
				"_second_of_minute",
				"_duration_since_midnight",
			},
			default = "_day_of_month",
		},
	},

	generator = function(source, config)
		local component = config and config.component or "_day_of_month"
		local extract = extractors[component]

		return function()
			local data_point = source.dp()
			if not data_point then
				return nil
			end

			local date = core.date(data_point)
			data_point.value = extract(date)

			return data_point
		end
	end,
}
