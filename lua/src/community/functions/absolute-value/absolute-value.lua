-- Lua Function to take absolute value
-- Converts all data point values to their absolute value

return {
	-- Configuration metadata
	id = "absolute-value",
	version = "1.0.1",
	inputCount = 1,
	categories = {"_arithmetic"},
	title = {
		["en"] = "Absolute Value",
		["af"] = "Absolute Waarde",
		["sq"] = "Vlera absolute",
		["am"] = "ፍፁም ዋጋ",
		["hy"] = "Բացարձակ արժեք",
		["az"] = "Mütləq qiymət",
		["bn"] = "পরম মান",
		["eu"] = "Balio absolutua",
		["be"] = "Абсалютнае значэнне",
		["bg"] = "Абсолютна стойност",
		["my"] = "အကြွင်းမဲ့တန်ဖိုး",
		["ca"] = "Valor absolut",
		["zh-Hans"] = "绝对值",
		["zh-Hant"] = "絕對值",
		["hr"] = "Apsolutna vrijednost",
		["cs"] = "Absolutní hodnota",
		["da"] = "Absolutværdi",
		["nl"] = "Absolute waarde",
		["et"] = "Absoluutväärtus",
		["fil"] = "Ganap na Halaga",
		["fi"] = "Absoluuttinen arvo",
		["fr"] = "Valeur absolue",
		["gl"] = "Valor absoluto",
		["ka"] = "აბსოლუტური მნიშვნელობა",
		["de"] = "Absolutwert",
		["el"] = "Απόλυτη τιμή",
		["gu"] = "પરમ મૂલ્ય",
		["hi"] = "निरपेक्ष मान",
		["hu"] = "Abszolút érték",
		["is"] = "Algildi",
		["id"] = "Nilai Absolut",
		["it"] = "Valore assoluto",
		["ja"] = "絶対値",
		["kn"] = "ನಿರಪೇಕ್ಷ ಮೌಲ್ಯ",
		["kk"] = "Абсолюттік мән",
		["km"] = "តម្លៃដាច់ខាត",
		["ko"] = "절댓값",
		["ky"] = "Абсолюттук маани",
		["lo"] = "ຄ່າສຳບູນ",
		["lv"] = "Absolūtā vērtība",
		["lt"] = "Absoliuti reikšmė",
		["mk"] = "Апсолутна вредност",
		["ms"] = "Nilai Mutlak",
		["ml"] = "പരമമൂല്യം",
		["mr"] = "निरपेक्ष मूल्य",
		["mn"] = "Абсолют утга",
		["ne"] = "निरपेक्ष मान",
		["no"] = "Absoluttverdi",
		["pl"] = "Wartość bezwzględna",
		["pt"] = "Valor absoluto",
		["pa"] = "ਪੂਰਨ ਮੁੱਲ",
		["ro"] = "Valoare absolută",
		["rm"] = "Valur absolut",
		["ru"] = "Абсолютное значение",
		["sr"] = "Apsolutna vrednost",
		["si"] = "නිරපේක්ෂ අගය",
		["sk"] = "Absolútna hodnota",
		["sl"] = "Absolutna vrednost",
		["es"] = "Valor absoluto",
		["sw"] = "Thamani Kamili",
		["sv"] = "Absolutvärde",
		["ta"] = "முழுமதிப்பு",
		["te"] = "సంపూర్ణ విలువ",
		["th"] = "ค่าสัมบูรณ์",
		["tr"] = "Mutlak Değer",
		["uk"] = "Абсолютне значення",
		["vi"] = "Giá trị tuyệt đối",
	},
	description = {
		["en"] = [[
Converts each data point's value to its absolute value (removes negative sign).
		]],
		["af"] = [[
Skakel elke datapunt se waarde om na sy absolute waarde (verwyder die negatiewe teken).
		]],
		["sq"] = [[
Shndërron vlerën e çdo pike të të dhënave në vlerën e saj absolute (heq shenjën negative).
		]],
		["am"] = [[
የእያንዳንዱን የውሂብ ነጥብ ዋጋ ወደ ፍፁም ዋጋው ይቀይራል (አሉታዊ ምልክቱን ያስወግዳል)።
		]],
		["hy"] = [[
Յուրաքանչյուր տվյալակետի արժեքը փոխակերպում է դրա բացարձակ արժեքի (հեռացնում է բացասական նշանը)։
		]],
		["az"] = [[
Hər məlumat nöqtəsinin qiymətini mütləq qiymətə çevirir (mənfi işarəni silir).
		]],
		["bn"] = [[
প্রতিটি ডেটা পয়েন্টের মানকে তার পরম মানে রূপান্তর করে (ঋণাত্মক চিহ্ন সরিয়ে দেয়)।
		]],
		["eu"] = [[
Datu-puntu bakoitzaren balioa haren balio absolutura bihurtzen du (zeinu negatiboa kentzen du).
		]],
		["be"] = [[
Пераўтварае значэнне кожнай кропкі даных у яго абсалютнае значэнне (выдаляе знак мінус).
		]],
		["bg"] = [[
Преобразува стойността на всяка точка от данни в абсолютната ѝ стойност (премахва знака минус).
		]],
		["my"] = [[
ဒေတာမှတ်တစ်ခုချင်းစီ၏ တန်ဖိုးကို အကြွင်းမဲ့တန်ဖိုးအဖြစ် ပြောင်းလဲသည် (အနှုတ်လက္ခဏာကို ဖယ်ရှားသည်။)
		]],
		["ca"] = [[
Converteix el valor de cada punt de dades en el seu valor absolut (elimina el signe negatiu).
		]],
		["zh-Hans"] = [[
将每个数据点的值转换为其绝对值（移除负号）。
		]],
		["zh-Hant"] = [[
將每個資料點的值轉換為絕對值（移除負號）。
		]],
		["hr"] = [[
Pretvara vrijednost svake podatkovne točke u apsolutnu vrijednost (uklanja znak minusa).
		]],
		["cs"] = [[
Převede hodnotu každého datového bodu na její absolutní hodnotu (odstraní znaménko minus).
		]],
		["da"] = [[
Konverterer hver datapunkts værdi til dens absolutte værdi (fjerner det negative fortegn).
		]],
		["nl"] = [[
Zet de waarde van elk gegevenspunt om naar de absolute waarde (verwijdert het minteken).
		]],
		["et"] = [[
Teisendab iga andmepunkti väärtuse absoluutväärtuseks (eemaldab miinusmärgi).
		]],
		["fil"] = [[
Ginagawang ganap na halaga ang halaga ng bawat data point (inaalis ang negatibong tanda).
		]],
		["fi"] = [[
Muuttaa kunkin datapisteen arvon itseisarvoksi (poistaa miinusmerkin).
		]],
		["fr"] = [[
Convertit la valeur de chaque point de données en sa valeur absolue (supprime le signe négatif).
		]],
		["gl"] = [[
Converte o valor de cada punto de datos no seu valor absoluto (elimina o signo negativo).
		]],
		["ka"] = [[
თითოეული მონაცემის წერტილის მნიშვნელობას გარდაქმნის მის აბსოლუტურ მნიშვნელობად (შლის უარყოფით ნიშანს).
		]],
		["de"] = [[
Wandelt den Wert jedes Datenpunkts in seinen Absolutwert um (entfernt das Minuszeichen).
		]],
		["el"] = [[
Μετατρέπει την τιμή κάθε σημείου δεδομένων στην απόλυτη τιμή της (αφαιρεί το αρνητικό πρόσημο).
		]],
		["gu"] = [[
દરેક ડેટા પોઇન્ટના મૂલ્યને તેના પરમ મૂલ્યમાં રૂપાંતરિત કરે છે (ઋણ ચિહ્ન દૂર કરે છે).
		]],
		["hi"] = [[
हर डेटा पॉइंट के मान को उसके निरपेक्ष मान में बदलता है (ऋण चिह्न हटाता है)।
		]],
		["hu"] = [[
Az egyes adatpontok értékét abszolút értékre alakítja (eltávolítja a negatív előjelet).
		]],
		["is"] = [[
Breytir gildi hvers gagnapunkts í algildi þess (fjarlægir mínusmerki).
		]],
		["id"] = [[
Mengonversi nilai setiap titik data menjadi nilai absolutnya (menghapus tanda negatif).
		]],
		["it"] = [[
Converte il valore di ogni punto dati nel suo valore assoluto (rimuove il segno negativo).
		]],
		["ja"] = [[
各データポイントの値を絶対値に変換します（負号を取り除きます）。
		]],
		["kn"] = [[
ಪ್ರತಿ ಡೇಟಾ ಬಿಂದುವಿನ ಮೌಲ್ಯವನ್ನು ಅದರ ನಿರಪೇಕ್ಷ ಮೌಲ್ಯಕ್ಕೆ ಪರಿವರ್ತಿಸುತ್ತದೆ (ಋಣ ಚಿಹ್ನೆಯನ್ನು ತೆಗೆದುಹಾಕುತ್ತದೆ).
		]],
		["kk"] = [[
Әр дерек нүктесінің мәнін абсолюттік мәніне түрлендіреді (теріс таңбаны алып тастайды).
		]],
		["km"] = [[
បម្លែងតម្លៃនៃចំណុចទិន្នន័យនីមួយៗទៅជាតម្លៃដាច់ខាត (លុបសញ្ញាអវិជ្ជមានចេញ)។
		]],
		["ko"] = [[
각 데이터 포인트의 값을 절댓값으로 변환합니다(음수 기호 제거).
		]],
		["ky"] = [[
Ар бир маалымат чекитинин маанисин абсолюттук мааниге өзгөртөт (терс белгини алып салат).
		]],
		["lo"] = [[
ປ່ຽນຄ່າຂອງຈຸດຂໍ້ມູນແຕ່ລະຈຸດເປັນຄ່າສຳບູນ (ລຶບເຄື່ອງໝາຍລົບອອກ).
		]],
		["lv"] = [[
Pārvērš katra datu punkta vērtību par tās absolūto vērtību (noņem negatīvo zīmi).
		]],
		["lt"] = [[
Kiekvieno duomenų taško reikšmę paverčia absoliučiąja reikšme (pašalina neigiamą ženklą).
		]],
		["mk"] = [[
Ја претвора вредноста на секоја точка на податоци во апсолутна вредност (го отстранува знакот минус).
		]],
		["ms"] = [[
Menukar nilai setiap titik data kepada nilai mutlaknya (membuang tanda negatif).
		]],
		["ml"] = [[
ഓരോ ഡാറ്റാ പോയിന്റിന്റെയും മൂല്യം അതിന്റെ പരമമൂല്യമായി മാറ്റുന്നു (നെഗറ്റീവ് ചിഹ്നം നീക്കം ചെയ്യുന്നു).
		]],
		["mr"] = [[
प्रत्येक डेटा बिंदूचे मूल्य त्याच्या निरपेक्ष मूल्यात रूपांतरित करते (ऋण चिन्ह काढून टाकते).
		]],
		["mn"] = [[
Өгөгдлийн цэг бүрийн утгыг абсолют утга болгон хөрвүүлнэ (сөрөг тэмдгийг арилгана).
		]],
		["ne"] = [[
प्रत्येक डेटा बिन्दुको मानलाई यसको निरपेक्ष मानमा रूपान्तरण गर्छ (ऋणात्मक चिह्न हटाउँछ)।
		]],
		["no"] = [[
Konverterer verdien til hvert datapunkt til absoluttverdien (fjerner minustegnet).
		]],
		["pl"] = [[
Konwertuje wartość każdego punktu danych na wartość bezwzględną (usuwa znak minus).
		]],
		["pt"] = [[
Converte o valor de cada ponto de dados no seu valor absoluto (remove o sinal negativo).
		]],
		["pa"] = [[
ਹਰੇਕ ਡਾਟਾ ਪੁਆਇੰਟ ਦੇ ਮੁੱਲ ਨੂੰ ਉਸਦੇ ਪੂਰਨ ਮੁੱਲ ਵਿੱਚ ਬਦਲਦਾ ਹੈ (ਨਕਾਰਾਤਮਕ ਚਿੰਨ੍ਹ ਹਟਾਉਂਦਾ ਹੈ)।
		]],
		["ro"] = [[
Convertește valoarea fiecărui punct de date în valoarea sa absolută (elimină semnul minus).
		]],
		["rm"] = [[
Converta la valur da mintga punct da datas en sia valur absoluta (ils segns negativs vegnan allontanads).
		]],
		["ru"] = [[
Преобразует значение каждой точки данных в его абсолютное значение (убирает знак минус).
		]],
		["sr"] = [[
Pretvara vrednost svake tačke podataka u njenu apsolutnu vrednost (uklanja znak minus).
		]],
		["si"] = [[
සෑම දත්ත ලක්ෂ්‍යයකම අගය එහි නිරපේක්ෂ අගයට පරිවර්තනය කරයි (ඍණ ලකුණ ඉවත් කරයි).
		]],
		["sk"] = [[
Prevedie hodnotu každého údajového bodu na absolútnu hodnotu (odstráni záporné znamienko).
		]],
		["sl"] = [[
Pretvori vrednost vsake podatkovne točke v absolutno vrednost (odstrani negativni predznak).
		]],
		["es"] = [[
Convierte el valor de cada punto de datos en su valor absoluto (elimina el signo negativo).
		]],
		["sw"] = [[
Hubadilisha thamani ya kila nukta ya data kuwa thamani yake kamili (huondoa alama hasi).
		]],
		["sv"] = [[
Omvandlar värdet för varje datapunkt till dess absolutvärde (tar bort minustecknet).
		]],
		["ta"] = [[
ஒவ்வொரு தரவுப் புள்ளியின் மதிப்பையும் அதன் முழுமதிப்பாக மாற்றுகிறது (எதிர்மறைக் குறியை நீக்குகிறது).
		]],
		["te"] = [[
ప్రతి డేటా పాయింట్ విలువను దాని సంపూర్ణ విలువగా మారుస్తుంది (మైనస్ గుర్తును తొలగిస్తుంది).
		]],
		["th"] = [[
แปลงค่าของจุดข้อมูลแต่ละจุดเป็นค่าสัมบูรณ์ (ลบเครื่องหมายลบออก)
		]],
		["tr"] = [[
Her veri noktasının değerini mutlak değerine dönüştürür (negatif işareti kaldırır).
		]],
		["uk"] = [[
Перетворює значення кожної точки даних на його абсолютне значення (видаляє знак мінус).
		]],
		["vi"] = [[
Chuyển đổi giá trị của từng điểm dữ liệu thành giá trị tuyệt đối (loại bỏ dấu âm).
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

			-- Convert to absolute value
			data_point.value = math.abs(data_point.value)

			return data_point
		end
	end,
}
