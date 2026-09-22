-- Lua Function to calculate value differences
-- Outputs the difference between each data point's value and the next one

return {
	-- Configuration metadata
	id = "value-difference",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_arithmetic", "_transform"},
	title = {
		["en"] = "Value Difference",
		["af"] = "Waardeverkil",
		["sq"] = "Diferenca e vlerës",
		["am"] = "የዋጋ ልዩነት",
		["hy"] = "Արժեքների տարբերություն",
		["az"] = "Qiymət fərqi",
		["bn"] = "মানের পার্থক্য",
		["eu"] = "Balio-aldea",
		["be"] = "Розніца значэнняў",
		["bg"] = "Разлика в стойностите",
		["my"] = "တန်ဖိုးကွာခြားချက်",
		["ca"] = "Diferència de valors",
		["zh-Hans"] = "值差",
		["zh-Hant"] = "值差異",
		["hr"] = "Razlika vrijednosti",
		["cs"] = "Rozdíl hodnot",
		["da"] = "Værdiforskel",
		["nl"] = "Waardeverschil",
		["et"] = "Väärtuste erinevus",
		["fil"] = "Pagkakaiba ng Halaga",
		["fi"] = "Arvojen erotus",
		["fr"] = "Différence de valeur",
		["gl"] = "Diferenza de valores",
		["ka"] = "მნიშვნელობათა სხვაობა",
		["de"] = "Wertdifferenz",
		["el"] = "Διαφορά τιμών",
		["gu"] = "મૂલ્યનો તફાવત",
		["hi"] = "मान का अंतर",
		["hu"] = "Értékkülönbség",
		["is"] = "Mismunur gilda",
		["id"] = "Selisih Nilai",
		["it"] = "Differenza di valore",
		["ja"] = "値の差分",
		["kn"] = "ಮೌಲ್ಯ ವ್ಯತ್ಯಾಸ",
		["kk"] = "Мән айырмасы",
		["km"] = "ភាពខុសគ្នានៃតម្លៃ",
		["ko"] = "값 차이",
		["ky"] = "Маанилердин айырмасы",
		["lo"] = "ຄວາມແຕກຕ່າງຂອງຄ່າ",
		["lv"] = "Vērtības starpība",
		["lt"] = "Reikšmių skirtumas",
		["mk"] = "Разлика во вредноста",
		["ms"] = "Perbezaan Nilai",
		["ml"] = "മൂല്യ വ്യത്യാസം",
		["mr"] = "मूल्याचा फरक",
		["mn"] = "Утгын зөрүү",
		["ne"] = "मानको अन्तर",
		["no"] = "Verdiforskjell",
		["pl"] = "Różnica wartości",
		["pt"] = "Diferença de valores",
		["pa"] = "ਮੁੱਲ ਦਾ ਅੰਤਰ",
		["ro"] = "Diferența valorilor",
		["rm"] = "Differenza da la valur",
		["ru"] = "Разница значений",
		["sr"] = "Razlika vrednosti",
		["si"] = "අගය වෙනස",
		["sk"] = "Rozdiel hodnôt",
		["sl"] = "Razlika vrednosti",
		["es"] = "Diferencia de valores",
		["sw"] = "Tofauti ya Thamani",
		["sv"] = "Värdeskillnad",
		["ta"] = "மதிப்பு வேறுபாடு",
		["te"] = "విలువ వ్యత్యాసం",
		["th"] = "ส่วนต่างของค่า",
		["tr"] = "Değer Farkı",
		["uk"] = "Різниця значень",
		["vi"] = "Chênh lệch giá trị",
	},
	description = {
		["en"] = [[
Calculates the difference between each data point's value and the next one. Each output point has its original identity with the value set to the difference.
		]],
		["af"] = [[
Bereken die verskil tussen elke datapunt se waarde en die volgende een. Elke uitvoerpunt behou sy oorspronklike identiteit, met die waarde op die verskil gestel.
		]],
		["sq"] = [[
Llogarit diferencën midis vlerës së çdo pike të të dhënave dhe pikës pasuese. Çdo pikë dalëse ruan identitetin e saj origjinal, ndërsa vlera vendoset në diferencë.
		]],
		["am"] = [[
በእያንዳንዱ የውሂብ ነጥብ ዋጋና ቀጣዩ የውሂብ ነጥብ ዋጋ መካከል ያለውን ልዩነት ያሰላል። እያንዳንዱ የውጤት ነጥብ የመጀመሪያውን መለያ ይይዛል፣ ዋጋው ግን ወደ ልዩነቱ ይቀየራል።
		]],
		["hy"] = [[
Հաշվում է յուրաքանչյուր տվյալակետի արժեքի և հաջորդի միջև տարբերությունը։ Յուրաքանչյուր ելքային կետ պահպանում է իր սկզբնական ինքնությունը, իսկ արժեքը սահմանվում է որպես տարբերություն։
		]],
		["az"] = [[
Hər məlumat nöqtəsinin qiyməti ilə növbəti məlumat nöqtəsinin qiyməti arasındakı fərqi hesablayır. Hər çıxış nöqtəsi orijinal kimliyini saxlayır və qiyməti fərqə təyin edilir.
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্টের মান এবং পরেরটির মধ্যে পার্থক্য গণনা করে। প্রতিটি আউটপুট পয়েন্টের মূল পরিচয় অপরিবর্তিত থাকে, শুধু মানটি পার্থক্যে সেট করা হয়।
		]],
		["eu"] = [[
Datu-puntu bakoitzaren eta hurrengoaren balioen arteko aldea kalkulatzen du. Irteerako puntu bakoitzak jatorrizko identitatea du, eta balioa aldearekin ezarrita.
		]],
		["be"] = [[
Вылічвае розніцу паміж значэннем кожнай кропкі даных і наступнай. Кожная выходная кропка захоўвае сваю зыходную ідэнтычнасць, а яе значэнне задаецца роўным розніцы.
		]],
		["bg"] = [[
Изчислява разликата между стойността на всяка точка от данни и следващата. Всяка изходна точка запазва оригиналната си идентичност, като стойността ѝ се задава на разликата.
		]],
		["my"] = [[
ဒေတာအမှတ်တစ်ခုစီ၏ တန်ဖိုးနှင့် နောက်တစ်ခု၏ တန်ဖိုးအကြား ကွာခြားချက်ကို တွက်ချက်သည်။ ထွက်လာသောအမှတ်တစ်ခုစီသည် မူလအမှတ်အချက်အလက်ကို ထိန်းသိမ်းထားပြီး တန်ဖိုးကို ကွာခြားချက်အဖြစ် သတ်မှတ်ထားသည်။
		]],
		["ca"] = [[
Calcula la diferència entre el valor de cada punt de dades i el següent. Cada punt de sortida conserva la seva identitat original i estableix el valor en la diferència.
		]],
		["zh-Hans"] = [[
计算每个数据点的值与下一个数据点的值之间的差。每个输出数据点保留其原始属性，仅将值设置为该差值。
		]],
		["zh-Hant"] = [[
計算每個資料點的值與下一個資料點值之間的差異。每個輸出點保留其原始識別資訊，並將值設為該差異。
		]],
		["hr"] = [[
Izračunava razliku između vrijednosti svake podatkovne točke i sljedeće. Svaka izlazna točka zadržava svoj izvorni identitet, a vrijednost se postavlja na razliku.
		]],
		["cs"] = [[
Vypočítá rozdíl mezi hodnotou každého datového bodu a následujícím bodem. Každý výstupní bod si zachová svou původní identitu a jeho hodnota bude nastavena na tento rozdíl.
		]],
		["da"] = [[
Beregner forskellen mellem hvert datapunkts værdi og det næste. Hvert outputpunkt har sin oprindelige identitet, mens værdien sættes til forskellen.
		]],
		["nl"] = [[
Berekent het verschil tussen de waarde van elk gegevenspunt en die van het volgende. Elk uitvoerpunt behoudt zijn oorspronkelijke identiteit, waarbij de waarde op het verschil wordt ingesteld.
		]],
		["et"] = [[
Arvutab iga andmepunkti väärtuse erinevuse järgmise punkti väärtusest. Iga väljundpunkt säilitab oma algse identiteedi, kuid väärtuseks määratakse erinevus.
		]],
		["fil"] = [[
Kinakalkula ang pagkakaiba sa pagitan ng halaga ng bawat data point at ng kasunod nito. Pinananatili ng bawat output point ang orihinal nitong pagkakakilanlan, ngunit itinatakda ang value sa pagkakaiba.
		]],
		["fi"] = [[
Laskee kunkin datapisteen arvon ja seuraavan arvon välisen erotuksen. Jokaisella tulospisteellä on alkuperäisen pisteen tunnistetiedot, mutta sen arvoksi asetetaan erotus.
		]],
		["fr"] = [[
Calcule la différence entre la valeur de chaque point de données et celle du point suivant. Chaque point produit conserve son identité d’origine, avec sa valeur remplacée par la différence.
		]],
		["gl"] = [[
Calcula a diferenza entre o valor de cada punto de datos e o seguinte. Cada punto de saída conserva a súa identidade orixinal, co valor establecido na diferenza.
		]],
		["ka"] = [[
ითვლის სხვაობას თითოეული მონაცემის წერტილის მნიშვნელობასა და შემდეგი წერტილის მნიშვნელობას შორის. თითოეულ გამოტანილ წერტილს აქვს თავდაპირველი იდენტობა, ხოლო მნიშვნელობად დაყენებულია სხვაობა.
		]],
		["de"] = [[
Berechnet die Differenz zwischen dem Wert jedes Datenpunkts und dem nächsten. Jeder Ausgabepunkt behält seine ursprünglichen Eigenschaften, wobei der Wert auf die Differenz gesetzt wird.
		]],
		["el"] = [[
Υπολογίζει τη διαφορά μεταξύ της τιμής κάθε σημείου δεδομένων και του επόμενου. Κάθε σημείο εξόδου διατηρεί την αρχική του ταυτότητα, με την τιμή να ορίζεται στη διαφορά.
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટના મૂલ્ય અને તેના પછીના મૂલ્ય વચ્ચેનો તફાવત ગણે છે. દરેક આઉટપુટ પોઇન્ટ તેની મૂળ ઓળખ જાળવે છે અને તેનું મૂલ્ય તફાવત પર સેટ થાય છે.
		]],
		["hi"] = [[
हर डेटा पॉइंट के मान और अगले डेटा पॉइंट के मान के बीच अंतर की गणना करता है। प्रत्येक आउटपुट पॉइंट की मूल पहचान रहती है और उसका मान अंतर पर सेट होता है।
		]],
		["hu"] = [[
Kiszámítja az egyes adatpontok értéke és a következő adatpont értéke közötti különbséget. Minden kimeneti pont megőrzi eredeti azonosságát, értéke pedig a különbség lesz.
		]],
		["is"] = [[
Reiknar út mismuninn milli gildis hvers gagnapunkts og þess næsta. Hver úttakspunktur hefur upphaflega auðkennið sitt en gildið er stillt á mismuninn.
		]],
		["id"] = [[
Menghitung selisih antara nilai setiap titik data dan titik berikutnya. Setiap titik keluaran memiliki identitas aslinya dengan nilai yang diatur ke selisih tersebut.
		]],
		["it"] = [[
Calcola la differenza tra il valore di ogni punto dati e quello successivo. Ogni punto di output mantiene la propria identità, con il valore impostato sulla differenza.
		]],
		["ja"] = [[
各データポイントの値と次のデータポイントの値との差を計算します。各出力ポイントは元の属性を持ち、値だけが差分に設定されます。
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯ ಮತ್ತು ಮುಂದಿನ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯದ ನಡುವಿನ ವ್ಯತ್ಯಾಸವನ್ನು ಲೆಕ್ಕಹಾಕುತ್ತದೆ. ಪ್ರತಿ ಔಟ್‌ಪುಟ್ ಬಿಂದುವು ತನ್ನ ಮೂಲ ಗುರುತನ್ನು ಉಳಿಸಿಕೊಂಡು, ಅದರ ಮೌಲ್ಯವನ್ನು ವ್ಯತ್ಯಾಸಕ್ಕೆ ಹೊಂದಿಸುತ್ತದೆ.
		]],
		["kk"] = [[
Әр дерек нүктесінің мәні мен келесі дерек нүктесінің мәні арасындағы айырманы есептейді. Әр шығыс нүктесі бастапқы бірегейлігін сақтап, оның мәні айырмаға орнатылады.
		]],
		["km"] = [[
គណនាភាពខុសគ្នារវាងតម្លៃរបស់ចំណុចទិន្នន័យនីមួយៗ និងចំណុចបន្ទាប់។ ចំណុចលទ្ធផលនីមួយៗមានអត្តសញ្ញាណដើមរបស់វា ដោយតម្លៃត្រូវបានកំណត់ទៅជាភាពខុសគ្នា។
		]],
		["ko"] = [[
각 데이터 포인트의 값과 다음 데이터 포인트의 값 사이의 차이를 계산합니다. 각 출력 포인트는 원래의 정체성을 유지하며 값만 차이로 설정됩니다.
		]],
		["ky"] = [[
Ар бир маалымат чекитинин мааниси менен кийинки маалымат чекитинин маанисинин айырмасын эсептейт. Ар бир чыгаруу чекити баштапкы өзгөчөлүктөрүн сактап, мааниси айырмага өзгөртүлөт.
		]],
		["lo"] = [[
ຄຳນວນຄວາມແຕກຕ່າງລະຫວ່າງຄ່າຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດກັບຈຸດຖັດໄປ. ຈຸດຜົນລັບແຕ່ລະຈຸດຈະຮັກສາຕົວຕົນເດີມໄວ້ ໂດຍກຳນົດຄ່າເປັນຄວາມແຕກຕ່າງ.
		]],
		["lv"] = [[
Aprēķina starpību starp katra datu punkta vērtību un nākamā punkta vērtību. Katram izvades punktam tiek saglabāta tā sākotnējā identitāte, bet vērtība tiek iestatīta uz starpību.
		]],
		["lt"] = [[
Apskaičiuoja skirtumą tarp kiekvieno duomenų taško reikšmės ir kito. Kiekvienas išvesties taškas išlaiko savo pradinę tapatybę, o jo reikšmė nustatoma į skirtumą.
		]],
		["mk"] = [[
Ја пресметува разликата помеѓу вредноста на секоја точка на податоци и следната. Секоја излезна точка го има својот оригинален идентитет, а вредноста е поставена на разликата.
		]],
		["ms"] = [[
Mengira perbezaan antara nilai setiap titik data dengan titik data seterusnya. Setiap titik output mengekalkan identiti asalnya dengan nilai ditetapkan kepada perbezaan tersebut.
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യവും അടുത്ത ഡാറ്റാ പോയിന്റിന്റെ മൂല്യവും തമ്മിലുള്ള വ്യത്യാസം കണക്കാക്കുന്നു. ഓരോ ഔട്ട്പുട്ട് പോയിന്റും അതിന്റെ യഥാർത്ഥ ഐഡന്റിറ്റി നിലനിർത്തുകയും മൂല്യം വ്യത്യാസമായി സജ്ജീകരിക്കുകയും ചെയ്യുന്നു.
		]],
		["mr"] = [[
प्रत्येक डेटा पॉइंटचे मूल्य आणि पुढील पॉइंटचे मूल्य यांमधील फरक मोजते. प्रत्येक आउटपुट पॉइंटची मूळ ओळख कायम राहते आणि त्याचे मूल्य फरकावर सेट केले जाते.
		]],
		["mn"] = [[
Өгөгдлийн цэг бүрийн утга болон дараагийн цэгийн утгын зөрүүг тооцоолно. Гаралтын цэг бүр анхны мэдээллээ хадгалж, утгыг зөрүүгээр тохируулна.
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दुको मान र अर्को डेटा बिन्दुको मानबीचको अन्तर गणना गर्छ। प्रत्येक आउटपुट बिन्दुले आफ्नो मूल पहिचान कायम राख्छ र मान अन्तरमा सेट हुन्छ।
		]],
		["no"] = [[
Beregner forskjellen mellom verdien til hvert datapunkt og det neste. Hvert utgående punkt beholder sin opprinnelige identitet, mens verdien settes til forskjellen.
		]],
		["pl"] = [[
Oblicza różnicę między wartością każdego punktu danych a wartością następnego punktu. Każdy punkt wyjściowy zachowuje swoją pierwotną tożsamość, a jego wartość zostaje ustawiona na różnicę.
		]],
		["pt"] = [[
Calcula a diferença entre o valor de cada ponto de dados e o seguinte. Cada ponto de saída mantém a sua identidade original, com o valor definido como a diferença.
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਅਤੇ ਅਗਲੇ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਵਿਚਕਾਰ ਅੰਤਰ ਗਿਣਦਾ ਹੈ। ਹਰੇਕ ਆਉਟਪੁੱਟ ਪੁਆਇੰਟ ਆਪਣੀ ਮੂਲ ਪਛਾਣ ਰੱਖਦਾ ਹੈ ਅਤੇ ਇਸਦਾ ਮੁੱਲ ਅੰਤਰ ’ਤੇ ਸੈੱਟ ਹੁੰਦਾ ਹੈ।
		]],
		["ro"] = [[
Calculează diferența dintre valoarea fiecărui punct de date și următorul. Fiecare punct rezultat își păstrează identitatea originală, iar valoarea este setată la diferență.
		]],
		["rm"] = [[
Calcula la differenza tranter la valur da mintga punct da datas e la valur dal proxim. Mintga punct da sortida ha sia identitad originala, cun la valur fixada a la differenza.
		]],
		["ru"] = [[
Вычисляет разницу между значением каждой точки данных и следующей точкой. Каждая выходная точка сохраняет исходные данные, а её значение устанавливается равным разнице.
		]],
		["sr"] = [[
Izračunava razliku između vrednosti svake tačke podataka i sledeće tačke. Svaka izlazna tačka zadržava svoj originalni identitet, a vrednost se postavlja na razliku.
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයකම අගය සහ ඊළඟ දත්ත ලක්ෂ්‍යයේ අගය අතර වෙනස ගණනය කරයි. සෑම ප්‍රතිදාන ලක්ෂ්‍යයකටම එහි මුල් අනන්‍යතාව ඇති අතර, අගය එම වෙනසට සකසා ඇත.
		]],
		["sk"] = [[
Vypočíta rozdiel medzi hodnotou každého údajového bodu a nasledujúcim bodom. Každý výstupný bod si zachová pôvodnú identitu a jeho hodnota sa nastaví na rozdiel.
		]],
		["sl"] = [[
Izračuna razliko med vrednostjo vsake podatkovne točke in naslednjo. Vsaka izhodna točka ohrani svojo izvirno identiteto, vrednost pa je nastavljena na razliko.
		]],
		["es"] = [[
Calcula la diferencia entre el valor de cada punto de datos y el siguiente. Cada punto de salida conserva su identidad original, con el valor establecido en la diferencia.
		]],
		["sw"] = [[
Hukokotoa tofauti kati ya thamani ya kila nukta ya data na inayofuata. Kila nukta ya matokeo huhifadhi utambulisho wake wa awali huku thamani ikiwekwa kuwa tofauti hiyo.
		]],
		["sv"] = [[
Beräknar skillnaden mellan varje datapunkts värde och nästa datapunkts värde. Varje utgående punkt har sin ursprungliga identitet med värdet inställt på skillnaden.
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளியின் மதிப்புக்கும் அடுத்த தரவுப் புள்ளியின் மதிப்புக்கும் இடையிலான வேறுபாட்டைக் கணக்கிடுகிறது. ஒவ்வொரு வெளியீட்டுப் புள்ளியும் அதன் அசல் அடையாளத்தைத் தக்கவைத்து, மதிப்பு வேறுபாடாக அமைக்கப்படும்.
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్ విలువకు తదుపరి డేటా పాయింట్ విలువకు మధ్య వ్యత్యాసాన్ని లెక్కిస్తుంది. ప్రతి అవుట్‌పుట్ పాయింట్ తన అసలు గుర్తింపును కలిగి ఉండి, విలువ వ్యత్యాసంగా సెట్ చేయబడుతుంది.
		]],
		["th"] = [[
คำนวณส่วนต่างระหว่างค่าของจุดข้อมูลแต่ละจุดกับจุดถัดไป จุดผลลัพธ์แต่ละจุดมีข้อมูลประจำตัวเดิม โดยตั้งค่าเป็นส่วนต่าง
		]],
		["tr"] = [[
Her veri noktasının değeri ile bir sonraki arasındaki farkı hesaplar. Her çıktı noktası özgün kimliğine sahip olur ve değeri farka ayarlanır.
		]],
		["uk"] = [[
Обчислює різницю між значенням кожної точки даних і наступним. Кожна вихідна точка зберігає свою початкову ідентичність, а її значення встановлюється як різниця.
		]],
		["vi"] = [[
Tính chênh lệch giữa giá trị của mỗi điểm dữ liệu và điểm tiếp theo. Mỗi điểm đầu ra giữ nguyên danh tính ban đầu, chỉ thay đổi giá trị thành phần chênh lệch.
		]],
	},
	config = {},

	-- Generator function
	generator = function(source)
		local next_point = nil

		return function()
			-- Pre-load the next point on first call
			if next_point == nil then
				next_point = source.dp()
				if not next_point then
					return nil
				end
			end

			-- Current point is what we'll output
			local current_point = next_point

			-- Pre-load the next point for the next iteration
			next_point = source.dp()
			if not next_point then
				-- No more points, can't calculate difference
				return nil
			end

			-- Calculate difference (current - next)
			local difference = current_point.value - next_point.value

			-- Return current point with difference as value
			return {
				timestamp = current_point.timestamp,
				offset = current_point.offset,
				value = difference,
				label = current_point.label,
				note = current_point.note,
			}
		end
	end,
}
